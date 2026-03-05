```mermaid
classDiagram
    direction TB

    %% ─── ENUMERACIONES ───────────────────────────────────────────
    class RolUsuario {
        <<enumeration>>
        NINO
        PADRE
        PROFESOR
        ADMIN
    }

    class ZonaHoraria {
        <<enumeration>>
        MANANA
        MEDIODIA
        NOCHE
    }

    class CategoriaTarea {
        <<enumeration>>
        MATEMATICAS
        LENGUA
        ARTE
        JUEGO
        HABITOS
    }

    class Emocion {
        <<enumeration>>
        FELIZ
        TRISTE
        ENFADADO
        MIEDO
        CALMA
        IRA
    }

    class EstadoSolicitud {
        <<enumeration>>
        PENDIENTE
        APROBADA
        RECHAZADA
    }

    class TipoMensaje {
        <<enumeration>>
        EMOCION
        CHAT
    }

    %% ─── ENTIDADES ───────────────────────────────────────────────
    class Usuario {
        +Long id
        +String email
        +String password
        +RolUsuario rol
        +String nombre
        +LocalDate fechaNacimiento
        +Integer puntosAcumulados
        +List~Usuario~ ninos
        +List~Rutina~ rutinas
        +List~Tarea~ tareas
    }

    class Tarea {
        +Long id
        +String titulo
        +String descripcion
        +CategoriaTarea categoria
        +Integer puntosPorCompletar
        +boolean completada
        +LocalDateTime fechaProgramada
        +Usuario nino
        +Usuario creador
    }

    class Rutina {
        +Long id
        +String nombre
        +ZonaHoraria zonaHoraria
        +String pictogramaUrl
        +boolean completada
        +Integer orden
        +Usuario nino
        +Usuario creador
        +List~PasoRutina~ pasos
    }

    class PasoRutina {
        +Long id
        +String descripcion
        +String pictogramaUrl
        +Integer orden
        +boolean completado
        +Rutina rutina
    }

    class Recompensa {
        +Long id
        +String descripcion
        +Integer puntosNecesarios
        +String pictogramaUrl
        +boolean activa
        +Usuario familia
    }

    class SolicitudCanje {
        +Long id
        +EstadoSolicitud estado
        +boolean leida
        +LocalDateTime fecha
        +Usuario nino
        +Recompensa recompensa
    }

    class RegistroEmocional {
        +Long id
        +Emocion emocion
        +Integer intensidad
        +LocalDateTime fecha
        +Usuario nino
    }

    class Mensaje {
        +Long id
        +String pictogramaUrl
        +String textoPictograma
        +TipoMensaje tipo
        +boolean leido
        +LocalDateTime fecha
        +Usuario emisor
        +Usuario receptor
    }

    %% ─── SERVICIOS ───────────────────────────────────────────────
    class UsuarioService {
        <<service>>
        +findAll() List~Usuario~
        +save(Usuario) Usuario
        +findByEmail(String) Optional~Usuario~
        +delete(Long) void
    }

    class TareaService {
        <<service>>
        +findByNino(Usuario) List~Tarea~
        +save(Tarea) Tarea
        +completar(Long) void
        +delete(Long) void
    }

    class RutinaService {
        <<service>>
        +findByNinoAndZona(Usuario, ZonaHoraria) List~Rutina~
        +save(Rutina) Rutina
        +delete(Long) void
    }

    class RecompensaService {
        <<service>>
        +findByFamilia(Usuario) List~Recompensa~
        +save(Recompensa) Recompensa
        +toggleActiva(Long) void
    }

    class SolicitudCanjeService {
        <<service>>
        +findPendientesByFamilia(Long) List~SolicitudCanje~
        +aprobar(Long) void
        +rechazar(Long) void
    }

    class MensajeService {
        <<service>>
        +findConversacion(Usuario, Usuario) List~Mensaje~
        +send(Mensaje) Mensaje
        +marcarLeidos(Long, Long) void
    }

    class ArasaacService {
        <<service>>
        -ConcurrentHashMap cache
        +buscar(String) List~PictogramaDTO~
        +getEmocionesBásicas() List~PictogramaDTO~
        +getImagenUrl(int) String
        +limpiarCache() void
    }

    %% ─── RELACIONES ──────────────────────────────────────────────
    Usuario "1" --> "0..*" Usuario : tutores_ninos (M:N)
    Usuario "1" --> "0..*" Tarea   : niño posee
    Usuario "1" --> "0..*" Tarea   : creador crea
    Usuario "1" --> "0..*" Rutina  : niño posee
    Usuario "1" --> "0..*" Rutina  : creador crea
    Usuario "1" --> "0..*" Recompensa : familia gestiona
    Usuario "1" --> "0..*" SolicitudCanje : niño solicita
    Usuario "1" --> "0..*" RegistroEmocional : niño registra
    Usuario "1" --> "0..*" Mensaje : emisor
    Usuario "1" --> "0..*" Mensaje : receptor

    Rutina "1" --> "0..*" PasoRutina : contiene
    Recompensa "1" --> "0..*" SolicitudCanje : referenciada en

    Tarea ..> CategoriaTarea
    Rutina ..> ZonaHoraria
    RegistroEmocional ..> Emocion
    SolicitudCanje ..> EstadoSolicitud
    Mensaje ..> TipoMensaje
    Usuario ..> RolUsuario

    UsuarioService ..> Usuario
    TareaService ..> Tarea
    RutinaService ..> Rutina
    RecompensaService ..> Recompensa
    SolicitudCanjeService ..> SolicitudCanje
    MensajeService ..> Mensaje
```
