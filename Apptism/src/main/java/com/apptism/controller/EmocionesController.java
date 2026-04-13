// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: EmocionesController.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.TipoMensaje;
import com.apptism.entity.Usuario;
import com.apptism.service.ArasaacService;
import com.apptism.service.ArasaacService.PictogramaDTO;
import com.apptism.service.MensajeService;
import com.apptism.service.UsuarioService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador del módulo de expresión emocional (exclusivo para niños).
 *
 * <p>Permite al niño seleccionar un pictograma de emoción básica y enviarlo
 * a uno de sus tutores. Los pictogramas se obtienen de la API de ARASAAC;
 * si no hay conexión, se muestran emojis Unicode como fallback.
 *
 * <p>Al enviar una emoción se reproduce la animación de éxito de
 * {@link AnimacionUtil} y se muestra un mensaje de confirmación que
 * desaparece automáticamente tras 3 segundos.
 */
@Component
public class EmocionesController implements Initializable {

    @FXML private FlowPane         panelPictogramas;
    @FXML private Label            lblConfirmacion;
    @FXML private ComboBox<String> cmbDestinatario;
    @FXML private StackPane        rootStack;

    @Autowired private ArasaacService  arasaacService;
    @Autowired private MensajeService  mensajeService;
    @Autowired private UsuarioService  usuarioService;
    @Autowired private StageManager    stageManager;

    /** Lista de tutores disponibles como destinatarios. */
    private List<Usuario> tutores;

    /**
     * Inicializa el módulo: oculta la confirmación, carga los tutores
     * del niño y muestra los pictogramas de emociones básicas.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblConfirmacion.setVisible(false);
        cargarTutores();
        cargarEmocionesBasicas();
    }

    /**
     * Carga los tutores vinculados al niño activo en el combo de destinatarios.
     * Si no hay tutores vinculados, muestra todos los del sistema como fallback.
     */
    private void cargarTutores() {
        Usuario nino = LoginController.usuarioActivo;
        tutores = usuarioService.getTutoresDeNino(nino.getId());
        if (tutores.isEmpty()) tutores = usuarioService.getTodosLosTutores();

        tutores.forEach(t -> cmbDestinatario.getItems().add(t.getNombre()));
        if (!cmbDestinatario.getItems().isEmpty()) {
            cmbDestinatario.getSelectionModel().selectFirst();
        } else {
            cmbDestinatario.setPromptText("No hay tutores disponibles");
        }
    }

    /**
     * Carga los pictogramas de emociones básicas desde {@link ArasaacService}
     * y los muestra como tarjetas seleccionables.
     */
    private void cargarEmocionesBasicas() {
        panelPictogramas.getChildren().clear();
        arasaacService.getEmocionesBásicas().forEach(this::crearTarjetaPictograma);
    }

    /**
     * Crea una tarjeta visual interactiva para un pictograma de emoción.
     * La imagen se carga en un hilo secundario. Si no hay URL disponible
     * (fallback), se muestra el emoji del nombre en tamaño grande.
     *
     * @param picto datos del pictograma a representar
     */
    private void crearTarjetaPictograma(PictogramaDTO picto) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(12));
        tarjeta.setPrefWidth(150); tarjeta.setPrefHeight(170);
        tarjeta.setStyle(
                "-fx-background-color:white; -fx-background-radius:20px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),8,0,0,2); -fx-cursor:hand;"
        );

        ImageView img = new ImageView();
        img.setFitWidth(100); img.setFitHeight(100); img.setPreserveRatio(true);

        if (picto.url() != null && !picto.url().isBlank()) {
            new Thread(() -> {
                try {
                    Image imagen = new Image(picto.url(), 100, 100, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception e) {
                    System.err.println("Error cargando imagen: " + picto.url());
                }
            }).start();
        }

        Label lblNombre = new Label(picto.nombre());
        lblNombre.setStyle(
                (picto.url() == null || picto.url().isBlank())
                        ? "-fx-font-size:40px;"
                        : "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;"
        );

        tarjeta.getChildren().addAll(img, lblNombre);
        tarjeta.setOnMouseClicked(e -> enviarEmocion(picto, tarjeta));
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(
                "-fx-background-color:#E8F4FD; -fx-background-radius:20px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3); -fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(
                "-fx-background-color:white; -fx-background-radius:20px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),8,0,0,2); -fx-cursor:hand;"));

        panelPictogramas.getChildren().add(tarjeta);
    }

    /**
     * Envía el pictograma de emoción seleccionado al tutor destinatario,
     * reproduce la animación de éxito y muestra la confirmación durante 3 segundos.
     *
     * @param picto   pictograma de emoción seleccionado
     * @param tarjeta nodo sobre el que se reproduce la animación de éxito
     */
    private void enviarEmocion(PictogramaDTO picto, VBox tarjeta) {
        int idxDestinatario = cmbDestinatario.getSelectionModel().getSelectedIndex();
        if (idxDestinatario < 0 || tutores == null || tutores.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Selecciona a quién enviar la emoción.").showAndWait();
            return;
        }
        Usuario receptor = tutores.get(idxDestinatario);
        mensajeService.enviarMensaje(
                LoginController.usuarioActivo.getId(), receptor.getId(),
                picto.url(), picto.nombre(), TipoMensaje.EMOCION);

        AnimacionUtil.animarExito(tarjeta);
        if (rootStack != null) AnimacionUtil.mostrarPuntos(rootStack, 0);

        lblConfirmacion.setText("Enviado a " + receptor.getNombre() + "!");
        lblConfirmacion.setVisible(true);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> lblConfirmacion.setVisible(false));
        }).start();
    }

    /** Vuelve al dashboard principal. */
    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}