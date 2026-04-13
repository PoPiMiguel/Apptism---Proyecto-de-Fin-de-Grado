package com.apptism.launcher;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.net.URI;

/**
 * Ventana de lanzador que se muestra antes de arrancar Spring Boot.
 *
 * <p>Gestiona la comprobación del estado de MySQL mediante
 * {@link DatabaseChecker} y presenta al usuario uno de los tres
 * estados posibles:
 * <ul>
 *   <li><b>MySQL disponible</b>: cierra el launcher y ejecuta el
 *       callback de éxito para lanzar la aplicación principal.</li>
 *   <li><b>MySQL instalado pero no activo</b>: informa al usuario y
 *       ofrece un botón para reintentar tras arrancar el servicio
 *       manualmente.</li>
 *   <li><b>MySQL no instalado</b>: guía al usuario paso a paso para
 *       descargarlo e instalarlo, con enlace directo a la web oficial.</li>
 * </ul>
 *
 * <p>La comprobación se realiza en un hilo secundario para no bloquear
 * el hilo de la UI de JavaFX. Todas las actualizaciones de la interfaz
 * se delegan a {@link Platform#runLater(Runnable)}.
 */
public class LauncherApp {

    /**
     * Callback que se invoca en el hilo de JavaFX cuando MySQL está
     * disponible y la aplicación principal puede arrancar.
     */
    private final Runnable onSuccess;

    /** Stage principal del launcher. */
    private Stage stage;

    // ── Nodos de la UI reutilizados entre transiciones de estado ──

    /** Etiqueta principal que muestra el estado actual de la comprobación. */
    private Label lblEstado;

    /** Etiqueta secundaria con información adicional o instrucciones. */
    private Label lblDetalle;

    /** Panel contenedor de los botones e instrucciones paso a paso. */
    private VBox panelBotones;

