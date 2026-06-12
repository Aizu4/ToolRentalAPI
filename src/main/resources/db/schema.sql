-- Tool Rental schema (SQLite)
-- Mirrors JPA entities. Hibernate normally manages this via ddl-auto=update;
-- this file is provided per the project rubric (requirement 5).

PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS item_rental_update;
DROP TABLE IF EXISTS item_rental;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    username            TEXT    NOT NULL UNIQUE,
    password            TEXT    NOT NULL,
    role                TEXT    NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER', 'SUSPENDED')),
    first_name          TEXT,
    last_name           TEXT,
    email               TEXT    UNIQUE,
    phone_number        TEXT,
    suspension_reason   TEXT,
    -- Embedded Address
    country             TEXT,
    city                TEXT,
    street_name         TEXT,
    street_number       TEXT,
    apartment           TEXT,
    postal_code         TEXT
);

CREATE TABLE item (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    description   TEXT,
    producer      TEXT    NOT NULL,
    model         TEXT    NOT NULL,
    total_amount  INTEGER,
    rent_period   INTEGER NOT NULL
);

CREATE TABLE item_rental (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id     INTEGER NOT NULL REFERENCES item(id),
    user_id     INTEGER NOT NULL REFERENCES user(id),
    start_date  TEXT    NOT NULL,
    due_date    TEXT    NOT NULL,
    amount      INTEGER NOT NULL CHECK (amount >= 1),
    status      TEXT    NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'RETURNED', 'CANCELLED', 'LOST'))
);

CREATE INDEX ix_item_rental_item   ON item_rental(item_id);
CREATE INDEX ix_item_rental_user   ON item_rental(user_id);
CREATE INDEX ix_item_rental_status ON item_rental(status);

CREATE TABLE item_rental_update (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    rental_id     INTEGER NOT NULL REFERENCES item_rental(id),
    status        TEXT    NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'RETURNED', 'CANCELLED', 'LOST')),
    created_by_id INTEGER REFERENCES user(id),
    created_at    TEXT    NOT NULL
);

CREATE INDEX ix_item_rental_update_rental ON item_rental_update(rental_id);
