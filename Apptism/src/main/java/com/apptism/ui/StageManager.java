package com.apptism.ui;

import com.apptism.config.FxmlView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

// ← SIN @Component: Spring no lo instancia solo
public class StageManager {

    private final ApplicationContext springContext;
    private Stage primaryStage;

    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

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

    private Parent loadFxml(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            return loader.load();
        } catch (Exception e) {
            throw new RuntimeException("Error cargando FXML: " + fxmlPath, e);
        }
    }

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
