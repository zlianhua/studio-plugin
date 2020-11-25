package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.ImportFieldsFromDBTableDialog;
import com.ai.abc.studio.plugin.util.ComponentCreator;
import com.ai.abc.studio.plugin.util.EntityCreator;
import com.ai.abc.studio.util.DBMetaDataUtil;
import com.ai.abc.studio.util.pdm.Column;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ImportFieldsFromDBTableAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        //获取当前类文件的路径
        String classPath = virtualFile.getPath();
        String modelPackageStarts = project.getBasePath();
        String modelPackageEnds = project.getName().toLowerCase() + "/model/";
        boolean enable = false;
        if ((classPath.contains(modelPackageStarts)) && classPath.contains(modelPackageEnds) && virtualFile.getFileType().getName().equalsIgnoreCase("java")) {
            enable = true;
        }
        e.getPresentation().setEnabledAndVisible(enable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiElement element = PsiUtilBase.getElementAtCaret(editor);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        try {
            ComponentDefinition component = ComponentCreator.loadComponent(project);
            ImportFieldsFromDBTableDialog dialog = new ImportFieldsFromDBTableDialog(component, psiClass.getName());
            if (dialog.showAndGet()) {
                int selectedRow = dialog.getDbTableTable().getSelectedRow();
                if (selectedRow > 0) {
                    String tableName = (String) dialog.getDbTableTable().getValueAt(selectedRow, 1);
                    DBConnectProp dbConnectProp = component.getDbConnectProp();
                    String dbUrl = dbConnectProp.getDbUrl();
                    String dbUserName = dbConnectProp.getDbUserName();
                    String dbPassword = dbConnectProp.getDbPassword();
                    List<Column> columns = DBMetaDataUtil.getTableColumns(dbUrl, dbUserName, dbPassword, tableName);
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {
                            EntityCreator.createPsiClassFieldsFromTableColumn(project, psiClass, columns, component);
                        }
                    });
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
