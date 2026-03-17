package com.apptism.controller;

import com.apptism.config.FxmlView;
import com.apptism.entity.Usuario;
import com.apptism.service.UsuarioService;
import com.apptism.ui.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class LoginController implements Initializable {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML private ImageView imgLogo;

    @Autowired private UsuarioService usuarioService;

    @Lazy
    @Autowired private StageManager stageManager;

    // Sesión global (se puede convertir en un @SessionScope bean)
    public static Usuario usuarioActivo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);
        cargarLogo();
    }

    /**
     * Carga el logo SVG desde resources/images/apptism_logo_v3.svg
     */
    private void cargarLogo() {
        try {
            String rutaLogo = getClass().getResource("/images/apptism_logo_v3.svg").toExternalForm();
            Image logo = new Image(rutaLogo, 260, 80, true, true);
            imgLogo.setImage(logo);
        } catch (Exception e) {
            System.err.println("Error cargando logo SVG: " + e.getMessage());
            // Fallback al PNG si el SVG no carga
            try {
                String rutaPng = getClass().getResource("/images/logo.png").toExternalForm();
                Image logoPng = new Image(rutaPng, 180, 60, true, true);
                imgLogo.setImage(logoPng);
            } catch (Exception ex) {
                System.err.println("Error cargando logo fallback: " + ex.getMessage());
                imgLogo.setVisible(false);
            }
        }
    }

    @FXML
    private void onLogin() {
        String email = txtEmail.getText().trim();
        String pass = txtPassword.getText();

        if (email.isBlank() || pass.isBlank()) {
            mostrarError("Por favor completa todos los campos");
            return;
        }

        Optional<Usuario> usuario = usuarioService.login(email, pass);
        if (usuario.isPresent()) {
            usuarioActivo = usuario.get();
            // Redirigir según rol
            if (usuarioActivo.getRol() == com.apptism.entity.RolUsuario.ADMIN) {
                stageManager.switchScene(FxmlView.ADMIN);
            } else {
                stageManager.switchScene(FxmlView.DASHBOARD);
            }
        } else {
            mostrarError("Email o contraseña incorrectos");
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}