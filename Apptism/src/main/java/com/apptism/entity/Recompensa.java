package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa una recompensa canjeable creada por un tutor.
 *
 * <p>El niño puede canjear una recompensa si su saldo de puntos es igual o
 * superior a {@link #puntosNecesarios}. Al canjear se genera una
 * {@link SolicitudCanje} que queda registrada en el historial del tutor.</p>
 */

@Entity
@Table(name = "recompensas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recompensa {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texto descriptivo de la recompensa, visible para el niño. */

    @Column(nullable = false)
    private String descripcion;

    /** Puntos necesarios para que el niño pueda canjear esta recompensa. */

    @Column(nullable = false)
    private Integer puntosNecesarios;

    /** Indica si la recompensa está disponible para ser canjeada. Por defecto {@code true}. */

    @Builder.Default
    private boolean activa = true;

    /**
     * Tutor que creó esta recompensa.
     * El niño solo verá las recompensas de los tutores que tenga asignados.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private Usuario familia;
}
