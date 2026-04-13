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
 * Controlador del módulo de chat con pictogramas.
 *
 * <p>Permite la comunicación visual entre niños y tutores mediante
 * pictogramas obtenidos de la API de ARASAAC. Al inicializarse, carga
 * los contactos disponibles y una selección de pictogramas por defecto
 * (emociones básicas + categorías frecuentes) sin necesidad de búsqueda.
 *
 * <p>El panel de mensajes se mantiene desplazado al final automáticamente
 * cada vez que se añade un nuevo mensaje, usando un listener sobre la
 * altura del panel.
 */
@Component
public class ChatController implements Initializable {

    // ── Nodos FXML ────────────────────────────────────────────────

    /**
     * Panel vertical donde se apilan las burbujas de mensaje.
     */
    @FXML
    private VBox panelMensajes;

    /**
     * Scroll que contiene {@link #panelMensajes}; se usa para desplazar al final.
     */
    @FXML
    private ScrollPane scrollMensajes;

    /**
     * Panel de selección de pictogramas disponibles.
     */
    @FXML
    private FlowPane panelPictos;

    /**
     * Campo de búsqueda de pictogramas por palabra clave.
     */
    @FXML
    private TextField txtBuscar;

    /**
     * Selector del interlocutor activo en la conversación.
     */
    @FXML
    private ComboBox<String> cmbInterlocutor;

    // ── Dependencias ──────────────────────────────────────────────
    @Autowired
    private MensajeService mensajeService;
    @Autowired
    private ArasaacService arasaacService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private StageManager stageManager;

    /**
     * Lista de contactos disponibles para el usuario activo.
     */
    private List<Usuario> contactos;

    /**
     * Interlocutor seleccionado actualmente en el combo.
     */
    private Usuario interlocutorActual;

    /**
     * Inicializa el módulo: carga contactos, pictogramas por defecto
     * y configura el scroll automático al final del panel de mensajes.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarContactos();
        cargarPictogramasPorDefecto();
        panelMensajes.heightProperty().addListener(
                (obs, old, val) -> scrollMensajes.setVvalue(1.0));
    }

    /**
     * Carga los contactos del usuario activo en el combo de interlocutores
     * y configura el listener para cargar la conversación al cambiar la selección.
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
     * Limpia el panel de mensajes y carga el historial de conversación
     * con el interlocutor actualmente seleccionado.
     */
    private void cargarConversacion() {
        panelMensajes.getChildren().clear();
        if (interlocutorActual == null) return;
        List<Mensaje> msgs = mensajeService.getConversacion(
                LoginController.usuarioActivo.getId(), interlocutorActual.getId());
        msgs.forEach(this::agregarBurbujaMensaje);
    }

    /**
     * Construye y añade al panel la burbuja visual de un mensaje.
     *
     * <p>Los mensajes propios se alinean a la derecha con fondo verde claro;
     * los del interlocutor, a la izquierda con fondo blanco. Las imágenes
     * de los pictogramas se cargan en un hilo secundario para no bloquear la UI.
     *
     * @param m mensaje a representar visualmente
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
     * Carga el panel de pictogramas con emociones básicas de forma inmediata
     * y categorías adicionales frecuentes (casa, comer, jugar) en segundo plano,
     * evitando duplicados en el panel.
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
     * Crea y añade al panel una tarjeta visual seleccionable para un pictograma.
     * La imagen se carga en un hilo secundario. Al hacer clic, envía el pictograma.
     *
     * @param picto datos del pictograma a representar
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
     * Envía el pictograma seleccionado al interlocutor activo y muestra
     * la burbuja del mensaje en el panel de conversación.
     *
     * @param picto pictograma seleccionado por el usuario
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

    /**
     * Vuelve al dashboard principal.
     */
    @FXML
    private void onVolver() {
        stageManager.switchScene(FxmlView.DASHBOARD);
    }
}