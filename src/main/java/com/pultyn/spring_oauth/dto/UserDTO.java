package com.pultyn.spring_oauth.dto;

import com.pultyn.spring_oauth.model.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String role;

    public UserDTO(UserEntity user)
    {
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole().name();
    }
}
