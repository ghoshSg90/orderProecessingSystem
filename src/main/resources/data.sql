INSERT INTO user_info (user_name, name, email, mobile_number, password, user_role_category)
VALUES ('supp_admin', 'Support Admin', 'suppAdmin@mail.com', '9999999999', '$2a$10$8GzY0wHpRT237TCcJCZoXO0jeqRgW/qN3iYc7/rCIYt5HqH6KjJ22', 'CUSTOMER_SUPPORT_EXECUTIVE')
ON CONFLICT (user_name) DO NOTHING;

INSERT INTO user_info (user_name, name, email, mobile_number, password, user_role_category)
VALUES ('sghosh', 'Shubhranshu Ghosh', 'sghosh@mail.com', '9999999998', '$2a$10$8GzY0wHpRT237TCcJCZoXO0jeqRgW/qN3iYc7/rCIYt5HqH6KjJ22', 'CUSTOMER')
ON CONFLICT (user_name) DO NOTHING;

INSERT INTO user_info (user_name, name, email, mobile_number, password, user_role_category)
VALUES ('sghosh2', 'Shourjya Ghosh', 'sghosh2@mail.com', '9999999997', '$2a$10$8GzY0wHpRT237TCcJCZoXO0jeqRgW/qN3iYc7/rCIYt5HqH6KjJ22', 'CUSTOMER')
ON CONFLICT (user_name) DO NOTHING;


INSERT INTO address (
    line1,
    line2,
    city,
    state,
    country,
    postal_code,
    address_type,
    user_id
) VALUES
(
    '45 Lake View Road',
    'Near City Park',
    'Bengaluru',
    'Karnataka',
    'India',
    '560001',
    'HOME',
    2
);

INSERT INTO address (
    line1,
    line2,
    city,
    state,
    country,
    postal_code,
    address_type,
    user_id
) VALUES
(
    'Prestige Tech Park, Tower A',
    'Marathahalli',
    'Bengaluru',
    'Karnataka',
    'India',
    '560103',
    'OFFICE',
    2
);

INSERT INTO address (
    line1,
    line2,
    city,
    state,
    country,
    postal_code,
    address_type,
    user_id
) VALUES
(
    '22 Green Avenue',
    'Sector 5',
    'Kolkata',
    'West Bengal',
    'India',
    '700091',
    'HOME',
    3
);

INSERT INTO address (
    line1,
    line2,
    city,
    state,
    country,
    postal_code,
    address_type,
    user_id
) VALUES
(
    'DLF IT Park',
    'New Town',
    'Kolkata',
    'West Bengal',
    'India',
    '700156',
    'OFFICE',
    3
);

INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Clean Code', 'Programming best practices', 45.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Effective Java', 'Java programming guide', 54.50, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Design Patterns', 'Classic GoF patterns', 59.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Spring in Action', 'Spring Boot reference', 49.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Algorithms', 'Algorithms handbook', 39.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('System Design', 'Distributed systems', 44.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Operating Systems', 'OS concepts', 42.75, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Database Systems', 'Database fundamentals', 41.25, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Networking Basics', 'Computer networking', 29.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Linux Essentials', 'Linux administration', 24.99, 'BOOK');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Wireless Mouse', '2.4GHz optical mouse', 19.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Mechanical Keyboard', 'RGB keyboard', 79.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('USB-C Hub', '6 in 1 hub', 34.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Bluetooth Speaker', 'Portable speaker', 49.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('27 Inch Monitor', 'Full HD monitor', 189.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Webcam', '1080p webcam', 39.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('External SSD', '1TB SSD', 99.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Power Bank', '20000mAh', 29.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Noise Cancelling Headphones', 'Over-ear headphones', 149.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Smart Watch', 'Fitness smartwatch', 129.99, 'ELECTRONICS');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Men T-Shirt', 'Cotton t-shirt', 14.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Women T-Shirt', 'Round neck t-shirt', 15.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Jeans', 'Slim fit jeans', 39.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Formal Shirt', 'Office wear', 34.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Hoodie', 'Winter hoodie', 44.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Jacket', 'Water resistant jacket', 69.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Sneakers', 'Casual sneakers', 59.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Socks Pack', 'Pack of five', 9.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Cap', 'Sports cap', 12.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Track Pants', 'Comfort fit', 29.99, 'CLOTHING');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Office Chair', 'Ergonomic chair', 149.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Study Table', 'Wooden study table', 179.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Bookshelf', '5-tier shelf', 119.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Coffee Table', 'Living room table', 99.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('TV Unit', 'Modern TV stand', 199.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Dining Chair', 'Wooden chair', 89.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Bedside Table', 'Compact side table', 79.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Wardrobe', '3-door wardrobe', 399.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Sofa', '3-seater sofa', 599.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Computer Desk', 'Gaming desk', 249.99, 'FURNITURE');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Greeting Card', 'Premium greeting card', 4.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Gift Hamper', 'Chocolate hamper', 29.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Coffee Mug', 'Printed mug', 11.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Photo Frame', 'Wooden frame', 16.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Soft Toy', 'Plush teddy bear', 24.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Scented Candle', 'Lavender candle', 14.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Gift Voucher', 'Store voucher', 50.00, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Keychain', 'Metal keychain', 7.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Desk Plant', 'Indoor succulent', 18.99, 'GIFT');
INSERT INTO product_details (name, description, price_per_unit, product_category) VALUES ('Luxury Pen', 'Gift pen', 39.99, 'GIFT');
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (1, 'Clean Code', 57);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (2, 'Effective Java', 64);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (3, 'Design Patterns', 71);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (4, 'Spring in Action', 78);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (5, 'Algorithms', 85);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (6, 'System Design', 92);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (7, 'Operating Systems', 99);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (8, 'Database Systems', 106);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (9, 'Networking Basics', 113);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (10, 'Linux Essentials', 120);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (11, 'Wireless Mouse', 127);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (12, 'Mechanical Keyboard', 134);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (13, 'USB-C Hub', 141);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (14, 'Bluetooth Speaker', 148);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (15, '27 Inch Monitor', 155);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (16, 'Webcam', 162);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (17, 'External SSD', 169);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (18, 'Power Bank', 176);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (19, 'Noise Cancelling Headphones', 183);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (20, 'Smart Watch', 190);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (21, 'Men T-Shirt', 197);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (22, 'Women T-Shirt', 53);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (23, 'Jeans', 60);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (24, 'Formal Shirt', 67);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (25, 'Hoodie', 74);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (26, 'Jacket', 81);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (27, 'Sneakers', 88);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (28, 'Socks Pack', 95);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (29, 'Cap', 102);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (30, 'Track Pants', 109);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (31, 'Office Chair', 116);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (32, 'Study Table', 123);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (33, 'Bookshelf', 130);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (34, 'Coffee Table', 137);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (35, 'TV Unit', 144);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (36, 'Dining Chair', 151);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (37, 'Bedside Table', 158);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (38, 'Wardrobe', 165);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (39, 'Sofa', 172);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (40, 'Computer Desk', 179);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (41, 'Greeting Card', 186);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (42, 'Gift Hamper', 193);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (43, 'Coffee Mug', 200);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (44, 'Photo Frame', 56);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (45, 'Soft Toy', 63);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (46, 'Scented Candle', 70);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (47, 'Gift Voucher', 77);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (48, 'Keychain', 84);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (49, 'Desk Plant', 91);
INSERT INTO product_inventory (product_id, name, total_available_units) VALUES (50, 'Luxury Pen', 98);
