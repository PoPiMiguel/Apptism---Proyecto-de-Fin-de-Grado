package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Recompensa;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.service.RecompensaService;
import com.apptism.service.UsuarioService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class RecompensasController implements Initializable {

    // ---- Vista TUTOR ----

    @FXML private BorderPane panelTutor;
    @FXML private ListView<String> listaRecompensas;
    @FXML private TextField txtDescripcion;
    @FXML private Spinner<Integer> spinnerPuntos;
    @FXML private Label lblPuntosDisponibles;

    // ---- Vista NIÑO ----
    @FXML private StackPane rootStackNino;
    @FXML private FlowPane flowRecompensasNino;
    @FXML private Label lblPuntosNino;

    @Autowired private RecompensaService recompensaService;
    @Autowired private UsuarioService usuarioService;
    @Lazy @Autowired private StageManager stageManager;

    private List<Recompensa> recompensasActualesTutor;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            spinnerPuntos.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10));
            lblPuntosDisponibles.setText("Recompensas que has creado");
            cargarRecompensasTutor();
        } else {
            actualizarPuntosNino();
            cargarRecompensasNino();
        }
    }

    // ======================== TUTOR ========================

    private void cargarRecompensasTutor() {
        recompensasActualesTutor = recompensaService.getRecompensasDisponibles(
                LoginController.usuarioActivo.getId());
        listaRecompensas.setItems(FXCollections.observableArrayList(
                recompensasActualesTutor.stream()
                        .map(r -> r.getDescripcion() + " — " + r.getPuntosNecesarios() + " pts")
                        .toList()
        ));
    }

    @FXML
    private void onCrearRecompensa() {
        String desc = txtDescripcion.getText().trim();
        if (desc.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Escribe una descripción.").showAndWait();
            return;
        }
        recompensaService.crearRecompensa(desc, spinnerPuntos.getValue(),
                LoginController.usuarioActivo.getId());
        txtDescripcion.clear();
        cargarRecompensasTutor();
    }

    // ======================== NIÑO ========================

    private void cargarRecompensasNino() {
        // El niño ve SOLO las recompensas creadas por sus tutores asignados
        // Si no tiene tutores asignados, ve un mensaje vacío
        Usuario nino = LoginController.usuarioActivo;
        List<Usuario> tutoresDelNino = usuarioService.getTutoresDeNino(nino.getId());

        List<Recompensa> recompensasDelNino = new java.util.ArrayList<>();
        for (Usuario tutor : tutoresDelNino) {
            recompensasDelNino.addAll(recompensaService.getRecompensasDisponibles(tutor.getId()));
        }

        int puntosNino = nino.getPuntosAcumulados();
        renderizarTarjetasNino(recompensasDelNino, puntosNino);
    }

    private void renderizarTarjetasNino(List<Recompensa> recompensas, int puntosDisponibles) {
        if (flowRecompensasNino == null) return;
        flowRecompensasNino.getChildren().clear();

        if (recompensas.isEmpty()) {
            Label vacio = new Label("Aún no hay recompensas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowRecompensasNino.getChildren().add(vacio);
            return;
        }

        for (Recompensa r : recompensas) {
            flowRecompensasNino.getChildren().add(crearTarjetaRecompensaNino(r, puntosDisponibles));
        }
    }

    private VBox crearTarjetaRecompensaNino(Recompensa recompensa, int puntosDisponibles) {
        boolean puedeGanar = puntosDisponibles >= recompensa.getPuntosNecesarios();
        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(200);
        tarjeta.setPrefHeight(230);
        tarjeta.setStyle(
                "-fx-background-color:" + (puedeGanar ? "#F2F1FD" : "#F5F5F5") + ";" +
                        "-fx-background-radius:24px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.09),10,0,0,3);" +
                        (puedeGanar ? "" : "-fx-opacity:0.7;")
        );

        Label lblGift = new Label("");
        lblGift.setStyle("-fx-font-size:50px;");
        tarjeta.getChildren().add(lblGift);

        Label lblNombre = new Label(recompensa.getDescripcion());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(180); lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        Label lblPts = new Label(recompensa.getPuntosNecesarios() + " pts");
        lblPts.setStyle("-fx-font-size:14px; -fx-text-fill:" + (puedeGanar ? "#4A6F5A" : "#9BB0A0") + "; -fx-font-weight:bold;");
        tarjeta.getChildren().add(lblPts);

        if (puedeGanar) {
            Button btn = new Button("¡Canjear!");
            btn.getStyleClass().add("btn-nino-verde");
            btn.setOnAction(e -> canjearRecompensaNino(recompensa, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            int faltanPts = recompensa.getPuntosNecesarios() - puntosDisponibles;
            Label lblFalta = new Label("Te faltan " + faltanPts + " pts");
            lblFalta.setStyle("-fx-text-fill:#9BB0A0; -fx-font-size:12px;");
            tarjeta.getChildren().add(lblFalta);
        }
        return tarjeta;
    }

    private void canjearRecompensaNino(Recompensa recompensa, VBox tarjeta) {
        boolean exito = recompensaService.canjearRecompensa(
                LoginController.usuarioActivo.getId(), recompensa.getId());
        if (exito) {
            int nuevos = LoginController.usuarioActivo.getPuntosAcumulados() - recompensa.getPuntosNecesarios();
            LoginController.usuarioActivo.setPuntosAcumulados(nuevos);
            AnimacionUtil.animarExito(tarjeta);
            if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, 0);
            actualizarPuntosNino();
            cargarRecompensasNino();
        } else {
            new Alert(Alert.AlertType.WARNING, "No tienes suficientes puntos.").showAndWait();
        }
    }

    private void actualizarPuntosNino() {
        if (lblPuntosNino != null)
            lblPuntosNino.setText(LoginController.usuarioActivo.getPuntosAcumulados() + " puntos");
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}