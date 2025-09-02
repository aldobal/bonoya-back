package com.bonoya.platform.iam.interfaces.rest.transform;

import com.bonoya.platform.iam.domain.model.commands.SignInCommand;
import com.bonoya.platform.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource resource) {
        return new SignInCommand(resource.username(), resource.password());
    }
}
