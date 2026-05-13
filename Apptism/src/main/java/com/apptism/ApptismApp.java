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
 * <p>Arranca el contexto de Spring Boot, abre la ventana principal y lo cierra
 * limpiamente cuando el usuario sale. No se lanza directamente desde el
 * {@code main()}; el control llega aquí a través de {@link Main.AppLauncher},
 * que es quien JavaFX instancia internamente.</p>
 */

public class ApptismApp extends Application {

    /** Contexto de Spring: se crea en {@link #init()} y se cierra en {@link #stop()}. */
    private ConfigurableApplicationContext springContext;

    /**
     * Arranca Spring Boot antes de que aparezca ninguna ventana.
     *
     * <p>JavaFX llama a este método en un hilo separado al hilo de interfaz,
     * por lo que no bloquea la apertura de la ventana.</p>
     */

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Main.class).run();
    }

    /**
     * Abre la ventana principal y navega a la pantalla de inicio de sesión.
     *
     * <p>Obtiene el {@link StageManager} del contexto de Spring, le asigna
     * el escenario principal y carga la vista de login.</p>
     *
     * @param primaryStage la ventana principal que proporciona JavaFX
     */

    @Override
    public void start(Stage primaryStage) {
        StageManager stageManager = springContext.getBean(StageManager.class);
        stageManager.setPrimaryStage(primaryStage);
        stageManager.switchScene(FxmlView.LOGIN);
    }

    /**
     * Cierra el contexto de Spring cuando el usuario cierra la aplicación.
     *
     * <p>Esto libera correctamente las conexiones a la base de datos
     * y demás recursos gestionados por Spring.</p>
     */

    @Override
    public void stop() {
        springContext.close();
    }
}
