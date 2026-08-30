package com.example.scaffold.service.auths;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.repository.PermissionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public Permissions create(String code, String name) {
        if (permissionRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Permission already exists with code: " + code);
        }
        return permissionRepository.save(new Permissions(code, name));
    }

    @Cacheable(value = "permissions", key = "#code")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Permissions> findByCode(String code) {
        return permissionRepository.findByCode(code);
    }

    @Cacheable(value = "permissions", key = "#id")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Permissions> findById(Long id) {
        return permissionRepository.findById(id);
    }
}
