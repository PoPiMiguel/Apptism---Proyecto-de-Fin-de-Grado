package com.apptism;

import com.apptism.launcher.LauncherApp;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Apptism.
 *
 * Está anotada con {@code @SpringBootApplication} para que Spring
 * encuentre todos los componentes del proyecto. El contexto de Spring
 * no se levanta aquí, sino más adelante dentro de {@link ApptismApp}.
 *
 * El flujo de arranque es este:
 * 1. JavaFX lanza {@link LauncherFxBridge}.
 * 2. {@link LauncherFxBridge} muestra el lanzador, que comprueba MySQL.
 * 3. Si todo va bien, se lanza {@link ApptismApp} en una ventana nueva.
 * 4. {@link ApptismApp} arranca Spring y lleva al usuario al login.
 */
@SpringBootApplication
public class Main {

    /** Punto de entrada de la JVM. Delega el arranque a JavaFX. */
    public static void main(String[] args) {
        Application.launch(LauncherFxBridge.class, args);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Puente entre JavaFX y el lanzador
    // ─────────────────────────────────────────────────────────────────

    /**
     * Aplicación JavaFX auxiliar que muestra la ventana de comprobación
     * de MySQL y, cuando todo está listo, abre la aplicación principal.
     *
     * Está aquí como clase interna para tener toda la lógica de arranque
     * en un solo archivo sin liar la estructura del proyecto.
     */
    public static class LauncherFxBridge extends Application {

        /**
         * Punto de entrada de JavaFX. Muestra el lanzador y, si MySQL
         * responde bien, abre la aplicación principal.
         *
         * @param primaryStage la ventana que nos proporciona JavaFX
         */
        @Override
        public void start(Stage primaryStage) {
            // Se crea el lanzador pasándole qué hacer cuando MySQL esté listo.
            // Ese callback se ejecutará en el hilo de JavaFX.
            LauncherApp launcher = new LauncherApp(() -> {
                Stage appStage = new Stage();
                ApptismApp app = new ApptismApp();
                try {
                    app.init();          // Arranca Spring Boot
                    app.start(appStage); // Muestra la pantalla de login
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            launcher.show(primaryStage);
        }
    }
}
