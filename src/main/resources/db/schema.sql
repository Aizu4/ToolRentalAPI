-- Tool Rental schema (PostgreSQL)
-- Mirrors JPA entities. Hibernate normally manages this via ddl-auto=update;
-- this file is provided per the project rubric (requirement 5).

DROP TABLE IF EXISTS item_rental_update CASCADE;
DROP TABLE IF EXISTS item_rental CASCADE;
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id                  BIGSERIAL    PRIMARY KEY,
    username            VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    role                VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER', 'SUSPENDED')),
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    email               VARCHAR(255) UNIQUE,
    phone_number        VARCHAR(255),
    suspension_reason   VARCHAR(255),
    -- Embedded Address
    country             VARCHAR(255),
    city                VARCHAR(255),
    street_name         VARCHAR(255),
    street_number       VARCHAR(255),
    apartment           VARCHAR(255),
    postal_code         VARCHAR(255)
);

CREATE TABLE item (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    producer      VARCHAR(255) NOT NULL,
    model         VARCHAR(255) NOT NULL,
    total_amount  INTEGER,
    rent_period   INTEGER      NOT NULL
);

CREATE TABLE item_rental (
    id          BIGSERIAL    PRIMARY KEY,
    item_id     BIGINT       NOT NULL REFERENCES item(id),
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    start_date  DATE         NOT NULL,
    due_date    DATE         NOT NULL,
    amount      INTEGER      NOT NULL CHECK (amount >= 1),
    status      VARCHAR(16)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'RETURNED', 'CANCELLED', 'LOST'))
);

CREATE INDEX ix_item_rental_status ON item_rental(status);
CREATE INDEX ix_item_rental_user   ON item_rental(user_id);

CREATE TABLE item_rental_update (
    id            BIGSERIAL    PRIMARY KEY,
    rental_id     BIGINT       NOT NULL REFERENCES item_rental(id),
    status        VARCHAR(16)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'RETURNED', 'CANCELLED', 'LOST')),
    created_by_id BIGINT       REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_item_rental_update_rental_created ON item_rental_update(rental_id, created_at);
CREATE INDEX ix_item_rental_update_created        ON item_rental_update(created_at);
