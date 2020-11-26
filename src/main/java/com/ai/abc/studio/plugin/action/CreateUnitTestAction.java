package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.ai.abc.studio.plugin.util.UnitTestCreator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
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
public class CreateUnitTestAction extends AnAction {
    private Project project;
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        boolean enable = false;
        if(null!=psiFile){
            String classPath = virtualFile.getPath();
            String apiPackageStarts=project.getBasePath();
            String apiPackageEnds=project.getName().toLowerCase()+"/api/";
            if((classPath.contains(apiPackageStarts))&&classPath.contains(apiPackageEnds) && virtualFile.getFileType().getName().equalsIgnoreCase("java")){
                enable =true;
            }
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            project = e.getData(PlatformDataKeys.PROJECT);
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
            String mainFileName = psiFile.getName().replaceAll(".java","");
            PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());
            String mainClassName = EntityCreator.getEntityClassFullName(project,mainFileName).replace(".model.",".api.");;
            PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    try {
                        UnitTestCreator.createTestApp(project,component);
                        UnitTestCreator.createTestAppPropFile(project, component);
                        UnitTestCreator.createUnitTest(project, component, mainPsiClass,e);
                    } catch (Exception exception) {
                        Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"创建单元测试出现错误");
                        exception.printStackTrace();
                    }
                }
            });
        }catch (Exception e1){
            Messages.showErrorDialog(ExceptionUtil.getMessage(e1),"创建单元测试出现错误");
            e1.printStackTrace();
        }
    }

}
