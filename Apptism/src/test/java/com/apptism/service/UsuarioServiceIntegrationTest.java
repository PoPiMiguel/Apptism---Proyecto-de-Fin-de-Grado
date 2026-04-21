package com.apptism.service;

import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UsuarioServiceIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void crearUsuario_seGuardaEnBaseDeDatos() {
        Usuario creado = usuarioService.crearUsuario(
                "Test", "test@apptism.com", "1234", RolUsuario.NINO);

        assertNotNull(creado.getId());
        assertEquals("Test", creado.getNombre());
        assertEquals(RolUsuario.NINO, creado.getRol());
    }
}