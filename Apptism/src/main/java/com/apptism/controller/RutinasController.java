package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Rutina;
import com.apptism.entity.Usuario;
import com.apptism.entity.ZonaHoraria;
import com.apptism.service.ArasaacService;
import com.apptism.service.ArasaacService.PictogramaDTO;
import com.apptism.service.RutinaService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de rutinas.
 *
 * <p>Muestra una interfaz diferente según el rol del usuario activo:</p>
 * <ul>
 *   <li><b>Tutor</b>: puede crear rutinas para sus niños asignados, seleccionar
 *       un pictograma de ARASAAC, organizarlas por franja horaria (mañana,
 *       mediodía, noche) y eliminarlas.</li>
 *   <li><b>Niño</b>: ve sus rutinas como tarjetas visuales con pictograma
 *       y puede marcarlas como completadas.</li>
 * </ul>
 */

@Component
public class RutinasController implements Initializable {

    @FXML private BorderPane panelTutor;
    @FXML private TabPane    tabZonas;
    @FXML private ListView<String> listaManana;
    @FXML private ListView<String> listaMediodia;
    @FXML private ListView<String> listaNoche;
    @FXML private TextField  txtNombreRutina;
    @FXML private TextField  txtBuscarPicto;
    @FXML private FlowPane   panelPictosRutina;
    @FXML private ComboBox<String> cmbZona;
    @FXML private ComboBox<String> cmbNinoTutor;

    @FXML private StackPane rootStackNino;
    @FXML private FlowPane  flowRutinasNino;

    @Autowired private RutinaService  rutinaService;
    @Autowired private ArasaacService arasaacService;
    @Lazy @Autowired private StageManager stageManager;

    /** Rutinas cargadas actualmente para la vista del niño. */

    private List<Rutina>  rutinasActualesNino;

    /** Niños disponibles para el combo del tutor. */

    private List<Usuario> ninosDisponibles;

    /** Rutinas de la franja de mañana (vista tutor). */

    private List<Rutina> rutinasManana;

    /** Rutinas de la franja de mediodía (vista tutor). */

    private List<Rutina> rutinasMediodia;

    /** Rutinas de la franja de noche (vista tutor). */

    private List<Rutina> rutinasNoche;

    /** Pictograma seleccionado actualmente en el formulario del tutor. */

    private PictogramaDTO pictogramaSeleccionado = null;

    /**
     * Inicializa la pantalla según el rol del usuario activo.
     * Si es tutor carga el formulario de creación y las listas por zona;
     * si es niño, carga sus tarjetas de rutinas.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE
                || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            cmbZona.setItems(FXCollections.observableArrayList("MANANA", "MEDIODIA", "NOCHE"));
            cmbZona.setValue("MANANA");
            cargarNinosEnComboTutor();
            cargarTodasLasRutinasTutor();

            txtBuscarPicto.setOnAction(e -> buscarPictogramas());
        } else {
            cargarRutinasNino();
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

        panelPictosRutina.getChildren().clear();
        Label cargando = new Label("Buscando...");
        cargando.setStyle("-fx-text-fill:#888; -fx-font-size:13px;");
        panelPictosRutina.getChildren().add(cargando);

        new Thread(() -> {
            List<PictogramaDTO> resultados = arasaacService.buscar(palabra);
            Platform.runLater(() -> {
                panelPictosRutina.getChildren().clear();
                if (resultados.isEmpty()) {
                    Label vacio = new Label("Sin resultados para \"" + palabra + "\"");
                    vacio.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");
                    panelPictosRutina.getChildren().add(vacio);
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

        panelPictosRutina.getChildren().add(tarjeta);
    }

    /**
     * Marca el pictograma elegido visualmente y desmarca el anterior.
     *
     * @param picto       datos del pictograma seleccionado
     * @param tarjetaActual tarjeta sobre la que se ha hecho clic
     */

    private void seleccionarPictograma(PictogramaDTO picto, VBox tarjetaActual) {
        panelPictosRutina.getChildren().forEach(n ->
                n.setStyle("-fx-background-color:#F7FFF7; -fx-background-radius:10px; -fx-cursor:hand;"));
        tarjetaActual.setStyle(
                "-fx-background-color:#B8EDD9; -fx-background-radius:10px;" +
                        "-fx-border-color:#4A6F5A; -fx-border-radius:10px; -fx-cursor:hand;");
        pictogramaSeleccionado = picto;
    }

