package com.bonoya.platform.iam.application.internal.commandservices;

import com.bonoya.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.bonoya.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.bonoya.platform.iam.domain.model.aggregates.User;
import com.bonoya.platform.iam.domain.model.commands.SignInCommand;
import com.bonoya.platform.iam.domain.model.commands.SignUpCommand;
import com.bonoya.platform.iam.domain.services.UserCommandService;
import com.bonoya.platform.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.bonoya.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.bonoya.platform.shared.application.exceptions.InvalidValueException;
import com.bonoya.platform.shared.application.exceptions.ResourceAlreadyException;
import com.bonoya.platform.shared.application.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(UserRepository userRepository, HashingService hashingService, TokenService tokenService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            throw new ResourceAlreadyException("Username already exists");
        
        // Get the role entities from the database based on the role names
        var roleEntities = command.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .toList();
        
        var user = new User(command.username(), hashingService.encode(command.password()), roleEntities);
        userRepository.save(user);
        return userRepository.findByUsername(command.username());
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username());
        if (user.isEmpty()) throw new ResourceNotFoundException("User not found");
        if (!hashingService.matches(command.password(), user.get().getPassword()))
            throw new InvalidValueException("Invalid password");
        var currentUser = user.get();
        var token = tokenService.generateToken(currentUser.getUsername());
        return Optional.of(ImmutablePair.of(currentUser, token));
    }
}
