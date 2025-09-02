package com.bonoya.platform.iam.interfaces.rest.transform;

import com.bonoya.platform.iam.domain.model.entities.Role;
import com.bonoya.platform.iam.interfaces.rest.resources.RoleResource;

public class RoleResourceFromEntityAssembler {
    public static RoleResource toResourceFromEntity(Role entity) {
        return new RoleResource(entity.getId(), entity.getStringName());

    }
}
