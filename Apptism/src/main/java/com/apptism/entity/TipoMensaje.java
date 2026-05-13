package com.apptism.entity;
/**
 * Tipo de mensaje almacenado en la tabla {@code mensajes}.
 *
 * <ul>
 *   <li>{@link #CHAT} – mensaje de conversación bidireccional entre niño y tutor.</li>
 *   <li>{@link #EMOCION} – pictograma emocional enviado por el niño,
 *       visible en el registro emocional del tutor.</li>
 * </ul>
 */
public enum TipoMensaje { EMOCION, CHAT }