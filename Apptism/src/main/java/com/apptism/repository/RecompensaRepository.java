package com.apptism.repository;

import com.apptism.entity.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Recompensa}.
 */

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {

    /**
     * Devuelve las recompensas activas creadas por un tutor.
     *
     * @param familiaId identificador del tutor
     * @return lista de recompensas con {@code activa = true}
     */

    List<Recompensa> findByFamiliaIdAndActivaTrue(Long familiaId);

    /**
     * Devuelve todas las recompensas de un tutor, activas o no.
     * Se usa al eliminar un usuario para localizar sus recompensas
     * antes de borrar las solicitudes de canje asociadas.
     *
     * @param familiaId identificador del tutor
     * @return lista de todas sus recompensas
     */

    List<Recompensa> findByFamiliaId(Long familiaId);

    /**
     * Elimina todas las recompensas creadas por un tutor.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param familiaId identificador del tutor
     */

    void deleteByFamiliaId(Long familiaId);
}
