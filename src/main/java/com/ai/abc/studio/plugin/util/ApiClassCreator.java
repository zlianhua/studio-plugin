package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.util.EntityUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.impl.file.PsiDirectoryFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApiClassCreator {
    public static PsiClass createApiClass(Project project, ComponentDefinition component, String className){
        Path apiPath = Paths.get(project.getBasePath()+ File.separator+ FileCreateHelper.getApiPath(component));
        List<String> packageImports = new ArrayList<>();
        packageImports.add(EntityUtil.getComponentPackageName(component)+".model");
        packageImports.add("java.util");
        packageImports.add("com.ai.abc.api.model");
        PsiClass apiClass = PsJavaFileHelper.createInterface(project,apiPath,className,packageImports,null,null);
        return apiClass;
    }

    public static void addMethodToInterfaceClass(PsiClass psiClass, PsiMethod[] methods, PsiElementFactory elementFactory, JavaCodeStyleManager codeStyleManager){
        for(PsiMethod method : methods){
            if(null!=psiClass.findMethodBySignature(method,true)){
                continue;
            }
            StringBuilder methodStr = new StringBuilder();
            methodStr.append(method.getReturnType().getPresentableText())
                    .append(" ")
                    .append(method.getName())
                    .append("(");
            JvmParameter[] parameters= method.getParameters();
            for(JvmParameter parameter : parameters){
                methodStr.append(parameter.getType().toString().replace("PsiType:", ""))
                        .append(" ")
                        .append(parameter.getName())
                        .append(",");
            }
            if(methodStr.toString().endsWith(",")){
                methodStr.deleteCharAt(methodStr.lastIndexOf(","));
            }
            methodStr.append(")");
            PsiReferenceList throwList = method.getThrowsList();
            if(null!=throwList && throwList.getRole().equals(PsiReferenceList.Role.THROWS_LIST)){
                PsiJavaCodeReferenceElement [] throwEles = throwList.getReferenceElements();

                if(null!=throwEles && throwEles.length>0){
                    methodStr.append(" throws ");
                    for(PsiJavaCodeReferenceElement element : throwEles){
                        methodStr.append(element.getReferenceName())
                                .append(",");
                    }
                }
                if(methodStr.toString().endsWith(",")){
                    methodStr.deleteCharAt(methodStr.lastIndexOf(","));
                }
            }
            methodStr.append(";");
            psiClass.add(elementFactory.createMethodFromText(methodStr.toString(),psiClass));
        }
        PsiJavaFile file = (PsiJavaFile)psiClass.getContainingFile();
        codeStyleManager.shortenClassReferences(file);
    }
}
