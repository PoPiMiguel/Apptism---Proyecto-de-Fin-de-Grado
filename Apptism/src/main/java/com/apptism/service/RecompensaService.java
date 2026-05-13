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
 * Servicio que gestiona las recompensas y los canjes.
 *
 * <p>Las recompensas son creadas por los tutores y pueden ser canjeadas por
 * los niños usando los puntos acumulados al completar tareas. Al canjear
 * se genera automáticamente una {@link SolicitudCanje} para que el tutor
 * tenga constancia del canje en su historial.</p>
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
     * @param familiaId identificador del tutor
     * @return lista de recompensas con {@code activa = true}
     */

    public List<Recompensa> getRecompensasDisponibles(Long familiaId) {
        return recompensaRepository.findByFamiliaIdAndActivaTrue(familiaId);
    }

    /**
     * Crea una recompensa nueva asociada al tutor indicado.
     *
     * @param descripcion texto descriptivo de la recompensa
     * @param puntos      puntos necesarios para canjearla
     * @param familiaId   identificador del tutor que la crea
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
     * <p>Comprueba que el niño tenga puntos suficientes. Si los tiene,
     * descuenta los puntos y genera una {@link SolicitudCanje} en estado
     * {@code PENDIENTE}. Si no, no realiza ninguna acción.</p>
     *
     * @param ninoId       identificador del niño que quiere canjear
     * @param recompensaId identificador de la recompensa a canjear
     * @return {@code true} si el canje se realizó correctamente;
     *         {@code false} si el niño no tenía puntos suficientes
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