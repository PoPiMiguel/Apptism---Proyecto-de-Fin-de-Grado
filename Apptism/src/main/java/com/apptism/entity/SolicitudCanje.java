package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que registra cada canje de recompensa realizado por un niño.
 *
 * <p>Se genera automáticamente cuando el niño canjea una {@link Recompensa}.
 * El tutor la verá en la pantalla de solicitudes de canje, donde queda
 * constancia del niño, la recompensa y la fecha del canje.</p>
 */

@Entity
@Table(name = "solicitudes_canje")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudCanje {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Estado actual de la solicitud. Por defecto {@link EstadoSolicitud#PENDIENTE}. */

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    /**
     * Indica si el tutor ha visto esta solicitud.
     * Se usa para calcular el badge de notificaciones en el dashboard.
     */

    @Builder.Default
    private boolean leida = false;

    /** Fecha y hora en que se realizó el canje. Se asigna automáticamente al crear el objeto. */

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    /**
     * Niño que realizó el canje.
     * Carga diferida para no traer el usuario completo en cada consulta.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;

    /**
     * Recompensa que fue canjeada.
     * Carga diferida para no traer la recompensa completa en cada consulta.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recompensa_id", nullable = false)
    private Recompensa recompensa;
}
