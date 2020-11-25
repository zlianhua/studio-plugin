package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ServiceCreator {
    public static void createServicePath(ComponentDefinition component){
        StringBuilder servicePath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase());
        File service = new File(servicePath.toString());
        if (!service.exists()) {
            service.mkdirs();
        }
        StringBuilder serviceSrcPkgPath = ComponentCreator.componentSrcPkg(component, servicePath)
                .append(File.separator)
                .append("service");
        File serviceSrcPkg = new File(serviceSrcPkgPath.toString());
        if (!serviceSrcPkg.exists()) {
            serviceSrcPkg.mkdirs();
        }
    }
    public static void createServiceModule(ComponentDefinition component) throws Exception{
        createServicePath(component);
        MemoryFile servicePom = ComponentVmUtil.createServicePom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(servicePom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,servicePom.content);
    }

    public static void createService(Project project,ComponentDefinition component,String rootEntityName){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path servicePath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getServicePath(component));
        VirtualFile serviceVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(servicePath);
        if(null==serviceVirtualFile){
            createServicePath(component);
            serviceVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(servicePath);
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");

        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Service");

        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(serviceVirtualFile));
        PsiClass command = PsJavaFileHelper.getEntity(psiPackage,rootEntityName+"ServiceImpl");
        if(null==command){
            command = PsJavaFileHelper.createPsiClass(project,serviceVirtualFile,rootEntityName+"ServiceImpl",packageImports,classImports,classAnnotations,null);
            command.getImplementsList().add(elementFactory.createReferenceFromText(rootEntityName+"Service",command));
        }
        PsiClass query = PsJavaFileHelper.getEntity(psiPackage,rootEntityName+"QueryServiceImpl");
        if(null==query){
            query = PsJavaFileHelper.createPsiClass(project,serviceVirtualFile,rootEntityName+"QueryServiceImpl",packageImports,classImports,classAnnotations,null);
            query.getImplementsList().add(elementFactory.createReferenceFromText(rootEntityName+"QueryService",query));
        }
    }
}
