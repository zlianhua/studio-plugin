package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thymeleaf.util.StringUtils;

import javax.swing.*;
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
    public static PsiClass createRepository(Project project, ComponentDefinition component, String rootEntityName, AnActionEvent e)  throws Exception{
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path repositoryPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getRepositoryPath(component));
        VirtualFile repositoryVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(repositoryPath);
        if(null==repositoryVirtualFile){
            createRepositoryPath(component);
            repositoryVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(repositoryPath);
        }
        PsiClass repositoryClass = null;
        PsiFile[] files = FilenameIndex.getFilesByName(project,rootEntityName+"Repository.java", ProjectScope.getProjectScope(project));
        if(null!=files && files.length>0) {
            JComponent source = PsJavaFileHelper.getDialogSource(e);
            if (Messages.showConfirmationDialog(source,
                    rootEntityName + "Repository" + "已经存在，是否覆盖已有对象？",
                    "Repository已经存在",
                    "覆盖",
                    "取消") == Messages.NO) {
                return null;
            }else{
                //delete
                files[0].delete();
            }
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add("java.util");

        List<String> classImports = new ArrayList<>();
        classImports.add(Serializable.class.getName());
        classImports.add(JpaRepository.class.getName());
        classImports.add(Timestamp.class.getName());

        String parentClassName = "JpaRepository<"+rootEntityName+", Serializable>";
        repositoryClass = PsJavaFileHelper.createInterface(project,repositoryVirtualFile,rootEntityName+"Repository",packageImports,classImports,null,parentClassName);

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

    public static void createRepositoryPath(ComponentDefinition component) throws Exception{
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
