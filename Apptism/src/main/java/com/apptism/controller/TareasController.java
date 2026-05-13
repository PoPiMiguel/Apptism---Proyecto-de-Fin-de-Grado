package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Tarea;
import com.apptism.entity.Usuario;
import com.apptism.service.ArasaacService;
import com.apptism.service.ArasaacService.PictogramaDTO;
import com.apptism.service.RutinaService;
import com.apptism.service.TareaService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
 * Controlador de la pantalla de tareas.
 *
 * <p>Muestra una interfaz diferente según el rol del usuario activo:</p>
 * <ul>
 *   <li><b>Tutor</b>: puede crear tareas para sus niños con un pictograma
 *       de ARASAAC, asignar puntos, verlas en lista y eliminarlas.</li>
 *   <li><b>Niño</b>: ve sus tareas como tarjetas visuales y las marca como
 *       completadas para ganar estrellas. La animación de puntos se muestra
 *       al completar cada tarea.</li>
 * </ul>
 */

@Component
public class TareasController implements Initializable {

    @FXML private BorderPane panelTutor;
    @FXML private ListView<String> listaTareas;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtBuscarPicto;
    @FXML private FlowPane  panelPictosTarea;
    @FXML private ComboBox<String> cmbNinoTutor;
    @FXML private Spinner<Integer> spinnerPuntos;
    @FXML private Label lblPuntosTotales;

    @FXML private StackPane rootStackNino;
    @FXML private FlowPane  flowTareasNino;
    @FXML private Label     lblPuntosNino;

    @Autowired private TareaService  tareaService;
    @Autowired private RutinaService rutinaService;
    @Autowired private ArasaacService arasaacService;
    @Autowired private StageManager  stageManager;

    /** Tareas cargadas actualmente para la vista del tutor. */

    private List<Tarea>   tareasActualesTutor;

    /** Tareas cargadas actualmente para la vista del niño. */

    private List<Tarea>   tareasActualesNino;

    /** Niños disponibles para el combo del tutor. */

    private List<Usuario> ninosDisponibles;

    /** Pictograma seleccionado actualmente en el formulario del tutor. */

    private PictogramaDTO pictogramaSeleccionado = null;

    /**
     * Inicializa la pantalla según el rol del usuario activo.
     * Si es tutor, configura el formulario y carga sus tareas;
     * si es niño, carga sus tarjetas de tareas pendientes.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE
                || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            SpinnerValueFactory<Integer> factory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 10, 5);
            spinnerPuntos.setValueFactory(factory);

            ninosDisponibles = rutinaService.getNinosDelTutor(usuario.getId());
            if (!ninosDisponibles.isEmpty()) {
                cmbNinoTutor.setItems(FXCollections.observableArrayList(
                        ninosDisponibles.stream().map(Usuario::getNombre).toList()));
                cmbNinoTutor.getSelectionModel().selectFirst();
            } else {
                cmbNinoTutor.setPromptText("No tienes niños asignados");
            }

            txtBuscarPicto.setOnAction(e -> buscarPictogramas());

            cargarTareasTutor();
        } else {
            cargarTareasNino();
        }
    }

    /**
     * Busca pictogramas en ARASAAC con la palabra del campo de búsqueda
     * y los muestra en el panel de selección. La búsqueda se realiza en
     * un hilo secundario para no bloquear la interfaz.
     */

    @FXML
    private void buscarPictogramas() {
        String palabra = txtBuscarPicto.getText().trim();
        if (palabra.isBlank()) return;

        panelPictosTarea.getChildren().clear();
        Label cargando = new Label("Buscando...");
        cargando.setStyle("-fx-text-fill:#888; -fx-font-size:13px;");
        panelPictosTarea.getChildren().add(cargando);

        new Thread(() -> {
            List<PictogramaDTO> resultados = arasaacService.buscar(palabra);
            Platform.runLater(() -> {
                panelPictosTarea.getChildren().clear();
                if (resultados.isEmpty()) {
                    Label vacio = new Label("Sin resultados para \"" + palabra + "\"");
                    vacio.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");
                    panelPictosTarea.getChildren().add(vacio);
                } else {
                    resultados.forEach(this::agregarPictoSeleccionable);
                }
            });
        }).start();
    }

    /**
     * Crea una tarjeta de pictograma seleccionable en el panel del formulario del tutor.
     * La imagen se carga en un hilo secundario. Al hacer clic se selecciona el pictograma.
     *
     * @param picto datos del pictograma a mostrar
     */

    private void agregarPictoSeleccionable(PictogramaDTO picto) {
        VBox tarjeta = new VBox(3);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(5));
        tarjeta.setPrefWidth(80);
        tarjeta.setUserData(picto.id());
        tarjeta.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;");

        ImageView img = new ImageView();
        img.setFitWidth(56);
        img.setFitHeight(56);
        img.setPreserveRatio(true);
        new Thread(() -> {
            try {
                Image imagen = new Image(picto.url(), 56, 56, true, true, true);
                Platform.runLater(() -> img.setImage(imagen));
            } catch (Exception ignored) {}
        }).start();

