package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un mensaje con pictograma entre dos usuarios.
 *
 * <p>Los módulos de chat y de emociones comparten esta misma tabla y entidad,
 * diferenciándose únicamente por el campo {@link #tipo}:
 * {@link TipoMensaje#CHAT} para conversación general y
 * {@link TipoMensaje#EMOCION} para el registro emocional del niño.</p>
 */

@Entity
@Table(name = "mensajes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensaje {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL de la imagen del pictograma de ARASAAC enviado. */

    private String pictogramaUrl;

    /** Nombre o etiqueta en español del pictograma enviado. */

    private String textoPictograma;

    /** Tipo de mensaje: {@link TipoMensaje#CHAT} o {@link TipoMensaje#EMOCION}. */

    @Enumerated(EnumType.STRING)
    private TipoMensaje tipo;

    /** Indica si el receptor ha leído el mensaje. Por defecto {@code false}. */

    @Builder.Default
    private boolean leido = false;

    /** Fecha y hora en que se envió el mensaje. Se asigna automáticamente al crear el objeto. */

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    /**
     * Usuario que envió el mensaje.
     * Carga diferida para no traer el usuario completo en cada consulta de mensajes.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    /**
     * Usuario que recibe el mensaje.
     * Carga diferida para no traer el usuario completo en cada consulta de mensajes.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;
}
