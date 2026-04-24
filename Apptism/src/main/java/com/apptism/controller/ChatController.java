// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: ChatController.java
// ═══════════════════════════════════════════════════════════════════
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

/**
 * Controlador de la pantalla de chat con pictogramas.
 *
 * Permite que niños y tutores se comuniquen usando pictogramas de ARASAAC.
 * Al abrirse, carga los contactos disponibles y una selección de pictogramas
 * por defecto (emociones básicas + categorías frecuentes) sin necesidad de buscar.
 *
 * El panel de mensajes se desplaza automáticamente al final cada vez que
 * llega un mensaje nuevo, usando un listener sobre la altura del panel.
 */
@Component
public class ChatController implements Initializable {

    /** Panel vertical donde se apilan las burbujas de mensajes. */
    @FXML
    private VBox panelMensajes;

    /** El scroll que envuelve el panel de mensajes; lo usamos para bajar al final. */
    @FXML
    private ScrollPane scrollMensajes;

    /** Panel donde se muestran los pictogramas seleccionables. */
    @FXML
    private FlowPane panelPictos;

    /** Campo para buscar pictogramas por palabra clave. */
    @FXML
    private TextField txtBuscar;

    /** Selector del contacto con el que estamos hablando. */
    @FXML
    private ComboBox<String> cmbInterlocutor;

    @Autowired
    private MensajeService mensajeService;
    @Autowired
    private ArasaacService arasaacService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private StageManager stageManager;

    /** Lista de contactos disponibles para el usuario activo. */
    private List<Usuario> contactos;

    /** El contacto que tenemos seleccionado en el desplegable ahora mismo. */
    private Usuario interlocutorActual;

    /**
     * Prepara la pantalla: carga los contactos, los pictogramas por defecto
     * y configura el scroll automático para que siempre se vea el último mensaje.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarContactos();
        cargarPictogramasPorDefecto();
        panelMensajes.heightProperty().addListener(
                (obs, old, val) -> scrollMensajes.setVvalue(1.0));
    }

    /**
     * Carga los contactos en el desplegable y configura el listener para
     * recargar la conversación cuando el usuario cambia de contacto.
     */
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

    /**
     * Limpia el panel y carga el historial de mensajes con el contacto seleccionado.
     */
    private void cargarConversacion() {
        panelMensajes.getChildren().clear();
        if (interlocutorActual == null) return;
        List<Mensaje> msgs = mensajeService.getConversacion(
                LoginController.usuarioActivo.getId(), interlocutorActual.getId());
        msgs.forEach(this::agregarBurbujaMensaje);
    }

    /**
     * Construye la burbuja visual de un mensaje y la añade al panel.
     * Los mensajes propios van a la derecha con fondo verde; los del otro, a la
     * izquierda con fondo blanco. Las imágenes se cargan en un hilo secundario.
     *
     * @param m el mensaje a mostrar
     */
    private void agregarBurbujaMensaje(Mensaje m) {
        boolean esMio = m.getEmisor().getId()
                .equals(LoginController.usuarioActivo.getId());

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
        img.setFitWidth(80);
        img.setFitHeight(80);
        img.setPreserveRatio(true);
        if (m.getPictogramaUrl() != null && !m.getPictogramaUrl().isBlank()) {
            new Thread(() -> {
                try {
                    Image imagen = new Image(m.getPictogramaUrl(), 80, 80, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {
                }
            }).start();
        }

        Label lblTexto = new Label(m.getTextoPictograma());
        lblTexto.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#333;");

        burbuja.getChildren().addAll(img, lblTexto);
        fila.getChildren().add(burbuja);
        panelMensajes.getChildren().add(fila);
    }

    /**
     * Carga los pictogramas por defecto: primero las emociones básicas de forma
     * inmediata, y luego en segundo plano categorías frecuentes (casa, comer, jugar),
     * evitando duplicados.
     */
    private void cargarPictogramasPorDefecto() {
        panelPictos.getChildren().clear();
        arasaacService.getEmocionesBásicas().forEach(this::agregarPictoSeleccionable);

        new Thread(() -> {
            List<PictogramaDTO>[] categorias = new List[]{
                    arasaacService.buscar("casa"),
                    arasaacService.buscar("comer"),
                    arasaacService.buscar("jugar")
            };
            for (List<PictogramaDTO> lista : categorias) {
                Platform.runLater(() -> lista.forEach(p -> {
                    boolean duplicado = panelPictos.getChildren().stream()
                            .anyMatch(node -> node.getUserData() != null &&
                                    node.getUserData().equals(p.id()));
                    if (!duplicado) agregarPictoSeleccionable(p);
                }));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    /**
     * Busca pictogramas por la palabra del campo y actualiza el panel.
     * Si el campo está vacío, recarga los pictogramas por defecto.
     */
    @FXML
    private void onBuscar() {
        String palabra = txtBuscar.getText().trim();
        if (palabra.isBlank()) {
            cargarPictogramasPorDefecto();
            return;
        }

        panelPictos.getChildren().clear();
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

    /**
     * Crea una tarjeta visual para un pictograma y la añade al panel.
     * La imagen se carga en un hilo secundario. Al hacer clic envía el pictograma.
     *
     * @param picto los datos del pictograma
     */
    private void agregarPictoSeleccionable(PictogramaDTO picto) {
        VBox tarjeta = new VBox(4);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(6));
        tarjeta.setPrefWidth(100);
        tarjeta.setUserData(picto.id());
        tarjeta.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:12px; -fx-cursor:hand;");

        ImageView img = new ImageView();
        img.setFitWidth(70);
        img.setFitHeight(70);
        img.setPreserveRatio(true);
        new Thread(() -> {
            try {
                Image imagen = new Image(picto.url(), 70, 70, true, true, true);
                Platform.runLater(() -> img.setImage(imagen));
            } catch (Exception ignored) {
            }
        }).start();

        Label lbl = new Label(picto.nombre());
        lbl.setStyle("-fx-font-size:11px;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(90);

        tarjeta.getChildren().addAll(img, lbl);
        tarjeta.setOnMouseClicked(e -> enviarPicto(picto));
        tarjeta.setOnMouseEntered(e ->
                tarjeta.setStyle("-fx-background-color:#EFFAF3; -fx-background-radius:12px; -fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e ->
                tarjeta.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:12px; -fx-cursor:hand;"));

        panelPictos.getChildren().add(tarjeta);
    }

    /**
     * Envía el pictograma al contacto activo y muestra la burbuja en la conversación.
     *
     * @param picto el pictograma que el usuario ha pulsado
     */
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

    /** Vuelve al dashboard. */
    @FXML
    private void onVolver() {
        stageManager.switchScene(FxmlView.DASHBOARD);
    }
}