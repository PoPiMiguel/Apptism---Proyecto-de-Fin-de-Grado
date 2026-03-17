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
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class AdminController implements Initializable {

    // --- Tab Crear Usuario ---
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private RadioButton rbNino;
    @FXML private RadioButton rbPadre;
    @FXML private RadioButton rbProfesor;
    @FXML private ToggleGroup grupoRol;
    @FXML private Label lblMensaje;
    @FXML private ListView<String> listaUsuarios;

    // --- Tab Vincular ---
    @FXML private ComboBox<String> cmbTutor;
    @FXML private ComboBox<String> cmbNino;
    @FXML private Label lblMensajeVinculo;
    @FXML private ListView<String> listaVinculos;

    @Autowired private UsuarioService usuarioService;
    @Autowired private StageManager stageManager;

    private List<Usuario> todosUsuarios;
    private List<Usuario> todosNinos;
    private List<Usuario> todosTutores;
    private List<Usuario> usuariosMostrados;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rbNino.setSelected(true); // Rol por defecto
        cargarTodosLosUsuarios();
        cargarCombosVinculo();
        actualizarTablaVinculos();
    }

    // ===================== TAB CREAR USUARIO =====================

    private void cargarTodosLosUsuarios() {
        todosUsuarios = usuarioService.getTodosLosUsuarios()
                .stream()
                .filter(u -> u.getRol() != RolUsuario.ADMIN)
                .collect(Collectors.toList());
        usuariosMostrados = todosUsuarios;
        renderizarLista(usuariosMostrados);
    }

    private void renderizarLista(List<Usuario> usuarios) {
        listaUsuarios.setItems(FXCollections.observableArrayList(
                usuarios.stream().map(u ->
                        labelRol(u.getRol()) + "  " + u.getNombre()
                        + "   |  " + u.getEmail()
                        + "   |  " + labelRol(u.getRol())
                ).toList()
        ));
    }

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
        if (seleccionado == rbNino)     rol = RolUsuario.NINO;
        else if (seleccionado == rbPadre)    rol = RolUsuario.PADRE;
        else                            rol = RolUsuario.PROFESOR;

        try {
            Usuario nuevo = usuarioService.crearUsuario(nombre, email, pass, rol);
            mostrarMensaje("Usuario '" + nombre + "' creado correctamente.", true);
            txtNombre.clear(); txtEmail.clear(); txtPassword.clear();
            cargarTodosLosUsuarios();
            cargarCombosVinculo();
        } catch (RuntimeException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    @FXML
    private void onEliminarUsuario() {
        int idx = listaUsuarios.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            mostrarMensaje("Selecciona un usuario para eliminar.", false);
            return;
        }
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

    @FXML private void onRefrescar() { cargarTodosLosUsuarios(); mostrarMensaje("Lista actualizada.", true); }

    // Filtros de lista
    @FXML private void onVerTodos()      { usuariosMostrados = todosUsuarios; renderizarLista(usuariosMostrados); }
    @FXML private void onVerNinos()      { usuariosMostrados = filtrarPorRol(RolUsuario.NINO);     renderizarLista(usuariosMostrados); }
    @FXML private void onVerPadres()     { usuariosMostrados = filtrarPorRol(RolUsuario.PADRE);    renderizarLista(usuariosMostrados); }
    @FXML private void onVerProfesores() { usuariosMostrados = filtrarPorRol(RolUsuario.PROFESOR); renderizarLista(usuariosMostrados); }

    private List<Usuario> filtrarPorRol(RolUsuario rol) {
        return todosUsuarios.stream().filter(u -> u.getRol() == rol).toList();
    }

    private void mostrarMensaje(String texto, boolean exito) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:"
                + (exito ? "#5DBFA0" : "#C96070") + ";");
    }

    // ===================== TAB VINCULAR =====================

    private void cargarCombosVinculo() {
        todosTutores = usuarioService.getTodosLosTutores();
        todosNinos   = usuarioService.getTodosLosNinos();

        cmbTutor.setItems(FXCollections.observableArrayList(
                todosTutores.stream()
                        .map(t -> labelRol(t.getRol()) + " " + t.getNombre() + " (" + t.getEmail() + ")")
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

    @FXML
    private void onVincular() {
        int idxTutor = cmbTutor.getSelectionModel().getSelectedIndex();
        int idxNino  = cmbNino.getSelectionModel().getSelectedIndex();

        if (idxTutor < 0 || idxNino < 0) {
            lblMensajeVinculo.setText("Selecciona tutor y alumno.");
            lblMensajeVinculo.setStyle("-fx-text-fill:#C96070; -fx-font-weight:bold;");
            return;
        }

        Usuario tutor = todosTutores.get(idxTutor);
        Usuario nino  = todosNinos.get(idxNino);

        try {
            usuarioService.vincularNinoATutor(tutor.getId(), nino.getId());
            lblMensajeVinculo.setText(nino.getNombre() + " vinculado a " + tutor.getNombre());
            lblMensajeVinculo.setStyle("-fx-text-fill:#5DBFA0; -fx-font-weight:bold;");
            actualizarTablaVinculos();
        } catch (RuntimeException e) {
            lblMensajeVinculo.setText(e.getMessage());
            lblMensajeVinculo.setStyle("-fx-text-fill:#C96070; -fx-font-weight:bold;");
        }
    }

    private void actualizarTablaVinculos() {
        List<String> filas = new java.util.ArrayList<>();
        List<Usuario> tutores = usuarioService.getTodosLosTutores();
        for (Usuario tutor : tutores) {
            List<Usuario> ninos = usuarioService.getNinosDetutor(tutor.getId());
            if (!ninos.isEmpty()) {
                for (Usuario nino : ninos) {
                    filas.add(labelRol(tutor.getRol()) + " " + tutor.getNombre()
                            + "   ->   " + nino.getNombre());
                }
            }
        }
        if (filas.isEmpty()) filas.add("Aún no hay vínculos establecidos.");
        listaVinculos.setItems(FXCollections.observableArrayList(filas));
    }

    private String labelRol(RolUsuario rol) {
        return switch (rol) {
            case NINO     -> "Alumno";
            case PADRE    -> "Padre";
            case PROFESOR -> "Profesor";
            case ADMIN    -> "Admin";
        };
    }

    @FXML
    private void onCerrarSesion() {
        LoginController.usuarioActivo = null;
        stageManager.switchScene(FxmlView.LOGIN);
    }
}
