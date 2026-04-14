package com.apptism.repository;

import com.apptism.entity.Recompensa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
    List<Recompensa> findByFamiliaIdAndActivaTrue(Long familiaId);
}
