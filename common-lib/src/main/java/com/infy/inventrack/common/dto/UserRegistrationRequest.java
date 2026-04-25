package com.infy.inventrack.common.dto;
import com.infy.inventrack.common.enums.UserRole; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class UserRegistrationRequest { @NotBlank @Size(min=3,max=50) private String userName; @NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$") private String password; @NotBlank @Email private String email; @NotNull private UserRole role; }
