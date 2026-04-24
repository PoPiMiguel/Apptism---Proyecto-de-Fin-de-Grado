package com.apptism;

import com.apptism.config.FxmlView;
import com.apptism.ui.StageManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Clase principal de la aplicación JavaFX.
 *
 * Es la que arranca Spring Boot, abre la ventana y la cierra cuando
 * el usuario sale. No se lanza directamente desde el {@code main()};
 * primero pasa por {@link Main.LauncherFxBridge}, que comprueba que
 * MySQL esté disponible antes de llegar aquí.
 */
public class ApptismApp extends Application {

    /** El contexto de Spring: se crea al iniciar y se cierra al salir. */
    private ConfigurableApplicationContext springContext;

    /**
     * Arranca Spring Boot antes de que aparezca ninguna ventana.
     * Se ejecuta en un hilo separado al de la interfaz, así que no bloquea nada.
     */
    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Main.class).run();
    }

    /**
     * Abre la ventana principal y navega a la pantalla de inicio de sesión.
     *
     * @param primaryStage la ventana que nos proporciona JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        StageManager stageManager = springContext.getBean(StageManager.class);
        stageManager.setPrimaryStage(primaryStage);
        stageManager.switchScene(FxmlView.LOGIN);
    }

    /**
     * Cierra el contexto de Spring cuando el usuario cierra la aplicación,
     * para que se limpien bien las conexiones a la base de datos y demás.
     */
    @Override
    public void stop() {
        springContext.close();
    }
}
