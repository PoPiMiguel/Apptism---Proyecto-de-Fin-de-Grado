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

/**
 * Controlador de la pantalla de inicio de sesión.
 *
 * <p>Gestiona la autenticación por email y contraseña. Si el usuario tiene
 * rol {@code ADMIN}, lo dirige al panel de administración; en cualquier otro
 * caso, al dashboard correspondiente a su rol.</p>
 *
 * <p>El usuario autenticado se guarda en {@link #usuarioActivo}, campo
 * estático que actúa como sesión compartida y al que acceden el resto
 * de controladores para conocer quién está usando la aplicación.</p>
 */

@Component
public class LoginController implements Initializable {

    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Button        btnLogin;
    @FXML private Label         lblError;
    @FXML private ImageView     imgLogo;

    @Autowired private UsuarioService usuarioService;

    @Lazy
    @Autowired private StageManager stageManager;

    /**
     * Usuario que ha iniciado sesión.
     * Lo leen el resto de controladores para saber quién está activo en cada momento.
     * Se pone a {@code null} al cerrar sesión.
     */

    public static Usuario usuarioActivo;

    /**
     * Prepara la pantalla: oculta el mensaje de error y carga el logotipo.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);
        cargarLogo();
    }

    /**
     * Carga el logotipo desde los recursos y lo muestra en la pantalla.
     * Si la imagen no se encuentra, oculta el componente sin lanzar ningún error.
     */

    private void cargarLogo() {
        try {
            String rutaLogo = getClass()
                    .getResource("/images/apptism_icon.png").toExternalForm();
            Image logo = new Image(rutaLogo, 260, 80, true, true);
            imgLogo.setImage(logo);
        } catch (Exception e) {
            System.err.println("Error cargando logo: " + e.getMessage());
            imgLogo.setVisible(false);
        }
    }

    /**
     * Se ejecuta cuando el usuario pulsa el botón de acceso.
     *
     * <p>Valida que los campos no estén vacíos, comprueba las credenciales
     * y navega a la pantalla correspondiente según el rol. Si algo falla,
     * muestra el mensaje de error bajo el formulario.</p>
     */

    @FXML
    private void onLogin() {
        String email = txtEmail.getText().trim();
        String pass  = txtPassword.getText();

        if (email.isBlank() || pass.isBlank()) {
            mostrarError("Por favor completa todos los campos");
            return;
        }

        Optional<Usuario> usuario = usuarioService.login(email, pass);
        if (usuario.isPresent()) {
            usuarioActivo = usuario.get();
            if (usuarioActivo.getRol() == com.apptism.entity.RolUsuario.ADMIN) {
                stageManager.switchScene(FxmlView.ADMIN);
            } else {
                stageManager.switchScene(FxmlView.DASHBOARD);
            }
        } else {
            mostrarError("Email o contraseña incorrectos");
        }
    }

    /**
     * Muestra un mensaje de error bajo el formulario de acceso.
     *
     * @param mensaje texto a mostrar
     */

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}