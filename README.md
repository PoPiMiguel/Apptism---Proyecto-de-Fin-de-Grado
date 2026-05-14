# Apptism

> Aplicación de escritorio para apoyar el desarrollo y la autonomía de personas con Trastorno del Espectro Autista (TEA), facilitando la comunicación entre tutores (padres/profesores) y sus alumnos o hijos.

---

## Descripción General

**Apptism** es una aplicación de escritorio desarrollada como Trabajo de Fin de Grado (TFG). Su objetivo principal es proporcionar una herramienta accesible e intuitiva que ayude a personas con TEA a:

- Gestionar sus **rutinas diarias** mediante pictogramas de la API de ARASAAC.
- Completar **tareas** asignadas por sus tutores y ganar puntos.
- Registrar su **estado emocional** de forma visual.
- Comunicarse con sus tutores a través de un **chat con pictogramas**.
- Canjear **recompensas** con los puntos acumulados.

Los tutores (padres y profesores) pueden crear, supervisar y gestionar todo el contenido de sus alumnos/hijos desde la misma interfaz.

---

## Arquitectura y Tecnologías

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Interfaz gráfica | JavaFX 21.0.2 + FXML |
| Framework backend | Spring Boot 3.2.3 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL 8 (H2 en tests) |
| Pictogramas | API REST de ARASAAC |
| Build | Maven |
| Utilidades | Lombok, Jackson Databind, javafx-swing |
| Control de versiones | Git / GitHub |

La arquitectura sigue el patrón **MVC** en capas adaptado a JavaFX con Spring:

```
com.apptism
├── entity/         # Entidades JPA (modelo de datos)
├── repository/     # Repositorios Spring Data
├── service/        # Lógica de negocio
├── controller/     # Controladores JavaFX (FXML)
├── config/         # Configuración Spring + JavaFX
└── ui/             # Utilidades de interfaz (StageManager, AnimacionUtil)
```

---

## Flujo de Arranque

La aplicación arranca en este orden antes de mostrar ninguna pantalla:

1. `Main.main()` lanza JavaFX pasándole `Main.AppLauncher` como clase de arranque.
2. `Main.AppLauncher.start()` crea e inicializa `ApptismApp`.
3. `ApptismApp.init()` arranca el contexto de Spring Boot (en hilo separado al de UI).
4. `ApptismApp.start()` obtiene el `StageManager` del contexto de Spring y navega al login.
5. Al cerrar la aplicación, `ApptismApp.stop()` cierra el contexto de Spring limpiamente, liberando las conexiones a base de datos.

> **Nota:** el doble salto `Main → AppLauncher → ApptismApp` es necesario porque JavaFX requiere que su hilo de interfaz arranque de una forma concreta, incompatible con el arranque directo de Spring Boot.

---

## Roles de Usuario

| Rol | Descripción |
|---|---|
| `NINO` | Usuario principal. Completa tareas, rutinas, registra emociones y canjea recompensas. |
| `PADRE` | Tutor familiar. Crea y supervisa tareas, rutinas y recompensas de sus hijos. |
| `PROFESOR` | Tutor educativo. Mismas capacidades que el padre, con acceso a sus alumnos. |
| `ADMIN` | Administrador del sistema. Gestiona todos los usuarios y vínculos de la plataforma. |

---

## Funcionalidades Principales

### Para Niños (`NINO`)
- **Dashboard** personalizado con puntos acumulados y acceso rápido a todas las secciones.
- **Tareas**: visualización como tarjetas visuales y marcado como completadas. Al completar se suman puntos automáticamente.
- **Rutinas**: listado organizado por zona horaria (Mañana, Mediodía, Noche) con tarjetas visuales y marcado de completadas.
- **Emociones**: selección del estado emocional actual con pictogramas de ARASAAC y envío al tutor.
- **Recompensas**: catálogo de recompensas canjeables con los puntos acumulados. Muestra cuántos puntos faltan si no se tienen suficientes.
- **Chat**: comunicación bidireccional con el tutor mediante pictogramas de ARASAAC.

