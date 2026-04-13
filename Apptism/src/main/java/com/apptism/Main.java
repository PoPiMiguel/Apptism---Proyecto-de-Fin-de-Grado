package com.apptism;

import com.apptism.launcher.LauncherApp;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de Apptism.
 *
 * <p>Actúa como punto de entrada de la aplicación. Está anotada con
 * {@code @SpringBootApplication} para que Spring Boot pueda escanear
 * los componentes del proyecto, aunque el contexto de Spring no se
 * levanta aquí directamente, sino dentro de {@link ApptismApp}.
 *
 * <p>Flujo de arranque:
 * <ol>
 *   <li>JavaFX lanza {@link LauncherFxBridge}.</li>
 *   <li>{@link LauncherFxBridge} muestra {@link LauncherApp}, que comprueba MySQL.</li>
 *   <li>Si MySQL está disponible, se lanza {@link ApptismApp} en un nuevo Stage.</li>
 *   <li>{@link ApptismApp} arranca Spring Boot y navega a la pantalla de login.</li>
 * </ol>
 */
@SpringBootApplication
public class Main {

    /**
     * Punto de entrada de la JVM.
     * Delega el arranque a JavaFX a través de {@link LauncherFxBridge}.
     *
     * @param args argumentos de línea de comandos (no se usan actualmente)
     */
    public static void main(String[] args) {
        Application.launch(LauncherFxBridge.class, args);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Clase interna: puente entre JavaFX y el Launcher
    // ─────────────────────────────────────────────────────────────────

    /**
     * Aplicación JavaFX auxiliar que muestra el launcher de comprobación
     * de MySQL y, cuando éste da luz verde, instancia y lanza {@link ApptismApp}.
     *
     * <p>Se define como clase estática interna para mantener toda la lógica
     * de arranque en un único archivo sin romper la estructura del proyecto.
     */
    public static class LauncherFxBridge extends Application {

        /**
         * Punto de entrada de JavaFX. Muestra el launcher en el Stage primario
         * y, si la comprobación de MySQL es exitosa, abre la aplicación principal.
         *
         * @param primaryStage ventana principal proporcionada por JavaFX
         */
        @Override
        public void start(Stage primaryStage) {
            // Se crea el launcher pasándole el callback de éxito.
            // Cuando MySQL esté disponible, el callback se ejecutará en el hilo de JavaFX.
            LauncherApp launcher = new LauncherApp(() -> {
                Stage appStage = new Stage();
                ApptismApp app = new ApptismApp();
                try {
                    app.init();          // Arranca el contexto de Spring Boot
                    app.start(appStage); // Muestra la pantalla de login
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            launcher.show(primaryStage);
        }
    }
}