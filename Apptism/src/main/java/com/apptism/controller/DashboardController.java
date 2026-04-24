// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: DashboardController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.service.SolicitudCanjeService;
import com.apptism.ui.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de inicio (dashboard).
 *
 * Muestra una interfaz distinta según el rol del usuario:
 * - Tutor: acceso a rutinas, tareas, recompensas, chat, registro emocional
 *   y canjes, con un badge que muestra las solicitudes pendientes.
 * - Niño: acceso simplificado a sus módulos junto con sus puntos acumulados.
 */
@Component
public class DashboardController implements Initializable {

    @FXML private BorderPane panelTutor;
    @FXML private Label      lblBienvenida;
    @FXML private Label      lblBadgeSolicitudes;

    @FXML private BorderPane panelNino;
    @FXML private Label      lblBienvenidaNino;
    @FXML private Label      lblPuntosNino;

    @Autowired private SolicitudCanjeService solicitudCanjeService;
    @Autowired private StageManager stageManager;

    /**
     * Prepara la pantalla según el rol del usuario. Muestra el panel
     * de tutor o el de niño y personaliza el saludo y los datos.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Usuario usuario = LoginController.usuarioActivo;
        if (usuario == null) return;

        boolean esTutor = usuario.getRol() == RolUsuario.PADRE
                || usuario.getRol() == RolUsuario.PROFESOR;

        if (esTutor) {
            panelTutor.setVisible(true);
            panelNino.setVisible(false);
            lblBienvenida.setText("¡Hola, " + usuario.getNombre() + "!");

            long pendientes = solicitudCanjeService.contarNoLeidas(usuario.getId());
            lblBadgeSolicitudes.setText(String.valueOf(pendientes));
            lblBadgeSolicitudes.setVisible(pendientes > 0);
        } else {
            panelTutor.setVisible(false);
            panelNino.setVisible(true);
            lblBienvenidaNino.setText("¡Hola, " + usuario.getNombre() + "!");
            lblPuntosNino.setText(usuario.getPuntosAcumulados() + " puntos");
        }
    }


    /** Abre la pantalla de rutinas. */
    @FXML private void onIrRutinas()      { stageManager.switchScene(FxmlView.RUTINAS); }

    /** Abre la pantalla de tareas. */
    @FXML private void onIrTareas()       { stageManager.switchScene(FxmlView.TAREAS); }

    /** Abre la pantalla de chat. */
    @FXML private void onIrChat()         { stageManager.switchScene(FxmlView.CHAT); }

    /** Abre la pantalla de recompensas. */
    @FXML private void onIrRecompensas()  { stageManager.switchScene(FxmlView.RECOMPENSAS); }

    /** Abre el registro emocional (solo tutores). */
    @FXML private void onIrRegistroEmocional() {
        stageManager.switchScene(FxmlView.REGISTRO_EMOCIONAL);
    }

    /** Abre el historial de canjes (solo tutores). */
    @FXML private void onIrSolicitudes() {
        stageManager.switchScene(FxmlView.SOLICITUDES_CANJE);
    }

    /** Abre la pantalla de emociones (solo niños). */
    @FXML private void onIrEmociones() { stageManager.switchScene(FxmlView.EMOCIONES); }

    /**
     * Cierra la sesión del usuario, limpia el usuario activo
     * y vuelve a la pantalla de login.
     */
    @FXML
    private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}