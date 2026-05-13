package com.apptism;

import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Apptism.
 *
 * <p>Lleva la anotación {@code @SpringBootApplication} para que Spring Boot
 * encuentre todos los componentes del proyecto al arrancar. El método
 * {@code main()} no lanza Spring directamente: delega en {@link AppLauncher},
 * que extiende {@link Application} de JavaFX y desde ahí instancia
 * {@link ApptismApp}, que es quien arranca el contexto de Spring y abre
 * la ventana principal.</p>
 *
 * <p>Este doble salto es necesario porque JavaFX requiere que su hilo de
 * interfaz arranque de una forma concreta, incompatible con el arranque
 * directo de Spring Boot.</p>
 */

@SpringBootApplication
public class Main {

    /**
     * Método de entrada de la JVM.
     *
     * <p>Cede el control a JavaFX pasándole {@link AppLauncher} como clase
     * de arranque. A partir de aquí JavaFX gestiona el ciclo de vida.</p>
     *
     * @param args argumentos de línea de comandos (no se usan)
     */

    public static void main(String[] args) {
        Application.launch(AppLauncher.class, args);
    }

    /**
     * Puente entre el hilo de JavaFX y {@link ApptismApp}.
     *
     * <p>JavaFX instancia esta clase internamente y llama a {@link #start(Stage)}.
     * Desde ahí se crea {@link ApptismApp}, se inicializa su contexto de Spring
     * y se abre la ventana principal.</p>
     */

    public static class AppLauncher extends Application {

        /**
         * Crea e inicializa {@link ApptismApp} y delega en ella la apertura
         * de la ventana principal.
         *
         * @param primaryStage la ventana principal que proporciona JavaFX
         * @throws Exception si {@link ApptismApp#init()} o {@link ApptismApp#start(Stage)} fallan
         */

        @Override
        public void start(Stage primaryStage) throws Exception {
            ApptismApp app = new ApptismApp();
            app.init();
            app.start(primaryStage);
        }
    }
}