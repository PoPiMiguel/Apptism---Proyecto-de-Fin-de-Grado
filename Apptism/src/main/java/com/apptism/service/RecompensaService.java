// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: RecompensaService.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.service;

import com.apptism.entity.Recompensa;
import com.apptism.entity.SolicitudCanje;
import com.apptism.entity.Usuario;
import com.apptism.repository.RecompensaRepository;
import com.apptism.repository.SolicitudCanjeRepository;
import com.apptism.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de recompensas y solicitudes de canje.
 *
 * <p>Las recompensas son creadas por los tutores y pueden ser canjeadas por
 * los niños usando los puntos acumulados al completar tareas. Al canjear una
 * recompensa se genera automáticamente una {@link SolicitudCanje} para que
 * el tutor pueda aprobarla o rechazarla.
 */
@Service
@RequiredArgsConstructor
public class RecompensaService {

    private final RecompensaRepository recompensaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCanjeRepository solicitudCanjeRepository;

    /**
     * Devuelve las recompensas activas creadas por un tutor.
     *
     * @param familiaId el identificador del tutor (llamado "familia" en el modelo)
     * @return lista de recompensas activas del tutor
     */
    public List<Recompensa> getRecompensasDisponibles(Long familiaId) {
        return recompensaRepository.findByFamiliaIdAndActivaTrue(familiaId);
    }

    /**
     * Crea una recompensa nueva asociada al tutor indicado.
     *
     * @param descripcion descripción de la recompensa
     * @param puntos      cuántos puntos cuesta canjearla
     * @param familiaId   el identificador del tutor que la crea
     * @return la recompensa guardada en base de datos
     */
    @Transactional
    public Recompensa crearRecompensa(String descripcion, int puntos, Long familiaId) {
        Usuario familia = usuarioRepository.findById(familiaId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Recompensa recompensa = Recompensa.builder()
                .descripcion(descripcion)
                .puntosNecesarios(puntos)
                .activa(true)
                .familia(familia)
                .build();

        return recompensaRepository.save(recompensa);
    }

    /**
     * Procesa el canje de una recompensa por parte de un niño.
     *
     * Comprueba que tenga puntos suficientes. Si los tiene, descuenta los puntos
     * y crea una solicitud de canje en estado PENDIENTE. Si no los tiene, no hace nada.
     *
     * @param ninoId       el identificador del niño que quiere canjear
     * @param recompensaId el identificador de la recompensa que quiere canjear
     * @return {@code true} si el canje se hizo bien; {@code false} si no tenía suficientes puntos
     */
    @Transactional
    public boolean canjearRecompensa(Long ninoId, Long recompensaId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("Recompensa no encontrada"));

        if (nino.getPuntosAcumulados() >= recompensa.getPuntosNecesarios()) {
            nino.setPuntosAcumulados(
                    nino.getPuntosAcumulados() - recompensa.getPuntosNecesarios());
            usuarioRepository.save(nino);

            SolicitudCanje solicitud = SolicitudCanje.builder()
                    .nino(nino)
                    .recompensa(recompensa)
                    .build();
            solicitudCanjeRepository.save(solicitud);

            return true;
        }
        return false;
    }
}