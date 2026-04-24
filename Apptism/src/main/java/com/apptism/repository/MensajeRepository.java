package com.apptism.repository;

import com.apptism.entity.Mensaje;
import com.apptism.entity.TipoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByFechaAsc(
            Long emisor1, Long receptor1, Long emisor2, Long receptor2);

    List<Mensaje> findByReceptorIdAndTipoOrderByFechaDesc(Long receptorId, TipoMensaje tipo);
    void deleteByEmisorId(Long emisorId);
    void deleteByReceptorId(Long receptorId);

}
