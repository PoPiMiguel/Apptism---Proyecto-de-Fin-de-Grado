package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pictogramaUrl;   // URL del pictograma enviado
    private String textoPictograma; // Palabra que representa (ej: "feliz")

    @Enumerated(EnumType.STRING)
    private TipoMensaje tipo; // EMOCION (solo niño→padre) o CHAT (bidireccional)

    @Builder.Default
    private boolean leido = false;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;
}
