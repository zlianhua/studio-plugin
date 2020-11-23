package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RestProxyCreator {
    public static PsiClass createRestProxy(Project project, ComponentDefinition component, String proxyName){
        Path restProxyPath = Paths.get(project.getBasePath()+ File.separator+ FileCreateHelper.getRestProxyPath(component));
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        packageImports.add("org.springframework.beans.factory.annotation");

        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());
        classImports.add("com.fasterxml.jackson.databind.type.TypeFactory");

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Service");

        PsiClass restProxy = PsJavaFileHelper.createPsiClass(project,restProxyPath,proxyName,packageImports,classImports,classAnnotations);

        PsiType StringType = new PsiJavaParserFacadeImpl(project).createTypeFromText(String.class.getName(),null);
        List<String> annotations = new ArrayList<>();
        annotations.add("@Autowired");
        PsJavaFileHelper.addField(restProxy,"baseUrl",null,StringType,annotations);

        PsiType restTemplateType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.web.client.RestTemplate",null);
        annotations = new ArrayList<>();
        PsJavaFileHelper.addField(restProxy,"restTemplate","new RestTemplate()",StringType,annotations);

        PsiType mapperType = new PsiJavaParserFacadeImpl(project).createTypeFromText("com.fasterxml.jackson.databind.ObjectMapper",null);
        PsJavaFileHelper.addField(restProxy,"mapper","new ObjectMapper()",StringType,annotations);

        PsiType headersType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.http.HttpHeaders",null);
        PsJavaFileHelper.addField(restProxy,"headers","new HttpHeaders()",StringType,annotations);

        return restProxy;
    }

    public static void addMethodToRestProxyClass(ComponentDefinition component, String rootEntitySimpleName, PsiClass psiClass, PsiMethod[] methods, PsiElementFactory elementFactory, JavaCodeStyleManager codeStyleManager, boolean isGet){
        String baseUrl = "        String url = baseUrl+\"/"+ StringUtils.uncapitalize(component.getSimpleName());
        String rootEntityVar = StringUtils.uncapitalize(rootEntitySimpleName);
        for(PsiMethod method : methods){
            boolean useCommonRequest = false;
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
                    String responseType = useCommonRequest?"CommonResponse":"String";
                    methodStr.append("    ResponseEntity<"+responseType+"> resultEntity =  restTemplate.exchange(url,HttpMethod.GET,entity, "+responseType+".class);\n")
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
                    String responseType = useCommonRequest?"CommonResponse":"String";
                    methodStr.append("    HttpEntity<" + requestType + "> request = new HttpEntity<>(" + rootEntityVar + ",headers);\n");
                    if(!returnTypeName.equals("void")){
                        methodStr.append("    ResponseEntity<"+responseType+"> resp = restTemplate.postForEntity(url,request,"+responseType+".class);\n")
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
}
