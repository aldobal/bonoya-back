-- Script para insertar datos iniciales en BonoFácil
-- Ejecutar en tu base de datos PostgreSQL de producción

-- 1. Insertar roles base
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_ADMIN'),
(2, 'ROLE_EMISOR'), 
(3, 'ROLE_INVERSOR')
ON CONFLICT (name) DO NOTHING;

-- 2. Insertar usuarios de prueba (contraseñas encriptadas con BCrypt)
-- password: "admin123" -> $2a$10$N2YF.S0Z6gGXeO5LqKM2JOKjULn1/Sh.8JKt3KuN6MgKS.LNnFTbK
-- password: "emisor123" -> $2a$10$xyz... (necesitas generar)
-- password: "inversor123" -> $2a$10$abc... (necesitas generar)

-- Usuario admin
INSERT INTO users (id, username, password) VALUES 
(1, 'admin', '$2a$10$N2YF.S0Z6gGXeO5LqKM2JOKjULn1/Sh.8JKt3KuN6MgKS.LNnFTbK')
ON CONFLICT (username) DO NOTHING;

-- Usuario emisor (temporal - actualiza la contraseña)
INSERT INTO users (id, username, password) VALUES 
(2, 'emisor', '$2a$10$N2YF.S0Z6gGXeO5LqKM2JOKjULn1/Sh.8JKt3KuN6MgKS.LNnFTbK')
ON CONFLICT (username) DO NOTHING;

-- Usuario inversor (temporal - actualiza la contraseña)
INSERT INTO users (id, username, password) VALUES 
(3, 'inversor', '$2a$10$N2YF.S0Z6gGXeO5LqKM2JOKjULn1/Sh.8JKt3KuN6MgKS.LNnFTbK')
ON CONFLICT (username) DO NOTHING;

-- 3. Asignar roles a usuarios
INSERT INTO user_roles (user_id, role_id) VALUES 
(1, 1), -- admin -> ROLE_ADMIN
(2, 2), -- emisor -> ROLE_EMISOR  
(3, 3)  -- inversor -> ROLE_INVERSOR
ON CONFLICT DO NOTHING;

-- 4. Verificar los datos insertados
SELECT u.username, r.name as role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id;
