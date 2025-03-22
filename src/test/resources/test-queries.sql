-- name: test_query
SELECT id, name, email FROM users WHERE status = :status;

-- name: test_query_with_numbers
SELECT id, name, age FROM users WHERE id = :id AND age > :age;

-- name: test_query_with_special_chars
SELECT id, name FROM users WHERE name = :name; 