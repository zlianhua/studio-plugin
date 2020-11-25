package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RestProxyCreator {
    public static PsiClass createRestProxy(Project project, ComponentDefinition component, String proxyName){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        String serviceName = proxyName.replace("RestProxy","");
        Path restProxyPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getRestProxyPath(component));
        VirtualFile apiVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(restProxyPath);
        if(null==apiVirtualFile){
            createRestProxyPath(component);
            apiVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(restProxyPath);
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        packageImports.add("org.springframework.beans.factory.annotation");
        packageImports.add("org.springframework.http");

        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());
        classImports.add("com.fasterxml.jackson.databind.type.TypeFactory");
        classImports.add(RestTemplate.class.getName());
        classImports.add(ObjectMapper.class.getName());

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Service");

        PsiClass restProxy = PsJavaFileHelper.createPsiClass(project,apiVirtualFile,proxyName,packageImports,classImports,classAnnotations,null);

        restProxy.getImplementsList().add(elementFactory.createReferenceFromText(serviceName,restProxy));

        PsiType StringType = new PsiJavaParserFacadeImpl(project).createTypeFromText(String.class.getName(),null);
        List<String> annotations = new ArrayList<>();
        annotations.add("@Autowired");
        PsJavaFileHelper.addField(restProxy,"baseUrl",null,StringType,annotations);

        PsiType restTemplateType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.web.client.RestTemplate",null);
        annotations = new ArrayList<>();
        PsJavaFileHelper.addField(restProxy,"restTemplate","new RestTemplate()",restTemplateType,annotations);

        PsiType mapperType = new PsiJavaParserFacadeImpl(project).createTypeFromText("com.fasterxml.jackson.databind.ObjectMapper",null);
        PsJavaFileHelper.addField(restProxy,"mapper","new ObjectMapper()",mapperType,annotations);

        PsiType headersType = new PsiJavaParserFacadeImpl(project).createTypeFromText("org.springframework.http.HttpHeaders",null);
        PsJavaFileHelper.addField(restProxy,"headers","new HttpHeaders()",headersType,annotations);

        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(restProxy.getContainingFile().getParent());
        PsiClass restConfigPsiClass = PsJavaFileHelper.getEntity(psiPackage,component.getSimpleName()+"RestConfiguration");
        if(null==restConfigPsiClass){
            restConfigPsiClass = createRestConfig(project,component);
        }
        StringBuilder methodStr = new StringBuilder();
        methodStr.append("public ")
                .append(serviceName)
                .append(" ")
                .append(StringUtils.uncapitalize(serviceName))
                .append("(){\n")
                .append("    return new ")
                .append(proxyName)
                .append("();\n")
                .append("}\n");
        PsiMethod getRestProxyBean = elementFactory.createMethodFromText(methodStr.toString(),restConfigPsiClass);
        getRestProxyBean.getModifierList().addAnnotation("Bean");
        restConfigPsiClass.add(getRestProxyBean);
        return restProxy;
    }

    public static PsiClass createRestConfig(Project project, ComponentDefinition component) {
        Path restProxyPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getRestProxyPath(component));
        VirtualFile apiVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(restProxyPath);
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("org.springframework.context.annotation");
        packageImports.add("org.springframework.beans.factory.annotation");

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Configuration");
        PsiClass restConfig = PsJavaFileHelper.createPsiClass(project,apiVirtualFile,component.getSimpleName()+"RestConfiguration",packageImports,null,classAnnotations,null);

        PsiType StringType = new PsiJavaParserFacadeImpl(project).createTypeFromText(String.class.getName(),null);
        List<String> annotations = new ArrayList<>();
        annotations.add("@Value(\"${com.ai.bss.demo.baseurl}\")");
        PsJavaFileHelper.addField(restConfig,"baseUrl",null,StringType,annotations);

        //getBaseUrl method
        StringBuilder methodStr = new StringBuilder();
        methodStr.append("public String getBaseUrl(){\n")
                .append("    return baseUrl;\n")
                .append("}\n");
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiMethod  getBaseUrl = elementFactory.createMethodFromText(methodStr.toString(),restConfig);
        getBaseUrl.getModifierList().addAnnotation("Bean");
        restConfig.add(getBaseUrl);
        return restConfig;
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

    public static void createRestProxyPath(ComponentDefinition component){
        StringBuilder restPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-rest-proxy".toLowerCase());
        File rest = new File(restPath.toString());
        if(!rest.exists()){
            rest.mkdirs();
        }
        StringBuilder restSrcPkgPath = ComponentCreator.componentSrcPkg(component,restPath)
                .append(File.separator)
                .append("rest")
                .append(File.separator)
                .append("proxy");
        File restSrcPkg = new File(restSrcPkgPath.toString());
        if(!restSrcPkg.exists()){
            restSrcPkg.mkdirs();
        }
    }

    public static void createRestProxyModule(ComponentDefinition component)  throws Exception{
        createRestProxyPath(component);
        MemoryFile restProxyPom = ComponentVmUtil.createRestProxyPom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(restProxyPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restProxyPom.content);
    }
}
