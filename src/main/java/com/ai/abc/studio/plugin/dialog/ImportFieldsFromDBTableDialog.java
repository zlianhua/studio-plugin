package com.ai.abc.studio.plugin.dialog;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Table;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
@Getter
@Setter
public class ImportFieldsFromDBTableDialog extends DialogWrapper {
    private JBTable dbTableTable =new JBTable();
    private ComponentDefinition component;
    public ImportFieldsFromDBTableDialog(ComponentDefinition component,String entityName) {
        super(true); // use current window as parent
        this.component = component;
        init();
        String title = "从数据库表导入属性";
        if(null!= entityName){
            title=entityName+":"+title;
        }
        setTitle(title);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        DBConnectProp dbConnectProp =  component.getDbConnectProp();
        String dbUrl = dbConnectProp.getDbUrl();
        String dbUserName = dbConnectProp.getDbUserName();
        String dbPassword = dbConnectProp.getDbPassword();
        JPanel dialogPanel = null;
        try {
            List<Table> tables = DBMetaDataUtil.getTables(dbUrl,dbUserName,dbPassword);
            if(null!=tables && !tables.isEmpty()){
                dbTableTable.setName("dbTableTable");
                dbTableTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                dbTableTable.setModel(this.createTableModel(tables));
                dbTableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                dbTableTable.getTableHeader().setReorderingAllowed(true);
                dbTableTable.setRowSelectionAllowed(true);
                dbTableTable.setRowSelectionInterval(0, 0);
                dbTableTable.getColumnModel().getColumn(0).setPreferredWidth(50);
                dbTableTable.getColumnModel().getColumn(1).setPreferredWidth(200);
                dbTableTable.getColumnModel().getColumn(2).setPreferredWidth(200);
                dbTableTable.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        if (e.getClickCount() == 2){
                            Component component = (Component) e.getSource();
                            if(component.getName().equalsIgnoreCase("dbTableTable")){
                                if(((JBTable)component).getSelectedRow()>0){
                                    doOKAction();
                                }
                            }
                        }
                    }
                });
            }
            JTextPane textPanel = new JTextPane();
            textPanel.setText("请选择合适的表");
            dialogPanel = FormBuilder.createFormBuilder()
                    .addComponent(textPanel)
                    .addComponent(new JScrollPane((dbTableTable)))
                    .getPanel();
            dialogPanel.setPreferredSize(new Dimension(450, 400));
        } catch (Exception e) {
            Messages.showErrorDialog(e.getMessage(),"出错啦");
            e.printStackTrace();
        }
        return dialogPanel;
    }

    private TableModel createTableModel(List<Table> dataList){
        //headers for the table
        String[] columns = new String[] {
                "序号", "表名", "备注"
        };

        //actual data for the table in a 2d array
        Object[][] data = new Object[dataList.size()][3];
        int count=0;
        for(Table table : dataList){
            data[count][0] = count+1;
            data[count][1] = table.getTableName();
            data[count][2] = table.getTableCode();
            count++;
        }

        final Class[] columnClass = new Class[] {
                Integer.class, String.class, String.class
        };
        //create table model with data
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex)
            {
                return columnClass[columnIndex];
            }
        };
        return model;
    }


}
