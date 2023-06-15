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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

import com.etendorx.das.utils.DefaultFilters;
import com.etendorx.das.utils.TestcontainersUtils;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
class DefaultFiltersTest {

    private static final String select_QUERY = "select * from table table0_ limit 10";
    private static final String update_QUERY = "update table0_ set column = 'value' where table0_.table0_id = 1";
    private static final String delete_QUERY = "delete from table table0_ where table0_.table0_id = 1";

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        TestcontainersUtils.setProperties(registry, postgreSQLContainer);
    }

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = TestcontainersUtils.createDBContainer();

    @Test
    void testAddFilters_AuthServiceBypass() {
        // Arrange
        String userId = null;
        String clientId = "123";
        String orgId = "456";
        String roleId = "789";
        boolean isActive = true;
        String restMethod = "GET";

        // Act
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ limit 10";
        // Assert
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFilters_SuperUserBypass() {
        // Arrange
        String userId = DefaultFilters.SUPER_USER_ID;
        String clientId = DefaultFilters.SUPER_USER_CLIENT_ID;
        String orgId = DefaultFilters.SUPER_USER_ORG_ID;
        String roleId = "789";
        boolean isActive = true;
        String restMethod = "GET";

        // Act
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ limit 10";
        // Assert
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFilters_GetMethod() {
        // Arrange
        String userId = "123";
        String clientId = "456";
        String orgId = "789";
        String roleId = "101112";
        boolean isActive = true;
        String restMethod = "GET";

        // Act
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        // Assert
        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', '456') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('456', '101112', 'r'))) limit 10";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PutMethod() {
        // Arrange
        String userId = "123";
        String clientId = "456";
        String orgId = "789";
        String roleId = "101112";
        boolean isActive = true;
        String restMethod = "PUT";

        // Act
        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, roleId, isActive, restMethod);

        // Assert
        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', '456') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('456', '101112', 'r'))) and table0_.table0_id = 1";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PostMethod() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "POST";

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) limit 10";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PutMethod_StartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PUT";

        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', 'client1') and " +
            "table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) and " +
            "table0_.table0_id = 1";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PutMethod_NotStartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PUT";

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) limit 10";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PatchMethod_StartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PATCH";

        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) " +
            "and table0_.table0_id = 1";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_PatchMethod_NotStartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PATCH";

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) limit 10";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_deleteMethod_StartingWithDelete() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "DELETE";

        String result = DefaultFilters.addFilters(delete_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "delete from table table0_ where table0_.ad_client_id in ('0', 'client1') and " +
            "table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) and " +
            "table0_.table0_id = 1";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_deleteMethod_NotStartingWithDelete() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "DELETE";

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, roleId, isActive, restMethod);

        String expected = "select * from table table0_ where table0_.ad_client_id in ('0', 'client1') " +
            "and table0_.ad_org_id in (select unnest(etrx_role_organizations('client1', 'role1', 'r'))) limit 10";
        assertEquals(expected, result);
    }

    @Test
    void testAddFilters_UnknownMethod() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "UNKNOWN";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, isActive, restMethod));

        String expectedMessage = "Unknown HTTP method: " + restMethod;
        assertEquals(expectedMessage, exception.getMessage());
    }
}
