package com.apptism.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a cualquier usuario del sistema.
 *
 * <p>El campo {@link #rol} determina el perfil y las funcionalidades accesibles:
 * niño, padre, profesor o administrador. Los puntos acumulados solo son
 * relevantes para el rol {@link RolUsuario#NINO}, que los obtiene al completar
 * tareas y los gasta al canjear recompensas.</p>
 *
 * <p>La relación {@link #ninos} modela el vínculo tutor → niño a través de la
 * tabla intermedia {@code tutores_ninos}. Solo los tutores (padre/profesor)
 * tienen entradas en esa tabla como propietarios de la relación.</p>
 */

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    /** Identificador único generado automáticamente por la base de datos. */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico del usuario. Actúa como nombre de usuario para el login. */

    @Column(nullable = false, unique = true)
    private String email;

    /** Contraseña del usuario en texto plano. */

    @Column(nullable = false)
    private String password;

    /** Rol del usuario, que determina su perfil y sus permisos dentro de la aplicación. */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    /** Nombre visible del usuario en la interfaz. */

    @Column(nullable = false)
    private String nombre;

    /** Puntos acumulados por completar tareas. Solo relevante para el rol {@link RolUsuario#NINO}. */

    @Builder.Default
    private Integer puntosAcumulados = 0;

    /**
     * Niños asignados a este tutor.
     *
     * <p>Relación Many-to-Many gestionada a través de la tabla {@code tutores_ninos}.
     * Solo se rellena cuando el usuario es tutor (padre o profesor).
     * La carga es diferida (LAZY) para no traer todos los niños en cada consulta.</p>
     */

    @ManyToMany
    @JoinTable(
            name = "tutores_ninos",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "nino_id")
    )
    @Builder.Default
    private List<Usuario> ninos = new ArrayList<>();

    /**
     * Rutinas asignadas a este niño como destinatario.
     *
     * <p>Relación One-to-Many: un niño puede tener múltiples rutinas.
     * Al eliminar el niño se eliminan también todas sus rutinas en cascada.</p>
     */

    @OneToMany(mappedBy = "nino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rutina> rutinas = new ArrayList<>();

    /**
     * Tareas asignadas a este niño como destinatario.
     *
     * <p>Relación One-to-Many: un niño puede tener múltiples tareas.
     * Al eliminar el niño se eliminan también todas sus tareas en cascada.</p>
     */

    @OneToMany(mappedBy = "nino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tarea> tareas = new ArrayList<>();
}
