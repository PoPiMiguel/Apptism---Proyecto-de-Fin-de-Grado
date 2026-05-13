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

/**
 * Servicio que gestiona los mensajes con pictogramas entre niños y tutores.
 *
 * <p>Hay dos tipos de mensaje según {@link TipoMensaje}:</p>
 * <ul>
 *   <li>{@link TipoMensaje#CHAT} – conversación general bidireccional.</li>
 *   <li>{@link TipoMensaje#EMOCION} – el niño envía cómo se siente,
 *       visible en el registro emocional del tutor.</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Guarda un mensaje nuevo con su pictograma en la base de datos.
     *
     * @param emisorId   identificador del usuario que envía
     * @param receptorId identificador del usuario que recibe
     * @param pictoUrl   URL de la imagen del pictograma
     * @param texto      nombre o etiqueta del pictograma
     * @param tipo       {@link TipoMensaje#CHAT} o {@link TipoMensaje#EMOCION}
     * @return el mensaje guardado
     */

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

    /**
     * Devuelve el historial de mensajes de tipo {@link TipoMensaje#CHAT} entre
     * dos usuarios, con los mensajes de ambas direcciones ordenados cronológicamente.
     *
     * @param userId1 identificador del primer participante
     * @param userId2 identificador del segundo participante
     * @return lista de mensajes ordenados por fecha ascendente
     */

    public List<Mensaje> getConversacion(Long userId1, Long userId2) {
        return mensajeRepository
                .findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
                        userId1, userId2, userId2, userId1);
    }

    /**
     * Devuelve los mensajes emocionales recibidos por un tutor,
     * del más reciente al más antiguo.
     *
     * @param tutorId identificador del tutor receptor
     * @return lista de mensajes de tipo {@link TipoMensaje#EMOCION} recibidos
     */

    public List<Mensaje> getEmocionesRecibidas(Long tutorId) {
        return mensajeRepository.findByReceptorIdAndTipoOrderByFechaDesc(
                tutorId, TipoMensaje.EMOCION);
    }
}