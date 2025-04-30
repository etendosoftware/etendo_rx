/*
 * Copyright 2022-2024  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendorx.das.test;

import com.etendorx.das.utils.DefaultFilters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This class contains unit tests for the DefaultFilters class.
 * It tests the addFilters method with different HTTP methods and SQL queries.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class DefaultFiltersTest {

  // Constants for testing
  public static final String REST_METHOD_PATCH = "PATCH";
  private static final String SELECT_QUERY = "SELECT * FROM table t1_0 LIMIT 10";
  private static final String UPDATE_QUERY = "UPDATE t1_0 SET column = 'value' WHERE t1_0.table_id = 1";
  private static final String DELETE_QUERY = "DELETE FROM table t1_0 WHERE t1_0.table_id = 1";
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

  /**
   * Tests the addFilters method with a null client ID and a GET request.
   */
  @Test
  void testAddFiltersAuthServiceBypass() {
    // Arrange
    boolean isActive = true;

    // Act
    String result = DefaultFilters.addFilters(SELECT_QUERY, null, USER_ID_123, ROLE_ID_789,
        isActive, REST_METHOD_GET);

    // Assert
    Assertions.assertEquals(SELECT_QUERY, result);
  }

  /**
   * Tests the addFilters method with the super user ID and client ID and a GET request.
   */
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

  /**
   * Tests the addFilters method with a GET request.
   */
  @Test
  void testAddFiltersGetMethod() {
    // Arrange
    boolean isActive = true;

    // Act
    String result = DefaultFilters.addFilters(SELECT_QUERY, USER_ID_123, CLIENT_ID_456,
        ROLE_ID_101112, isActive, REST_METHOD_GET);

    // Assert
    String expected = "SELECT * FROM table t1_0 WHERE t1_0.ad_client_id IN ('0', '456') AND etrx_role_organizations('456', '101112', 'r') LIKE concat('%|', t1_0.ad_org_id, '|%') AND t1_0.isactive = 'Y' LIMIT 10"; //NOSONAR
    Assertions.assertEquals(expected, result);
  }

  /**
   * Tests the addFilters method with a PUT request.
   */
  @Test
  void testAddFiltersPutMethod() {
    // Arrange
    boolean isActive = true;

    // Act
    String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_ID_123, CLIENT_ID_456,
        ROLE_ID_101112, isActive, REST_METHOD_PUT);

    // Assert
    String expected = "UPDATE t1_0 SET column = 'value' WHERE t1_0.table_id = 1 AND ad_client_id IN ('0', '456') AND etrx_role_organizations('456', '101112', 'r') LIKE concat('%|', ad_org_id, '|%')"; //NOSONAR
    Assertions.assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a POST HTTP method.
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFiltersPostMethod() {
    // Arrange: Set up the isActive flag and the expected modified query
    boolean isActive = true;
    String expected = "SELECT * FROM table t1_0 WHERE t1_0.ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', t1_0.ad_org_id, '|%') AND t1_0.isactive = 'Y' LIMIT 10"; //NOSONAR

    // Act: Call the addFilters method with a POST HTTP method and the test constants
    String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_POST);

    // Assert: Verify that the modified query matches the expected query
    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a PUT HTTP method and the SQL query starts with "update".
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFiltersPutMethodStartingWithUpdate() {
    boolean isActive = true;

    String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_PUT);

    String expected = "UPDATE t1_0 SET column = 'value' WHERE t1_0.table_id = 1 AND ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', ad_org_id, '|%')"; //NOSONAR

    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a PUT HTTP method and the SQL query does not start with "update".
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFiltersPutMethodNotStartingWithUpdate() {
    boolean isActive = true;

    String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_PUT);

    String expected = "SELECT * FROM table t1_0 WHERE t1_0.ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', t1_0.ad_org_id, '|%') AND t1_0.isactive = 'Y' LIMIT 10"; //NOSONAR
    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a PATCH HTTP method and the SQL query starts with "update".
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.z
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFilters_PatchMethod_StartingWithUpdate() {
    boolean isActive = true;

    String result = DefaultFilters.addFilters(UPDATE_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_PATCH);

    String expected = "UPDATE t1_0 SET column = 'value' WHERE t1_0.table_id = 1 AND ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', ad_org_id, '|%')"; //NOSONAR
    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a PATCH HTTP method and the SQL query does not start with "update".
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFilters_PatchMethod_NotStartingWithUpdate() {
    boolean isActive = true;

    String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_PATCH);

    String expected = "SELECT * FROM table t1_0 WHERE t1_0.ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', t1_0.ad_org_id, '|%') AND t1_0.isactive = 'Y' LIMIT 10"; //NOSONAR
    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with a DELETE HTTP method and the SQL query does not start with "delete".
   * <p>
   * The addFilters method is expected to modify the provided SQL query by adding additional
   * filters based on the provided user, client, and role identifiers, as well as the isActive flag.
   * <p>
   * In this test, the method is expected to add filters that restrict the query to rows where:
   * - the client ID is either '0' or the provided client ID,
   * - the organization ID is included in the list of organizations associated with the provided role, and
   * - the isActive flag is 'Y'.
   * <p>
   * The modified query is then compared to an expected query string to verify that the method
   * has added the correct filters.
   */
  @Test
  void testAddFilters_deleteMethod_NotStartingWithDelete() {
    boolean isActive = true;

    String result = DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
        REST_METHOD_DELETE);

    String expected = "SELECT * FROM table t1_0 WHERE t1_0.ad_client_id IN ('0', 'client1') AND etrx_role_organizations('client1', 'role1', 'r') LIKE concat('%|', t1_0.ad_org_id, '|%') AND t1_0.isactive = 'Y' LIMIT 10"; //NOSONAR
    assertEquals(expected, result);
  }

  /**
   * This test method verifies the behavior of the addFilters method in the DefaultFilters class
   * when it is called with an unknown HTTP method.
   * <p>
   * The addFilters method is expected to throw an IllegalArgumentException with a message
   * indicating that the provided HTTP method is unknown.
   */
  @Test
  void testAddFilters_UnknownMethod() {
    boolean isActive = true;
    String restMethod = "UNKNOWN";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> DefaultFilters.addFilters(SELECT_QUERY, USER_1, CLIENT_1, ROLE_1, isActive,
            restMethod));

    String expectedMessage = "Unknown HTTP method: " + restMethod;
    assertEquals(expectedMessage, exception.getMessage());
  }
}
