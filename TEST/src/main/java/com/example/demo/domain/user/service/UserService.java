package com.example.demo.domain.user.service;


import com.example.demo.domain.user.dto.*;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserListResponse getAllUsers();


    UserResponse getUserById(Long id);


    UserResponse updateUser(Long id, UpdateUserRequest request);


    void changePassword(Long id, ChangePasswordRequest request);

    void deleteUser(Long id);
}
