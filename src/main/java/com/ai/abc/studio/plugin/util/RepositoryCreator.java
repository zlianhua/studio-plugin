package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class RepositoryCreator {
    public static PsiClass createRepository(Project project, ComponentDefinition component,String rootEntityName) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path repositoryPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getRepositoryPath(component));
        VirtualFile repositoryVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(repositoryPath);
        if(null==repositoryVirtualFile){
            createRepositoryPath(component);
            repositoryVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(repositoryPath);
        }
        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(repositoryVirtualFile));
        PsiClass repositoryClass = PsJavaFileHelper.getEntity(psiPackage,rootEntityName+"Repository");
        if(null==repositoryClass){
            List<String> packageImports = new ArrayList<>();
            packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
            packageImports.add("java.util");

            List<String> classImports = new ArrayList<>();
            classImports.add(Serializable.class.getName());
            classImports.add(JpaRepository.class.getName());
            classImports.add(Timestamp.class.getName());

            String parentClassName = "JpaRepository<"+rootEntityName+", Serializable>";
            repositoryClass = PsJavaFileHelper.createInterface(project,repositoryVirtualFile,rootEntityName+"Repository",packageImports,classImports,null,parentClassName);
        }

        PsiField primaryField = EntityCreator.findPrimaryField(project,rootEntityName,component);
        if(null!=primaryField && component.isLogicalDelete()){
            StringBuilder methodStr = new StringBuilder();
            methodStr.append("Optional<")
                    .append(rootEntityName).append(">")
                    .append(" findBy")
                    .append(StringUtils.capitalize(primaryField.getName()))
                    .append("AndDeletedFalse(Serializable id);\n");
            boolean hasMethod=false;
            PsiMethod method = elementFactory.createMethodFromText(methodStr.toString(),repositoryClass);
            for(PsiMethod existMethod : repositoryClass.getMethods()){
                if(existMethod.getReturnType().getPresentableText().equals(method.getReturnType().getPresentableText())
                && existMethod.getName().equals(method.getName())){
                    hasMethod=true;
                }
            }
            if(!hasMethod){
                repositoryClass.add(method);
            }
        }
        return repositoryClass;
    }

    public static void createRepositoryPath(ComponentDefinition component) {
        StringBuilder servicePath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service");
        File service = new File(servicePath.toString());
        if (!service.exists()) {
            service.mkdirs();
        }
        StringBuilder repositorySrcPkgPath = ComponentCreator.componentSrcPkg(component, servicePath)
                .append(File.separator)
                .append("service")
                .append(File.separator)
                .append("repository");
        File repositorySrcPkg = new File(repositorySrcPkgPath.toString());
        if (!repositorySrcPkg.exists()) {
            repositorySrcPkg.mkdirs();
        }
    }
}
