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
public class ComponentDialog extends DialogWrapper {
    private JTextField projectDirectoryTextField = new JTextField("C:/test");
    private TextFieldWithBrowseButton projectDirectoryTextFieldWithButton = new TextFieldWithBrowseButton(projectDirectoryTextField, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            VirtualFile[] file = FileChooser.chooseFiles(new FileChooserDescriptor(false, true, false, false, false, false), projectDirectoryTextFieldWithButton.getTextField(), null, null);
            if(file.length > 0) {
                projectDirectoryTextFieldWithButton.getTextField().setText(file[0].getPath());
            }
        }
    });
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
                .addLabeledComponent(new JLabel("工程目录:"),projectDirectoryTextFieldWithButton,1,false)
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
