-- Migración para agregar campos adicionales a la tabla calculos
-- para enriquecer el historial de análisis

-- Agregar columnas para el tipo de análisis
ALTER TABLE calculos 
ADD COLUMN tipo_analisis VARCHAR(50) DEFAULT 'TREA';

-- Agregar columnas para parámetros del bono en el momento del cálculo
ALTER TABLE calculos 
ADD COLUMN valor_nominal DECIMAL(19,6),
ADD COLUMN tasa_cupon DECIMAL(19,6),
ADD COLUMN plazo_anios INT,
ADD COLUMN frecuencia_pagos INT,
ADD COLUMN moneda VARCHAR(10);

-- Agregar columnas para resultados adicionales del cálculo  
ALTER TABLE calculos 
ADD COLUMN trea_porcentaje DECIMAL(19,6),
ADD COLUMN valor_presente DECIMAL(19,4);

-- Actualizar registros existentes con valores por defecto
UPDATE calculos c
INNER JOIN bonos b ON c.bono_id = b.id 
SET 
    c.valor_nominal = b.valor_nominal,
    c.tasa_cupon = b.tasa_cupon,
    c.plazo_anios = b.plazo_anios,
    c.frecuencia_pagos = b.frecuencia_pagos,
    c.moneda = b.moneda,
    c.trea_porcentaje = c.trea * 100,
    c.valor_presente = c.precio_maximo
WHERE c.valor_nominal IS NULL;
