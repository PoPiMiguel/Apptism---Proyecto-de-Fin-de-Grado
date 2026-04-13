package com.apptism;

import com.apptism.config.FxmlView;
import com.apptism.ui.StageManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Clase principal de la aplicación JavaFX de Apptism.
 *
 * <p>Extiende {@link Application} de JavaFX e integra el contexto de
 * Spring Boot dentro del ciclo de vida de la ventana gráfica. Es
 * responsable de:
 * <ul>
 *   <li>Arrancar el contexto de Spring Boot en el método {@link #init()}.</li>
 *   <li>Obtener el {@link StageManager} del contexto y mostrar la pantalla
 *       de login en {@link #start(Stage)}.</li>
 *   <li>Cerrar el contexto de Spring Boot al cerrar la ventana en
 *       {@link #stop()}.</li>
 * </ul>
 *
 * <p>Esta clase no se lanza directamente desde {@code main()}; lo hace
 * {@link Main.LauncherFxBridge} una vez que el launcher confirma que
 * MySQL está disponible.
 */
public class ApptismApp extends Application {

    /** Contexto de Spring Boot, inicializado en {@link #init()} y cerrado en {@link #stop()}. */
    private ConfigurableApplicationContext springContext;

    /**
     * Inicializa el contexto de Spring Boot antes de que JavaFX muestre ninguna ventana.
     *
     * <p>Se ejecuta en el hilo de inicialización de JavaFX, no en el hilo de la UI,
     * por lo que es seguro realizar operaciones bloqueantes como el arranque de Spring.
     */
    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Main.class).run();
    }

    /**
     * Configura y muestra la ventana principal de la aplicación.
     *
     * <p>Obtiene el {@link StageManager} del contexto de Spring, le asigna
     * el Stage principal y navega a la vista de login.
     *
     * @param primaryStage ventana principal proporcionada por el runtime de JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        StageManager stageManager = springContext.getBean(StageManager.class);
        stageManager.setPrimaryStage(primaryStage);
        stageManager.switchScene(FxmlView.LOGIN);
    }

    /**
     * Cierra el contexto de Spring Boot cuando el usuario cierra la aplicación.
     *
     * <p>Garantiza que todos los beans de Spring (conexiones a base de datos,
     * repositorios JPA, etc.) se destruyan correctamente antes de que la JVM termine.
     */
    @Override
    public void stop() {
        springContext.close();
    }
}