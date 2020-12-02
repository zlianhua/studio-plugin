package com.ai.abc.studio.plugin.util;

import com.ai.abc.core.annotations.AiAbcMemberEntity;
import com.ai.abc.core.annotations.AiAbcRootEntity;
import com.ai.abc.core.annotations.AiAbcValueEntity;
import com.ai.abc.jpa.model.AbstractEntity;
import com.ai.abc.jpa.model.EntityToJsonConverter;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.util.*;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.lang.java.JavaCommenter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
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
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class EntityCreator {
    public enum EntityType{
        RootEntity,
        MemberEntity,
        ValueEntity;
    }

    public static PsiClass createEntity(Project project, ComponentDefinition component, String entityName,
                                        String tableName,EntityType entityType,String desc,boolean isAbstract,String parentClass) throws Exception{
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path modelPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getModelPath(component));
        VirtualFile modelVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(modelPath);
        if(null==modelVirtualFile){
            createModelPath(component);
            modelVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(modelPath);
        }
        if(StringUtils.isEmpty(tableName)){
            tableName = getTableNameFromEntityName(component,entityName);
        }
        tableName = tableName.toUpperCase();
        List<String> packageImports = new ArrayList<>();
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        packageImports.add("javax.persistence");
        packageImports.add("com.ai.abc.core.annotations");
        packageImports.add("io.swagger.annotations");

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
        if(StringUtils.isEmpty(desc)){
            desc = entityName;
        }
        if(!isAbstract) {
            classAnnotations.add("@ApiModel(description = \"" + desc + "\")");
        }
        ComponentDefinition.InheritanceStrategy inheritanceType = component.getInheritanceStrategy();
        String inheritanceTypeName = inheritanceType.name();
        if(inheritanceType.equals(ComponentDefinition.InheritanceStrategy.SECONDARY_TABLE)){
            inheritanceTypeName = ComponentDefinition.InheritanceStrategy.SINGLE_TABLE.name();
        }
        if(isAbstract){
            if(inheritanceTypeName.equals(ComponentDefinition.InheritanceStrategy.TABLE_PER_CLASS.name())&&null!=parentClass){
                classAnnotations.add("@MappedSuperclass");
            }else{
                classAnnotations.add("@Entity");
                if(!StringUtils.isEmpty(tableName)&&null==parentClass){
                    classAnnotations.add("@Table(name=\""+tableName+"\")");
                }
            }
            if (null==parentClass){
                if(inheritanceType.equals(ComponentDefinition.InheritanceStrategy.SINGLE_TABLE)
                        ||inheritanceType.equals(ComponentDefinition.InheritanceStrategy.SECONDARY_TABLE)){
                    classAnnotations.add("@DiscriminatorColumn(name=\"TYPE\",discriminatorType=DiscriminatorType.STRING)");
                }
                classAnnotations.add("@Inheritance (strategy = InheritanceType."+inheritanceTypeName+")");
                classImports.add(Timestamp.class.getName());
            }
        }else {
            if (null != entityType) {
                if (entityType.equals(EntityType.RootEntity)) {
                    classAnnotations.add("@AiAbcRootEntity");
                    classAnnotations.add("@Entity");
                    classImports.add(AiAbcRootEntity.class.getName());
                } else if (entityType.equals(EntityType.ValueEntity)) {
                    classAnnotations.add("@AiAbcValueEntity");
                    classImports.add(AiAbcValueEntity.class.getName());
                } else {
                    classAnnotations.add("@AiAbcMemberEntity");
                    classAnnotations.add("@Entity");
                    classImports.add(AiAbcMemberEntity.class.getName());
                }
            } else {
                if (entityName.equalsIgnoreCase(component.getSimpleName())) {
                    classAnnotations.add("@AiAbcRootEntity");
                    classAnnotations.add("@Entity");
                    classImports.add(AiAbcRootEntity.class.getName());
                } else {
                    classAnnotations.add("@AiAbcMemberEntity");
                    classAnnotations.add("@Entity");
                    classImports.add(AiAbcMemberEntity.class.getName());
                }
            }
            if(null!=parentClass && !entityType.equals(EntityType.ValueEntity)){
                classAnnotations.add("@DiscriminatorValue(\"" + entityName + "\")");
                if (inheritanceType.equals(ComponentDefinition.InheritanceStrategy.SECONDARY_TABLE)){
                    classAnnotations.add("@SecondaryTable(name = \"" + tableName + "\",pkJoinColumns = @PrimaryKeyJoinColumn(name = \"请替换主键\"))");
                }else if (inheritanceType.equals(ComponentDefinition.InheritanceStrategy.TABLE_PER_CLASS)){
                    if(!StringUtils.isEmpty(tableName)){
                        classAnnotations.add("@Table(name=\""+tableName+"\")");
                    }
                }
            }else{
                if(!StringUtils.isEmpty(tableName) && !entityType.equals(EntityType.ValueEntity)){
                    classAnnotations.add("@Table(name=\""+tableName+"\")");
                }
            }
        }
        if(null==parentClass && component.isExtendsAbstractEntity()){
            parentClass = AbstractEntity.class.getName();
        }

        PsiClass entityClass = PsJavaFileHelper.createPsiClass(project,modelVirtualFile,entityName,packageImports,classImports,classAnnotations,parentClass);
        if(isAbstract){
            if (!entityClass.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT)) {
                entityClass.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
            }
        }
        if(null==entityClass.getExtendsList()){
            entityClass.getImplementsList().add(elementFactory.createReferenceFromText(Serializable.class.getName(),entityClass));
        }
        //comment
        JavaCommenter commenter = new JavaCommenter();
        String commentText = commenter.getBlockCommentPrefix()+desc+commenter.getBlockCommentSuffix();
        PsiComment comment = elementFactory.createCommentFromText(commentText , null);
        entityClass.addBefore(comment,entityClass.getFirstChild());
        //comment

        if(component.isLogicalDelete()){
            PsiType fieldType = new PsiJavaParserFacadeImpl(project).createTypeFromText("Boolean", null);
            List<String> annotations = new ArrayList<>();
            annotations.add("@Column(name =\"DELETED\")");
            annotations.add("@Type(type=\"yes_no\")");
            PsJavaFileHelper.addField(entityClass, "deleted", "false",fieldType, annotations,"/**是否已删除*/");
        }
        PsiJavaFile file = (PsiJavaFile)entityClass.getContainingFile();
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager.shortenClassReferences(file);
        return entityClass;
    }

    public static void createPsiClassFieldsFromTableColumn(Project project, PsiClass psiClass, List<Column> columns, ComponentDefinition component) throws Exception{
        List<String> abstractEntityFieldNames = new ArrayList<>();
        if(component.isExtendsAbstractEntity()){
            abstractEntityFieldNames = ComponentCreator.getAbstractEntityFields();
        }
        String pkColumnName=null;
        JavaCommenter commenter = new JavaCommenter();
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
                if(!PsJavaFileHelper.isValueEntity(psiClass)) {
                    annotations.add("@Column(name =\"" + columnName.toUpperCase() + "\")");
                    if (column.isPkFlag()) {
                        annotations.add("@GeneratedValue(strategy = GenerationType.AUTO)");
                        annotations.add("@Id");
                        pkColumnName = columnName.toUpperCase();
                    }
                }
                if (column.isClob()) {
                    annotations.add("@Lob");
                }
                String commentText = commenter.getBlockCommentPrefix();
                String description = "";
                if(null!=remarks&&!remarks.trim().equals("")){
                    description+=remarks;
                }else{
                    description+=fieldName;
                }
                annotations.add("@ApiModelProperty(notes = \""+description+"\")");
                commentText+=description;
                commentText+=commenter.getBlockCommentSuffix();
                field = PsJavaFileHelper.addField(psiClass, fieldName, null,fieldType, annotations,commentText);
                PsiComment comment = elementFactory.createCommentFromText(commentText , null);
                field.addBefore(comment,field.getFirstChild());
            }
            if(StringUtils.isEmpty(pkColumnName)){
                pkColumnName="请补充主键";
            }
            if(!PsJavaFileHelper.isValueEntity(psiClass)){
                boolean isAbstract = psiClass.getModifierList().hasModifierProperty("abstract");
                ComponentDefinition.InheritanceStrategy inheritanceType = component.getInheritanceStrategy();
                if (!isAbstract && inheritanceType.equals(ComponentDefinition.InheritanceStrategy.SECONDARY_TABLE)){
                    PsiAnnotation secondTable = psiClass.getModifierList().findAnnotation("javax.persistence.SecondaryTable");
                    if(null!=secondTable){
                        secondTable.delete();
                    }
                    String tableName = getTableNameFromEntityName(component,psiClass.getName());
                    psiClass.getModifierList().addAnnotation("SecondaryTable(name = \"" + tableName + "\",pkJoinColumns = @PrimaryKeyJoinColumn(name = \""+pkColumnName+"\"))");
                }
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

    public static PsiField findPrimaryField(Project project,String entityName,ComponentDefinition component) throws Exception{
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

    public static List<PsiClass> findRootEntities(Project project,ComponentDefinition component){
        List<PsiClass> rootEntities = new ArrayList<>();
        Path modelPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getModelPath(component));
        VirtualFile modelVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(modelPath);
        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(modelVirtualFile));
        PsiClass[] entityClasses = psiPackage.getClasses();
        if(null!=entityClasses && entityClasses.length>0){
            for(PsiClass entityClass : entityClasses){
                PsiAnnotation[] annotations = entityClass.getAnnotations();
                if(null!=annotations){
                    for(PsiAnnotation annotation : annotations){
                        if(annotation.getQualifiedName().equals("com.ai.abc.core.annotations.AiAbcRootEntity")){
                            rootEntities.add(entityClass);
                            break;
                        }
                    }
                }
            }
        }
        if(rootEntities.isEmpty()){
            for(PsiClass entityClass : entityClasses){
                rootEntities.add(entityClass);
            }
        }
        return rootEntities;
    }

    public static String getEntityNameFromTableName(ComponentDefinition component,String tableName){
        String prefix = component.getTablePrefix();
        if(null!=prefix && !prefix.endsWith("_")){
            prefix+="_";
        }
        String tmpEntityName = tableName;
        if(!StringUtils.isEmpty(prefix)){
            tmpEntityName = tmpEntityName.substring(prefix.length());
        }
        return StringUtils.capitalize(CamelCaseStringUtil.underScore2Camel(tmpEntityName,true));
    }

    public static String getTableNameFromEntityName(ComponentDefinition component,String entityName){
        String prefix = component.getTablePrefix();
        if(null!=prefix && !prefix.endsWith("_")){
            prefix+="_";
        }
        String tableName = CamelCaseStringUtil.camelCase2UnderScore(entityName);
        if(!StringUtils.isEmpty(prefix)){
            tableName=prefix+tableName;
        }
        return tableName.toUpperCase();
    }

    public static void createEntityFromTable(Project project, ComponentDefinition component, TableInfo tableInfo, AnActionEvent e) throws Exception{
        String tableName = tableInfo.getTableName();
        String entityName = EntityCreator.getEntityNameFromTableName(component,tableName);
        String desc = tableInfo.getDesc();
        if(null==desc){
            desc = entityName;
        }
        EntityType entityType = EntityCreator.EntityType.valueOf(tableInfo.getEntityType());
        boolean isRoot = entityType.equals(EntityCreator.EntityType.RootEntity);
        DBConnectProp dbConnectProp = component.getDbConnectProp();
        String dbUrl = dbConnectProp.getDbUrl();
        String dbUserName = dbConnectProp.getDbUserName();
        String dbPassword = dbConnectProp.getDbPassword();
        List<Column> columns = DBMetaDataUtil.getTableColumns(dbUrl, dbUserName, dbPassword, tableName);

        String parentClass = null;
        if(null!=tableInfo.getParentTableInfo()){
            String parentTableName = tableInfo.getParentTableInfo().getTableName();
            if(!StringUtils.isEmpty(parentTableName)){
                parentClass = EntityCreator.getEntityNameFromTableName(component,parentTableName);
                String parentEntityName = getEntityNameFromTableName(component,parentTableName);
                PsiFile[] files = FilenameIndex.getFilesByName(project,parentEntityName+".java", ProjectScope.getProjectScope(project));
                if(null==files || files.length==0) {
                    createEntityFromTable(project,component,tableInfo.getParentTableInfo(),e);
                }
            }
        }
        PsiFile[] files = FilenameIndex.getFilesByName(project,entityName+".java", ProjectScope.getProjectScope(project));
        if(null==files || files.length==0) {
            PsiClass psiClass = EntityCreator.createEntity(project, component,entityName,tableName,entityType,desc,tableInfo.isAbstract(),parentClass);
            if(null!=columns && !columns.isEmpty()){
                EntityCreator.createPsiClassFieldsFromTableColumn(project,psiClass,columns,component);
            }
        }
        if(isRoot){
            RepositoryCreator.createRepository(project,component,entityName,e);
            ApiClassCreator.createApiClasses(project,component,entityName);
            ServiceCreator.createService(project,component,entityName);
        }
    }
}
