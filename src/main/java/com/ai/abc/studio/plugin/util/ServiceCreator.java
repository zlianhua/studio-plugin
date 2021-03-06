package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.util.ComponentVmUtil;
import com.ai.abc.studio.util.EntityUtil;
import com.ai.abc.studio.util.MemoryFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class ServiceCreator {
    public static void createServicePath(ComponentDefinition component){
        StringBuilder servicePath = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(component.getSimpleName().toLowerCase())
                .append("-service".toLowerCase());
        File service = new File(servicePath.toString());
        if (!service.exists()) {
            service.mkdirs();
        }
        StringBuilder serviceSrcPkgPath = ComponentCreator.componentSrcPkg(component, servicePath)
                .append(File.separator)
                .append("service");
        File serviceSrcPkg = new File(serviceSrcPkgPath.toString());
        if (!serviceSrcPkg.exists()) {
            serviceSrcPkg.mkdirs();
        }
    }
    public static void createServiceModule(ComponentDefinition component) throws Exception{
        createServicePath(component);
        MemoryFile servicePom = ComponentVmUtil.createServicePom(component);
        StringBuilder fileName = ComponentCreator.getPackagePath(component)
                .append(File.separator)
                .append(servicePom.fileName);
        Path filePath = Paths.get(fileName.toString());
        if(filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath,servicePom.content);
    }

    public static void createService(Project project,ComponentDefinition component,String rootEntityName) throws Exception{
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        Path servicePath = Paths.get(project.getBasePath()+ File.separator+ ComponentCreator.getServicePath(component));
        VirtualFile serviceVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(servicePath);
        if(null==serviceVirtualFile){
            createServicePath(component);
            serviceVirtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(servicePath);
        }
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add(EntityUtil.getComponentPackageName(component)+".api");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");

        List<String> classImports = new ArrayList<>();
        classImports.add(org.springframework.stereotype.Service.class.getName());

        List<String> classAnnotations = new ArrayList<>();
        classAnnotations.add("@Service");

        PsiPackage psiPackage =  JavaDirectoryService.getInstance().getPackage(PsiManager.getInstance(project).findDirectory(serviceVirtualFile));
        PsiClass command = PsJavaFileHelper.getEntity(psiPackage,rootEntityName+"CommandImpl");
        if(null==command){
            command = PsJavaFileHelper.createPsiClass(project,serviceVirtualFile,rootEntityName+"CommandImpl",packageImports,classImports,classAnnotations,null);
            command.getImplementsList().add(elementFactory.createReferenceFromText(rootEntityName+"Command",command));
        }
        PsiClass query = PsJavaFileHelper.getEntity(psiPackage,rootEntityName+"QueryImpl");
        if(null==query){
            query = PsJavaFileHelper.createPsiClass(project,serviceVirtualFile,rootEntityName+"QueryImpl",packageImports,classImports,classAnnotations,null);
            query.getImplementsList().add(elementFactory.createReferenceFromText(rootEntityName+"Query",query));
        }
    }

    public static void createCommandMethod(Project project, ComponentDefinition component, PsiClass commandServiceCls, String rootEntityName, String methodName, AnActionEvent e) throws Exception{
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiField[] fields = commandServiceCls.getFields();
        boolean hasRootRepository= false;
        if(null!=fields && fields.length>0){
            for(PsiField field : fields){
                if(field.getName().equals(StringUtils.unCapitalize(rootEntityName)+"Repository")){
                    hasRootRepository = true;
                    break;
                }
            }
        }
        if(!hasRootRepository){
            String repositoryPath = EntityUtil.getComponentPackageName(component)+".service.repository."+rootEntityName+"Repository";
            PsiClass repositoryCls = PsJavaFileHelper.findClass(project,repositoryPath);
            if(null==repositoryCls){
                repositoryCls = RepositoryCreator.createRepository(project,component,rootEntityName,e);
            }
            PsiImportStatement importStatement = elementFactory.createImportStatement(repositoryCls);
            PsiJavaFile file = (PsiJavaFile)commandServiceCls.getContainingFile();
            if(null==file.getImportList().findSingleImportStatement(rootEntityName+"Repository")){
                file.getImportList().add(importStatement);
            }
            PsiClass autoWired = PsJavaFileHelper.findClass(project,repositoryPath);
            importStatement = elementFactory.createImportStatement(autoWired);
            if(null==file.getImportList().findSingleImportStatement("Autowired")){
                file.getImportList().add(importStatement);
            }

            PsiField repository = PsJavaFileHelper.findField(commandServiceCls,StringUtils.unCapitalize(rootEntityName)+"Repository");
            if(null==repository){
                List<String> annotations = new ArrayList<>();
                PsiType apiType = new PsiJavaParserFacadeImpl(project).createTypeFromText(rootEntityName+"Repository",null);
                annotations.add("@Autowired");
                PsJavaFileHelper.addField(commandServiceCls,StringUtils.unCapitalize(rootEntityName)+"Repository",null,apiType,annotations,null);
            }
        }
        StringBuilder methodStr = new StringBuilder();
        methodStr.append("public CommonResponse<").append(rootEntityName).append("> ").append(methodName).append("(CommonRequest<").append(rootEntityName).append("> request) throws Exception{\n")
                .append("    ").append(rootEntityName).append(" ").append(StringUtils.unCapitalize(rootEntityName)).append(" = request.getData();\n")
                .append("    ").append(StringUtils.unCapitalize(rootEntityName)).append(" = ").append(StringUtils.unCapitalize(rootEntityName)).append("Repository").append(".save(").append(StringUtils.unCapitalize(rootEntityName)).append(");\n")
                .append("    ").append("return CommonResponse.ok(").append(StringUtils.unCapitalize(rootEntityName)).append(");\n")
                .append("}\n");
        commandServiceCls.add(elementFactory.createMethodFromText(methodStr.toString(),commandServiceCls));

    }
}
