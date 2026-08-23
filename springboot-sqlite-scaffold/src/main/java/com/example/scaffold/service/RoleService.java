package com.example.scaffold.service;

import com.example.scaffold.domain.Role;
import com.example.scaffold.repository.RoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role create(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        return roleRepository.save(new Role(name, description));
    }

    @Cacheable(value = "roles", key = "#name")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Cacheable(value = "roles", key = "#id")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }
}
