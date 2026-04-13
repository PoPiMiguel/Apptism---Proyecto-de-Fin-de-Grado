// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: TareaService.java
// ═══════════════════════════════════════════════════════════════════
package com.apptism.service;

import com.apptism.entity.CategoriaTarea;
import com.apptism.entity.Tarea;
import com.apptism.entity.Usuario;
import com.apptism.repository.TareaRepository;
import com.apptism.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para la gestión de tareas asignadas a los niños.
 *
 * <p>Gestiona el ciclo de vida completo de una tarea: creación por parte
 * del tutor, completación por parte del niño (con acumulación automática
 * de puntos) y eliminación. También proporciona consultas filtradas por
 * niño, creador y estado.
 */
@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve todas las tareas asignadas a un niño, independientemente
     * de su estado de completación.
     *
     * @param ninoId identificador del niño
     * @return lista de tareas del niño ordenadas por el repositorio
     */
    public List<Tarea> getTareasByNino(Long ninoId) {
        return tareaRepository.findByNinoId(ninoId);
    }

    /**
     * Devuelve únicamente las tareas pendientes (no completadas) de un niño.
     *
     * @param ninoId identificador del niño
     * @return lista de tareas con {@code completada = false}
     */
    public List<Tarea> getTareasPendientes(Long ninoId) {
        return tareaRepository.findByNinoIdAndCompletada(ninoId, false);
    }

    /**
     * Crea una nueva tarea y la asigna a un niño concreto.
     *
     * @param titulo       título descriptivo de la tarea
     * @param categoriaStr nombre de la categoría (debe coincidir con {@link CategoriaTarea})
     * @param puntos       puntos que recibirá el niño al completarla
     * @param ninoId       identificador del niño al que se asigna
     * @param creadorId    identificador del tutor que la crea
     * @return entidad {@link Tarea} persistida
     * @throws RuntimeException si el niño o el creador no existen
     */
    @Transactional
    public Tarea crearTarea(String titulo, String categoriaStr, int puntos,
                            Long ninoId, Long creadorId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RuntimeException("Creador no encontrado"));

        Tarea tarea = Tarea.builder()
                .titulo(titulo)
                .categoria(CategoriaTarea.valueOf(categoriaStr))
                .puntosPorCompletar(puntos)
                .nino(nino)
                .creador(creador)
                .completada(false)
                .build();

        return tareaRepository.save(tarea);
    }

    /**
     * Sobrecarga de {@link #crearTarea(String, String, int, Long, Long)} que
     * asigna la tarea y la marca como creada por el mismo usuario.
     *
     * @param titulo       título de la tarea
     * @param categoriaStr categoría de la tarea
     * @param puntos       puntos por completar
     * @param usuarioId    identificador del usuario (actúa como niño y creador)
     * @return entidad {@link Tarea} persistida
     */
    @Transactional
    public Tarea crearTarea(String titulo, String categoriaStr, int puntos, Long usuarioId) {
        return crearTarea(titulo, categoriaStr, puntos, usuarioId, usuarioId);
    }

    /**
     * Marca una tarea como completada y suma los puntos correspondientes
     * al saldo del niño.
     *
     * <p>Si la tarea ya estaba completada, devuelve el saldo actual
     * sin modificar nada, evitando doble acumulación de puntos.
     *
     * @param tareaId identificador de la tarea a completar
     * @return nuevo saldo de puntos acumulados del niño tras la completación
     * @throws RuntimeException si no existe ninguna tarea con ese identificador
     */
    @Transactional
    public int completarTarea(Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (tarea.isCompletada()) return tarea.getNino().getPuntosAcumulados();

        tarea.setCompletada(true);
        tareaRepository.save(tarea);

        Usuario nino = tarea.getNino();
        nino.setPuntosAcumulados(nino.getPuntosAcumulados() + tarea.getPuntosPorCompletar());
        usuarioRepository.save(nino);

        return nino.getPuntosAcumulados();
    }

    /**
     * Elimina permanentemente una tarea por su identificador.
     *
     * @param tareaId identificador de la tarea a eliminar
     */
    @Transactional
    public void eliminarTarea(Long tareaId) {
        tareaRepository.deleteById(tareaId);
    }

    /**
     * Devuelve todas las tareas creadas por un tutor concreto.
     *
     * @param creadorId identificador del tutor creador
     * @return lista de tareas creadas por ese tutor
     */
    public List<Tarea> getTareasCreadasPor(Long creadorId) {
        return tareaRepository.findByCreadorId(creadorId);
    }

    /**
     * Devuelve todas las tareas de los niños vinculados a un tutor,
     * incluyendo también las que el tutor creó directamente.
     *
     * <p>La colección {@code ninos} del tutor es LAZY, por lo que este
     * método se ejecuta dentro de una transacción de solo lectura.
     * Los duplicados se eliminan comparando por identidad de entidad.
     *
     * @param tutorId identificador del tutor
     * @return lista deduplicada de tareas de los niños del tutor
     * @throws RuntimeException si no existe ningún tutor con ese identificador
     */
    @Transactional(readOnly = true)
    public List<Tarea> getTareasDeNinosDelTutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        List<Usuario> ninos = new ArrayList<>(tutor.getNinos());
        List<Tarea> todas = new ArrayList<>();
        for (Usuario nino : ninos) {
            todas.addAll(tareaRepository.findByNinoId(nino.getId()));
        }
        todas.addAll(tareaRepository.findByCreadorId(tutorId));
        return todas.stream().distinct().toList();
    }
}