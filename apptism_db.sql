-- ============================================================
--  Apptism — Script de inicialización
--  Ejecutar con root: mysql -u root -p < apptism_db.sql
-- ============================================================


-- ------------------------------------------------------------
-- 1. BASE DE DATOS
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS apptism_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 2. USUARIO ADMINISTRADOR DE MySQL (con todos los privilegios)
-- ------------------------------------------------------------
CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'admin';
GRANT ALL PRIVILEGES ON apptism_db.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;


-- ------------------------------------------------------------
-- 3. USUARIOS DE PRUEBA DE LA APP
--    (se insertan una vez Hibernate haya creado las tablas)
--    Ejecutar después del primer arranque de la aplicación,
--    o dejar en data.sql para que Spring los inserte solo.
-- ------------------------------------------------------------
USE apptism_db;

INSERT INTO usuarios (nombre, email, password, rol, puntos_acumulados) VALUES
    ('Administrador',   'admin@apptism.com',    'admin',    'ADMIN',    0),
    ('Pedro García',    'padre@apptism.com',    'padre123', 'PADRE',    0),
    ('Laura Martínez',  'profe@apptism.com',    'profe123', 'PROFESOR', 0),
    ('Lucía García',    'nino@apptism.com',     'nino123',  'NINO',     0);
