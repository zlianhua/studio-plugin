package com.ai.abc.studio.plugin.file;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.EntityAttributeDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.json.JsonParser;
import com.intellij.openapi.project.Project;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileCreateHelper {
    public static String createMainPom(ComponentDefinition component) throws Exception{
        MemoryFile mainPom = ComponentVmUtil.createMainPom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(mainPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,mainPom.content, StandardOpenOption.CREATE);
        return fileName.toString();
    }

    public static void createAbcDirectory(ComponentDefinition component) throws Exception{
        String abcRootPath =component.getSimpleName()+File.separator+".abc";
        File abcRoot = new File(abcRootPath);
        if(!abcRoot.exists()){
            abcRoot.mkdirs();
        }
        saveMetaData(component);
    }

    public static void saveMetaData(ComponentDefinition component) throws Exception{
        StringBuilder fileName =getPackagePath(component)
                .append(File.separator)
                .append(".abc")
                .append(File.separator)
                .append(component.getSimpleName())
                .append(".json");
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(component);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,json.getBytes(), StandardOpenOption.CREATE);
    }

    public static void createModelModule(ComponentDefinition component) throws Exception{
        StringBuilder modelPath =getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-model".toLowerCase());
        File model = new File(modelPath.toString());
        if(!model.exists()){
            model.mkdirs();
        }
        StringBuilder modelSrcPkgPath = componentSrcPkg(component,modelPath)
                .append(File.separator)
                .append("model");
        File modelSrcPkg = new File(modelSrcPkgPath.toString());
        if(!modelSrcPkg.exists()){
            modelSrcPkg.mkdirs();
        }

        MemoryFile modelPom = ComponentVmUtil.createModelPom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(modelPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,modelPom.content, StandardOpenOption.CREATE);
    }

    public static void createServiceModule(ComponentDefinition component) throws Exception{
        StringBuilder servicePath =getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase());
        File service = new File(servicePath.toString());
        if(!service.exists()){
            service.mkdirs();
        }
        StringBuilder serviceSrcPkgPath = componentSrcPkg(component,servicePath)
                .append(File.separator)
                .append("service");
        File serviceSrcPkg = new File(serviceSrcPkgPath.toString());
        if(!serviceSrcPkg.exists()){
            serviceSrcPkg.mkdirs();
        }
        MemoryFile servicePom = ComponentVmUtil.createServicePom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(servicePom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,servicePom.content);
    }

    public static void createApiModule(ComponentDefinition component) throws Exception{
        StringBuilder apiPath =getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-api".toLowerCase());
        File api = new File(apiPath.toString());
        if(!api.exists()){
            api.mkdirs();
        }
        StringBuilder apiSrcPkgPath = componentSrcPkg(component,apiPath)
                .append(File.separator)
                .append("api");
        File apiSrcPkg = new File(apiSrcPkgPath.toString());
        if(!apiSrcPkg.exists()){
            apiSrcPkg.mkdirs();
        }
        MemoryFile apiPom = ComponentVmUtil.createApiPom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(apiPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,apiPom.content);
    }

    public static void createRestModule(ComponentDefinition component) throws Exception{
        StringBuilder restPath =getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-rest".toLowerCase());
        File rest = new File(restPath.toString());
        if(!rest.exists()){
            rest.mkdirs();
        }
        StringBuilder restSrcPkgPath = componentSrcPkg(component,restPath)
                .append(File.separator)
                .append("rest");
        File restSrcPkg = new File(restSrcPkgPath.toString());
        if(!restSrcPkg.exists()){
            restSrcPkg.mkdirs();
        }
        MemoryFile restPom = ComponentVmUtil.createRestPom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(restPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restPom.content);
    }

    public static void createRestProxyModule(ComponentDefinition component) throws Exception{
        StringBuilder restPath =getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-rest-proxy".toLowerCase());
        File rest = new File(restPath.toString());
        if(!rest.exists()){
            rest.mkdirs();
        }
        StringBuilder restSrcPkgPath = componentSrcPkg(component,restPath)
                .append(File.separator)
                .append("rest")
                .append(File.separator)
                .append("proxy");
        File restSrcPkg = new File(restSrcPkgPath.toString());
        if(!restSrcPkg.exists()){
            restSrcPkg.mkdirs();
        }
        MemoryFile restProxyPom = ComponentVmUtil.createRestProxyPom(component);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(restProxyPom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restProxyPom.content);
    }

    public static StringBuilder componentSrcPkg(ComponentDefinition component,StringBuilder typePath) throws Exception{
        return new StringBuilder().append(typePath)
                .append(File.separator)
                .append("src/main/java/")
                .append(StringUtils.replace(component.getBasePackageName(),".",File.separator))
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase());
    }

    public static StringBuilder getPackagePath(ComponentDefinition component){
        StringBuilder path =new StringBuilder()
                .append(component.getProjectDirectory())
                .append(File.separator)
                .append(component.getSimpleName());
        return path;
    }

    public static String getPackagePath(Project project){
        String basePath = project.getBasePath();
        String projectName = project.getName();
        return basePath+File.separator+projectName;
    }

    public static ComponentDefinition loadComponent(Project project) throws Exception{
        String basePath = project.getBasePath();
        String projectName = project.getName();
        String fileName = basePath+File.separator+".abc"+File.separator+projectName+".json";
        String content = new String(Files.readAllBytes( Paths.get(fileName))) ;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content,ComponentDefinition.class);
    }

    public static String getEntityClassFullName(Project project,String entitySimpleName) throws Exception{
        ComponentDefinition component = loadComponent(project);
        String packageName = EntityUtil.getComponentPackageName(component);
        return packageName+".model."+entitySimpleName;
    }

    public static boolean isRootEntity(Project project,String fileName)throws Exception{
        ComponentDefinition component = loadComponent(project);
        for(EntityDefinition entity : component.getEntities()){
            if(entity.getSimpleName().equalsIgnoreCase(fileName)){
                if(entity.isRoot()){
                    return true;
                }
            }
        }
        return false;
    }

    public static EntityDefinition getEntity(Project project,String fileName)throws Exception{
        ComponentDefinition component = loadComponent(project);
        for(EntityDefinition entity : component.getEntities()){
            if(entity.getSimpleName().equalsIgnoreCase(fileName)){
                return entity;
            }
        }
        return null;
    }

    public static String createEntityCode(Project project,EntityDefinition entity) throws Exception{
        ComponentDefinition component = loadComponent(project);
        if ((null == entity.getParentEntityName() || entity.getParentEntityName().isEmpty() || null == entity.getExtendsFromExternal() || entity.getExtendsFromExternal().isEmpty()) && component.isExtendsAbstractEntity()) {
            entity.setExtendsFromExternal("com.ai.abc.jpa.model.AbstractEntity");
        }
        //TODO 处理继承相关的注解
        if(!entity.isValueObject()){
            entity.getAnnotations().add("@Entity");
            String tableName = CamelCaseStringUtil.camelCase2UnderScore(entity.getSimpleName());
            entity.setTableName(tableName);
            entity.getAnnotations().add("@Table( name=\"" + entity.getTableName() + "\")");
            entity.setTableName(tableName);
            EntityAttributeDefinition identifier = new EntityAttributeDefinition();
            identifier.setIdentifier(true);
            identifier.setDataType("Long");
            identifier.setName(StringUtils.uncapitalize(entity.getSimpleName())+"Id");
            identifier.getAnnotations().add("@Id");
            identifier.getAnnotations().add("@GeneratedValue(strategy = GenerationType.AUTO)");
            entity.getAttributes().add(identifier);
        }

        MemoryFile entityCode = ComponentVmUtil.createModelCode(component,entity);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(entityCode.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,entityCode.content);
        component.getEntities().add(entity);
        saveMetaData(component);
        //生成接口、服务、rest和rest Proxy
        if(entity.isRoot()){
            createInterfaceRelCodes(component,entity);
        }
        return filePath.toString();
    }

    private static void createInterfaceRelCodes(ComponentDefinition component,EntityDefinition rootEntity) throws Exception{
        List<EntityDefinition> entities = new ArrayList<>();
        entities.add(rootEntity);
        //repository
        MemoryFile repositoryCode = ComponentVmUtil.createRepositoryCode(component,rootEntity);
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(repositoryCode.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,repositoryCode.content);
        //command api
        MemoryFile apiCode = ComponentVmUtil.createApiCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(apiCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,apiCode.content);
        //query api
        MemoryFile queryApiCode = ComponentVmUtil.createQueryApiCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(queryApiCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,queryApiCode.content);
        //service
        MemoryFile serviceCode = ComponentVmUtil.createServiceCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(serviceCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,serviceCode.content);
        //queryService
        MemoryFile queryServiceCode = ComponentVmUtil.createQueryServiceCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(queryServiceCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,queryServiceCode.content);
        //service test
        //service test code
        MemoryFile testServiceCode = ComponentVmUtil.createTestCode(component,rootEntity,entities);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testServiceCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testServiceCode.content);
        //service test app java
        MemoryFile testServiceAppCode = ComponentVmUtil.createAppCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testServiceAppCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testServiceAppCode.content);
        //service test app properties
        MemoryFile testServiceAppFileMainCode = ComponentVmUtil.createApplicationFile(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testServiceAppFileMainCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testServiceAppFileMainCode.content);
        //rest
        MemoryFile restCode = ComponentVmUtil.createRestCode(component,entities,entities);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(restCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restCode.content);
        //rest test code
        MemoryFile testRestCode = ComponentVmUtil.createRestTestCode(component,entities);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testRestCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testRestCode.content);
        //rest test app java
        MemoryFile testRestAppCode = ComponentVmUtil.createRestAppCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testRestAppCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testRestAppCode.content);
        //rest test app properties
        MemoryFile testRestAppFileCode = ComponentVmUtil.createRestApplicationFile(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(testRestAppFileCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,testRestAppFileCode.content);
        //rest proxy
        MemoryFile restProxyCode = ComponentVmUtil.createRestProxyCode(component,rootEntity,entities);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(restProxyCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restProxyCode.content);
        //query rest proxy
        MemoryFile queryRestProxyCode = ComponentVmUtil.createQueryRestProxyCode(component,rootEntity);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(queryRestProxyCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,queryRestProxyCode.content);
        //rest configration
        MemoryFile restConfigCode = ComponentVmUtil.createRestConfigrationCode(component,entities,entities);
        fileName = getPackagePath(component)
                .append(File.separator)
                .append(restConfigCode.fileName);
        filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,restConfigCode.content);
    }
}