package com.apptism.service;

import com.apptism.entity.*;
import com.apptism.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudCanjeService {

    private final SolicitudCanjeRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final RecompensaRepository recompensaRepo;

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

    @Transactional(readOnly = true)
    public List<SolicitudCanje> getSolicitudesTutor(Long tutorId) {
        List<SolicitudCanje> solicitudes =
                solicitudRepo.findByRecompensaFamiliaIdOrderByFechaDesc(tutorId);
        // Forzar inicialización de las relaciones LAZY dentro de la transacción
        solicitudes.forEach(s -> {
            if (s.getNino() != null)       s.getNino().getNombre();
            if (s.getRecompensa() != null) {
                s.getRecompensa().getDescripcion();
                s.getRecompensa().getPuntosNecesarios();
            }
        });
        return solicitudes;
    }

    public long contarNoLeidas(Long tutorId) {
        return solicitudRepo.countByRecompensaFamiliaIdAndLeidaFalse(tutorId);
    }

    @Transactional
    public void aprobar(Long solicitudId) {
        SolicitudCanje s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.APROBADA);
        s.setLeida(true);
        // Descontar puntos al niño
        Usuario nino = s.getNino();
        nino.setPuntosAcumulados(
                nino.getPuntosAcumulados() - s.getRecompensa().getPuntosNecesarios());
        usuarioRepo.save(nino);
        solicitudRepo.save(s);
    }

    @Transactional
    public void rechazar(Long solicitudId) {
        SolicitudCanje s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.RECHAZADA);
        s.setLeida(true);
        solicitudRepo.save(s);
    }

    @Transactional
    public void marcarTodasLeidas(Long tutorId) {
        getSolicitudesTutor(tutorId).forEach(s -> {
            s.setLeida(true);
            solicitudRepo.save(s);
        });
    }
}
