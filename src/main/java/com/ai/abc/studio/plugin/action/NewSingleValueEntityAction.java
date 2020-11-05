package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
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
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enable = false;
        if(virtualFile.getFileType().getName().equalsIgnoreCase("java")){
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
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        String refFieldName = StringUtils.uncapitalize(entity.getSimpleName());
                        PsiType fieldType;
                        if(newSingleEntityDialog.getIsOneToManyCheckBox().isSelected()){
                            refFieldName = refFieldName+"List";
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText("List<"+entity.getSimpleName()+">",null);
                        }else{
                            fieldType = new PsiJavaParserFacadeImpl(mainPsiClass.getProject()).createTypeFromText(entity.getSimpleName(),null);
                        }
                        PsJavaFileHelper.deleteField(mainPsiClass,refFieldName);
                        List<String> annotations = new ArrayList<>();
                        annotations.add("@Convert(converter = com.ai.abc.jpa.model.EntityToJsonConverter.class)");
                        annotations.add("@Lob");
                        PsJavaFileHelper.addFieldWithAnnotations(mainPsiClass,refFieldName,fieldType,annotations);
                    }
                });
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
