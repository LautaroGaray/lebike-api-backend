package com.example.scaffold.service;

import com.example.scaffold.domain.Role;
import com.example.scaffold.domain.Users;
import com.example.scaffold.dto.auth.UserEditRequestDTO;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.mapper.UsersMapper;
import com.example.scaffold.repository.RoleRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UsersMapper usersMapper;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UsersMapper usersMapper,
                       PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.usersMapper = usersMapper;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<List<UserDTO>> findAll() {
        List<UserDTO> users = userRepository.findAll()
                .stream()
                .map(usersMapper::toDto)
                .collect(Collectors.toList());

        return users.isEmpty() ? Optional.empty() : Optional.of(users);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserDTO create(UserDTO userDTO) {
        String username = userDTO.getNickName();
        if (username != null && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        String email = userDTO.getEmail();
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        Users user = usersMapper.toEntity(userDTO);
        user.setRole(resolveRoleForCreate(userDTO));
        return usersMapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id).map(usersMapper::toDto);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(usersMapper::toDto);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Users> findByEmailDb(String email) {
        return userRepository.findByEmail(email).stream().findFirst();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> validateCredentials(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordHasher.matches(rawPassword, user.getPasswordHash()))
                .map(usersMapper::toDto);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(usersMapper::toDto);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Users> findByUsernameDB(String username){
        return userRepository.findByUsername(username).stream().findFirst();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> updateRole(Long userId, Long roleId, String roleName) {
        Optional<Role> resolvedRole = resolveRoleOptional(roleId, roleName);
        if (!resolvedRole.isPresent()) {
            return Optional.empty();
        }
        return updateById(userId, user -> user.setRole(resolvedRole.get()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<UserDTO> updateUserProfile(Long userId, UserEditRequestDTO request) {
        Users user = userRepository.findById(userId).orElse(null);
        if (user == null || request == null) {
            return Optional.empty();
        }

        String newEmail = StringUtils.hasText(request.getNewEmail()) ? request.getNewEmail().trim() : null;
        String newNickName = StringUtils.hasText(request.getNewNickName()) ? request.getNewNickName().trim() : null;
        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : null;

        if (newEmail == null && newNickName == null && password == null) {
            return Optional.empty();
        }

        if (newEmail != null && !user.getEmail().equalsIgnoreCase(newEmail)) {
            Optional<Users> userWithEmail = userRepository.findByEmail(newEmail);
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(user.getId())) {
                return Optional.empty();
            }
            user.setEmail(newEmail);
        }

        if (newNickName != null && !user.getUsername().equalsIgnoreCase(newNickName)) {
            Optional<Users> userWithNickName = userRepository.findByUsername(newNickName);
            if (userWithNickName.isPresent() && !userWithNickName.get().getId().equals(user.getId())) {
                return Optional.empty();
            }
            user.setUsername(newNickName);
        }

        if (password != null) {
            user.setPasswordHash(passwordHasher.hash(password));
        }

        return Optional.of(usersMapper.toDto(userRepository.save(user)));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    private Optional<UserDTO> updateById(Long userId, Consumer<Users> updater) {
        return userRepository.findById(userId)
                .map(user -> {
                    updater.accept(user);
                    return userRepository.save(user);
                })
                .map(usersMapper::toDto);
    }

    private Role resolveRoleForCreate(UserDTO userDTO) {
        String requestedRoleName = StringUtils.hasText(userDTO.getRoleName())
                ? userDTO.getRoleName()
                : (userDTO.getRole() != null ? userDTO.getRole().name() : null);

        boolean hasRequestedRole = userDTO.getRoleId() != null || StringUtils.hasText(requestedRoleName);
        if (hasRequestedRole) {
            return resolveRoleOptional(userDTO.getRoleId(), requestedRoleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        }

        return roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing"));
    }

    private Optional<Role> resolveRoleOptional(Long roleId, String roleName) {
        if (roleId != null) {
            return roleRepository.findById(roleId);
        }
        if (StringUtils.hasText(roleName)) {
            return roleRepository.findByName(roleName);
        }
        return Optional.empty();
    }
}
