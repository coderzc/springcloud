package com.zc.dal.plugin.encryption.config;

import java.util.HashMap;
import java.util.HashSet;

public class MapperConfigModel {
    private HashMap<String, String> columnsMap;

    private HashSet<String> excludeMapperIds;

    private HashMap<String, String> extraColumnsMap;

    private Boolean encryptionSwitch = false;

    private Boolean readEncryptionSwitch = false;

    private Boolean onlyWriteEncryptionSwitch = false;


    public Boolean getReadEncryptionSwitch() {
        return readEncryptionSwitch;
    }

    public void setReadEncryptionSwitch(Boolean readEncryptionSwitch) {
        this.readEncryptionSwitch = readEncryptionSwitch;
    }

    public Boolean getOnlyWriteEncryptionSwitch() {
        return onlyWriteEncryptionSwitch;
    }

    public void setOnlyWriteEncryptionSwitch(Boolean onlyWriteEncryptionSwitch) {
        this.onlyWriteEncryptionSwitch = onlyWriteEncryptionSwitch;
    }

    public Boolean getEncryptionSwitch() {
        return encryptionSwitch;
    }

    public void setEncryptionSwitch(Boolean encryptionSwitch) {
        this.encryptionSwitch = encryptionSwitch;
    }

    public HashSet<String> getExcludeMapperIds() {
        return excludeMapperIds;
    }

    public void setExcludeMapperIds(HashSet<String> excludeMapperIds) {
        this.excludeMapperIds = excludeMapperIds;
    }

    public HashMap<String, String> getExtraColumnsMap() {
        return extraColumnsMap;
    }

    public void setExtraColumnsMap(HashMap<String, String> extraColumnsMap) {
        this.extraColumnsMap = extraColumnsMap;
    }

    public HashMap<String, String> getColumnsMap() {
        return columnsMap;
    }

    public void setColumnsMap(HashMap<String, String> columnsMap) {
        this.columnsMap = columnsMap;
    }
}
