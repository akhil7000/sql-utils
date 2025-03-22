package io.github.sqlutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryUtil Tests")
class QueryUtilTest {

	@TempDir
	Path tempDir;

	private String testSqlFile;

	@BeforeEach
	void setUp() throws IOException {
		// Create a temporary SQL file for testing
		testSqlFile = tempDir.resolve("test-queries.sql").toString();
		String sqlContent = "-- name: test_query\n" + "SELECT id, name, email FROM users WHERE status = :status;\n\n"
				+ "-- name: test_query_with_numbers\n"
				+ "SELECT id, name, age FROM users WHERE id = :id AND age > :age;\n\n"
				+ "-- name: test_query_with_special_chars\n" + "SELECT id, name FROM users WHERE name = :name;";
		Files.writeString(tempDir.resolve("test-queries.sql"), sqlContent);

		// Initialize the SQL file path
		QueryUtil.setFilePath(testSqlFile);
	}

	@Nested
	@DisplayName("Query Loading and Caching Tests")
	class QueryLoadingTests {
		@Test
		@DisplayName("Should throw exception when file path is not set")
		void shouldThrowExceptionWhenFilePathNotSet() {
			QueryUtil.setFilePath(null);
			assertThrows(IllegalStateException.class, () -> QueryUtil.getQuery("testQuery"));
		}

		@Test
		@DisplayName("Should throw exception when query not found")
		void shouldThrowExceptionWhenQueryNotFound() {
			assertThrows(IllegalArgumentException.class, () -> QueryUtil.getQuery("nonexistentQuery"));
		}

		@Test
		@DisplayName("Should load existing query")
		void shouldLoadExistingQuery() {
			String query = QueryUtil.getQuery("test_query");
			assertNotNull(query);
			assertTrue(query.contains("SELECT id, name, email"));
		}
	}

	@Nested
	@DisplayName("Query Formatting Tests")
	class QueryFormattingTests {
		@Test
		@DisplayName("Should format complex query with proper spacing")
		void shouldFormatComplexQuery() {
			String unformatted = "SELECT id,\n  name,\n  email\nFROM users\nWHERE\n  status = 'active'";
			String expected = "SELECT id, name, email FROM users WHERE status = 'active'";

			String formatted = QueryUtil.formatSqlQuery(unformatted);
			assertEquals(expected, formatted);
		}

		@Test
		@DisplayName("Should handle trailing semicolons")
		void shouldHandleTrailingSemicolons() {
			String[] queries = {"SELECT * FROM users;", "SELECT * FROM users;  ", "SELECT * FROM users;  \n"};

			for (String query : queries) {
				assertEquals("SELECT * FROM users", QueryUtil.formatSqlQuery(query));
			}
		}

		@Test
		@DisplayName("Should handle null and empty inputs")
		void shouldHandleNullAndEmpty() {
			assertNull(QueryUtil.formatSqlQuery(null));
			assertEquals("", QueryUtil.formatSqlQuery(""));
		}

		@Test
		@DisplayName("Should preserve semicolons within string literals")
		void shouldPreserveSemicolonsInLiterals() {
			String query = "INSERT INTO users (name) VALUES ('John; Doe');";
			assertEquals("INSERT INTO users (name) VALUES ('John; Doe')", QueryUtil.formatSqlQuery(query));
		}
	}

	@Nested
	@DisplayName("Query Modification Tests")
	class QueryModificationTests {
		@Test
		@DisplayName("Should modify SELECT columns")
		void shouldModifySelectColumns() {
			String originalQuery = "SELECT id, name, email FROM users WHERE status = 'active'";
			List<String> newColumns = Arrays.asList("id", "username", "last_login_date");

			String modifiedQuery = QueryUtil.modifySelectColumnsWithQuery(originalQuery, newColumns);

			assertTrue(modifiedQuery.contains("SELECT id, username, last_login_date"));
			assertTrue(modifiedQuery.contains("FROM users"));
			assertTrue(modifiedQuery.contains("WHERE status = 'active'"));
		}

		@Test
		@DisplayName("Should handle empty or null inputs for SELECT modification")
		void shouldHandleEmptyInputsForSelectModification() {
			String query = "SELECT * FROM users";
			List<String> emptyColumns = Arrays.asList();

			assertEquals(query, QueryUtil.modifySelectColumnsWithQuery(query, emptyColumns));
			assertEquals(query, QueryUtil.modifySelectColumnsWithQuery(query, null));
			assertNull(QueryUtil.modifySelectColumnsWithQuery(null, Arrays.asList("id")));
		}

