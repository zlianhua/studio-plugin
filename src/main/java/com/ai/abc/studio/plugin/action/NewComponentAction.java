package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.*;
import com.intellij.openapi.actionSystem.*;
import com.ai.abc.studio.plugin.dialog.*;

import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ExceptionUtil;
import org.springframework.util.StringUtils;

import java.io.File;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class NewComponentAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        ComponentDialog componentDialog = new ComponentDialog();
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
            component.setTablePrefix(componentDialog.getTableNamePrefix().getText());
            component.setInheritanceStrategy(ComponentDefinition.InheritanceStrategy.valueOf((String)componentDialog.getInheritanceStrategy().getSelectedItem()));
            DBConnectProp dbConnectProp = new DBConnectProp();
            dbConnectProp.setDbUrl(componentDialog.getDbUrlTextField().getText());
            dbConnectProp.setDbUserName(componentDialog.getDbUserTextField().getText());
            dbConnectProp.setDbPassword(componentDialog.getDbPasswordTextField().getText());
            component.setDbConnectProp(dbConnectProp);
            try {
                ComponentCreator.createAbcDirectory(component);
                ComponentCreator.createMainPom(component);
                EntityCreator.createModelModule(component);
                ServiceCreator.createServiceModule(component);
                ApiClassCreator.createApiModule(component);
                RestControllerCreator.createRestModule(component);
                RestProxyCreator.createRestProxyModule(component);
                StringBuilder fileName = new StringBuilder()
                        .append(component.getProjectDirectory())
                        .append(File.separator)
                        .append(component.getSimpleName());
                ProjectManagerImpl.getInstanceEx().loadAndOpenProject(fileName.toString());
            } catch (Exception exception) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"新建业务构建项目出现错误");
                exception.printStackTrace();
            }
        }
    }
}