### Para Tutores (`PADRE` / `PROFESOR`)
- **Dashboard** con badge de notificación de solicitudes de canje pendientes.
- **Tareas**: creación y eliminación de tareas para sus niños, con título y puntos configurables.
- **Rutinas**: creación y eliminación de rutinas por zona horaria y niño destinatario.
- **Recompensas**: gestión del catálogo de recompensas (crear y listar).
- **Registro Emocional**: historial de pictogramas emocionales enviados por los niños, con gráfico de barras semanal por emoción y día.
- **Solicitudes de Canje**: historial de todas las recompensas canjeadas por sus niños.
- **Chat**: comunicación bidireccional con sus niños mediante pictogramas.

### Para Administradores (`ADMIN`)
- Creación de usuarios con nombre, email, contraseña y rol.
- Filtrado de usuarios por rol (todos, alumnos, padres, profesores).
- Eliminación de usuarios del sistema.
- Gestión de vínculos tutor ↔ niño: asignar y consultar relaciones.

---

## Integración con ARASAAC

La aplicación se integra con la **API pública de ARASAAC** para obtener pictogramas en español. El servicio `ArasaacService` implementa:

- Búsqueda de pictogramas por palabra clave (`https://api.arasaac.org/v1/pictograms/es/search/{palabra}`), con un máximo de 12 resultados.
- Carga de las imágenes PNG desde `https://static.arasaac.org/pictograms/{id}/{id}_500.png`.
- Carga automática de las 8 emociones básicas al abrir los módulos de emociones y chat: Alegre, Triste, Enfadado, Miedo, Tranquilo, Sorprendido, Cansado y Nervioso.
- **Caché en memoria** (`ConcurrentHashMap`) para evitar peticiones repetidas durante la sesión.
- Timeout de conexión de **8 segundos**. Si la API no responde, los métodos devuelven listas vacías o entradas con URL vacía para que la interfaz no quede en blanco.

---

## Modelo de Datos

La base de datos se llama `apptism_db`. Hibernate genera y actualiza el esquema automáticamente con `ddl-auto=update`.

| Entidad | Tabla | Descripción |
|---|---|---|
| `Usuario` | `usuarios` | Todos los perfiles del sistema diferenciados por rol. Almacena los puntos acumulados de cada niño. |
| `Tarea` | `tareas` | Tareas asignadas a un niño por un tutor, con título, pictograma y puntos por completar. |
| `Rutina` | `rutinas` | Rutinas organizadas por zona horaria, con pictograma y orden visual, asociadas a un niño. |
| `PasoRutina` | `pasos_rutina` | Pasos individuales de una rutina (modelo de datos definido; no implementado en la interfaz actual). |
| `Recompensa` | `recompensas` | Recompensas canjeables creadas por un tutor. |
| `SolicitudCanje` | `solicitudes_canje` | Registro de cada canje realizado por un niño. |
| `Mensaje` | `mensajes` | Mensajes de chat y emocionales entre usuarios, diferenciados por tipo (`CHAT` o `EMOCION`). |
| — | `tutores_ninos` | Tabla de unión tutor ↔ niño (Many-to-Many). |

### Enumerados

| Enum | Valores |
|---|---|
| `RolUsuario` | `NINO`, `PADRE`, `PROFESOR`, `ADMIN` |
| `ZonaHoraria` | `MANANA`, `MEDIODIA`, `NOCHE` |
| `TipoMensaje` | `CHAT`, `EMOCION` |
| `EstadoSolicitud` | `PENDIENTE`, `APROBADA`, `RECHAZADA` |

---

## Configuración

La aplicación usa perfiles de Spring para gestionar distintos entornos. El perfil activo por defecto es `produccion` (definido en `application.properties`).

| Perfil | Fichero | Descripción |
|---|---|---|
| `produccion` | `application-produccion.properties` | Base de datos principal de producción (Railway) |
| `colon` | `application-colon.properties` | Base de datos alternativa (Railway) |
| `pruebas` | `application-pruebas.properties` | Base de datos de pruebas (Railway) |

Para cambiar el perfil activo, modificar en `application.properties`:

```properties
spring.profiles.active=produccion
```

Al arrancar por primera vez, `data.sql` inserta automáticamente el usuario administrador por defecto si no existe:

```
Email:    admin@apptism.com
Password: admin123
Rol:      ADMIN
```

---

## Estructura del Proyecto

