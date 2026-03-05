package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_emocionales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroEmocional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Emocion emocion;

    private Integer intensidad; // 1-5

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;
}
