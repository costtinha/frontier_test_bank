package com.frontier.bank.common.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Codifica e decodifica o cursor de paginação.
 * <p>
 * Formato: {@code Base64URL(epochMillis:uuid)} — ex.: {@code MTczOTAwMDAwMDAwMDphYmNk...}.
 * O par {@code (created_at, id)} é estável e único, permitindo navegação
 * determinística para frente e para trás mesmo com inserções concorrentes.
 */
public final class CursorCodec {

	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private CursorCodec() {
	}

	public static String encode(Instant createdAt, UUID id) {
		String raw = createdAt.toEpochMilli() + ":" + id;
		return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @param cursor cursor em Base64URL
	 * @return o par {@code (created_at, id)} decodificado
	 * @throws IllegalArgumentException se o cursor for inválido
	 */
	public static Cursor decode(String cursor) {
		try {
			String raw = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
			String[] parts = raw.split(":", -1);
			if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
				throw new IllegalArgumentException("formato de cursor inválido");
			}
			Instant createdAt = Instant.ofEpochMilli(Long.parseLong(parts[0]));
			UUID id = UUID.fromString(parts[1]);
			return new Cursor(createdAt, id);
		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Cursor inválido: " + cursor, e);
		}
	}

	public record Cursor(Instant createdAt, UUID id) {
	}

}
