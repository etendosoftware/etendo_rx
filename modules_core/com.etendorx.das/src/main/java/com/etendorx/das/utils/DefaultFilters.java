package com.etendorx.das.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DefaultFilters {

    public static final String AND = " and ";
    public static final String SELECT = "select";
    public static final String INSERT = "insert into";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String WHERE = " where ";
    public static final String FROM = " from ";
    public static final String SUPER_USER_ID = "100";
    public static final String SUPER_USER_CLIENT_ID = "0";
    public static final String SUPER_USER_ORG_ID = "0";
    public static final String REG_EXP_SELECT = "\\sfrom\\s(\\w*)\\s(\\w*0_)\\s?(where)?";
    public static final String REG_EXP_INSERT = "insert\\sinto\\s(\\w*)";
    public static final String REG_EXP_UPDATE = "update\\s(\\w*)()?.*(where)?"; //NOSONAR
    public static final String REG_EXP_DELETE = "delete\\sfrom\\s(\\w*)\\s(\\w*0_)\\s?(where)?";
    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    public static final String PUT_METHOD = "PUT";
    public static final String PATCH_METHOD = "PATCH";
    public static final String DELETE_METHOD = "DELETE";

    private DefaultFilters() {
        throw new IllegalStateException("Utility class");
    }

    public static String addFilters(String sql, String userId, String clientId, String roleId, boolean isActive,
        String restMethod) {
        // AUTH SERVICE BYPASS FILTERS
        if (isAuthService(userId, clientId)) {
            return sql;
        }
        // SUPERUSER BYPASS FILTERS
        if (isSuperUser(userId, clientId)) {
            return sql;
        }

        switch (restMethod) {
            case GET_METHOD:
                return replaceInQuery(sql, clientId, roleId, isActive, true);
            case DELETE_METHOD:
            case PUT_METHOD:
            case PATCH_METHOD:
                return replaceInQuery(sql, clientId, roleId, isActive, sql.startsWith(SELECT));
            case POST_METHOD:
                if (sql.startsWith(SELECT)) {
                    return replaceInQuery(sql, clientId, roleId, isActive, true);
                } else {
                    return sql;
                }
            default:
                log.error("[ processSql ] - Unknown HTTP method: " + restMethod);
                throw new IllegalArgumentException("Unknown HTTP method: " + restMethod);
        }
    }

    private static List<String> getDefaultWhereClause(String tableAlias, String clientId, String roleId) {
        List<String> conditions = new ArrayList<>();
        conditions.add(String.format("%s.ad_client_id in ('0', '%s')", tableAlias, clientId));
        conditions.add(
            String.format("etrx_role_organizations('%s', '%s', 'r') like concat('%%|', %s.ad_org_id, '|%%')",
                clientId, roleId, tableAlias));
        return conditions;
    }

    private static String applyFilters(String sql, QueryInfo tableInfo, List<String> conditions) {
        String whereClause = String.join(AND, conditions);
        String finalSql;
        String baseLookup;
        if (StringUtils.equals(tableInfo.getSqlAction(), UPDATE)) {
            return sql.replace(WHERE, WHERE + whereClause + AND);
        } else if (StringUtils.equalsAny(tableInfo.getSqlAction(), SELECT, DELETE)) {
            baseLookup = String.format(FROM + "%s %s", tableInfo.getTableName(), tableInfo.getTableAlias());
        } else {
            throw new QueryException("applyFilters ERROR - SQL operation not supported");
        }
        if (tableInfo.isContainsWhere()) {
            String lookup = baseLookup + WHERE;
            finalSql = sql.replace(
                lookup, lookup + whereClause + AND
            );
        } else {
            finalSql = sql.replace(
                baseLookup, baseLookup + WHERE + whereClause
            );
        }
        if (StringUtils.equals(finalSql, sql)) {
            throw new QueryException("applyFilters ERROR - SQL query was not modified");
        }
        return finalSql;
    }

    @NotNull
    private static String replaceInQuery(String sql, String clientId, String roleId, boolean isActive,
        boolean isActiveFilter) {
        QueryInfo tableInfo = getQueryInfo(sql);
        List<String> conditions = getDefaultWhereClause(tableInfo.getTableAlias(), clientId, roleId);
        if (isActiveFilter && tableInfo.isContainsWhere() && isActive) {
            conditions.add(tableInfo.getTableAlias() + ".isactive = 'Y'");
        }
        return applyFilters(sql, tableInfo, conditions);
    }

    private static boolean isSuperUser(String userId, String clientId) {
        return StringUtils.equals(userId, SUPER_USER_ID) &&
            StringUtils.equals(clientId, SUPER_USER_CLIENT_ID);
    }

    // IS_AUTH_SERVICE CONDITION NEEDS IMPROVEMENT
    private static boolean isAuthService(String userId, String clientId) {
        return StringUtils.isEmpty(userId) || StringUtils.isEmpty(clientId);
    }

    private static QueryInfo getQueryInfo(String sql) {
        Pattern qryPattern = null;
        String sqlAction;
        if (sql.startsWith(SELECT)) {
            sqlAction = SELECT;
            qryPattern = Pattern.compile(REG_EXP_SELECT);
        } else if (sql.startsWith(UPDATE)) {
            sqlAction = UPDATE;
            qryPattern = Pattern.compile(REG_EXP_UPDATE);
        } else if (sql.startsWith(INSERT)) {
            sqlAction = INSERT;
            qryPattern = Pattern.compile(REG_EXP_INSERT);
        } else if (sql.startsWith(DELETE)) {
            sqlAction = DELETE;
            qryPattern = Pattern.compile(REG_EXP_DELETE);
        } else {
            throw new QueryException("getQueryInfo ERROR - SQL operation not supported");
        }
        Assert.notNull(qryPattern, "Unknown SQL query type");
        Matcher qryMatcher = qryPattern.matcher(sql);
        if (qryMatcher.find()) {
            String tableName = qryMatcher.group(1);
            String tableAlias = qryMatcher.groupCount() > 1 && !StringUtils.isEmpty(qryMatcher.group(2)) ?
                qryMatcher.group(2) :
                tableName;
            boolean containsWhere = qryMatcher.groupCount() > 2 && qryMatcher.group(3) != null;

            return new QueryInfo(sqlAction, tableName, tableAlias, containsWhere);
        }
        log.error("[getQueryInfo] - PATTERN ERROR");
        throw new QueryException("getQueryInfo ERROR");
    }

    static class QueryInfo {
        private String sqlAction;
        private String tableName;
        private String tableAlias;
        private boolean containsWhere;

        public QueryInfo(String sqlAction, String tableName, String tableAlias, boolean containsWhere) {
            this.sqlAction = sqlAction;
            this.tableName = tableName;
            this.tableAlias = tableAlias;
            this.containsWhere = containsWhere;
        }

        public String getSqlAction() {
            return sqlAction;
        }

        public void setSqlAction(String sqlAction) {
            this.sqlAction = sqlAction;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getTableAlias() {
            return tableAlias;
        }

        public void setTableAlias(String tableAlias) {
            this.tableAlias = tableAlias;
        }

        public boolean isContainsWhere() {
            return containsWhere;
        }

        public void setContainsWhere(boolean containsWhere) {
            this.containsWhere = containsWhere;
        }
    }
}
