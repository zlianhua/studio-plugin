package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.AbstractEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Path modelPath = Paths.get(project.getBasePath()+ File.separator+ FileCreateHelper.getModelPath(component));
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
        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Getter");
        classAnnotations.add("@Setter");
        classAnnotations.add("@NoArgsConstructor");
        if(!StringUtils.isEmpty(tableName)){
            classAnnotations.add("Table(name=\""+tableName.toUpperCase()+"\")");
        }
        classAnnotations.add("@Entity");
        if(null!=entityType){
            if(entityType.equals(EntityType.RootEntity)){
                classAnnotations.add("@AiAbcRootEntity");
                classImports.add(AiAbcRootEntity.class.getName());
            }else if (entityType.equals(EntityType.ValueEntity)){
                classAnnotations.add("@AiAbcValueEntity");
                classImports.add(AiAbcValueEntity.class.getName());
            }else{
                classAnnotations.add("@AiAbcMemberEntity");
                classImports.add(AiAbcMemberEntity.class.getName());
            }
        }else{
            if(entityName.equalsIgnoreCase(component.getSimpleName())){
                classAnnotations.add("@AiAbcRootEntity");
                classImports.add(AiAbcRootEntity.class.getName());
            }else{
                classAnnotations.add("@AiAbcMemberEntity");
                classImports.add(AiAbcMemberEntity.class.getName());
            }
        }
        String parentClass = null;
        if(component.isExtendsAbstractEntity()){
            parentClass = AbstractEntity.class.getName();
        }
        PsiClass entityClass = PsJavaFileHelper.createPsiClass(project,modelPath,entityName,packageImports,classImports,classAnnotations,parentClass);
        return entityClass;
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
                field = PsJavaFileHelper.addField(psiClass, fieldName, null,fieldType, annotations);
                PsiComment comment = elementFactory.createCommentFromText("/**" + remarks + "*/", null);
                field.getModifierList().addBefore(comment, field.getModifierList().getFirstChild());
            }
            CodeStyleManager.getInstance(project).reformat(psiClass);
        }
    }
}
