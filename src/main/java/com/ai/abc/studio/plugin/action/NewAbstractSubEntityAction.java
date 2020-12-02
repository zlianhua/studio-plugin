package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class NewAbstractSubEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        boolean enable = false;
        if(null!=psiFile){
            String classPath = virtualFile.getPath();
            PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());
            String modelPackageStarts=project.getBasePath();
            String modelPackageEnds=project.getName().toLowerCase()+"/model/";
            if((classPath.contains(modelPackageStarts))&&classPath.contains(modelPackageEnds) && virtualFile.getFileType().getName().equalsIgnoreCase("java")){
                try {
                    String fileName = psiFile.getName().replaceAll(".java","");
                    PsiClass cls = PsJavaFileHelper.getEntity(psiPackage,fileName);
                    if(!PsJavaFileHelper.isValueEntity(cls)){
                        try {
                            String mainFileName = psiFile.getName().replaceAll(".java","");
                            String mainClassName = EntityCreator.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
                            PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
                            if(mainPsiClass.getModifierList().hasModifierProperty("abstract")){
                                enable = true;
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                } catch (Exception exception) {
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setSelected(false);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setSelected(false);
        if (newSingleEntityDialog.showAndGet()) {
            String simpleEntityName = newSingleEntityDialog.getNameTextField().getText();
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = ComponentCreator.loadComponent(project);
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String desc = newSingleEntityDialog.getDescTextField().getText();
                            if(null==desc){
                                desc = simpleEntityName;
                            }
                            String superClass = psiFile.getName().replaceAll(".java","");
                            PsiClass memberEntity = EntityCreator.createEntity(project,component,simpleEntityName,"", null,desc,true,superClass);
                            PsiJavaFile memberEntityFile = (PsiJavaFile)memberEntity.getContainingFile();
                            new OpenFileDescriptor(project, memberEntityFile.getVirtualFile()).navigate(true);
                        } catch (Exception exception) {
                            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"新建实体成员出现错误");
                            exception.printStackTrace();
                        }
                    }
                });
            } catch (Exception exception) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"新建实体成员出现错误");
                exception.printStackTrace();
            }
        }
    }
}
