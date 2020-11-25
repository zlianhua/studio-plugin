package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.AbstractEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.*;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.jetbrains.jsonProtocol.JsonField;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EntityCreator {
    public enum EntityType{
        RootEntity,
        MemberEntity,
        ValueEntity;
    }

    public static PsiClass createEntity(Project project, ComponentDefinition component, String entityName,String tableName,EntityType entityType){
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path modelPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getModelPath(component));
        VirtualFile modelVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(modelPath);
        if(null==modelVirtualFile){
            createModelPath(component);
            modelVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(modelPath);
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        packageImports.add("javax.persistence");
        packageImports.add("com.ai.abc.core.annotations");

        List<String> classImports = new ArrayList<>();
        if(component.isAuditable()){
            classImports.add(Audited.class.getName());
        }
        classImports.add(Getter.class.getName());
        classImports.add(Setter.class.getName());
        classImports.add(NoArgsConstructor.class.getName());
        classImports.add(EntityToJsonConverter.class.getName());
        classImports.add(Timestamp.class.getName());
        classImports.add(org.hibernate.annotations.Type.class.getName());

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Getter");
        classAnnotations.add("@Setter");
        classAnnotations.add("@NoArgsConstructor");
        if(!StringUtils.isEmpty(tableName)){
            classAnnotations.add("@Table(name=\""+tableName.toUpperCase()+"\")");
        }
        if(null!=entityType){
            if(entityType.equals(EntityType.RootEntity)){
                classAnnotations.add("@AiAbcRootEntity");
                classAnnotations.add("@Entity");
                classImports.add(AiAbcRootEntity.class.getName());
            }else if (entityType.equals(EntityType.ValueEntity)){
                classAnnotations.add("@AiAbcValueEntity");
                classImports.add(AiAbcValueEntity.class.getName());
            }else{
                classAnnotations.add("@AiAbcMemberEntity");
                classAnnotations.add("@Entity");
                classImports.add(AiAbcMemberEntity.class.getName());
            }
        }else{
            if(entityName.equalsIgnoreCase(component.getSimpleName())){
                classAnnotations.add("@AiAbcRootEntity");
                classAnnotations.add("@Entity");
                classImports.add(AiAbcRootEntity.class.getName());
            }else{
                classAnnotations.add("@AiAbcMemberEntity");
                classAnnotations.add("@Entity");
                classImports.add(AiAbcMemberEntity.class.getName());
            }
        }
        String parentClass = null;
        if(component.isExtendsAbstractEntity()){
            parentClass = AbstractEntity.class.getName();
        }
        PsiClass entityClass = PsJavaFileHelper.createPsiClass(project,modelVirtualFile,entityName,packageImports,classImports,classAnnotations,parentClass);
        if(null==entityClass.getExtendsList()){
            entityClass.getImplementsList().add(elementFactory.createReferenceFromText(Serializable.class.getName(),entityClass));
        }
        if(component.isLogicalDelete()){
            PsiType fieldType = new PsiJavaParserFacadeImpl(project).createTypeFromText("Boolean", null);
            List<String> annotations = new ArrayList<>();
            annotations.add("@Column(name =\"DELETED\")");
            annotations.add("@Type(type=\"yes_no\")");
            PsJavaFileHelper.addField(entityClass, "deleted", "false",fieldType, annotations);
        }
        PsiJavaFile file = (PsiJavaFile)entityClass.getContainingFile();
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager.shortenClassReferences(file);
        return entityClass;
    }

    public static void createPsiClassFieldsFromTableColumn(Project project, PsiClass psiClass, List<Column> columns, ComponentDefinition component){
        List<String> abstractEntityFieldNames = new ArrayList<>();
        if(component.isExtendsAbstractEntity()){
            abstractEntityFieldNames = ComponentCreator.getAbstractEntityFields();
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
                field = PsJavaFileHelper.addField(psiClass, fieldName, null,fieldType, annotations);
                PsiComment comment = elementFactory.createCommentFromText("/**" + remarks + "*/", null);
                field.getModifierList().addBefore(comment, field.getModifierList().getFirstChild());
            }
            CodeStyleManager.getInstance(project).reformat(psiClass);
        }
    }

    public static void createModelPath(ComponentDefinition component) {
        StringBuilder modelPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-model".toLowerCase());
        File model = new File(modelPath.toString());
        if (!model.exists()) {
            model.mkdirs();
        }
        StringBuilder modelSrcPkgPath = ComponentCreator.componentSrcPkg(component, modelPath)
                .append(File.separator)
                .append("model");
        File modelSrcPkg = new File(modelSrcPkgPath.toString());
        if (!modelSrcPkg.exists()) {
            modelSrcPkg.mkdirs();
        }
    }

    public static void createModelModule(ComponentDefinition component) throws Exception{
        createModelPath(component);
        MemoryFile modelPom = ComponentVmUtil.createModelPom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(modelPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,modelPom.content, StandardOpenOption.CREATE);
    }

    public static void saveEntityMockedJson(ComponentDefinition component,String entityName,String json) throws Exception{
        StringBuilder entityJsonPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase())
                .append(File.separator)
                .append("src")
                .append(File.separator)
                .append("test")
                .append(File.separator)
                .append("resources")
                .append(File.separator)
                .append(entityName)
                .append(".json");
        Path filePath = Paths.get(entityJsonPath.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,json.getBytes(), StandardOpenOption.CREATE);
    }

    public static String getEntityClassFullName(Project project,String entitySimpleName) throws Exception{
        ComponentDefinition component = ComponentCreator.loadComponent(project);
        String packageName = EntityUtil.getComponentPackageName(component);
        return packageName+".model."+entitySimpleName;
    }

    public static PsiField findPrimaryField(Project project,String entityName,ComponentDefinition component){
        Path modelPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getModelPath(component));
        VirtualFile modelVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(modelPath);
        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(modelVirtualFile));
        PsiClass entityClass = PsJavaFileHelper.getEntity(psiPackage,entityName);
        PsiField[] fields = entityClass.getFields();
        if(null!=fields){
            for(PsiField field : fields){
                PsiAnnotation[] annotations = field.getAnnotations();
                if(null!=annotations){
                    for(PsiAnnotation annotation : annotations){
                        if(annotation.getQualifiedName().equals("javax.persistence.Id")){
                            return field;
                        }
                    }
                }
            }
        }
        return null;
    }
}
