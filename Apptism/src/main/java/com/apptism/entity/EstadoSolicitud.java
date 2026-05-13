package com.apptism.entity;
/**
 * Estado de una solicitud de canje generada cuando un niño canjea una recompensa.
 *
 * <ul>
 *   <li>{@link #PENDIENTE} – el canje se ha realizado pero el tutor aún no lo ha revisado.</li>
 *   <li>{@link #APROBADA} – el tutor ha confirmado el canje.</li>
 *   <li>{@link #RECHAZADA} – el tutor ha denegado el canje.</li>
 * </ul>
 */
public enum EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA }
