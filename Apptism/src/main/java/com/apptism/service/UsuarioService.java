// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: UsuarioService.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.service;

import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de usuarios de Apptism.
 *
 * <p>Centraliza toda la lógica relacionada con usuarios: autenticación,
 * consultas por rol, creación, vinculación tutor-niño y eliminación.
 *
 * <p>Los métodos que acceden a colecciones con carga diferida ({@code LAZY})
 * están anotados con {@code @Transactional} para evitar
 * {@code LazyInitializationException} fuera del contexto de persistencia.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // ── Autenticación ──────────────────────────────────────────────

    /**
     * Autentica a un usuario comprobando email y contraseña en la base de datos.
     *
     * @param email    correo electrónico del usuario
     * @param password contraseña en texto plano
     * @return {@link Optional} con el usuario si las credenciales son correctas,
     *         o vacío si no se encuentra ninguna coincidencia
     */
    public Optional<Usuario> login(String email, String password) {
        return usuarioRepository.findByEmailAndPassword(email, password);
    }

    // ── Consultas básicas ──────────────────────────────────────────

    /**
     * Devuelve todos los usuarios con rol de tutor (PADRE o PROFESOR).
     *
     * @return lista combinada de padres y profesores registrados en el sistema
     */
    public List<Usuario> getTodosLosTutores() {
        List<Usuario> padres = usuarioRepository.findByRol(RolUsuario.PADRE);
        List<Usuario> profes = usuarioRepository.findByRol(RolUsuario.PROFESOR);
        List<Usuario> todos  = new ArrayList<>(padres);
        todos.addAll(profes);
        return todos;
    }

    /**
     * Devuelve todos los usuarios con rol de niño (NINO).
     *
     * @return lista de todos los niños registrados en el sistema
     */
    public List<Usuario> getTodosLosNinos() {
        return usuarioRepository.findByRol(RolUsuario.NINO);
    }

    /**
     * Devuelve los contactos disponibles para el chat de un usuario.
     *
     * <p>Un niño ve a todos los tutores del sistema; un tutor ve a
     * todos los niños.
     *
     * @param usuario usuario cuya lista de contactos se desea obtener
     * @return lista de usuarios con los que puede comunicarse
     */
    public List<Usuario> getContactos(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.NINO) {
            return getTodosLosTutores();
        } else {
            return getTodosLosNinos();
        }
    }

    // ── Carga lazy segura (dentro de @Transactional) ───────────────

    /**
     * Obtiene la lista de niños asignados a un tutor dentro de una transacción
     * de solo lectura, evitando {@code LazyInitializationException}.
     *
     * @param tutorId identificador del tutor
     * @return lista de niños vinculados al tutor
     * @throws RuntimeException si no existe ningún tutor con ese identificador
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDetutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    /**
     * Dado el identificador de un niño, devuelve los tutores que lo tienen
     * asignado. La búsqueda se realiza dentro de una transacción de solo
     * lectura para inicializar correctamente las colecciones LAZY.
     *
     * @param ninoId identificador del niño
     * @return lista de tutores que tienen vinculado a ese niño
     */
    @Transactional(readOnly = true)
    public List<Usuario> getTutoresDeNino(Long ninoId) {
        List<Usuario> todosLosTutores = getTodosLosTutores();
        List<Usuario> tutoresDelNino  = new ArrayList<>();
        for (Usuario tutor : todosLosTutores) {
            List<Usuario> ninosDelTutor = new ArrayList<>(tutor.getNinos());
            boolean esNinoDelTutor = ninosDelTutor.stream()
                    .anyMatch(n -> n.getId().equals(ninoId));
            if (esNinoDelTutor) {
                tutoresDelNino.add(tutor);
            }
        }
        return tutoresDelNino;
    }

    // ── Gestión de usuarios (panel de administración) ──────────────

    /**
     * Devuelve todos los usuarios del sistema sin filtrar por rol.
     *
     * @return lista completa de usuarios registrados
     */
    @Transactional(readOnly = true)
    public List<Usuario> getTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Crea un nuevo usuario en el sistema desde el panel de administración.
     *
     * @param nombre   nombre completo del usuario
     * @param email    correo electrónico único
     * @param password contraseña en texto plano
     * @param rol      rol asignado al usuario
     * @return entidad {@link Usuario} persistida con su identificador generado
     * @throws RuntimeException si ya existe un usuario con el mismo email
     */
    @Transactional
    public Usuario crearUsuario(String nombre, String email, String password, RolUsuario rol) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email: " + email);
        }
        Usuario nuevo = Usuario.builder()
                .nombre(nombre)
                .email(email)
                .password(password)
                .rol(rol)
                .puntosAcumulados(0)
                .build();
        return usuarioRepository.save(nuevo);
    }

    /**
     * Establece un vínculo entre un tutor y un niño si aún no existe.
     *
     * @param tutorId identificador del tutor
     * @param ninoId  identificador del niño
     * @throws RuntimeException si el tutor o el niño no existen en la base de datos
     */
    @Transactional
    public void vincularNinoATutor(Long tutorId, Long ninoId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        Usuario nino  = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        List<Usuario> ninos = new ArrayList<>(tutor.getNinos());
        boolean yaVinculado = ninos.stream().anyMatch(n -> n.getId().equals(ninoId));
        if (!yaVinculado) {
            tutor.getNinos().add(nino);
            usuarioRepository.save(tutor);
        }
    }

    /**
     * Elimina permanentemente un usuario del sistema por su identificador.
     *
     * @param id identificador del usuario a eliminar
     */
    @Transactional
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    /**
     * Suma una cantidad de puntos al saldo acumulado de un niño.
     *
     * @param ninoId identificador del niño
     * @param puntos cantidad de puntos a añadir (puede ser negativa para restar)
     * @return entidad {@link Usuario} actualizada con el nuevo saldo
     * @throws RuntimeException si no existe ningún niño con ese identificador
     */
    @Transactional
    public Usuario actualizarPuntos(Long ninoId, int puntos) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        nino.setPuntosAcumulados(nino.getPuntosAcumulados() + puntos);
        return usuarioRepository.save(nino);
    }

    /**
     * Registra un nuevo usuario validando que el email no esté en uso.
     *
     * @param usuario entidad {@link Usuario} con los datos a persistir
     * @return entidad persistida con su identificador generado
     * @throws RuntimeException si el email ya está registrado
     */
    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }
        return usuarioRepository.save(usuario);
    }
}