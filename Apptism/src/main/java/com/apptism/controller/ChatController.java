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
 * <p>Permite que niños y tutores se comuniquen usando pictogramas de ARASAAC.
 * Al abrirse, carga los contactos vinculados al usuario activo y una selección
 * de pictogramas por defecto (emociones básicas y categorías frecuentes) sin
 * necesidad de buscar.</p>
 *
 * <p>El panel de mensajes se desplaza automáticamente al final cada vez que
 * se añade un mensaje nuevo, usando un listener sobre la altura del panel.</p>
 */

@Component
public class ChatController implements Initializable {

    /** Panel vertical donde se apilan las burbujas de mensajes. */

    @FXML
    private VBox panelMensajes;

    /** Scroll que envuelve el panel de mensajes, usado para bajar al último mensaje. */

    @FXML
    private ScrollPane scrollMensajes;

    /** Panel donde se muestran los pictogramas disponibles para enviar. */

    @FXML
    private FlowPane panelPictos;

    /** Campo para buscar pictogramas por palabra clave. */

    @FXML
    private TextField txtBuscar;

    /** Selector del contacto con el que se está hablando. */

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

    /** Contacto seleccionado actualmente en el desplegable. */

    private Usuario interlocutorActual;

    /**
     * Prepara la pantalla: carga los contactos vinculados, los pictogramas por
     * defecto y configura el scroll automático al último mensaje.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarContactos();
        cargarPictogramasPorDefecto();
        panelMensajes.heightProperty().addListener(
                (obs, old, val) -> scrollMensajes.setVvalue(1.0));
    }

    /**
     * Carga los contactos vinculados al usuario activo en el desplegable
     * y configura el listener para recargar la conversación al cambiar de contacto.
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
     * Limpia el panel de mensajes y carga el historial de chat con el contacto seleccionado.
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
     *
     * <p>Los mensajes propios se alinean a la derecha con fondo verde;
     * los del interlocutor, a la izquierda con fondo blanco.
     * Las imágenes se cargan en un hilo secundario.</p>
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
     * evitando duplicados por identificador.
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
     * Busca pictogramas por la palabra del campo de búsqueda y actualiza el panel.
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
     * Crea una tarjeta visual de pictograma y la añade al panel de selección.
     * La imagen se carga en un hilo secundario. Al hacer clic se envía el pictograma.
     *
     * @param picto datos del pictograma a mostrar
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
     * Envía el pictograma al contacto activo, lo guarda en base de datos y
     * muestra la burbuja en la conversación. Si no hay contacto seleccionado,
     * muestra un aviso.
     *
     * @param picto el pictograma que el usuario ha seleccionado para enviar
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