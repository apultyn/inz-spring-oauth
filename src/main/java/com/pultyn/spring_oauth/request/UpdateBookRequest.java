package com.pultyn.spring_oauth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBookRequest {
    @NotBlank(message = "Title required")
    @Size(max = 255, message = "Title may not exceed 255 characters")
    private String title;

    @NotBlank(message = "Author required")
    @Size(max = 255, message = "Author may not exceed 255 characters")
    private String author;
}