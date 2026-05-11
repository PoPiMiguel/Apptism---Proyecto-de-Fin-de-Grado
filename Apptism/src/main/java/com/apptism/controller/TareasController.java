// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: TareasController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Tarea;
import com.apptism.entity.Usuario;
import com.apptism.service.ArasaacService;
import com.apptism.service.ArasaacService.PictogramaDTO;
import com.apptism.service.RutinaService;
import com.apptism.service.TareaService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de tareas.
 *
 * Vista tutor: crear tareas para sus niños con un pictograma de ARASAAC,
 * verlas y eliminarlas.
 * Vista niño: ver sus tareas pendientes como tarjetas visuales (con pictograma)
 * y marcarlas como completadas para ganar estrellas.
 */
@Component
public class TareasController implements Initializable {

    @FXML private BorderPane panelTutor;
    @FXML private ListView<String> listaTareas;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtBuscarPicto;
    @FXML private FlowPane  panelPictosTarea;
    @FXML private ComboBox<String> cmbNinoTutor;
    @FXML private Spinner<Integer> spinnerPuntos;
    @FXML private Label lblPuntosTotales;

    @FXML private StackPane rootStackNino;
    @FXML private FlowPane  flowTareasNino;
    @FXML private Label     lblPuntosNino;

    @Autowired private TareaService  tareaService;
    @Autowired private RutinaService rutinaService;
    @Autowired private ArasaacService arasaacService;
    @Autowired private StageManager  stageManager;

    private List<Tarea>   tareasActualesTutor;
    private List<Tarea>   tareasActualesNino;
    private List<Usuario> ninosDisponibles;

