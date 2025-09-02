package com.bonoya.platform.iam.interfaces.rest.transform;

import com.bonoya.platform.iam.domain.model.commands.SignUpCommand;
import com.bonoya.platform.iam.domain.model.entities.Role;
import com.bonoya.platform.iam.domain.model.valueobjects.Roles;
import com.bonoya.platform.iam.interfaces.rest.resources.SignUpResource;

import java.util.List;

public class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        List<Roles> roleNames;
        
        // Check if roles are null or empty
        if (resource.roles() == null || resource.roles().isEmpty()) {
            roleNames = List.of(Role.getDefaultRoleName());
        } else {
            // Filter and convert valid role strings to Roles enum
            var validRoleNames = resource.roles().stream()
                    .filter(role -> role != null && !role.trim().isEmpty())
                    .map(Role::toRoleNameFromString)
                    .toList();
            
            // If no valid roles after filtering, use default
            roleNames = validRoleNames.isEmpty() ? List.of(Role.getDefaultRoleName()) : validRoleNames;
        }
        
        return new SignUpCommand(resource.username(), resource.password(), roleNames);
    }
}
