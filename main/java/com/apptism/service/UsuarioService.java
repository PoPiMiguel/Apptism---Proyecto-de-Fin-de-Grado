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

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // ── Autenticación ─────────────────────────────────────────

    public Optional<Usuario> login(String email, String password) {
        return usuarioRepository.findByEmailAndPassword(email, password);
    }

    // ── Consultas básicas ─────────────────────────────────────

    public List<Usuario> getTodosLosTutores() {
        List<Usuario> padres = usuarioRepository.findByRol(RolUsuario.PADRE);
        List<Usuario> profes = usuarioRepository.findByRol(RolUsuario.PROFESOR);
        List<Usuario> todos  = new ArrayList<>(padres);
        todos.addAll(profes);
        return todos;
    }

    public List<Usuario> getTodosLosNinos() {
        return usuarioRepository.findByRol(RolUsuario.NINO);
    }

    /** Devuelve los contactos de un usuario para el chat. */
    public List<Usuario> getContactos(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.NINO) {
            return getTodosLosTutores();
        } else {
            return getTodosLosNinos();
        }
    }

    // ── Carga lazy segura (dentro de @Transactional) ──────────

    /**
     * Carga la lista de niños de un tutor dentro de transacción
     * para evitar LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDetutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    /**
     * Dado un niño, devuelve los tutores que lo tienen asignado.
     * Carga lazy dentro de transacción.
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

    // ── Gestión de usuarios (Admin) ───────────────────────────

    /** Devuelve todos los usuarios del sistema. */
    @Transactional(readOnly = true)
    public List<Usuario> getTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    /** Crea un usuario nuevo desde el panel de administración. */
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

    /** Vincula un alumno a un tutor (padre/profesor). */
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

    @Transactional
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public Usuario actualizarPuntos(Long ninoId, int puntos) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        nino.setPuntosAcumulados(nino.getPuntosAcumulados() + puntos);
        return usuarioRepository.save(nino);
    }

    @Transactional
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }
        return usuarioRepository.save(usuario);
    }
}
