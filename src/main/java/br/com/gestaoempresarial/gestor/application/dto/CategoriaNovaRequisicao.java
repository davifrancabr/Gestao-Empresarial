package br.com.gestaoempresarial.gestor.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public record CategoriaNovaRequisicao(@NotBlank String name) {
}
