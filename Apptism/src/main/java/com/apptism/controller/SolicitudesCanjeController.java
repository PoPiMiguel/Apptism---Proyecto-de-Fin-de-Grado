package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.SolicitudCanje;
import com.apptism.service.SolicitudCanjeService;
import com.apptism.ui.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla del historial de canjes (exclusivo para tutores).
 *
 * Muestra todas las recompensas que han canjeado los niños del tutor,
 * con el nombre del niño, la descripción de la recompensa y la fecha.
 */
@Component
public class SolicitudesCanjeController implements Initializable {

    @FXML private ListView<String> listaSolicitudes;
    @FXML private Label            lblContador;

    @Autowired private SolicitudCanjeService solicitudService;
    @Autowired private StageManager          stageManager;

    private List<SolicitudCanje> todasLasSolicitudes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarSolicitudes();
        solicitudService.marcarTodasLeidas(LoginController.usuarioActivo.getId());
    }

    private void cargarSolicitudes() {
        Long tutorId = LoginController.usuarioActivo.getId();
        todasLasSolicitudes = solicitudService.getSolicitudesTutor(tutorId);

        listaSolicitudes.setItems(FXCollections.observableArrayList(
                todasLasSolicitudes.stream().map(s -> {
                    String ninoNombre     = s.getNino()       != null ? s.getNino().getNombre()                : "?";
                    String recompensaDesc = s.getRecompensa() != null ? s.getRecompensa().getDescripcion()     : "?";
                    int    pts            = s.getRecompensa() != null ? s.getRecompensa().getPuntosNecesarios() : 0;
                    String fecha          = s.getFecha()      != null ? s.getFecha().toLocalDate().toString()  : "";
                    return "🎁 " + ninoNombre + " -> " + recompensaDesc
                            + " (" + pts + " pts) | " + fecha;
                }).toList()
        ));

        lblContador.setText("Total de canjes realizados: " + todasLasSolicitudes.size());
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}