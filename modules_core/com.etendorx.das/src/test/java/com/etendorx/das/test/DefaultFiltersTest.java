package com.etendorx.das.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import com.etendorx.das.utils.DefaultFilters;
import com.etendorx.das.utils.TestcontainersUtils;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
class DefaultFiltersTest {

    public static final String REST_METHOD_PATCH = "PATCH";
    private static final String SELECT_QUERY = "select * from table table0_ limit 10";
    private static final String UPDATE_QUERY = "update table0_ set column = 'value' where table0_.table0_id = 1";
    private static final String DELETE_QUERY = "delete from table table0_ where table0_.table0_id = 1";
    public static final String USER_1 = "user1";
    public static final String CLIENT_1 = "client1";
    public static final String ROLE_1 = "role1";
    public static final String REST_METHOD_POST = "POST";
    public static final String REST_METHOD_PUT = "PUT";
    public static final String REST_METHOD_DELETE = "DELETE";
    public static final String USER_ID_123 = "123";
    public static final String CLIENT_ID_456 = "456";
    public static final String ROLE_ID_101112 = "101112";
    public static final String REST_METHOD_GET = "GET";
    public static final String ROLE_ID_789 = "789";

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        TestcontainersUtils.setProperties(registry, postgreSQLContainer);
    }

    @Container
    public static final PostgreSQLContainer<?> postgreSQLContainer = TestcontainersUtils.createDBContainer();

    @Test
    void testAddFiltersAuthServiceBypass() {
        // Arrange
        String userId = null;
        boolean isActive = true;

        // Act
        String result = DefaultFilters.addFilters(SELECT_QUERY, userId, USER_ID_123, ROLE_ID_789, isActive, REST_METHOD_GET);

        // Assert
        Assertions.assertEquals(SELECT_QUERY, result);
    }

    @Test
    void testAddFiltersSuperUserBypass() {
        // Arrange
        String userId = DefaultFilters.SUPER_USER_ID;
        String clientId = DefaultFilters.SUPER_USER_CLIENT_ID;
        boolean isActive = true;

        // Act
        String result = DefaultFilters.addFilters(SELECT_QUERY, userId, clientId, ROLE_ID_789, isActive,
            REST_METHOD_GET);

        // Assert
        Assertions.assertEquals(SELECT_QUERY, result);
    }

    @Test
    void testAddFiltersGetMethod() {
        // Arrange
        boolean isActive = true;

        // Act
        String result = DefaultFilters.addFilters(SELECT_QUERY, USER_ID_123, CLIENT_ID_456, ROLE_ID_101112, isActive,
            REST_METHOD_GET);

        // Assert
        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', '456') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('456', '101112', 'r'))) limit 10"; //NOSONAR
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFiltersPutMethod() {
        // Arrange
        boolean isActive = true;

        // Act
        String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_ID_123, CLIENT_ID_456, ROLE_ID_101112, isActive, REST_METHOD_PUT);

        // Assert
        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', '456') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('456', '101112', 'r'))) " +
            "and table0_.table0_id = 1"; //NOSONAR
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFiltersPostMethod() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_POST);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) limit 10"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFiltersPutMethodStartingWithUpdate() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_PUT);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', 'client1') and " +
            "table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) and " +
            "table0_.table0_id = 1"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFiltersPutMethodNotStartingWithUpdate() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_PUT);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) " +
            "limit 10"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PatchMethod_StartingWithUpdate() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_PATCH);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) " +
            "and table0_.table0_id = 1"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PatchMethod_NotStartingWithUpdate() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_PATCH);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) " +
            "limit 10"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_deleteMethod_StartingWithDelete() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(DELETE_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_DELETE);

        String expected = "delete from table table0_ where table0_.ad_client_id in ('0', 'client1') and " +
            "table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) and " +
            "table0_.table0_id = 1"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_deleteMethod_NotStartingWithDelete() {
        boolean isActive = true;

        String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, REST_METHOD_DELETE);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) " +
            "limit 10"; //NOSONAR
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_UnknownMethod() {
        boolean isActive = true;
        String restMethod = "UNKNOWN";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive, restMethod));

        String expectedMessage = "Unknown HTTP method: " + restMethod;
        assertEquals(expectedMessage, exception.getMessage());
    }
}
