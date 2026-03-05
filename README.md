# 🧩 Apptism

> Aplicación de escritorio para apoyar el desarrollo y la autonomía de niños con Trastorno del Espectro Autista (TEA), facilitando la comunicación entre tutores (padres/profesores) y sus alumnos o hijos.

---

## 📋 Descripción General

**Apptism** es una aplicación de escritorio desarrollada como Trabajo de Fin de Grado (TFG). Su objetivo principal es proporcionar una herramienta accesible e intuitiva que ayude a niños con TEA a:

- Gestionar sus **rutinas diarias** mediante pictogramas de la API de ARASAAC.
- Completar **tareas** asignadas por sus tutores y ganar puntos.
- Registrar su **estado emocional** de forma visual.
- Comunicarse con sus tutores a través de un **chat con pictogramas**.
- Canjear **recompensas** con los puntos acumulados.

Los tutores (padres y profesores) pueden crear, supervisar y gestionar todo el contenido de sus alumnos/hijos desde la misma interfaz.

---

## 🏗️ Arquitectura y Tecnologías

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Interfaz gráfica | JavaFX 21.0.2 + FXML |
| Framework backend | Spring Boot 3.2.3 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | MySQL 8 |
| Pictogramas | API REST de ARASAAC |
| Build | Maven |
| Utilidades | Lombok, Jackson |

La arquitectura sigue el patrón **MVC** adaptado a JavaFX con Spring:

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

## 👥 Roles de Usuario

| Rol | Descripción |
|---|---|
| `NINO` | Usuario principal. Completa tareas, rutinas, registra emociones y canjea recompensas. |
| `PADRE` | Tutor familiar. Crea y supervisa tareas, rutinas y recompensas de sus hijos. |
| `PROFESOR` | Tutor educativo. Igual que el padre, con acceso a sus alumnos. |
| `ADMIN` | Administrador del sistema. Gestiona todos los usuarios de la plataforma. |

---

## ✨ Funcionalidades Principales

### Para Niños (`NINO`)
- **Dashboard** personalizado con puntos acumulados y acceso rápido a todas las secciones.
- **Tareas**: visualización y marcado de tareas completadas. Al completar una tarea se suman puntos automáticamente.
- **Rutinas**: listado de rutinas divididas por zona horaria (Mañana, Mediodía, Noche) con pasos visuales mediante pictogramas.
- **Registro Emocional**: selección del estado emocional actual con pictogramas de ARASAAC. Historial de emociones registradas.
- **Recompensas**: catálogo de recompensas canjeables con los puntos acumulados. Envío de solicitudes de canje al tutor.
- **Chat**: envío de mensajes con pictogramas al tutor (modo EMOCION y modo CHAT bidireccional).

### Para Tutores (`PADRE` / `PROFESOR`)
- **Dashboard** con badge de notificación de solicitudes de canje pendientes.
- **Tareas**: creación, edición y eliminación de tareas para sus hijos/alumnos, con categoría y puntos configurables.
- **Rutinas**: creación de rutinas con pasos ordenados, zona horaria y pictogramas de ARASAAC.
- **Recompensas**: gestión del catálogo de recompensas (crear, activar/desactivar).
- **Solicitudes de Canje**: revisión y aprobación o rechazo de solicitudes enviadas por los niños.
- **Chat**: comunicación bidireccional con pictogramas.

### Para Administradores (`ADMIN`)
- Gestión completa de usuarios: alta, edición, baja y asignación de roles.
- Vista de todos los usuarios del sistema.

---

## 🔌 Integración con ARASAAC

La aplicación se integra con la **API pública de ARASAAC** (`https://api.arasaac.org/v1/pictograms`) para obtener pictogramas en español. El servicio `ArasaacService` implementa:

- Búsqueda de pictogramas por palabra clave.
- Carga de emociones básicas con pictogramas reales.
- **Caché en memoria** (`ConcurrentHashMap`) para evitar peticiones repetidas y reducir la latencia.
- **Fallback con emojis Unicode** si la API no está disponible.

---

