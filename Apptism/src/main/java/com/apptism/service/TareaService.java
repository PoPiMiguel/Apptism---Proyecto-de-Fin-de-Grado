package com.apptism.service;

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
 * Servicio que gestiona el ciclo de vida de las tareas: el tutor las crea
 * y las asigna a un niño, el niño las completa acumulando puntos,
 * y el tutor puede eliminarlas.
 */

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve todas las tareas asignadas a un niño, completadas o no.
     *
     * @param ninoId identificador del niño
     * @return lista de sus tareas
     */

    public List<Tarea> getTareasByNino(Long ninoId) {
        return tareaRepository.findByNinoId(ninoId);
    }

    /**
     * Crea una tarea nueva y la asigna a un niño concreto.
     *
     * @param titulo        título de la tarea
     * @param pictogramaId  ID del pictograma ARASAAC seleccionado; puede ser {@code null}
     * @param pictogramaUrl URL de la imagen del pictograma; puede ser {@code null}
     * @param puntos        puntos que gana el niño al completarla
     * @param ninoId        identificador del niño al que se asigna
     * @param creadorId     identificador del tutor que la crea
     * @return la tarea guardada en base de datos
     */

    @Transactional
    public Tarea crearTarea(String titulo, Integer pictogramaId, String pictogramaUrl,
                            int puntos, Long ninoId, Long creadorId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RuntimeException("Creador no encontrado"));

        Tarea tarea = Tarea.builder()
                .titulo(titulo)
                .pictogramaId(pictogramaId)
                .pictogramaUrl(pictogramaUrl)
                .puntosPorCompletar(puntos)
                .nino(nino)
                .creador(creador)
                .completada(false)
                .build();

        return tareaRepository.save(tarea);
    }

    /**
     * Marca una tarea como completada y suma los puntos al niño.
     *
     * <p>Si la tarea ya estaba completada devuelve el saldo actual sin modificar
     * nada, para evitar que se acumulen puntos dos veces.</p>
     *
     * @param tareaId identificador de la tarea a completar
     * @return los puntos acumulados del niño tras la operación
     */

    @Transactional
    public int completarTarea(Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (tarea.isCompletada()) {
            return tarea.getNino().getPuntosAcumulados();
        }

        tarea.setCompletada(true);
        tareaRepository.save(tarea);

        Usuario nino = usuarioRepository.findById(tarea.getNino().getId())
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));
        int nuevosPuntos = nino.getPuntosAcumulados() + tarea.getPuntosPorCompletar();
        nino.setPuntosAcumulados(nuevosPuntos);
        usuarioRepository.save(nino);

        return nuevosPuntos;
    }

    /**
     * Elimina una tarea del sistema.
     *
     * @param tareaId identificador de la tarea a eliminar
     */

    @Transactional
    public void eliminarTarea(Long tareaId) {
        tareaRepository.deleteById(tareaId);
    }

    /**
     * Devuelve todas las tareas de los niños asignados a un tutor.
     *
     * @param tutorId identificador del tutor
     * @return lista de tareas de todos sus niños asignados
     */

    @Transactional(readOnly = true)
    public List<Tarea> getTareasDeNinosDelTutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        List<Tarea> todas = new ArrayList<>();
        for (Usuario nino : tutor.getNinos()) {
            todas.addAll(tareaRepository.findByNinoId(nino.getId()));
        }
        return todas;
    }
}