        Label lbl = new Label(picto.nombre());
        lbl.setStyle("-fx-font-size:10px;");
        lbl.setWrapText(true);
        lbl.setMaxWidth(74);

        tarjeta.getChildren().addAll(img, lbl);

        tarjeta.setOnMouseClicked(e -> seleccionarPictograma(picto, tarjeta));
        tarjeta.setOnMouseEntered(e ->
                tarjeta.setStyle("-fx-background-color:#DFFAEC; -fx-background-radius:10px; -fx-cursor:hand;"));
        tarjeta.setOnMouseExited(e -> {
            boolean esSel = pictogramaSeleccionado != null
                    && pictogramaSeleccionado.id() == (int) tarjeta.getUserData();
            tarjeta.setStyle(esSel
                    ? "-fx-background-color:#B8EDD9; -fx-background-radius:10px; -fx-border-color:#4A6F5A; -fx-border-radius:10px; -fx-cursor:hand;"
                    : "-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;");
        });

        panelPictosTarea.getChildren().add(tarjeta);
    }

    /**
     * Marca el pictograma elegido visualmente y desmarca el anterior.
     *
     * @param picto        datos del pictograma seleccionado
     * @param tarjetaActual tarjeta sobre la que se ha hecho clic
     */

    private void seleccionarPictograma(PictogramaDTO picto, VBox tarjetaActual) {
        panelPictosTarea.getChildren().forEach(n ->
                n.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;"));
        tarjetaActual.setStyle(
                "-fx-background-color:#B8EDD9; -fx-background-radius:10px;" +
                        "-fx-border-color:#4A6F5A; -fx-border-radius:10px; -fx-cursor:hand;");
        pictogramaSeleccionado = picto;
    }

    /**
     * Carga todas las tareas de los niños del tutor y las muestra en la lista.
     * Actualiza también el contador de tareas asignadas.
     */

    private void cargarTareasTutor() {
        tareasActualesTutor = tareaService.getTareasDeNinosDelTutor(
                LoginController.usuarioActivo.getId());
        listaTareas.setItems(FXCollections.observableArrayList(
                tareasActualesTutor.stream().map(t ->
                        (t.isCompletada() ? "[HECHA] " : "[NOHECHA] ") +
                                "[" + t.getNino().getNombre() + "] " +
                                t.getTitulo() +
                                (t.getPuntosPorCompletar() > 0
                                        ? " [+" + t.getPuntosPorCompletar() + " pts]" : "") +
                                (t.getPictogramaId() != null ? " Con picto" : " Sin picto")
                ).toList()
        ));
        lblPuntosTotales.setText(tareasActualesTutor.size() + " tareas asignadas");
    }

    /**
     * Crea una tarea nueva con los datos del formulario y la asigna al niño seleccionado.
     * Limpia el formulario y recarga la lista al terminar.
     */

    @FXML
    private void onCrearTarea() {
        String titulo = txtTitulo.getText().trim();
        if (titulo.isBlank()) {
            alerta("El título de la tarea es obligatorio.");
            return;
        }
        int idxNino = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        if (idxNino < 0 || ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            alerta("Selecciona un niño al que asignar la tarea.\n" +
                    "Asegúrate de que tienes niños asignados en el sistema.");
            return;
        }

        Long ninoId    = ninosDisponibles.get(idxNino).getId();
        Long creadorId = LoginController.usuarioActivo.getId();
        int  puntos    = spinnerPuntos.getValue();

        Integer pictoId  = pictogramaSeleccionado != null ? pictogramaSeleccionado.id() : null;
        String  pictoUrl = pictogramaSeleccionado != null ? pictogramaSeleccionado.url() : null;

        tareaService.crearTarea(titulo, pictoId, pictoUrl, puntos, ninoId, creadorId);
        txtTitulo.clear();
        txtBuscarPicto.clear();
        panelPictosTarea.getChildren().clear();
        pictogramaSeleccionado = null;
        cargarTareasTutor();
    }

    /**
     * Elimina la tarea seleccionada en la lista y recarga la vista del tutor.
     */

    @FXML
    private void onEliminarTarea() {
        int idx = listaTareas.getSelectionModel().getSelectedIndex();
        if (idx < 0) { alerta("Selecciona una tarea para eliminar."); return; }
        tareaService.eliminarTarea(tareasActualesTutor.get(idx).getId());
        cargarTareasTutor();
    }

    /**
     * Carga las tareas del niño activo y actualiza el contador de puntos.
     */

    private void cargarTareasNino() {
        tareasActualesNino = tareaService.getTareasByNino(
                LoginController.usuarioActivo.getId());
        actualizarPuntosNino();
        renderizarTarjetasNino();
    }

    /**
     * Limpia el panel y vuelve a dibujar todas las tarjetas de tareas del niño.
     * Si no hay tareas, muestra un mensaje informativo.
     */

    private void renderizarTarjetasNino() {
        if (flowTareasNino == null) return;
        flowTareasNino.getChildren().clear();

        if (tareasActualesNino == null || tareasActualesNino.isEmpty()) {
            Label vacio = new Label("Aún no tienes tareas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowTareasNino.getChildren().add(vacio);
            return;
        }

        for (Tarea t : tareasActualesNino) {
            flowTareasNino.getChildren().add(crearTarjetaTareaNino(t));
        }
    }

    /**
     * Construye la tarjeta visual de una tarea para la vista del niño.
     * Incluye el pictograma (si lo tiene), el título, los puntos que vale
     * y un botón para marcarla como completada (si aún no lo está).
     *
     * @param tarea la tarea a representar
     * @return nodo VBox listo para añadir al panel
     */

    private VBox crearTarjetaTareaNino(Tarea tarea) {
        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(210);
        tarjeta.setPrefHeight(260);
        String colorFondo = tarea.isCompletada() ? "#E8FAF4" : "white";
        tarjeta.setStyle(
                "-fx-background-color:" + colorFondo + ";" +
                        "-fx-background-radius:24px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3);"
        );

        if (tarea.getPictogramaUrl() != null && !tarea.getPictogramaUrl().isBlank()) {
            ImageView img = new ImageView();
            img.setFitWidth(90);
            img.setFitHeight(90);
            img.setPreserveRatio(true);
            new Thread(() -> {
                try {
                    Image imagen = new Image(tarea.getPictogramaUrl(), 90, 90, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {}
            }).start();
            tarjeta.getChildren().add(img);
        }

        Label lblNombre = new Label(tarea.getTitulo());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(185);
        lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        if (tarea.getPuntosPorCompletar() > 0) {
            Label lblEstrellas = new Label(puntosAEstrellas(tarea.getPuntosPorCompletar()));
            lblEstrellas.setStyle("-fx-font-size:16px;");
            tarjeta.getChildren().add(lblEstrellas);
        }

        if (!tarea.isCompletada()) {
            Button btn = new Button("Hecho");
            btn.setStyle(
                    "-fx-background-color:#B8EDD9; -fx-text-fill:#4A6F5A;" +
                            "-fx-font-size:15px; -fx-font-weight:bold;" +
                            "-fx-background-radius:16px; -fx-padding:9px 18px; -fx-cursor:hand;"
            );
            btn.setOnAction(e -> completarTareaNino(tarea, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            Label lbl = new Label("Completada");
            lbl.setStyle("-fx-text-fill:#81D8A3; -fx-font-weight:bold; -fx-font-size:13px;");
            tarjeta.getChildren().add(lbl);
        }
        return tarjeta;
    }

    /**
     * Marca la tarea como completada, actualiza los puntos del niño en sesión,
     * lanza la animación de celebración y recarga las tarjetas.
     *
     * @param tarea   la tarea que el niño ha completado
     * @param tarjeta la tarjeta visual asociada (usada para la animación)
     */

    private void completarTareaNino(Tarea tarea, VBox tarjeta) {
        int puntos      = tarea.getPuntosPorCompletar();
        int nuevosPuntos = tareaService.completarTarea(tarea.getId());
        LoginController.usuarioActivo.setPuntosAcumulados(nuevosPuntos);
        tarea.setCompletada(true);

        if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, puntos);

        actualizarPuntosNino();
        Platform.runLater(this::renderizarTarjetasNino);
    }

    /**
     * Actualiza la etiqueta de puntos del niño con la representación visual actual.
     */

    private void actualizarPuntosNino() {
        if (lblPuntosNino != null)
            lblPuntosNino.setText(
                    puntosAEstrellas(LoginController.usuarioActivo.getPuntosAcumulados()));
    }

    /**
     * Convierte una cantidad de puntos en emojis visuales escalonados.
     *
     * <p>Escala: 1–9 puntos = estrellas, cada 10 estrellas = 1 corona,
     * cada 5 coronas (50 puntos) = 1 diamante.</p>
     *
     * @param puntos cantidad de puntos a convertir
     * @return cadena de emojis representando los puntos; cadena vacía si es 0 o negativo
     */

    static String puntosAEstrellas(int puntos) {
        if (puntos <= 0) return "";

        int diamantes = puntos / 50;
        int resto1    = puntos % 50;
        int coronas   = resto1 / 10;
        int estrellas = resto1 % 10;

        StringBuilder sb = new StringBuilder();
        sb.append("💎".repeat(diamantes));
        sb.append("👑".repeat(coronas));
        sb.append("⭐".repeat(estrellas));
        return sb.toString();
    }

    /**
     * Muestra un diálogo de información con el mensaje indicado.
     *
     * @param msg mensaje a mostrar
     */

    private void alerta(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    /** Vuelve al dashboard. */

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}