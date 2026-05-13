package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.service.UsuarioService;
import com.apptism.ui.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador del panel de administración.
 *
 * <p>Solo accesible para usuarios con rol {@code ADMIN}. Tiene dos pestañas:</p>
 * <ul>
 *   <li><b>Gestión de usuarios</b>: crear, eliminar y filtrar por rol.</li>
 *   <li><b>Vinculación tutor–niño</b>: asignar niños a tutores y ver los vínculos existentes.</li>
 * </ul>
 */

@Component
public class AdminController implements Initializable {

    @FXML private TextField     txtNombre;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private RadioButton   rbNino;
    @FXML private RadioButton   rbPadre;
    @FXML private RadioButton   rbProfesor;
    @FXML private ToggleGroup   grupoRol;
    @FXML private Label         lblMensaje;
    @FXML private ListView<String> listaUsuarios;

    @FXML private ComboBox<String> cmbTutor;
    @FXML private ComboBox<String> cmbNino;
    @FXML private Label            lblMensajeVinculo;
    @FXML private ListView<String> listaVinculos;

    @Autowired private UsuarioService usuarioService;
    @Autowired private StageManager   stageManager;

    /** Lista completa de usuarios (sin ADMIN) para la pestaña de gestión. */

    private List<Usuario> todosUsuarios;

    /** Lista que se muestra actualmente tras aplicar el filtro de rol. */

    private List<Usuario> usuariosMostrados;
    private List<Usuario> todosNinos;
    private List<Usuario> todosTutores;

    /**
     * Inicializa el panel: selecciona el rol «Niño» por defecto y carga
     * los datos iniciales de las dos pestañas.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rbNino.setSelected(true);
        cargarTodosLosUsuarios();
        cargarCombosVinculo();
        actualizarTablaVinculos();
    }

    /**
     * Carga todos los usuarios del sistema (excluyendo administradores)
     * y los muestra en la lista de la pestaña de gestión.
     */

    private void cargarTodosLosUsuarios() {
        todosUsuarios = usuarioService.getTodosLosUsuarios()
                .stream()
                .filter(u -> u.getRol() != RolUsuario.ADMIN)
                .collect(Collectors.toList());
        usuariosMostrados = todosUsuarios;
        renderizarLista(usuariosMostrados);
    }

    /**
     * Renderiza la lista de usuarios mostrando rol, nombre y email de cada uno.
     *
     * @param usuarios lista de usuarios a mostrar
     */

    private void renderizarLista(List<Usuario> usuarios) {
        listaUsuarios.setItems(FXCollections.observableArrayList(
                usuarios.stream().map(u ->
                        labelRol(u.getRol()) + "  " + u.getNombre()
                                + "   |  " + u.getEmail()
                ).toList()
        ));
    }

    /**
     * Crea un nuevo usuario con los datos del formulario y el rol seleccionado.
     * Muestra un mensaje de confirmación o de error según el resultado.
     */

    @FXML
    private void onCrearUsuario() {
        String nombre = txtNombre.getText().trim();
        String email  = txtEmail.getText().trim();
        String pass   = txtPassword.getText();

        if (nombre.isBlank() || email.isBlank() || pass.isBlank()) {
            mostrarMensaje("Completa todos los campos.", false);
            return;
        }
        Toggle seleccionado = grupoRol.getSelectedToggle();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un rol.", false);
            return;
        }

        RolUsuario rol;
        if      (seleccionado == rbNino)   rol = RolUsuario.NINO;
        else if (seleccionado == rbPadre)  rol = RolUsuario.PADRE;
        else                               rol = RolUsuario.PROFESOR;

