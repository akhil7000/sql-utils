-- name: get_users
SELECT id, username, email, created_at
FROM users
WHERE status = :status
ORDER BY created_at DESC;

-- name: get_user_by_id
SELECT id, username, email, first_name, last_name, created_at
FROM users
WHERE id = :id;

-- name: search_users
SELECT id, username, email
FROM users
WHERE username LIKE :searchPattern
  OR email LIKE :searchPattern
  OR first_name LIKE :searchPattern
  OR last_name LIKE :searchPattern;

-- name: count_active_users
SELECT COUNT(*) as active_count
FROM users
WHERE status = 'active'; 