-- Script para poblar la base de datos con datos de ejemplo para Historial de Análisis
-- Este script debe ejecutarse después de la creación de las tablas

-- Insertar bonos de ejemplo
INSERT INTO bonos (nombre, descripcion, valor_nominal, tasa_cupon, plazo_anios, frecuencia_pagos, moneda, fecha_emision, plazos_gracia_total, plazos_gracia_parcial, tasa_descuento, metodo_amortizacion, emisor_username, created_at, updated_at) VALUES 
('Bono Corporativo ABC', 'Bono corporativo de empresa ABC con alta calificación crediticia', 1000.0000, 8.000000, 5, 2, 'PEN', '2024-01-15', 0, 0, 7.500000, 'ALEMAN', 'emisor1', NOW(), NOW()),
('Bono Gubernamental XYZ', 'Bono del gobierno peruano de largo plazo', 5000.0000, 6.000000, 10, 2, 'PEN', '2024-02-01', 0, 0, 5.500000, 'ALEMAN', 'gobierno', NOW(), NOW()),
('Bono Municipal DEF', 'Bono municipal para infraestructura urbana', 2000.0000, 7.500000, 7, 2, 'PEN', '2024-03-10', 0, 1, 7.000000, 'ALEMAN', 'municipio', NOW(), NOW()),
('Bono Hipotecario GHI', 'Bono respaldado por hipotecas residenciales', 3000.0000, 9.000000, 15, 4, 'USD', '2024-04-20', 0, 0, 8.500000, 'ALEMAN', 'banco1', NOW(), NOW()),
('Bono Verde JKL', 'Bono sostenible para proyectos ambientales', 1500.0000, 5.250000, 8, 2, 'USD', '2024-05-15', 0, 0, 5.000000, 'ALEMAN', 'verde_corp', NOW(), NOW());

-- Insertar cálculos de ejemplo para el historial
INSERT INTO calculos (bono_id, inversor_username, tasa_esperada, trea, precio_maximo, fecha_calculo, informacion_adicional, tipo_analisis, valor_nominal, tasa_cupon, plazo_anios, frecuencia_pagos, moneda, trea_porcentaje, valor_presente, created_at, updated_at) VALUES 
(1, 'inversor1', 8.5, 0.0873, 1050.25, DATE_SUB(NOW(), INTERVAL 1 DAY), 'Cálculo TREA para inversión corporativa', 'TREA', 1000.0000, 8.000000, 5, 2, 'PEN', 8.73, 1050.25, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'inversor1', 6.25, 0.0645, 4750.80, DATE_SUB(NOW(), INTERVAL 2 DAY), 'Análisis de bono gubernamental', 'TREA', 5000.0000, 6.000000, 10, 2, 'PEN', 6.45, 4750.80, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'inversor1', 7.8, 0.0795, 1980.45, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Evaluación municipal de medio plazo', 'TREA', 2000.0000, 7.500000, 7, 2, 'PEN', 7.95, 1980.45, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 'inversor1', 9.2, 0.0958, 2850.67, DATE_SUB(NOW(), INTERVAL 7 DAY), 'Análisis de bono hipotecario largo plazo', 'TREA', 3000.0000, 9.000000, 15, 4, 'USD', 9.58, 2850.67, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 'inversor1', 5.5, 0.0578, 1420.30, DATE_SUB(NOW(), INTERVAL 14 DAY), 'Inversión sostenible y verde', 'TREA', 1500.0000, 5.250000, 8, 2, 'USD', 5.78, 1420.30, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY));

-- Insertar más cálculos para hacer el historial más rico
INSERT INTO calculos (bono_id, inversor_username, tasa_esperada, trea, precio_maximo, fecha_calculo, informacion_adicional, tipo_analisis, valor_nominal, tasa_cupon, plazo_anios, frecuencia_pagos, moneda, trea_porcentaje, valor_presente, created_at, updated_at) VALUES 
(1, 'inversor1', 7.8, 0.0826, 1075.45, DATE_SUB(NOW(), INTERVAL 21 DAY), 'Re-evaluación de bono corporativo ABC', 'TREA', 1000.0000, 8.000000, 5, 2, 'PEN', 8.26, 1075.45, DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY)),
(2, 'inversor1', 6.0, 0.0615, 4890.20, DATE_SUB(NOW(), INTERVAL 28 DAY), 'Análisis optimista del bono gubernamental', 'TREA', 5000.0000, 6.000000, 10, 2, 'PEN', 6.15, 4890.20, DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY)),
(5, 'inversor1', 5.8, 0.0601, 1385.75, DATE_SUB(NOW(), INTERVAL 35 DAY), 'Segunda evaluación del bono verde', 'TREA', 1500.0000, 5.250000, 8, 2, 'USD', 6.01, 1385.75, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY));
