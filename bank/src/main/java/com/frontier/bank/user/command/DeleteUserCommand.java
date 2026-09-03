package com.frontier.bank.user.command;

import java.util.UUID;

/**
 * Comando: remover usuário (exclusão física; saldo é removido por cascata).
 */
public record DeleteUserCommand(UUID id) {
}
