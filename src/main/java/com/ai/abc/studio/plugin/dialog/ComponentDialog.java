package com.ai.abc.studio.plugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
@Getter
@Setter
public class ComponentDialog extends DialogWrapper {
    private JTextField projectDirectoryTextField = new JTextField("C:/test");
    private JTextField groupTextField = new JTextField("com.ai.bss");
    private JTextField nameTextField = new JTextField("Test");
    private JTextField descTextField = new JTextField("测试");
    private JTextField versionTextField = new JTextField("1.0-SNAPSHOT");
    private JCheckBox isLogicalDeleteCheckBox = new JCheckBox("是否逻辑删除",true);
    private JCheckBox isExtendBaseEntityCheckBox = new JCheckBox("是否继承基础对象",true);
    private JCheckBox isAuditableCheckBox = new JCheckBox("是否生成审计（历史表）",true);

    public ComponentDialog() {
        super(true); // use current window as parent
        init();
        setTitle(null!= nameTextField.getText()? nameTextField.getText():""+"构件信息");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("工程目录:"),projectDirectoryTextField,1,false)
                .addLabeledComponent(new JLabel("构件分组(基础包名):"),groupTextField,1,false)
                .addLabeledComponent(new JLabel("构件名称:"), nameTextField,1,false)
                .addLabeledComponent(new JLabel("构件描述:"), descTextField,1,false)
                .addLabeledComponent(new JLabel("版本号:"),versionTextField,1,false)
                .addComponent(isLogicalDeleteCheckBox)
                .addComponent(isExtendBaseEntityCheckBox)
                .addComponent(isAuditableCheckBox)
                .getPanel();
        return dialogPanel;
    }
}
