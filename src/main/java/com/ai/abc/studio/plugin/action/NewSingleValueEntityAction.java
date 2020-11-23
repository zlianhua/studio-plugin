package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NewSingleValueEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts=project.getBasePath();
        String modelPackageEnds=project.getName().toLowerCase()+"/model/";
        boolean enable = false;
        if((classPath.contains(modelPackageStarts))&&classPath.contains(modelPackageEnds)&&virtualFile.getFileType().getName().equalsIgnoreCase("java")){
            enable =true;
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setSelected(true);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(true);
        if (newSingleEntityDialog.showAndGet()) {
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = FileCreateHelper.loadComponent(project);
                String simpleEntityName = newSingleEntityDialog.getNameTextField().getText();
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                String mainFileName = psiFile.getName().replaceAll(".java","");
                String mainClassName = FileCreateHelper.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
                PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());
                PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        String refFieldName = StringUtils.uncapitalize(simpleEntityName);
                        PsiType fieldType;
                        if(newSingleEntityDialog.getIsOneToManyCheckBox().isSelected()){
                            refFieldName = refFieldName+"List";
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText("List<"+simpleEntityName+">",null);
                        }else{
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText(simpleEntityName,null);
                        }
                        PsJavaFileHelper.deleteField(mainPsiClass,refFieldName);
                        List<String> annotations = new ArrayList<>();
                        annotations.add("@Convert(converter = EntityToJsonConverter.class)");
                        annotations.add("@Lob");
                        PsJavaFileHelper.addField(mainPsiClass,refFieldName,null,fieldType,annotations);
                        PsiClass valueEntity = EntityCreator.createEntity(project, component, simpleEntityName, "", EntityCreator.EntityType.ValueEntity);
                        if (newSingleEntityDialog.getIsAbstractCheckBox().isSelected()) {
                            if (!valueEntity.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT)) {
                                valueEntity.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
                            }
                        }new OpenFileDescriptor(project, valueEntity.getContainingFile().getVirtualFile()).navigate(true);
                    }
                });
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
