package com.apptism.repository;

import com.apptism.entity.Rutina;
import com.apptism.entity.ZonaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Rutina}.
 */

@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    /**
     * Devuelve todas las rutinas de un niño ordenadas por el campo {@code orden}.
     *
     * @param ninoId identificador del niño
     * @return lista de rutinas ordenadas ascendentemente por orden
     */

    List<Rutina> findByNinoIdOrderByOrdenAsc(Long ninoId);

    /**
     * Devuelve las rutinas de un niño filtradas por franja horaria y ordenadas por {@code orden}.
     *
     * @param ninoId identificador del niño
     * @param zona   franja horaria a filtrar
     * @return lista de rutinas de esa franja, ordenadas ascendentemente por orden
     */

    List<Rutina> findByNinoIdAndZonaHorariaOrderByOrdenAsc(Long ninoId, ZonaHoraria zona);

    /**
     * Elimina todas las rutinas creadas por un tutor concreto.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param creadorId identificador del tutor creador
     */

    void deleteByCreadorId(Long creadorId);

    /**
     * Elimina todas las rutinas asignadas a un niño concreto.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param ninoId identificador del niño
     */

    void deleteByNinoId(Long ninoId);
}
