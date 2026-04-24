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
 * Servicio que gestiona el ciclo de vida de las tareas: el tutor las crea,
 * el niño las completa (y acumula puntos), y el tutor puede eliminarlas.
 * También tiene consultas filtradas por niño, creador y estado.
 */
@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve todas las tareas asignadas a un niño, estén completadas o no.
     *
     * @param ninoId el identificador del niño
     * @return lista de sus tareas
     */
    public List<Tarea> getTareasByNino(Long ninoId) {
        return tareaRepository.findByNinoId(ninoId);
    }

    /**
     * Crea una tarea nueva y la asigna a un niño concreto.
     *
     * @param titulo       título de la tarea
     * @param categoriaStr nombre de la categoría (tiene que coincidir con {@link CategoriaTarea})
     * @param puntos       puntos que gana el niño al completarla
     * @param ninoId       identificador del niño al que se asigna
     * @param creadorId    identificador del tutor que la crea
     * @return la tarea guardada en base de datos
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
     * Marca una tarea como completada y le suma los puntos al niño.
     * Si la tarea ya estaba completada, devuelve el saldo actual sin tocar nada
     * para evitar que se acumulen puntos dos veces.
     *
     * @param tareaId el identificador de la tarea a completar
     * @return el nuevo saldo de puntos del niño
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
     * Elimina una tarea del sistema.
     *
     * @param tareaId el identificador de la tarea a borrar
     */
    @Transactional
    public void eliminarTarea(Long tareaId) {
        tareaRepository.deleteById(tareaId);
    }

    /**
     * Devuelve todas las tareas de los niños asignados a un tutor, incluidas las
     * que el propio tutor creó directamente. Los duplicados se eliminan.
     *
     * La colección de niños es LAZY, así que se ejecuta dentro de una transacción.
     *
     * @param tutorId el identificador del tutor
     * @return lista deduplicada de tareas de todos sus niños
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