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
 * Ventana de arranque que aparece antes de que Spring Boot empiece a cargar.
 *
 * Usa {@link DatabaseChecker} para saber si MySQL está disponible y presenta
 * al usuario uno de estos tres estados:
 *
 * - MySQL funcionando: cierra el lanzador y arranca la aplicación.
 * - MySQL instalado pero parado: pide al usuario que lo arranque y ofrece un botón para reintentar.
 * - MySQL no instalado: guía paso a paso para descargarlo e instalarlo.
 *
 * La comprobación se hace en un hilo secundario para no congelar la interfaz.
 * Todas las actualizaciones de pantalla se delegan al hilo de JavaFX con {@link Platform#runLater}.
 */
public class LauncherApp {

    /** Lo que hay que hacer cuando MySQL esté listo: arrancar la aplicación principal. */
    private final Runnable onSuccess;

    /** La ventana del lanzador. */
    private Stage stage;

    // ── Nodos de la UI reutilizados entre transiciones de estado ──

    /** Etiqueta principal con el estado actual de la comprobación. */
    private Label lblEstado;

    /** Etiqueta secundaria con instrucciones o detalles. */
    private Label lblDetalle;

    /** Panel donde metemos los botones y los pasos de instrucción. */
    private VBox panelBotones;

    /**
     * Crea el lanzador con la acción que se ejecutará cuando MySQL esté listo.
     *
     * @param onSuccess qué hacer cuando MySQL responde correctamente
     */
    public LauncherApp(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Construye la pantalla del lanzador y la muestra. Después lanza en un
     * hilo secundario la comprobación de MySQL con {@link #checkDatabase()}.
     *
     * @param primaryStage la ventana de JavaFX donde mostrar el lanzador
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
     * Comprueba el estado de MySQL en este orden:
     * 1. ¿Está corriendo? → si sí, arrancamos.
     * 2. ¿Está instalado? → intentamos arrancar el servicio.
     * 3. ¿No está instalado? → mostramos la guía de instalación.
     *
     * Se ejecuta en un hilo secundario. Los cambios de pantalla van
     * siempre por {@link Platform#runLater}.
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
     * MySQL está listo: muestra el mensaje de éxito y arranca la aplicación
     * después de una pequeña pausa para que el usuario lo vea.
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
     * MySQL está instalado pero no pudo arrancarse solo. Muestra los pasos
     * para que el usuario lo arranque a mano y un botón para reintentar.
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
     * MySQL no está instalado. Muestra los pasos para descargarlo e instalarlo,
     * con un enlace directo a la web oficial y un botón para reintentar.
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
     * Vuelve al estado de "comprobando..." y lanza otra vez la comprobación
     * de MySQL en un hilo secundario. Lo llaman los botones de reintentar.
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
     * Crea una etiqueta con el estilo de paso numerado para las instrucciones.
     *
     * @param text el texto del paso
     * @return la etiqueta con el estilo aplicado
     */
    private Label styledStep(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(380);
        return lbl;
    }

    /**
     * Crea un botón principal con el estilo visual de Apptism.
     *
     * @param text el texto que mostrará el botón
     * @return el botón con el estilo aplicado
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
     * Abre una URL en el navegador predeterminado del sistema.
     *
     * @param url la dirección que queremos abrir
     */
    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {}
    }
}