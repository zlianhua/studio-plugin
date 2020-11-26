package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.ComponentDialog;
import com.ai.abc.studio.plugin.util.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class MakeAsComponentAction extends AnAction {
    private Project project;
    @Override
    public void update(@NotNull AnActionEvent e) {
        project = e.getData(PlatformDataKeys.PROJECT);
        boolean enable = ComponentCreator.isAbcComponentProject(project)?false:true;
        e.getPresentation().setEnabledAndVisible(enable);
    }
    @Override
    public void actionPerformed(AnActionEvent e) {
        ComponentDialog componentDialog = new ComponentDialog();
        componentDialog.getProjectDirectoryTextField().setText(project.getBasePath().replaceFirst("/"+project.getName(),""));
        componentDialog.getNameTextField().setText(project.getName());
        if (componentDialog.showAndGet()) {
            String componentName = StringUtils.capitalize(componentDialog.getNameTextField().getText());
            ComponentDefinition component = new ComponentDefinition();
            component.setProjectDirectory(componentDialog.getProjectDirectoryTextField().getText());
            component.setBasePackageName(componentDialog.getGroupTextField().getText());
            component.setSimpleName(componentName);
            component.setName(componentDialog.getGroupTextField().getText()+"."+componentName);
            component.setVersion(componentDialog.getVersionTextField().getText());
            component.setLogicalDelete(componentDialog.getIsLogicalDeleteCheckBox().isSelected());
            component.setExtendsAbstractEntity(componentDialog.getIsExtendBaseEntityCheckBox().isSelected());
            component.setAuditable(componentDialog.getIsAuditableCheckBox().isSelected());
            DBConnectProp dbConnectProp = new DBConnectProp();
            dbConnectProp.setDbUrl(componentDialog.getDbUrlTextField().getText());
            dbConnectProp.setDbUserName(componentDialog.getDbUserTextField().getText());
            dbConnectProp.setDbPassword(componentDialog.getDbPasswordTextField().getText());
            component.setDbConnectProp(dbConnectProp);
            try {
                ComponentCreator.createAbcDirectory(component);
            } catch (Exception exception) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"生成业务构件元数据出现错误");
                exception.printStackTrace();
            }
        }
    }
}
