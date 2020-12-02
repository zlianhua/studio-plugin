package com.ai.abc.studio.plugin.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TableInfo {
    private String tableName;
    private String desc;
    private boolean isAbstract;
    private String entityType;
    private TableInfo parentTableInfo;
    private List<String> children = new ArrayList<>();
}
