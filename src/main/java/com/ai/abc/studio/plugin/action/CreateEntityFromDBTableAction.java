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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ExceptionUtil;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            CreateEntityFromDBTableDialog dialog = new CreateEntityFromDBTableDialog(component);
            if (dialog.showAndGet()) {
               int[] selectedRows = dialog.getDbTableTable().getSelectedRows();
               for(int selectedRow : selectedRows){
                   String tableName = (String) dialog.getDbTableTable().getValueAt(selectedRow, 1);
                   String prefix = component.getTablePrefix();
                   if(null!=prefix && !prefix.endsWith("_")){
                       prefix+="_";
                   }
                   String tmpEntityName = tableName;
                   if(!StringUtils.isEmpty(prefix)){
                       tmpEntityName.substring(prefix.length()+1);
                   }
                   tmpEntityName = StringUtils.capitalise(CamelCaseStringUtil.underScore2Camel(tmpEntityName,true));
                   String entityName = tmpEntityName;
                   boolean isRoot = (Boolean)dialog.getDbTableTable().getValueAt(selectedRow, 3);
                   EntityCreator.EntityType entityType;
                   if(isRoot){
                       entityType = EntityCreator.EntityType.RootEntity;
                   }else{
                       entityType = null;
                   }
                   DBConnectProp dbConnectProp = component.getDbConnectProp();
                   String dbUrl = dbConnectProp.getDbUrl();
                   String dbUserName = dbConnectProp.getDbUserName();
                   String dbPassword = dbConnectProp.getDbPassword();
                   List<Column> columns = DBMetaDataUtil.getTableColumns(dbUrl, dbUserName, dbPassword, tableName);
                   WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                       @Override
                       public void run() {
                           try {
                               PsiClass psiClass = EntityCreator.createEntity(project, component,entityName,tableName,entityType);
                               if(null!=columns && !columns.isEmpty()){
                                   EntityCreator.createPsiClassFieldsFromTableColumn(project,psiClass,columns,component);
                               }
                           } catch (Exception exception) {
                               Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"从数据库导入实体出现错误");
                               exception.printStackTrace();
                           }
                       }
                   });
               }
            }
        } catch (Exception exception) {
            Messages.showErrorDialog(ExceptionUtil.getMessage(exception),"从数据库导入实体出现错误");
            exception.printStackTrace();
        }

    }
}
