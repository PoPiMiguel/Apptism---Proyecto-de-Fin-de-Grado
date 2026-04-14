package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recompensas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Integer puntosNecesarios;

    @Builder.Default
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private Usuario familia;
}
