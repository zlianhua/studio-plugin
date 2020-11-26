package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.ComponentDBConfigDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class ComponentDBConfigAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(e.getProject());
            if(null==component){
                e.getPresentation().setEnabledAndVisible(false);
            }
        } catch (Exception exception) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(e.getProject());
            DBConnectProp dbConnectProp = component.getDbConnectProp();
            ComponentDBConfigDialog componentDialog = new ComponentDBConfigDialog();
            componentDialog.getDbUrlTextField().setText(dbConnectProp.getDbUrl());
            componentDialog.getDbUserTextField().setText(dbConnectProp.getDbUserName());
            componentDialog.getDbPasswordTextField().setText(dbConnectProp.getDbPassword());
            if (componentDialog.showAndGet()) {
                dbConnectProp.setDbUrl(componentDialog.getDbUrlTextField().getText());
                dbConnectProp.setDbUserName(componentDialog.getDbUserTextField().getText());
                dbConnectProp.setDbPassword(componentDialog.getDbPasswordTextField().getText());
                component.setDbConnectProp(dbConnectProp);
                ComponentCreator.saveMetaData(component);
            }
        } catch (Exception exception) {
            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"配置数据库连接信息出现错误");
            exception.printStackTrace();
        }
    }
}
