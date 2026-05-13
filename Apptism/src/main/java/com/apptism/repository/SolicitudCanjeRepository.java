package com.apptism.repository;

import com.apptism.entity.SolicitudCanje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link SolicitudCanje}.
 */

@Repository
public interface SolicitudCanjeRepository extends JpaRepository<SolicitudCanje, Long> {

    /**
     * Devuelve todas las solicitudes de canje de las recompensas de un tutor,
     * de la más reciente a la más antigua.
     *
     * @param tutorId identificador del tutor propietario de las recompensas
     * @return lista de solicitudes ordenada por fecha descendente
     */

    List<SolicitudCanje> findByRecompensaFamiliaIdOrderByFechaDesc(Long tutorId);

    /**
     * Cuenta las solicitudes no leídas de las recompensas de un tutor.
     * Se usa para mostrar el badge de notificaciones en el dashboard.
     *
     * @param tutorId identificador del tutor
     * @return número de solicitudes con {@code leida = false}
     */

    long countByRecompensaFamiliaIdAndLeidaFalse(Long tutorId);

    /**
     * Elimina todas las solicitudes realizadas por un niño.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param ninoId identificador del niño
     */

    void deleteByNinoId(Long ninoId);

    /**
     * Elimina todas las solicitudes asociadas a una recompensa concreta.
     * Se usa al eliminar una recompensa antes de borrarla de la base de datos.
     *
     * @param recompensaId identificador de la recompensa
     */

    void deleteByRecompensaId(Long recompensaId);
}