		@Test
		@DisplayName("Should modify WHERE clause with multiple conditions")
		void shouldModifyWhereClause() {
			String originalQuery = "SELECT id, name, email FROM users";
			Map<String, Object> conditions = new HashMap<>();
			conditions.put("status", "active");
			conditions.put("department_id", 5);

			String modifiedQuery = QueryUtil.modifyWhereClauseWithQuery(originalQuery, conditions);

			assertTrue(modifiedQuery.contains("SELECT id, name, email"));
			assertTrue(modifiedQuery.contains("FROM users"));
			assertTrue(modifiedQuery.contains("WHERE"));
			assertTrue(modifiedQuery.contains("status = 'active'"));
			assertTrue(modifiedQuery.contains("department_id = 5"));
		}

		@Test
		@DisplayName("Should handle numeric values in WHERE clause")
		void shouldHandleNumericValuesInWhereClause() {
			String originalQuery = "SELECT id, name, email FROM users";
			Map<String, Object> conditions = new HashMap<>();
			conditions.put("id", 100);
			conditions.put("department_id", 5);

			String modifiedQuery = QueryUtil.modifyWhereClauseWithQuery(originalQuery, conditions);

			assertTrue(modifiedQuery.contains("id = 100"));
			assertTrue(modifiedQuery.contains("department_id = 5"));
		}

		@Test
		@DisplayName("Should handle empty or null inputs for WHERE modification")
		void shouldHandleEmptyInputsForWhereModification() {
			String query = "SELECT * FROM users";
			Map<String, Object> emptyConditions = new HashMap<>();

			assertEquals(query, QueryUtil.modifyWhereClauseWithQuery(query, emptyConditions));
			assertEquals(query, QueryUtil.modifyWhereClauseWithQuery(query, null));
			assertNull(QueryUtil.modifyWhereClauseWithQuery(null, new HashMap<>()));
		}

		@Test
		@DisplayName("Should throw exception for non-SELECT queries")
		void shouldThrowExceptionForNonSelectQueries() {
			String insertQuery = "INSERT INTO users (name) VALUES ('John')";
			List<String> columns = Arrays.asList("id");
			Map<String, Object> conditions = new HashMap<>();
			conditions.put("id", 1);

			assertThrows(IllegalArgumentException.class,
					() -> QueryUtil.modifySelectColumnsWithQuery(insertQuery, columns));
			assertThrows(IllegalArgumentException.class,
					() -> QueryUtil.modifyWhereClauseWithQuery(insertQuery, conditions));
		}
	}

	@Nested
	@DisplayName("Parameter Replacement Tests")
	class ParameterReplacementTests {
		@Test
		@DisplayName("Should replace string parameters")
		void shouldReplaceStringParameters() {
			String query = "SELECT * FROM users WHERE name = :name AND status = :status";
			Map<String, Object> params = new HashMap<>();
			params.put("name", "John");
			params.put("status", "active");

			String result = QueryUtil.getQuery(query, params);
			assertTrue(result.contains("name = 'John'"));
			assertTrue(result.contains("status = 'active'"));
		}

		@Test
		@DisplayName("Should handle special characters in string parameters")
		void shouldHandleSpecialCharactersInStringParameters() {
			String query = "SELECT * FROM users WHERE name = :name";
			Map<String, Object> params = new HashMap<>();
			params.put("name", "O'Connor");

			String result = QueryUtil.getQuery(query, params);
			assertTrue(result.contains("name = 'O''Connor'"));
		}

		@Test
		@DisplayName("Should replace numeric parameters")
		void shouldReplaceNumericParameters() {
			String query = "SELECT * FROM users WHERE id = :id AND age > :age";
			Map<String, Object> params = new HashMap<>();
			params.put("id", 100);
			params.put("age", 25);

			String result = QueryUtil.getQuery(query, params);
			assertTrue(result.contains("id = 100"));
			assertTrue(result.contains("age > 25"));
		}

		@Test
		@DisplayName("Should handle empty parameter map")
		void shouldHandleEmptyParameterMap() {
			String query = "SELECT * FROM users WHERE id = :id";
			Map<String, Object> params = new HashMap<>();

			String result = QueryUtil.getQuery(query, params);
			assertEquals(query, result);
		}
	}
}