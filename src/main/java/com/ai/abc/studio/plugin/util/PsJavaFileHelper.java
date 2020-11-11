package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.CollectionListModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PsJavaFileHelper {
    public static PsiField findField(PsiClass psiClass, String fieldName){
        List<PsiField> fields = (new CollectionListModel<>(psiClass.getFields())).getItems();
        for(PsiField field : fields){
            if(field.getName().equalsIgnoreCase(fieldName)){
                return field;
            }
        }
        return null;
    }

    public static void deleteField(PsiClass psiClass, String fieldName){
        PsiField field = findField(psiClass,fieldName);
        if(null!=field){
            PsiAnnotation[] annotations = field.getAnnotations();
            for(PsiAnnotation annotation : annotations){
                annotation.delete();
            }
            field.delete();
        }
    }

    public static PsiField addField(PsiClass psiClass,PsiType fieldType, String fieldName){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, fieldType);
        psiClass.add(field);
        return field;
    }

    public static PsiField addFieldWithAnnotations(PsiClass psiClass, String fieldName, PsiType type, List<String> annotaions){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, type);
        for (String annotation : annotaions){
            PsiElement psiElement = codeStyleManager.shortenClassReferences(elementFactory.createAnnotationFromText(annotation, field));
            field.getModifierList().addBefore(psiElement, field.getModifierList().getFirstChild());
        }
        psiClass.add(field);
        return field;
    }

    public static JComponent getDialogSource(AnActionEvent e){
        JComponent source;
        Component comp = e.getInputEvent().getComponent();
        if (comp instanceof JComponent) {
            source = (JComponent) comp;
        } else {
            JWindow w;
            if (comp instanceof JWindow) {
                w = (JWindow) comp;
            } else {
                w = (JWindow) WindowManager.getInstance().suggestParentWindow(e.getProject());
            }
            source = w.getRootPane();
        }
        return source;
    }

    public static PsiClass getEntity(PsiPackage psiPackage, String fileName)throws Exception{
        PsiClass[] allClasses = psiPackage.getClasses();
        for(PsiClass acls : allClasses){
            if(acls.getQualifiedName().endsWith(fileName)){
                return acls;
            }
        }
        return null;
    }

    public static boolean isRootEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcRootEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static boolean isValueEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcValueEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static boolean isMemberEntity(PsiClass aClass){
        PsiAnnotation[] annotations= aClass.getModifierList().getAnnotations();
        for(PsiAnnotation annotation : annotations){
            if(annotation.getQualifiedName().equalsIgnoreCase(AiAbcMemberEntity.class.getName())){
                return true;
            }
        }
        return false;
    }

    public static void createPsiClassFieldsFromTableColumn(Project project, PsiClass psiClass, List<Column> columns, ComponentDefinition component){
        List<String> abstractEntityFieldNames = new ArrayList<>();
        if(component.isExtendsAbstractEntity()){
            abstractEntityFieldNames = FileCreateHelper.getAbstractEntityFields();
        }
        final List<String> ignoreFields = abstractEntityFieldNames;
        if (null != columns && !columns.isEmpty()) {
            for (Column column : columns) {
                String remarks = column.getName();
                String columnName = column.getCode();
                String fieldName = CamelCaseStringUtil.underScore2Camel(columnName, true);
                if(component.isExtendsAbstractEntity() && ignoreFields.contains(fieldName)){
                    continue;
                }
                PsiField field = PsJavaFileHelper.findField(psiClass, fieldName);
                if (null != field) {
                    PsJavaFileHelper.deleteField(psiClass, fieldName);
                }
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                String fieldJavaType = DBMetaDataUtil.columnDataTypeToJavaType(column.getType());
                PsiType fieldType = new PsiJavaParserFacadeImpl(psiClass.getProject()).createTypeFromText(fieldJavaType, null);
                List<String> annotations = new ArrayList<>();
                annotations.add("@Column(name =\"" + columnName.toUpperCase() + "\")");
                if (column.isPkFlag()) {
                    annotations.add("@GeneratedValue(strategy = GenerationType.AUTO)");
                    annotations.add("@Id");
                }
                if (column.isClob()) {
                    annotations.add("@Lob");
                }
                field = PsJavaFileHelper.addFieldWithAnnotations(psiClass, fieldName, fieldType, annotations);
                PsiComment comment = elementFactory.createCommentFromText("/**" + remarks + "*/", null);
                field.getModifierList().addBefore(comment, field.getModifierList().getFirstChild());
            }
            CodeStyleManager.getInstance(project).reformat(psiClass);
        }
    }

    public static void addNewEntityImports(Project project, PsiClass psiClass){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand("javax.persistence");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Audited.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Getter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(NoArgsConstructor.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Setter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatementOnDemand("com.ai.abc.core.annotations");
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(EntityToJsonConverter.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(Timestamp.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
        importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(List.class.getName(), GlobalSearchScope.allScope(project)));
        file.getImportList().add(importStatement);
    }
}
