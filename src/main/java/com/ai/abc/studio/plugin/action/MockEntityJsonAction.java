package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class MockEntityJsonAction extends AnAction {
    private Project project;
    private ObjectMapper mapper = new ObjectMapper();
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        project = e.getData(PlatformDataKeys.PROJECT);
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
                    if(PsJavaFileHelper.isRootEntity(cls)){
                        enable =true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        try {
            String mainFileName = psiFile.getName().replaceAll(".java","");
            String mainClassName = FileCreateHelper.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
            PsiClass model = JavaPsiFacade.getInstance(project).findClass(mainClassName, GlobalSearchScope.projectScope(project));
            JSONObject jsonModel = FileCreateHelper.generatePsiClassJson(project,model,null);
            FileCreateHelper.saveEntityMockedJson(FileCreateHelper.loadComponent(project),model.getName(),jsonModel.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
