package com.apptism.service;

import com.apptism.entity.Rutina;
import com.apptism.entity.Usuario;
import com.apptism.entity.ZonaHoraria;
import com.apptism.repository.RutinaRepository;
import com.apptism.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Rutina> getRutinasByZona(Long ninoId, ZonaHoraria zona) {
        return rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona);
    }

    public List<Rutina> todasLasRutinas(Long ninoId) {
        return rutinaRepository.findByNinoIdOrderByOrdenAsc(ninoId);
    }

    /**
     * Obtiene todas las rutinas de todos los niños del tutor.
     * La colección ninos es LAZY — hay que cargarla dentro de transacción.
     */
    @Transactional(readOnly = true)
    public List<Rutina> getRutinasPorZonaDeNinosDelTutor(Long tutorId, ZonaHoraria zona) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        List<Usuario> ninos = new ArrayList<>(tutor.getNinos());
        List<Rutina> todas = new ArrayList<>();
        for (Usuario nino : ninos) {
            todas.addAll(rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(nino.getId(), zona));
        }
        return todas;
    }

    /**
     * Obtiene los niños del tutor dentro de transacción.
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDelTutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    @Transactional
    public Rutina crearRutina(String nombre, ZonaHoraria zona, Long ninoId) {
        return crearRutina(nombre, zona, ninoId, ninoId);
    }

    @Transactional
    public Rutina crearRutina(String nombre, ZonaHoraria zona, Long ninoId, Long creadorId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));

        long orden = rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona).size();

        Rutina rutina = Rutina.builder()
                .nombre(nombre)
                .zonaHoraria(zona)
                .nino(nino)
                .completada(false)
                .orden((int) orden + 1)
                .build();

        return rutinaRepository.save(rutina);
    }

    @Transactional
    public void marcarCompletada(Long rutinaId) {
        Rutina rutina = rutinaRepository.findById(rutinaId)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));
        rutina.setCompletada(true);
        rutinaRepository.save(rutina);
    }

    @Transactional
    public void eliminarRutina(Long rutinaId) {
        rutinaRepository.deleteById(rutinaId);
    }
}
