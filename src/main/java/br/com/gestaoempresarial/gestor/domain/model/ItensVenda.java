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
@Table(name = "itens_venda")
public class ItensVenda {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
