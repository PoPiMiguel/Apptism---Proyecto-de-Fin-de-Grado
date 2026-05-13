package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Recompensa;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.service.RecompensaService;
import com.apptism.service.UsuarioService;
import com.apptism.ui.AnimacionUtil;
import com.apptism.ui.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de recompensas.
 *
 * <p>Muestra una interfaz diferente según el rol del usuario activo:</p>
 * <ul>
 *   <li><b>Tutor</b>: puede crear recompensas y ver las que ha creado.</li>
 *   <li><b>Niño</b>: ve las recompensas disponibles de sus tutores y puede
 *       canjearlas si tiene puntos suficientes. Los puntos se descuentan
 *       automáticamente y el canje queda registrado en el historial del tutor.</li>
 * </ul>
 */

@Component
public class RecompensasController implements Initializable {

    @FXML private BorderPane       panelTutor;
    @FXML private ListView<String> listaRecompensas;
    @FXML private TextField        txtDescripcion;
    @FXML private Spinner<Integer> spinnerPuntos;
    @FXML private Label            lblPuntosDisponibles;

    @FXML private StackPane rootStackNino;
    @FXML private FlowPane  flowRecompensasNino;
    @FXML private Label     lblPuntosNino;

    @Autowired private RecompensaService recompensaService;
    @Autowired private UsuarioService    usuarioService;
    @Lazy @Autowired private StageManager stageManager;

    /** Recompensas cargadas actualmente para la vista del tutor. */

    private List<Recompensa> recompensasActualesTutor;

    /**
     * Inicializa la pantalla según el rol del usuario activo.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Usuario usuario = LoginController.usuarioActivo;
        boolean esTutor = usuario.getRol() == RolUsuario.PADRE
                || usuario.getRol() == RolUsuario.PROFESOR;

        if (panelTutor != null)    panelTutor.setVisible(esTutor);
        if (rootStackNino != null) rootStackNino.setVisible(!esTutor);

        if (esTutor) {
            spinnerPuntos.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10));
            lblPuntosDisponibles.setText("Recompensas que has creado");
            cargarRecompensasTutor();
        } else {
            actualizarPuntosNino();
            cargarRecompensasNino();
        }
    }

    /**
     * Carga las recompensas activas del tutor y las muestra en la lista.
     */

    private void cargarRecompensasTutor() {
        recompensasActualesTutor = recompensaService.getRecompensasDisponibles(
                LoginController.usuarioActivo.getId());
        listaRecompensas.setItems(FXCollections.observableArrayList(
                recompensasActualesTutor.stream()
                        .map(r -> r.getDescripcion() + " — " + r.getPuntosNecesarios() + " pts")
                        .toList()
        ));
    }

    /**
     * Crea una recompensa nueva con los datos del formulario.
     * Limpia el campo y recarga la lista al terminar.
     */

    @FXML
    private void onCrearRecompensa() {
        String desc = txtDescripcion.getText().trim();
        if (desc.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Escribe una descripción.").showAndWait();
            return;
        }
        recompensaService.crearRecompensa(desc, spinnerPuntos.getValue(),
                LoginController.usuarioActivo.getId());
        txtDescripcion.clear();
        cargarRecompensasTutor();
    }

    /**
     * Carga las recompensas disponibles de todos los tutores asignados al niño.
     */

    private void cargarRecompensasNino() {
        Usuario nino = LoginController.usuarioActivo;
        List<Usuario> tutoresDelNino = usuarioService.getTutoresDeNino(nino.getId());

        List<Recompensa> recompensasDelNino = new java.util.ArrayList<>();
        for (Usuario tutor : tutoresDelNino) {
            recompensasDelNino.addAll(recompensaService.getRecompensasDisponibles(tutor.getId()));
        }

        renderizarTarjetasNino(recompensasDelNino, nino.getPuntosAcumulados());
    }

    /**
     * Limpia el panel y dibuja las tarjetas de recompensas disponibles para el niño.
     * Las tarjetas muestran si el niño puede o no canjear cada recompensa.
     *
     * @param recompensas      lista de recompensas a mostrar
     * @param puntosDisponibles puntos actuales del niño
     */

