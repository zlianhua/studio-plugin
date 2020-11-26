package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.dialog.CreateRestProxyDialog;
import com.ai.abc.studio.plugin.util.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class CreateRestProxyAction extends AnAction {
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
            CreateRestProxyDialog dialog = new CreateRestProxyDialog();
            if(dialog.showAndGet()){
                project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = ComponentCreator.loadComponent(project);
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                String mainFileName = psiFile.getName().replaceAll(".java","");
                PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());
                String mainClassName = EntityCreator.getEntityClassFullName(project,mainFileName).replace(".model.",".api.");;
                PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
                JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                PsiMethod[] methods = mainPsiClass.getMethods();
                boolean isGet =  mainFileName.endsWith("Query")?true:false;
                String serviceName = StringUtils.uncapitalize(mainFileName);
                String tmpRootEntitySimpleName = StringUtils.replace(mainFileName,"Query","");
                tmpRootEntitySimpleName = StringUtils.replace(tmpRootEntitySimpleName,"Command","");
                String rootEntitySimpleName = tmpRootEntitySimpleName;
                if(dialog.getToRestControllerCheckBox().isSelected()){
                    String controllerClsName = RestControllerCreator.getControllerClassFullName(project);
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PsiClass controllerClass = JavaPsiFacade.getInstance(project).findClass(controllerClsName, GlobalSearchScope.projectScope(project));
                                if (null == controllerClass) {
                                    controllerClass = RestControllerCreator.createRestController(project, component,component.getSimpleName()+"Controller");
                                }
                                PsiField serviceField = PsJavaFileHelper.findField(controllerClass, serviceName);
                                if (null == serviceField) {
                                    PsiType serviceType = new PsiJavaParserFacadeImpl(project).createTypeFromText(mainFileName, null);
                                    List<String> serviceAutowire = new ArrayList<>();
                                    serviceAutowire.add("@Autowired");
                                    PsJavaFileHelper.addField(controllerClass, serviceName, null,serviceType, serviceAutowire);
                                }
                                RestControllerCreator.addMethodToControllerClass(project, rootEntitySimpleName, serviceName, controllerClass, methods, elementFactory, codeStyleManager, isGet);
                                CodeStyleManager.getInstance(project).reformat(controllerClass);
                            } catch (Exception exception) {
                                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"创建RestController出现错误");
                                exception.printStackTrace();
                            }
                        }
                    });
                }
                if(dialog.getToRestProxyCheckBox().isSelected()){
                    String restProxyClsName = EntityCreator.getEntityClassFullName(project, StringUtils.replace(mainFileName+"RestProxy","Impl","")).replace(".model.",".rest.proxy.");
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PsiClass proxyClass = JavaPsiFacade.getInstance(project).findClass(restProxyClsName, GlobalSearchScope.projectScope(project));
                                if(null==proxyClass){
                                    proxyClass = RestProxyCreator.createRestProxy(project,component,StringUtils.replace(mainFileName+"RestProxy","Impl",""));
                                }else{
                                    PsiClass restConfigPsiClass = PsJavaFileHelper.getEntity(psiPackage,component.getSimpleName()+"RestConfiguration");
                                    if(null==restConfigPsiClass){
                                        RestProxyCreator.createRestConfig(project,component);
                                    }
                                }
                                RestProxyCreator.addMethodToRestProxyClass(component,rootEntitySimpleName,proxyClass,methods,elementFactory,codeStyleManager,isGet);
                                CodeStyleManager.getInstance(project).reformat(proxyClass);
                            } catch (Exception exception) {
                                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"创建RestProxy出现错误");
                                exception.printStackTrace();
                            }
                        }
                    });
                }
                if(dialog.getToBmgProxyCheckBox().isSelected()) {
                    //TODO
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }
            }
        } catch (Exception exception) {
            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"创建RestProxy出现错误");
            exception.printStackTrace();
        }
    }
}
