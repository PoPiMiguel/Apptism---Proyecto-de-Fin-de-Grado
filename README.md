# Apptism

> Aplicación de escritorio para apoyar el desarrollo y la autonomía de niños con Trastorno del Espectro Autista (TEA), facilitando la comunicación entre tutores (padres/profesores) y sus alumnos o hijos.

---

## Descripción General

**Apptism** es una aplicación de escritorio desarrollada como Trabajo de Fin de Grado (TFG). Su objetivo principal es proporcionar una herramienta accesible e intuitiva que ayude a niños con TEA a:

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
| Base de datos | MySQL 8 |
| Pictogramas | API REST de ARASAAC |
| Build | Maven |
| Utilidades | Lombok, Jackson Databind |
| Control de versiones | Git / GitHub |

La arquitectura sigue el patrón **MVC** en capas adaptado a JavaFX con Spring:

```
com.apptism
├── entity/         # Entidades JPA (modelo de datos)
├── repository/     # Repositorios Spring Data
├── service/        # Lógica de negocio
├── controller/     # Controladores JavaFX (FXML)
├── config/         # Configuración Spring + JavaFX
├── launcher/       # Lanzador con comprobación de MySQL
└── ui/             # Utilidades de interfaz (StageManager, AnimacionUtil)
```

---

## Flujo de Arranque

La aplicación arranca en este orden antes de mostrar ninguna pantalla:

1. `Main` lanza `LauncherFxBridge` (puente JavaFX).
2. `LauncherApp` comprueba si MySQL está disponible en el puerto 3306.
   - Si MySQL está corriendo → arranca la aplicación.
   - Si está instalado pero parado → intenta arrancarlo con `net start`.
   - Si no está instalado → muestra una guía de instalación paso a paso.
3. Una vez confirmada la conexión, `ApptismApp` arranca el contexto de Spring Boot y navega al login.

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
- **Tareas**: creación y eliminación de tareas para sus niños, con título, categoría y puntos configurables.
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

La aplicación se integra con la **API pública de ARASAAC** (`https://api.arasaac.org/v1/pictograms`) para obtener pictogramas en español. El servicio `ArasaacService` implementa:

- Búsqueda de pictogramas por palabra clave (máximo 12 resultados).
- Carga automática de las 8 emociones básicas al abrir los módulos de emociones y chat.
- **Caché en memoria** (`ConcurrentHashMap`) para evitar peticiones repetidas durante la sesión.
- **Fallback con emojis Unicode** si la API no está disponible, garantizando que la UI siempre muestre contenido.

---

## Modelo de Datos

La base de datos se llama `apptism_db`. Hibernate genera y actualiza el esquema automáticamente con `ddl-auto=update`.

| Entidad | Tabla | Descripción |
|---|---|---|
| `Usuario` | `usuarios` | Todos los perfiles del sistema diferenciados por rol. Almacena los puntos acumulados de cada niño. |
| `Tarea` | `tareas` | Tareas asignadas a un niño por un creador (tutor). |
| `Rutina` | `rutinas` | Rutinas organizadas por zona horaria, asociadas a un niño. |
| `PasoRutina` | `pasos_rutina` | Pasos individuales de una rutina, con pictograma y orden. |
| `Recompensa` | `recompensas` | Recompensas canjeables creadas por un tutor. |
| `SolicitudCanje` | `solicitudes_canje` | Registro de cada canje realizado por un niño. |
| `Mensaje` | `mensajes` | Mensajes de chat y emocionales entre usuarios (diferenciados por tipo: `CHAT` o `EMOCION`). |
| — | `tutores_ninos` | Tabla de unión tutor ↔ niño (Many-to-Many). |

### Enumerados

| Enum | Valores |
|---|---|
| `RolUsuario` | `NINO`, `PADRE`, `PROFESOR`, `ADMIN` |
| `CategoriaTarea` | `MATEMATICAS`, `LENGUA`, `ARTE`, `JUEGO`, `HABITOS` |
| `ZonaHoraria` | `MANANA`, `MEDIODIA`, `NOCHE` |
| `TipoMensaje` | `CHAT`, `EMOCION` |
| `EstadoSolicitud` | `PENDIENTE`, `APROBADA`, `RECHAZADA` |

---

## Configuración

La aplicación se conecta a MySQL con estos valores por defecto en `application.properties`:

```
URL:      jdbc:mysql://localhost:3306/apptism_db
Usuario:  admin
Password: admin
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
│       │   ├── ApptismApp.java               # Arranca Spring Boot y abre la ventana principal
│       │   ├── Main.java                     # Punto de entrada de la JVM
│       │   ├── config/
│       │   │   ├── ApplicationConfig.java    # Registra el StageManager como bean de Spring
│       │   │   └── FxmlView.java             # Enum con las rutas y títulos de cada vista
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
│       │   │   ├── CategoriaTarea.java
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
│       │   ├── launcher/
│       │   │   ├── LauncherApp.java          # Ventana de comprobación de MySQL
│       │   │   └── DatabaseChecker.java      # Comprueba si MySQL está disponible
│       │   └── ui/
│       │       ├── StageManager.java         # Gestiona la navegación entre pantallas
│       │       └── AnimacionUtil.java        # Animaciones de refuerzo positivo (ABA)
│       └── resources/
│           ├── fxml/                         # Vistas de la interfaz
│           ├── styles/
│           │   └── apptism.css              # Estilos globales con tokens de paleta
│           ├── images/                       # Iconos de la aplicación (64, 128, 256, 512px)
│           ├── data.sql                      # Usuario admin por defecto
│           └── application.properties
└── pom.xml
```

---

## Interfaz de Usuario

La interfaz está construida con **JavaFX + FXML** y un CSS personalizado (`apptism.css`) con tokens de paleta definidos en `.root`. Hay dos modos visuales:

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
