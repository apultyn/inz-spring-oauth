package com.pultyn.spring_oauth.response;

import com.pultyn.spring_oauth.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String message;
    private UserDTO user;
}
