package com.apptism.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Random;

/**
 * Utilidad estática de animaciones para refuerzo positivo ABA (Applied Behaviour Analysis).
 *
 * <p>Proporciona efectos visuales de celebración que se activan cuando el
 * usuario (niño) completa una tarea, rutina o canjea una recompensa. Estos
 * efectos están diseñados siguiendo principios de refuerzo positivo utilizados
 * en terapias ABA para personas con TEA.
 *
 * <p>Todos los métodos son estáticos. Las actualizaciones de la UI que se
 * invocan desde hilos secundarios se delegan a {@link Platform#runLater(Runnable)}.
 */
public class AnimacionUtil {

    // Constructor privado: clase de utilidad, no se instancia.
    private AnimacionUtil() {}

    /**
     * Reproduce una animación de éxito sobre el nodo indicado.
     *
     * <p>La animación consta de tres fases en secuencia:
     * <ol>
     *   <li>Escala el nodo un 25% hacia arriba (180 ms).</li>
     *   <li>Aplica una rotación de celebración de ±12° (120 ms, 6 ciclos).</li>
     *   <li>Devuelve el nodo a su tamaño original (220 ms).</li>
     * </ol>
     *
     * @param nodo nodo JavaFX sobre el que se aplica la animación
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
     * Muestra un mensaje flotante de puntos obtenidos sobre el contenedor indicado.
     *
     * <p>El mensaje aparece desde la parte inferior, sube flotando y desaparece
     * con un efecto de desvanecimiento. Si los puntos son mayores que cero,
     * se acompaña de un efecto de lluvia de emojis de celebración.
     *
     * <p>Este método es seguro para llamarse desde hilos secundarios ya que
     * todas las operaciones de UI se envuelven en {@link Platform#runLater(Runnable)}.
     *
     * @param contenedor {@link StackPane} sobre el que se superpone el mensaje flotante
     * @param puntos     cantidad de puntos a mostrar; si es 0 se muestra un mensaje
     *                   de ánimo genérico
     */
    public static void mostrarPuntos(StackPane contenedor, int puntos) {
        String texto = puntos > 0
                ? "Has ganado +" + puntos + " pts"
                : "¡Muy bien! ¡Sigue así!";

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

            // Animación de entrada: sube desde abajo mientras aparece
            TranslateTransition subir = new TranslateTransition(Duration.millis(450), lblPuntos);
            subir.setFromY(60);
            subir.setToY(-40);

            FadeTransition aparecer = new FadeTransition(Duration.millis(350), lblPuntos);
            aparecer.setFromValue(0);
            aparecer.setToValue(1);

            ParallelTransition entrada = new ParallelTransition(subir, aparecer);

            // Animación de salida: desaparece tras 1,4 s de pausa
            FadeTransition desaparecer = new FadeTransition(Duration.millis(700), lblPuntos);
            desaparecer.setFromValue(1);
            desaparecer.setToValue(0);
            desaparecer.setDelay(Duration.millis(1400));
            desaparecer.setOnFinished(e -> contenedor.getChildren().remove(lblPuntos));

            entrada.play();
            desaparecer.play();

            if (puntos > 0) {
                lluviaEmojis(contenedor);
            }
        });
    }

    /**
     * Genera un efecto de lluvia de emojis de celebración sobre el contenedor.
     *
     * <p>Lanza 10 emojis en posiciones horizontales aleatorias con retardos
     * escalonados. Cada emoji sube y desaparece con un efecto de desvanecimiento,
     * siendo eliminado del contenedor al finalizar su animación.
     *
     * @param contenedor {@link StackPane} sobre el que se añaden los emojis temporalmente
     */
    private static void lluviaEmojis(StackPane contenedor) {
        String[] emojis = {"⭐", "🌟", "🎉", "✨", "🎊", "💫"};
        Random rnd = new Random();

        for (int i = 0; i < 10; i++) {
            int delay = rnd.nextInt(600);

            Label star = new Label(emojis[rnd.nextInt(emojis.length)]);
            double startX = rnd.nextDouble() * 800 - 400;
            star.setStyle("-fx-font-size: " + (18 + rnd.nextInt(20)) + "px;");
            star.setTranslateX(startX);
            star.setTranslateY(200);
            star.setOpacity(0);

            Platform.runLater(() -> contenedor.getChildren().add(star));

            // Retardo escalonado para que los emojis no aparezcan todos a la vez
            PauseTransition pausa = new PauseTransition(Duration.millis(delay));
            pausa.setOnFinished(e -> {
                FadeTransition fa = new FadeTransition(Duration.millis(200), star);
                fa.setFromValue(0);
                fa.setToValue(1);

                TranslateTransition sube = new TranslateTransition(Duration.millis(900), star);
                sube.setToY(-300);

                FadeTransition fd = new FadeTransition(Duration.millis(500), star);
                fd.setFromValue(1);
                fd.setToValue(0);
                fd.setDelay(Duration.millis(500));
                fd.setOnFinished(ev ->
                        Platform.runLater(() -> contenedor.getChildren().remove(star))
                );

                new ParallelTransition(fa, sube).play();
                fd.play();
            });
            pausa.play();
        }
    }

    /**
     * Aplica un parpadeo suave en verde sobre el nodo indicado para destacar
     * que ha sido actualizado o que ha ocurrido un cambio relevante.
     *
     * <p>La opacidad oscila entre 1,0 y 0,3 en 6 ciclos con reversión automática.
     *
     * @param nodo nodo JavaFX sobre el que se aplica el efecto de parpadeo
     */
    public static void parpadearVerde(Node nodo) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), nodo);
        fade.setFromValue(1.0);
        fade.setToValue(0.3);
        fade.setCycleCount(6);
        fade.setAutoReverse(true);
        fade.play();
    }
}