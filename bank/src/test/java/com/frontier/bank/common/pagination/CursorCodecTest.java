package com.frontier.bank.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CursorCodecTest {

	@Test
	void shouldRoundTripCursor() {
		Instant createdAt = Instant.parse("2025-06-01T10:15:30.123Z");
		UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

		String encoded = CursorCodec.encode(createdAt, id);

		CursorCodec.Cursor decoded = CursorCodec.decode(encoded);
		assertThat(decoded.createdAt()).isEqualTo(createdAt);
		assertThat(decoded.id()).isEqualTo(id);
	}

	@Test
	void shouldRejectGarbageCursor() {
		assertThatThrownBy(() -> CursorCodec.decode("não-é-um-cursor"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectCursorWithoutIdPart() {
		// "123456789" codificado em Base64URL — falta o componente de UUID
		assertThatThrownBy(() -> CursorCodec.decode("MTIzNDU2Nzg5"))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
