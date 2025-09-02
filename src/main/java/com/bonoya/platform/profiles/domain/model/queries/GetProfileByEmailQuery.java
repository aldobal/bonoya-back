package com.bonoya.platform.profiles.domain.model.queries;

import com.bonoya.platform.profiles.domain.model.valueobjects.EmailAddress;

public record GetProfileByEmailQuery(EmailAddress emailAddress) {
}