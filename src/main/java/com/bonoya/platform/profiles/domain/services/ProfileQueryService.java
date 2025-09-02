package com.bonoya.platform.profiles.domain.services;

import com.bonoya.platform.profiles.domain.model.aggregates.Profile;
import com.bonoya.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import com.bonoya.platform.profiles.domain.model.queries.GetProfileByEmailQuery;
import com.bonoya.platform.profiles.domain.model.queries.GetProfileByIdQuery;

import java.util.List;
import java.util.Optional;

public interface ProfileQueryService {
  Optional<Profile> handle(GetProfileByEmailQuery query);
  Optional<Profile> handle(GetProfileByIdQuery query);
  List<Profile> handle(GetAllProfilesQuery query);
}
