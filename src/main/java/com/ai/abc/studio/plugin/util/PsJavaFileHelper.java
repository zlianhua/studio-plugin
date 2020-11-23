package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.CollectionListModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.Audited;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static PsiField addField(PsiClass psiClass, String fieldName, String initialValue, PsiType type, List<String> annotaions){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, type);
        for (String annotation : annotaions){
            PsiElement psiElement = codeStyleManager.shortenClassReferences(elementFactory.createAnnotationFromText(annotation, field));
            field.getModifierList().addBefore(psiElement, field.getModifierList().getFirstChild());
        }
        if (!StringUtils.isEmpty(initialValue) ){
            PsiExpression initialValueExpress = elementFactory.createExpressionFromText(initialValue,field);
            field.setInitializer(initialValueExpress);
        }
        psiClass.add(field);
        return field;
    }

    public static void addClassAnnotation(PsiClass psiClass,String annotation){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiAnnotation aAnnotation = elementFactory.createAnnotationFromText(annotation, psiClass);
        PsiElement psiElement = codeStyleManager.shortenClassReferences(aAnnotation);
        psiClass.getModifierList().addBefore(psiElement, psiClass.getModifierList().getFirstChild());
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

    public static PsiClass getEntity(PsiPackage psiPackage, String fileName){
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
    public static PsiClass createPsiClass(Project project,Path classPath,String className,List<String> packageImports,List<String> classImports,List<String> classAnnotations,String parentClassName){
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(VirtualFileManager.getInstance().findFileByNioPath(classPath));
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiClass psiClass = JavaDirectoryService.getInstance().createClass(directory, className);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        for(String packageImport: packageImports){
            PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand(packageImport);
            file.getImportList().add(importStatement);
        }
        for(String classImport : classImports){
            PsiImportStatement importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(classImport,GlobalSearchScope.allScope(project)));
            file.getImportList().add(importStatement);
        }
        for(String annotation : classAnnotations){
            addClassAnnotation(psiClass,annotation);
        }
        if(!StringUtils.isEmpty(parentClassName)){
            psiClass.getExtendsList().add(elementFactory.createReferenceFromText(parentClassName,psiClass));
        }else{
            psiClass.getImplementsList().add(elementFactory.createReferenceFromText(Serializable.class.getName(),psiClass));
        }
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        codeStyleManager.shortenClassReferences(file);
        return psiClass;
    }

    public static PsiClass createInterface(Project project,Path classPath,String className,List<String> packageImports,List<String> classImports,List<String> classAnnotations){
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(VirtualFileManager.getInstance().findFileByNioPath(classPath));
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiClass psiClass = JavaDirectoryService.getInstance().createInterface(directory, className);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        for(String packageImport: packageImports){
            PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand(packageImport);
            file.getImportList().add(importStatement);
        }
        for(String classImport : classImports){
            PsiImportStatement importStatement = elementFactory.createImportStatement(JavaPsiFacade.getInstance(project).findClass(classImport,GlobalSearchScope.allScope(project)));
            file.getImportList().add(importStatement);
        }
        for(String annotation : classAnnotations){
            addClassAnnotation(psiClass,annotation);
        }
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        codeStyleManager.shortenClassReferences(file);
        return psiClass;
    }
}
