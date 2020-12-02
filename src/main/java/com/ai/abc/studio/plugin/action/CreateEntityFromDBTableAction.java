package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.CreateEntityFromDBTableDialog;
import com.ai.abc.studio.plugin.util.*;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Lianhua zhang zhanglh2@asiainfo.com
 * 2020.11
 */
public class CreateEntityFromDBTableAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = null;
        virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if(null==virtualFile){
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts=project.getBasePath();
        String modelPackageEnds=project.getName().toLowerCase()+"/model";
        if((classPath.contains(modelPackageStarts))&&classPath.endsWith(modelPackageEnds)){
            e.getPresentation().setEnabledAndVisible(true);
        }else{
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Map<String,TableInfo> selectedTableInfoMap = new HashMap<>();
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            CreateEntityFromDBTableDialog dialog = new CreateEntityFromDBTableDialog(component);
            if (dialog.showAndGet()) {
                JBTable jbTable = dialog.getDbTableTable();
               int[] selectedRows = jbTable.getSelectedRows();

               for(int selectedRow : selectedRows){
                   TableInfo tableInfo = new TableInfo();
                   tableInfo.setTableName((String) jbTable.getValueAt(selectedRow, 1));
                   tableInfo.setDesc((String)jbTable.getValueAt(selectedRow, 2));
                   tableInfo.setEntityType((String)jbTable.getValueAt(selectedRow, 3));
                   tableInfo.setAbstract((boolean)jbTable.getValueAt(selectedRow, 4));
                   String parentTableName = (String)jbTable.getValueAt(selectedRow, 5);
                   TableInfo parent = selectedTableInfoMap.get(parentTableName);
                   if(null==parent){
                       parent=createTableInfo(parentTableName,jbTable,selectedTableInfoMap);
                       parent.getChildren().add(tableInfo.getTableName());
                   }
                   tableInfo.setParentTableInfo(parent);
                   selectedTableInfoMap.put(tableInfo.getTableName(),tableInfo);
               }
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for(TableInfo tableInfo : selectedTableInfoMap.values()){
                                EntityCreator.createEntityFromTable(project,component,tableInfo,e);
                            }
                        } catch (Exception exception) {
                            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"从数据库导入实体出现错误");
                            exception.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception exception) {
            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"从数据库导入实体出现错误");
            exception.printStackTrace();
        }

    }

    private TableInfo createTableInfo(String tableName, JBTable jbTable,Map<String,TableInfo> selectedTableInfoMap){
        TableInfo tableInfo = null;
        for(int selectedRow=jbTable.getModel().getRowCount()-1;selectedRow>=0;selectedRow--){
            String rowTableName = (String)jbTable.getValueAt(selectedRow, 1);
            if(null!=rowTableName && rowTableName.equalsIgnoreCase(tableName)){
                tableInfo = new TableInfo();
                tableInfo.setTableName((String) jbTable.getValueAt(selectedRow, 1));
                tableInfo.setDesc((String)jbTable.getValueAt(selectedRow, 2));
                tableInfo.setEntityType((String)jbTable.getValueAt(selectedRow, 3));
                tableInfo.setAbstract((boolean)jbTable.getValueAt(selectedRow, 4));
                String parentTableName = (String)jbTable.getValueAt(selectedRow, 5);
                if(!StringUtils.isEmpty(parentTableName)){
                    TableInfo parentTableInfo = selectedTableInfoMap.get(parentTableName);
                    if(null==parentTableInfo){
                        parentTableInfo = createTableInfo(parentTableName,jbTable,selectedTableInfoMap);
                        parentTableInfo.getChildren().add(tableInfo.getTableName());
                    }
                    tableInfo.setParentTableInfo(parentTableInfo);
                }
                selectedTableInfoMap.put(tableInfo.getTableName(),tableInfo);
                return tableInfo;
            }
        }
        return tableInfo;
    }
}
