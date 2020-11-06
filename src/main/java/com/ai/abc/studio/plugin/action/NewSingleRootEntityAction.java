package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManagerImpl;
import com.intellij.util.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewSingleRootEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts=project.getBasePath();
        String modelPackaheEnds=project.getName().toLowerCase()+"/model";
        if((classPath.contains(modelPackageStarts))&&classPath.endsWith(modelPackaheEnds)){
            e.getPresentation().setEnabledAndVisible(true);
        }else{
            e.getPresentation().setEnabledAndVisible(false);
        }
        //virtualFile.getFileType().getName().equalsIgnoreCase("java")
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
                EntityDefinition entity = new EntityDefinition();
                String entitySimpleName = newSingleEntityDialog.getNameTextField().getText();
                entity.setSimpleName(entitySimpleName);
                ComponentDefinition component = FileCreateHelper.loadComponent(project);
                entity.setName(component.getName().toLowerCase() +".model."+entitySimpleName);
                entity.setDescription(newSingleEntityDialog.getDescTextField().getText());
                entity.setRoot(newSingleEntityDialog.getIsRootCheckBox().isSelected());
                entity.setValueObject(newSingleEntityDialog.getIsValueCheckBox().isSelected());
                entity.setAbstract(newSingleEntityDialog.getIsAbstractCheckBox().isSelected());
                String fileName = FileCreateHelper.createEntityCode(project,entity);
                Path path = Paths.get(fileName);
                VirtualFile virtualFile = VfsUtil.findFile(path,true);
                new OpenFileDescriptor(project, virtualFile).navigate(true);

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
