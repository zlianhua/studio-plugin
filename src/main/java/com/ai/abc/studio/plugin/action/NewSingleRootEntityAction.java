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
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class NewSingleRootEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts=project.getBasePath();
        String modelPackageEnds=project.getName().toLowerCase()+"/model";
        if((classPath.contains(modelPackageStarts))&&classPath.endsWith(modelPackageEnds)){
            e.getPresentation().setEnabledAndVisible(true);
        }else{
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setSelected(true);
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(false);
        if (newSingleEntityDialog.showAndGet()) {
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = ComponentCreator.loadComponent(project);
                String packageName = EntityUtil.getComponentPackageName(component)+".model.";
                String entitySimpleName = newSingleEntityDialog.getNameTextField().getText();
                PsiClass mainPsiClass = JavaPsiFacade.getInstance(project).findClass(packageName+entitySimpleName, GlobalSearchScope.projectScope(project));
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
                        PsiClass rootEntity = EntityCreator.createEntity(project, component, entitySimpleName, "", EntityCreator.EntityType.RootEntity);
                        if (newSingleEntityDialog.getIsAbstractCheckBox().isSelected()) {
                            if (!rootEntity.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT)) {
                                rootEntity.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
                            }
                        }
                        ApiClassCreator.createApiClasses(project,component,entitySimpleName);
                        ServiceCreator.createService(project,component,entitySimpleName);
                        new OpenFileDescriptor(project, rootEntity.getContainingFile().getVirtualFile()).navigate(true);
                    }
                });
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
