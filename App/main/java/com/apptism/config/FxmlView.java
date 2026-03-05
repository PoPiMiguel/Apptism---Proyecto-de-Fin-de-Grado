package com.apptism.config;

public enum FxmlView {
    ADMIN {
        @Override public String getFxmlPath() { return "/fxml/admin.fxml"; }
        @Override public String getTitle()    { return "Apptism - Administración"; }
    },
    LOGIN {
        @Override public String getFxmlPath() { return "/fxml/login.fxml"; }
        @Override public String getTitle()    { return "Apptism - Acceso"; }
    },
    DASHBOARD {
        @Override public String getFxmlPath() { return "/fxml/dashboard.fxml"; }
        @Override public String getTitle()    { return "Apptism - Panel Principal"; }
    },
    RUTINAS {
        @Override public String getFxmlPath() { return "/fxml/rutinas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Rutinas"; }
    },
    TAREAS {
        @Override public String getFxmlPath() { return "/fxml/tareas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Tareas"; }
    },
    RECOMPENSAS {
        @Override public String getFxmlPath() { return "/fxml/recompensas.fxml"; }
        @Override public String getTitle()    { return "Apptism - Recompensas"; }
    },
    EMOCIONES {
        @Override public String getFxmlPath() { return "/fxml/emociones.fxml"; }
        @Override public String getTitle()    { return "Apptism - Mis Emociones"; }
    },
    REGISTRO_EMOCIONAL {
        @Override public String getFxmlPath() { return "/fxml/registro_emocional.fxml"; }
        @Override public String getTitle()    { return "Apptism - Registro Emocional"; }
    },
    CHAT {
        @Override public String getFxmlPath() { return "/fxml/chat.fxml"; }
        @Override public String getTitle()    { return "Apptism - Conversación"; }
    },
    SOLICITUDES_CANJE {
        @Override public String getFxmlPath() { return "/fxml/solicitudes_canje.fxml"; }
        @Override public String getTitle()    { return "Apptism - Solicitudes de Canje"; }
    };

    public abstract String getFxmlPath();
    public abstract String getTitle();
}
