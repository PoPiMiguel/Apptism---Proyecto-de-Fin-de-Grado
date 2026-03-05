package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.repository.SolicitudCanjeRepository;
import com.apptism.ui.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class DashboardController implements Initializable {

    // ---- Panel TUTOR ----
    @FXML private BorderPane panelTutor;
    @FXML private Label lblBienvenida;
    @FXML private HBox   hboxTutor;
    @FXML private Label  lblBadgeSolicitudes;

    // ---- Panel NIÑO ----
    @FXML private BorderPane panelNino;
    @FXML private Label lblBienvenidaNino;
    @FXML private Label lblPuntosNino;

    @Autowired private SolicitudCanjeRepository solicitudRepo;
    @Autowired private StageManager stageManager;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Usuario usuario = LoginController.usuarioActivo;
        if (usuario == null) return;

        boolean esTutor = usuario.getRol() == RolUsuario.PADRE || usuario.getRol() == RolUsuario.PROFESOR;

        if (esTutor) {
            panelTutor.setVisible(true);
            panelNino.setVisible(false);

            lblBienvenida.setText("¡Hola, " + usuario.getNombre() + "! 👋");

            // Badge solicitudes pendientes
            long pendientes = solicitudRepo.countByRecompensaFamiliaIdAndLeidaFalse(usuario.getId());
            if (pendientes > 0) {
                lblBadgeSolicitudes.setText(String.valueOf(pendientes));
                lblBadgeSolicitudes.setVisible(true);
            } else {
                lblBadgeSolicitudes.setVisible(false);
            }
        } else {
            panelTutor.setVisible(false);
            panelNino.setVisible(true);

            lblBienvenidaNino.setText("¡Hola, " + usuario.getNombre() + "! 👋");
            lblPuntosNino.setText("🏆 " + usuario.getPuntosAcumulados() + " puntos");
        }
    }

    // Navegación — común
    @FXML private void onIrRutinas()   { stageManager.switchScene(FxmlView.RUTINAS); }
    @FXML private void onIrTareas()    { stageManager.switchScene(FxmlView.TAREAS); }
    @FXML private void onIrChat()      { stageManager.switchScene(FxmlView.CHAT); }
    @FXML private void onIrRecompensas() { stageManager.switchScene(FxmlView.RECOMPENSAS); }

    // Solo tutores
    @FXML private void onIrRegistroEmocional() { stageManager.switchScene(FxmlView.REGISTRO_EMOCIONAL); }
    @FXML private void onIrSolicitudes()       { stageManager.switchScene(FxmlView.SOLICITUDES_CANJE); }

    // Solo niños
    @FXML private void onIrEmociones() { stageManager.switchScene(FxmlView.EMOCIONES); }

    @FXML
    private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}
