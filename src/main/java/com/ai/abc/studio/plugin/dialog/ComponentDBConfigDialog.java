package com.ai.abc.studio.plugin.dialog;


import com.ai.abc.studio.model.ComponentDefinition;
import com.intellij.openapi.ui.DialogWrapper;

import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;

/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
@Getter
@Setter
public class ComponentDBConfigDialog extends DialogWrapper {
    private JTextField tableNamePrefix = new JTextField("");
    private JComboBox<String> inheritanceStrategy;
    private JTextField dbUrlTextField = new JTextField("jdbc:mysql://10.19.14.28:3306/bmgnew?useUnicode=true&ampcharacterEncoding=utf-8&useSSL=false");
    private JTextField dbUserTextField = new JTextField("aibp");
    private JTextField dbPasswordTextField = new JTextField("Aibp@123");

    public ComponentDBConfigDialog() {
        super(true); // use current window as parent
        inheritanceStrategy = new JXComboBox();
        inheritanceStrategy.addItem(ComponentDefinition.InheritanceStrategy.SINGLE_TABLE.name());
        inheritanceStrategy.addItem(ComponentDefinition.InheritanceStrategy.SECONDARY_TABLE.name());
        inheritanceStrategy.addItem(ComponentDefinition.InheritanceStrategy.TABLE_PER_CLASS.name());
        init();
        setTitle("构件实体导入数据源设置");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("表名前缀:"),tableNamePrefix,1,false)
                .addLabeledComponent(new JLabel("继承策略:"),inheritanceStrategy,1,false)
                .addLabeledComponent(new JLabel("数据连接URL:"), dbUrlTextField,1,false)
                .addLabeledComponent(new JLabel("数据连接用户名:"), dbUserTextField,1,false)
                .addLabeledComponent(new JLabel("数据连接密码:"), dbPasswordTextField,1,false)
                .getPanel();
        return dialogPanel;
    }
}
