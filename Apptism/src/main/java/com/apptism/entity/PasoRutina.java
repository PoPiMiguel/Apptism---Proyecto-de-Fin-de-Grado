package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pasos_rutina")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PasoRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion;

    private String pictogramaUrl;

    private Integer orden;

    @Builder.Default
    private boolean completado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;
}
