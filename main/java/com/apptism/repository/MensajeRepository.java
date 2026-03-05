package com.apptism.repository;

import com.apptism.entity.Mensaje;
import com.apptism.entity.TipoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Chat entre dos usuarios (bidireccional)
    List<Mensaje> findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
            Long emisor1, Long receptor1, Long emisor2, Long receptor2);

    // Emociones enviadas por un niño (tipo EMOCION)
    List<Mensaje> findByReceptorIdAndTipoOrderByFechaDesc(Long receptorId, TipoMensaje tipo);

    // Mensajes no leídos
    long countByReceptorIdAndLeidoFalse(Long receptorId);
}
