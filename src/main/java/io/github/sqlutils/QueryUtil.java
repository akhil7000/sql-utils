package io.github.sqlutils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for working with SQL queries. Provides functionality for
 * loading, caching, and manipulating SQL queries from SQL files. The class
 * supports:
 * <ul>
 * <li>Loading and caching SQL queries from files</li>
 * <li>Parameter substitution in queries</li>
 * <li>Query formatting and manipulation</li>
 * <li>Modification of SELECT columns and WHERE clauses</li>
 * </ul>
 */
@Slf4j
public final class QueryUtil {

	private static final Map<String, Map<String, String>> sqlQueriesCache = new HashMap<>();
	private static String filePath;

	/**
	 * Sets the default SQL file path to be used by simplified getQuery methods.
	 * Loads all queries from the file and caches them for quick access. The method
	 * supports both resource paths (relative to classpath) and absolute file paths.
	 * 
	 * @param sqlFilePath
	 *            Path to the SQL file (can be resource path or absolute file path)
	 */
	public static void setFilePath(String sqlFilePath) {
		if (sqlFilePath == null) {
			filePath = null;
			log.info("Default SQL file path cleared");
			return;
		}

		filePath = sqlFilePath;
		log.info("Default SQL file path set to: {}", sqlFilePath);

		// Load all queries from the file
		try {
			String content;

			// First try to load as a resource
			try (InputStream resourceStream = QueryUtil.class.getClassLoader().getResourceAsStream(sqlFilePath)) {
				if (resourceStream != null) {
					content = new String(resourceStream.readAllBytes());
				} else {
					// If not found as resource, try as absolute file path
					content = Files.readString(Paths.get(sqlFilePath));
				}
			}

			// Create a new map for this file if it doesn't exist
			if (!sqlQueriesCache.containsKey(sqlFilePath)) {
				sqlQueriesCache.put(sqlFilePath, new HashMap<>());
			}

			// Pattern to match all named queries in the file
			Pattern namePattern = Pattern.compile("(?m)--\\s*name:\\s*([^\\s]+)\\s*\\R([\\s\\S]*?)(?=--\\s*name:|\\z)");
			Matcher matcher = namePattern.matcher(content);

			int queryCount = 0;
			while (matcher.find()) {
				String queryName = matcher.group(1);
				String queryText = matcher.group(2).trim();

				// Format and cache the query
				queryText = formatSqlQuery(queryText);
				sqlQueriesCache.get(sqlFilePath).put(queryName, queryText);
				queryCount++;
			}

			log.info("Successfully loaded and cached {} queries from file: {}", queryCount, sqlFilePath);
		} catch (IOException e) {
			log.error("Failed to read SQL file during initialization: {}", sqlFilePath, e);
			throw new RuntimeException("Failed to read SQL file during initialization: " + sqlFilePath, e);
		}
	}

	/**
	 * Get a specific query from the default SQL file without parameters.
	 *
	 * @param queryName
	 *            Name of the query to extract
	 * @return The extracted SQL query
	 */
	public static String getQuery(String queryName) {
		if (filePath == null) {
			throw new IllegalStateException("Default SQL file path not set. Call setFilePath first.");
		}

		// Check if the queries are already cached
		if (!sqlQueriesCache.containsKey(filePath)) {
			log.error("No queries cached for default file path: {}", filePath);
			throw new IllegalStateException("No queries cached for default file path: " + filePath);
		}

		// Check if the specific query exists
		Map<String, String> fileQueries = sqlQueriesCache.get(filePath);
		if (!fileQueries.containsKey(queryName)) {
			log.error("Query with name '{}' not found in file: {}", queryName, filePath);
			throw new IllegalArgumentException("Query with name '" + queryName + "' not found in file: " + filePath);
		}

		return fileQueries.get(queryName);
	}

	/**
	 * Get a query with parameters substituted. This method can handle both named
	 * queries from the SQL file and direct query strings.
	 *
	 * @param queryNameOrText
	 *            Either a query name from the SQL file or a direct query string
	 * @param params
	 *            Map of parameter names to values
	 * @return The SQL query with parameters substituted
	 */
	public static String getQuery(String queryNameOrText, Map<String, Object> params) {
		if (filePath == null) {
			throw new IllegalStateException("Default SQL file path not set. Call setFilePath first.");
		}

		String queryText;
		try {
			// First try to get it as a named query
			queryText = getQuery(queryNameOrText);
		} catch (IllegalArgumentException e) {
			// If not found as a named query, use the input as the query text
			queryText = queryNameOrText;
		}

		return params.isEmpty() ? queryText : replaceParameters(queryText, params);
	}

