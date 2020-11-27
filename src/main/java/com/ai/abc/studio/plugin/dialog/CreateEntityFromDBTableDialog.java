package com.ai.abc.studio.plugin.dialog;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Table;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ui.FormBuilder;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.tomcat.util.ExceptionUtils;
import org.thymeleaf.util.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
@Getter
@Setter
public class CreateEntityFromDBTableDialog extends DialogWrapper {
    private JBTable dbTableTable =new JBTable();
    JTextField searchText = new JTextField();
    private ComponentDefinition component;
    private TableRowSorter<TableModel> rowSorter;
    public CreateEntityFromDBTableDialog(ComponentDefinition component) {
        super(true); // use current window as parent
        this.component = component;
        init();
        String title = "选择数据库表生成实体对象";
        setTitle(title);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        DBConnectProp dbConnectProp =  component.getDbConnectProp();
        boolean dbConnectPropConfiged = false;
        if(null==dbConnectProp
                || StringUtils.isEmpty(dbConnectProp.getDbUrl())
                || StringUtils.isEmpty(dbConnectProp.getDbUserName() )
                || StringUtils.isEmpty(dbConnectProp.getDbPassword())){
            ComponentDBConfigDialog componentDialog = new ComponentDBConfigDialog();
            if (componentDialog.showAndGet()) {
                try {
                    dbConnectProp = new DBConnectProp();
                    dbConnectProp.setDbUrl(componentDialog.getDbUrlTextField().getText());
                    dbConnectProp.setDbUserName(componentDialog.getDbUserTextField().getText());
                    dbConnectProp.setDbPassword(componentDialog.getDbPasswordTextField().getText());
                    if(null!=dbConnectProp.getDbUrl()
                            && null!=dbConnectProp.getDbUserName()
                            && null!=dbConnectProp.getDbPassword()){
                        component.setDbConnectProp(dbConnectProp);
                        ComponentCreator.saveMetaData(component);
                        dbConnectPropConfiged = true;
                    }else{
                        Messages.showInfoMessage("数据库连接信息为空！", "未设置数据库连接信息");
                    }
                } catch (Exception e) {
                    Messages.showErrorDialog(e.getMessage(), "保存构件数据库连接信息出错");
                    e.printStackTrace();
                }
            }
        }else{
            dbConnectPropConfiged = true;
        }

        if(dbConnectPropConfiged){
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
                    dbTableTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    dbTableTable.getTableHeader().setReorderingAllowed(true);
                    dbTableTable.setRowSelectionAllowed(true);
                    dbTableTable.setRowSelectionInterval(0, 0);
                    dbTableTable.getColumnModel().getColumn(0).setPreferredWidth(50);
                    dbTableTable.getColumnModel().getColumn(1).setPreferredWidth(200);
                    dbTableTable.getColumnModel().getColumn(2).setPreferredWidth(100);
                    dbTableTable.getColumnModel().getColumn(3).setPreferredWidth(50);
                    rowSorter = new TableRowSorter<>(dbTableTable.getModel());
                    dbTableTable.setRowSorter(rowSorter);
                    searchText.getDocument().addDocumentListener(new DocumentListener(){
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            String text = searchText.getText();

                            if (text.trim().length() == 0) {
                                rowSorter.setRowFilter(null);
                            } else {
                                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                            }
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            String text = searchText.getText();

                            if (text.trim().length() == 0) {
                                rowSorter.setRowFilter(null);
                            } else {
                                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                            }
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                        }
                    });
                }
                JTextPane textPanel = new JTextPane();
                textPanel.setText("请勾选需要生成实体对象的表");
                dialogPanel = FormBuilder.createFormBuilder()
                        .addComponent(textPanel)
                        .addLabeledComponent(new JLabel("请输入表名检索条件:"),searchText)
                        .addComponent(new JScrollPane((dbTableTable)))
                        .getPanel();
                dialogPanel.setPreferredSize(new Dimension(400, 400));
            } catch (Exception e) {
                Messages.showErrorDialog(ExceptionUtil.getMessage(e),"出错啦");
                e.printStackTrace();
            }
            return dialogPanel;
        }
        return null;
    }

    private TableModel createTableModel(List<Table> dataList){
        //headers for the table
        String[] columns = new String[] {
                "序号", "表名", "备注","是否根对象"
        };

        //actual data for the table in a 2d array
        Object[][] data = new Object[dataList.size()][4];
        int count=0;
        for(Table table : dataList){
            String tableName = table.getTableName();
            if(!tableName.toLowerCase().endsWith("$seq")){
                data[count][0] = count+1;
                data[count][1] = tableName;
                data[count][2] = table.getTableCode();
                data[count][3] = false;
                count++;
            }
        }

        final Class[] columnClass = new Class[] {
                Integer.class, String.class, String.class, Boolean.class
        };
        //create table model with data
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column==3;
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
