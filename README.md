# SQL Utils

[![Maven Central](https://img.shields.io/maven-central/v/io.github.akhil7000/sql-utils)](https://central.sonatype.com/artifact/io.github.akhil7000/sql-utils)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-11%2B-blue)](https://openjdk.java.net/)

A Java utility library for SQL query manipulation and management.

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Requirements](#requirements)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Features

- Load and cache SQL queries from files
- Parse SQL queries with named placeholders
- Substitute named parameters in SQL queries
- Programmatically modify SQL queries (SELECT columns, WHERE clauses)
- Format SQL queries for consistent styling
- Handle special characters in parameters
- Support for both string and numeric parameters

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.akhil7000</groupId>
    <artifactId>sql-utils</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.akhil7000:sql-utils:1.2.1'
```

## Usage

If you're storing SQL files as resources, place them in `src/main/resources` directory.

Include JSQLParser if not managed through transitive dependencies:

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
QueryUtil.setFilePath("your-queries.sql");

// Get a query without parameters
String getUsersQuery = QueryUtil.getQuery("get_users");

// Get a query with parameters substituted
Map<String, Object> params = new HashMap<>();
params.put("id", 123);
String userByIdQuery = QueryUtil.getQuery("get_user_by_id", params);
```

### Parameter Substitution

The library supports both string and numeric parameters, and handles special characters:

```java
// String parameters
Map<String, Object> params = new HashMap<>();
params.put("name", "John");
params.put("status", "active");
String query = "SELECT * FROM users WHERE name = :name AND status = :status";
String result = QueryUtil.getQuery(query, params);

// Special characters in strings
params.put("name", "O'Connor");
String queryWithSpecialChars = "SELECT * FROM users WHERE name = :name";
String result = QueryUtil.getQuery(queryWithSpecialChars, params);

// Numeric parameters
params.put("id", 100);
params.put("age", 25);
String queryWithNumbers = "SELECT * FROM users WHERE id = :id AND age > :age";
String result = QueryUtil.getQuery(queryWithNumbers, params);
```

### Modifying SQL Queries Programmatically

```java
// First, set up your SQL file with named queries
QueryUtil.setFilePath("queries.sql");

// Modify columns using a query name from the file
String modifiedQuery = QueryUtil.modifySelectColumns("get_users", 
    Arrays.asList("id", "username", "last_login_date"));
// Result: SELECT id, username, last_login_date FROM users WHERE status = :status ORDER BY created_at DESC

// Modify columns using a direct query string
String originalQuery = "SELECT id, name, email FROM users WHERE status = 'active'";
List<String> newColumns = Arrays.asList("id", "username", "last_login_date");
String modifiedQuery = QueryUtil.modifySelectColumnsWithQuery(originalQuery, newColumns);
// Result: SELECT id, username, last_login_date FROM users WHERE status = 'active'

// Modify WHERE clause using a query name from the file
Map<String, Object> conditions = new HashMap<>();
conditions.put("status", "active");
conditions.put("department_id", 5);
String modifiedWhereQuery = QueryUtil.modifyWhereClause("get_users", conditions);
// Result: SELECT id, username, email, created_at FROM users 
//        WHERE status = 'active' AND department_id = 5 
//        ORDER BY created_at DESC

// Modify WHERE clause using a direct query string
String query = "SELECT id, name, email FROM users";
String modifiedWhereQuery = QueryUtil.modifyWhereClauseWithQuery(query, conditions);
// Result: SELECT id, name, email FROM users WHERE status = 'active' AND department_id = 5

// Handle empty or null inputs
String query = "SELECT * FROM users";
List<String> emptyColumns = Arrays.asList();
String result = QueryUtil.modifySelectColumnsWithQuery(query, emptyColumns); // Returns original query
```

### Query Formatting

The library provides utilities for formatting SQL queries:

```java
// Format a complex query
String unformatted = "SELECT id,\n  name,\n  email\nFROM users\nWHERE\n  status = 'active'";
String formatted = QueryUtil.formatSqlQuery(unformatted);
// Result: SELECT id, name, email FROM users WHERE status = 'active'

// Handle trailing semicolons
String query = "SELECT * FROM users;";
String formatted = QueryUtil.formatSqlQuery(query);
// Result: SELECT * FROM users

// Preserve semicolons in string literals
String query = "INSERT INTO users (name) VALUES ('John; Doe');";
String formatted = QueryUtil.formatSqlQuery(query);
// Result: INSERT INTO users (name) VALUES ('John; Doe')
```

## Requirements

- Java 11 or higher
- JSQLParser library

## Documentation

For more detailed documentation and examples, please see our [Wiki](https://github.com/akhil7000/sql-utils/wiki).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an Issue.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## Support

If you encounter any problems or have any questions, please open an issue on our [GitHub Issues](https://github.com/akhil7000/sql-utils/issues) page.

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.