package com.zc.dal.plugin.encryption.interceptor;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zc.dal.plugin.encryption.config.EncryptionConfig;
import com.zc.dal.plugin.encryption.config.MapperConfigModel;
import com.zc.dal.plugin.encryption.utils.ReflectionUtils;
import com.zc.dal.plugin.encryption.utils.SpringBeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import java.util.regex.Pattern;


@Intercepts
        ({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class EncodeDecodeFieldInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(EncodeDecodeFieldInterceptor.class);

    private static final String ENCRYPTION_COLUMN_SUFFIX = "_encrypt";

    static HashMap<String, MapperConfigModel> mapperConfigurationMap = new HashMap<String, MapperConfigModel>();

    @Autowired
    private EncryptionConfig encryptionConfig;

    @PostConstruct
    public void loadEncryptionConfig() {
        try {
            logger.info("start init config");
            String encodeColumnConfig = encryptionConfig.getEncodeMapperConfig();
            if (StringUtils.isEmpty(encodeColumnConfig)) {
                logger.warn("encodeColumnConfig is empty");
            } else {
                mapperConfigurationMap = JSON.parseObject(encodeColumnConfig, new TypeReference<HashMap<String, MapperConfigModel>>() {
                });
            }
        } catch (Exception e) {
            logger.error("load config error:", e);
        }
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            long start = System.currentTimeMillis();

            // 全局加密开关
            if (!encryptionConfig.getEncryptionAllSwitch()) {
                return invocation.proceed();
            }

            Object target = invocation.getTarget();
            if (Proxy.isProxyClass(target.getClass())) { // 防止多级代理对象，被执行多次，只对真实对象做处理
                return invocation.proceed();
            }

            if (target instanceof Executor) {
                logger.debug("Executor:{}", target.getClass().getName());
                MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
                String mapperName = getMapperName(mappedStatement);

                MapperConfigModel mapperConfigModel = mapperConfigurationMap.get(mapperName);
                if (mapperConfigModel != null) {
                    // 加密总开关
                    if (!mapperConfigModel.getEncryptionSwitch()) {
                        return invocation.proceed();
                    }
                    // 加密读开关
                    if (!mapperConfigModel.getReadEncryptionSwitch()) {
                        return invocation.proceed();
                    }
                    // 排除的方法
                    if (mapperConfigModel.getExcludeMapperIds() != null && mapperConfigModel.getExcludeMapperIds().contains(mappedStatement.getId())) {
                        return invocation.proceed();
                    }

                    HashMap<String, String> columnsMap = mapperConfigModel.getColumnsMap();
                    if (CollectionUtils.isEmpty(columnsMap)) {
                        return invocation.proceed();
                    }
                    // 对查询结果的resultMapping进行配置
                    List<ResultMap> resultMapsNew = new ArrayList<ResultMap>();
                    boolean flag = false;
                    for (ResultMap resultMap : mappedStatement.getResultMaps()) {
                        List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
                        for (ResultMapping resultMapping : resultMap.getResultMappings()) {
                            if (columnsMap.containsKey(resultMapping.getColumn())) {
                                // 设置typeHandler、设置列名 因为MappedStatement是个全局共享对象，并且 ResultMapping 一旦被改变下次不会自动恢复所以要重新构建一个 ResultMapping
                                resultMapping = buildFromResultMapping(resultMapping, getTypeHandler(columnsMap.get(resultMapping.getColumn())));
                                flag = true;
                            }
                            resultMappings.add(resultMapping);
                        }
                        if (flag) {
                            // 由于 mappedColumns 是个不可操作对象，所以要用resultMapping重新构建一个ResultMap
                            ResultMap.Builder builder = new ResultMap.Builder(mappedStatement.getConfiguration(), resultMap.getId(), resultMap.getType(), resultMappings);
                            builder.discriminator(resultMap.getDiscriminator());
                            ResultMap resultMapNew = builder.build();
                            resultMapsNew.add(resultMapNew);
                        }
                    }

                    // 如果resultMap未发现目标列则使用原resultMaps进行sql修改组装
                    if (!flag) {
                        resultMapsNew = mappedStatement.getResultMaps();
                    }

                    StringBuilder generatedSql = new StringBuilder();
                    Object parameterObject = invocation.getArgs()[1];
                    BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
                    String originalSql = boundSql.getSql();
                    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
                    String[] sqlss = originalSql.split("[w|W][h|H][e|E][r|R][e|E]");
                    if (sqlss.length == 1 || sqlss.length == 2) {
                        String selectSql = sqlss[0];
                        Set<String> columns = columnsMap.keySet();
                        for (String column : columns) {
                            // 如果查询的字段里有目标字段并且没有加密字段则加上加密字段
                            if (StringUtils.containsIgnoreCase(selectSql, column) && !StringUtils.containsIgnoreCase(selectSql, column + ENCRYPTION_COLUMN_SUFFIX)) {
                                selectSql = selectSql.replaceAll("(?i)" + column + "(?![a-zA-Z0-9_])", column + ENCRYPTION_COLUMN_SUFFIX);
                            }
                            if (!flag && StringUtils.containsIgnoreCase(originalSql, column)) { //用于标识这个语句需要被替换
                                flag = true;
                            }
                        }
                        generatedSql.append(selectSql);
                        if (sqlss.length == 2) {
                            String whereSql = sqlss[1];
                            int count = StringUtils.countMatches(selectSql, "?");
                            whereSql = handlerWhereSql(originalSql, whereSql, parameterMappings, columnsMap, count);
                            generatedSql.append(" where ").append(whereSql);
                        }

                    } else {
                        logger.error("select sql nonsupport,sql:{}", originalSql);
                    }

                    if (flag) {
                        MappedStatement mappedStatementNew = buildFromMappedStatement(mappedStatement, resultMapsNew, boundSql);
                        MetaObject metaObject = SystemMetaObject.forObject(mappedStatementNew);
                        metaObject.setValue("sqlSource.boundSql.sql", generatedSql.toString());
                        metaObject.setValue("sqlSource.boundSql.parameterMappings", parameterMappings);
                        invocation.getArgs()[0] = mappedStatementNew;
                    }
                }

            } else if (target instanceof StatementHandler) {
                logger.debug("StatementHandler:{}", target.getClass().getName());
                StatementHandler statementHandler = (StatementHandler) target;
                MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
                MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
                String mapperName = getMapperName(mappedStatement);
                MapperConfigModel mapperConfigModel = mapperConfigurationMap.get(mapperName);
                if (mapperConfigModel != null) {
                    // 加密总开关
                    if (!mapperConfigModel.getEncryptionSwitch()) {
                        return invocation.proceed();
                    }
                    // 排除的方法
                    if (mapperConfigModel.getExcludeMapperIds() != null && mapperConfigModel.getExcludeMapperIds().contains(mappedStatement.getId())) {
                        return invocation.proceed();
                    }
                    HashMap<String, String> columnsMap = mapperConfigModel.getColumnsMap();
                    if (CollectionUtils.isEmpty(columnsMap)) {
                        return invocation.proceed();
                    }
                    // 获取 BoundSql 对象，此对象包含生成的sql和sql的参数map映射
                    BoundSql boundSql = statementHandler.getBoundSql();
                    String originalSql = boundSql.getSql();
                    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

                    SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
                    if (SqlCommandType.INSERT.equals(sqlCommandType)) {
                        String generatedSql = handlerParameterInsertSql(originalSql, parameterMappings, mapperConfigModel);
                        if (!StringUtils.equals(originalSql, generatedSql)) {
                            metaObject.setValue("delegate.boundSql.sql", generatedSql);
                        }
                    } else if ((SqlCommandType.UPDATE.equals(sqlCommandType)) || (SqlCommandType.DELETE.equals(sqlCommandType))) {
                        String[] sqlss = originalSql.split("[w|W][h|H][e|E][r|R][e|E]");
                        boolean flag = false;
                        if (sqlss.length == 1 || sqlss.length == 2) {
                            String modifierSql = sqlss[0];
                            if (!SqlCommandType.DELETE.equals(sqlCommandType)) {
                                modifierSql = handlerParameterUpdateSql(modifierSql, parameterMappings, mapperConfigModel, mapperName);
                                if (!StringUtils.equals(sqlss[0], modifierSql)) {
                                    flag = true;
                                }
                            }

                            StringBuilder generatedSql = new StringBuilder(modifierSql);
                            if (sqlss.length == 2) {
                                String whereSql = sqlss[1];
                                if (mapperConfigModel.getReadEncryptionSwitch()) {
                                    int count = StringUtils.countMatches(modifierSql, "?");
                                    whereSql = handlerWhereSql(originalSql, whereSql, parameterMappings, columnsMap, count);
                                }
                                if (!StringUtils.equals(sqlss[1], whereSql)) {
                                    flag = true;
                                }
                                generatedSql.append(" where ").append(whereSql);
                            }
                            if (flag) {
                                metaObject.setValue("delegate.boundSql.sql", generatedSql.toString());
                            }

                        } else {
                            logger.error("sql nonsupport,sql:{}", originalSql);
                        }
                    }
                }

            }
            logger.info("plugin cost time :{}ms", (System.currentTimeMillis() - start));
        } catch (Exception e) {
            logger.error("EncodeDecodeFieldInterceptor intercept error:", e);
        }

        return invocation.proceed();

    }

    private String getMapperName(MappedStatement mappedStatement) {
        String[] split = mappedStatement.getId().split("\\.");
        return split[split.length - 2];
    }

    /**
     * @param resultMapping
     * @param typeHandler
     * @return
     */
    private ResultMapping buildFromResultMapping(ResultMapping resultMapping, TypeHandler<?> typeHandler) {
        Configuration configuration = (Configuration) ReflectionUtils.getFieldValue(resultMapping, "configuration");
        ResultMapping.Builder builder = new ResultMapping.Builder(configuration, resultMapping.getProperty(), resultMapping.getColumn() + ENCRYPTION_COLUMN_SUFFIX, typeHandler);
        builder.javaType(resultMapping.getJavaType());
        builder.jdbcType(resultMapping.getJdbcType());
        builder.nestedResultMapId(resultMapping.getNestedResultMapId());
        builder.nestedQueryId(resultMapping.getNestedQueryId());
        builder.resultSet(resultMapping.getResultSet());
        builder.foreignColumn(resultMapping.getForeignColumn());
        builder.notNullColumns(resultMapping.getNotNullColumns());
        builder.columnPrefix(resultMapping.getColumnPrefix());
        builder.flags(resultMapping.getFlags());
        builder.composites(resultMapping.getComposites());
        return builder.build();
    }

    /**
     * @param ms
     * @param resultMaps
     * @return
     */
    private MappedStatement buildFromMappedStatement(MappedStatement ms, List<ResultMap> resultMaps, BoundSql boundSql) {
        BoundSqlSource sqlSource = new BoundSqlSource(boundSql);
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), sqlSource,
                ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(resultMaps);
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    /**
     * @param parameterMapping
     * @param typeHandler
     * @return
     */
    private ParameterMapping buildFromParameterMapping(ParameterMapping parameterMapping, TypeHandler<?> typeHandler) {
        Configuration configuration = (Configuration) ReflectionUtils.getFieldValue(parameterMapping, "configuration");
        ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, parameterMapping.getProperty(), typeHandler);
        builder.mode(parameterMapping.getMode());
        builder.javaType(parameterMapping.getJavaType());
        builder.jdbcTypeName(parameterMapping.getJdbcTypeName());
        builder.resultMapId(parameterMapping.getResultMapId());
        builder.expression(parameterMapping.getExpression());
        builder.numericScale(parameterMapping.getNumericScale());
        builder.jdbcType(parameterMapping.getJdbcType());
        return builder.build();
    }


    /**
     * 处理where 查询
     *
     * @param whereSql
     * @param parameterMappings
     * @param columnsMap
     * @param parameterStartIndex
     * @return
     * @throws Exception
     */
    private String handlerWhereSql(String originalSql, String whereSql, List<ParameterMapping> parameterMappings, HashMap<String, String> columnsMap, int parameterStartIndex) throws Exception {
        SchemaStatVisitor schemaStatVisitor = getSchemaStatVisitor(originalSql);
        List<String> conditions = getConditionParams(schemaStatVisitor);
        if (!CollectionUtils.isEmpty(conditions)) {
            Set<String> searchColumns = columnsMap.keySet();
            for (int i = 0; i < conditions.size(); i++) {
                String column = conditions.get(i);
                for (String searchColumn : searchColumns) {
                    if (StringUtils.equalsIgnoreCase(column, searchColumn)) {
                        whereSql = whereSql.replaceFirst("(?i)" + column + "(?![a-zA-Z0-9_])", column + ENCRYPTION_COLUMN_SUFFIX);
                        ParameterMapping parameterMapping = parameterMappings.get(i + parameterStartIndex);
                        ReflectionUtils.setFieldValue(parameterMapping, "typeHandler", getTypeHandler(columnsMap.get(column)));
                        break;
                    }
                }
            }
        }

        return whereSql;
    }

    /**
     * @param beanName
     * @return
     */
    private TypeHandler<?> getTypeHandler(String beanName) {
        if (StringUtils.isBlank(beanName)) {
            return (TypeHandler<?>) SpringBeanUtil.getBean("commonEncryptTypeHandler");
        } else {
            return (TypeHandler<?>) SpringBeanUtil.getBean(beanName);
        }
    }

    /**
     * 处理写入 insert sql 语句
     *
     * @param insertSql
     * @param parameterMappings
     * @param mapperConfigModel
     * @return
     * @throws Exception
     */
    private String handlerParameterInsertSql(String insertSql, List<ParameterMapping> parameterMappings, MapperConfigModel mapperConfigModel) throws Exception {
        HashMap<String, String> columnsMaps = mapperConfigModel.getColumnsMap();
        SchemaStatVisitor schemaStatVisitor = getSchemaStatVisitor(insertSql);
        List<String> allColumns = getColumnNames(schemaStatVisitor);
        if (CollectionUtils.isEmpty(allColumns)) {
            logger.error("insert columns is empty");
            return insertSql;
        }
        String[] sqlss = insertSql.split("[\\s][v|V][a|A][l|L][u|U][e|E][s|S]");
        String[] strs = sqlss[1].split(",");
        ArrayList<String> paramsColumns = new ArrayList<>();
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].contains("?")) {
                paramsColumns.add(allColumns.get(i % (allColumns.size())));
            }
        }


        Set<String> searchColumns = columnsMaps.keySet();
        String originInsertSql = sqlss[0] + " ";
        int paramIndex = 0;
        for (String column : paramsColumns) {
            for (String searchColumn : searchColumns) {
                if (StringUtils.equalsIgnoreCase(searchColumn, column)) {
                    if (mapperConfigModel.getOnlyWriteEncryptionSwitch()) {// 单写密文字段 (?i) 不分区分大小写匹配
                        if (!Pattern.compile("(?i)" + column + ENCRYPTION_COLUMN_SUFFIX).matcher(insertSql).find()) {
                            insertSql = insertSql.replaceFirst("(?i)" + column + "(?![a-zA-Z0-9_])", column + ENCRYPTION_COLUMN_SUFFIX);
                        }
                        if (!Pattern.compile("(?i)" + column + ENCRYPTION_COLUMN_SUFFIX).matcher(originInsertSql).find()) {
                            ParameterMapping parameterMapping = parameterMappings.get(paramIndex);
                            ReflectionUtils.setFieldValue(parameterMapping, "typeHandler", getTypeHandler(columnsMaps.get(column)));
                        }
                    } else { // 双写(明文+密文)
                        if (!Pattern.compile("(?i)" + column + ENCRYPTION_COLUMN_SUFFIX).matcher(insertSql).find()) {
                            insertSql = insertSql.replaceFirst("(?i)" + column + "(?![a-zA-Z0-9_])", column + " ," + column + ENCRYPTION_COLUMN_SUFFIX);
                        }
                        if (!Pattern.compile("(?i)" + column + ENCRYPTION_COLUMN_SUFFIX).matcher(originInsertSql).find()) {
                            insertSql = replaceCountChars(insertSql, paramIndex + 1);
                            ParameterMapping parameterMapping = parameterMappings.get(paramIndex);
                            ParameterMapping parameterMappingNew = buildFromParameterMapping(parameterMapping, getTypeHandler(columnsMaps.get(column)));
                            parameterMappings.add(++paramIndex, parameterMappingNew);
                        }
                    }
                    break;
                }
            }
            paramIndex++;
        }
        return insertSql;
    }

    /**
     * 替换第几个?
     *
     * @param str
     * @param num
     * @return
     */
    private String replaceCountChars(String str, int num) {
        StringBuilder stringBuilder = new StringBuilder(str);
        char[] chars = str.toCharArray();
        int count = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '?') {
                count++;
                if (count == num) {
                    stringBuilder.insert(i + 1, new char[]{' ', ',', '?'});
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 处理写入sql语句 （update 参数赋值）
     *
     * @param modifierSql
     * @param parameterMappings
     * @param mapperConfigModel
     * @return
     * @throws Exception
     */
    private String handlerParameterUpdateSql(String modifierSql, List<ParameterMapping> parameterMappings, MapperConfigModel mapperConfigModel, String mapperName) throws Exception {
        String[] sqls = modifierSql.replaceAll("[\\s]+", "").split("\\?");
        HashMap<String, String> columnsMaps = mapperConfigModel.getColumnsMap();
        Set<String> columns = columnsMaps.keySet();
        int paramIndex = 0;
        for (int i = 0; i < sqls.length; i++) {
            String sqlSlice = sqls[i];
            for (String column : columns) {
                // todo 如果上层已经对加密字段进行明确赋值则不用再处理?
                if (StringUtils.containsIgnoreCase(sqlSlice, column + "=") && !Pattern.compile("(?i)" + column + ENCRYPTION_COLUMN_SUFFIX + "([\\s]+=|=)").matcher(modifierSql).find()) {
                    if (mapperConfigModel.getOnlyWriteEncryptionSwitch()) {// 单写密文字段 (?i) 不分区分大小写匹配
                        modifierSql = modifierSql.replaceFirst("(?i)" + column + "([\\s]+=|=)", column + ENCRYPTION_COLUMN_SUFFIX + " =");
                        ParameterMapping parameterMapping = parameterMappings.get(paramIndex);
                        ReflectionUtils.setFieldValue(parameterMapping, "typeHandler", getTypeHandler(columnsMaps.get(column)));
                    } else { // 双写(明文+密文)
                        modifierSql = modifierSql.replaceFirst("(?i)" + column + "([\\s]+=|=)", column + " = ? ," + column + ENCRYPTION_COLUMN_SUFFIX + " =");
                        ParameterMapping parameterMapping = parameterMappings.get(paramIndex);
                        ParameterMapping parameterMappingNew = buildFromParameterMapping(parameterMapping, getTypeHandler(columnsMaps.get(column)));
                        parameterMappings.add(++paramIndex, parameterMappingNew);
                    }
                    break;
                }
            }
            paramIndex++;
        }
        return modifierSql;
    }


    /**
     * getSchemaStatVisitor
     *
     * @param sql
     * @return
     */
    private SchemaStatVisitor getSchemaStatVisitor(String sql) {
        final String dbType = JdbcConstants.MYSQL; // 可以是ORACLE、POSTGRESQL、SQLSERVER、ODPS等
        sql = sql.replaceAll("\\?", "'?'");//替换?
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, dbType);
        SchemaStatVisitor statVisitor = SQLUtils.createSchemaStatVisitor(stmtList, dbType);
        for (SQLStatement sqlStatement : stmtList) {
            sqlStatement.accept(statVisitor);
        }
        return statVisitor;
    }

    /**
     * 获取 条件参数列表
     *
     * @param statVisitor
     * @return
     */
    private List<String> getConditionParams(SchemaStatVisitor statVisitor) {
        List<String> conditionsParams = new ArrayList<String>();
        List<TableStat.Condition> conditions = statVisitor.getConditions();
        for (TableStat.Condition condition : conditions) {
            List<Object> values = condition.getValues();
            if (!CollectionUtils.isEmpty(values)) {
                for (Object value : values) {
                    // 查找含有?项为预编译参数占位符
                    if (StringUtils.equals("?", String.valueOf(value))) {
                        conditionsParams.add(condition.getColumn().getName());
                    }
                }
            }
        }
        return conditionsParams;
    }


    /**
     * 获取 所有列名
     *
     * @param statVisitor
     * @return
     */
    private List<String> getColumnNames(SchemaStatVisitor statVisitor) {
        List<String> columnNames = new ArrayList<String>();
        Collection<TableStat.Column> columns = statVisitor.getColumns();
        for (TableStat.Column column : columns) {
            columnNames.add(column.getName());
        }
        return columnNames;
    }

    private class BoundSqlSource implements SqlSource {
        private BoundSql boundSql;

        private BoundSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}





