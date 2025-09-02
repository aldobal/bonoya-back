package com.bonoya.platform.iam.domain.model.commands;

import com.bonoya.platform.iam.domain.model.valueobjects.Roles;

import java.util.List;

public record SignUpCommand(String username, String password, List<Roles> roles) {
}
