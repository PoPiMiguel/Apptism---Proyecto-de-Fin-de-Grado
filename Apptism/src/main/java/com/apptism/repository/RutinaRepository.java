package com.apptism.repository;

import com.apptism.entity.Rutina;
import com.apptism.entity.ZonaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Long> {
    List<Rutina> findByNinoIdOrderByOrdenAsc(Long ninoId);
    List<Rutina> findByNinoIdAndZonaHorariaOrderByOrdenAsc(Long ninoId, ZonaHoraria zona);
}