	/**
	 * Replaces parameter placeholders in an SQL query with their corresponding
	 * values. Parameters in the query should be prefixed with ':' (e.g.,
	 * :paramName). String values are automatically escaped and wrapped in single
	 * quotes.
	 *
	 * @param queryText
	 *            The SQL query containing parameter placeholders
	 * @param params
	 *            Map of parameter names to their values
	 * @return The SQL query with parameters replaced by their values
	 */
	private static String replaceParameters(String queryText, Map<String, Object> params) {
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			String param = ":" + entry.getKey();
			Object value = entry.getValue();
			String replacement = (value instanceof String)
					? "'" + ((String) value).replace("'", "''") + "'"
					: value.toString();

			queryText = queryText.replaceAll(Pattern.quote(param), replacement);
		}
		return queryText;
	}

	/**
	 * Format SQL query by removing unnecessary whitespace, newlines, and trailing
	 * semicolons.
	 *
	 * @param sqlQuery
	 *            The SQL query to format
	 * @return The formatted SQL query
	 */
	public static String formatSqlQuery(String sqlQuery) {
		if (sqlQuery == null || sqlQuery.isEmpty())
			return sqlQuery;

		String formatted = sqlQuery.replaceAll("\\s+", " ").trim();
		// Remove trailing semicolon if present
		formatted = formatted.replaceAll(";\\s*$", "");
		return formatted;
	}

	/**
	 * Gets a specific query from a SQL file and modifies its SELECT columns.
	 *
	 * @param queryName
	 *            The name of the query to extract and modify
	 * @param newSelectColumns
	 *            List of column names to replace in the SELECT clause
	 * @return The modified SQL query with updated SELECT columns
	 * @throws IllegalStateException
	 *             if the default file path is not set
	 * @throws IllegalArgumentException
	 *             if the query is not found or is not a SELECT statement
	 * @throws RuntimeException
	 *             if the modification fails
	 */
	public static String modifySelectColumns(String queryName, List<String> newSelectColumns) {
		String sqlQuery = getQuery(queryName);
		return modifySelectColumnsWithQuery(sqlQuery, newSelectColumns);
	}

	/**
	 * Modifies the SELECT columns in an SQL query string.
	 *
	 * @param sqlQuery
	 *            The original SQL query string to modify
	 * @param newSelectColumns
	 *            List of column names to use in the SELECT clause
	 * @return The modified SQL query with updated SELECT columns
	 * @throws IllegalArgumentException
	 *             if the query is not a SELECT statement
	 * @throws RuntimeException
	 *             if the modification fails
	 */
	public static String modifySelectColumnsWithQuery(String sqlQuery, List<String> newSelectColumns) {
		if (sqlQuery == null || sqlQuery.isEmpty() || newSelectColumns == null || newSelectColumns.isEmpty()) {
			return sqlQuery;
		}

		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(sqlQuery);
		} catch (Exception e) {
			log.error("Failed to parse SQL query", e);
			throw new IllegalArgumentException("Invalid SQL query: " + e.getMessage());
		}

		if (!(statement instanceof Select)) {
			log.error("Query is not a SELECT statement");
			throw new IllegalArgumentException("Query is not a SELECT statement");
		}

		try {
			Select selectStatement = (Select) statement;
			PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

			List<SelectItem> selectItems = newSelectColumns.stream().map(column -> {
				SelectExpressionItem item = new SelectExpressionItem();
				item.setExpression(new Column(column));
				return (SelectItem) item;
			}).collect(Collectors.toList());

			plainSelect.setSelectItems(selectItems);
			log.info("Successfully modified SELECT columns");
			return selectStatement.toString();
		} catch (Exception e) {
			log.error("Failed to modify SELECT columns", e);
			throw new RuntimeException("Failed to modify SELECT columns", e);
		}
	}

	/**
	 * Gets a specific query from a SQL file and modifies its WHERE clause.
	 *
	 * @param queryName
	 *            The name of the query to extract and modify
	 * @param newConditions
	 *            Map of column names to their values for the WHERE clause
	 *            conditions
	 * @return The modified SQL query with updated WHERE clause
	 * @throws IllegalStateException
	 *             if the default file path is not set
	 * @throws IllegalArgumentException
	 *             if the query is not found or is not a SELECT statement
	 * @throws RuntimeException
	 *             if the modification fails
	 */
	public static String modifyWhereClause(String queryName, Map<String, Object> newConditions) {
		String sqlQuery = getQuery(queryName);
		return modifyWhereClauseWithQuery(sqlQuery, newConditions);
	}

	/**
	 * Modifies the WHERE clause conditions in an SQL query string. The method
	 * creates equality conditions for each entry in the newConditions map and
	 * combines them with AND operators.
	 *
	 * @param sqlQuery
	 *            The original SQL query string to modify
	 * @param newConditions
	 *            Map of column names to their values for the WHERE clause
	 *            conditions
	 * @return The modified SQL query with updated WHERE clause
	 * @throws IllegalArgumentException
	 *             if the query is not a SELECT statement
	 * @throws RuntimeException
	 *             if the modification fails
	 */
	public static String modifyWhereClauseWithQuery(String sqlQuery, Map<String, Object> newConditions) {
		if (sqlQuery == null || sqlQuery.isEmpty() || newConditions == null || newConditions.isEmpty()) {
			return sqlQuery;
		}

		Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(sqlQuery);
		} catch (Exception e) {
			log.error("Failed to parse SQL query", e);
			throw new IllegalArgumentException("Invalid SQL query: " + e.getMessage());
		}

		if (!(statement instanceof Select)) {
			log.error("Query is not a SELECT statement");
			throw new IllegalArgumentException("Query is not a SELECT statement");
		}

		try {
			Select selectStatement = (Select) statement;
			PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

			Expression whereClause = null;

			for (Map.Entry<String, Object> entry : newConditions.entrySet()) {
				Expression columnExpr = new Column(entry.getKey());
				Object value = entry.getValue();
				Expression valueExpr = (value instanceof String)
						? new StringValue((String) value)
						: new LongValue(value.toString());

				EqualsTo equalsTo = new EqualsTo(columnExpr, valueExpr);

				if (whereClause == null) {
					whereClause = equalsTo;
				} else {
					whereClause = new AndExpression(whereClause, equalsTo);
				}
			}

			plainSelect.setWhere(whereClause);
			log.info("Successfully modified WHERE clause");
			return selectStatement.toString();
		} catch (Exception e) {
			log.error("Failed to modify WHERE clause", e);
			throw new RuntimeException("Failed to modify WHERE clause", e);
		}
	}
}