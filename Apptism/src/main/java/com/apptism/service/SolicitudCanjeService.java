package com.apptism.service;

import com.apptism.entity.*;
import com.apptism.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la consulta del historial de canjes.
 *
 * Gestiona la obtención de las solicitudes de canje generadas cuando
 * un niño canjea una recompensa, y su marcado como leídas.
 */
@Service
@RequiredArgsConstructor
public class SolicitudCanjeService {

    private final SolicitudCanjeRepository solicitudRepo;
    private final UsuarioRepository        usuarioRepo;
    private final RecompensaRepository     recompensaRepo;

    /**
     * Obtiene todas las solicitudes de canje asociadas a las recompensas
     * creadas por un tutor, ordenadas de más reciente a más antigua.
     *
     * @param tutorId identificador del tutor
     * @return lista de solicitudes con sus relaciones inicializadas
     */
    @Transactional(readOnly = true)
    public List<SolicitudCanje> getSolicitudesTutor(Long tutorId) {
        List<SolicitudCanje> solicitudes =
                solicitudRepo.findByRecompensaFamiliaIdOrderByFechaDesc(tutorId);
        // Inicialización explícita de relaciones LAZY dentro de la transacción
        solicitudes.forEach(s -> {
            if (s.getNino() != null)       s.getNino().getNombre();
            if (s.getRecompensa() != null) {
                s.getRecompensa().getDescripcion();
                s.getRecompensa().getPuntosNecesarios();
            }
        });
        return solicitudes;
    }

    /**
     * Cuenta las solicitudes no leídas de las recompensas de un tutor.
     * Se usa para el badge de notificación en el dashboard.
     *
     * @param tutorId identificador del tutor
     * @return número de solicitudes con leida = false
     */
    public long contarNoLeidas(Long tutorId) {
        return solicitudRepo.countByRecompensaFamiliaIdAndLeidaFalse(tutorId);
    }

    /**
     * Marca como leídas todas las solicitudes de canje de un tutor.
     * Se invoca al entrar en el módulo para limpiar el badge del dashboard.
     *
     * @param tutorId identificador del tutor
     */
    @Transactional
    public void marcarTodasLeidas(Long tutorId) {
        getSolicitudesTutor(tutorId).forEach(s -> {
            s.setLeida(true);
            solicitudRepo.save(s);
        });
    }
}