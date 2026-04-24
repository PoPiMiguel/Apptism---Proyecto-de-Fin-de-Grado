package com.apptism.ui;

import com.apptism.config.FxmlView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * El gestor de ventanas: se encarga de cargar las pantallas y navegar entre ellas.
 *
 * Todos los controladores que quieran cambiar de pantalla llaman a
 * {@link #switchScene(FxmlView)}. También aplica el CSS global cada vez
 * que se cambia de vista.
 *
 * No lleva {@code @Component} porque necesita el escenario de JavaFX para
 * funcionar, y ese escenario no está disponible cuando Spring arranca sus
 * componentes. Por eso se registra a mano en {@link com.apptism.config.ApplicationConfig}.
 */
public class StageManager {

    /** El contexto de Spring, que usamos para crear los controladores con sus dependencias. */
    private final ApplicationContext springContext;

    /** La ventana principal. Se asigna una sola vez desde {@link #setPrimaryStage(Stage)}. */
    private Stage primaryStage;

    /**
     * Crea el gestor con el contexto de Spring que necesita para inyectar
     * dependencias en los controladores de los FXML.
     *
     * @param springContext el contexto de Spring activo
     */
    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    /**
     * Asigna la ventana principal. Hay que llamar a esto una sola vez
     * desde {@link com.apptism.ApptismApp#start(Stage)} antes de navegar a ninguna pantalla.
     *
     * @param stage la ventana principal de JavaFX
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Cambia la pantalla activa cargando el FXML que corresponde al valor
     * de {@link FxmlView} que se le pasa.
     *
     * La primera vez crea la escena completa con tamaño 1280×800 y aplica
     * el CSS. Las siguientes veces reutiliza la escena y solo cambia el
     * contenido, para no recrearla entera cada vez.
     *
     * @param view la pantalla a la que queremos navegar
     */
    public void switchScene(FxmlView view) {
        Parent root = loadFxml(view.getFxmlPath());
        primaryStage.setTitle(view.getTitle());
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        if (primaryStage.getScene() == null) {
            // Primera carga: se crea la escena completa con estilos
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                    getClass().getResource("/styles/apptism.css").toExternalForm()
            );
            primaryStage.setScene(scene);
        } else {
            // Cambio de vista: se reutiliza la escena y se reaplican los estilos
            primaryStage.getScene().setRoot(root);
            cargarEstilos(primaryStage.getScene());
            primaryStage.setTitle(view.getTitle());
        }
        primaryStage.show();
    }

    /**
     * Carga el archivo FXML indicado y le pide a Spring que cree el controlador,
     * de modo que llegue con todas sus dependencias ya inyectadas.
     *
     * @param fxmlPath ruta al archivo FXML dentro del classpath
     * @return el nodo raíz de la pantalla cargada
     * @throws RuntimeException si el archivo no existe o tiene algún error
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
     * Limpia y recarga el CSS global en la escena. Se llama cada vez que
     * se navega entre pantallas para asegurarse de que los estilos se aplican
     * bien después de cambiar el nodo raíz.
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
