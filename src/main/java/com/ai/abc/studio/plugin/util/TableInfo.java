package com.ai.abc.studio.plugin.util;

import com.ai.abc.studio.util.pdm.Column;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TableInfo {
    private String tableName;
    private String desc;
    private Column pkColumn;
    private boolean isAbstract;
    private String entityType;
    private TableInfo parentTableInfo;
    private List<String> children = new ArrayList<>();
}
