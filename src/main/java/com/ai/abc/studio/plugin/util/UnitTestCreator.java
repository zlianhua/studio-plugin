package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.EntityDefinition;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import org.springframework.util.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class UnitTestCreator {
    public static PsiClass createUnitTest(Project project, ComponentDefinition component, PsiClass apiClass, AnActionEvent e) throws Exception{
        String apiServiceName = apiClass.getName();
        String apiServiceNameVar = StringUtils.uncapitalize(apiServiceName);
        String rootEntityName = apiServiceName.replace("Query","");
        rootEntityName = apiServiceName.replace("Command","");
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path testPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getServiceUnitTestPath(component));
        VirtualFile unitTestVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(testPath);
        if(null==unitTestVirtualFile){
            createUnitTestPath(component);
            unitTestVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(testPath);
        }
        PsiFile[] files = FilenameIndex.getFilesByName(project,apiServiceName+"Test.java", ProjectScope.getProjectScope(project));
        if(null!=files && files.length>0){
            JComponent source = PsJavaFileHelper.getDialogSource(e);
            if (Messages.showConfirmationDialog(source,
                    rootEntityName + "Repository" + "已经存在，是否覆盖已有对象？",
                    "Repository已经存在",
                    "覆盖",
                    "取消") == Messages.NO) {
                return null;
            }else{
                //delete
                files[0].delete();
            }
        }

        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");

        List<String> classImports = new ArrayList<>();
        classImports.add("org.junit.Test");
        classImports.add("org.junit.runner.RunWith");
        classImports.add("org.springframework.beans.factory.annotation.Autowired");
        classImports.add("org.springframework.boot.test.context.SpringBootTest");
        classImports.add("org.springframework.test.context.junit4.SpringRunner");
        classImports.add("lombok.extern.slf4j.Slf4j");
        classImports.add("com.ai.abc.util.JsonUtils");
        classImports.add("com.fasterxml.jackson.databind.ObjectMapper");

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Slf4j");
        classAnnotations.add("@RunWith(SpringRunner.class)");
        classAnnotations.add("@SpringBootTest");

        PsiClass unitTestClass = PsJavaFileHelper.createPsiClass(project,unitTestVirtualFile,apiServiceName+"Test",packageImports,classImports,classAnnotations,null);

        PsiType apiType = new PsiJavaParserFacadeImpl(project).createTypeFromText(apiServiceName,null);
        List<String> annotations = new ArrayList<>();
        annotations.add("@Autowired");
        PsJavaFileHelper.addField(unitTestClass,apiServiceNameVar,null,apiType,annotations);

        PsiType mapperType = new PsiJavaParserFacadeImpl(project).createTypeFromText("ObjectMapper",null);
        PsJavaFileHelper.addField(unitTestClass,"mapper","new ObjectMapper()",mapperType,null);

        StringBuilder entityJsonPath = new StringBuilder().append("/").append(rootEntityName).append(".json");
        PsiMethod[] apiMethods = apiClass.getMethods();
        PsiMethod[] testMethods = unitTestClass.getMethods();
        if(null!=apiMethods){
            for(PsiMethod apiMethod : apiMethods){
                boolean hasMethod=false;
                if(null!=testMethods){
                    for(PsiMethod testMethod : testMethods){
                        if(testMethod.getName().substring("test".length(),testMethod.getName().length()).equals(StringUtils.capitalize(apiMethod.getName()))){
                            hasMethod = true;
                            break;
                        }
                    }
                }
                if(hasMethod){
                    continue;
                }
                StringBuilder methodStr = new StringBuilder();
                String jsonObjName = StringUtils.uncapitalize(rootEntityName)+"Json";
                String responseType = apiMethod.getReturnType().getPresentableText();
                methodStr.append("public void test")
                        .append(StringUtils.capitalize(apiMethod.getName()))
                        .append("() throws Throwable {\n");
                if(responseType.contains("CommonResponse<")){
                    String requestType = apiMethod.getParameterList().getParameter(0).getType().getPresentableText();
                    methodStr.append("    String").append(" ").append(jsonObjName)
                            .append(" = JsonUtils.loadStringFromResourceFile(\"").append(entityJsonPath.toString()).append("\");\n")
                            .append("    ").append(rootEntityName).append(" ").append(StringUtils.uncapitalize(rootEntityName)).append(" = ").append("mapper.readValue(").append(jsonObjName).append(",").append(rootEntityName).append(".class);\n")
                            .append("    ").append(requestType).append(" request = new CommonRequest<>(").append(StringUtils.uncapitalize(rootEntityName)).append(");\n");
                    if(responseType.equals("void")){
                        methodStr.append("    ").append(apiServiceNameVar).append(".").append(apiMethod.getName()).append("(request);\n");
                    }else{
                        methodStr.append("    ").append(responseType).append(" response = ").append(apiServiceNameVar).append(".").append(apiMethod.getName()).append("(request);\n")
                                .append("    log.info(\"result:\",JsonUtils.toJSONStringByDateFormat(response,true));\n");
                    }
                }else{
                    JvmParameter[] parameters= apiMethod.getParameters();
                    String paramValueList="";
                    for(JvmParameter parameter : parameters) {
                        String paramName = parameter.getName();
                        String paramType = parameter.getType().toString();
                        paramType = paramType.replace("PsiType:", "");
                        methodStr.append("    ").append(paramType).append(" ").append(paramName).append(" = null;\n");
                        paramValueList+=paramName+",";
                    }
                    if(paramValueList.endsWith(",")){
                        paramValueList=paramValueList.substring(0,paramValueList.length()-1);
                    }
                    if(responseType.equals("void")){
                        methodStr.append("    ").append(apiServiceNameVar).append(".").append(apiMethod.getName()).append("(").append(paramValueList).append(");\n");
                    }else{
                        methodStr.append("    ").append(responseType).append(" response = ").append(apiServiceNameVar).append(".").append(apiMethod.getName()).append("(").append(paramValueList).append(");\n")
                                .append("    log.info(\"result:\",JsonUtils.toJSONStringByDateFormat(response,true));\n");
                    }
                }
                methodStr.append("}\n");
                PsiMethod method = elementFactory.createMethodFromText(methodStr.toString(),unitTestClass);
                method.getModifierList().addAnnotation("Test");
                unitTestClass.add(method);
            }
        }
        return unitTestClass;
    }

    public static void createUnitTestPath(ComponentDefinition component) throws Exception{
        StringBuilder restPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase());
        File rest = new File(restPath.toString());
        if(!rest.exists()){
            rest.mkdirs();
        }
        StringBuilder restSrcPkgPath = ComponentCreator.componentTestSrcPkg(component,restPath)
                .append(File.separator)
                .append("service");
        File restSrcPkg = new File(restSrcPkgPath.toString());
        if(!restSrcPkg.exists()){
            restSrcPkg.mkdirs();
        }
    }

    public static void createTestResourcesPath(ComponentDefinition component){
        StringBuilder testPath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase());
        File test = new File(testPath.toString());
        if(!test.exists()){
            test.mkdirs();
        }
        StringBuilder testResPkgPath = ComponentCreator.componentTestResourcesPkg(component,testPath);
        File testResPkg = new File(testResPkgPath.toString());
        if(!testResPkg.exists()){
            testResPkg.mkdirs();
        }
    }

    public static PsiClass createTestApp(Project project, ComponentDefinition component) throws Exception{
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path testPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getTestAppPath(component));
        VirtualFile testAppVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(testPath);
        if(null==testAppVirtualFile){
            createUnitTestPath(component);
            testAppVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(testPath);
        }
        String testAppName = "Test"+component.getSimpleName()+"App";
        PsiFile[] files = FilenameIndex.getFilesByName(project,testAppName+".java", ProjectScope.getProjectScope(project));
        if(null!=files && files.length>0){
            return null;
        }

        List<String> packageImports = new ArrayList<>();
        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());
        classImports.add("org.springframework.boot.SpringApplication");
        classImports.add("org.springframework.boot.autoconfigure.SpringBootApplication");
        classImports.add("org.springframework.data.jpa.repository.config.EnableJpaAuditing");

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@SpringBootApplication(scanBasePackages = {\""+component.getBasePackageName()+"\"})");

        PsiClass testAppClass = PsJavaFileHelper.createPsiClass(project,testAppVirtualFile,testAppName,packageImports,classImports,classAnnotations,null);
        StringBuilder methodStr=new StringBuilder();
        methodStr.append("public static void main(String[] args) throws Exception {\n")
                .append("    SpringApplication.run(").append(testAppName).append(".class, args);\n")
                .append("}\n");
        PsiMethod method = elementFactory.createMethodFromText(methodStr.toString(),testAppClass);
        testAppClass.add(method);
        return testAppClass;
    }

    public static void createTestAppPropFile(Project project,ComponentDefinition component){
        Path testAppPropPath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getTestAppPropPath(component));
        VirtualFile testAppPropVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(testAppPropPath);
        if(null==testAppPropVirtualFile){
            createTestResourcesPath(component);
        }
        String testAppPropName = "application.properties";
        String fullAppPropName = testAppPropPath.toString()+"/"+testAppPropName;
        PsiFile[] files = FilenameIndex.getFilesByName(project,testAppPropName, ProjectScope.getProjectScope(project));
        if(null!=files && files.length>0){
            return;
        }
        EntityDefinition entity = new EntityDefinition();
        entity.setSimpleName(component.getSimpleName());
        try {
            MemoryFile appFile= ComponentVmUtil.createApplicationFile(component,entity);
            Path filePath = Paths.get(fullAppPropName);
            Files.write(filePath,appFile.content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
