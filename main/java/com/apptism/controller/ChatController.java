package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Mensaje;
import com.apptism.entity.TipoMensaje;
import com.apptism.entity.Usuario;
import com.apptism.service.ArasaacService;
import com.apptism.service.ArasaacService.PictogramaDTO;
import com.apptism.service.MensajeService;
import com.apptism.service.UsuarioService;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class ChatController implements Initializable {

    @FXML private VBox panelMensajes;
    @FXML private ScrollPane scrollMensajes;
    @FXML private FlowPane panelPictos;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbInterlocutor;

    @Autowired private MensajeService mensajeService;
    @Autowired private ArasaacService arasaacService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private StageManager stageManager;

    private List<Usuario> contactos;
    private Usuario interlocutorActual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarContactos();
        // Mostrar pictogramas por defecto SIN necesidad de buscar
        cargarPictogramasPorDefecto();
        panelMensajes.heightProperty().addListener(
                (obs, old, val) -> scrollMensajes.setVvalue(1.0));
    }

    private void cargarContactos() {
        Usuario yo = LoginController.usuarioActivo;
        contactos = usuarioService.getContactos(yo);
        contactos.forEach(c -> cmbInterlocutor.getItems().add(c.getNombre()));
        cmbInterlocutor.getSelectionModel().selectedIndexProperty().addListener(
                (obs, old, idx) -> {
                    if (idx.intValue() >= 0) {
                        interlocutorActual = contactos.get(idx.intValue());
                        cargarConversacion();
                    }
                });
        if (!contactos.isEmpty()) cmbInterlocutor.getSelectionModel().selectFirst();
    }

    private void cargarConversacion() {
        panelMensajes.getChildren().clear();
        if (interlocutorActual == null) return;
        List<Mensaje> msgs = mensajeService.getConversacion(
                LoginController.usuarioActivo.getId(), interlocutorActual.getId());
        msgs.forEach(this::agregarBurbujaMensaje);
    }

    private void agregarBurbujaMensaje(Mensaje m) {
        boolean esMio = m.getEmisor().getId().equals(LoginController.usuarioActivo.getId());

        HBox fila = new HBox();
        fila.setPadding(new Insets(5, 15, 5, 15));
        fila.setAlignment(esMio ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox burbuja = new VBox(5);
        burbuja.setPadding(new Insets(10));
        burbuja.setMaxWidth(180);
        burbuja.setStyle(
            "-fx-background-radius:16px;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),5,0,0,2);" +
            (esMio ? "-fx-background-color:#C8EEC8;" : "-fx-background-color:white;")
        );

        ImageView img = new ImageView();
        img.setFitWidth(80); img.setFitHeight(80);
        img.setPreserveRatio(true);
        if (m.getPictogramaUrl() != null && !m.getPictogramaUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image imagen = new Image(m.getPictogramaUrl(), 80, 80, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {}
            }).start();
        }

        Label lblTexto = new Label(m.getTextoPictograma());
        lblTexto.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#333;");

        burbuja.getChildren().addAll(img, lblTexto);
        fila.getChildren().add(burbuja);
        panelMensajes.getChildren().add(fila);
    }

    /**
     * Carga emociones básicas + categorías temáticas al inicio,
     * sin necesidad de buscar. Luego el usuario puede buscar más.
     */
    private void cargarPictogramasPorDefecto() {
        panelPictos.getChildren().clear();
        // 1. Mostrar emociones básicas inmediatamente (son locales, no requieren red)
        arasaacService.getEmocionesBásicas().forEach(this::agregarPictoSeleccionable);

        // 2. Cargar en background categorías adicionales frecuentes
        new Thread(() -> {
            List<PictogramaDTO>[] categorias = new List[]{
                arasaacService.buscar("casa"),
                arasaacService.buscar("comer"),
                arasaacService.buscar("jugar")
            };
            for (List<PictogramaDTO> lista : categorias) {
                Platform.runLater(() -> lista.forEach(p -> {
                    // No duplicar los que ya están
                    boolean duplicado = panelPictos.getChildren().stream()
                        .anyMatch(node -> node.getUserData() != null &&
                                  node.getUserData().equals(p.id()));
                    if (!duplicado) agregarPictoSeleccionable(p);
                }));
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    @FXML
    private void onBuscar() {
        String palabra = txtBuscar.getText().trim();
        if (palabra.isBlank()) {
            // Si vacío, recargar los por defecto
            cargarPictogramasPorDefecto();
            return;
        }
        panelPictos.getChildren().clear();
        // Mostrar emociones básicas mientras carga
        arasaacService.getEmocionesBásicas().forEach(this::agregarPictoSeleccionable);
        new Thread(() -> {
            List<PictogramaDTO> resultados = arasaacService.buscar(palabra);
            Platform.runLater(() -> {
                panelPictos.getChildren().clear();
                resultados.forEach(this::agregarPictoSeleccionable);
                if (resultados.isEmpty()) {
                    Label vacio = new Label("Sin resultados para \"" + palabra + "\"");
                    vacio.setStyle("-fx-text-fill:#999; -fx-font-size:13px;");
                    panelPictos.getChildren().add(vacio);
                }
            });
        }).start();
    }

    private void agregarPictoSeleccionable(PictogramaDTO picto) {
        VBox tarjeta = new VBox(4);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(6));
        tarjeta.setPrefWidth(100);
        tarjeta.setUserData(picto.id()); // Para evitar duplicados
        tarjeta.setStyle(
            "-fx-background-color:#F8F9FA; -fx-background-radius:12px; -fx-cursor:hand;");

        ImageView img = new ImageView();
        img.setFitWidth(70); img.setFitHeight(70);
        img.setPreserveRatio(true);
        new Thread(() -> {
            try {
                Image imagen = new Image(picto.url(), 70, 70, true, true, true);
                Platform.runLater(() -> img.setImage(imagen));
            } catch (Exception ignored) {}
        }).start();

        Label lbl = new Label(picto.nombre());
        lbl.setStyle("-fx-font-size:11px;");
        lbl.setWrapText(true); lbl.setMaxWidth(90);

        tarjeta.getChildren().addAll(img, lbl);
        tarjeta.setOnMouseClicked(e -> enviarPicto(picto));
        tarjeta.setOnMouseEntered(e ->
            tarjeta.setStyle("-fx-background-color:#E3F2FD; -fx-background-radius:12px; -fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e ->
            tarjeta.setStyle("-fx-background-color:#F8F9FA; -fx-background-radius:12px; -fx-cursor:hand;"));

        panelPictos.getChildren().add(tarjeta);
    }

    private void enviarPicto(PictogramaDTO picto) {
        if (interlocutorActual == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un contacto primero.").showAndWait();
            return;
        }
        Mensaje m = mensajeService.enviarMensaje(
                LoginController.usuarioActivo.getId(),
                interlocutorActual.getId(),
                picto.url(), picto.nombre(),
                TipoMensaje.CHAT
        );
        agregarBurbujaMensaje(m);
        Platform.runLater(() -> scrollMensajes.setVvalue(1.0));
    }

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}
