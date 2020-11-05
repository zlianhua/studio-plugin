package com.ai.abc.studio.plugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
public class NewSingleEntityDialog extends DialogWrapper {
    private JTextField nameTextField = new JTextField();
    private JTextField descTextField = new JTextField();
    private JCheckBox isOneToManyCheckBox = new JCheckBox("属主对象拥有多个本对象",false);
    private JCheckBox isRootCheckBox = new JCheckBox("根对象",false);
    private JCheckBox isAbstractCheckBox = new JCheckBox("抽象类",false);
    private JCheckBox isValueCheckBox = new JCheckBox("值对象",false);

    public NewSingleEntityDialog() {
        super(true); // use current window as parent
        init();
        setTitle(null!= nameTextField.getText()? nameTextField.getText():""+"对象信息");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("名称:"),nameTextField,1,false)
                .addLabeledComponent(new JLabel("描述:"),descTextField,1,false)
                .addComponent(isRootCheckBox)
                .addComponent(isAbstractCheckBox)
                .addComponent(isValueCheckBox)
                .addComponent(isOneToManyCheckBox)
                .getPanel();
        return dialogPanel;
    }
}
