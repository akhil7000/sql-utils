package io.github.sqlutils;

import org.junit.jupiter.api.Test;

import io.github.sqlutils.QueryUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryUtilTest {

    @Test
    void testFormatSqlQuery() {
        String unformatted = "SELECT id,\n  name,\n  email\nFROM users\nWHERE\n  status = 'active'";
        String expected = "SELECT id, name, email FROM users WHERE status = 'active'";
        
        String formatted = QueryUtil.formatSqlQuery(unformatted);
        
        assertEquals(expected, formatted);
    }
    
    @Test
    void testModifySelectColumns() {
        String originalQuery = "SELECT id, name, email FROM users WHERE status = 'active'";
        List<String> newColumns = Arrays.asList("id", "username", "last_login_date");
        
        String modifiedQuery = QueryUtil.modifySelectColumns(originalQuery, newColumns);
        
        assertTrue(modifiedQuery.contains("SELECT id, username, last_login_date"));
        assertTrue(modifiedQuery.contains("FROM users"));
        assertTrue(modifiedQuery.contains("WHERE status = 'active'"));
    }
    
    @Test
    void testModifyWhereClause() {
        String originalQuery = "SELECT id, name, email FROM users";
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("status", "active");
        conditions.put("department_id", 5);
        
        String modifiedQuery = QueryUtil.modifyWhereClause(originalQuery, conditions);
        
        assertTrue(modifiedQuery.contains("SELECT id, name, email"));
        assertTrue(modifiedQuery.contains("FROM users"));
        assertTrue(modifiedQuery.contains("WHERE"));
        assertTrue(modifiedQuery.contains("status = 'active'"));
        assertTrue(modifiedQuery.contains("department_id = 5"));
    }
    
    @Test
    void testModifyWhereClauseWithNumericValues() {
        String originalQuery = "SELECT id, name, email FROM users";
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("id", 100);
        conditions.put("department_id", 5);
        
        String modifiedQuery = QueryUtil.modifyWhereClause(originalQuery, conditions);
        
        assertTrue(modifiedQuery.contains("id = 100"));
        assertTrue(modifiedQuery.contains("department_id = 5"));
    }

    @Test
    void formatSqlQuery_ShouldRemoveTrailingSemicolon() {
        // Test cases with semicolons
        String query1 = "SELECT * FROM users;";
        String query2 = "SELECT * FROM users;  ";
        String query3 = "SELECT * FROM users;  \n";
        
        assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query1));
        assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query2));
        assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query3));
    }

    @Test
    void formatSqlQuery_ShouldNotModifyQueriesWithoutSemicolon() {
        // Test cases without semicolons
        String query1 = "SELECT * FROM users";
        String query2 = "SELECT * FROM users  ";
        
        assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query1));
        assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query2));
    }

    @Test
    void formatSqlQuery_ShouldHandleNullAndEmpty() {
        // Test null and empty cases
        assertNull(QueryUtil.formatSqlQuery(null));
        assertEquals("", QueryUtil.formatSqlQuery(""));
    }

    @Test
    void formatSqlQuery_ShouldPreserveSemicolonsInMiddle() {
        // Test case with semicolon in the middle of the query
        String query = "INSERT INTO users (name) VALUES ('John; Doe');";
        assertEquals("INSERT INTO users (name) VALUES ('John; Doe')", QueryUtil.formatSqlQuery(query));
    }
} 