-- ============================================================
-- Apptism — Datos iniciales
-- Se ejecuta al arrancar la app (Spring Boot)
-- Inserta el usuario administrador por defecto si no existe
-- ============================================================

INSERT INTO usuarios (nombre, email, password, rol, puntos_acumulados)
SELECT 'Administrador', 'admin@apptism.com', 'admin123', 'ADMIN', 0
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'admin@apptism.com'
);
