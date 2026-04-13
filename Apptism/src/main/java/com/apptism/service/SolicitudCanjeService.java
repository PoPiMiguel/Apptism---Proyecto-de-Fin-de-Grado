// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: SolicitudCanjeService.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.service;

import com.apptism.entity.*;
import com.apptism.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión del flujo de aprobación de solicitudes de canje.
 *
 * <p>Una solicitud de canje se genera cuando un niño solicita canjear una recompensa
 * desde el módulo de recompensas. El tutor puede entonces aprobarla (descontando
 * los puntos definitivamente) o rechazarla desde el módulo de solicitudes de canje.
 */
@Service
@RequiredArgsConstructor
public class SolicitudCanjeService {

    private final SolicitudCanjeRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final RecompensaRepository recompensaRepo;

    /**
     * Crea una nueva solicitud de canje verificando que el niño tenga
     * puntos suficientes para la recompensa indicada.
     *
     * @param ninoId       identificador del niño solicitante
     * @param recompensaId identificador de la recompensa solicitada
     * @return entidad {@link SolicitudCanje} persistida en estado {@code PENDIENTE}
     * @throws RuntimeException si el niño o la recompensa no existen,
     *                          o si los puntos son insuficientes
     */
    @Transactional
    public SolicitudCanje solicitarCanje(Long ninoId, Long recompensaId) {
        Usuario nino = usuarioRepo.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        Recompensa recompensa = recompensaRepo.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("Recompensa no encontrada"));

        if (nino.getPuntosAcumulados() < recompensa.getPuntosNecesarios())
            throw new RuntimeException("Puntos insuficientes");

        return solicitudRepo.save(SolicitudCanje.builder()
                .nino(nino)
                .recompensa(recompensa)
                .estado(EstadoSolicitud.PENDIENTE)
                .leida(false)
                .build());
    }

    /**
     * Obtiene todas las solicitudes de canje asociadas a las recompensas
     * creadas por un tutor, ordenadas de más reciente a más antigua.
     *
     * <p>Las relaciones {@code nino} y {@code recompensa} son LAZY; se
     * inicializan explícitamente dentro de la transacción para evitar
     * {@code LazyInitializationException} al acceder a ellas desde la UI.
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
     * Se usa para mostrar el badge de notificación en el dashboard.
     *
     * @param tutorId identificador del tutor
     * @return número de solicitudes con {@code leida = false}
     */
    public long contarNoLeidas(Long tutorId) {
        return solicitudRepo.countByRecompensaFamiliaIdAndLeidaFalse(tutorId);
    }

    /**
     * Aprueba una solicitud de canje, descuenta los puntos del niño
     * y la marca como leída.
     *
     * @param solicitudId identificador de la solicitud a aprobar
     * @throws RuntimeException si no existe ninguna solicitud con ese identificador
     */
    @Transactional
    public void aprobar(Long solicitudId) {
        SolicitudCanje s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.APROBADA);
        s.setLeida(true);
        // Descontar puntos definitivamente al aprobar
        Usuario nino = s.getNino();
        nino.setPuntosAcumulados(
                nino.getPuntosAcumulados() - s.getRecompensa().getPuntosNecesarios());
        usuarioRepo.save(nino);
        solicitudRepo.save(s);
    }

    /**
     * Rechaza una solicitud de canje y la marca como leída.
     *
     * @param solicitudId identificador de la solicitud a rechazar
     * @throws RuntimeException si no existe ninguna solicitud con ese identificador
     */
    @Transactional
    public void rechazar(Long solicitudId) {
        SolicitudCanje s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.RECHAZADA);
        s.setLeida(true);
        solicitudRepo.save(s);
    }

    /**
     * Marca como leídas todas las solicitudes de canje de un tutor.
     * Se invoca al entrar en el módulo de solicitudes para limpiar
     * el badge de notificación del dashboard.
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