    /** Pictograma seleccionado actualmente en el formulario del tutor. */
    private PictogramaDTO pictogramaSeleccionado = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE
                || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            SpinnerValueFactory<Integer> factory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 10, 5);
            spinnerPuntos.setValueFactory(factory);

            ninosDisponibles = rutinaService.getNinosDelTutor(usuario.getId());
            if (!ninosDisponibles.isEmpty()) {
                cmbNinoTutor.setItems(FXCollections.observableArrayList(
                        ninosDisponibles.stream().map(Usuario::getNombre).toList()));
                cmbNinoTutor.getSelectionModel().selectFirst();
            } else {
                cmbNinoTutor.setPromptText("No tienes niños asignados");
            }

            txtBuscarPicto.setOnAction(e -> buscarPictogramas());

            cargarTareasTutor();
        } else {
            cargarTareasNino();
        }
    }

    @FXML
    private void buscarPictogramas() {
        String palabra = txtBuscarPicto.getText().trim();
        if (palabra.isBlank()) return;

        panelPictosTarea.getChildren().clear();
        Label cargando = new Label("Buscando...");
        cargando.setStyle("-fx-text-fill:#888; -fx-font-size:13px;");
        panelPictosTarea.getChildren().add(cargando);

        new Thread(() -> {
            List<PictogramaDTO> resultados = arasaacService.buscar(palabra);
            Platform.runLater(() -> {
                panelPictosTarea.getChildren().clear();
                if (resultados.isEmpty()) {
                    Label vacio = new Label("Sin resultados para \"" + palabra + "\"");
                    vacio.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");
                    panelPictosTarea.getChildren().add(vacio);
                } else {
                    resultados.forEach(this::agregarPictoSeleccionable);
                }
            });
        }).start();
    }

    /** Crea una tarjeta de pictograma seleccionable en el formulario del tutor. */
    private void agregarPictoSeleccionable(PictogramaDTO picto) {
        VBox tarjeta = new VBox(3);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(5));
        tarjeta.setPrefWidth(80);
        tarjeta.setUserData(picto.id());
        tarjeta.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;");

        ImageView img = new ImageView();
        img.setFitWidth(56);
        img.setFitHeight(56);
        img.setPreserveRatio(true);
        new Thread(() -> {
            try {
                Image imagen = new Image(picto.url(), 56, 56, true, true, true);
                Platform.runLater(() -> img.setImage(imagen));
            } catch (Exception ignored) {}
        }).start();

        Label lbl = new Label(picto.nombre());
        lbl.setStyle("-fx-font-size:10px;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(74);

        tarjeta.getChildren().addAll(img, lbl);

        tarjeta.setOnMouseClicked(e -> seleccionarPictograma(picto, tarjeta));
        tarjeta.setOnMouseEntered(e ->
                tarjeta.setStyle("-fx-background-color:#DFFAEC; -fx-background-radius:10px; -fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e -> {
            boolean esSel = pictogramaSeleccionado != null
                    && pictogramaSeleccionado.id() == (int) tarjeta.getUserData();
            tarjeta.setStyle(esSel
                    ? "-fx-background-color:#B8EDD9; -fx-background-radius:10px; -fx-border-color:#4A6F5A; -fx-border-radius:10px; -fx-cursor:hand;"
                    : "-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;");
        });

        panelPictosTarea.getChildren().add(tarjeta);
    }

    /** Marca el pictograma elegido y desmarca el anterior. */
    private void seleccionarPictograma(PictogramaDTO picto, VBox tarjetaActual) {
        // Desmarcar anterior
        panelPictosTarea.getChildren().forEach(n ->
                n.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;"));
        // Marcar actual
        tarjetaActual.setStyle(
                "-fx-background-color:#B8EDD9; -fx-background-radius:10px;" +
                        "-fx-border-color:#4A6F5A; -fx-border-radius:10px; -fx-cursor:hand;");
        pictogramaSeleccionado = picto;
    }

    private void cargarTareasTutor() {
        tareasActualesTutor = tareaService.getTareasDeNinosDelTutor(
                LoginController.usuarioActivo.getId());
        listaTareas.setItems(FXCollections.observableArrayList(
                tareasActualesTutor.stream().map(t ->
                        (t.isCompletada() ? "[OK] " : "[ ] ") +
                                "[" + t.getNino().getNombre() + "] " +
                                t.getTitulo() +
                                (t.getPuntosPorCompletar() > 0
                                        ? " [+" + t.getPuntosPorCompletar() + " pts]" : "") +
                                (t.getPictogramaId() != null ? " 🖼" : "")
                ).toList()
        ));
        lblPuntosTotales.setText(tareasActualesTutor.size() + " tareas asignadas");
    }

    @FXML
    private void onCrearTarea() {
        String titulo = txtTitulo.getText().trim();
        if (titulo.isBlank()) {
            alerta("El título de la tarea es obligatorio.");
            return;
        }
        int idxNino = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        if (idxNino < 0 || ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            alerta("Selecciona un niño al que asignar la tarea.\n" +
                    "Asegúrate de que tienes niños asignados en el sistema.");
            return;
        }

        Long ninoId    = ninosDisponibles.get(idxNino).getId();
        Long creadorId = LoginController.usuarioActivo.getId();
        int  puntos    = spinnerPuntos.getValue();

        Integer pictoId  = pictogramaSeleccionado != null ? pictogramaSeleccionado.id() : null;
        String  pictoUrl = pictogramaSeleccionado != null ? pictogramaSeleccionado.url() : null;

        tareaService.crearTarea(titulo, pictoId, pictoUrl, puntos, ninoId, creadorId);
        txtTitulo.clear();
        txtBuscarPicto.clear();
        panelPictosTarea.getChildren().clear();
        pictogramaSeleccionado = null;
        cargarTareasTutor();
    }

    @FXML
    private void onEliminarTarea() {
        int idx = listaTareas.getSelectionModel().getSelectedIndex();
        if (idx < 0) { alerta("Selecciona una tarea para eliminar."); return; }
        tareaService.eliminarTarea(tareasActualesTutor.get(idx).getId());
        cargarTareasTutor();
    }

    private void cargarTareasNino() {
        tareasActualesNino = tareaService.getTareasByNino(
                LoginController.usuarioActivo.getId());
        actualizarPuntosNino();
        renderizarTarjetasNino();
    }

    private void renderizarTarjetasNino() {
        if (flowTareasNino == null) return;
        flowTareasNino.getChildren().clear();

        if (tareasActualesNino == null || tareasActualesNino.isEmpty()) {
            Label vacio = new Label("Aún no tienes tareas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowTareasNino.getChildren().add(vacio);
            return;
        }

        for (Tarea t : tareasActualesNino) {
            flowTareasNino.getChildren().add(crearTarjetaTareaNino(t));
        }
    }

    private VBox crearTarjetaTareaNino(Tarea tarea) {
        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(210);
        tarjeta.setPrefHeight(260);
        String colorFondo = tarea.isCompletada() ? "#E8FAF4" : "white";
        tarjeta.setStyle(
                "-fx-background-color:" + colorFondo + ";" +
                        "-fx-background-radius:24px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3);"
        );

        if (tarea.getPictogramaUrl() != null && !tarea.getPictogramaUrl().isBlank()) {
            ImageView img = new ImageView();
            img.setFitWidth(90);
            img.setFitHeight(90);
            img.setPreserveRatio(true);
            new Thread(() -> {
                try {
                    Image imagen = new Image(tarea.getPictogramaUrl(), 90, 90, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {}
            }).start();
            tarjeta.getChildren().add(img);
        }

        Label lblNombre = new Label(tarea.getTitulo());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(185);
        lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        if (tarea.getPuntosPorCompletar() > 0) {
            Label lblEstrellas = new Label(puntosAEstrellas(tarea.getPuntosPorCompletar()));
            lblEstrellas.setStyle("-fx-font-size:16px;");
            tarjeta.getChildren().add(lblEstrellas);
        }

        if (!tarea.isCompletada()) {
            Button btn = new Button("¡Hecho!");
            btn.setStyle(
                    "-fx-background-color:#B8EDD9; -fx-text-fill:#4A6F5A;" +
                            "-fx-font-size:15px; -fx-font-weight:bold;" +
                            "-fx-background-radius:16px; -fx-padding:9px 18px; -fx-cursor:hand;"
            );
            btn.setOnAction(e -> completarTareaNino(tarea, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            Label lbl = new Label("✅ Completada");
            lbl.setStyle("-fx-text-fill:#81D8A3; -fx-font-weight:bold; -fx-font-size:13px;");
            tarjeta.getChildren().add(lbl);
        }
        return tarjeta;
    }

    private void completarTareaNino(Tarea tarea, VBox tarjeta) {
        int puntos      = tarea.getPuntosPorCompletar();
        int nuevosPuntos = tareaService.completarTarea(tarea.getId());
        LoginController.usuarioActivo.setPuntosAcumulados(nuevosPuntos);
        tarea.setCompletada(true);

        if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, puntos);

        actualizarPuntosNino();
        Platform.runLater(this::renderizarTarjetasNino);
    }

    private void actualizarPuntosNino() {
        if (lblPuntosNino != null)
            lblPuntosNino.setText(
                    puntosAEstrellas(LoginController.usuarioActivo.getPuntosAcumulados()));
    }

    /**
     * Convierte una cantidad de puntos en emojis visuales para el niño.
     * Escala: 1 punto = 1 ⭐ | cada 10 estrellas = 1 👑 | cada 5 coronas = 1 💎
     */
    static String puntosAEstrellas(int puntos) {
        if (puntos <= 0) return "";

        int diamantes = puntos / 50;
        int resto1    = puntos % 50;
        int coronas   = resto1 / 10;
        int estrellas = resto1 % 10;

        StringBuilder sb = new StringBuilder();
        sb.append("💎".repeat(diamantes));
        sb.append("👑".repeat(coronas));
        sb.append("⭐".repeat(estrellas));
        return sb.toString();
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}