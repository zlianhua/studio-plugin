package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

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
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(controllerClsName, GlobalSearchScope.projectScope(project));
            String restProxyClsName = FileCreateHelper.getEntityClassFullName(project, StringUtils.replace(mainFileName+"RestProxy","ServiceImpl","")).replace(".model.",".rest.proxy.");
            PsiClass restProxyPsiClass = JavaPsiFacade.getInstance(project).findClass(restProxyClsName, GlobalSearchScope.projectScope(project));
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    PsiField serviceField = PsJavaFileHelper.findField(psiClass,serviceName);
                    if(null==serviceField){
                        PsiType serviceType = new PsiJavaParserFacadeImpl(project).createTypeFromText(intfaceName,null);
                        List<String> serviceAutowire = new ArrayList<>();
                        serviceAutowire.add("@Autowired");
                        PsJavaFileHelper.addFieldWithAnnotations(psiClass,serviceName,serviceType,serviceAutowire);
                    }
                    PsJavaFileHelper.addMethodToControllerClass(project,rootEntitySimpleName,serviceName,psiClass,methods,elementFactory,codeStyleManager,isGet);
                    CodeStyleManager.getInstance(project).reformat(psiClass);
                    PsJavaFileHelper.addMethodToRestProxyClass(component,rootEntitySimpleName,restProxyPsiClass,methods,elementFactory,codeStyleManager,isGet);
                    CodeStyleManager.getInstance(project).reformat(restProxyPsiClass);
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}