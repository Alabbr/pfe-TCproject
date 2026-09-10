CREATE EXTENSION IF NOT EXISTS pgcrypto;
UPDATE users SET password = crypt('admin123123', gen_salt('bf', 10)) WHERE email = 'admin@tunisie-clearing.com';
