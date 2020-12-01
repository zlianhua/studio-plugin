package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.util.*;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class NewChildrenRootEntityAction extends AnAction {
    private Project project;
    @Override
    public void update(@NotNull AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = null;
        boolean enable = false;
        try {
            virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        } catch (Exception exception) {
            e.getPresentation().setEnabledAndVisible(enable);
            return;
        }
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts=project.getBasePath();
        String modelPackageEnds=project.getName().toLowerCase()+"/model";
        if((classPath.contains(modelPackageStarts))&&classPath.endsWith(modelPackageEnds)){
            try {
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                String mainFileName = psiFile.getName().replaceAll(".java","");
                String mainClassName = EntityCreator.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
                PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());
                PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
                if(mainPsiClass.getModifierList().hasModifierProperty("abstract")){
                    enable = true;
                }
            } catch (Exception exception) {
            }
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setSelected(true);
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(false);
        IdeFocusManager.getInstance(project).requestFocus(newSingleEntityDialog.getPreferredFocusedComponent(),true);
        if (newSingleEntityDialog.showAndGet()) {
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = ComponentCreator.loadComponent(project);
                String packageName = EntityUtil.getComponentPackageName(component)+".model.";
                String entitySimpleName = newSingleEntityDialog.getNameTextField().getText();
                PsiClass mainPsiClass = PsJavaFileHelper.findClass(project,packageName+entitySimpleName);
                if(null!=mainPsiClass){
                    JComponent source = PsJavaFileHelper.getDialogSource(e);
                    if (Messages.showConfirmationDialog(source,
                            "根对象"+entitySimpleName+"已经存在，是否覆盖已有对象？",
                            "根对象"+entitySimpleName+"已经存在",
                            "覆盖",
                            "取消"
                            )==Messages.NO){
                        return;
                    }
                }

                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String desc = newSingleEntityDialog.getDescTextField().getText();
                            if(null==desc){
                                desc = entitySimpleName;
                            }
                            PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                            String superClass = psiFile.getName().replaceAll(".java","");
                            PsiClass rootEntity = EntityCreator.createEntity(project, component, entitySimpleName, "", EntityCreator.EntityType.RootEntity,desc,false,superClass);
                            new OpenFileDescriptor(project, rootEntity.getContainingFile().getVirtualFile()).navigate(true);
                            RepositoryCreator.createRepository(project,component,entitySimpleName,e);
                            ApiClassCreator.createApiClasses(project,component,entitySimpleName);
                            ServiceCreator.createService(project,component,entitySimpleName);
                        } catch (Exception exception) {
                            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"生成根对象出现错误");
                            exception.printStackTrace();
                        }
                    }
                });
            } catch (Exception exception) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"生成根对象出现错误");
                exception.printStackTrace();
            }
        }
    }
}
