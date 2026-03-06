# Diagramas de Base de Datos — Apptism

---

## 1. Diagrama Entidad-Relación

```mermaid
erDiagram

    USUARIO {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password
        VARCHAR rol
        VARCHAR nombre
        DATE fecha_nacimiento
        INT puntos_acumulados
    }

    TAREA {
        BIGINT id PK
        VARCHAR titulo
        VARCHAR descripcion
        VARCHAR categoria
        INT puntos_por_completar
        BOOLEAN completada
        DATETIME fecha_programada
        BIGINT nino_id FK
        BIGINT creador_id FK
    }

    RUTINA {
        BIGINT id PK
        VARCHAR nombre
        VARCHAR zona_horaria
        VARCHAR pictograma_url
        BOOLEAN completada
        INT orden
        BIGINT nino_id FK
        BIGINT creador_id FK
    }

    PASO_RUTINA {
        BIGINT id PK
        VARCHAR descripcion
        VARCHAR pictograma_url
        INT orden
        BOOLEAN completado
        BIGINT rutina_id FK
    }

    RECOMPENSA {
        BIGINT id PK
        VARCHAR descripcion
        INT puntos_necesarios
        VARCHAR pictograma_url
        BOOLEAN activa
        BIGINT familia_id FK
    }

    SOLICITUD_CANJE {
        BIGINT id PK
        VARCHAR estado
        BOOLEAN leida
        DATETIME fecha
        BIGINT nino_id FK
        BIGINT recompensa_id FK
    }

    MENSAJE {
        BIGINT id PK
        VARCHAR pictograma_url
        VARCHAR texto_pictograma
        VARCHAR tipo
        BOOLEAN leido
        DATETIME fecha
        BIGINT emisor_id FK
        BIGINT receptor_id FK
    }

    REGISTRO_EMOCIONAL {
        BIGINT id PK
        VARCHAR emocion
        INT intensidad
        DATETIME fecha
        BIGINT nino_id FK
    }

    TUTORES_NINOS {
        BIGINT tutor_id FK
        BIGINT nino_id FK
    }

    %% ── Relaciones ──────────────────────────────────────────────

    USUARIO ||--o{ TAREA             : "se asigna a (nino)"
    USUARIO ||--o{ TAREA             : "crea (creador)"
    USUARIO ||--o{ RUTINA            : "pertenece a (nino)"
    USUARIO ||--o{ RUTINA            : "crea (creador)"
    USUARIO ||--o{ RECOMPENSA        : "crea (familia/tutor)"
    USUARIO ||--o{ SOLICITUD_CANJE   : "solicita (nino)"
    USUARIO ||--o{ MENSAJE           : "emite (emisor)"
    USUARIO ||--o{ MENSAJE           : "recibe (receptor)"
    USUARIO ||--o{ REGISTRO_EMOCIONAL: "registra (nino)"
    USUARIO ||--o{ TUTORES_NINOS     : "es tutor"
    USUARIO ||--o{ TUTORES_NINOS     : "es niño"
    RUTINA  ||--o{ PASO_RUTINA       : "contiene"
    RECOMPENSA ||--o{ SOLICITUD_CANJE: "referenciada en"
```

---

## 2. Diagrama Relacional

