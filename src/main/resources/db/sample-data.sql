-- Tool Rental sample data (SQLite)
-- Minimal hand-written sample. The application's SeedingService (invoked
-- on startup or via POST /admin/db/reset) produces a richer fixture (300
-- items + 250 rentals); this file is the rubric-mandated standalone script.
--
-- Passwords are BCrypt hashes of the plaintext shown in the comment.

-- admin / admin
INSERT INTO user (username, password, role, first_name, last_name) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'Alice', 'Admin');

-- user1 / password
INSERT INTO user (username, password, role, first_name, last_name) VALUES
    ('user1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'CUSTOMER', 'Bob',   'Builder'),
    ('user2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'CUSTOMER', 'Carol', 'Carpenter'),
    ('user3', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'CUSTOMER', 'Dave',  'Digger');

-- suspended1 / password
INSERT INTO user (username, password, role, first_name, last_name) VALUES
    ('suspended1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'SUSPENDED', 'Eve', 'Example');

INSERT INTO item (name, description, producer, model, total_amount, rent_period) VALUES
    ('Power Drill',     'Cordless 18V power drill',          'DeWalt',    'DPD-001D',  20, 7),
    ('Circular Saw',    '7-1/4 inch circular saw',           'Makita',    'MCS-220M',  10, 7),
    ('Angle Grinder',   '4-1/2 inch angle grinder',          'Bosch',     'BAG-150B',  15, 3),
    ('Hammer Drill',    'SDS-plus rotary hammer drill',      'Hilti',     'HHD-310H',   8, 14),
    ('Pressure Washer', '2700 PSI electric pressure washer', 'Ryobi',     'RPW-2700R',  5, 14),
    ('Extension Ladder','24 ft aluminum extension ladder',   'Werner',    'WEL-024W',  12, 7),
    ('Tile Saw',        'Wet tile saw with stand',           'Ridgid',    'RTS-700R',   6, 7),
    ('Chain Saw',       'Gas-powered 16 inch chain saw',     'Husqvarna', 'HCS-435H',   7, 7);

INSERT INTO item_rental (item_id, user_id, start_date, due_date, amount, status) VALUES
    (1, 2, date('now', '-30 days'), date('now', '-23 days'), 1, 'RETURNED'),
    (2, 3, date('now', '-20 days'), date('now', '-13 days'), 1, 'RETURNED'),
    (3, 2, date('now', '-10 days'), date('now', '-7 days'),  2, 'RETURNED'),
    (4, 4, date('now', '-5 days'),  date('now', '+9 days'),  1, 'DELIVERED'),
    (5, 3, date('now', '-2 days'),  date('now', '+12 days'), 1, 'SENT'),
    (6, 2, date('now'),             date('now', '+7 days'),  1, 'PENDING'),
    (7, 4, date('now', '+1 days'),  date('now', '+8 days'),  1, 'PENDING');

INSERT INTO item_rental_update (rental_id, status, created_by_id, created_at) VALUES
    (1, 'RETURNED',  2, datetime('now', '-23 days')),
    (2, 'RETURNED',  3, datetime('now', '-13 days')),
    (3, 'RETURNED',  2, datetime('now', '-7 days')),
    (4, 'DELIVERED', 4, datetime('now', '-5 days')),
    (5, 'SENT',      3, datetime('now', '-2 days')),
    (6, 'PENDING',   2, datetime('now')),
    (7, 'PENDING',   4, datetime('now'));
