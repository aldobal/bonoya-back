package com.bonoya.platform.iam.domain.model.queries;

import com.bonoya.platform.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles name) {
}