```mermaid
erDiagram

    usuarios {
        BIGINT id PK
        VARCHAR email UK "NOT NULL"
        VARCHAR password "NOT NULL"
        ENUM rol "NOT NULL -- NINO|PADRE|PROFESOR|ADMIN"
        VARCHAR nombre "NOT NULL"
        DATE fecha_nacimiento
        INT puntos_acumulados "DEFAULT 0"
    }

    tutores_ninos {
        BIGINT tutor_id PK,FK
        BIGINT nino_id PK,FK
    }

    tareas {
        BIGINT id PK
        VARCHAR titulo "NOT NULL"
        VARCHAR descripcion
        ENUM categoria "MATEMATICAS|LENGUA|ARTE|JUEGO|HABITOS"
        INT puntos_por_completar "DEFAULT 10"
        BOOLEAN completada "DEFAULT false"
        DATETIME fecha_programada
        BIGINT nino_id FK "NOT NULL"
        BIGINT creador_id FK
    }

    rutinas {
        BIGINT id PK
        VARCHAR nombre "NOT NULL"
        ENUM zona_horaria "NOT NULL -- MANANA|MEDIODIA|NOCHE"
        VARCHAR pictograma_url
        BOOLEAN completada "DEFAULT false"
        INT orden
        BIGINT nino_id FK "NOT NULL"
        BIGINT creador_id FK
    }

    pasos_rutina {
        BIGINT id PK
        VARCHAR descripcion "NOT NULL"
        VARCHAR pictograma_url
        INT orden
        BOOLEAN completado "DEFAULT false"
        BIGINT rutina_id FK "NOT NULL"
    }

    recompensas {
        BIGINT id PK
        VARCHAR descripcion "NOT NULL"
        INT puntos_necesarios "NOT NULL"
        VARCHAR pictograma_url
        BOOLEAN activa "DEFAULT true"
        BIGINT familia_id FK
    }

    solicitudes_canje {
        BIGINT id PK
        ENUM estado "DEFAULT PENDIENTE"
        BOOLEAN leida "DEFAULT false"
        DATETIME fecha "DEFAULT NOW()"
        BIGINT nino_id FK "NOT NULL"
        BIGINT recompensa_id FK "NOT NULL"
    }

    mensajes {
        BIGINT id PK
        VARCHAR pictograma_url
        VARCHAR texto_pictograma
        ENUM tipo "EMOCION|CHAT"
        BOOLEAN leido "DEFAULT false"
        DATETIME fecha "DEFAULT NOW()"
        BIGINT emisor_id FK "NOT NULL"
        BIGINT receptor_id FK "NOT NULL"
    }

    registros_emocionales {
        BIGINT id PK
        ENUM emocion "NOT NULL -- FELIZ|TRISTE|ENFADADO|MIEDO|CALMA|IRA"
        INT intensidad "1-5"
        DATETIME fecha "DEFAULT NOW()"
        BIGINT nino_id FK "NOT NULL"
    }

    %% ── Claves foráneas ─────────────────────────────────────────

    usuarios          ||--o{ tutores_ninos        : "tutor_id"
    usuarios          ||--o{ tutores_ninos        : "nino_id"
    usuarios          ||--o{ tareas               : "nino_id"
    usuarios          ||--o{ tareas               : "creador_id"
    usuarios          ||--o{ rutinas              : "nino_id"
    usuarios          ||--o{ rutinas              : "creador_id"
    usuarios          ||--o{ recompensas          : "familia_id"
    usuarios          ||--o{ solicitudes_canje    : "nino_id"
    usuarios          ||--o{ mensajes             : "emisor_id"
    usuarios          ||--o{ mensajes             : "receptor_id"
    usuarios          ||--o{ registros_emocionales: "nino_id"
    rutinas           ||--o{ pasos_rutina         : "rutina_id"
    recompensas       ||--o{ solicitudes_canje    : "recompensa_id"
```

---

## 3. Descripción de tablas

### `usuarios`
Entidad central del sistema. El campo `rol` determina el comportamiento de la aplicación: los usuarios `NINO` acumulan puntos, completan tareas y rutinas; los `PADRE` y `PROFESOR` crean y supervisan; el `ADMIN` gestiona el sistema.

### `tutores_ninos`
Tabla intermedia de la relación **ManyToMany** entre tutores y niños. Un tutor puede tener varios niños asignados y un niño puede tener varios tutores (padre y profesor simultáneamente).

### `tareas`
Tareas académicas o de hábitos asignadas a un niño (`nino_id`), creadas por un tutor (`creador_id`). Al completarse suman `puntos_por_completar` a `usuarios.puntos_acumulados`.

### `rutinas`
Rutinas diarias organizadas por zona horaria (mañana, mediodía, noche). Cada rutina pertenece a un niño y puede ser creada por él mismo o por su tutor.

### `pasos_rutina`
Pasos ordenados dentro de una rutina. Cada paso puede llevar un pictograma de ARASAAC y se marca completado de forma independiente.

### `recompensas`
Premios creados por un tutor (`familia_id`) que los niños pueden canjear gastando sus puntos acumulados.

### `solicitudes_canje`
Registra cada intento de canje de un niño. El tutor puede aprobar o rechazar la solicitud. Los estados son `PENDIENTE`, `APROBADA` o `RECHAZADA`.

### `mensajes`
Soporta dos flujos de comunicación: mensajes de chat bidireccional (`tipo = CHAT`) y envío de emociones del niño al tutor (`tipo = EMOCION`). Todos los mensajes usan pictogramas de ARASAAC.

### `registros_emocionales`
Historial de emociones registradas por un niño, con intensidad del 1 al 5. Permite al tutor consultar la evolución emocional semanal en forma de gráfico.

---

## 4. Restricciones y reglas de integridad

| Restricción | Descripción |
|---|---|
| `usuarios.email` | `UNIQUE NOT NULL` — no se permiten emails duplicados |
| `tutores_ninos (tutor_id, nino_id)` | `PRIMARY KEY` compuesta — evita vínculos duplicados |
| `tareas.nino_id` | `NOT NULL` — toda tarea debe tener un niño asignado |
| `rutinas.nino_id` | `NOT NULL` — toda rutina pertenece a un niño |
| `pasos_rutina.rutina_id` | `NOT NULL` con `CASCADE DELETE` — los pasos se eliminan con su rutina |
| `solicitudes_canje.estado` | `DEFAULT PENDIENTE` — estado inicial siempre pendiente |
| `mensajes.emisor_id / receptor_id` | `NOT NULL` — todo mensaje necesita emisor y receptor |
| `registros_emocionales.emocion` | `NOT NULL` — el campo emoción es obligatorio |