    /**
     * Carga los niños asignados al tutor en el combo de selección.
     * Si no tiene ninguno, muestra un texto informativo en el combo.
     */

    private void cargarNinosEnComboTutor() {
        ninosDisponibles = rutinaService.getNinosDelTutor(LoginController.usuarioActivo.getId());
        if (ninosDisponibles.isEmpty()) {
            cmbNinoTutor.setPromptText("No tienes niños asignados");
        } else {
            cmbNinoTutor.setItems(FXCollections.observableArrayList(
                    ninosDisponibles.stream().map(Usuario::getNombre).toList()));
            cmbNinoTutor.getSelectionModel().selectFirst();
        }
    }

    /**
     * Carga las rutinas del niño seleccionado en el combo, separadas por franja horaria.
     */

    private void cargarTodasLasRutinasTutor() {
        if (ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            listaManana.setItems(FXCollections.observableArrayList());
            listaMediodia.setItems(FXCollections.observableArrayList());
            listaNoche.setItems(FXCollections.observableArrayList());
            return;
        }
        int idx = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        Long ninoId = (idx >= 0)
                ? ninosDisponibles.get(idx).getId()
                : ninosDisponibles.get(0).getId();

        cargarRutinasPorZona(ninoId, ZonaHoraria.MANANA,   listaManana);
        cargarRutinasPorZona(ninoId, ZonaHoraria.MEDIODIA, listaMediodia);
        cargarRutinasPorZona(ninoId, ZonaHoraria.NOCHE,    listaNoche);
    }

    /**
     * Carga las rutinas de una franja horaria concreta en la lista correspondiente
     * y actualiza la caché interna de esa zona.
     *
     * @param ninoId identificador del niño
     * @param zona   franja horaria a cargar
     * @param lista  componente ListView donde se mostrarán
     */

    private void cargarRutinasPorZona(Long ninoId, ZonaHoraria zona, ListView<String> lista) {
        List<Rutina> rutinas = rutinaService.getRutinasByZona(ninoId, zona);

        switch (zona) {
            case MANANA   -> rutinasManana   = rutinas;
            case MEDIODIA -> rutinasMediodia = rutinas;
            case NOCHE    -> rutinasNoche    = rutinas;
        }

        lista.setItems(FXCollections.observableArrayList(
                rutinas.stream()
                        .map(r -> (r.isCompletada() ? "[COMPL] " : "[NCOMPL] ") +
                                r.getNombre() +
                                (r.getPictogramaId() != null ? " CON PIC" : " SIN PIC"))
                        .toList()
        ));
    }

    /**
     * Elimina la rutina seleccionada en la pestaña activa, tras pedir confirmación.
     */

