package com.bonoya.platform.iam.domain.model.entities;

import com.bonoya.platform.iam.domain.model.valueobjects.Roles;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Getter
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, length = 20)
    private Roles name;

    public Role() {}

    public Role(Roles name) {
        this.name = name;
    }

    public String getStringName() { return name.name(); }

    public static Roles getDefaultRoleName() { return Roles.ROLE_EMISOR; }

    public static Roles toRoleNameFromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getDefaultRoleName();
        }
        try {
            return Roles.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return getDefaultRoleName();
        }
    }

    public static List<Role> validateRoleSet(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(); // Return empty list, the caller should handle default roles
        }
        return roles;
    }
}
