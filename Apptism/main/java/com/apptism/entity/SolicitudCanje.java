package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_canje")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudCanje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Builder.Default
    private boolean leida = false;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recompensa_id", nullable = false)
    private Recompensa recompensa;
}
