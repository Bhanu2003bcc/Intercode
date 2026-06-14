package com.interview.platform.response;

import com.interview.platform.dto.UserDTO;

public record AuthResponse(String token, UserDTO user) {}
