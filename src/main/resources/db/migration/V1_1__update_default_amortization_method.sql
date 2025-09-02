-- Migración para cambiar el método de amortización por defecto de AMERICANO a ALEMAN
-- Y actualizar bonos existentes si es necesario

-- Actualizar bonos existentes que usan método AMERICANO al método ALEMAN
-- Solo si no hay una preferencia específica del usuario
UPDATE bonos 
SET metodo_amortizacion = 'ALEMAN' 
WHERE metodo_amortizacion = 'AMERICANO' 
   OR metodo_amortizacion IS NULL;

-- Comentario: Esta migración cambia todos los bonos existentes al método alemán
-- Si necesitas conservar algunos bonos con método americano, 
-- modifica esta query con condiciones específicas