## 🗄️ Modelo de Datos

Las entidades principales son:

| Entidad | Tabla | Descripción |
|---|---|---|
| `Usuario` | `usuarios` | Usuarios del sistema (todos los roles). |
| `Tarea` | `tareas` | Tareas asignadas a un niño por un creador. |
| `Rutina` | `rutinas` | Rutinas con pasos ordenados para un niño. |
| `PasoRutina` | `pasos_rutina` | Pasos individuales de una rutina (con pictograma). |
| `Recompensa` | `recompensas` | Recompensas canjeables asociadas a una familia. |
| `SolicitudCanje` | `solicitudes_canje` | Solicitudes de canje de recompensas por un niño. |
| `RegistroEmocional` | `registros_emocionales` | Historial de emociones de un niño. |
| `Mensaje` | `mensajes` | Mensajes de chat entre usuarios (con pictogramas). |
| `tutores_ninos` | `tutores_ninos` | Tabla de unión tutor ↔ niño (Many-to-Many). |

---

## ⚙️ Instalación y Configuración

### Requisitos previos
- Java 17+
- Maven 3.8+
- MySQL 8+

### Pasos

**1. Clonar el repositorio**
```bash
git clone https://github.com/tu-usuario/apptism.git
cd apptism/Apptism
```

**2. Crear la base de datos**
```sql
CREATE DATABASE apptism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**3. Configurar la conexión** en `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/apptism
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASEÑA
spring.jpa.hibernate.ddl-auto=update
```

**4. Compilar y ejecutar**
```bash
mvn clean javafx:run
```

El sistema creará automáticamente el usuario administrador por defecto:
- **Email**: `admin@apptism.com`
- **Contraseña**: `admin123`

---

## 📁 Estructura del Proyecto

```
Apptism/
├── src/
│   └── main/
│       ├── java/com/apptism/
│       │   ├── ApptismApp.java          # Punto de entrada Spring Boot + JavaFX
│       │   ├── Main.java                # Lanzador JavaFX
│       │   ├── config/
│       │   │   ├── ApplicationConfig.java
│       │   │   └── FxmlView.java        # Enum de vistas FXML
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
│       │   ├── entity/                  # Entidades JPA
│       │   ├── service/                 # Servicios de negocio
│       │   └── ui/
│       │       ├── StageManager.java    # Gestión de ventanas JavaFX
│       │       └── AnimacionUtil.java   # Utilidades de animación
│       └── resources/
│           ├── fxml/                    # Vistas de la interfaz
│           ├── styles/
│           │   └── apptism.css          # Estilos globales
│           ├── images/
│           │   └── logo.png
│           ├── data.sql                 # Datos iniciales (admin)
│           └── application.properties
└── pom.xml
```

---

## 🎨 Interfaz de Usuario

La interfaz está construida con **JavaFX + FXML** y un CSS personalizado (`apptism.css`). Las vistas disponibles son:

| Vista | Archivo FXML | Descripción |
|---|---|---|
| Login | `login.fxml` | Pantalla de inicio de sesión |
| Dashboard | `dashboard.fxml` | Panel principal (diferente para niño y tutor) |
| Tareas | `tareas.fxml` | Gestión de tareas |
| Rutinas | `rutinas.fxml` | Gestión de rutinas con pictogramas |
| Emociones | `emociones.fxml` | Registro de emoción actual |
| Registro Emocional | `registro_emocional.fxml` | Historial de emociones |
| Recompensas | `recompensas.fxml` | Catálogo de recompensas |
| Solicitudes de Canje | `solicitudes_canje.fxml` | Revisión de solicitudes |
| Chat | `chat.fxml` | Chat con pictogramas |
| Administración | `admin.fxml` | Panel de administración |

---

## 📄 Licencia

Proyecto académico desarrollado como Trabajo de Fin de Grado. Todos los pictogramas son propiedad de [ARASAAC](https://arasaac.org) y están sujetos a su licencia Creative Commons BY-NC-SA.

---

*Desarrollado con ❤️ como TFG — Apptism © 2024*
