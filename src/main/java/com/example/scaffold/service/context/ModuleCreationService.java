package com.example.scaffold.service.context;

import com.example.scaffold.domain.context.Module;
import com.example.scaffold.repository.ModuleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ModuleCreationService {
    private final ModuleRepository moduleRepository;

    public ModuleCreationService(ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public Module create(String name, Long parentId) {
        if (moduleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Module already exists with name: " + name);
        }
        
        Module module = new Module();
        module.setName(name);
        module.setParentId(parentId);
        return moduleRepository.save(module);
    }

    @Cacheable(value = "modules", key = "#name")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Module> findByName(String name) {
        return moduleRepository.findByName(name);
    }

    @Cacheable(value = "modules", key = "#mainId")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Module> findByMainId(String mainId) {
        return moduleRepository.findByMainId(mainId);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Module> findById(Long id) {
        return moduleRepository.findById(id);
    }
}
