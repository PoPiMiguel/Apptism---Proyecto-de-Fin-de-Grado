package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa un paso individual dentro de una rutina.
 *
 * <p>Está prevista para desglosar una {@link Rutina} en pasos secuenciales,
 * cada uno con su propio pictograma. Esta funcionalidad no está implementada
 * en la interfaz de la versión actual de la aplicación.</p>
 */

@Entity
@Table(name = "pasos_rutina")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PasoRutina {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Descripción textual del paso, visible para el niño. */

    @Column(nullable = false)
    private String descripcion;

    /** URL de la imagen del pictograma de ARASAAC asociado a este paso. Puede ser {@code null}. */

    private String pictogramaUrl;

    /** Posición del paso dentro de la rutina, para mostrarlos en orden. */

    private Integer orden;

    /** Indica si el niño ha completado este paso. Por defecto {@code false}. */

    @Builder.Default
    private boolean completado = false;

    /**
     * Rutina a la que pertenece este paso.
     * Carga diferida para no traer la rutina completa en cada consulta de pasos.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;
}
