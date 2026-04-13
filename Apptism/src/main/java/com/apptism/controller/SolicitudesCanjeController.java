// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: SolicitudesCanjeController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.EstadoSolicitud;
import com.apptism.entity.SolicitudCanje;
import com.apptism.service.SolicitudCanjeService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


/**
 * Controlador del módulo de gestión de solicitudes de canje (exclusivo para tutores).
 *
 * <p>Muestra todas las solicitudes de canje generadas por los niños del tutor
 * autenticado. Permite filtrar por estado (todas, pendientes, aprobadas o rechazadas)
 * y aprobar o rechazar solicitudes en estado {@code PENDIENTE}.
 *
 * <p>Al inicializarse, marca todas las solicitudes como leídas para limpiar
 * el badge de notificación del dashboard.
 */
@Component
public class SolicitudesCanjeController implements Initializable {

    @FXML private ListView<String> listaSolicitudes;
    @FXML private Label            lblContador;
    @FXML private Button           btnAprobar;
    @FXML private Button           btnRechazar;
    @FXML private StackPane        rootStack;

    @Autowired private SolicitudCanjeService solicitudService;
    @Autowired private StageManager          stageManager;

    /** Lista completa de solicitudes del tutor (sin filtro). */
    private List<SolicitudCanje> todasLasSolicitudes;

    /** Lista actualmente visible según el filtro activo. */
    private List<SolicitudCanje> solicitudesActuales;

    /** Filtro de estado activo; {@code null} significa "todas". */
    private EstadoSolicitud filtroActivo = null;

    /**
     * Carga las solicitudes y marca todas como leídas al entrar en el módulo.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarSolicitudes();
        solicitudService.marcarTodasLeidas(LoginController.usuarioActivo.getId());
    }

    /**
     * Obtiene todas las solicitudes del tutor desde el servicio
     * y aplica el filtro activo para actualizar la lista visible.
     */
    private void cargarSolicitudes() {
        todasLasSolicitudes = solicitudService
                .getSolicitudesTutor(LoginController.usuarioActivo.getId());
        aplicarFiltro();
    }

    /**
     * Filtra {@link #todasLasSolicitudes} por {@link #filtroActivo} y
     * actualiza la lista de la UI y el contador de resumen.
     */
    private void aplicarFiltro() {
        solicitudesActuales = (filtroActivo == null)
                ? todasLasSolicitudes
                : todasLasSolicitudes.stream()
                .filter(s -> s.getEstado() == filtroActivo)
                .collect(Collectors.toList());

        listaSolicitudes.setItems(FXCollections.observableArrayList(
                solicitudesActuales.stream().map(s -> {
                    String emoji = switch (s.getEstado()) {
                        case PENDIENTE -> "[?]";
                        case APROBADA  -> "[OK]";
                        case RECHAZADA -> "[X]";
                    };
                    String ninoNombre     = s.getNino()       != null ? s.getNino().getNombre()               : "?";
                    String recompensaDesc = s.getRecompensa() != null ? s.getRecompensa().getDescripcion()    : "?";
                    int    pts            = s.getRecompensa() != null ? s.getRecompensa().getPuntosNecesarios(): 0;
                    String fecha          = s.getFecha()      != null ? s.getFecha().toLocalDate().toString() : "";
                    return emoji + "  " + ninoNombre + "  ->  " + recompensaDesc
                            + "  (" + pts + " pts)  |  " + fecha;
                }).toList()
        ));

        long pendientes = todasLasSolicitudes.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE).count();
        long aprobadas  = todasLasSolicitudes.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.APROBADA).count();
        lblContador.setText("Total: " + todasLasSolicitudes.size()
                + "  |  Pendientes: " + pendientes + "  |  Aprobadas: " + aprobadas);
    }

    // Filtros de la lista
    @FXML private void onFiltrarTodos()      { filtroActivo = null;                      aplicarFiltro(); }
    @FXML private void onFiltrarPendientes() { filtroActivo = EstadoSolicitud.PENDIENTE; aplicarFiltro(); }
    @FXML private void onFiltrarAprobadas()  { filtroActivo = EstadoSolicitud.APROBADA;  aplicarFiltro(); }
    @FXML private void onFiltrarRechazadas() { filtroActivo = EstadoSolicitud.RECHAZADA; aplicarFiltro(); }

    /**
     * Aprueba la solicitud seleccionada en la lista. Solo actúa sobre
     * solicitudes en estado {@code PENDIENTE}.
     */
    @FXML
    private void onAprobar() {
        int idx = listaSolicitudes.getSelectionModel().getSelectedIndex();
        if (idx < 0) { alerta("Selecciona un canje de la lista."); return; }
        SolicitudCanje s = solicitudesActuales.get(idx);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            alerta("Solo puedes aprobar canjes en estado PENDIENTE.");
            return;
        }
        solicitudService.aprobar(s.getId());
        AnimacionUtil.animarExito(btnAprobar);
        if (rootStack != null) AnimacionUtil.mostrarPuntos(rootStack, 0);
        new Alert(Alert.AlertType.INFORMATION,
                "Canje aprobado para " + s.getNino().getNombre()
                        + "\n🎁 " + s.getRecompensa().getDescripcion()).showAndWait();
        cargarSolicitudes();
    }

    /**
     * Rechaza la solicitud seleccionada en la lista. Solo actúa sobre
     * solicitudes en estado {@code PENDIENTE}.
     */
    @FXML
    private void onRechazar() {
        int idx = listaSolicitudes.getSelectionModel().getSelectedIndex();
        if (idx < 0) { alerta("Selecciona un canje de la lista."); return; }
        SolicitudCanje s = solicitudesActuales.get(idx);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            alerta("Solo puedes rechazar canjes en estado PENDIENTE.");
            return;
        }
        solicitudService.rechazar(s.getId());
        cargarSolicitudes();
    }

    private void alerta(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }

    /** Vuelve al dashboard principal. */
    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}