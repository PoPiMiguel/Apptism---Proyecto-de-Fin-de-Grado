package com.apptism.service;

import com.apptism.entity.Recompensa;
import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.repository.RecompensaRepository;
import com.apptism.repository.SolicitudCanjeRepository;
import com.apptism.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecompensaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RecompensaRepository recompensaRepository;

    @Mock
    private SolicitudCanjeRepository solicitudCanjeRepository;

    @InjectMocks
    private RecompensaService recompensaService;

    private Usuario nino;
    private Recompensa recompensa;

    @BeforeEach
    void setUp() {
        nino = Usuario.builder()
                .id(1L)
                .nombre("Carlos")
                .email("carlos@test.com")
                .password("1234")
                .rol(RolUsuario.NINO)
                .puntosAcumulados(100)
                .build();

        recompensa = Recompensa.builder()
                .id(1L)
                .descripcion("Tiempo libre")
                .puntosNecesarios(50)
                .activa(true)
                .build();
    }

    @Test
    void canjear_conPuntosSuficientes_devuelveTrue() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(nino));
        when(recompensaRepository.findById(1L)).thenReturn(Optional.of(recompensa));

        boolean resultado = recompensaService.canjearRecompensa(1L, 1L);

        assertTrue(resultado);
        assertEquals(50, nino.getPuntosAcumulados());
        verify(usuarioRepository).save(nino);
        verify(solicitudCanjeRepository).save(any());
    }

    @Test
    void canjear_conPuntosInsuficientes_devuelveFalse() {
        nino.setPuntosAcumulados(10);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(nino));
        when(recompensaRepository.findById(1L)).thenReturn(Optional.of(recompensa));

        boolean resultado = recompensaService.canjearRecompensa(1L, 1L);

        assertFalse(resultado);
        assertEquals(10, nino.getPuntosAcumulados());
        verify(usuarioRepository, never()).save(any());
    }
}