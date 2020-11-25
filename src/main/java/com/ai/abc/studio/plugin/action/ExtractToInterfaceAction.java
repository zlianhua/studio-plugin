package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.ApiClassCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class ExtractToInterfaceAction extends AnAction {
    private Project project;
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        boolean enable = false;
        if(null!=psiFile){
            String classPath = virtualFile.getPath();
            String modelPackageStarts=project.getBasePath();
            String modelPackageEnds=project.getName().toLowerCase()+"/service/";
            if((classPath.contains(modelPackageStarts))&&classPath.contains(modelPackageEnds) && virtualFile.getFileType().getName().equalsIgnoreCase("java")){
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
            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
            PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
            String mainFileName = psiFile.getName().replaceAll(".java", "");
            String mainClassName = EntityCreator.getEntityClassFullName(project, mainFileName).replace(".model.", ".service.");
            PsiClass mainPsiClass = JavaPsiFacade.getInstance(project).findClass(mainClassName, GlobalSearchScope.projectScope(project));
            JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
            PsiMethod[] methods = mainPsiClass.getMethods();
            String intfaceName = mainFileName.replace("Impl", "");
            //addToInterface
            String interfaceClsName = EntityCreator.getEntityClassFullName(project, StringUtils.replace(mainFileName, "Impl", "")).replace(".model.", ".api.");
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    PsiClass intfPsiClass = JavaPsiFacade.getInstance(project).findClass(interfaceClsName, GlobalSearchScope.projectScope(project));
                    if (null == intfPsiClass) {
                        intfPsiClass = ApiClassCreator.createApiClass(project, component, intfaceName);
                    }
                    ApiClassCreator.addMethodToInterfaceClass(intfPsiClass, methods, elementFactory, codeStyleManager);
                }
            });
        }catch(Exception e1){
            e1.printStackTrace();
        }
    }
}
