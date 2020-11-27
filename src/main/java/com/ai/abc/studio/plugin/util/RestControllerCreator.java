package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class RestControllerCreator {
    public static PsiClass createRestController(Project project, ComponentDefinition component, String className) throws Exception{
        Path controllerPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getRestControllerPath(component));
        VirtualFile apiVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(controllerPath);
        if(null==apiVirtualFile){
            createControllerPath(component);
            apiVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(controllerPath);
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        packageImports.add("org.springframework.http");
        packageImports.add("org.springframework.beans.factory.annotation");
        packageImports.add("org.springframework.web.bind.annotation");

        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());
        classImports.add("com.fasterxml.jackson.databind.type.TypeFactory");
        classImports.add(ResponseStatusException.class.getName());
        classImports.add(Slf4j.class.getName());

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Slf4j");

        PsiClass controller = PsJavaFileHelper.createPsiClass(project,apiVirtualFile,className,packageImports,classImports,classAnnotations,null);
        return controller;
    }

    public static void addMethodToControllerClass(Project project, String rootEntitySimpleName, String serviceName, PsiClass psiClass, PsiMethod[] methods, PsiElementFactory elementFactory, JavaCodeStyleManager codeStyleManager, boolean isGet){
        boolean hasList = false;
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
                String paramType = parameter.getType().toString();
                paramType = paramType.replace("PsiType:", "");
                if(paramType.contains("List<")){
                    hasList = true;
                }
                boolean isCommonRequestParam =paramType.startsWith("CommonRequest<")?true:false;
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
            mappingAnnotation.append("\"");
//          mappingAnnotation.append(,consumes = \"application/json;charset=utf-8\",produces=\"application/json;charset=utf-8\"");
            mappingAnnotation.append(")");
            restMethod.getModifierList().addAnnotation(mappingAnnotation.toString());
            restMethod.getModifierList().addAnnotation("ResponseBody");
            psiClass.add(restMethod);
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        if(hasList){
            PsiClassType typeByName = PsiType.getTypeByName(List.class.getName(), project, GlobalSearchScope.allScope(project));
            PsiClass listClass = typeByName.resolve();
            PsiImportStatement importStatement = elementFactory.createImportStatement(listClass);
            file.getImportList().add(importStatement);
        }
        codeStyleManager.shortenClassReferences(file);
    }

    public static void createControllerPath(ComponentDefinition component){
        StringBuilder restPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-rest".toLowerCase());
        File rest = new File(restPath.toString());
        if (!rest.exists()) {
            rest.mkdirs();
        }
        StringBuilder restSrcPkgPath = ComponentCreator.componentSrcPkg(component, restPath)
                .append(File.separator)
                .append("rest");
        File restSrcPkg = new File(restSrcPkgPath.toString());
        if (!restSrcPkg.exists()) {
            restSrcPkg.mkdirs();
        }
    }

    public static void createRestModule(ComponentDefinition component) throws Exception{
        createControllerPath(component);
        MemoryFile restPom = ComponentVmUtil.createRestPom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(restPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restPom.content);
    }

    public static String getControllerClassFullName(Project project) throws Exception{
        ComponentDefinition component = ComponentCreator.loadComponent(project);
        String packageName = EntityUtil.getComponentPackageName(component);
        return packageName+".rest."+component.getSimpleName()+"Controller";
    }
}
