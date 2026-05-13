package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa una tarea asignada a un niño por su tutor.
 *
 * <p>Las tareas se muestran al niño como tarjetas visuales con el pictograma
 * de ARASAAC asociado. Al marcarlas como completadas, el niño acumula los
 * puntos definidos en {@link #puntosPorCompletar}.</p>
 */

@Entity
@Table(name = "tareas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título descriptivo de la tarea, visible en la tarjeta del niño. */

    @Column(nullable = false)
    private String titulo;

    /** Identificador del pictograma de ARASAAC asociado. Puede ser {@code null}. */

    private Integer pictogramaId;

    /** URL de la imagen del pictograma de ARASAAC. Puede ser {@code null}. */

    private String pictogramaUrl;

    /** Puntos que el niño gana al completar esta tarea. Por defecto 10. */

    @Builder.Default
    private Integer puntosPorCompletar = 10;

    /** Indica si el niño ha completado esta tarea. */

    @Builder.Default
    private boolean completada = false;

    /**
     * Niño al que está asignada esta tarea.
     * Carga diferida para no traer el usuario completo en cada consulta de tareas.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;

    /**
     * Tutor que creó esta tarea.
     * Puede ser {@code null} si la tarea fue creada sin asociar un creador.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;
}