package com.ai.abc.studio.plugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
public class CreateRestProcyDialog extends DialogWrapper {
    private JCheckBox toRestControllerCheckBox = new JCheckBox("RestController",true);
    private JCheckBox toRestProxyCheckBox = new JCheckBox("RestProxy",true);
    private JCheckBox toBmgProxyCheckBox = new JCheckBox("BmgProxy",false);

    public CreateRestProcyDialog() {
        super(true); // use current window as parent
        init();
        setTitle("选择需要生成的接口文件");

    }
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addComponent(toRestControllerCheckBox)
                .addComponent(toRestProxyCheckBox)
                .addComponent(toBmgProxyCheckBox)
                .getPanel();
        return dialogPanel;
    }
}