        try {
            usuarioService.crearUsuario(nombre, email, pass, rol);
            mostrarMensaje("Usuario '" + nombre + "' creado correctamente.", true);
            txtNombre.clear(); txtEmail.clear(); txtPassword.clear();
            cargarTodosLosUsuarios();
            cargarCombosVinculo();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    /**
     * Elimina el usuario seleccionado en la lista tras pedir confirmación al administrador.
     */

    @FXML
    private void onEliminarUsuario() {
        int idx = listaUsuarios.getSelectionModel().getSelectedIndex();
        if (idx < 0) { mostrarMensaje("Selecciona un usuario para eliminar.", false); return; }
        Usuario u = usuariosMostrados.get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al usuario " + u.getNombre() + "?\nEsta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                usuarioService.eliminarUsuario(u.getId());
                mostrarMensaje("Usuario eliminado.", true);
                cargarTodosLosUsuarios();
                cargarCombosVinculo();
            }
        });
    }

    /** Recarga la lista de usuarios desde la base de datos. */

    @FXML private void onRefrescar() {
        cargarTodosLosUsuarios();
        mostrarMensaje("Lista actualizada.", true);
    }
    /** Muestra todos los usuarios sin filtrar. */

    @FXML private void onVerTodos()      { usuariosMostrados = todosUsuarios;                    renderizarLista(usuariosMostrados); }

    /** Filtra la lista para mostrar solo los niños. */

    @FXML private void onVerNinos()      { usuariosMostrados = filtrarPorRol(RolUsuario.NINO);     renderizarLista(usuariosMostrados); }

    /** Filtra la lista para mostrar solo los padres. */

    @FXML private void onVerPadres()     { usuariosMostrados = filtrarPorRol(RolUsuario.PADRE);    renderizarLista(usuariosMostrados); }

    /** Filtra la lista para mostrar solo los profesores. */

    @FXML private void onVerProfesores() { usuariosMostrados = filtrarPorRol(RolUsuario.PROFESOR); renderizarLista(usuariosMostrados); }

    /**
     * Filtra la lista de todos los usuarios por un rol específico.
     *
     * @param rol rol por el que filtrar
     * @return lista de usuarios con ese rol
     */

    private List<Usuario> filtrarPorRol(RolUsuario rol) {
        return todosUsuarios.stream().filter(u -> u.getRol() == rol).toList();
    }

    /**
     * Muestra un mensaje de estado en el área de feedback de la pestaña de usuarios.
     *
     * @param texto  mensaje a mostrar
     * @param exito  {@code true} para estilo de éxito (verde); {@code false} para error (rojo)
     */

    private void mostrarMensaje(String texto, boolean exito) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:"
                + (exito ? "#81D8A3" : "#C96070") + ";");
    }

    /**
     * Carga los combos de tutores y niños disponibles para establecer vínculos.
     */

    private void cargarCombosVinculo() {
        todosTutores = usuarioService.getTodosLosTutores();
        todosNinos   = usuarioService.getTodosLosNinos();

        cmbTutor.setItems(FXCollections.observableArrayList(
                todosTutores.stream()
                        .map(t -> labelRol(t.getRol()) + " " + t.getNombre()
                                + " (" + t.getEmail() + ")")
                        .toList()
        ));
        cmbNino.setItems(FXCollections.observableArrayList(
                todosNinos.stream()
                        .map(n -> n.getNombre() + " (" + n.getEmail() + ")")
                        .toList()
        ));
        if (!todosTutores.isEmpty()) cmbTutor.getSelectionModel().selectFirst();
        if (!todosNinos.isEmpty())   cmbNino.getSelectionModel().selectFirst();
    }

    /**
     * Establece el vínculo entre el tutor y el niño seleccionados en los combos.
     * Muestra el resultado de la operación en el área de feedback.
     */

    @FXML
    private void onVincular() {
        int idxTutor = cmbTutor.getSelectionModel().getSelectedIndex();
        int idxNino  = cmbNino.getSelectionModel().getSelectedIndex();
        if (idxTutor < 0 || idxNino < 0) {
            lblMensajeVinculo.setText("Selecciona tutor y alumno.");
            lblMensajeVinculo.setStyle("-fx-text-fill:#C96070; -fx-font-weight:bold;");
            return;
        }
        try {
            usuarioService.vincularNinoATutor(
                    todosTutores.get(idxTutor).getId(),
                    todosNinos.get(idxNino).getId());
            lblMensajeVinculo.setText(
                    todosNinos.get(idxNino).getNombre() + " vinculado a "
                            + todosTutores.get(idxTutor).getNombre());
            lblMensajeVinculo.setStyle("-fx-text-fill:#81D8A3; -fx-font-weight:bold;");
            actualizarTablaVinculos();
        } catch (RuntimeException e) {
            lblMensajeVinculo.setText(e.getMessage());
            lblMensajeVinculo.setStyle("-fx-text-fill:#C96070; -fx-font-weight:bold;");
        }
    }

    /**
     * Actualiza la lista de vínculos existentes mostrando las relaciones
     * tutor → niño de todos los tutores del sistema.
     */

    private void actualizarTablaVinculos() {
        List<String> filas = new java.util.ArrayList<>();
        for (Usuario tutor : usuarioService.getTodosLosTutores()) {
            for (Usuario nino : usuarioService.getNinosDetutor(tutor.getId())) {
                filas.add(labelRol(tutor.getRol()) + " " + tutor.getNombre()
                        + "   ->   " + nino.getNombre());
            }
        }
        if (filas.isEmpty()) filas.add("Aún no hay vínculos establecidos.");
        listaVinculos.setItems(FXCollections.observableArrayList(filas));
    }

    /**
     * Devuelve una etiqueta legible en español para el rol de un usuario.
     *
     * @param rol rol del usuario
     * @return texto descriptivo del rol
     */
    private String labelRol(RolUsuario rol) {
        return switch (rol) {
            case NINO     -> "Alumno";
            case PADRE    -> "Padre";
            case PROFESOR -> "Profesor";
            case ADMIN    -> "Admin";
        };
    }

    /**
     * Cierra la sesión del administrador y vuelve a la pantalla de login.
     */

    @FXML
    private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}