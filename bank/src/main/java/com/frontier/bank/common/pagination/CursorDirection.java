package com.frontier.bank.common.pagination;

/**
 * Direção da navegação na paginação por cursor.
 * <ul>
 *   <li>{@link #FORWARD}: primeira página ou próxima página (mais antigas → mais recentes).</li>
 *   <li>{@link #BACKWARD}: última página ou página anterior (mais recentes → mais antigas).</li>
 * </ul>
 */
public enum CursorDirection {

	FORWARD,
	BACKWARD

}
