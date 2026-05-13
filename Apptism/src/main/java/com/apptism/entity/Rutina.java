package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una rutina diaria asignada a un niño.
 *
 * <p>Las rutinas se organizan por franja horaria ({@link ZonaHoraria}) y
 * se muestran al niño como tarjetas visuales con el pictograma de ARASAAC
 * asociado. El tutor las crea y el niño las marca como completadas.</p>
 *
 * <p>El campo {@link #pasos} está previsto para desglosar una rutina en
 * pasos individuales, pero esta funcionalidad no está implementada en
 * la interfaz de la versión actual.</p>
 */

@Entity
@Table(name = "rutinas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rutina {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre descriptivo de la rutina, visible en la tarjeta del niño. */

    @Column(nullable = false)
    private String nombre;

    /** Franja horaria a la que pertenece la rutina (mañana, mediodía o noche). */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZonaHoraria zonaHoraria;

    /** Indica si el niño ha marcado esta rutina como completada. */

    @Builder.Default
    private boolean completada = false;

    /** Posición de la rutina dentro de su franja horaria, para ordenarlas visualmente. */

    private Integer orden;

    /** Identificador del pictograma de ARASAAC asociado a esta rutina. Puede ser {@code null}. */

    private Integer pictogramaId;

    /** URL de la imagen del pictograma de ARASAAC. Puede ser {@code null}. */

    private String pictogramaUrl;

    /**
     * Niño al que está asignada esta rutina.
     * Carga diferida para no traer el usuario completo en cada consulta de rutinas.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;

    /**
     * Tutor que creó esta rutina.
     * Puede ser {@code null} si la rutina fue creada sin asociar un creador.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    /**
     * Pasos individuales que componen esta rutina, ordenados por el campo {@code orden}.
     * Funcionalidad prevista pero no implementada en la interfaz actual.
     */

    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PasoRutina> pasos = new ArrayList<>();
}