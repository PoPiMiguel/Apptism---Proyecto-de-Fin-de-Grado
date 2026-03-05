package com.apptism.repository;

import com.apptism.entity.PasoRutina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PasoRutinaRepository extends JpaRepository<PasoRutina, Long> {
    List<PasoRutina> findByRutinaIdOrderByOrdenAsc(Long rutinaId);
}
