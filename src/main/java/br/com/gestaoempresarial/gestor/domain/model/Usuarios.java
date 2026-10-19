package br.com.gestaoempresarial.gestor.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuarios")
public class Usuarios {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NonNull
    private String nome;

    @NonNull
    @Email
    private String email;

    @EnumeratedValue
    @NonNull
    private UsuariosTipoEnum tipo;
}
