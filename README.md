# SQL Utils

A Java utility library for SQL query manipulation and management.

## Features

- Load and cache SQL queries from files
- Parse SQL queries with named placeholders
- Substitute named parameters in SQL queries
- Programmatically modify SQL queries (SELECT columns, WHERE clauses)

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.sqlutils</groupId>
    <artifactId>sql-utils</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.sqlutils:sql-utils:1.0.0'
```

## Usage

### Loading SQL Queries from a File

Create an SQL file with named queries in your resources directory:

```sql
-- name: get_users
SELECT id, username, email, created_at
FROM users
WHERE status = :status
ORDER BY created_at DESC;

-- name: get_user_by_id
SELECT id, username, email, first_name, last_name, created_at
FROM users
WHERE id = :id;
```

Then in your Java code:

```java
// Initialize the SQL file path
SqlUtil.setFilePath("your-queries.sql");

// Get a query without parameters
String getUsersQuery = SqlUtil.getQuery("get_users");

// Get a query with parameters substituted
Map<String, Object> params = new HashMap<>();
params.put("id", 123);
String userByIdQuery = SqlUtil.getQuery("get_user_by_id", params);
```

### Modifying SQL Queries Programmatically

```java
// Modify the columns in a SELECT query
String originalQuery = "SELECT id, name, email FROM users WHERE status = 'active'";
List<String> newColumns = Arrays.asList("id", "username", "last_login_date");
String modifiedQuery = SqlUtil.modifySelectColumns(originalQuery, newColumns);
// Result: SELECT id, username, last_login_date FROM users WHERE status = 'active'

// Modify the WHERE clause in a SELECT query
String query = "SELECT id, name, email FROM users";
Map<String, Object> conditions = new HashMap<>();
conditions.put("status", "active");
conditions.put("department_id", 5);
String modifiedWhereQuery = SqlUtil.modifyWhereClause(query, conditions);
// Result: SELECT id, name, email FROM users WHERE status = 'active' AND department_id = 5
```

## Requirements

- Java 11 or higher
- JSQLParser library

## License

Apache License, Version 2.0 