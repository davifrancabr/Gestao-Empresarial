package com.empresa.gestao.user.presentation.web;

import com.empresa.gestao.user.application.dto.UserCreateCommand;
import com.empresa.gestao.user.application.dto.UserResult;
import com.empresa.gestao.user.application.dto.UserUpdateCommand;
import com.empresa.gestao.user.application.usecase.CreateUserUseCase;
import com.empresa.gestao.user.application.usecase.ListUsersUseCase;
import com.empresa.gestao.user.application.usecase.UpdateUserUseCase;
import com.empresa.gestao.user.presentation.web.request.UserCreateRequest;
import com.empresa.gestao.user.presentation.web.request.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final ListUsersUseCase listUsers;
    private final CreateUserUseCase createUser;
    private final UpdateUserUseCase updateUser;

    public UserController(ListUsersUseCase listUsers, CreateUserUseCase createUser,
                          UpdateUserUseCase updateUser) {
        this.listUsers = listUsers;
        this.createUser = createUser;
        this.updateUser = updateUser;
    }

    @GetMapping
    public List<UserResult> list() {
        return listUsers.execute();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResult create(@Valid @RequestBody UserCreateRequest request) {
        return createUser.execute(new UserCreateCommand(request.name(), request.email(),
                request.password(), request.role(), request.storeIds()));
    }

    @PutMapping("/{id}")
    public UserResult update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return updateUser.execute(id, new UserUpdateCommand(request.name(), request.email(),
                request.password(), request.role(), request.active(), request.storeIds()));
    }
}
