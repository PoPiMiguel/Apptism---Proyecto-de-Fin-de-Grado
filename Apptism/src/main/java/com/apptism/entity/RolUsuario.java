package com.apptism.entity;
/**
 * Roles posibles de un usuario en el sistema.
 *
 * <ul>
 *   <li>{@link #NINO} – usuario principal de la aplicación, interfaz adaptada.</li>
 *   <li>{@link #PADRE} – tutor en el entorno familiar.</li>
 *   <li>{@link #PROFESOR} – tutor en el entorno educativo.</li>
 *   <li>{@link #ADMIN} – administrador global del sistema.</li>
 * </ul>
 */
public enum RolUsuario { NINO, PADRE, PROFESOR, ADMIN }

