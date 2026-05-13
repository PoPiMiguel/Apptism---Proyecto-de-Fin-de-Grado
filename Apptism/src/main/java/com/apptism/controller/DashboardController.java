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
 * <p>Muestra una interfaz diferente según el rol del usuario:</p>
 * <ul>
 *   <li><b>Tutor</b>: acceso a rutinas, tareas, recompensas, chat, registro
 *       emocional y canjes, con un badge que indica las solicitudes pendientes.</li>
 *   <li><b>Niño</b>: acceso simplificado a sus módulos y sus puntos acumulados
 *       representados visualmente como estrellas, coronas y diamantes.</li>
 * </ul>
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
     * Inicializa el dashboard mostrando el panel correspondiente al rol del usuario activo.
     * Si no hay usuario en sesión, no hace nada.
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
            lblPuntosNino.setText(TareasController.puntosAEstrellas(usuario.getPuntosAcumulados()));
        }
    }

    /** Navega a la pantalla de rutinas. */

    @FXML private void onIrRutinas()      { stageManager.switchScene(FxmlView.RUTINAS); }

    /** Navega a la pantalla de tareas. */

    @FXML private void onIrTareas()       { stageManager.switchScene(FxmlView.TAREAS); }

    /** Navega a la pantalla de chat. */

    @FXML private void onIrChat()         { stageManager.switchScene(FxmlView.CHAT); }

    /** Navega a la pantalla de recompensas. */

    @FXML private void onIrRecompensas()  { stageManager.switchScene(FxmlView.RECOMPENSAS); }

    /** Navega a la pantalla de emociones (niño). */

    @FXML private void onIrEmociones()    { stageManager.switchScene(FxmlView.EMOCIONES); }

    /** Navega a la pantalla de registro emocional (tutor). */

    @FXML private void onIrRegistroEmocional() { stageManager.switchScene(FxmlView.REGISTRO_EMOCIONAL); }

    /** Navega a la pantalla de solicitudes de canje. */

    @FXML private void onIrSolicitudes()  { stageManager.switchScene(FxmlView.SOLICITUDES_CANJE); }

    /**
     * Cierra la sesión del usuario activo y vuelve a la pantalla de login.
     */

    @FXML private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}