```
Apptism/
├── src/
│   └── main/
│       ├── java/com/apptism/
│       │   ├── Main.java                             # Punto de entrada de la JVM + AppLauncher (clase interna)
│       │   ├── ApptismApp.java                       # Arranca Spring Boot y abre la ventana principal
│       │   ├── config/
│       │   │   ├── ApplicationConfig.java            # Registra el StageManager como bean de Spring
│       │   │   └── FxmlView.java                     # Enum con las rutas y títulos de cada vista
│       │   ├── controller/
│       │   │   ├── LoginController.java
│       │   │   ├── DashboardController.java
│       │   │   ├── TareasController.java
│       │   │   ├── RutinasController.java
│       │   │   ├── RecompensasController.java
│       │   │   ├── SolicitudesCanjeController.java
│       │   │   ├── RegistroEmocionalController.java
│       │   │   ├── EmocionesController.java
│       │   │   ├── ChatController.java
│       │   │   └── AdminController.java
│       │   ├── entity/
│       │   │   ├── Usuario.java
│       │   │   ├── Tarea.java
│       │   │   ├── Rutina.java
│       │   │   ├── PasoRutina.java
│       │   │   ├── Recompensa.java
│       │   │   ├── SolicitudCanje.java
│       │   │   ├── Mensaje.java
│       │   │   ├── RolUsuario.java
│       │   │   ├── ZonaHoraria.java
│       │   │   ├── TipoMensaje.java
│       │   │   └── EstadoSolicitud.java
│       │   ├── repository/
│       │   │   ├── UsuarioRepository.java
│       │   │   ├── TareaRepository.java
│       │   │   ├── RutinaRepository.java
│       │   │   ├── RecompensaRepository.java
│       │   │   ├── MensajeRepository.java
│       │   │   └── SolicitudCanjeRepository.java
│       │   ├── service/
│       │   │   ├── UsuarioService.java
│       │   │   ├── TareaService.java
│       │   │   ├── RutinaService.java
│       │   │   ├── RecompensaService.java
│       │   │   ├── MensajeService.java
│       │   │   ├── SolicitudCanjeService.java
│       │   │   └── ArasaacService.java
│       │   └── ui/
│       │       ├── StageManager.java                 # Gestiona la navegación entre pantallas (ventana 1280×800, mínimo 800×600)
│       │       └── AnimacionUtil.java                # Animaciones de refuerzo positivo (ABA)
│       └── resources/
│           ├── fxml/                                 # Vistas de la interfaz
│           ├── styles/
│           │   └── apptism.css                       # Estilos globales con tokens de paleta
│           ├── images/                               # Iconos de la aplicación
│           ├── data.sql                              # Usuario admin por defecto
│           ├── application.properties
│           ├── application-produccion.properties
│           ├── application-colon.properties
│           └── application-pruebas.properties
└── pom.xml
```

---

## Interfaz de Usuario

La interfaz está construida con **JavaFX + FXML** y un CSS personalizado (`apptism.css`) con tokens de paleta definidos en `.root`. La ventana arranca con tamaño **1280×800** y tiene un mínimo de **800×600**. Hay dos modos visuales:

- **Perfil niño**: botones de gran tamaño, tipografía clara y paleta de colores suaves para reducir la sobreestimulación sensorial.
- **Perfil tutor/admin**: interfaz más densa e informativa, adaptada a usuarios sin necesidades especiales.

| Vista | Archivo FXML | Descripción |
|---|---|---|
| Login | `login.fxml` | Pantalla de inicio de sesión |
| Dashboard | `dashboard.fxml` | Panel principal (diferente para niño y tutor) |
| Tareas | `tareas.fxml` | Gestión y visualización de tareas |
| Rutinas | `rutinas.fxml` | Gestión y visualización de rutinas |
| Emociones | `emociones.fxml` | Registro y envío de emoción actual (niño) |
| Registro Emocional | `registro_emocional.fxml` | Historial de emociones con gráfico semanal (tutor) |
| Recompensas | `recompensas.fxml` | Catálogo y canje de recompensas |
| Solicitudes de Canje | `solicitudes_canje.fxml` | Historial de canjes realizados (tutor) |
| Chat | `chat.fxml` | Chat bidireccional con pictogramas |
| Administración | `admin.fxml` | Gestión de usuarios y vínculos (admin) |

---

## Licencia

Proyecto académico desarrollado como Trabajo de Fin de Grado. Todos los pictogramas son propiedad de [ARASAAC](https://arasaac.org) y están sujetos a su licencia Creative Commons BY-NC-SA.
