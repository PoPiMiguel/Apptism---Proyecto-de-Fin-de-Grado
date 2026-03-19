package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Rutina;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.entity.ZonaHoraria;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class RutinasController implements Initializable {

    // --- Vista TUTOR ---

    @FXML private BorderPane panelTutor;
    @FXML private TabPane tabZonas;
    @FXML private ListView<String> listaManana;
    @FXML private ListView<String> listaMediodia;
    @FXML private ListView<String> listaNoche;
    @FXML private TextField txtNombreRutina;
    @FXML private ComboBox<String> cmbZona;
    @FXML private ComboBox<String> cmbNinoTutor;  // Selector de niño para tutor

    // --- Vista NIÑO ---
    // rootStackNino es el StackPane INTERNO (no el raíz) — así se puede inyectar
    @FXML private StackPane rootStackNino;
    @FXML private FlowPane flowRutinasNino;

    @Autowired private RutinaService rutinaService;
    @Lazy @Autowired private StageManager stageManager;

    private List<Rutina> rutinasActualesNino;
    private List<Usuario> ninosDisponibles; // Para el tutor

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            cmbZona.setItems(FXCollections.observableArrayList("MANANA", "MEDIODIA", "NOCHE"));
            cmbZona.setValue("MANANA");
            cargarNinosEnComboTutor();
            cargarTodasLasRutinasTutor();
        } else {
            cargarRutinasNino();
        }
    }

    // ======================== VISTA TUTOR ========================

    private void cargarNinosEnComboTutor() {
        ninosDisponibles = rutinaService.getNinosDelTutor(LoginController.usuarioActivo.getId());
        if (ninosDisponibles.isEmpty()) {
            cmbNinoTutor.setPromptText("No tienes niños asignados");
        } else {
            cmbNinoTutor.setItems(FXCollections.observableArrayList(
                    ninosDisponibles.stream().map(Usuario::getNombre).toList()));
            cmbNinoTutor.getSelectionModel().selectFirst();
        }
    }

    private void cargarTodasLasRutinasTutor() {
        // Mostrar rutinas de todos los niños del tutor
        if (ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            listaManana.setItems(FXCollections.observableArrayList());
            listaMediodia.setItems(FXCollections.observableArrayList());
            listaNoche.setItems(FXCollections.observableArrayList());
            return;
        }
        // Usar primer niño seleccionado para mostrar
        int idx = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        Long ninoId = (idx >= 0) ? ninosDisponibles.get(idx).getId()
                                 : ninosDisponibles.get(0).getId();

        cargarRutinasPorZona(ninoId, ZonaHoraria.MANANA, listaManana);
        cargarRutinasPorZona(ninoId, ZonaHoraria.MEDIODIA, listaMediodia);
        cargarRutinasPorZona(ninoId, ZonaHoraria.NOCHE, listaNoche);
    }

    private void cargarRutinasPorZona(Long ninoId, ZonaHoraria zona, ListView<String> lista) {
        List<Rutina> rutinas = rutinaService.getRutinasByZona(ninoId, zona);
        lista.setItems(FXCollections.observableArrayList(
                rutinas.stream()
                       .map(r -> (r.isCompletada() ? "[OK] " : "[ ] ") + r.getNombre())
                       .toList()
        ));
    }

    @FXML
    private void onCrearRutina() {
        String nombre = txtNombreRutina.getText().trim();
        if (nombre.isBlank()) {
            alerta("Escribe un nombre para la rutina.");
            return;
        }

        int idxNino = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        if (idxNino < 0 || ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            alerta("Selecciona un niño al que asignar la rutina.\n" +
                   "Asegúrate de que tienes niños asignados en el sistema.");
            return;
        }

        Long ninoId = ninosDisponibles.get(idxNino).getId();
        rutinaService.crearRutina(nombre, ZonaHoraria.valueOf(cmbZona.getValue()),
                ninoId, LoginController.usuarioActivo.getId());
        txtNombreRutina.clear();
        cargarTodasLasRutinasTutor();
    }

    // ======================== VISTA NIÑO ========================

    private void cargarRutinasNino() {
        Long ninoId = LoginController.usuarioActivo.getId();
        rutinasActualesNino = rutinaService.todasLasRutinas(ninoId);
        renderizarTarjetasNino();
    }

    private void renderizarTarjetasNino() {
        if (flowRutinasNino == null) return;
        flowRutinasNino.getChildren().clear();

        if (rutinasActualesNino == null || rutinasActualesNino.isEmpty()) {
            Label vacio = new Label("Aún no tienes rutinas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowRutinasNino.getChildren().add(vacio);
            return;
        }

        for (Rutina r : rutinasActualesNino) {
            flowRutinasNino.getChildren().add(crearTarjetaRutinaNino(r));
        }
    }

    private VBox crearTarjetaRutinaNino(Rutina rutina) {
        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(200);
        tarjeta.setPrefHeight(220);
        String colorFondo = rutina.isCompletada() ? "#E8FAF4" : "white";
        tarjeta.setStyle(
            "-fx-background-color:" + colorFondo + ";" +
            "-fx-background-radius:24px;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3);"
        );

        if (rutina.getPictogramaUrl() != null && !rutina.getPictogramaUrl().isBlank()) {
            ImageView img = new ImageView();
            img.setFitWidth(90); img.setFitHeight(90);
            img.setPreserveRatio(true);
            new Thread(() -> {
                try {
                    Image imagen = new Image(rutina.getPictogramaUrl(), 90, 90, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {}
            }).start();
            tarjeta.getChildren().add(img);
        } else {
            Label emoji = new Label(emojiZonaEmoji(rutina.getZonaHoraria()));
            emoji.setStyle("-fx-font-size:48px;");
            tarjeta.getChildren().add(emoji);
        }

        Label lblNombre = new Label(rutina.getNombre());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(180); lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        Label lblZona = new Label(emojiZonaTexto(rutina.getZonaHoraria()));
        lblZona.setStyle("-fx-font-size:12px; -fx-text-fill:#888;");
        tarjeta.getChildren().add(lblZona);

        if (!rutina.isCompletada()) {
            Button btn = new Button("¡Hecho!");
            btn.setStyle(
                "-fx-background-color:#B8EDD9; -fx-text-fill:#4A6F5A;" +
                "-fx-font-size:14px; -fx-font-weight:bold;" +
                "-fx-background-radius:16px; -fx-padding:8px 16px; -fx-cursor:hand;"
            );
            btn.setOnAction(e -> completarRutinaNino(rutina, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            Label lbl = new Label("Completada");
            lbl.setStyle("-fx-text-fill:#81D8A3; -fx-font-weight:bold; -fx-font-size:13px;");
            tarjeta.getChildren().add(lbl);
        }
        return tarjeta;
    }

    private void completarRutinaNino(Rutina rutina, VBox tarjeta) {
        rutinaService.marcarCompletada(rutina.getId());
        rutina.setCompletada(true);
        AnimacionUtil.animarExito(tarjeta);
        if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, 0);
        Platform.runLater(this::renderizarTarjetasNino);
    }

    private String emojiZonaTexto(ZonaHoraria zona) {
        return switch (zona) {
            case MANANA   -> "Mañana";
            case MEDIODIA -> "Mediodía";
            case NOCHE    -> "Noche";
        };
    }
    private String emojiZonaEmoji(ZonaHoraria zona) {
        return switch (zona) {
            case MANANA   -> "M";
            case MEDIODIA -> "D";
            case NOCHE    -> "N";
        };
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}
