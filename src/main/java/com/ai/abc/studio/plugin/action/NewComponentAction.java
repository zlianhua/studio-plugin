package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.intellij.openapi.actionSystem.*;
import com.ai.abc.studio.plugin.dialog.*;

import com.intellij.openapi.project.impl.ProjectManagerImpl;

import java.io.File;

public class NewComponentAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        ComponentDialog componentDialog = new ComponentDialog();
        if (componentDialog.showAndGet()) {
            ComponentDefinition component = new ComponentDefinition();
            component.setProjectDirectory(componentDialog.getProjectDirectoryTextField().getText());
            component.setBasePackageName(componentDialog.getGroupTextField().getText());
            component.setSimpleName(componentDialog.getNameTextField().getText());
            component.setName(componentDialog.getGroupTextField().getText()+"."+componentDialog.getDescTextField().getText());
            component.setVersion(componentDialog.getVersionTextField().getText());
            component.setLogicalDelete(componentDialog.getIsLogicalDeleteCheckBox().isSelected());
            component.setExtendsAbstractEntity(componentDialog.getIsExtendBaseEntityCheckBox().isSelected());
            component.setAuditable(componentDialog.getIsAuditableCheckBox().isSelected());
            try {
                FileCreateHelper.createAbcDirectory(component);
                FileCreateHelper.createModelModule(component);
                FileCreateHelper.createServiceModule(component);
                FileCreateHelper.createApiModule(component);
                FileCreateHelper.createRestModule(component);
                FileCreateHelper.createRestProxyModule(component);
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
