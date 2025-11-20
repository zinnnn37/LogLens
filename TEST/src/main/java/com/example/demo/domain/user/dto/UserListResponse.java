package com.example.demo.domain.user.dto;

import java.util.List;

public record UserListResponse(
        List<UserResponse> users,
        int totalCount
) {
}
