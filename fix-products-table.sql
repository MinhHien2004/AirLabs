-- Script to fix products table
-- Chay script nay trong pgAdmin hoac psql

-- Drop table cu (NEU TON TAI)
DROP TABLE IF EXISTS products CASCADE;

-- Tao lai table voi cot id tu dong tang
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    price DOUBLE PRECISION,
    description TEXT,
    category VARCHAR(255),
    image VARCHAR(500),
    rate DOUBLE PRECISION,
    count INTEGER
);

-- Kiem tra
SELECT * FROM products;
