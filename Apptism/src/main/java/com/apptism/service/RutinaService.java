// ═══════════════════════════════════════════════════════════════════
// ARCHIVO: RutinaService.java
// ═══════════════════════════════════════════════════════════════════
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

/**
 * Servicio de negocio para la gestión de rutinas diarias.
 *
 * <p>Las rutinas representan actividades organizadas por franja horaria
 * (mañana, mediodía y noche) asignadas a un niño por su tutor. Este
 * servicio gestiona su creación, consulta por zona horaria, marcado de
 * completación y eliminación.
 */
@Service
@RequiredArgsConstructor
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve las rutinas de un niño filtradas por zona horaria,
     * ordenadas por su campo {@code orden} de forma ascendente.
     *
     * @param ninoId identificador del niño
     * @param zona   franja horaria a filtrar (MANANA, MEDIODIA o NOCHE)
     * @return lista ordenada de rutinas del niño en esa franja
     */
    public List<Rutina> getRutinasByZona(Long ninoId, ZonaHoraria zona) {
        return rutinaRepository.findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona);
    }

    /**
     * Devuelve todas las rutinas de un niño de todas las franjas horarias,
     * ordenadas por su campo {@code orden}.
     *
     * @param ninoId identificador del niño
     * @return lista completa de rutinas del niño
     */
    public List<Rutina> todasLasRutinas(Long ninoId) {
        return rutinaRepository.findByNinoIdOrderByOrdenAsc(ninoId);
    }

    /**
     * Devuelve las rutinas de todos los niños del tutor filtradas por zona horaria.
     *
     * <p>La colección {@code ninos} del tutor es LAZY, por lo que este
     * método se ejecuta dentro de una transacción de solo lectura.
     *
     * @param tutorId identificador del tutor
     * @param zona    franja horaria a consultar
     * @return lista de rutinas de todos los niños del tutor en esa franja
     * @throws RuntimeException si no existe ningún tutor con ese identificador
     */
    @Transactional(readOnly = true)
    public List<Rutina> getRutinasPorZonaDeNinosDelTutor(Long tutorId, ZonaHoraria zona) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        List<Usuario> ninos = new ArrayList<>(tutor.getNinos());
        List<Rutina> todas = new ArrayList<>();
        for (Usuario nino : ninos) {
            todas.addAll(rutinaRepository
                    .findByNinoIdAndZonaHorariaOrderByOrdenAsc(nino.getId(), zona));
        }
        return todas;
    }

    /**
     * Obtiene los niños vinculados a un tutor dentro de una transacción
     * de solo lectura para inicializar correctamente la colección LAZY.
     *
     * @param tutorId identificador del tutor
     * @return lista de niños asignados al tutor
     * @throws RuntimeException si no existe ningún tutor con ese identificador
     */
    @Transactional(readOnly = true)
    public List<Usuario> getNinosDelTutor(Long tutorId) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));
        return new ArrayList<>(tutor.getNinos());
    }

    /**
     * Crea una rutina asignándola al mismo usuario como niño y creador.
     * Sobrecarga de conveniencia para llamadas internas.
     *
     * @param nombre  nombre descriptivo de la rutina
     * @param zona    franja horaria de la rutina
     * @param ninoId  identificador del niño (y creador)
     * @return entidad {@link Rutina} persistida
     */
    @Transactional
    public Rutina crearRutina(String nombre, ZonaHoraria zona, Long ninoId) {
        return crearRutina(nombre, zona, ninoId, ninoId);
    }

    /**
     * Crea una rutina asignada a un niño por un tutor, calculando
     * automáticamente su posición de orden dentro de la franja horaria.
     *
     * @param nombre    nombre descriptivo de la rutina
     * @param zona      franja horaria de la rutina
     * @param ninoId    identificador del niño al que se asigna
     * @param creadorId identificador del tutor que la crea
     * @return entidad {@link Rutina} persistida
     * @throws RuntimeException si no existe ningún niño con ese identificador
     */
    @Transactional
    public Rutina crearRutina(String nombre, ZonaHoraria zona, Long ninoId, Long creadorId) {
        Usuario nino = usuarioRepository.findById(ninoId)
                .orElseThrow(() -> new RuntimeException("Niño no encontrado"));

        // El orden se calcula como el número de rutinas existentes en esa franja + 1
        long orden = rutinaRepository
                .findByNinoIdAndZonaHorariaOrderByOrdenAsc(ninoId, zona).size();

        Rutina rutina = Rutina.builder()
                .nombre(nombre)
                .zonaHoraria(zona)
                .nino(nino)
                .completada(false)
                .orden((int) orden + 1)
                .build();

        return rutinaRepository.save(rutina);
    }

    /**
     * Marca una rutina como completada por el niño.
     *
     * @param rutinaId identificador de la rutina a completar
     * @throws RuntimeException si no existe ninguna rutina con ese identificador
     */
    @Transactional
    public void marcarCompletada(Long rutinaId) {
        Rutina rutina = rutinaRepository.findById(rutinaId)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));
        rutina.setCompletada(true);
        rutinaRepository.save(rutina);
    }

    /**
     * Elimina permanentemente una rutina por su identificador.
     *
     * @param rutinaId identificador de la rutina a eliminar
     */
    @Transactional
    public void eliminarRutina(Long rutinaId) {
        rutinaRepository.deleteById(rutinaId);
    }
}