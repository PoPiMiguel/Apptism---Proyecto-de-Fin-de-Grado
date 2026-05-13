package com.apptism.service;

import com.apptism.entity.*;
import com.apptism.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Servicio que gestiona el historial de canjes de recompensas.
 *
 * <p>Obtiene las solicitudes generadas cuando un niño canjea una recompensa,
 * cuenta las no leídas para el badge del dashboard y las marca como leídas
 * al entrar al módulo de canjes.</p>
 */

@Service
@RequiredArgsConstructor
public class SolicitudCanjeService {

    private final SolicitudCanjeRepository solicitudRepo;

    /**
     * Devuelve todas las solicitudes de canje de las recompensas de un tutor,
     * de la más reciente a la más antigua.
     *
     * <p>Inicializa las relaciones LAZY ({@code nino} y {@code recompensa})
     * dentro de la transacción para que no fallen al usarlas fuera del contexto
     * de persistencia.</p>
     *
     * @param tutorId identificador del tutor
     * @return lista de solicitudes con sus datos ya cargados
     */

    @Transactional(readOnly = true)
    public List<SolicitudCanje> getSolicitudesTutor(Long tutorId) {
        List<SolicitudCanje> solicitudes =
                solicitudRepo.findByRecompensaFamiliaIdOrderByFechaDesc(tutorId);
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
     * Cuenta las solicitudes no leídas del tutor.
     * Se usa para mostrar el número en el badge del dashboard.
     *
     * @param tutorId identificador del tutor
     * @return número de solicitudes con {@code leida = false}
     */

    public long contarNoLeidas(Long tutorId) {
        return solicitudRepo.countByRecompensaFamiliaIdAndLeidaFalse(tutorId);
    }

    /**
     * Marca como leídas todas las solicitudes del tutor.
     * Se llama al entrar al módulo de canjes para limpiar el badge del dashboard.
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