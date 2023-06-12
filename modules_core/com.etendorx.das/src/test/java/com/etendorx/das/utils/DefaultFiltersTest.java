package com.etendorx.das.utils;

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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
class DefaultFiltersTest {

    private static final String select_QUERY = "select * from table table0_ limit 10";
    private static final String update_QUERY = "update table0_ set column = 'value'";
    private static final String delete_QUERY = "delete from table0_";

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
        DockerImageName.parse("etendo/etendodata:rx-1.2.1").asCompatibleSubstituteFor("postgres")
    )
        .withPassword("syspass")
        .withUsername("postgres")
        .withEnv("PGDATA", "/postgres")
        .withDatabaseName("etendo")
        .waitingFor(
            Wait.forLogMessage(".*database system is ready to accept connections*\\n", 1)
        );

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
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        // Assert
        Assertions.assertEquals(select_QUERY, result);
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
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        // Assert
        Assertions.assertEquals(select_QUERY, result);
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
        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        // Assert
        String expected = "select * from table table0_ where " +
            "table0_.ad_client_id = '456' " +
            "and (table0_.ad_org_id = '789' OR ((etrx_is_org_in_org_tree(table0_.ad_org_id, '789', '1')) = 1)) " +
            "and table0_.isactive = 'Y' limit 10";
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
        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        // Assert
        String expected = "update table0_ set column = 'value' where table0_.ad_client_id = '456' " +
            "and (table0_.ad_org_id = '789' OR ((etrx_is_org_in_org_tree(table0_.ad_org_id, '789', '1')) = 1))";
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

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        assertEquals(select_QUERY, result);
    }

    @Test
    void testAddFilters_PutMethod_StartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PUT";

        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id = 'client1' and (table0_.ad_org_id = 'org1' OR ((etrx_is_org_in_org_tree(table0_.ad_org_id, 'org1', '1')) = 1))";
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

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        assertEquals(select_QUERY, result);
    }

    @Test
    void testAddFilters_PatchMethod_StartingWithUpdate() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "PATCH";

        String result = DefaultFilters.addFilters(update_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        String expected = "update table0_ set column = 'value' where table0_.ad_client_id = 'client1' and (table0_.ad_org_id = 'org1' OR ((etrx_is_org_in_org_tree(table0_.ad_org_id, 'org1', '1')) = 1))";
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

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        assertEquals(select_QUERY, result);
    }

    @Test
    void testAddFilters_deleteMethod_StartingWithDelete() {
        String userId = "user1";
        String clientId = "client1";
        String orgId = "org1";
        String roleId = "role1";
        boolean isActive = true;
        String restMethod = "DELETE";

        String result = DefaultFilters.addFilters(delete_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        String expected = "delete from table0_ WHERE table0_.ad_client_id = 'client1' and (table0_.ad_org_id = 'org1' OR ((etrx_is_org_in_org_tree(table0_.ad_org_id, 'org1', '1')) = 1))";
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

        String result = DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod);

        assertEquals(select_QUERY, result);
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
            DefaultFilters.addFilters(select_QUERY, userId, clientId, orgId, roleId, isActive, restMethod));

        String expectedMessage = "Unknown HTTP method: " + restMethod;
        assertEquals(expectedMessage, exception.getMessage());
    }
}
