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

@Component
public class SolicitudesCanjeController implements Initializable {

    @FXML private ListView<String> listaSolicitudes;
    @FXML private Label lblContador;
    @FXML private Button btnAprobar;
    @FXML private Button btnRechazar;
    @FXML private StackPane rootStack;
    @FXML private ToggleButton btnTodos;
    @FXML private ToggleButton btnPendientes;
    @FXML private ToggleButton btnAprobadas;
    @FXML private ToggleButton btnRechazadas;

    @Autowired private SolicitudCanjeService solicitudService;
    @Autowired private StageManager stageManager;

    private List<SolicitudCanje> todasLasSolicitudes;
    private List<SolicitudCanje> solicitudesActuales;
    private EstadoSolicitud filtroActivo = null; // null = todos

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarSolicitudes();
        solicitudService.marcarTodasLeidas(LoginController.usuarioActivo.getId());
    }

    private void cargarSolicitudes() {
        Long tutorId = LoginController.usuarioActivo.getId();
        todasLasSolicitudes = solicitudService.getSolicitudesTutor(tutorId);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        if (filtroActivo == null) {
            solicitudesActuales = todasLasSolicitudes;
        } else {
            solicitudesActuales = todasLasSolicitudes.stream()
                    .filter(s -> s.getEstado() == filtroActivo)
                    .collect(Collectors.toList());
        }

        listaSolicitudes.setItems(FXCollections.observableArrayList(
                solicitudesActuales.stream().map(s -> {
                    String estadoEmoji = switch (s.getEstado()) {
                        case PENDIENTE  -> "[?]";
                        case APROBADA   -> "[OK]";
                        case RECHAZADA  -> "[X]";
                    };
                    String ninoNombre = s.getNino() != null ? s.getNino().getNombre() : "?";
                    String recompensaDesc = s.getRecompensa() != null ? s.getRecompensa().getDescripcion() : "?";
                    int pts = s.getRecompensa() != null ? s.getRecompensa().getPuntosNecesarios() : 0;
                    String fecha = s.getFecha() != null ? s.getFecha().toLocalDate().toString() : "";
                    return estadoEmoji + "  " + ninoNombre + "  ->  " + recompensaDesc
                            + "  (" + pts + " pts)  |  " + fecha;
                }).toList()
        ));

        long pendientes = todasLasSolicitudes.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.PENDIENTE).count();
        long aprobadas  = todasLasSolicitudes.stream()
                .filter(s -> s.getEstado() == EstadoSolicitud.APROBADA).count();

        lblContador.setText("Total: " + todasLasSolicitudes.size()
                + "  |  Pendientes: " + pendientes
                + "  |  Aprobadas: " + aprobadas);
    }

    // Filtros
    @FXML private void onFiltrarTodos()      { filtroActivo = null;                         aplicarFiltro(); }
    @FXML private void onFiltrarPendientes() { filtroActivo = EstadoSolicitud.PENDIENTE;    aplicarFiltro(); }
    @FXML private void onFiltrarAprobadas()  { filtroActivo = EstadoSolicitud.APROBADA;     aplicarFiltro(); }
    @FXML private void onFiltrarRechazadas() { filtroActivo = EstadoSolicitud.RECHAZADA;    aplicarFiltro(); }

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

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}
