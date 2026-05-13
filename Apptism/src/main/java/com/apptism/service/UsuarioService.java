package com.apptism.service;

import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import com.apptism.repository.UsuarioRepository;
import com.apptism.repository.MensajeRepository;
import com.apptism.repository.RecompensaRepository;
import com.apptism.repository.RutinaRepository;
import com.apptism.repository.SolicitudCanjeRepository;
import com.apptism.repository.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio que gestiona todo lo relacionado con los usuarios: inicio de sesión,
 * consultas por rol, creación, vinculación entre tutores y niños, y eliminación.
 *
 * <p>Los métodos que acceden a colecciones con carga diferida (LAZY) llevan
 * {@code @Transactional} para evitar errores de inicialización fuera del
 * contexto de persistencia.</p>
 */

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final RutinaRepository rutinaRepository;
    private final RecompensaRepository recompensaRepository;
    private final SolicitudCanjeRepository solicitudCanjeRepository;
    private final MensajeRepository mensajeRepository;

    /**
     * Comprueba si el email y la contraseña son correctos y devuelve el usuario.
     *
             * @param email    correo del usuario
     * @param password contraseña en texto plano
     * @return el usuario si las credenciales son correctas, o vacío si no coincide ninguno
     */

    public Optional<Usuario> login(String email, String password) {
        return usuarioRepository.findByEmailAndPassword(email, password);
    }

    /**
     * Devuelve todos los tutores del sistema (padres y profesores juntos).
     *
     * @return lista con todos los usuarios de rol PADRE y PROFESOR
     */

    public List<Usuario> getTodosLosTutores() {
        List<Usuario> padres = usuarioRepository.findByRol(RolUsuario.PADRE);
        List<Usuario> profes = usuarioRepository.findByRol(RolUsuario.PROFESOR);
        List<Usuario> todos  = new ArrayList<>(padres);
        todos.addAll(profes);
        return todos;
    }

    /**
     * Devuelve todos los niños registrados en el sistema.
     *
     * @return lista con todos los usuarios de rol NINO
     */

    public List<Usuario> getTodosLosNinos() {
        return usuarioRepository.findByRol(RolUsuario.NINO);
    }

    /**
     * Devuelve los contactos con los que puede chatear un usuario,
     * respetando los vínculos establecidos en la tabla {@code tutores_ninos}.
     *
     * <p>Si el usuario es niño, devuelve sus tutores asignados.
     * Si es tutor, devuelve sus niños asignados.</p>
     *
     * @param usuario el usuario cuya lista de contactos se quiere obtener
     * @return lista de usuarios con los que puede comunicarse
     */

    @Transactional(readOnly = true)
    public List<Usuario> getContactos(Usuario usuario) {
        if (usuario.getRol() == RolUsuario.NINO) {
            return getTutoresDeNino(usuario.getId());
        } else {
            return getNinosDetutor(usuario.getId());
        }
    }

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
     * Busca qué tutores tienen asignado a un niño concreto.
     *
     * <p>Itera todos los tutores del sistema y filtra los que tienen al niño
     * en su lista de asignados. La transacción permite cargar las colecciones LAZY.</p>
     *
     * @param ninoId identificador del niño
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


    /**
     * Devuelve todos los usuarios del sistema sin filtrar por rol.
     *
     * @return lista con todos los usuarios registrados
     */

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
     * @param tutorId identificador del tutor
     * @param ninoId  identificador del niño
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
     * Elimina un usuario y todos sus datos asociados, en el orden correcto
     * para no romper las claves foráneas de MySQL.
     *
     * <p>Orden de borrado:</p>
     * <ol>
     *   <li>Mensajes donde aparece como emisor o receptor.</li>
     *   <li>Solicitudes de canje donde es el niño.</li>
     *   <li>Solicitudes de canje ligadas a sus recompensas (si es tutor).</li>
     *   <li>Sus recompensas.</li>
     *   <li>Tareas donde es creador o niño destinatario.</li>
     *   <li>Rutinas donde es creador o niño destinatario.</li>
     *   <li>Los vínculos tutor–niño donde aparece.</li>
     *   <li>El propio usuario.</li>
     * </ol>
     *
     * @param id identificador del usuario a eliminar
     */

    @Transactional
    public void eliminarUsuario(Long id) {
        mensajeRepository.deleteByEmisorId(id);
        mensajeRepository.deleteByReceptorId(id);
        solicitudCanjeRepository.deleteByNinoId(id);
        recompensaRepository.findByFamiliaId(id).forEach(r ->
                solicitudCanjeRepository.deleteByRecompensaId(r.getId()));
        recompensaRepository.deleteByFamiliaId(id);
        tareaRepository.deleteByCreadorId(id);
        tareaRepository.deleteByNinoId(id);
        rutinaRepository.deleteByCreadorId(id);
        rutinaRepository.deleteByNinoId(id);
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null) {
            usuarioRepository.findAll().forEach(tutor -> {
                if (tutor.getNinos().removeIf(n -> n.getId().equals(id))) {
                    usuarioRepository.save(tutor);
                }
            });
        }
        usuarioRepository.deleteById(id);
    }
}