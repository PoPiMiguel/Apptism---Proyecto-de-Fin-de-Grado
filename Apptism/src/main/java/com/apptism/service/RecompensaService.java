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
     * Devuelve las recompensas activas creadas por un tutor concreto.
     *
     * @param familiaId identificador del tutor (denominado "familia" en el modelo)
     * @return lista de recompensas activas del tutor
     */
    public List<Recompensa> getRecompensasDisponibles(Long familiaId) {
        return recompensaRepository.findByFamiliaIdAndActivaTrue(familiaId);
    }

    /**
     * Crea una nueva recompensa asociada al tutor indicado.
     *
     * @param descripcion texto descriptivo de la recompensa
     * @param puntos      coste en puntos para canjearla
     * @param familiaId   identificador del tutor que la crea
     * @return entidad {@link Recompensa} persistida
     * @throws RuntimeException si no existe ningún usuario con ese identificador
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
     * Procesa la solicitud de canje de una recompensa por parte de un niño.
     *
     * <p>Comprueba que el niño tenga puntos suficientes. Si es así, descuenta
     * los puntos de su saldo, persiste una {@link SolicitudCanje} en estado
     * {@code PENDIENTE} y devuelve {@code true}. Si no tiene puntos suficientes,
     * devuelve {@code false} sin modificar nada.
     *
     * @param ninoId       identificador del niño que solicita el canje
     * @param recompensaId identificador de la recompensa a canjear
     * @return {@code true} si el canje se procesó correctamente;
     *         {@code false} si el niño no tiene puntos suficientes
     * @throws RuntimeException si el niño o la recompensa no existen
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