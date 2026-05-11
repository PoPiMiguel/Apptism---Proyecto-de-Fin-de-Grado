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

    /**
     * Devuelve los niños asignados a un tutor.
     * Necesita transacción porque la colección es LAZY.
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDelTutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    /**
     * Crea una rutina nueva y la asigna a un niño concreto.
     * El pictograma (id y url) lo selecciona el tutor desde el buscador de ARASAAC.
     *
     * @param nombre         nombre de la rutina
     * @param zona           franja horaria (MANANA, MEDIODIA, NOCHE)
     * @param ninoId         identificador del niño destinatario
     * @param creadorId      identificador del tutor que la crea
     * @param pictogramaId   ID del pictograma ARASAAC seleccionado (puede ser null)
     * @param pictogramaUrl  URL de la imagen del pictograma (puede ser null)
     * @return la rutina guardada en base de datos
     */
    @Transactional
    public Rutina crearRutina(String nombre, ZonaHoraria zona, Long ninoId, Long creadorId,
                              Integer pictogramaId, String pictogramaUrl) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));

        long orden = rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona).size();

        Rutina rutina = Rutina.builder()
                .nombre(nombre)
                .zonaHoraria(zona)
                .nino(nino)
                .completada(false)
                .orden((int) orden + 1)
                .pictogramaId(pictogramaId)
                .pictogramaUrl(pictogramaUrl)
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

    /**
     * Devuelve todas las rutinas de un niño sin filtrar por zona,
     * ordenadas por el campo orden.
     * Usa el método del repositorio existente: findByNinoIdOrderByOrdenAsc.
     */
    @Transactional(readOnly = true)
    public List<Rutina> todasLasRutinas(Long ninoId) {
        return rutinaRepository.findByNinoIdOrderByOrdenAsc(ninoId);
    }

    /**
     * Devuelve las rutinas de un niño filtradas por zona horaria.
     */
    @Transactional(readOnly = true)
    public List<Rutina> getRutinasByZona(Long ninoId, ZonaHoraria zona) {
        return rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona);
    }
}