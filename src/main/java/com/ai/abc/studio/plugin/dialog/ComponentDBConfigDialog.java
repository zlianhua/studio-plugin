package com.ai.abc.studio.plugin.dialog;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Getter
@Setter
public class ComponentDBConfigDialog extends DialogWrapper {
    private JTextField dbUrlTextField = new JTextField("jdbc:mysql://10.19.14.28:3306/bmgnew?useUnicode=true&ampcharacterEncoding=utf-8&useSSL=false");
    private JTextField dbUserTextField = new JTextField("aibp");
    private JTextField dbPasswordTextField = new JTextField("Aibp@123");

    public ComponentDBConfigDialog() {
        super(true); // use current window as parent
        init();
        setTitle("构件实体导入数据源设置");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("数据连接URL:"), dbUrlTextField,1,false)
                .addLabeledComponent(new JLabel("数据连接用户名:"), dbUserTextField,1,false)
                .addLabeledComponent(new JLabel("数据连接密码:"), dbPasswordTextField,1,false)
                .getPanel();
        return dialogPanel;
    }
}
