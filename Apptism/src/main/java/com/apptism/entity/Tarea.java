package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tareas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    private CategoriaTarea categoria;

    @Builder.Default
    private Integer puntosPorCompletar = 10;

    @Builder.Default
    private boolean completada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nino_id", nullable = false)
    private Usuario nino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;
}