package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.CreateEntityFromDBTableDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.util.CamelCaseStringUtil;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateEntityFromDBTableAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
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
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            CreateEntityFromDBTableDialog dialog = new CreateEntityFromDBTableDialog(component);
            if (dialog.showAndGet()) {
               int[] selectedRows = dialog.getDbTableTable().getSelectedRows();
               for(int selectedRow : selectedRows){
                   String tableName = (String) dialog.getDbTableTable().getValueAt(selectedRow, 1);
                   String entityName = StringUtils.capitalise(CamelCaseStringUtil.underScore2Camel(tableName,true));
                   DBConnectProp dbConnectProp = component.getDbConnectProp();
                   String dbUrl = dbConnectProp.getDbUrl();
                   String dbUserName = dbConnectProp.getDbUserName();
                   String dbPassword = dbConnectProp.getDbPassword();
                   List<Column> columns = DBMetaDataUtil.getTableColumns(dbUrl, dbUserName, dbPassword, tableName);
                   WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                       @Override
                       public void run() {
                           PsiClass psiClass = EntityCreator.createEntity(project, component,entityName,tableName,null);
                           if(null!=columns && !columns.isEmpty()){
                               EntityCreator.createPsiClassFieldsFromTableColumn(project,psiClass,columns,component);
                           }
                       }
                   });
               }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
