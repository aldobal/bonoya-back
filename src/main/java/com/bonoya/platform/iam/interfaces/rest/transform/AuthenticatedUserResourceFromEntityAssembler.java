package com.bonoya.platform.iam.interfaces.rest.transform;

import com.bonoya.platform.iam.domain.model.aggregates.User;
import com.bonoya.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntity(User entity, String token) {
        return new AuthenticatedUserResource(entity.getId(), entity.getUsername(), token);
    }
}
