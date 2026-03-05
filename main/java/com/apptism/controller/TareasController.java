package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Tarea;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.service.TareaService;
import com.apptism.service.RutinaService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class TareasController implements Initializable {

    // --- Vista TUTOR ---
    @FXML private BorderPane panelTutor;
    @FXML private ListView<String> listaTareas;
    @FXML private TextField txtTitulo;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbNinoTutor;   // Selector de niño para tutor
    @FXML private Spinner<Integer> spinnerPuntos;
    @FXML private Label lblPuntosTotales;

    // --- Vista NIÑO ---
    @FXML private StackPane rootStackNino;
    @FXML private FlowPane flowTareasNino;
    @FXML private Label lblPuntosNino;

    @Autowired private TareaService tareaService;
    @Autowired private RutinaService rutinaService; // Para obtener niños del tutor
    @Autowired private StageManager stageManager;

    private List<Tarea> tareasActualesTutor;
    private List<Tarea> tareasActualesNino;
    private List<Usuario> ninosDisponibles;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            SpinnerValueFactory<Integer> factory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 10, 5);
            spinnerPuntos.setValueFactory(factory);

            cmbCategoria.setItems(FXCollections.observableArrayList(
                    "MATEMATICAS", "LENGUA", "ARTE", "JUEGO", "HABITOS"));
            cmbCategoria.getSelectionModel().selectFirst();

            // Cargar niños del tutor en el combo
            ninosDisponibles = rutinaService.getNinosDelTutor(usuario.getId());
            if (!ninosDisponibles.isEmpty()) {
                cmbNinoTutor.setItems(FXCollections.observableArrayList(
                        ninosDisponibles.stream().map(Usuario::getNombre).toList()));
                cmbNinoTutor.getSelectionModel().selectFirst();
            } else {
                cmbNinoTutor.setPromptText("No tienes niños asignados");
            }

            cargarTareasTutor();
        } else {
            cargarTareasNino();
        }
    }

    // ======================== TUTOR ========================

    private void cargarTareasTutor() {
        // Mostrar las tareas creadas por el tutor (para sus niños asignados)
        tareasActualesTutor = tareaService.getTareasDeNinosDelTutor(
                LoginController.usuarioActivo.getId());
        listaTareas.setItems(FXCollections.observableArrayList(
                tareasActualesTutor.stream().map(t ->
                        (t.isCompletada() ? "✅ " : "⬜ ") +
                        "[" + t.getNino().getNombre() + "] " +
                        t.getTitulo() +
                        (t.getPuntosPorCompletar() > 0 ? " [+" + t.getPuntosPorCompletar() + " pts]" : "") +
                        (t.getCategoria() != null ? " · " + t.getCategoria().name() : "")
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

        Long ninoId = ninosDisponibles.get(idxNino).getId();
        Long creadorId = LoginController.usuarioActivo.getId();
        String categoria = cmbCategoria.getValue();
        int puntos = spinnerPuntos.getValue();

        tareaService.crearTarea(titulo, categoria, puntos, ninoId, creadorId);
        txtTitulo.clear();
        cargarTareasTutor();
    }

    @FXML
    private void onEliminarTarea() {
        int idx = listaTareas.getSelectionModel().getSelectedIndex();
        if (idx < 0) { alerta("Selecciona una tarea para eliminar."); return; }
        tareaService.eliminarTarea(tareasActualesTutor.get(idx).getId());
        cargarTareasTutor();
    }

    // ======================== NIÑO ========================

    private void cargarTareasNino() {
        Usuario usuario = LoginController.usuarioActivo;
        tareasActualesNino = tareaService.getTareasByNino(usuario.getId());
        actualizarPuntosNino();
        renderizarTarjetasNino();
    }

    private void renderizarTarjetasNino() {
        if (flowTareasNino == null) return;
        flowTareasNino.getChildren().clear();

        if (tareasActualesNino == null || tareasActualesNino.isEmpty()) {
            Label vacio = new Label("Aún no tienes tareas 🙂\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#999; -fx-text-alignment:center;");
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
        tarjeta.setPrefHeight(240);
        String colorFondo = tarea.isCompletada() ? "#D4F5D4" : "white";
        String catStr = tarea.getCategoria() != null ? tarea.getCategoria().name() : "";
        tarjeta.setStyle(
            "-fx-background-color:" + colorFondo + ";" +
            "-fx-background-radius:24px;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3);"
        );

        Label lblEmoji = new Label(emojiCategoria(catStr));
        lblEmoji.setStyle("-fx-font-size:50px;");
        tarjeta.getChildren().add(lblEmoji);

        Label lblNombre = new Label(tarea.getTitulo());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#444;");
        lblNombre.setMaxWidth(190); lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        Label lblPts = new Label("🏆 +" + tarea.getPuntosPorCompletar() + " pts");
        lblPts.setStyle("-fx-font-size:13px; -fx-text-fill:#D46A00; -fx-font-weight:bold;");
        tarjeta.getChildren().add(lblPts);

        if (!tarea.isCompletada()) {
            Button btn = new Button("✅ ¡Hecho!");
            btn.setStyle(
                "-fx-background-color:#89D9A0; -fx-text-fill:#1a5e35;" +
                "-fx-font-size:15px; -fx-font-weight:bold;" +
                "-fx-background-radius:16px; -fx-padding:9px 18px; -fx-cursor:hand;"
            );
            btn.setOnAction(e -> completarTareaNino(tarea, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            Label lbl = new Label("✅ ¡Completada!");
            lbl.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold; -fx-font-size:13px;");
            tarjeta.getChildren().add(lbl);
        }
        return tarjeta;
    }

    private void completarTareaNino(Tarea tarea, VBox tarjeta) {
        int puntos = tarea.getPuntosPorCompletar();
        int nuevosPuntos = tareaService.completarTarea(tarea.getId());
        LoginController.usuarioActivo.setPuntosAcumulados(nuevosPuntos);
        tarea.setCompletada(true);

        AnimacionUtil.animarExito(tarjeta);
        if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, puntos);

        actualizarPuntosNino();
        Platform.runLater(this::renderizarTarjetasNino);
    }

    private void actualizarPuntosNino() {
        if (lblPuntosNino != null)
            lblPuntosNino.setText("🏆 " + LoginController.usuarioActivo.getPuntosAcumulados() + " puntos");
    }

    private String emojiCategoria(String cat) {
        return switch (cat) {
            case "MATEMATICAS" -> "🔢";
            case "LENGUA"      -> "📖";
            case "ARTE"        -> "🎨";
            case "JUEGO"       -> "🎮";
            case "HABITOS"     -> "🦷";
            default            -> "📚";
        };
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}
