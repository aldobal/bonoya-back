package com.bonoya.platform.profiles.domain.services;

import com.bonoya.platform.iam.domain.model.aggregates.User;
import com.bonoya.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.bonoya.platform.profiles.domain.model.aggregates.Profile;
import com.bonoya.platform.profiles.domain.model.valueobjects.EmailAddress;
import com.bonoya.platform.profiles.domain.model.valueobjects.NombreContacto;
import com.bonoya.platform.profiles.domain.model.valueobjects.Password;
import com.bonoya.platform.profiles.domain.model.valueobjects.RazonSocial;
import com.bonoya.platform.profiles.domain.model.valueobjects.Ruc;
import com.bonoya.platform.profiles.infrastructure.persistence.jpa.repositories.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Profile createAndAssignProfile(Long userId, String ruc, String razonSocial, String email, String password, String nombreContacto) {
        Profile newProfile = new Profile(new Ruc(ruc), new RazonSocial(razonSocial), new EmailAddress(email), new Password(password), new NombreContacto(nombreContacto));
        profileRepository.save(newProfile);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        user.setProfile(newProfile);
        userRepository.save(user);

        return newProfile;
    }
}