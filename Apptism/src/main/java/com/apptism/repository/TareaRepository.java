package com.apptism.repository;

import com.apptism.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByNinoId(Long ninoId);
    List<Tarea> findByNinoIdAndCompletada(Long ninoId, boolean completada);
    List<Tarea> findByCreadorId(Long creadorId);
    void deleteByCreadorId(Long creadorId);
    void deleteByNinoId(Long ninoId);
}