    @FXML
    private void onEliminarRutina() {
        int tabIdx = tabZonas.getSelectionModel().getSelectedIndex();

        ListView<String> listaActiva;
        List<Rutina>     rutinasActivas;

        switch (tabIdx) {
            case 1  -> { listaActiva = listaMediodia; rutinasActivas = rutinasMediodia; }
            case 2  -> { listaActiva = listaNoche;    rutinasActivas = rutinasNoche;    }
            default -> { listaActiva = listaManana;   rutinasActivas = rutinasManana;   }
        }

        int idx = listaActiva.getSelectionModel().getSelectedIndex();
        if (idx < 0 || rutinasActivas == null || rutinasActivas.isEmpty()) {
            alerta("Selecciona una rutina de la lista para eliminarla.");
            return;
        }

        Rutina rutina = rutinasActivas.get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la rutina \"" + rutina.getNombre() + "\"?\nEsta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                rutinaService.eliminarRutina(rutina.getId());
                cargarTodasLasRutinasTutor();
            }
        });
    }

    /**
     * Crea una rutina nueva con los datos del formulario y la asigna al niño seleccionado.
     * Limpia el formulario y recarga las listas al terminar.
     */

    @FXML
    private void onCrearRutina() {
        String nombre = txtNombreRutina.getText().trim();
        if (nombre.isBlank()) {
            alerta("Escribe un nombre para la rutina.");
            return;
        }

        int idxNino = cmbNinoTutor.getSelectionModel().getSelectedIndex();
        if (idxNino < 0 || ninosDisponibles == null || ninosDisponibles.isEmpty()) {
            alerta("Selecciona un niño al que asignar la rutina.\n" +
                    "Asegúrate de que tienes niños asignados en el sistema.");
            return;
        }

        Long ninoId  = ninosDisponibles.get(idxNino).getId();
        Integer pictoId  = pictogramaSeleccionado != null ? pictogramaSeleccionado.id() : null;
        String  pictoUrl = pictogramaSeleccionado != null ? pictogramaSeleccionado.url() : null;

        rutinaService.crearRutina(nombre, ZonaHoraria.valueOf(cmbZona.getValue()),
                ninoId, LoginController.usuarioActivo.getId(), pictoId, pictoUrl);

        txtNombreRutina.clear();
        txtBuscarPicto.clear();
        panelPictosRutina.getChildren().clear();
        pictogramaSeleccionado = null;
        cargarTodasLasRutinasTutor();
    }

    /**
     * Carga todas las rutinas del niño activo y las renderiza como tarjetas.
     */

    private void cargarRutinasNino() {
        rutinasActualesNino = rutinaService.todasLasRutinas(
                LoginController.usuarioActivo.getId());
        renderizarTarjetasNino();
    }

    /**
     * Limpia el panel y vuelve a dibujar todas las tarjetas de rutinas del niño.
     * Si no hay rutinas, muestra un mensaje informativo.
     */

    private void renderizarTarjetasNino() {
        if (flowRutinasNino == null) return;
        flowRutinasNino.getChildren().clear();

        if (rutinasActualesNino == null || rutinasActualesNino.isEmpty()) {
            Label vacio = new Label("Aún no tienes rutinas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowRutinasNino.getChildren().add(vacio);
            return;
        }

        for (Rutina r : rutinasActualesNino) {
            flowRutinasNino.getChildren().add(crearTarjetaRutinaNino(r));
        }
    }

    /**
     * Construye la tarjeta visual de una rutina para la vista del niño.
     * Incluye el pictograma (si lo tiene), el nombre, la franja horaria
     * y un botón para marcarla como completada (si aún no lo está).
     *
     * @param rutina la rutina a representar
     * @return nodo VBox listo para añadir al panel
     */

    private VBox crearTarjetaRutinaNino(Rutina rutina) {
        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(200);
        tarjeta.setPrefHeight(240);
        String colorFondo = rutina.isCompletada() ? "#E8FAF4" : "white";
        tarjeta.setStyle(
                "-fx-background-color:" + colorFondo + ";" +
                        "-fx-background-radius:24px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),10,0,0,3);"
        );

        if (rutina.getPictogramaUrl() != null && !rutina.getPictogramaUrl().isBlank()) {
            ImageView img = new ImageView();
            img.setFitWidth(90);
            img.setFitHeight(90);
            img.setPreserveRatio(true);
            new Thread(() -> {
                try {
                    Image imagen = new Image(rutina.getPictogramaUrl(), 90, 90, true, true, true);
                    Platform.runLater(() -> img.setImage(imagen));
                } catch (Exception ignored) {}
            }).start();
            tarjeta.getChildren().add(img);
        }

        Label lblNombre = new Label(rutina.getNombre());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(180);
        lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        Label lblZona = new Label(emojiZonaTexto(rutina.getZonaHoraria()));
        lblZona.setStyle("-fx-font-size:12px; -fx-text-fill:#888;");
        tarjeta.getChildren().add(lblZona);

        if (!rutina.isCompletada()) {
            Button btn = new Button("Hecho");
            btn.setStyle(
                    "-fx-background-color:#B8EDD9; -fx-text-fill:#4A6F5A;" +
                            "-fx-font-size:15px; -fx-font-weight:bold;" +
                            "-fx-background-radius:16px; -fx-padding:9px 18px; -fx-cursor:hand;"
            );
            btn.setOnAction(e -> {
                rutinaService.marcarCompletada(rutina.getId());
                rutina.setCompletada(true);
                renderizarTarjetasNino();
            });
            tarjeta.getChildren().add(btn);
        } else {
            Label lbl = new Label("Completada");
            lbl.setStyle("-fx-text-fill:#81D8A3; -fx-font-weight:bold; -fx-font-size:13px;");
            tarjeta.getChildren().add(lbl);
        }
        return tarjeta;
    }

    /**
     * Devuelve el nombre legible en español de una franja horaria.
     *
     * @param zona franja horaria
     * @return texto descriptivo de la zona
     */

    private String emojiZonaTexto(ZonaHoraria zona) {
        return switch (zona) {
            case MANANA   -> "Mañana";
            case MEDIODIA -> "Mediodía";
            case NOCHE    -> "Noche";
        };
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