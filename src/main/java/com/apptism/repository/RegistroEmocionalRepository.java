package com.apptism.repository;

import com.apptism.entity.Emocion;
import com.apptism.entity.RegistroEmocional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroEmocionalRepository extends JpaRepository<RegistroEmocional, Long> {
    List<RegistroEmocional> findByNinoIdOrderByFechaDesc(Long ninoId);
    List<RegistroEmocional> findByNinoIdAndFechaAfter(Long ninoId, LocalDateTime desde);
    long countByNinoIdAndEmocionAndFechaAfter(Long ninoId, Emocion emocion, LocalDateTime desde);
}
