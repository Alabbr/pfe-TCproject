INSERT INTO users (first_name, last_name, email, password, role, is_active, created_at) 
VALUES ('Admin', 'Super', 'admin@tunisie-clearing.com', '$2a$10$EblZqNptyYvcLm/VwDCVAuIssDAT/8zDAK42L6TfM8G4Hl4ZlX3x2', 'SUPER_ADMIN', true, NOW()) 
ON CONFLICT (email) DO NOTHING;
