package com.apptism.ui;

import com.apptism.config.FxmlView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Gestor centralizado de ventanas y navegación entre vistas FXML.
 *
 * <p>Es el componente responsable de cargar los archivos {@code .fxml},
 * inyectar los controladores a través del contexto de Spring Boot y
 * aplicar la hoja de estilos global de la aplicación. Actúa como
 * router de navegación: todos los controladores que necesiten cambiar
 * de pantalla llaman a {@link #switchScene(FxmlView)}.
 *
 * <p>No está anotado con {@code @Component} porque requiere el Stage de
 * JavaFX para funcionar, el cual no está disponible en el momento en que
 * Spring inicializa sus beans. Se registra manualmente en
 * {@link com.apptism.config.ApplicationConfig}.
 */
public class StageManager {

    /** Contexto de Spring, usado como factoría de controladores FXML. */
    private final ApplicationContext springContext;

    /** Ventana principal de la aplicación. Se asigna en {@link #setPrimaryStage(Stage)}. */
    private Stage primaryStage;

    /**
     * Construye el StageManager con el contexto de Spring necesario para
     * la inyección de dependencias en los controladores FXML.
     *
     * @param springContext contexto de Spring Boot activo
     */
    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    /**
     * Asigna la ventana principal de la aplicación.
     *
     * <p>Debe llamarse una sola vez desde {@link com.apptism.ApptismApp#start(Stage)}
     * antes de cualquier llamada a {@link #switchScene(FxmlView)}.
     *
     * @param stage ventana principal proporcionada por JavaFX
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Cambia la vista activa de la ventana principal cargando el FXML
     * correspondiente al valor de {@link FxmlView} indicado.
     *
     * <p>En la primera llamada crea una {@link Scene} nueva con tamaño
     * inicial de 1280×800 y aplica la hoja de estilos global. En llamadas
     * posteriores reutiliza la escena existente y solo sustituye el nodo raíz,
     * evitando recrear la escena cada vez.
     *
     * @param view enumerado que identifica la vista a mostrar y su título
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
     * Carga el archivo FXML indicado y asigna como factoría de controladores
     * el contexto de Spring, de modo que los controladores reciben sus
     * dependencias inyectadas automáticamente.
     *
     * @param fxmlPath ruta al archivo {@code .fxml} dentro del classpath
     * @return nodo raíz del árbol de escena definido en el FXML
     * @throws RuntimeException si el archivo FXML no se encuentra o contiene errores
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
     * Limpia y recarga la hoja de estilos CSS global en la escena indicada.
     *
     * <p>Se llama cada vez que se navega entre vistas para asegurar que los
     * estilos se aplican correctamente tras el cambio de nodo raíz.
     *
     * @param scene escena sobre la que se aplican los estilos
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