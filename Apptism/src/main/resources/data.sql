/* Este archivo se ejecuta al iniciar la app e inserta el usuario administrador en caso de no existir*/

INSERT INTO usuarios (nombre, email, password, rol, puntos_acumulados)
SELECT 'Administrador', 'admin@apptism.com', 'admin123', 'ADMIN', 0
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'admin@apptism.com'
);
