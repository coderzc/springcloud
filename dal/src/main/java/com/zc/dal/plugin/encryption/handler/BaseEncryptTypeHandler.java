package com.zc.dal.plugin.encryption.handler;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@Component("baseEncryptTypeHandler")
public class BaseEncryptTypeHandler extends BaseTypeHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(BaseEncryptTypeHandler.class);

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, String parameter, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, encrypt(String.valueOf(parameter)));
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        String columnValue = resultSet.getString(columnName);
        return decrypt(columnValue);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        String columnValue = resultSet.getString(columnIndex);
        return decrypt(columnValue);
    }

    @Override
    public String getNullableResult(CallableStatement callableStatement, int columnIndex) throws SQLException {
        String columnValue = callableStatement.getString(columnIndex);
        return decrypt(columnValue);
    }

    private String encrypt(String content) {
        if (StringUtils.isBlank(content))
            return content;
        try {
            return Base64Utils.encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("encrypt error:", e);
        }

        return content;
    }

    private String decrypt(String content) {
        if (StringUtils.isBlank(content))
            return content;
        try {
            return new String(Base64Utils.decodeFromString(content), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("decrypt error:", e);
        }

        return content;
    }
}
