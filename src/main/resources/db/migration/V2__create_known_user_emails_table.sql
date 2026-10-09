-- Local read-model cache: notification-service never calls user-service synchronously, so
-- the only way it learns a user's email is by consuming UserRegisteredEvent (the only
-- event that carries one) and keeping it here for later lookups when emailing.
CREATE TABLE known_user_emails (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);
