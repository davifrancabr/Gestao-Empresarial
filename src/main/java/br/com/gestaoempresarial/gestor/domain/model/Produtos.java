package br.com.gestaoempresarial.gestor.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "produtos")
public class Produtos {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private String codigoBarras;

    @Column(nullable = false)
    private Integer estoqueMinimo;

    @Column(nullable = false)
    private Number precoVenda;
}
