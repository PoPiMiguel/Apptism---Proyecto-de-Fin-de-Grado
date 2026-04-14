// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: MensajeService.java
// ═══════════════════════════════════════════════════════════════════
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
 * Servicio de negocio para la gestión del sistema de mensajería con pictogramas.
 *
 * <p>Gestiona dos tipos de mensajes diferenciados por {@link TipoMensaje}:
 * <ul>
 *   <li>{@code CHAT}: mensajes de comunicación general entre niño y tutor.</li>
 *   <li>{@code EMOCION}: pictogramas enviados por el niño para expresar
 *       su estado emocional, visibles en el módulo de registro emocional.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Persiste un nuevo mensaje con pictograma entre dos usuarios.
     *
     * @param emisorId   identificador del usuario que envía el mensaje
     * @param receptorId identificador del usuario que recibe el mensaje
     * @param pictoUrl   URL de la imagen del pictograma seleccionado
     * @param texto      nombre o texto descriptivo del pictograma
     * @param tipo       tipo del mensaje: {@code CHAT} o {@code EMOCION}
     * @return entidad {@link Mensaje} persistida con su identificador generado
     * @throws RuntimeException si el emisor o el receptor no existen
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
     * Obtiene el historial de conversación de chat entre dos usuarios,
     * incluyendo los mensajes enviados en ambas direcciones, ordenados
     * cronológicamente de forma ascendente.
     *
     * @param userId1 identificador del primer participante
     * @param userId2 identificador del segundo participante
     * @return lista de mensajes de chat entre ambos usuarios, ordenados por fecha
     */
    public List<Mensaje> getConversacion(Long userId1, Long userId2) {
        return mensajeRepository
                .findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
                        userId1, userId2, userId2, userId1);
    }

    /**
     * Obtiene los mensajes de tipo {@code EMOCION} recibidos por un tutor,
     * ordenados de más reciente a más antiguo.
     *
     * @param tutorId identificador del tutor receptor
     * @return lista de mensajes emocionales recibidos por el tutor
     */
    public List<Mensaje> getEmocionesRecibidas(Long tutorId) {
        return mensajeRepository.findByReceptorIdAndTipoOrderByFechaDesc(
                tutorId, TipoMensaje.EMOCION);
    }
}