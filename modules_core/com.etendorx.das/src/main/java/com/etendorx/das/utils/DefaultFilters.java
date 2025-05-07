package com.etendorx.das.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.update.Update;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for applying default filters to SQL queries based on user ID, client ID, role ID, active filter status, and REST method.
 */
@Slf4j
public class DefaultFilters {
  public static final String SELECT = "SELECT"; // SQL SELECT action
  public static final String INSERT = "INSERT INTO"; // SQL INSERT action
  public static final String UPDATE = "UPDATE"; // SQL UPDATE action
  public static final String DELETE = "DELETE"; // SQL DELETE action
  public static final String SUPER_USER_ID = "100"; // Superuser ID
  public static final String SUPER_USER_CLIENT_ID = "0"; // Superuser client ID
  public static final String GET_METHOD = "GET"; // HTTP GET method
  public static final String POST_METHOD = "POST"; // HTTP POST method
  public static final String PUT_METHOD = "PUT"; // HTTP PUT method
  public static final String PATCH_METHOD = "PATCH"; // HTTP PATCH method
  public static final String DELETE_METHOD = "DELETE"; // HTTP DELETE method

  /**
   * Private constructor to prevent instantiation of the DefaultFilters utility class.
   * This constructor throws an IllegalStateException if called, indicating that the class
   * is intended to be used as a utility class and should not be instantiated.
   *
   * @throws IllegalStateException always thrown to indicate that the class should not be instantiated
   */
  private DefaultFilters() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Adds filters to the provided SQL query based on the user ID, client ID, role ID, active filter status, and REST method.
   * The method bypasses filters for authentication services and superusers.
   *
   * @param sql the original SQL query
   * @param userId the user ID to be used in the filters
   * @param clientId the client ID to be used in the filters
   * @param roleId the role ID to be used in the filters
   * @param isActive a boolean indicating whether the active filter should be applied
   * @param restMethod the HTTP method (GET, POST, PUT, PATCH, DELETE) to determine the type of filters to apply
   * @return the SQL query with the applied filters, or the original SQL query if bypass conditions are met
   * @throws IllegalArgumentException if the provided HTTP method is unknown
   */
  public static String addFilters(String sql, String userId, String clientId, String roleId,
      boolean isActive, String restMethod) {
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
        return replaceInQuery(sql, clientId, roleId, isActive);
      case DELETE_METHOD, PUT_METHOD:
      case PATCH_METHOD:
        return replaceInQuery(sql, clientId, roleId, StringUtils.startsWithIgnoreCase(sql, SELECT));
      case POST_METHOD:
        if (StringUtils.startsWithIgnoreCase(sql, SELECT)) {
          return replaceInQuery(sql, clientId, roleId, isActive);
        } else {
          return sql;
        }
      default:
        log.error("[ processSql ] - Unknown HTTP method: " + restMethod);
        throw new IllegalArgumentException("Unknown HTTP method: " + restMethod);
    }
  }

  /**
   * Generates a list of default WHERE clause conditions based on the provided SQL statement, client ID, role ID, and active filter status.
   * The method extracts information about the SQL query and constructs the conditions accordingly.
   *
   * @param statement the SQL statement to be analyzed
   * @param clientId the client ID to be used in the conditions
   * @param roleId the role ID to be used in the conditions
   * @param isActiveFilter a boolean indicating whether the active filter should be applied
   * @return a list of conditions to be applied as filters to the SQL query
   */
  private static List<String> getDefaultWhereClause(Statement statement, String clientId, String roleId, boolean isActiveFilter) {
    List<String> conditions = new ArrayList<>();
    QueryInfo tableInfo = getQueryInfo(statement);
    if (!StringUtils.isEmpty(tableInfo.getTableAlias())) {
      conditions.add(String.format("%s.ad_client_id in ('0', '%s')", tableInfo.getTableAlias(), clientId));
      conditions.add(String.format(
          "etrx_role_organizations('%s', '%s', 'r') like concat('%%|', %s.ad_org_id, '|%%')",
          clientId, roleId, tableInfo.getTableAlias()));
      if (isActiveFilter) {
        conditions.add(String.format("%s.isactive = 'Y'", tableInfo.getTableAlias()));
      }
    } else {
      conditions.add(String.format("ad_client_id in ('0', '%s')", clientId));
      conditions.add(String.format(
          "etrx_role_organizations('%s', '%s', 'r') like concat('%%|', ad_org_id, '|%%')",
          clientId, roleId));
      if (isActiveFilter) {
        conditions.add("isactive = 'Y'");
      }
    }
    return conditions;
  }

  /**
   * Applies the provided conditions as filters to the given SQL statement.
   * This method modifies the SQL statement by adding the specified conditions
   * to the WHERE clause, depending on the type of SQL operation (SELECT, UPDATE, DELETE).
   * If the statement is an INSERT, it returns the statement as is.
   *
   * @param sql the original SQL query
   * @param statement the parsed SQL statement to be modified
   * @param conditions the list of conditions to be applied as filters
   * @return the modified SQL query with the applied filters
   * @throws QueryException if the SQL operation is not supported, the query was not modified,
   *                        or there is an error parsing the conditions
   */
  static String applyFilters(String sql, Statement statement, List<String> conditions) {
    String finalSql;
    try {
      if (statement instanceof Select select) {
        applyFiltersInSelect(conditions, select);
      } else if (statement instanceof Update update) {
        applyFiltersInUpdate(conditions, update);
      } else if (statement instanceof Delete delete) {
        applyFiltersInDelete(conditions, delete);
      } else if (statement instanceof Insert) {
          return statement.toString();
      } else {
          throw new QueryException("applyFilters ERROR - SQL operation not supported");
      }
      finalSql = statement.toString();
      if (StringUtils.equals(finalSql, sql)) {
        throw new QueryException("applyFilters ERROR - SQL query was not modified");
      }
      log.debug("sql: {}", statement.toString());
      log.debug("finalSql: {}", finalSql);
    } catch (JSQLParserException e) {
      throw new QueryException(e);
    }
    return finalSql;
  }

  /**
   * Applies the provided conditions as filters to a SQL DELETE statement.
   * This method modifies the WHERE clause of the DELETE statement by adding
   * the specified conditions using an AND expression.
   *
   * @param conditions the list of conditions to be applied as filters
   * @param delete the SQL DELETE statement to be modified
   * @throws JSQLParserException if there is an error parsing the conditions or modifying the statement
   */
  private static void applyFiltersInDelete(List<String> conditions, Delete delete) throws JSQLParserException {
    for (String condition : conditions) {
      AndExpression andExpression = new AndExpression();
      andExpression.setLeftExpression(delete.getWhere());
      andExpression.setRightExpression(CCJSqlParserUtil.parseCondExpression(condition));
      delete.setWhere(andExpression);
    }
  }

  /**
   * Applies the provided conditions as filters to a SQL UPDATE statement.
   * This method modifies the WHERE clause of the UPDATE statement by adding
   * the specified conditions using an AND expression.
   *
   * @param conditions the list of conditions to be applied as filters
   * @param update the SQL UPDATE statement to be modified
   * @throws JSQLParserException if there is an error parsing the conditions or modifying the statement
   */
  private static void applyFiltersInUpdate(List<String> conditions, Update update) throws JSQLParserException {
    for (String condition : conditions) {
      AndExpression andExpression = new AndExpression();
      andExpression.setLeftExpression(update.getWhere());
      andExpression.setRightExpression(CCJSqlParserUtil.parseCondExpression(condition));
      update.setWhere(andExpression);
    }
  }

  /**
   * Applies the provided conditions as filters to a SQL SELECT statement.
   * This method modifies the WHERE clause of the SELECT statement by adding
   * the specified conditions using an AND expression. If the WHERE clause
   * does not exist, it initializes it with the first condition.
   *
   * @param conditions the list of conditions to be applied as filters
   * @param select the SQL SELECT statement to be modified
   * @throws JSQLParserException if there is an error parsing the conditions or modifying the statement
   */
  private static void applyFiltersInSelect(List<String> conditions, Select select) throws JSQLParserException {
    PlainSelect plainSelect = select.getPlainSelect();
    for (String condition : conditions) {
      if (plainSelect.getWhere() == null) {
        plainSelect.setWhere(CCJSqlParserUtil.parseCondExpression(condition));
      } else {
        AndExpression andExpression = new AndExpression();
        andExpression.setLeftExpression(plainSelect.getWhere());
        andExpression.setRightExpression(CCJSqlParserUtil.parseCondExpression(condition));
        plainSelect.setWhere(andExpression);
      }
    }
  }

  /**
   * Replaces the SQL query with the appropriate filters based on the client ID, role ID, and active filter status.
   * The method parses the SQL query, generates the default WHERE clause conditions, and applies the filters to the query.
   *
   * @param sql the original SQL query
   * @param clientId the client ID to be used in the filters
   * @param roleId the role ID to be used in the filters
   * @param isActiveFilter a boolean indicating whether the active filter should be applied
   * @return the SQL query with the applied filters
   * @throws QueryException if there is a parsing error
   */
  @NotNull
  private static String replaceInQuery(String sql, String clientId, String roleId,
      boolean isActiveFilter) {
    try {
      Statement statement = CCJSqlParserUtil.parse(sql);
      List<String> conditions = getDefaultWhereClause(statement, clientId, roleId, isActiveFilter);
      return applyFilters(sql, statement, conditions);
    } catch (JSQLParserException e) {
      log.error("[replaceInQuery] - PATTERN ERROR: " + e.getMessage());
      throw new QueryException("replaceInQuery ERROR");
    }
  }

  /**
   * Checks if the given userId and clientId correspond to a superuser.
   * A superuser is identified by having a specific userId and clientId.
   *
   * @param userId the user ID to check
   * @param clientId the client ID to check
   * @return true if the userId and clientId match the superuser credentials, false otherwise
   */
  private static boolean isSuperUser(String userId, String clientId) {
    return StringUtils.equals(userId, SUPER_USER_ID) && StringUtils.equals(clientId,
        SUPER_USER_CLIENT_ID);
  }

  /**
   * Checks if the given userId and clientId correspond to an authentication service.
   * An authentication service is identified by either the userId or clientId being empty.
   *
   * @param userId the user ID to check
   * @param clientId the client ID to check
   * @return true if either the userId or clientId is empty, false otherwise
   */
  // IS_AUTH_SERVICE CONDITION NEEDS IMPROVEMENT
  private static boolean isAuthService(String userId, String clientId) {
    return StringUtils.isEmpty(userId) || StringUtils.isEmpty(clientId);
  }

  /**
   * Extracts information about the SQL query such as the table name, the table alias, and whether the query contains a WHERE clause.
   * The method determines the type of SQL statement (SELECT, UPDATE, INSERT, DELETE) and extracts relevant information accordingly.
   *
   * @param statement the SQL statement to be analyzed
   * @return a QueryInfo object containing details about the SQL query
   * @throws QueryException if the SQL operation is not supported or if there is a parsing error
   */
  static QueryInfo getQueryInfo(Statement statement) {
    String sqlAction = null;
    String tableName = null;
    String tableAlias = null;
    boolean containsWhere = false;
    if (statement instanceof Select select) {
      PlainSelect plainSelect = select.getPlainSelect();
      sqlAction = SELECT;
      tableName = plainSelect.getFromItem().getASTNode().jjtGetFirstToken().toString();
      tableAlias = plainSelect.getFromItem().getAlias().getName();
      containsWhere = plainSelect.getWhere() != null;
    } else if (statement instanceof Update update) {
      sqlAction = UPDATE;
      tableName = update.getTable().getName();
      containsWhere = update.getWhere() != null;
    } else if (statement instanceof Insert insert) {
      sqlAction = INSERT;
      tableName = insert.getTable().getName();
    } else if (statement instanceof Delete delete) {
      sqlAction = DELETE;
      tableName = delete.getTable().getName();
      tableAlias = delete.getTable().getAlias().getName();
      containsWhere = delete.getWhere() != null;
    } else {
      log.error("getQueryInfo ERROR - SQL operation not supported: " + statement.toString());
      throw new QueryException("getQueryInfo ERROR - SQL operation not supported");
    }
    return new QueryInfo(sqlAction, tableName, tableAlias, containsWhere);
  }

  /**
   * A class representing information about an SQL query.
   * This class contains details such as the SQL action (SELECT, UPDATE, INSERT, DELETE),
   * the table name, the table alias, and whether the query contains a WHERE clause.
   */
  @Data
  @AllArgsConstructor
  static class QueryInfo {
    private String sqlAction;  // The action type of the SQL query (SELECT, UPDATE, INSERT, DELETE)
    private String tableName;  // The name of the table involved in the SQL query
    private String tableAlias; // The alias of the table in the SQL query
    private boolean containsWhere; // A boolean indicating whether the SQL query contains a WHERE clause
  }
}
