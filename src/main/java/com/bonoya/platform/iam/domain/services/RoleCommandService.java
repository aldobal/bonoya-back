package com.bonoya.platform.iam.domain.services;

import com.bonoya.platform.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
