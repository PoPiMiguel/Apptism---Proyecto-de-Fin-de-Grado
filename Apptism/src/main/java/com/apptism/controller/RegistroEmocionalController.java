// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: RegistroEmocionalController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Mensaje;
import com.apptism.entity.Usuario;
import com.apptism.service.MensajeService;
import com.apptism.service.UsuarioService;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ResourceBundle;

/**
 * Controlador del módulo de registro emocional (exclusivo para tutores).
 *
 * <p>Muestra los pictogramas de emoción enviados por los niños del tutor,
 * y una representación gráfica de barras con la frecuencia de cada emoción
 * por día de la semana durante los últimos 7 días.
 *
 * <p>El tutor puede seleccionar a qué niño ver mediante un combo,
 * actualizándose tanto el panel de pictogramas como el gráfico.
 */
@Component
public class RegistroEmocionalController implements Initializable {

    @FXML private ComboBox<String>          cmbNino;
    @FXML private FlowPane                  panelMensajes;
    @FXML private BarChart<String, Number>  graficoEmociones;
    @FXML private CategoryAxis              ejeX;
    @FXML private NumberAxis                ejeY;
    @FXML private Label                     lblSinMensajes;

    @Autowired private MensajeService  mensajeService;
    @Autowired private UsuarioService  usuarioService;
    @Autowired private StageManager    stageManager;

    private List<Usuario> ninos;

    /**
     * Configura el gráfico y carga los niños vinculados al tutor.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarGrafico();
        cargarNinos();
    }

    /**
     * Carga en el combo los niños vinculados al tutor. Si no hay ninguno,
     * muestra un mensaje informativo. Configura el listener para recargar
     * los datos al cambiar la selección.
     */
    private void cargarNinos() {
        ninos = usuarioService.getNinosDetutor(LoginController.usuarioActivo.getId());
        if (ninos.isEmpty()) {
            cmbNino.setPromptText("No tienes niños asignados");
            if (lblSinMensajes != null) {
                lblSinMensajes.setText("No tienes niños asignados aún.");
                lblSinMensajes.setVisible(true);
            }
            return;
        }
        ninos.forEach(n -> cmbNino.getItems().add(n.getNombre()));
        cmbNino.getSelectionModel().selectedIndexProperty().addListener(
                (obs, old, idx) -> {
                    if (idx.intValue() >= 0) cargarEmociones(ninos.get(idx.intValue()));
                });
        cmbNino.getSelectionModel().selectFirst();
    }

    /**
     * Establece las etiquetas de los ejes y el título del gráfico de barras.
     */
    private void configurarGrafico() {
        if (ejeX != null) ejeX.setLabel("Día de la semana");
        if (ejeY != null) ejeY.setLabel("Nº de emociones");
        if (graficoEmociones != null) {
            graficoEmociones.setTitle("Registro emocional — última semana");
            graficoEmociones.setAnimated(true);
        }
    }

    /**
     * Carga y muestra los pictogramas emocionales enviados por el niño
     * seleccionado, filtrando los mensajes del tutor activo por emisor.
     * Actualiza también el gráfico semanal.
     *
     * @param nino niño cuyo historial emocional se desea visualizar
     */
    private void cargarEmociones(Usuario nino) {
        if (panelMensajes != null) panelMensajes.getChildren().clear();
        if (graficoEmociones != null) graficoEmociones.getData().clear();

        List<Mensaje> mensajes = mensajeService
                .getEmocionesRecibidas(LoginController.usuarioActivo.getId())
                .stream()
                .filter(m -> m.getEmisor().getId().equals(nino.getId()))
                .toList();

        if (lblSinMensajes != null) lblSinMensajes.setVisible(mensajes.isEmpty());

        mensajes.forEach(m -> {
            VBox tarjeta = new VBox(6);
            tarjeta.setAlignment(Pos.CENTER);
            tarjeta.setPadding(new Insets(10));
            tarjeta.setPrefWidth(120);
            tarjeta.setStyle(
                    "-fx-background-color:#FFF9F0; -fx-background-radius:16px;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
            );

            ImageView img = new ImageView();
            img.setFitWidth(80); img.setFitHeight(80); img.setPreserveRatio(true);
            if (m.getPictogramaUrl() != null && !m.getPictogramaUrl().isBlank()) {
                new Thread(() -> {
                    try {
                        Image imagen = new Image(m.getPictogramaUrl(), 80, 80, true, true, true);
                        Platform.runLater(() -> img.setImage(imagen));
                    } catch (Exception ignored) {}
                }).start();
            }

            Label lblTexto = new Label(m.getTextoPictograma());
            lblTexto.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

            Label lblFecha = new Label(m.getFecha() != null
                    ? m.getFecha().toLocalDate().toString() : "");
            lblFecha.setStyle("-fx-font-size:11px; -fx-text-fill:#999;");

            tarjeta.getChildren().addAll(img, lblTexto, lblFecha);
            if (panelMensajes != null) panelMensajes.getChildren().add(tarjeta);
        });

        actualizarGrafico(mensajes);
    }

    /**
     * Genera las series del gráfico de barras agrupando los mensajes
     * por emoción (nombre del pictograma) y por día de la semana,
     * considerando únicamente los mensajes de los últimos 7 días.
     *
     * @param mensajes lista de mensajes emocionales a representar
     */
    private void actualizarGrafico(List<Mensaje> mensajes) {
        if (graficoEmociones == null) return;
        String[] dias = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        LocalDateTime haceUnaSemana = LocalDateTime.now().minusDays(7);

        Map<String, Map<String, Long>> datos = mensajes.stream()
                .filter(m -> m.getFecha() != null && m.getFecha().isAfter(haceUnaSemana))
                .collect(Collectors.groupingBy(
                        m -> m.getTextoPictograma(),
                        Collectors.groupingBy(
                                m -> dias[m.getFecha().getDayOfWeek().getValue() - 1],
                                Collectors.counting()
                        )
                ));

        datos.forEach((emocion, porDia) -> {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(emocion);
            for (String dia : dias) {
                serie.getData().add(new XYChart.Data<>(dia, porDia.getOrDefault(dia, 0L)));
            }
            graficoEmociones.getData().add(serie);
        });
    }

    /** Vuelve al dashboard principal. */
    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}