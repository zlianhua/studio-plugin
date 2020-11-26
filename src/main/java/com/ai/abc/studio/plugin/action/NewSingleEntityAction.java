package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.dialog.NewSingleEntityDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
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
public class NewSingleEntityAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getData(PlatformDataKeys.PROJECT);
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
                    if(!PsJavaFileHelper.isValueEntity(cls)){
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
        NewSingleEntityDialog newSingleEntityDialog = new NewSingleEntityDialog();
        newSingleEntityDialog.getIsRootCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsValueCheckBox().setSelected(false);
        newSingleEntityDialog.getIsAbstractCheckBox().setEnabled(false);
        newSingleEntityDialog.getIsOneToManyCheckBox().setEnabled(true);
        newSingleEntityDialog.getIsOneToManyCheckBox().setSelected(true);
        if (newSingleEntityDialog.showAndGet()) {
            String simpleEntityName = newSingleEntityDialog.getNameTextField().getText();
            try {
                Project project = e.getData(PlatformDataKeys.PROJECT);
                ComponentDefinition component = ComponentCreator.loadComponent(project);
                PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
                String mainFileName = psiFile.getName().replaceAll(".java","");
                String mainClassName = EntityCreator.getEntityClassFullName(project,mainFileName).replaceAll(".java","");
                PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(psiFile.getParent());

                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PsiClass mainPsiClass = PsJavaFileHelper.getEntity(psiPackage,mainClassName);
//                        PsiClass mainPsiClass = JavaPsiFacade.getInstance(project).findClass(mainClassName, GlobalSearchScope.projectScope(project));
                            JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                            String refFieldName = StringUtils.uncapitalize(simpleEntityName);
                            PsiType fieldType;
                            List<String> annotations = new ArrayList<>();
                            if(newSingleEntityDialog.getIsOneToManyCheckBox().isSelected()){
                                annotations.add("@OneToMany(cascade= CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)");
                                refFieldName = refFieldName+"List";
                                fieldType = new PsiJavaParserFacadeImpl(project).createTypeFromText("List<"+simpleEntityName+">",null);
                            }else{
                                fieldType = new PsiJavaParserFacadeImpl(project).createTypeFromText(simpleEntityName,null);
                            }
                            String memFileName = CamelCaseStringUtil.camelCase2UnderScore(simpleEntityName);
                            annotations.add("@JoinColumn(name=\""+memFileName.toUpperCase()+"_ID\")");
                            PsJavaFileHelper.deleteField(mainPsiClass,refFieldName);
                            PsJavaFileHelper.addField(mainPsiClass,refFieldName,null,fieldType,annotations);
                            PsiJavaFile file = (PsiJavaFile)mainPsiClass.getContainingFile();
                            PsiClass listClass = JavaPsiFacade.getInstance(project).findClass("java.util.List", GlobalSearchScope.allScope(project));
                            PsiImportStatement importStatement = elementFactory.createImportStatement(listClass);
                            file.getImportList().add(importStatement);
                            codeStyleManager.shortenClassReferences(file);
                            List<String> relFieldAnnos = new ArrayList<>();

                            relFieldAnnos.add("@ManyToOne");
                            String columnName = CamelCaseStringUtil.camelCase2UnderScore(mainFileName);
                            relFieldAnnos.add("@JoinColumn(name =\""+columnName.toUpperCase()+"_ID\", insertable = false, updatable = false)");
                            relFieldAnnos.add("@JsonBackReference");
                            PsiClass memberEntity = EntityCreator.createEntity(project,component,simpleEntityName,"", EntityCreator.EntityType.MemberEntity);
                            if(newSingleEntityDialog.getIsAbstractCheckBox().isSelected()){
                                if (!memberEntity.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT)) {
                                    memberEntity.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
                                }
                            }
                            PsiJavaFile memberEntityFile = (PsiJavaFile)memberEntity.getContainingFile();
                            PsiImportList imports = memberEntityFile.getImportList();
                            if(null==imports.findSingleImportStatement(JsonBackReference.class.getName())){
                                PsiImportStatement JsonBackImportStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(JsonBackReference.class.getName(),GlobalSearchScope.allScope(project)));
                                imports.add(JsonBackImportStatement);
                            }
                            PsiType relFieldType = new PsiJavaParserFacadeImpl(project).createTypeFromText(mainFileName,null);
                            PsJavaFileHelper.addField(memberEntity,StringUtils.uncapitalize(mainFileName),null,relFieldType,relFieldAnnos);
                            new OpenFileDescriptor(project, memberEntityFile.getVirtualFile()).navigate(true);
                        } catch (Exception exception) {
                            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"新建实体成员出现错误");
                            exception.printStackTrace();
                        }
                    }
                });
            } catch (Exception exception) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"新建实体成员出现错误");
                exception.printStackTrace();
            }
        }
    }
}
