package com.apptism.repository;

import com.apptism.entity.EstadoSolicitud;
import com.apptism.entity.SolicitudCanje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SolicitudCanjeRepository extends JpaRepository<SolicitudCanje, Long> {
    List<SolicitudCanje> findByRecompensaFamiliaIdOrderByFechaDesc(Long tutorId);
    long countByRecompensaFamiliaIdAndLeidaFalse(Long tutorId);
    void deleteByNinoId(Long ninoId);
    void deleteByRecompensaId(Long recompensaId);
}