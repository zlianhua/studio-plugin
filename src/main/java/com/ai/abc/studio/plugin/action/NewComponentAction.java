package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.*;
import com.intellij.openapi.actionSystem.*;
import com.ai.abc.studio.plugin.dialog.*;

import com.intellij.openapi.project.impl.ProjectManagerImpl;
import org.springframework.util.StringUtils;

import java.io.File;

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
                exception.printStackTrace();
            }
        }
    }
}
