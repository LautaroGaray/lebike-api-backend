package com.example.scaffold.controller;

import com.example.scaffold.domain.Role;
import com.example.scaffold.domain.Users;
import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.auth.UserEditRequestDTO;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/users")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/find")
    public ResponseEntity<ResponseData> findAll(){

        List<UserDTO> usersList = userService.findAll().orElse(null);
        int usersFound = Objects.isNull(usersList) ? 0 : usersList.size();
        ResponseData responseData = new ResponseData();
        responseData.setData(usersList);
        responseData.setSuccess(usersFound > 0);
        responseData.setMessage(usersFound == 0?"No users founded":"Users retrieved successfully");
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody UserDTO userDTO) {
        if(Objects.isNull(userDTO)){
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "User data is null"));
        }

        String requesterRoleName = getRequesterRoleName();
        if (requesterRoleName == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester role"));
        }

        String requestedRoleName = StringUtils.hasText(userDTO.getRoleName())
                ? userDTO.getRoleName().trim().toUpperCase()
                : (userDTO.getRole() != null ? userDTO.getRole().name() : Role.USER);
        if (!Role.OWNER.equals(requesterRoleName) && Role.OWNER.equals(requestedRoleName)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only OWNER can create another OWNER"));
        }

        userDTO.setRoleName(requestedRoleName);

        if (!StringUtils.hasText(userDTO.getEmail()) || !StringUtils.hasText(userDTO.getPassword())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Email or password is empty"));
        }

        UserDTO existingUser = userService.findByEmail(userDTO.getEmail()).orElse(null);
        if (existingUser != null) {
            return ResponseEntity.status(409).body(new ResponseData(null, false, "User with this email already exists"));
        }

        UserDTO existingUserNickName = userService.findByUsername(userDTO.getNickName()).orElse(null);
        if (existingUserNickName != null) {
            return ResponseEntity.status(409).body(new ResponseData(null, false, "User with this nickname already exists"));
        }

        UserDTO createdUser;
        try {
            createdUser = userService.create(userDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }

        ResponseData responseData = new ResponseData();
        responseData.setMessage("User registered successfully");
        responseData.setData(createdUser);

        return ResponseEntity.ok(responseData);
    }

    @PutMapping("/edit")
    public ResponseEntity<ResponseData> edit(@RequestBody UserEditRequestDTO request) {
        if (Objects.isNull(request)) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "User data is null"));
        }

        String requesterRoleName = getRequesterRoleName();
        if (requesterRoleName == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester role"));
        }

        if (!StringUtils.hasText(request.getCurrentEmail()) && !StringUtils.hasText(request.getCurrentNickName())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Current email or nickname is required"));
        }

        Users existingUser = null;
        if (StringUtils.hasText(request.getCurrentEmail())) {
            existingUser = userService.findByEmailDb(request.getCurrentEmail()).orElse(null);
        }
        if (existingUser == null && StringUtils.hasText(request.getCurrentNickName())) {
            existingUser = userService.findByUsernameDB(request.getCurrentNickName()).orElse(null);
        }

        if (existingUser == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User does not exist"));
        }

        if (existingUser.getRole() != null
                && Role.OWNER.equals(existingUser.getRole().getName())
                && !Role.OWNER.equals(requesterRoleName)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only OWNER can modify an OWNER user"));
        }

        if (!StringUtils.hasText(request.getNewEmail()) && !StringUtils.hasText(request.getNewNickName()) && !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Nothing to update"));
        }

        UserDTO editedUser = userService.updateUserProfile(existingUser.getId(), request).orElse(null);
        if (Objects.isNull(editedUser)) {
            return ResponseEntity.status(409).body(new ResponseData(null, false, "Duplicate email or nickname"));
        }

        return ResponseEntity.ok(new ResponseData(editedUser, true, "User updated successfully"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ResponseData> delete(@RequestBody UserDTO userDto){

        if(Objects.isNull(userDto)){
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "User data is null"));
        }

        Users userDb = userService.findByEmailDb(userDto.getEmail()).orElse(null);
        if(Objects.isNull(userDb)){
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User with this email does not exist"));
        }

        userService.deleteById(userDb.getId());

        UserDTO userDbDeleted = userService.findById(userDb.getId()).orElse(null);
        if(!Objects.isNull(userDbDeleted)){
            return ResponseEntity.status(500).body(new ResponseData(null, false, "Failed to delete user"));
        }

        return ResponseEntity.ok(new ResponseData(null, true, "User deleted successfully"));
    }


    @PutMapping("/editRole")
    public ResponseEntity<ResponseData> editRole(@RequestBody UserDTO userDto){

        if(Objects.isNull(userDto)){
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "User data is null"));
        }

        String requesterRoleName = getRequesterRoleName();
        if (requesterRoleName == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester role"));
        }

        if(!StringUtils.hasText(userDto.getEmail())){
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Email is empty"));
        }

        if (userDto.getRoleId() == null && !StringUtils.hasText(userDto.getRoleName()) && userDto.getRole() == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Role id or role name is required"));
        }

        String requestedRoleName = StringUtils.hasText(userDto.getRoleName())
                ? userDto.getRoleName().trim().toUpperCase()
                : (userDto.getRole() != null ? userDto.getRole().name() : null);

        Users existingUser = userService.findByEmailDb(userDto.getEmail()).orElse(null);
        if(Objects.isNull(existingUser)){
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User with this email does not exist"));
        }

        if (existingUser.getRole() != null
                && Role.OWNER.equals(existingUser.getRole().getName())
                && !Role.OWNER.equals(requesterRoleName)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only OWNER can modify an OWNER user"));
        }

        UserDTO updatedUser = userService.updateRole(existingUser.getId(), userDto.getRoleId(), requestedRoleName).orElse(null);
        if(Objects.isNull(updatedUser)){
            return ResponseEntity.status(500).body(new ResponseData(null, false, "Failed to update role"));
        }

        return ResponseEntity.ok(new ResponseData(updatedUser, true, "Role updated successfully"));

    }


    private String getRequesterRoleName() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ATTRIBUTE)
                : null;

        return roleAttribute instanceof String ? ((String) roleAttribute).trim().toUpperCase() : null;
    }

}
