package com.etendorx.das.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.StringUtils;
import org.hibernate.QueryException;
import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultFilters {

    public static final String WHERE = " where ";
    public static final String SUPER_USER_ID = "100";
    public static final String SUPER_USER_CLIENT_ID = "0";
    public static final String SUPER_USER_ORG_ID = "0";
    public static final String REG_EXP = "\\sfrom\\s\\w+\\s(\\w+_)";
    public static final String LIMIT = " limit ";


    public static String addFilters(String sql, String userId, String clientId, String orgId, String isActive) {
            //SUPERUSER BY PASS FILTERS
            if (isSuperUser(userId, clientId, orgId)) {
                return sql;
            }

            boolean containsWhere = sql.contains(WHERE);
            //GET THE TABLE NAME USING REGULAR EXPRESSION
            String tableAlias = getTableAlias(sql);

            //RETURN DEFAULT FILTERS
            return replaceInQuery(sql, clientId, orgId, isActive, containsWhere, tableAlias);
    }

    @NotNull
    private static String replaceInQuery(String sql, String clientId, String orgId, String isActive, boolean containsWhere, String tableAlias) {
        String resultSql;
        if (containsWhere) {
            resultSql = sql.replace(WHERE, WHERE + tableAlias +".ad_client_id = '"+ clientId +"' " +
                    "and "+ tableAlias +".ad_org_id = ANY (etrx_org_tree('"+ orgId +"')) " +
                    "and "+ tableAlias +".isactive = '"+ isActive +"' and ");
        } else {
            resultSql = sql.replace(LIMIT, WHERE + tableAlias +".ad_client_id = '"+ clientId +"' " +
                    "and "+ tableAlias +".ad_org_id = ANY (etrx_org_tree('"+ orgId +"')) " +
                    "and "+ tableAlias +".isactive = '"+ isActive +"' limit ");
        }
        return resultSql;
    }

    private static boolean isSuperUser(String userId, String clientId, String orgId) {
        if ((StringUtils.equals(userId, SUPER_USER_ID)) && (StringUtils.equals(clientId, SUPER_USER_CLIENT_ID))
                && (StringUtils.equals(orgId, SUPER_USER_ORG_ID))) {
            return true;
        }
        return false;
    }

    private static String getTableAlias(String sql) {
        Pattern qryPattern = Pattern.compile(REG_EXP);
        Matcher qryMatcher = qryPattern.matcher(sql);
        if (qryMatcher.find()) {
            return qryMatcher.group(1);
        } else {
            log.error("[ getTableAlias ] - PATTERN ERROR");
            throw new QueryException("getTableAlias ERROR ");
        }
    }
}
