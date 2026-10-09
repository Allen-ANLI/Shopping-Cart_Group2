/**
 * 为已有商品数据库补入缺少的商品示例，保留已有同名商品。
 * @author 王重一
 */
-- Demo products and prices are fictional examples for the assignment.
-- Run once on an existing shopping_cart database to add the ten new examples.
-- Existing names are skipped; prices, descriptions and active flags are never updated.
SET NAMES utf8mb4;
USE shopping_cart;
START TRANSACTION;
INSERT INTO products (name, description, price, image_url, active)
SELECT 'Headphones', 'Comfortable over-ear headphones for music, online classes and focused work.', 79.00, '/images/headphones.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Headphones');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'USB-C Hub', 'A compact multi-port USB-C hub for connecting everyday desk accessories.', 39.90, '/images/usb-c-hub.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'USB-C Hub');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Laptop Stand', 'An aluminium stand that raises your laptop for a more comfortable desk setup.', 35.00, '/images/laptop-stand.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Laptop Stand');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Desk Lamp', 'A warm LED desk lamp with an adjustable arm for reading and evening study.', 29.00, '/images/desk-lamp.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Desk Lamp');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Webcam', 'A compact HD webcam for video calls, group meetings and online lessons.', 59.90, '/images/webcam.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Webcam');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Portable SSD', 'A slim portable solid-state drive for keeping documents and project files handy.', 99.90, '/images/portable-ssd.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Portable SSD');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Monitor', 'A clear widescreen monitor that gives your workspace more room for everyday tasks.', 199.90, '/images/monitor.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Monitor');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Bluetooth Speaker', 'A small wireless speaker for music and podcasts during your breaks.', 49.90, '/images/bluetooth-speaker.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Bluetooth Speaker');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'Desk Mat', 'A soft desk mat that keeps your keyboard and mouse area comfortable and tidy.', 12.90, '/images/desk-mat.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Desk Mat');

INSERT INTO products (name, description, price, image_url, active)
SELECT 'USB Microphone', 'A desktop USB microphone for clear voice recordings and online conversations.', 89.90, '/images/usb-microphone.svg', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'USB Microphone');

COMMIT;