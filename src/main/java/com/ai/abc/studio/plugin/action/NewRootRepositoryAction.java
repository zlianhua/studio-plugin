package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NewRootRepositoryAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        String mainFileName = psiFile.getName();
        mainFileName = StringUtils.replace(mainFileName,".java","");
        try {
            if(FileCreateHelper.isRootEntity(project,mainFileName)){
                e.getPresentation().setEnabledAndVisible(true);
            }else{
                e.getPresentation().setEnabledAndVisible(false);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
            String mainFileName = psiFile.getName();
            mainFileName = StringUtils.replace(mainFileName,".java","");
            String fileName = FileCreateHelper.createRepositoryCode(project,mainFileName);
            Path path = Paths.get(fileName);
            VirtualFile virtualFile = VfsUtil.findFile(path,true);
            new OpenFileDescriptor(project, virtualFile).navigate(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
