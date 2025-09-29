package tacos.security;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Data;
import tacos.User;

@Data
public class RegistrationForm {

  @NotBlank(message = "Username is required")
  private String username;
  @NotBlank(message = "Password is required")
  private String password;
  @NotBlank(message = "Full name is required")
  private String fullname;
  @NotBlank(message = "Street is required")
  private String street;
  @NotBlank(message = "City is required")
  private String city;
  @NotBlank(message = "State is required")
  private String state;
  @NotBlank(message = "Zip is required")
  private String zip;
  @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number is invalid")
  private String phone;
  
  public User toUser(PasswordEncoder passwordEncoder) {
    return new User(
        username, passwordEncoder.encode(password), 
        fullname, street, city, state, zip, phone);
  }
  
}