    /**
     * Construye un nuevo launcher con el callback que se ejecutará
     * cuando MySQL esté disponible.
     *
     * @param onSuccess acción a ejecutar en el hilo de JavaFX tras
     *                  confirmar que MySQL está accesible
     */
    public LauncherApp(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Construye la escena del launcher y la muestra en el Stage indicado.
     *
     * <p>Tras mostrar la ventana, lanza en un hilo secundario la
     * comprobación de MySQL mediante {@link #checkDatabase()}.
     *
     * @param primaryStage Stage de JavaFX sobre el que se muestra el launcher
     */
    public void show(Stage primaryStage) {
        this.stage = primaryStage;

        // ── Icono de la aplicación ──
        ImageView logo = new ImageView();
        logo.setFitHeight(80);
        logo.setPreserveRatio(true);
        try {
            logo.setImage(new Image(
                    getClass().getResourceAsStream("/images/apptism_icon.png")
            ));
        } catch (Exception ignored) { /* La ausencia del icono no es un error crítico */ }

        Label lblTitulo = new Label("Apptism");
        lblTitulo.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2F5244;"
        );

        Label lblSubtitulo = new Label("Iniciando la aplicación...");
        lblSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #777;");

        // ── Etiquetas dinámicas de estado ──
        lblEstado = new Label("Comprobando conexión con la base de datos...");
        lblEstado.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2F5244;"
        );
        lblEstado.setWrapText(true);

        lblDetalle = new Label("");
        lblDetalle.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        lblDetalle.setWrapText(true);
        lblDetalle.setMaxWidth(380);

        // ── Indicador de progreso visible durante la comprobación ──
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        spinner.setStyle("-fx-progress-color: #2F5244;");

        // ── Panel de botones: se rellena según el resultado de la comprobación ──
        panelBotones = new VBox(10);
        panelBotones.setAlignment(Pos.CENTER);

        // ── Composición del layout principal ──
        VBox root = new VBox(18, logo, lblTitulo, lblSubtitulo, spinner,
                lblEstado, lblDetalle, panelBotones);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle(
                "-fx-background-color: #FAFAF7;" +
                        "-fx-background-radius: 16px;"
        );

        Scene scene = new Scene(root, 460, 400);
        stage.setScene(scene);
        stage.setTitle("Apptism — Iniciando");
        stage.setResizable(false);
        stage.initStyle(StageStyle.DECORATED);
        stage.show();

        // Comprobación en hilo secundario para no bloquear el hilo de JavaFX
        Thread checker = new Thread(this::checkDatabase);
        checker.setDaemon(true);
        checker.start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Lógica de comprobación de MySQL
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ejecuta la comprobación del estado de MySQL en el siguiente orden:
     * <ol>
     *   <li>Intenta conectar al puerto 3306 con {@link DatabaseChecker#isMySQLRunning()}.</li>
     *   <li>Si falla, comprueba si MySQL está instalado con
     *       {@link DatabaseChecker#isMySQLInstalled()} e intenta arrancar
     *       el servicio con {@link DatabaseChecker#tryStartMySQLService()}.</li>
     *   <li>Si MySQL no está instalado, muestra la guía de instalación.</li>
     * </ol>
     *
     * <p>Este método se ejecuta en un hilo secundario. Todas las
     * actualizaciones de la UI se realizan mediante {@link Platform#runLater(Runnable)}.
     */
    private void checkDatabase() {
        // Paso 1: ¿MySQL está corriendo?
        if (DatabaseChecker.isMySQLRunning()) {
            Platform.runLater(this::onMySQLOk);
            return;
        }

        // Paso 2: MySQL no responde — informar e intentar arrancar el servicio
        Platform.runLater(() -> {
            lblEstado.setText("MySQL no está en ejecución.");
            lblDetalle.setText("Intentando iniciar el servicio automáticamente...");
        });

        if (DatabaseChecker.isMySQLInstalled()) {
            boolean started = DatabaseChecker.tryStartMySQLService();
            if (started) {
                Platform.runLater(this::onMySQLOk);
            } else {
                Platform.runLater(this::onMySQLInstalledButNotStarted);
            }
            return;
        }

        // Paso 3: MySQL no está instalado
        Platform.runLater(this::onMySQLNotInstalled);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Métodos de transición de estado de la UI
    // ─────────────────────────────────────────────────────────────────

    /**
     * Actualiza la UI para indicar que MySQL está disponible y
     * lanza la aplicación principal tras una breve pausa visual.
     */
    private void onMySQLOk() {
        lblEstado.setText("✔ Conexión establecida correctamente.");
        lblDetalle.setText("Cargando Apptism...");
        panelBotones.getChildren().clear();

        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                stage.close();
                onSuccess.run(); // Lanza ApptismApp
            });
        }).start();
    }

    /**
     * Muestra instrucciones para que el usuario arranque manualmente el
     * servicio de MySQL, junto con un botón para reintentar la conexión.
     *
     * <p>Se muestra cuando MySQL está instalado pero el servicio no pudo
     * arrancarse automáticamente.
     */
    private void onMySQLInstalledButNotStarted() {
        lblEstado.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #C0392B;"
        );
        lblEstado.setText("⚠ El servicio de MySQL no pudo iniciarse.");
        lblDetalle.setText(
                "MySQL está instalado pero su servicio no está activo y no se pudo\n" +
                        "arrancar automáticamente. Puedes iniciarlo manualmente siguiendo estos pasos:"
        );

        Label paso1 = styledStep("1. Abre el menú Inicio y busca \"Servicios\".");
        Label paso2 = styledStep("2. Localiza el servicio \"MySQL80\" (o similar).");
        Label paso3 = styledStep("3. Haz clic derecho → Iniciar.");
        Label paso4 = styledStep("4. Una vez iniciado, pulsa el botón de abajo.");

        Button btnReintentar = primaryButton("Reintentar conexión");
        btnReintentar.setOnAction(e -> reintentar());

        panelBotones.getChildren().setAll(paso1, paso2, paso3, paso4, btnReintentar);
    }

    /**
     * Muestra una guía de instalación de MySQL con un enlace a la descarga
     * oficial y un botón para reintentar una vez finalizada la instalación.
     *
     * <p>Se muestra cuando MySQL no está instalado en el sistema.
     */
    private void onMySQLNotInstalled() {
        lblEstado.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #C0392B;"
        );
        lblEstado.setText("⚠ MySQL no está instalado en este equipo.");
        lblDetalle.setText(
                "Apptism necesita MySQL para funcionar. Sigue estos pasos para instalarlo:"
        );

        Label paso1 = styledStep("1. Descarga MySQL Community desde el enlace de abajo.");
        Label paso2 = styledStep("2. Ejecuta el instalador y selecciona \"Developer Default\".");
        Label paso3 = styledStep("3. Completa la instalación (anota la contraseña root).");
        Label paso4 = styledStep("4. Asegúrate de que el servicio MySQL queda en ejecución.");
        Label paso5 = styledStep("5. Pulsa \"Reintentar\" cuando hayas terminado.");

        Hyperlink link = new Hyperlink("Descargar MySQL Community Installer");
        link.setStyle("-fx-font-size: 12px; -fx-text-fill: #2F5244;");
        link.setOnAction(e -> openBrowser("https://dev.mysql.com/downloads/installer/"));

        Button btnReintentar = primaryButton("Reintentar conexión");
        btnReintentar.setOnAction(e -> reintentar());

        Button btnSalir = new Button("Salir");
        btnSalir.setStyle(
                "-fx-background-color: #E8E4DC; -fx-text-fill: #2F5244;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;" +
                        "-fx-padding: 8px 20px; -fx-cursor: hand;"
        );
        btnSalir.setOnAction(e -> Platform.exit());

        HBox botones = new HBox(12, btnReintentar, btnSalir);
        botones.setAlignment(Pos.CENTER);

        panelBotones.getChildren().setAll(
                paso1, paso2, paso3, paso4, paso5, link, botones
        );
    }

    /**
     * Restablece la UI al estado de comprobación y relanza {@link #checkDatabase()}
     * en un nuevo hilo secundario.
     *
     * <p>Se invoca desde los botones «Reintentar conexión» de los estados de error.
     */
    private void reintentar() {
        panelBotones.getChildren().clear();
        lblEstado.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2F5244;"
        );
        lblEstado.setText("Comprobando conexión...");
        lblDetalle.setText("");
        Thread checker = new Thread(this::checkDatabase);
        checker.setDaemon(true);
        checker.start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Métodos auxiliares de UI
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crea una etiqueta con estilo de paso de instrucción.
     *
     * @param text texto descriptivo del paso
     * @return {@link Label} con el estilo aplicado
     */
    private Label styledStep(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(380);
        return lbl;
    }

    /**
     * Crea un botón principal con el estilo corporativo de Apptism.
     *
     * @param text texto que mostrará el botón
     * @return {@link Button} con el estilo aplicado
     */
    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #2F5244; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;" +
                        "-fx-padding: 10px 24px; -fx-cursor: hand;"
        );
        return btn;
    }

    /**
     * Abre la URL indicada en el navegador predeterminado del sistema.
     *
     * @param url dirección web a abrir
     */
    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {}
    }
}