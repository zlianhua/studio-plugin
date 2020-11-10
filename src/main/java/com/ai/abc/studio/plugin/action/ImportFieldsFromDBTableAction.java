package com.ai.abc.studio.plugin.action;

import com.ai.abc.studio.model.ComponentDefinition;
import com.ai.abc.studio.model.DBConnectProp;
import com.ai.abc.studio.plugin.dialog.ImportFieldsFromDBTableDialog;
import com.ai.abc.studio.plugin.file.FileCreateHelper;
import com.ai.abc.studio.plugin.util.PsJavaFileHelper;
import com.ai.abc.studio.util.CamelCaseStringUtil;
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
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
            ComponentDefinition component = FileCreateHelper.loadComponent(project);
            List<String> abstractEntityFieldNames = new ArrayList<>();
            if(component.isExtendsAbstractEntity()){
                abstractEntityFieldNames = FileCreateHelper.getAbstractEntityFields();
            }
            final List<String> ignoreFields = abstractEntityFieldNames;
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
                            if (null != columns && !columns.isEmpty()) {
                                for (Column column : columns) {
                                    String remarks = column.getName();
                                    String columnName = column.getCode();
                                    String fieldName = CamelCaseStringUtil.underScore2Camel(columnName, true);
                                    if(component.isExtendsAbstractEntity() && ignoreFields.contains(fieldName)){
                                        continue;
                                    }
                                    PsiField field = PsJavaFileHelper.findField(psiClass, fieldName);
                                    if (null != field) {
                                        PsJavaFileHelper.deleteField(psiClass, fieldName);
                                    }
                                    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                                    String fieldJavaType = DBMetaDataUtil.columnDataTypeToJavaType(column.getType());
                                    PsiType fieldType = new PsiJavaParserFacadeImpl(psiClass.getProject()).createTypeFromText(fieldJavaType, null);
                                    List<String> annotations = new ArrayList<>();
                                    annotations.add("@Column(name =\"" + columnName.toUpperCase() + "\")");
                                    if (column.isPkFlag()) {
                                        annotations.add("@GeneratedValue(strategy = GenerationType.AUTO)");
                                        annotations.add("@Id");
                                    }
                                    if (column.isClob()) {
                                        annotations.add("@Lob");
                                    }
                                    field = PsJavaFileHelper.addFieldWithAnnotations(psiClass, fieldName, fieldType, annotations);
                                    PsiComment comment = elementFactory.createCommentFromText("/**" + remarks + "*/", null);
                                    field.getModifierList().addBefore(comment, field.getModifierList().getFirstChild());
                                }
                                CodeStyleManager.getInstance(project).reformat(psiClass);
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
