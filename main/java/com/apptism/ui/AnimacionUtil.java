package com.apptism.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Random;

public class AnimacionUtil {

    /**
     * Animación de "éxito" mejorada: escala + rotación tipo celebración ABA.
     */
    public static void animarExito(Node nodo) {
        // 1. Escala hacia arriba
        ScaleTransition escalar = new ScaleTransition(Duration.millis(180), nodo);
        escalar.setToX(1.25); escalar.setToY(1.25);

        // 2. Volver al tamaño original
        ScaleTransition reducir = new ScaleTransition(Duration.millis(220), nodo);
        reducir.setToX(1.0); reducir.setToY(1.0);

        // 3. Rotación de celebración
        RotateTransition rotar = new RotateTransition(Duration.millis(120), nodo);
        rotar.setByAngle(12); rotar.setCycleCount(6); rotar.setAutoReverse(true);

        new SequentialTransition(escalar, rotar, reducir).play();
    }

    /**
     * Mensaje flotante "¡Bien hecho! +X pts" con animación de entrada y salida.
     * Aparece desde abajo, sube flotando y desaparece con fade.
     */
    public static void mostrarPuntos(StackPane contenedor, int puntos) {
        String texto = puntos > 0 ? "🌟 ¡Bien hecho! +" + puntos + " pts" : "🌟 ¡Muy bien! ¡Sigue así!";
        Label lblPuntos = new Label(texto);
        lblPuntos.setStyle(
                "-fx-background-color: #6FCF97;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 18px 36px;" +
                "-fx-background-radius: 36px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 14, 0, 0, 5);"
        );

        Platform.runLater(() -> {
            contenedor.getChildren().add(lblPuntos);

            TranslateTransition subir = new TranslateTransition(Duration.millis(450), lblPuntos);
            subir.setFromY(60); subir.setToY(-40);

            FadeTransition aparecer = new FadeTransition(Duration.millis(350), lblPuntos);
            aparecer.setFromValue(0); aparecer.setToValue(1);

            ParallelTransition entrada = new ParallelTransition(subir, aparecer);

            FadeTransition desaparecer = new FadeTransition(Duration.millis(700), lblPuntos);
            desaparecer.setFromValue(1); desaparecer.setToValue(0);
            desaparecer.setDelay(Duration.millis(1400));
            desaparecer.setOnFinished(e -> contenedor.getChildren().remove(lblPuntos));

            entrada.play();
            desaparecer.play();

            // Lluvia de emojis celebración
            if (puntos > 0) {
                llueviaEmojis(contenedor);
            }
        });
    }

    /**
     * Efecto lluvia de emojis (estrellas, confetti) para refuerzo positivo ABA.
     */
    private static void llueviaEmojis(StackPane contenedor) {
        String[] emojis = {"⭐", "🌟", "🎉", "✨", "🎊", "💫"};
        Random rnd = new Random();

        for (int i = 0; i < 10; i++) {
            final int idx = i;
            int delay = rnd.nextInt(600);

            Label star = new Label(emojis[rnd.nextInt(emojis.length)]);
            double startX = rnd.nextDouble() * 800 - 400;
            star.setStyle("-fx-font-size: " + (18 + rnd.nextInt(20)) + "px;");
            star.setTranslateX(startX);
            star.setTranslateY(200);
            star.setOpacity(0);

            Platform.runLater(() -> contenedor.getChildren().add(star));

            PauseTransition pausa = new PauseTransition(Duration.millis(delay));
            pausa.setOnFinished(e -> {
                FadeTransition fa = new FadeTransition(Duration.millis(200), star);
                fa.setFromValue(0); fa.setToValue(1);

                TranslateTransition sube = new TranslateTransition(Duration.millis(900), star);
                sube.setToY(-300);

                FadeTransition fd = new FadeTransition(Duration.millis(500), star);
                fd.setFromValue(1); fd.setToValue(0);
                fd.setDelay(Duration.millis(500));
                fd.setOnFinished(ev -> Platform.runLater(() -> contenedor.getChildren().remove(star)));

                new ParallelTransition(fa, sube).play();
                fd.play();
            });
            pausa.play();
        }
    }

    /**
     * Parpadeo suave — para destacar un elemento actualizado.
     */
    public static void parpadearVerde(Node nodo) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), nodo);
        fade.setFromValue(1.0); fade.setToValue(0.3);
        fade.setCycleCount(6); fade.setAutoReverse(true);
        fade.play();
    }
}
