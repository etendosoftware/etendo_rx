package com.etendorx.das.utils;

import org.hibernate.QueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DefaultFiltersTest {

    private String sql;
    private String adUser;
    private String clientId;
    private String orgId;
    private String isActive;

    @BeforeEach
    void setUp() {
        //Initialize the variables before each test method
        sql = "select user0_.ad_user_id as ad_user_1_129_ from ad_user user0_ limit ?";
        adUser = "101";
        clientId = "1";
        orgId = "1";
        isActive = "Y";
    }

    @DisplayName("Test addFilters method when not a superuser and not contains count")
    @Test
    void testAddFiltersWhenNotSuperuserAndNotContainsCount() {
        String resultSql = DefaultFilters.addFilters(sql, adUser, clientId, orgId, isActive);
        Assertions.assertNotNull(resultSql);
        Assertions.assertNotEquals(sql, resultSql);
        Assertions.assertTrue(resultSql.contains("ad_client_id = '"+ clientId +"'"));
        Assertions.assertTrue(resultSql.contains("ad_org_id = ANY (etrx_org_tree('"+ orgId +"'))"));
        Assertions.assertTrue(resultSql.contains("isactive = '"+ isActive +"'"));
    }

    @DisplayName("Test addFilters method when not a superuser and contains count")
    @Test
    void testAddFiltersWhenNotSuperuserAndContainsCount() {
        sql = "select count(user0_.ad_user_id) as ad_user_1_129_ from ad_user user0_ limit ?";
        String resultSql = DefaultFilters.addFilters(sql, adUser, clientId, orgId, isActive);
        Assertions.assertNotNull(resultSql);
        Assertions.assertNotEquals(sql, resultSql);
        Assertions.assertTrue(resultSql.contains("ad_client_id = '"+ clientId +"'"));
        Assertions.assertTrue(resultSql.contains("ad_org_id = ANY (etrx_org_tree('"+ orgId +"'))"));
        Assertions.assertTrue(resultSql.contains("isactive = '"+ isActive +"'"));
    }

    @DisplayName("Test getTableName method when Pattern not Match")
    @Test
    void testGetTableNameWhenPatternError() {
        sql = "from(user0_.ad_user_id as ad_user_1_129_)from_ad_user user0_from ?";
        Assertions.assertThrows(QueryException.class, () -> {
            DefaultFilters.addFilters(sql, adUser, clientId, orgId, isActive);
        });
    }

    @DisplayName("Test addFilters method when a superuser")
    @Test
    void testAddFiltersWhenSuperuser() {
        adUser = "100";
        clientId = "0";
        orgId = "0";
        String resultSql = DefaultFilters.addFilters(sql, adUser, clientId, orgId, isActive);
        Assertions.assertEquals(sql, resultSql);
    }
}