    private void renderizarTarjetasNino(List<Recompensa> recompensas, int puntosDisponibles) {
        if (flowRecompensasNino == null) return;
        flowRecompensasNino.getChildren().clear();

        if (recompensas.isEmpty()) {
            Label vacio = new Label("Aún no hay recompensas.\nTu tutor las añadirá pronto.");
            vacio.setStyle("-fx-font-size:18px; -fx-text-fill:#9BB0A0; -fx-text-alignment:center;");
            flowRecompensasNino.getChildren().add(vacio);
            return;
        }

        for (Recompensa r : recompensas) {
            flowRecompensasNino.getChildren().add(crearTarjetaRecompensaNino(r, puntosDisponibles));
        }
    }

    /**
     * Construye la tarjeta visual de una recompensa para la vista del niño.
     * Si el niño tiene puntos suficientes se muestra el botón de canje;
     * si no, se indica cuántos puntos le faltan.
     *
     * @param recompensa        la recompensa a representar
     * @param puntosDisponibles puntos actuales del niño
     * @return nodo VBox listo para añadir al panel
     */

    private VBox crearTarjetaRecompensaNino(Recompensa recompensa, int puntosDisponibles) {
        boolean puedeGanar = puntosDisponibles >= recompensa.getPuntosNecesarios();

        VBox tarjeta = new VBox(12);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setPrefWidth(200);
        tarjeta.setPrefHeight(230);
        tarjeta.setStyle(
                "-fx-background-color:" + (puedeGanar ? "#F2F1FD" : "#F5F5F5") + ";" +
                        "-fx-background-radius:24px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.09),10,0,0,3);" +
                        (puedeGanar ? "" : "-fx-opacity:0.7;")
        );

        Label lblGift = new Label("🎁");
        lblGift.setStyle("-fx-font-size:50px;");
        tarjeta.getChildren().add(lblGift);

        Label lblNombre = new Label(recompensa.getDescripcion());
        lblNombre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#4A6F5A;");
        lblNombre.setMaxWidth(180);
        lblNombre.setWrapText(true);
        tarjeta.getChildren().add(lblNombre);

        Label lblEstrellas = new Label(TareasController.puntosAEstrellas(recompensa.getPuntosNecesarios()));
        lblEstrellas.setStyle("-fx-font-size:16px; -fx-text-fill:"
                + (puedeGanar ? "#4A6F5A" : "#9BB0A0") + "; -fx-font-weight:bold;");
        tarjeta.getChildren().add(lblEstrellas);

        if (puedeGanar) {
            Button btn = new Button("¡Canjear!");
            btn.getStyleClass().add("btn-nino-verde");
            btn.setOnAction(e -> canjearRecompensaNino(recompensa, tarjeta));
            tarjeta.getChildren().add(btn);
        } else {
            int faltanPts = recompensa.getPuntosNecesarios() - puntosDisponibles;
            Label lblFalta = new Label("Faltan " + TareasController.puntosAEstrellas(faltanPts));
            lblFalta.setStyle("-fx-text-fill:#9BB0A0; -fx-font-size:13px;");
            tarjeta.getChildren().add(lblFalta);
        }
        return tarjeta;
    }

    /**
     * Procesa el canje de una recompensa por el niño activo.
     * Si el canje es exitoso, actualiza los puntos en sesión, lanza la animación
     * y recarga las tarjetas. Si no hay puntos suficientes, muestra un aviso.
     *
     * @param recompensa la recompensa que el niño quiere canjear
     * @param tarjeta    la tarjeta visual asociada
     */

    private void canjearRecompensaNino(Recompensa recompensa, VBox tarjeta) {
        boolean exito = recompensaService.canjearRecompensa(
                LoginController.usuarioActivo.getId(), recompensa.getId());
        if (exito) {
            int nuevos = LoginController.usuarioActivo.getPuntosAcumulados()
                    - recompensa.getPuntosNecesarios();
            LoginController.usuarioActivo.setPuntosAcumulados(nuevos);
            if (rootStackNino != null) AnimacionUtil.mostrarPuntos(rootStackNino, 0);
            actualizarPuntosNino();
            cargarRecompensasNino();
        } else {
            new Alert(Alert.AlertType.WARNING, "No tienes suficientes estrellas.").showAndWait();
        }
    }

    /**
     * Actualiza la etiqueta de puntos del niño con la representación visual actual.
     */

    private void actualizarPuntosNino() {
        if (lblPuntosNino != null)
            lblPuntosNino.setText(
                    TareasController.puntosAEstrellas(
                            LoginController.usuarioActivo.getPuntosAcumulados()));
    }

    /** Vuelve al dashboard. */

    @FXML private void onVolver() { stageManager.switchScene(FxmlView.DASHBOARD); }
}