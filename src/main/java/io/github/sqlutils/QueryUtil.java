package io.github.sqlutils;

import java.io.IOException;
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
 * Utility class for working with SQL queries.
 * Provides functionality for loading, caching, and manipulating SQL queries.
 */
@Slf4j
public final class QueryUtil {
    
    private static final Map<String, Map<String, String>> sqlQueriesCache = new HashMap<>();
    private static String filePath;
        
    /**
     * Sets the default SQL file path to be used by simplified getQuery methods.
     * Loads all queries from the file and caches them for quick access.
     * 
     * @param sqlFilePath Path to the SQL file relative to the resources directory
     */
    public static void setFilePath(String sqlFilePath) {
        filePath = sqlFilePath;
        log.info("Default SQL file path set to: {}", sqlFilePath);
        
        // Load all queries from the file
        try {
            // Get the full path for the resource
            String fullPath = QueryUtil.class.getClassLoader().getResource(sqlFilePath).getPath();
            String content = new String(Files.readAllBytes(Paths.get(fullPath)));
            
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
     * @param queryName Name of the query to extract
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
     * Get a specific query from the default SQL file with parameters substituted.
     *
     * @param queryName Name of the query to extract
     * @param params Map of parameter names to values
     * @return The SQL query with parameters substituted
     */
    public static String getQuery(String queryName, Map<String, Object> params) {
        if (filePath == null) {
            throw new IllegalStateException("Default SQL file path not set. Call setFilePath first.");
        }
        
        String queryText = getQuery(queryName);
        return params.isEmpty() ? queryText : replaceParameters(queryText, params);
    }
    
    /**
     * Replace `:paramName` placeholders in the SQL query with actual values.
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
     * Format SQL query by removing unnecessary whitespace and newlines.
     *
     * @param sqlQuery The SQL query to format
     * @return The formatted SQL query
     */
    public static String formatSqlQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.isEmpty()) return sqlQuery;
        
        String formatted = sqlQuery.replaceAll("\\s+", " ").trim();
        return formatted;
    }
    
    /**
     * Modify SELECT columns in an SQL query
     *
     * @param sqlQuery The original SQL query
     * @param newSelectColumns List of new columns to use in the SELECT clause
     * @return The modified SQL query
     */
    public static String modifySelectColumns(String sqlQuery, List<String> newSelectColumns) {
        if (sqlQuery == null || sqlQuery.isEmpty() || newSelectColumns == null || newSelectColumns.isEmpty()) {
            return sqlQuery;
        }
        
        try {
            Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            
            if (statement instanceof Select) {
                Select selectStatement = (Select) statement;
                PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
                
                List<SelectItem> selectItems = newSelectColumns.stream()
                    .map(column -> {
                        SelectExpressionItem item = new SelectExpressionItem();
                        item.setExpression(new Column(column));
                        return (SelectItem) item;
                    })
                    .collect(Collectors.toList());
                
                plainSelect.setSelectItems(selectItems);
                log.info("Successfully modified SELECT columns");
                return selectStatement.toString();
            } else {
                log.error("Query is not a SELECT statement");
                throw new IllegalArgumentException("Query is not a SELECT statement");
            }
        } catch (Exception e) {
            log.error("Failed to modify SELECT columns", e);
            throw new RuntimeException("Failed to modify SELECT columns", e);
        }
    }
    
    /**
     * Modify WHERE clause conditions in an SQL query
     *
     * @param sqlQuery The original SQL query
     * @param newConditions Map of column names and their new values for the WHERE clause
     * @return The modified SQL query
     */
    public static String modifyWhereClause(String sqlQuery, Map<String, Object> newConditions) {
        if (sqlQuery == null || sqlQuery.isEmpty() || newConditions == null || newConditions.isEmpty()) {
            return sqlQuery;
        }
        
        try {
            Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            
            if (statement instanceof Select) {
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
            } else {
                log.error("Query is not a SELECT statement");
                throw new IllegalArgumentException("Query is not a SELECT statement");
            }
        } catch (Exception e) {
            log.error("Failed to modify WHERE clause", e);
            throw new RuntimeException("Failed to modify WHERE clause", e);
        }
    }
} 