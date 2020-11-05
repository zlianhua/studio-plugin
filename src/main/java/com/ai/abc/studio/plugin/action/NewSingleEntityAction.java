package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.EntityAttributeDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NewSingleEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
        boolean enable = false;
        if(virtualFile.getFileType().getName().equalsIgnoreCase("java")){
            try {
                String mainFileName = psiFile.getName().replaceAll(".java","");
                EntityDefinition entity = FileCreateHelper.getEntity(project,mainFileName);
                if(!entity.isValueObject()){
                    enable =true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setSelected(false);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(true);
        newSingleEntityDialog.getIsOneToManyCheckBox().setSelected(true);
        if (newSingleEntityDialog.showAndGet()) {
            EntityDefinition entity = new EntityDefinition();
            entity.setSimpleName(newSingleEntityDialog.getNameTextField().getText());
            entity.setDescription(newSingleEntityDialog.getDescTextField().getText());
            entity.setRoot(newSingleEntityDialog.getIsRootCheckBox().isSelected());
            entity.setValueObject(newSingleEntityDialog.getIsValueCheckBox().isSelected());
            entity.setAbstract(newSingleEntityDialog.getIsAbstractCheckBox().isSelected());
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                String mainFileName = psiFile.getName().replaceAll(".java","");
                String mainClassName = FileCreateHelper.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
                PsiClass mainPsiClass = JavaPsiFacade.getInstance(project).findClass(mainClassName, GlobalSearchScope.projectScope(project));
                JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        String refFieldName = StringUtils.uncapitalize(entity.getSimpleName());
                        PsiType fieldType;
                        List<String> annotations = new ArrayList<>();
                        if(newSingleEntityDialog.getIsOneToManyCheckBox().isSelected()){
                            annotations.add("@OneToMany(cascade= CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)");
                            refFieldName = refFieldName+"List";
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText("List<"+entity.getSimpleName()+">",null);
                        }else{
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText(entity.getSimpleName(),null);
                        }
                        String memFileName = CamelCaseStringUtil.camelCase2UnderScore(entity.getSimpleName());
                        annotations.add("@JoinColumn(name=\""+memFileName.toUpperCase()+"_ID\")");
                        PsJavaFileHelper.deleteField(mainPsiClass,refFieldName);
                        PsJavaFileHelper.addFieldWithAnnotations(mainPsiClass,refFieldName,fieldType,annotations);
                        PsiJavaFile file = (PsiJavaFile)mainPsiClass.getContainingFile();
                        PsiClass listClass = JavaPsiFacade.getInstance(project).findClass("java.util.List", GlobalSearchScope.allScope(project));
                        PsiImportStatement importStatement = elementFactory.createImportStatement(listClass);
                        file.getImportList().add(importStatement);
                        codeStyleManager.shortenClassReferences(file);
                    }
                });
                EntityAttributeDefinition relMainAttr = new EntityAttributeDefinition();
                relMainAttr.setName(StringUtils.uncapitalize(mainFileName));
                relMainAttr.setDataType(mainClassName);
                relMainAttr.getAnnotations().add("@ManyToOne");
                mainFileName = CamelCaseStringUtil.camelCase2UnderScore(mainFileName);
                relMainAttr.getAnnotations().add("@JoinColumn(name =\""+mainFileName.toUpperCase()+"_ID\", insertable = false, updatable = false)");
                relMainAttr.getAnnotations().add("@JsonBackReference");
                entity.getAttributes().add(relMainAttr);
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
