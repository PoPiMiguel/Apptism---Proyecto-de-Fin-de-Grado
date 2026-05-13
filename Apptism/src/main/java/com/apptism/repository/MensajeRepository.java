package com.apptism.repository;

import com.apptism.entity.Mensaje;
import com.apptism.entity.TipoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Mensaje}.
 */

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /**
     * Devuelve el historial de mensajes entre dos usuarios en ambas direcciones,
     * ordenado cronológicamente de más antiguo a más reciente.
     *
     * <p>La firma larga es la forma en que Spring Data JPA expresa la condición
     * {@code (emisor=A AND receptor=B) OR (emisor=B AND receptor=A)}.</p>
     *
     * @param emisor1   identificador del primer participante como emisor
     * @param receptor1 identificador del segundo participante como receptor
     * @param emisor2   identificador del segundo participante como emisor
     * @param receptor2 identificador del primer participante como receptor
     * @return lista de mensajes entre ambos usuarios, ordenada por fecha ascendente
     */

    List<Mensaje> findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
            Long emisor1, Long receptor1, Long emisor2, Long receptor2);

    /**
     * Devuelve los mensajes de un tipo concreto recibidos por un usuario,
     * del más reciente al más antiguo.
     * Se usa para obtener las emociones recibidas por un tutor.
     *
     * @param receptorId identificador del receptor
     * @param tipo       tipo de mensaje a filtrar ({@link TipoMensaje#EMOCION} o {@link TipoMensaje#CHAT})
     * @return lista de mensajes de ese tipo, ordenada por fecha descendente
     */

    List<Mensaje> findByReceptorIdAndTipoOrderByFechaDesc(Long receptorId, TipoMensaje tipo);

    /**
     * Elimina todos los mensajes enviados por un usuario.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param emisorId identificador del emisor
     */

    void deleteByEmisorId(Long emisorId);

    /**
     * Elimina todos los mensajes recibidos por un usuario.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param receptorId identificador del receptor
     */

    void deleteByReceptorId(Long receptorId);

}
