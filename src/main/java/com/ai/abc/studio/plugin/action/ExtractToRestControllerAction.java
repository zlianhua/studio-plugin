package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExtractToRestControllerAction extends AnAction {
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
            ComponentDefinition component = FileCreateHelper.loadComponent(project);
            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
            PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
            String mainFileName = psiFile.getName().replaceAll(".java","");
            String tmpRootEntitySimpleName = StringUtils.replace(mainFileName,"QueryServiceImpl","");
            tmpRootEntitySimpleName = StringUtils.replace(tmpRootEntitySimpleName,"ServiceImpl","");
            String rootEntitySimpleName = tmpRootEntitySimpleName;
            String intfaceName = mainFileName.replace("Impl","");
            boolean isGet =  intfaceName.endsWith("QueryService")?true:false;
            String serviceName = StringUtils.uncapitalize(intfaceName);
            String mainClassName = FileCreateHelper.getEntityClassFullName(project,mainFileName).replace(".model.",".service.");
            PsiClass mainPsiClass = JavaPsiFacade.getInstance(project).findClass(mainClassName, GlobalSearchScope.projectScope(project));
            JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
            PsiMethod[] methods = mainPsiClass.getMethods();
            String controllerClsName = FileCreateHelper.getControllerClassFullName(project);
            String restProxyClsName = FileCreateHelper.getEntityClassFullName(project, StringUtils.replace(mainFileName+"RestProxy","ServiceImpl","")).replace(".model.",".rest.proxy.");
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    PsiClass controllerClass = JavaPsiFacade.getInstance(project).findClass(controllerClsName, GlobalSearchScope.projectScope(project));
                    try {
                        if(null==controllerClass){
                            controllerClass = PsJavaFileHelper.createRestController(project,component);
                        }
                        PsiField serviceField = PsJavaFileHelper.findField(controllerClass,serviceName);
                        if(null==serviceField){
                            PsiType serviceType = new PsiJavaParserFacadeImpl(project).createTypeFromText(intfaceName,null);
                            List<String> serviceAutowire = new ArrayList<>();
                            serviceAutowire.add("@Autowired");
                            PsJavaFileHelper.addFieldWithAnnotations(controllerClass,serviceName,serviceType,serviceAutowire);
                        }
                        PsJavaFileHelper.addMethodToControllerClass(project,rootEntitySimpleName,serviceName,controllerClass,methods,elementFactory,codeStyleManager,isGet);
                        CodeStyleManager.getInstance(project).reformat(controllerClass);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    PsiClass proxyClass = JavaPsiFacade.getInstance(project).findClass(restProxyClsName, GlobalSearchScope.projectScope(project));
                    try {
                        if(null==proxyClass){
                            proxyClass = PsJavaFileHelper.createRestProxy(project,component,StringUtils.replace(mainFileName+"RestProxy","ServiceImpl",""));
                        }
                        PsJavaFileHelper.addMethodToRestProxyClass(component,rootEntitySimpleName,proxyClass,methods,elementFactory,codeStyleManager,isGet);
                        CodeStyleManager.getInstance(project).reformat(proxyClass);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
