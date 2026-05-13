package com.apptism.config;

/**
 * Enumeración de todas las pantallas de la aplicación.
 *
 * <p>Cada valor asocia una pantalla con su archivo FXML y el título
 * que debe mostrarse en la barra de la ventana. El {@link com.apptism.ui.StageManager}
 * usa esta enumeración para cargar y navegar entre vistas sin manejar
 * rutas de cadena directamente en el código.</p>
 */

public enum FxmlView {

    /** Panel de administración, solo accesible para el rol ADMIN. */

    ADMIN {
        @Override public String getFxmlPath() { return "/fxml/admin.fxml"; }
        @Override public String getTitle()    { return "Apptism - Administración"; }
    },

    /** Pantalla de inicio de sesión. */

    LOGIN {
        @Override public String getFxmlPath() { return "/fxml/login.fxml"; }
        @Override public String getTitle()    { return "Apptism - Acceso"; }
    },

    /** Panel principal, diferente según el rol del usuario. */

    DASHBOARD {
        @Override public String getFxmlPath() { return "/fxml/dashboard.fxml"; }
        @Override public String getTitle()    { return "Apptism - Panel Principal"; }
    },

    /** Pantalla de gestión y visualización de rutinas. */

    RUTINAS {
        @Override public String getFxmlPath() { return "/fxml/rutinas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Rutinas"; }
    },

    /** Pantalla de gestión y visualización de tareas. */

    TAREAS {
        @Override public String getFxmlPath() { return "/fxml/tareas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Tareas"; }
    },

    /** Pantalla de gestión y canje de recompensas. */

    RECOMPENSAS {
        @Override public String getFxmlPath() { return "/fxml/recompensas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Recompensas"; }
    },

    /** Pantalla de selección y envío de emociones mediante pictogramas (niño). */

    EMOCIONES {
        @Override public String getFxmlPath() { return "/fxml/emociones.fxml"; }
        @Override public String getTitle()    { return "Apptism - Mis Emociones"; }
    },

    /** Pantalla de historial emocional con gráfico de barras (tutor). */

    REGISTRO_EMOCIONAL {
        @Override public String getFxmlPath() { return "/fxml/registro_emocional.fxml"; }
        @Override public String getTitle()    { return "Apptism - Registro Emocional"; }
    },

    /** Pantalla de chat mediante pictogramas entre niño y tutor. */

    CHAT {
        @Override public String getFxmlPath() { return "/fxml/chat.fxml"; }
        @Override public String getTitle()    { return "Apptism - Conversación"; }
    },

    /** Pantalla del historial de solicitudes de canje (tutor). */

    SOLICITUDES_CANJE {
        @Override public String getFxmlPath() { return "/fxml/solicitudes_canje.fxml"; }
        @Override public String getTitle()    { return "Apptism - Solicitudes de Canje"; }
    };

    /**
     * Devuelve la ruta al archivo FXML de esta pantalla dentro del classpath.
     *
     * @return ruta FXML, por ejemplo {@code "/fxml/login.fxml"}
     */

    public abstract String getFxmlPath();

    /**
     * Devuelve el título que se muestra en la barra de la ventana para esta pantalla.
     *
     * @return título de la ventana
     */

    public abstract String getTitle();
}
