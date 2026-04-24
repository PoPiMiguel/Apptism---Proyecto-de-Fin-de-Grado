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
 * Servicio que gestiona todo lo relacionado con los usuarios: login,
 * consultas por rol, creación, vinculación entre tutores y niños, y borrado.
 *
 * Los métodos que acceden a colecciones con carga diferida (LAZY) llevan
 * {@code @Transactional} para evitar errores de inicialización fuera del
 * contexto de persistencia.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // ── Autenticación ──────────────────────────────────────────────

    /**
     * Comprueba si el email y la contraseña son correctos y devuelve el usuario.
     *
     * @param email    el correo del usuario
     * @param password la contraseña en texto plano
     * @return el usuario si las credenciales son correctas, o vacío si no coincide nada
     */
    public Optional<Usuario> login(String email, String password) {
        return usuarioRepository.findByEmailAndPassword(email, password);
    }

    // ── Consultas básicas ──────────────────────────────────────────

    /** Devuelve todos los tutores del sistema (padres y profesores juntos). */
    public List<Usuario> getTodosLosTutores() {
        List<Usuario> padres = usuarioRepository.findByRol(RolUsuario.PADRE);
        List<Usuario> profes = usuarioRepository.findByRol(RolUsuario.PROFESOR);
        List<Usuario> todos  = new ArrayList<>(padres);
        todos.addAll(profes);
        return todos;
    }

    /** Devuelve todos los niños registrados en el sistema. */
    public List<Usuario> getTodosLosNinos() {
        return usuarioRepository.findByRol(RolUsuario.NINO);
    }

    /**
     * Devuelve los contactos con los que puede chatear un usuario.
     * Si es niño, ve a todos los tutores. Si es tutor, ve a todos los niños.
     *
     * @param usuario el usuario cuya lista de contactos queremos obtener
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
     * Devuelve los niños asignados a un tutor dentro de una transacción de solo
     * lectura, para evitar errores al cargar la colección diferida.
     *
     * @param tutorId el identificador del tutor
     * @return lista de niños vinculados al tutor
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDetutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    /**
     * Busca qué tutores tienen asignado a un niño concreto. La búsqueda se hace
     * dentro de una transacción para poder cargar bien las colecciones diferidas.
     *
     * @param ninoId el identificador del niño
     * @return lista de tutores que tienen a ese niño asignado
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

    /** Devuelve todos los usuarios del sistema sin filtrar. */
    @Transactional(readOnly = true)
    public List<Usuario> getTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Crea un usuario nuevo desde el panel de administración.
     * Lanza una excepción si ya existe alguien con ese email.
     *
     * @param nombre   nombre completo
     * @param email    correo electrónico (tiene que ser único)
     * @param password contraseña en texto plano
     * @param rol      rol que se le asigna
     * @return el usuario recién creado y guardado en base de datos
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
     * Vincula un niño a un tutor si todavía no están vinculados.
     *
     * @param tutorId el identificador del tutor
     * @param ninoId  el identificador del niño
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
     * Elimina un usuario del sistema por su identificador.
     *
     * @param id el identificador del usuario a borrar
     */
    @Transactional
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}