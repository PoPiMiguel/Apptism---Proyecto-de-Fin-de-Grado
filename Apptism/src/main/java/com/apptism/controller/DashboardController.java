// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: DashboardController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.repository.SolicitudCanjeRepository;
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
 * Controlador del panel de inicio (dashboard) de la aplicación.
 *
 * <p>Muestra una interfaz adaptada al rol del usuario autenticado:
 * <ul>
 *   <li><b>Tutor</b>: acceso a rutinas, tareas, recompensas, chat,
 *       registro emocional y solicitudes de canje. Incluye un badge
 *       con el número de solicitudes pendientes.</li>
 *   <li><b>Niño</b>: acceso simplificado a rutinas, tareas,
 *       recompensas, chat y módulo de emociones, junto con su
 *       saldo de puntos acumulados.</li>
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

    @Autowired private SolicitudCanjeRepository solicitudRepo;
    @Autowired private StageManager stageManager;

    /**
     * Configura la vista según el rol del usuario autenticado.
     * Muestra el panel de tutor o de niño y personaliza el saludo
     * y los datos mostrados.
     *
     * @param url URL del FXML (no se usa)
     * @param rb  ResourceBundle de internacionalización (no se usa)
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

            // Badge de notificación: solicitudes de canje pendientes
            long pendientes = solicitudRepo
                    .countByRecompensaFamiliaIdAndLeidaFalse(usuario.getId());
            lblBadgeSolicitudes.setText(String.valueOf(pendientes));
            lblBadgeSolicitudes.setVisible(pendientes > 0);
        } else {
            panelTutor.setVisible(false);
            panelNino.setVisible(true);
            lblBienvenidaNino.setText("¡Hola, " + usuario.getNombre() + "!");
            lblPuntosNino.setText(usuario.getPuntosAcumulados() + " puntos");
        }
    }

    // ── Navegación común ──────────────────────────────────────────

    /** Navega al módulo de gestión de rutinas. */
    @FXML private void onIrRutinas()      { stageManager.switchScene(FxmlView.RUTINAS); }

    /** Navega al módulo de gestión de tareas. */
    @FXML private void onIrTareas()       { stageManager.switchScene(FxmlView.TAREAS); }

    /** Navega al módulo de chat con pictogramas. */
    @FXML private void onIrChat()         { stageManager.switchScene(FxmlView.CHAT); }

    /** Navega al módulo de recompensas. */
    @FXML private void onIrRecompensas()  { stageManager.switchScene(FxmlView.RECOMPENSAS); }

    // ── Navegación exclusiva para tutores ─────────────────────────

    /** Navega al módulo de registro emocional (solo tutores). */
    @FXML private void onIrRegistroEmocional() {
        stageManager.switchScene(FxmlView.REGISTRO_EMOCIONAL);
    }

    /** Navega al módulo de solicitudes de canje (solo tutores). */
    @FXML private void onIrSolicitudes() {
        stageManager.switchScene(FxmlView.SOLICITUDES_CANJE);
    }

    // ── Navegación exclusiva para niños ───────────────────────────

    /** Navega al módulo de expresión emocional con pictogramas (solo niños). */
    @FXML private void onIrEmociones() { stageManager.switchScene(FxmlView.EMOCIONES); }

    /**
     * Cierra la sesión del usuario actual, limpia {@link LoginController#usuarioActivo}
     * y vuelve a la pantalla de login.
     */
    @FXML
    private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}