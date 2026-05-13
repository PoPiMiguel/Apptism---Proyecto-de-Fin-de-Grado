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
 * Controlador de la pantalla de emociones (exclusivo para niños).
 *
 * <p>Muestra los pictogramas de las emociones básicas de ARASAAC para que el
 * niño pueda indicar cómo se siente y enviárselo a su tutor. El tutor lo
 * verá después en el registro emocional. Solo se muestran los tutores
 * vinculados al niño mediante la tabla {@code tutores_ninos}.</p>
 */

@Component
public class EmocionesController implements Initializable {

    @FXML private FlowPane panelPictogramas;
    @FXML private Label lblConfirmacion;
    @FXML private ComboBox<String> cmbDestinatario;
    @FXML private StackPane rootStack;

    @Autowired private ArasaacService arasaacService;
    @Autowired private MensajeService mensajeService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private StageManager stageManager;

    /** Tutores vinculados al niño activo, disponibles como destinatarios. */

    private List<Usuario> tutores;

    /**
     * Inicializa la pantalla: oculta la confirmación, carga los tutores
     * del niño y los pictogramas de emociones básicas.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        lblConfirmacion.setVisible(false);
        cargarTutores();
        cargarEmocionesBasicas();
    }

    /**
     * Carga los tutores vinculados al niño activo en el combo de destinatarios.
     * Si no tiene ninguno, muestra un texto informativo en el combo.
     */

    private void cargarTutores() {
        Usuario nino = LoginController.usuarioActivo;
        tutores = usuarioService.getTutoresDeNino(nino.getId());

        tutores.forEach(t -> cmbDestinatario.getItems().add(t.getNombre()));
        if (!cmbDestinatario.getItems().isEmpty()) {
            cmbDestinatario.getSelectionModel().selectFirst();
        } else {
            cmbDestinatario.setPromptText("No hay tutores disponibles");
        }
    }

    /**
     * Solicita a ARASAAC los pictogramas de las emociones básicas y los muestra en el panel.
     */

    private void cargarEmocionesBasicas() {
        panelPictogramas.getChildren().clear();
        List<PictogramaDTO> emociones = arasaacService.getEmocionesBásicas();
        emociones.forEach(this::crearTarjetaPictograma);
    }

    /**
     * Crea la tarjeta visual de un pictograma de emoción y la añade al panel.
     * La imagen se carga en un hilo secundario. Al hacer clic se envía la emoción.
     *
     * @param picto datos del pictograma a mostrar
     */

    private void crearTarjetaPictograma(PictogramaDTO picto) {
        VBox tarjeta = new VBox(8);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(12));
        tarjeta.setPrefWidth(150);
        tarjeta.setPrefHeight(170);
        tarjeta.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);" +
                        "-fx-cursor: hand;"
        );

        ImageView img = new ImageView();
        img.setFitWidth(100); img.setFitHeight(100);
        img.setPreserveRatio(true);

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
        if (picto.url() == null || picto.url().isBlank()) {
            lblNombre.setStyle("-fx-font-size:40px;");
        } else {
            lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        }

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
     * Envía el pictograma de emoción al tutor seleccionado y muestra una
     * confirmación visual durante 3 segundos.
     *
     * @param picto   datos del pictograma de emoción seleccionado
     * @param tarjeta tarjeta visual sobre la que se ha hecho clic
     */

    private void enviarEmocion(PictogramaDTO picto, VBox tarjeta) {
        int idxDestinatario = cmbDestinatario.getSelectionModel().getSelectedIndex();
        if (idxDestinatario < 0 || tutores == null || tutores.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Selecciona a quién enviar la emoción.").showAndWait();
            return;
        }
        Usuario receptor = tutores.get(idxDestinatario);
        mensajeService.enviarMensaje(
                LoginController.usuarioActivo.getId(),
                receptor.getId(),
                picto.url(), picto.nombre(),
                TipoMensaje.EMOCION
        );

        if (rootStack != null) AnimacionUtil.mostrarPuntos(rootStack, 0);

        lblConfirmacion.setText("Enviado a " + receptor.getNombre() + "!");
        lblConfirmacion.setVisible(true);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> lblConfirmacion.setVisible(false));
        }).start();
    }

    /** Vuelve al dashboard. */

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}