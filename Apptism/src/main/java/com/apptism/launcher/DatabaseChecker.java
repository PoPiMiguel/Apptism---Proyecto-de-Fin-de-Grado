package com.apptism.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Utilidad estática para comprobar si MySQL está disponible antes de arrancar
 * el contexto de Spring Boot.
 *
 * Tiene tres métodos que se usan en secuencia desde {@link LauncherApp}:
 * primero mira si MySQL está corriendo, luego si está instalado, y por último
 * intenta arrancarlo si puede. Todos son estáticos, no hace falta instanciar la clase.
 */
public class DatabaseChecker {

    /** Host donde esperamos encontrar MySQL. */
    private static final String HOST = "localhost";

    /** Puerto estándar de MySQL. */
    private static final int PORT = 3306;

    /** Tiempo máximo que esperamos al intentar conectar, en milisegundos. */
    private static final int TIMEOUT_MS = 2000;

    // Clase de utilidad: no se instancia.
    private DatabaseChecker() {}

    /**
     * Comprueba si MySQL está corriendo intentando abrir una conexión al puerto 3306.
     * No necesita usuario ni contraseña, solo mira si el puerto responde.
     *
     * @return {@code true} si el puerto responde antes de los 2 segundos; {@code false} si no
     */
    public static boolean isMySQLRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new java.net.InetSocketAddress(HOST, PORT),
                    TIMEOUT_MS
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si MySQL está instalado buscando el ejecutable {@code mysql}
     * en el PATH del sistema. Ejecuta {@code mysql --version} y mira si la
     * salida contiene la palabra "mysql".
     *
     * @return {@code true} si el ejecutable está en el PATH; {@code false} si no se encuentra
     */
    public static boolean isMySQLInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("mysql", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            process.waitFor();

            return line != null && line.toLowerCase().contains("mysql");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Intenta arrancar el servicio de MySQL en Windows usando {@code net start}.
     * Prueba los nombres más habituales que deja el instalador oficial:
     * {@code MySQL80}, {@code MySQL}, {@code MySQL57} y {@code MySQL56}.
     * Si alguno arranca, espera 2 segundos y comprueba que la conexión funciona.
     *
     * @return {@code true} si algún servicio arrancó y el puerto responde; {@code false} si no
     */
    public static boolean tryStartMySQLService() {
        String[] serviceNames = {"MySQL80", "MySQL", "MySQL57", "MySQL56"};

        for (String serviceName : serviceNames) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "cmd", "/c", "net", "start", serviceName
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    // Esperamos un poco para que el servicio termine de arrancar
                    Thread.sleep(2000);
                    return isMySQLRunning();
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
