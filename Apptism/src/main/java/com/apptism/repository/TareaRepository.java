package com.apptism.repository;

import com.apptism.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Tarea}.
 */

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    /**
     * Devuelve todas las tareas asignadas a un niño.
     *
     * @param ninoId identificador del niño
     * @return lista de sus tareas, completadas o no
     */

    List<Tarea> findByNinoId(Long ninoId);

    /**
     * Devuelve todas las tareas creadas por un tutor.
     *
     * @param creadorId identificador del tutor
     * @return lista de tareas creadas por ese tutor
     */

    List<Tarea> findByCreadorId(Long creadorId);

    /**
     * Elimina todas las tareas creadas por un tutor concreto.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param creadorId identificador del tutor creador
     */

    void deleteByCreadorId(Long creadorId);

    /**
     * Elimina todas las tareas asignadas a un niño concreto.
     * Se usa al eliminar un usuario para limpiar sus datos asociados.
     *
     * @param ninoId identificador del niño
     */

    void deleteByNinoId(Long ninoId);
}

