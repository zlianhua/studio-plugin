package com.ai.abc.studio.plugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
@Getter
@Setter
public class CreateRestProxyDialog extends DialogWrapper {
    private JCheckBox toRestControllerCheckBox = new JCheckBox("RestController",true);
    private JCheckBox toRestProxyCheckBox = new JCheckBox("RestProxy",true);

    public CreateRestProxyDialog() {
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
                .getPanel();
        return dialogPanel;
    }
}
