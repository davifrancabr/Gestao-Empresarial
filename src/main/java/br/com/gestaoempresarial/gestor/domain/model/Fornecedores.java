package br.com.gestaoempresarial.gestor.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fornecedores")
public class Fornecedores {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
