package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.CollectionListModel;
import org.springframework.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
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

    public static PsiField addField(PsiClass psiClass, String fieldName, String initialValue, PsiType type, List<String> annotaions,String comment){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        PsiField field = elementFactory.createField(fieldName, type);
        if(null!=annotaions){
            for (String annotation : annotaions){
                PsiElement psiElement = codeStyleManager.shortenClassReferences(elementFactory.createAnnotationFromText(annotation, field));
                field.getModifierList().addBefore(psiElement, field.getModifierList().getFirstChild());
            }
        }
        if (!StringUtils.isEmpty(initialValue) ){
            PsiExpression initialValueExpress = elementFactory.createExpressionFromText(initialValue,field);
            field.setInitializer(initialValueExpress);
        }
        if(null!=comment){
            PsiComment aComment = elementFactory.createCommentFromText(comment,null);
            field.addBefore(aComment,field.getFirstChild());
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

    public static PsiClass getEntity(PsiPackage psiPackage, String fileName) throws Exception{
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
    public static PsiClass createPsiClass(Project project,VirtualFile controllerVirtualFile,String className,List<String> packageImports,List<String> classImports,List<String> classAnnotations,String parentClassName) throws Exception{
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(controllerVirtualFile);
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiClass psiClass = JavaDirectoryService.getInstance().createClass(directory, className);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        if (null!=packageImports) {
            for(String packageImport: packageImports){
                PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand(packageImport);
                file.getImportList().add(importStatement);
            }
        }
        if (null!=classImports) {
            for(String classImport : classImports){
                PsiClass aImportCls = findClass(project,classImport);
                if(null!=aImportCls){
                    PsiImportStatement importStatement = elementFactory.createImportStatement(aImportCls);
                    file.getImportList().add(importStatement);
                }
            }
        }
        if (null!=classAnnotations) {
            for(String annotation : classAnnotations){
                addClassAnnotation(psiClass,annotation);
            }
        }
        if(!StringUtils.isEmpty(parentClassName)){
            psiClass.getExtendsList().add(elementFactory.createReferenceFromText(parentClassName,psiClass));
        }
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager.shortenClassReferences(file);
        return psiClass;
    }

    public static PsiClass createInterface(Project project,VirtualFile apiVirtualFile,String className,List<String> packageImports,List<String> classImports,List<String> classAnnotations,String parentClassName) throws Exception{
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(apiVirtualFile);
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiClass psiClass = JavaDirectoryService.getInstance().createInterface(directory, className);
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();

        if (null!=packageImports) {
            for(String packageImport: packageImports){
                PsiImportStatement importStatement = elementFactory.createImportStatementOnDemand(packageImport);
                file.getImportList().add(importStatement);
            }
        }
        if (null!=classImports) {
            for(String classImport : classImports){
                PsiClass aImportCls = findClass(project,classImport);
                if(null!=aImportCls){
                    PsiImportStatement importStatement = elementFactory.createImportStatement(aImportCls);
                    file.getImportList().add(importStatement);
                }
            }
        }
        if (null!=classAnnotations) {
            for(String annotation : classAnnotations){
                addClassAnnotation(psiClass,annotation);
            }
        }
        if(!StringUtils.isEmpty(parentClassName)){
            psiClass.getExtendsList().add(elementFactory.createReferenceFromText(parentClassName,psiClass));
        }
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager.shortenClassReferences(file);
        return psiClass;
    }

    public static PsiClass findClass(Project project,String className){
        PsiClassType typeByName = PsiType.getTypeByName(className, project, GlobalSearchScope.allScope(project));
        PsiClass aImportCls = typeByName.resolve();
        if(null==aImportCls){
            aImportCls = JavaPsiFacade.getInstance(project).findClass(className,GlobalSearchScope.allScope(project));
        }
        if(null==aImportCls){
            try {
                Thread.sleep(500);
                aImportCls = JavaPsiFacade.getInstance(project).findClass(className,GlobalSearchScope.allScope(project));
                if(null==aImportCls){
                    aImportCls = typeByName.resolve();
                }
            } catch (InterruptedException e) {

            }
        }
        return aImportCls;
    }
}
