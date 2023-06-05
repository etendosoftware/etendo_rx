package com.etendorx.das.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DefaultFilters {

    public static final String WHERE = " where ";
    public static final String SUPER_USER_ID = "100";
    public static final String SUPER_USER_CLIENT_ID = "0";
    public static final String SUPER_USER_ORG_ID = "0";
    public static final String REG_EXP_SELECT = "\\sfrom\\s\\w+\\s(\\w+_)";
    public static final String REG_EXP_INSERT = "insert into\\s+(\\w+)";
    public static final String REG_EXP_UPDATE = "update\\s+(\\w+)";
    public static final String REG_EXP_DELETE = "delete from\\s+(\\w+)";
    public static final String LIMIT = " limit ";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String PATCH = "PATCH";
    public static final String DELETE = "DELETE";
    public static final String SELECT = "select";
    public static final String INSERT = "insert into";
    public static final String UPDATE = "update";
    public static final String DELETE_FROM = "delete";

    public static String addFilters(String sql, String userId, String clientId, String orgId, String roleId, String isActive, String restMethod) {
        // AUTH SERVICE BYPASS FILTERS
        if (isAuthService(userId, clientId, orgId)) {
            return sql;
        }
        // SUPERUSER BYPASS FILTERS
        if (isSuperUser(userId, clientId, orgId)) {
            return sql;
        }

        boolean containsWhere = sql.contains(WHERE);

        // GET THE TABLE NAME USING REGULAR EXPRESSION
        String tableAlias = getTableAlias(sql);

        checkAccessByRole(sql, userId, clientId, orgId, roleId);

        switch (restMethod) {
            case GET:
                return replaceInQueryForSelect(sql, clientId, orgId, isActive, containsWhere, tableAlias);
            case POST:
                return sql;
            case PUT:
            case PATCH: {
                if(sql.startsWith("update")) {
                    return replaceInQueryForUpdate(sql, clientId, orgId, containsWhere, tableAlias);
                }
                return sql;
            }
            case DELETE:
                if(sql.startsWith("delete")) {
                    return replaceInQueryForDelete(sql, clientId, orgId, containsWhere, tableAlias);
                }
                return sql;
            default:
                log.error("[ processSql ] - Unknown HTTP method: " + restMethod);
                throw new IllegalArgumentException("Unknown HTTP method: " + restMethod);
        }
    }

    @NotNull
    private static String replaceInQueryForSelect(String sql, String clientId, String orgId, String isActive, boolean containsWhere, String tableAlias) {
        return sql.replace((containsWhere ? WHERE : LIMIT), WHERE + tableAlias + ".isactive = '" + isActive + "' " +
                "AND " + tableAlias + ".ad_client_id = '" + clientId + "' " +
                "AND (" + tableAlias + ".ad_org_id = '" + orgId + "' OR ((etrx_is_org_in_org_tree(" + tableAlias + ".ad_org_id, '" + orgId + "', '1')) = 1))" +
                (containsWhere ? " AND " : "") + LIMIT);
    }

    private static String replaceInQueryForUpdate(String sql, String clientId, String orgId, boolean containsWhere, String tableAlias) {
        String whereCondition = tableAlias + ".ad_client_id = '" + clientId + "' " +
                "AND (" + tableAlias + ".ad_org_id = '" + orgId + "' OR ((etrx_is_org_in_org_tree(" + tableAlias + ".ad_org_id, '" + orgId + "', '1')) = 1))";

        if (containsWhere) {
            return sql.replace(WHERE, WHERE + whereCondition + " AND ");
        } else {
            return sql + WHERE + whereCondition;
        }
    }


    private static String replaceInQueryForDelete(String sql, String clientId, String orgId, boolean containsWhere, String tableAlias) {
        String whereCondition = tableAlias + ".ad_client_id = '" + clientId + "' " +
                "AND (" + tableAlias + ".ad_org_id = '" + orgId + "' OR ((etrx_is_org_in_org_tree(" + tableAlias + ".ad_org_id, '" + orgId + "', '1')) = 1))";

        if (containsWhere) {
            return sql.replace(WHERE, WHERE + whereCondition + " AND ");
        } else {
            return sql + " WHERE " + whereCondition;
        }
    }


    private static void checkAccessByRole(String sql, String userId, String clientId, String orgId, String roleId) {
    }

    private static boolean isSuperUser(String userId, String clientId, String orgId) {
        return StringUtils.equals(userId, SUPER_USER_ID) &&
               StringUtils.equals(clientId, SUPER_USER_CLIENT_ID) &&
               StringUtils.equals(orgId, SUPER_USER_ORG_ID);
    }

    // IS_AUTH_SERVICE CONDITION NEEDS IMPROVEMENT
    private static boolean isAuthService(String userId, String clientId, String orgId) {
        return StringUtils.isEmpty(userId) || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(orgId);
    }

    private static String getTableAlias(String sql) {
        Pattern qryPattern = null;
        if (sql.startsWith(SELECT)) {
            qryPattern = Pattern.compile(REG_EXP_SELECT);
        } else if (sql.startsWith(INSERT)) {
            qryPattern = Pattern.compile(REG_EXP_INSERT);
        } else if (sql.startsWith(UPDATE)) {
            qryPattern = Pattern.compile(REG_EXP_UPDATE);
        } else if (sql.startsWith(DELETE_FROM)) {
        qryPattern = Pattern.compile(REG_EXP_DELETE);
        }
        if (qryPattern != null) {
            Matcher qryMatcher = qryPattern.matcher(sql);
            if (qryMatcher.find()) {
                return qryMatcher.group(1);
            }
        }
        log.error("[getTableAlias] - PATTERN ERROR");
        throw new QueryException("getTableAlias ERROR");
    }
}
