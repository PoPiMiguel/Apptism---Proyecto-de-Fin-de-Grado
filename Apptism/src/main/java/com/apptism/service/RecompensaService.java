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

@Service
@RequiredArgsConstructor
public class RecompensaService {

    private final RecompensaRepository recompensaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCanjeRepository solicitudCanjeRepository;

    /** Para el tutor: recompensas que él ha creado */
    public List<Recompensa> getRecompensasDisponibles(Long familiaId) {
        return recompensaRepository.findByFamiliaIdAndActivaTrue(familiaId);
    }

    /** Para el niño: TODAS las recompensas activas (de cualquier tutor) */
    public List<Recompensa> getTodasRecompensasActivas() {
        return recompensaRepository.findByActivaTrue();
    }

    public List<Recompensa> getRecompensasAccesibles(int puntos) {
        return recompensaRepository.findByPuntosNecesariosLessThanEqual(puntos);
    }

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

    @Transactional
    public boolean canjearRecompensa(Long ninoId, Long recompensaId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        Recompensa recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RuntimeException("Recompensa no encontrada"));

        if (nino.getPuntosAcumulados() >= recompensa.getPuntosNecesarios()) {
            // Restar puntos al niño
            nino.setPuntosAcumulados(
                    nino.getPuntosAcumulados() - recompensa.getPuntosNecesarios()
            );
            usuarioRepository.save(nino);

            // IMPORTANTE: Registrar la solicitud de canje para que el tutor pueda verla
            SolicitudCanje solicitud = SolicitudCanje.builder()
                    .nino(nino)
                    .recompensa(recompensa)
                    .build();
            solicitudCanjeRepository.save(solicitud);

            return true;
        }
        return false; // Puntos insuficientes
    }

    @Transactional
    public void eliminarRecompensa(Long recompensaId) {
        recompensaRepository.deleteById(recompensaId);
    }
}