package com.ai.abc.studio.plugin.dialog;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import org.jdesktop.swingx.JXComboBox;
import javax.swing.*;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
@Getter
public class CreateCommandMethodDialog extends DialogWrapper {
    private JXComboBox rootEntityNameCBX;
    private JTextField methodName = new JTextField();
    private String[] rootEntityNames;
    public CreateCommandMethodDialog(String[] rootEntityNames) {
        super(true); // use current window as parent
        this.rootEntityNames = rootEntityNames;
        rootEntityNameCBX = new JXComboBox(this.rootEntityNames);
        init();
        setTitle("生成CommandService方法");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JLabel("选择根对象:"),rootEntityNameCBX)
                .addLabeledComponent(new JLabel("方法名:"),methodName,1,false)
                .getPanel();
        return dialogPanel;
    }
}
