// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: LoginController.java
// ═══════════════════════════════════════════════════════════════════
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
 * <p>Gestiona la autenticación del usuario mediante email y contraseña.
 * Tras una autenticación exitosa, redirige al panel de administración si
 * el usuario tiene rol {@code ADMIN}, o al dashboard general en cualquier
 * otro caso.
 *
 * <p>Almacena el usuario autenticado en la variable estática
 * {@link #usuarioActivo}, que actúa como sesión compartida entre
 * todos los controladores mientras la aplicación está en uso.
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
     * Usuario autenticado actualmente en la sesión.
     * Es accedido por el resto de controladores para obtener
     * el contexto de usuario sin necesidad de parámetros entre vistas.
     */
    public static Usuario usuarioActivo;

    /**
     * Inicializa la pantalla de login: oculta el label de error
     * y carga el logotipo de la aplicación.
     *
     * @param url            URL de localización del archivo FXML (no se usa)
     * @param rb             ResourceBundle de internacionalización (no se usa)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);
        cargarLogo();
    }

    /**
     * Carga la imagen del logotipo desde el classpath y la muestra en
     * la vista. Si la imagen no se encuentra, oculta el componente.
     */
    private void cargarLogo() {
        try {
            String rutaLogo = getClass()
                    .getResource("/images/apptism_icon_512.png").toExternalForm();
            Image logo = new Image(rutaLogo, 260, 80, true, true);
            imgLogo.setImage(logo);
        } catch (Exception e) {
            System.err.println("Error cargando logo: " + e.getMessage());
            imgLogo.setVisible(false);
        }
    }

    /**
     * Maneja el evento del botón de inicio de sesión.
     *
     * <p>Valida que los campos no estén vacíos, delega la autenticación
     * en {@link UsuarioService#login(String, String)} y navega a la
     * vista correspondiente al rol del usuario. Si las credenciales
     * son incorrectas, muestra un mensaje de error.
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
     * Muestra un mensaje de error bajo el formulario de login.
     *
     * @param mensaje texto descriptivo del error a mostrar
     */
    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}