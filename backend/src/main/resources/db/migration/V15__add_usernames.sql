ALTER TABLE users ADD COLUMN username VARCHAR(32);

WITH generated AS (
    SELECT id,
           CASE
               WHEN regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._]', '', 'g') ~ '^[a-z0-9]'
                    AND length(regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._]', '', 'g')) >= 3
                   THEN left(regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._]', '', 'g'), 22)
               ELSE 'user'
           END AS base
    FROM users
)
UPDATE users u
SET username = generated.base || '_' || u.id
FROM generated
WHERE generated.id = u.id;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_username_format
    CHECK (username ~ '^[a-z0-9][a-z0-9._]{2,31}$');
CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));
