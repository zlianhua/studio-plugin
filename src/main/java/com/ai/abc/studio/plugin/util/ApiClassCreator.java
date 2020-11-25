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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApiClassCreator {
    public static PsiClass createApiClass(Project project, ComponentDefinition component, String className){
        Path apiPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getApiPath(component));
        VirtualFile apiVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(apiPath);
        if(null==apiVirtualFile){
            createApiPath(component);
            apiVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(apiPath);
        }
        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(apiVirtualFile));
        PsiClass apiClass = PsJavaFileHelper.getEntity(psiPackage,className);
        if(null!=apiClass){
            return null;
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        apiClass = PsJavaFileHelper.createInterface(project,apiVirtualFile,className,packageImports,null,null,null);
        return apiClass;
    }

    public static void addMethodToInterfaceClass(PsiClass psiClass, PsiMethod[] methods, PsiElementFactory elementFactory, JavaCodeStyleManager codeStyleManager){
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

                if(null!=throwEles && throwEles.length>0){
                    methodStr.append(" throws ");
                    for(PsiJavaCodeReferenceElement element : throwEles){
                        methodStr.append(element.getReferenceName())
                                .append(",");
                    }
                }
                if(methodStr.toString().endsWith(",")){
                    methodStr.deleteCharAt(methodStr.lastIndexOf(","));
                }
            }
            methodStr.append(";");
            psiClass.add(elementFactory.createMethodFromText(methodStr.toString(),psiClass));
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        codeStyleManager.shortenClassReferences(file);
    }

    public static void createApiPath(ComponentDefinition component){
        StringBuilder apiPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-api".toLowerCase());
        File apiPathFile = new File(apiPath.toString());
        if (!apiPathFile.exists()) {
            apiPathFile.mkdirs();
        }
        StringBuilder apiSrcPkgPath = ComponentCreator.componentSrcPkg(component,apiPath)
                .append(File.separator)
                .append("api");
        File apiSrcPkg = new File(apiSrcPkgPath.toString());
        if(!apiSrcPkg.exists()){
            apiSrcPkg.mkdirs();
        }
    }

    public static void createApiModule(ComponentDefinition component) throws Exception{
        createApiPath(component);
        MemoryFile apiPom = ComponentVmUtil.createApiPom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(apiPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,apiPom.content);
    }

    public static void createApiClasses(Project project, ComponentDefinition component,String rootEntityName){
        String commandApiName = rootEntityName+"Service";
        String queryApiName = rootEntityName+"QueryService";
        createApiClass(project,component,commandApiName);
        createApiClass(project,component,queryApiName);
    }
}
