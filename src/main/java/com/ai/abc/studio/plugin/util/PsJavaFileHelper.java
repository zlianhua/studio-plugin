package com.ai.abc.studio.plugin.util;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.ui.CollectionListModel;
import org.thymeleaf.util.StringUtils;

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

    public static void addField(PsiClass psiClass,PsiType fieldType, String fienldName){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiField field = elementFactory.createField(fienldName, fieldType);
        psiClass.add(field);
    }

    public static void addFieldWithAnnotations(PsiClass psiClass, String fieldName, PsiType type, List<String> annotaions){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, type);
        for (String annotation : annotaions){
            PsiElement psiElement = codeStyleManager.shortenClassReferences(elementFactory.createAnnotationFromText(annotation, field));
            field.getModifierList().addBefore(psiElement, field.getModifierList().getFirstChild());
        }
        psiClass.add(field);
    }
}
