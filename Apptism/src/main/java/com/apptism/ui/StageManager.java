package com.apptism.ui;

import com.apptism.config.FxmlView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Gestor de ventanas de la aplicación.
 *
 * <p>Centraliza la carga de pantallas y la navegación entre ellas.
 * Todos los controladores que necesiten cambiar de vista llaman a
 * {@link #switchScene(FxmlView)}. El CSS global se aplica (o recarga)
 * en cada cambio de pantalla.</p>
 *
 * <p>No lleva {@code @Component} porque necesita el escenario de JavaFX
 * para funcionar, y ese escenario no está disponible cuando Spring
 * inicializa sus componentes. Por eso se registra manualmente como bean
 * en {@link com.apptism.config.ApplicationConfig}.</p>
 */

public class StageManager {

    /** Contexto de Spring, usado para crear los controladores con sus dependencias inyectadas. */

    private final ApplicationContext springContext;

    /** Ventana principal. Se asigna una sola vez desde {@link #setPrimaryStage(Stage)}. */

    private Stage primaryStage;

    /**
     * Crea el gestor con el contexto de Spring necesario para inyectar
     * dependencias en los controladores de los FXML.
     *
     * @param springContext el contexto de Spring activo
     */

    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    /**
     * Asigna la ventana principal.
     *
     * <p>Debe llamarse una sola vez desde {@link com.apptism.ApptismApp#start(Stage)}
     * antes de navegar a cualquier pantalla.</p>
     *
     * @param stage la ventana principal de JavaFX
     */

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Cambia la pantalla activa cargando el FXML correspondiente al valor
     * de {@link FxmlView} indicado.
     *
     * <p>La primera vez crea la escena con tamaño 1280×800 y aplica el CSS.
     * Las siguientes veces reutiliza la escena y solo sustituye el nodo raíz,
     * evitando recrearla entera en cada navegación.</p>
     *
     * @param view la pantalla a la que se desea navegar
     */

    public void switchScene(FxmlView view) {
        Parent root = loadFxml(view.getFxmlPath());
        primaryStage.setTitle(view.getTitle());
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/apptism.css").toExternalForm()
            );
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(root);
            cargarEstilos(primaryStage.getScene());
            primaryStage.setTitle(view.getTitle());
        }
        primaryStage.show();
    }

    /**
     * Carga el archivo FXML indicado y delega en Spring la creación del
     * controlador, para que llegue con todas sus dependencias inyectadas.
     *
     * @param fxmlPath ruta al archivo FXML dentro del classpath
     * @return el nodo raíz de la pantalla cargada
     * @throws RuntimeException si el archivo no existe o contiene algún error
     */

    private Parent loadFxml(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            return loader.load();
        } catch (Exception e) {
            throw new RuntimeException("Error cargando FXML: " + fxmlPath, e);
        }
    }

    /**
     * Limpia y recarga el CSS global en la escena.
     *
     * <p>Se llama en cada navegación para asegurar que los estilos
     * se aplican correctamente tras sustituir el nodo raíz.</p>
     *
     * @param scene la escena sobre la que aplicar los estilos
     */

    private void cargarEstilos(Scene scene) {
        try {
            String rutaCss = getClass().getResource("/styles/apptism.css").toExternalForm();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(rutaCss);
        } catch (Exception e) {
            System.err.println("Error cargando CSS: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
