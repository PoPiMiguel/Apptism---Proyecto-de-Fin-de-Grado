package com.apptism;

import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        Application.launch(AppLauncher.class, args);
    }

    public static class AppLauncher extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            ApptismApp app = new ApptismApp();
            app.init();
            app.start(primaryStage);
        }
    }
}