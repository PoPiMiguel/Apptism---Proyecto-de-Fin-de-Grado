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

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Tarea> getTareasByNino(Long ninoId) {
        return tareaRepository.findByNinoId(ninoId);
    }

    public List<Tarea> getTareasPendientes(Long ninoId) {
        return tareaRepository.findByNinoIdAndCompletada(ninoId, false);
    }

    /**
     * Crea una tarea asignada a un niño concreto, creada por el tutor.
     * ninoId: el niño al que se asigna la tarea
     * creadorId: el tutor que la crea
     */
    @Transactional
    public Tarea crearTarea(String titulo, String categoriaStr, int puntos, Long ninoId, Long creadorId) {
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

    /** Sobrecarga: crea tarea asignada al mismo usuario (compatibilidad) */
    @Transactional
    public Tarea crearTarea(String titulo, String categoriaStr, int puntos, Long usuarioId) {
        return crearTarea(titulo, categoriaStr, puntos, usuarioId, usuarioId);
    }

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

    @Transactional
    public void eliminarTarea(Long tareaId) {
        tareaRepository.deleteById(tareaId);
    }

    public List<Tarea> getTareasCreadasPor(Long creadorId) {
        return tareaRepository.findByCreadorId(creadorId);
    }

    /**
     * Devuelve todas las tareas de los niños asignados a un tutor.
     * Carga la colección lazy dentro de la misma transacción.
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
        // También incluir las creadas directamente por el tutor
        todas.addAll(tareaRepository.findByCreadorId(tutorId));
        // Eliminar duplicados por ID
        return todas.stream()
                .distinct()
                .toList();
    }
}
