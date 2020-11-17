package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.CollectionListModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.Audited;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PsJavaFileHelper {
    public static PsiField findField(PsiClass psiClass, String fieldName){
        List<PsiField> fields = (new CollectionListModel<>(psiClass.getFields())).getItems();
        for(PsiField field : fields){
            if(field.getName().equalsIgnoreCase(fieldName)){
                return field;
            }
        }
        return null;
    }

    public static void deleteField(PsiClass psiClass, String fieldName){
        PsiField field = findField(psiClass,fieldName);
        if(null!=field){
            PsiAnnotation[] annotations = field.getAnnotations();
            for(PsiAnnotation annotation : annotations){
                annotation.delete();
            }
            field.delete();
        }
    }

    public static PsiField addField(PsiClass psiClass,PsiType fieldType, String fieldName){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, fieldType);
        psiClass.add(field);
        return field;
    }

    public static PsiField addFieldWithAnnotations(PsiClass psiClass, String fieldName, PsiType type, List<String> annotaions){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, type);
        for (String annotation : annotaions){
            PsiElement psiElement = codeStyleManager.shortenClassReferences(elementFactory.createAnnotationFromText(annotation, field));
            field.getModifierList().addBefore(psiElement, field.getModifierList().getFirstChild());
        }
        psiClass.add(field);
        return field;
    }

    public static void addClassAnnotation(PsiClass psiClass,String annotation){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiAnnotation aAnnotation = elementFactory.createAnnotationFromText(annotation, psiClass);
        PsiElement psiElement = codeStyleManager.shortenClassReferences(aAnnotation);
        psiClass.getModifierList().addBefore(psiElement, psiClass.getModifierList().getFirstChild());
    }

    public static JComponent getDialogSource(AnActionEvent e){
        JComponent source;
        Component comp = e.getInputEvent().getComponent();
        if (comp instanceof JComponent) {
            source = (JComponent) comp;
        } else {
            JWindow w;
            if (comp instanceof JWindow) {
                w = (JWindow) comp;
            } else {
                w = (JWindow) WindowManager.getInstance().suggestParentWindow(e.getProject());
            }
            source = w.getRootPane();
        }
        return source;
    }

    public static PsiClass getEntity(PsiPackage psiPackage, String fileName)throws Exception{
        PsiClass[] allClasses = psiPackage.getClasses();
        for(PsiClass acls : allClasses){
            if(acls.getQualifiedName().endsWith(fileName)){
                return acls;
            }
        }
        return null;
    }

    public static boolean isRootEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcRootEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static boolean isValueEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcValueEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static boolean isMemberEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcMemberEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static void createPsiClassFieldsFromTableColumn(Project project, PsiClass psiClass, List<Column> columns, ComponentDefinition component){
        List<String> abstractEntityFieldNames = new ArrayList<>();
        if(component.isExtendsAbstractEntity()){
            abstractEntityFieldNames = FileCreateHelper.getAbstractEntityFields();
        }
        final List<String> ignoreFields = abstractEntityFieldNames;
        if (null != columns && !columns.isEmpty()) {
            for (Column column : columns) {
                String remarks = column.getName();
                String columnName = column.getCode();
                String fieldName = CamelCaseStringUtil.underScore2Camel(columnName, true);
                if(component.isExtendsAbstractEntity() && ignoreFields.contains(fieldName)){
                    continue;
                }
                PsiField field = PsJavaFileHelper.findField(psiClass, fieldName);
                if (null != field) {
                    PsJavaFileHelper.deleteField(psiClass, fieldName);
                }
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                String fieldJavaType = DBMetaDataUtil.columnDataTypeToJavaType(column.getType());
                PsiType fieldType = new PsiJavaParserFacadeImpl(psiClass.getProject()).createTypeFromText(fieldJavaType, null);
                List<String> annotations = new ArrayList<>();
                annotations.add("@Column(name =\"" + columnName.toUpperCase() + "\")");
                if (column.isPkFlag()) {
                    annotations.add("@GeneratedValue(strategy = GenerationType.AUTO)");
                    annotations.add("@Id");
                }
                if (column.isClob()) {
                    annotations.add("@Lob");
                }
                field = PsJavaFileHelper.addFieldWithAnnotations(psiClass, fieldName, fieldType, annotations);
                PsiComment comment = elementFactory.createCommentFromText("/**" + remarks + "*/", null);
                field.getModifierList().addBefore(comment, field.getModifierList().getFirstChild());
            }
            CodeStyleManager.getInstance(project).reformat(psiClass);
        }
    }

    public static void addNewEntityImports(Project project, PsiClass psiClass){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand("javax.persistence");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Audited.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Getter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(NoArgsConstructor.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Setter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("com.ai.abc.core.annotations");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(EntityToJsonConverter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Timestamp.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(List.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
    }

    public static void addMethodToInterfaceClass(PsiClass psiClass,PsiMethod[] methods,PsiElementFactory elementFactory,JavaCodeStyleManager codeStyleManager){
        for(PsiMethod method : methods){
            if(null!=psiClass.findMethodBySignature(method,true)){
                continue;
            }
            StringBuilder methodStr = new StringBuilder();
            methodStr.append(method.getReturnType().getPresentableText())
                    .append(" ")
                    .append(method.getName())
                    .append("(");
            JvmParameter[] parameters= method.getParameters();
            for(JvmParameter parameter : parameters){
                methodStr.append(parameter.getType().toString().replace("PsiType:", ""))
                        .append(" ")
                        .append(parameter.getName())
                        .append(",");
            }
            if(methodStr.toString().endsWith(",")){
                methodStr.deleteCharAt(methodStr.lastIndexOf(","));
            }
            methodStr.append(")");
            PsiReferenceList throwList = method.getThrowsList();
            if(null!=throwList && throwList.getRole().equals(PsiReferenceList.Role.THROWS_LIST)){
                PsiJavaCodeReferenceElement [] throwEles = throwList.getReferenceElements();
                methodStr.append(" throws ");
                for(PsiJavaCodeReferenceElement element : throwEles){
                    methodStr.append(element.getReferenceName())
                            .append(",");
                }
                methodStr.deleteCharAt(methodStr.lastIndexOf(","));
            }
            methodStr.append(";");
            psiClass.add(elementFactory.createMethodFromText(methodStr.toString(),psiClass));
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        codeStyleManager.shortenClassReferences(file);
    }

    public static void addMethodToRestProxyClass(ComponentDefinition component,String rootEntitySimpleName,PsiClass psiClass,PsiMethod[] methods,PsiElementFactory elementFactory,JavaCodeStyleManager codeStyleManager,boolean isGet){
        String baseUrl = "        String url = baseUrl+\"/"+StringUtils.uncapitalize(component.getSimpleName());
        String rootEntityVar = StringUtils.uncapitalize(rootEntitySimpleName);
        boolean useCommonRequest = false;
        for(PsiMethod method : methods){
            String returnTypeName = method.getReturnType().getPresentableText();
            if(null!=psiClass.findMethodBySignature(method,false)){
                continue;
            }
            String url = baseUrl+"/"+method.getName()+"\"";
            StringBuilder paramMap = new StringBuilder();
            paramMap.append("        Map<String, Object> params = new HashMap<>();\n");
            StringBuilder methodStr = new StringBuilder();
            methodStr.append("public ")
                    .append(returnTypeName)
                    .append(" ")
                    .append(method.getName())
                    .append("(");
            JvmParameter[] parameters= method.getParameters();
            for(JvmParameter parameter : parameters){
                String paramName = parameter.getName();
                String paramType = parameter.getType().toString();
                paramType = paramType.replace("PsiType:", "");
                boolean isCommonRequestParam =paramType.startsWith("CommonRequest<")?true:false;
                if(isCommonRequestParam){
                    useCommonRequest = true;
                }
                if(!isCommonRequestParam && (isGet || method.getName().startsWith("delete"))){
                    paramMap.append("        params.put(").append("\"").append(paramName).append("\",").append(paramName).append(");\n");
                    url+="+\"/\"+"+paramName+"+\"/\"";
                }else if (!paramType.contains(rootEntitySimpleName)){
                    if(!url.contains("?")){
                        url+="+\"?"+paramName+"=\"+"+paramName;
                    }else{
                        url+="+\"&"+paramName+"=\"+"+paramName;
                    }
                }
                methodStr.append(paramType)
                        .append(" ")
                        .append(paramName)
                        .append(",");
            }
            if(methodStr.toString().endsWith(",")){
                methodStr.deleteCharAt(methodStr.lastIndexOf(","));
            }
            if (url.endsWith("+\"/\"")){
                int lastIdx=url.lastIndexOf("+\"/\"");
                url = url.substring(0,lastIdx);
            }
            url+=";\n";
            methodStr.append(")");
            PsiReferenceList throwList = method.getThrowsList();
            if(null!=throwList && throwList.getRole().equals(PsiReferenceList.Role.THROWS_LIST)){
                PsiJavaCodeReferenceElement [] throwEles = throwList.getReferenceElements();
                if(null!=throwEles && throwEles.length>0){
                    methodStr.append(" throws ");
                    for(PsiJavaCodeReferenceElement element : throwEles){
                        methodStr.append(element.getReferenceName())
                                .append(",");
                    }
                    if(methodStr.toString().endsWith(",")){
                        methodStr.deleteCharAt(methodStr.lastIndexOf(","));
                    }
                }
            }
            methodStr.append("{\n")
                     .append("    ").append(url);
            if(isGet){
                methodStr.append(paramMap)
                        .append("    HttpEntity<Map<String,Object>> entity = new HttpEntity<>(params, headers);\n");
                if(!returnTypeName.contains("CommonResponse<") && returnTypeName.contains("List<")){
                    int start = returnTypeName.indexOf("List<");
                    int end  = returnTypeName.indexOf(">",start);
                    returnTypeName=returnTypeName.substring(start+"List<".length(),end);
                    methodStr.append("    ResponseEntity<List> resultEntity =  restTemplate.exchange(url,HttpMethod.GET,entity, List.class);\n")
                            .append("    List<Map> mapList = (List<Map>) resultEntity.getBody();\n")
                            .append("    List<"+returnTypeName+"> list = new ArrayList<>();\n")
                            .append("    if(null!=mapList && mapList.size()>0){\n")
                            .append("        list = mapper.convertValue(mapList, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class,"+rootEntitySimpleName+".class));\n")
                            .append("    }\n")
                            .append("    return list;\n");
                }else{
                    methodStr.append("    ResponseEntity<"+returnTypeName+"> resultEntity =  restTemplate.exchange(url,HttpMethod.GET,entity, "+returnTypeName+".class);\n")
                            .append("    return resultEntity.getBody();\n");
                }
                methodStr.append("}\n");
            }else{
                if(!returnTypeName.startsWith("CommonResponse<") && method.getName().startsWith("delete")){
                    methodStr.append(paramMap)
                    .append("    restTemplate.delete(url,params);");
                }else {
                    methodStr.append("    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));\n")
                            .append("    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);\n");
                    String requestType = useCommonRequest?"CommonRequest<"+rootEntitySimpleName+">":rootEntitySimpleName;
                    methodStr.append("    HttpEntity<" + requestType + "> request = new HttpEntity<>(" + rootEntityVar + ",headers);\n");
                    if(!returnTypeName.equals("void")){
                        methodStr.append("    ResponseEntity<"+requestType+"> resp = restTemplate.postForEntity(url,request,String.class);\n")
                                .append("    return resp.getBody();\n");
                    }else{
                        methodStr.append("    restTemplate.postForEntity(url,request,String.class);\n");
                    }
                }
                methodStr.append("}\n");
            }
            psiClass.add(elementFactory.createMethodFromText(methodStr.toString(),psiClass));
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        codeStyleManager.shortenClassReferences(file);
    }

    public static void addMethodToControllerClass(Project project,String rootEntitySimpleName,String serviceName,PsiClass psiClass,PsiMethod[] methods,PsiElementFactory elementFactory,JavaCodeStyleManager codeStyleManager,boolean isGet){
        for(PsiMethod method : methods){
            if(null!=psiClass.findMethodBySignature(method,false)){
                continue;
            }
            StringBuilder mappingAnnotation = new StringBuilder();
            String returnType = method.getReturnType().getPresentableText();
            if(isGet) {
                mappingAnnotation.append("GetMapping");
            }else if (method.getName().startsWith("delete")){
                mappingAnnotation.append("DeleteMapping");
            }else{
                mappingAnnotation.append("PostMapping");
            }
            mappingAnnotation.append("(value = \"/")
            .append(method.getName());
            StringBuilder methodStr = new StringBuilder();
            methodStr.append("public ");
            if(!returnType.equals("void")){
                methodStr.append("ResponseEntity<");
                methodStr.append(returnType)
                .append(">");
            }else{
                methodStr.append("void");
            }
            methodStr.append(" ")
                    .append(method.getName())
                    .append("(");
            JvmParameter[] parameters= method.getParameters();
            StringBuilder paramValue = new StringBuilder();
            for(JvmParameter parameter : parameters){
                String parameterType = parameter.getType().toString();
                String paramType = parameterType.replace("PsiType:", "");
                boolean isCommonRequestParam =parameterType.startsWith("CommonRequest<")?true:false;
                if(!isCommonRequestParam && (isGet || method.getName().startsWith("delete"))){
                    methodStr.append("@PathVariable(value = \"")
                    .append(parameter.getName())
                    .append("\") ");
                    mappingAnnotation.append("/{")
                    .append(parameter.getName())
                    .append("}");
                }else{
                    if(paramType.equals(rootEntitySimpleName) || isCommonRequestParam){
                        methodStr.append("@RequestBody ");
                    }else{
                        methodStr.append("@RequestParam ");
                    }
                }
                paramValue.append(parameter.getName()+",");
                methodStr.append(paramType)
                        .append(" ")
                        .append(parameter.getName())
                        .append(",");
            }
            if(paramValue.toString().endsWith(",")){
                paramValue.deleteCharAt(paramValue.lastIndexOf(","));
            }
            if(methodStr.toString().endsWith(",")){
                methodStr.deleteCharAt(methodStr.lastIndexOf(","));
            }
            methodStr.append("){\n")
                    .append("    try {\n");
            if(!method.getReturnType().getPresentableText().equals("void")){
                methodStr.append("       "+returnType+" returnValue = ");
            }
            methodStr.append("      "+serviceName+"."+method.getName()+"("+paramValue+");\n");
            if(!returnType.equals("void")){
                if(returnType.contains("CommonResponse<")){
                    methodStr.append("    if(returnValue.isSuccess()){\n")
                            .append("        return ResponseEntity.ok(returnValue);\n")
                            .append("    }else{\n")
                            .append("        ResponseEntity resp = new ResponseEntity(returnValue,HttpStatus.INTERNAL_SERVER_ERROR);\n")
                            .append("        return resp;\n")
                            .append("    }\n");
                }

            }
            methodStr.append("    } catch (Exception e) {\n")
                    .append("       log.error(\"业务执行错误\",e);\n")
                    .append("       throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, \"业务执行错误\", e);\n")
                    .append("    }\n")
                    .append("}\n");

            PsiMethod restMethod = elementFactory.createMethodFromText(methodStr.toString(),psiClass);
            mappingAnnotation.append("\",consumes = \"application/json;charset=utf-8\",produces=\"application/json;charset=utf-8\")");
            restMethod.getModifierList().addAnnotation(mappingAnnotation.toString());
            restMethod.getModifierList().addAnnotation("ResponseBody");
            psiClass.add(restMethod);
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        codeStyleManager.shortenClassReferences(file);
    }

    public static PsiClass createRestController(Project project,ComponentDefinition component) throws Exception{
        Path controllerPath = Paths.get(project.getBasePath()+ File.separator+FileCreateHelper.getRestControllerPath(component));
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(VirtualFileManager.getInstance().findFileByNioPath(controllerPath));
        PsiClass controller = JavaDirectoryService.getInstance().createClass(directory, component.getSimpleName()+"Controller");
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile file = (PsiJavaFile)controller.getContainingFile();
        addClassAnnotation(controller,"@Slf4j");
        PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand("com.ai.abc.api.model");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("org.springframework.beans.factory.annotation");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("org.springframework.http");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("org.springframework.web.bind.annotation");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(ResponseStatusException.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Slf4j.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        String modelPkgName = EntityUtil.getComponentPackageName(component)+".model";
        importStatement = elementFactory.createImportStatementOnDemand(modelPkgName);
        file.getImportList().add(importStatement);
        String apiPkgName = EntityUtil.getComponentPackageName(component)+".api";
        importStatement = elementFactory.createImportStatementOnDemand(apiPkgName);
        file.getImportList().add(importStatement);
        return controller;
    }
    public static PsiClass createRestProxy(Project project,ComponentDefinition component,String commandProxyName) throws Exception{
        Path controllerPath = Paths.get(project.getBasePath()+ File.separator+FileCreateHelper.getRestProxyPath(component));
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(VirtualFileManager.getInstance().findFileByNioPath(controllerPath));
        PsiClass restProxy = JavaDirectoryService.getInstance().createClass(directory, commandProxyName);
        PsiJavaFile file = (PsiJavaFile)restProxy.getContainingFile();
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        String modelPkgName = EntityUtil.getComponentPackageName(component)+".model";
        PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand(modelPkgName);
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("org.springframework.http");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("java.util");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("com.ai.abc.api.model");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(org.springframework.stereotype.Service.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("org.springframework.beans.factory.annotation");
        file.getImportList().add(importStatement);
        addClassAnnotation(restProxy,"@Service");
        PsiType StringType = new PsiJavaParserFacadeImpl(project).createTypeFromText(String.class.getName(),null);
        List<String> annotations = new ArrayList<>();
        annotations.add("@Autowired");
        addFieldWithAnnotations(restProxy,"baseUrl",StringType,annotations);

        PsiType restTemplateType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.web.client.RestTemplate",null);
        PsiField restTemplateField = addField(restProxy,restTemplateType,"restTemplate");
        PsiExpression restTemplate = elementFactory.createExpressionFromText("new RestTemplate()",restTemplateField);
        restTemplateField.setInitializer(restTemplate);

        PsiType mapperType = new PsiJavaParserFacadeImpl(project).createTypeFromText("com.fasterxml.jackson.databind.ObjectMapper",null);
        PsiField mapperField = addField(restProxy,mapperType,"mapper");
        PsiExpression mapper = elementFactory.createExpressionFromText("new ObjectMapper()",mapperField);
        mapperField.setInitializer(mapper);

        PsiType headersType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.http.HttpHeaders",null);
        PsiField headersField = addField(restProxy,headersType,"headers");
        PsiExpression headers = elementFactory.createExpressionFromText("new HttpHeaders()",headersField);
        headersField.setInitializer(headers);
        return restProxy;
    }
}
