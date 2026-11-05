-- Store one local resource reference for every user role.
ALTER TABLE users
    ADD COLUMN profile_picture VARCHAR(255) NULL AFTER is_active;

-- Preserve existing undergraduate profile picture references after moving the
-- common value to users.
UPDATE users u
JOIN undergraduates s ON s.user_id = u.user_id
SET u.profile_picture = s.profile_picture
WHERE u.profile_picture IS NULL
  AND s.profile_picture IS NOT NULL;
