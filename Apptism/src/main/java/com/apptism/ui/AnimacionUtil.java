package com.apptism.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Random;

/**
 * Utilidad estática con las animaciones de refuerzo positivo de la aplicación.
 *
 * <p>Los efectos visuales de celebración se activan cuando el niño completa
 * una tarea o canjea una recompensa. Están diseñados siguiendo principios
 * de accesibilidad para personas con TEA: breves, claros y no intrusivos.</p>
 *
 * <p>Todos los métodos son estáticos. Los que modifican la interfaz desde
 * hilos secundarios envuelven sus cambios en {@link Platform#runLater}.</p>
 */

public class AnimacionUtil {

    /** Clase de utilidad: no se instancia. */

    private AnimacionUtil() {}

    /**
     * Reproduce una animación de éxito sobre el nodo indicado.
     *
     * <p>Consta de tres fases encadenadas:</p>
     * <ol>
     *   <li>Escala el nodo un 25% hacia arriba (180 ms).</li>
     *   <li>Lo rota de un lado a otro como celebración (120 ms, 6 ciclos).</li>
     *   <li>Lo devuelve a su tamaño original (220 ms).</li>
     * </ol>
     *
     * @param nodo el elemento de JavaFX sobre el que se aplica la animación
     */

    public static void animarExito(Node nodo) {
        ScaleTransition escalar = new ScaleTransition(Duration.millis(180), nodo);
        escalar.setToX(1.25);
        escalar.setToY(1.25);

        ScaleTransition reducir = new ScaleTransition(Duration.millis(220), nodo);
        reducir.setToX(1.0);
        reducir.setToY(1.0);

        RotateTransition rotar = new RotateTransition(Duration.millis(120), nodo);
        rotar.setByAngle(12);
        rotar.setCycleCount(6);
        rotar.setAutoReverse(true);

        new SequentialTransition(escalar, rotar, reducir).play();
    }

    /**
     * Muestra un mensaje flotante con los puntos conseguidos sobre el contenedor.
     *
     * <p>El mensaje sube desde abajo mientras aparece, permanece visible un
     * momento y luego se desvanece. Si los puntos son mayores que cero se
     * muestra la cantidad ganada; si no, aparece un mensaje de ánimo genérico.</p>
     *
     * <p>Es seguro llamarlo desde un hilo secundario: todos los cambios de
     * interfaz se ejecutan dentro de {@link Platform#runLater}.</p>
     *
     * @param contenedor el {@link StackPane} sobre el que se superpone el mensaje flotante
     * @param puntos     puntos a mostrar; si es 0 se muestra un mensaje de ánimo
     */

    public static void mostrarPuntos(StackPane contenedor, int puntos) {
        String texto = puntos > 0
                ? "Has ganado +" + puntos + " pts"
                : "Muy bien";

        Label lblPuntos = new Label(texto);
        lblPuntos.setStyle(
                "-fx-background-color: -apptism-principal;" +
                        "-fx-text-fill: -apptism-text-light;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 18px 36px;" +
                        "-fx-background-radius: 36px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 14, 0, 0, 5);"
        );

        Platform.runLater(() -> {
            contenedor.getChildren().add(lblPuntos);

            TranslateTransition subir = new TranslateTransition(Duration.millis(450), lblPuntos);
            subir.setFromY(60);
            subir.setToY(-40);

            FadeTransition aparecer = new FadeTransition(Duration.millis(350), lblPuntos);
            aparecer.setFromValue(0);
            aparecer.setToValue(1);

            ParallelTransition entrada = new ParallelTransition(subir, aparecer);

            FadeTransition desaparecer = new FadeTransition(Duration.millis(700), lblPuntos);
            desaparecer.setFromValue(1);
            desaparecer.setToValue(0);
            desaparecer.setDelay(Duration.millis(1400));
            desaparecer.setOnFinished(e -> contenedor.getChildren().remove(lblPuntos));

            entrada.play();
            desaparecer.play();
        });
    }

}
