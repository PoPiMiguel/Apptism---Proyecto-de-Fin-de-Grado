package com.apptism.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Utilidad estática para comprobar el estado de MySQL antes de arrancar
 * el contexto de Spring Boot.
 *
 * <p>Proporciona tres métodos independientes que se ejecutan de forma
 * secuencial desde {@link LauncherApp}:
 * <ol>
 *   <li>{@link #isMySQLRunning()} — comprueba si el puerto 3306 está escuchando.</li>
 *   <li>{@link #isMySQLInstalled()} — comprueba si el ejecutable {@code mysql}
 *       está en el PATH del sistema.</li>
 *   <li>{@link #tryStartMySQLService()} — intenta arrancar el servicio de Windows
 *       con {@code net start}.</li>
 * </ol>
 *
 * <p>Todos los métodos son estáticos y no requieren instanciación.
 */
public class DatabaseChecker {

    /** Host donde se espera encontrar MySQL. */
    private static final String HOST = "localhost";

    /** Puerto estándar de MySQL. */
    private static final int PORT = 3306;

    /** Tiempo máximo de espera para abrir el socket TCP, en milisegundos. */
    private static final int TIMEOUT_MS = 2000;

    // Constructor privado: clase de utilidad, no se instancia.
    private DatabaseChecker() {}

    /**
     * Comprueba si MySQL está activo intentando abrir una conexión TCP
     * al puerto 3306 en localhost.
     *
     * <p>No requiere credenciales; únicamente verifica que el puerto
     * esté escuchando, lo que indica que el servicio está en ejecución.
     *
     * @return {@code true} si el puerto 3306 responde dentro del tiempo
     *         de espera definido; {@code false} en caso contrario
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
     * Comprueba si MySQL está instalado en el sistema buscando el ejecutable
     * {@code mysql} en el PATH del sistema operativo.
     *
     * <p>Ejecuta {@code mysql --version} y analiza la salida. Un resultado
     * que contenga la palabra {@code mysql} indica que el cliente está instalado,
     * aunque el servicio pueda no estar en ejecución.
     *
     * @return {@code true} si el ejecutable {@code mysql} está accesible desde
     *         el PATH; {@code false} si no se encuentra o si ocurre algún error
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
     * Intenta arrancar el servicio de Windows de MySQL usando el comando
     * {@code net start <nombre_servicio>}.
     *
     * <p>Prueba los nombres de servicio más comunes instalados por el instalador
     * oficial de MySQL para Windows: {@code MySQL80}, {@code MySQL},
     * {@code MySQL57} y {@code MySQL56}. Si alguno arranca correctamente,
     * espera 2 segundos para que el servicio esté listo y verifica la conexión
     * con {@link #isMySQLRunning()}.
     *
     * @return {@code true} si alguno de los servicios arrancó y la conexión
     *         TCP al puerto 3306 fue exitosa; {@code false} en caso contrario
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
                    // Espera prudencial para que el servicio esté completamente listo
                    Thread.sleep(2000);
                    return isMySQLRunning();
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}