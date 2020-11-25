package com.ai.abc.studio.plugin.util;

import com.ai.abc.jpa.model.AbstractEntity;
import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.EntityAttributeDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.intellij.json.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ComponentCreator {
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


    public static StringBuilder componentSrcPkg(ComponentDefinition component,StringBuilder typePath){
        return new StringBuilder().append(typePath)
                .append(File.separator)
                .append("src/main/java/")
                .append(StringUtils.replace(component.getBasePackageName(),".",File.separator))
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase());
    }

    public static StringBuilder componentTestSrcPkg(ComponentDefinition component,StringBuilder typePath){
        return new StringBuilder().append(typePath)
                .append(File.separator)
                .append("src/test/java/")
                .append(StringUtils.replace(component.getBasePackageName(),".",File.separator))
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase());
    }

    public static StringBuilder componentTestResourcesPkg(ComponentDefinition component,StringBuilder typePath){
        return new StringBuilder().append(typePath)
                .append(File.separator)
                .append("src/test/resources/");
    }

    public static StringBuilder getPackagePath(ComponentDefinition component){
        StringBuilder path =new StringBuilder()
                .append(component.getProjectDirectory())
                .append("/")
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

//    public static boolean isRootEntity(Project project,String fileName)throws Exception{
//        ComponentDefinition component = loadComponent(project);
//        for(EntityDefinition entity : component.getEntities()){
//            if(entity.getSimpleName().equalsIgnoreCase(fileName)){
//                if(entity.isRoot()){
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//    public static String createEntityCode(Project project,EntityDefinition entity) throws Exception{
//        ComponentDefinition component = loadComponent(project);
//        if ((null == entity.getParentEntityName() || entity.getParentEntityName().isEmpty() || null == entity.getExtendsFromExternal() || entity.getExtendsFromExternal().isEmpty()) && component.isExtendsAbstractEntity()) {
//            entity.setExtendsFromExternal("com.ai.abc.jpa.model.AbstractEntity");
//        }
//        //TODO 处理继承相关的注解
//        if(!entity.isValueObject()){
//            entity.getAnnotations().add("@Entity");
//            String tableName = CamelCaseStringUtil.camelCase2UnderScore(entity.getSimpleName());
//            entity.setTableName(tableName);
//            entity.getAnnotations().add("@Table( name=\"" + entity.getTableName() + "\")");
//            entity.setTableName(tableName);
//            EntityAttributeDefinition identifier = new EntityAttributeDefinition();
//            identifier.setIdentifier(true);
//            identifier.setDataType("Long");
//            identifier.setName(StringUtils.uncapitalize(entity.getSimpleName())+"Id");
//            identifier.getAnnotations().add("@Id");
//            identifier.getAnnotations().add("@GeneratedValue(strategy = GenerationType.AUTO)");
//            entity.getAttributes().add(identifier);
//        }
//
//        MemoryFile entityCode = ComponentVmUtil.createModelCode(component,entity);
//        StringBuilder fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(entityCode.fileName);
//        Path filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,entityCode.content);
//        component.getEntities().add(entity);
//        saveMetaData(component);
//        //生成接口、服务、rest和rest Proxy
//        if(entity.isRoot()){
//            createInterfaceRelCodes(component);
//        }
//        return filePath.toString();
//    }

//    private static void createInterfaceRelCodes(ComponentDefinition component) throws Exception{
//        List<EntityDefinition> entities = new ArrayList<>();
//        EntityDefinition rootEntity =EntityUtil.getRootEntity(component);
//        entities.add(rootEntity);
//        //repository
//        MemoryFile repositoryCode = ComponentVmUtil.createRepositoryCode(component,rootEntity);
//        StringBuilder fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(repositoryCode.fileName);
//        Path filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,repositoryCode.content);
//        //command api
//        createCommandApi(component);
//        //query api
//        createQueryApi(component);
//        //service
//        createCommandService(component);
//        //queryService
//        createQueryService(component);
//        //service test
//        //service test code
//        MemoryFile testServiceCode = ComponentVmUtil.createTestCode(component,rootEntity,entities);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testServiceCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testServiceCode.content);
//        //service test app java
//        MemoryFile testServiceAppCode = ComponentVmUtil.createAppCode(component,rootEntity);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testServiceAppCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testServiceAppCode.content);
//        //service test app properties
//        MemoryFile testServiceAppFileMainCode = ComponentVmUtil.createApplicationFile(component,rootEntity);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testServiceAppFileMainCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testServiceAppFileMainCode.content);
//        //rest
//        createRestController(component);
//        //rest test code
//        MemoryFile testRestCode = ComponentVmUtil.createRestTestCode(component,entities);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testRestCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testRestCode.content);
//        //rest test app java
//        MemoryFile testRestAppCode = ComponentVmUtil.createRestAppCode(component,rootEntity);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testRestAppCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testRestAppCode.content);
//        //rest test app properties
//        MemoryFile testRestAppFileCode = ComponentVmUtil.createRestApplicationFile(component,rootEntity);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(testRestAppFileCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,testRestAppFileCode.content);
//        //rest proxy
//        createCommandRestProxy(component);
//        //query rest proxy
//        createQueryRestProxy(component);
//        //rest configration
//        MemoryFile restConfigCode = ComponentVmUtil.createRestConfigrationCode(component,entities,entities);
//        fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(restConfigCode.fileName);
//        filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,restConfigCode.content);
//    }
//
//    public static String createCommandApi(ComponentDefinition component) throws Exception{
//        //command api
//        MemoryFile apiCode = ComponentVmUtil.createApiCode(component,EntityUtil.getRootEntity(component));
//        StringBuilder fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(apiCode.fileName);
//        Path filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,apiCode.content);
//        return fileName.toString();
//    }
//
//    public static String createQueryApi(ComponentDefinition component) throws Exception{
//        MemoryFile queryApiCode = ComponentVmUtil.createQueryApiCode(component,EntityUtil.getRootEntity(component));
//        StringBuilder fileName= getPackagePath(component)
//                .append(File.separator)
//                .append(queryApiCode.fileName);
//        Path filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,queryApiCode.content);
//        return fileName.toString();
//    }
//
//    public static String createCommandService(ComponentDefinition component) throws Exception{
//        MemoryFile serviceCode = ComponentVmUtil.createServiceCode(component,EntityUtil.getRootEntity(component));
//        StringBuilder fileName = getPackagePath(component)
//                .append(File.separator)
//                .append(serviceCode.fileName);
//        Path filePath = Paths.get(fileName.toString());
//        if(filePath.getParent() != null) {
//            Files.createDirectories(filePath.getParent());
//        }
//        Files.write(filePath,serviceCode.content);
//        return fileName.toString();
//    }

    public static String createQueryService(ComponentDefinition component) throws Exception{
        MemoryFile queryServiceCode = ComponentVmUtil.createQueryServiceCode(component,EntityUtil.getRootEntity(component));
        StringBuilder fileName = getPackagePath(component)
                .append(File.separator)
                .append(queryServiceCode.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,queryServiceCode.content);
        return fileName.toString();
    }

    public static String getRestControllerPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-rest/src/main/java/" + EntityUtil.getComponentPathName(component) + "/rest";
    }

    public static String getRestProxyPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-rest-proxy/src/main/java/" + EntityUtil.getComponentPathName(component) + "/rest/proxy";
    }

    public static String getModelPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-model/src/main/java/" + EntityUtil.getComponentPathName(component) + "/model";
    }

    public static String getApiPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-api/src/main/java/" + EntityUtil.getComponentPathName(component) + "/api";
    }

    public static String getServicePath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-service/src/main/java/" + EntityUtil.getComponentPathName(component) + "/service";
    }

    public static String getRepositoryPath(ComponentDefinition component){
        return  getServicePath(component)+ "/repository";
    }

    public static String getServiceUnitTestPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-service/src/test/java/" + EntityUtil.getComponentPathName(component) + "/service";
    }

    public static String getTestAppPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-service/src/test/java/" + EntityUtil.getComponentPathName(component);
    }

    public static String getTestAppPropPath(ComponentDefinition component){
        return  ComponentVmUtil.getArtifactPrefix(component) + "-service/src/test/resources";
    }

    public static List<String> getAbstractEntityFields(){
        List<String> retList =new ArrayList<>();
        try {
            Class aCls = Class.forName(AbstractEntity.class.getName());
            Field[] fields = aCls.getDeclaredFields();
            for(Field field : fields){
                retList.add(field.getName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return retList;
    }

    public static JSONObject generatePsiClassJson(Project project,PsiClass psiClass,JSONObject jsonObject) throws Exception{
        if(jsonObject==null){
            jsonObject = new JSONObject();
        }
        PsiClassType[] extendsList = psiClass.getExtendsListTypes();
        for(PsiClassType extendsType : extendsList){
            PsiClass extendsClass = JavaPsiFacade.getInstance(project).findClass(extendsType.getCanonicalText(), GlobalSearchScope.allScope(project));
            if(null!=extendsClass){
                jsonObject = generatePsiClassJson(project,extendsClass,jsonObject);
            }
        }
        for(PsiField field : psiClass.getFields()){
            if(field.getModifierList().hasModifierProperty("final")){
                continue;
            }
            PsiType fieldType = field.getType();
            boolean hasJsonIgnoreAnnotation = false;
            for(PsiAnnotation annotation : field.getAnnotations()){
                if(annotation.getQualifiedName().equals(JsonIgnore.class.getName())
                ||annotation.getQualifiedName().equals(JsonBackReference.class.getName())){
                    hasJsonIgnoreAnnotation=true;
                    break;
                }
            }
            if(hasJsonIgnoreAnnotation){
                continue;
            }

            if(fieldType instanceof PsiClassType){
                if(PsiPrimitiveType.getUnboxedType(field.getType()) instanceof PsiPrimitiveType
                || field.getType().getCanonicalText().equals(String.class.getName())
                        || field.getType().getCanonicalText().equals(Date.class.getName())
                        | field.getType().getCanonicalText().equals(Timestamp.class.getName())){
                    jsonObject = generatePrimitiveField(fieldType.getPresentableText(),field.getName(),jsonObject);
                }else if (fieldType.getCanonicalText().startsWith(List.class.getName())
                        || fieldType.getCanonicalText().equals(Set.class.getName())){
                    PsiType psiType1 = PsiUtil.extractIterableTypeParameter(fieldType, false);
                    PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(psiType1.getCanonicalText(), GlobalSearchScope.allScope(project));
                    JSONArray array = new JSONArray();
                    if (null != fieldClass) {
                        JSONObject fieldJson = generatePsiClassJson(project, fieldClass, null);
                        array.put(fieldJson);
                    }
                    jsonObject.put(field.getName(), array);
                }else{
                    PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(fieldType.getCanonicalText(), GlobalSearchScope.allScope(project));
                    if (null != fieldClass) {
                        JSONObject fieldJson = generatePsiClassJson(project, fieldClass, null);
                        jsonObject.put(field.getName(), fieldJson);
                    }
                }
            }else{
                jsonObject = generatePrimitiveField(fieldType.getPresentableText(),field.getName(),jsonObject);
            }
        }
        return jsonObject;
    }

    public static JSONObject generatePrimitiveField(String type,String name,JSONObject jsonObject){
        if(null==jsonObject){
            jsonObject = new JSONObject();
        }
        if(type.equals(Long.class.getSimpleName()) || type.equals(long.class.getSimpleName())){
            jsonObject.put(name,0L);
        }else if (type.equals(Integer.class.getSimpleName()) || type.equals(int.class.getSimpleName())){
            jsonObject.put(name,0);
        }else if (type.equals(Double.class.getSimpleName()) || type.equals(double.class.getSimpleName())){
            jsonObject.put(name,0);
        }else if (type.equals(Float.class.getSimpleName()) || type.equals(float.class.getSimpleName())){
            jsonObject.put(name,0);
        }else if (type.equals(Boolean.class.getSimpleName()) || type.equals(boolean.class.getSimpleName())) {
            jsonObject.put(name,false);
        }else if (type.equals(Date.class.getSimpleName())) {
            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            jsonObject.put(name,formatter.format(date));
        }else if (type.equals(Timestamp.class.getSimpleName())) {
            jsonObject.put(name,new Date().getTime());
        }else {
            jsonObject.put(name, "");
        }

        return jsonObject;
    }
}
