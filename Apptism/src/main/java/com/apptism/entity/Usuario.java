package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private String nombre;

    @Builder.Default
    private Integer puntosAcumulados = 0;

    @ManyToMany
    @JoinTable(
            name = "tutores_ninos",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "nino_id")
    )
    @Builder.Default
    private List<Usuario> ninos = new ArrayList<>();

    @OneToMany(mappedBy = "nino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rutina> rutinas = new ArrayList<>();

    @OneToMany(mappedBy = "nino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tarea> tareas = new ArrayList<>();
}
