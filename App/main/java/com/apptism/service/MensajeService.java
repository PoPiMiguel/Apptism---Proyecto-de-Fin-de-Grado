package com.apptism.service;

import com.apptism.entity.Mensaje;
import com.apptism.entity.TipoMensaje;
import com.apptism.entity.Usuario;
import com.apptism.repository.MensajeRepository;
import com.apptism.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Mensaje enviarMensaje(Long emisorId, Long receptorId,
                                 String pictoUrl, String texto, TipoMensaje tipo) {
        Usuario emisor   = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RuntimeException("Receptor no encontrado"));

        return mensajeRepository.save(Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .pictogramaUrl(pictoUrl)
                .textoPictograma(texto)
                .tipo(tipo)
                .leido(false)
                .build());
    }

    public List<Mensaje> getConversacion(Long userId1, Long userId2) {
        return mensajeRepository
                .findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
                        userId1, userId2, userId2, userId1);
    }

    public List<Mensaje> getEmocionesRecibidas(Long tutorId) {
        return mensajeRepository.findByReceptorIdAndTipoOrderByFechaDesc(
                tutorId, TipoMensaje.EMOCION);
    }

    @Transactional
    public void marcarLeidos(Long receptorId) {
        List<Mensaje> noLeidos = mensajeRepository
                .findByReceptorIdAndTipoOrderByFechaDesc(receptorId, TipoMensaje.CHAT);
        noLeidos.forEach(m -> m.setLeido(true));
        mensajeRepository.saveAll(noLeidos);
    }

    public long contarNoLeidos(Long receptorId) {
        return mensajeRepository.countByReceptorIdAndLeidoFalse(receptorId);
    }
}
