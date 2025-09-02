-- Agregar restricciones de longitud a las columnas de texto
ALTER TABLE bonos 
    ALTER COLUMN nombre TYPE VARCHAR(100),
    ALTER COLUMN descripcion TYPE VARCHAR(500),
    ALTER COLUMN moneda TYPE VARCHAR(10),
    ALTER COLUMN metodo_amortizacion TYPE VARCHAR(20),
    ALTER COLUMN emisor_username TYPE VARCHAR(50); 