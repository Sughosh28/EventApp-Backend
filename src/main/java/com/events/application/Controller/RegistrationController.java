package com.events.application.Controller;

import com.events.application.DTO.LoginDTO;
import com.events.application.Model.UserEntity;
import com.events.application.Repository.UserRepository;
import com.events.application.Service.RegistrationAndLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegistrationController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegistrationAndLoginService registrationAndLoginService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserEntity user) {
       return registrationAndLoginService.registerUser(user);
    }

    @PostMapping("/loginUser")
    public ResponseEntity<?> loginUser(@RequestBody LoginDTO loginDTO) {
        return registrationAndLoginService.loginUser(loginDTO);
    }


}
