package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.dialog.CreateCommandMethodDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.plugin.util.ServiceCreator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class CreateCommandMethodAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String servicePackageStarts = project.getBasePath();
        String servicePackageEnds = project.getName().toLowerCase() + "/service/";
        boolean enable = false;
        if ((classPath.contains(servicePackageStarts)) && classPath.contains(servicePackageEnds) && virtualFile.getFileType().getName().equalsIgnoreCase("java")
        && classPath.contains("Command")) {
            enable = true;
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiElement element = PsiUtilBase.getElementAtCaret(editor);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            List<PsiClass> rootEntities = EntityCreator.findRootEntities(project,component);
            if(null!=rootEntities && rootEntities.size()>0){
                String[] rootEntityNames = new String[rootEntities.size()];
                int count=0;
                for(PsiClass rootEntity : rootEntities){
                    rootEntityNames[count] = rootEntity.getName();
                    count++;
                }
                CreateCommandMethodDialog dialog = new CreateCommandMethodDialog(rootEntityNames);
                if (dialog.showAndGet()) {
                    String rootEntityName = (String)dialog.getRootEntityNameCBX().getSelectedItem();
                    String methodName = dialog.getMethodName().getText();
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ServiceCreator.createCommandMethod(project,component,psiClass,rootEntityName,methodName,e);
                            } catch (Exception exception) {
                                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"生成命令方法出现错误");
                                exception.printStackTrace();
                            }
                        }
                    });
                }
            }
        }catch (Exception exception) {
            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"生成命令方法出现错误");
            exception.printStackTrace();
        }
    }
}
