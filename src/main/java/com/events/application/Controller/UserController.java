package com.events.application.Controller;

import com.events.application.DTO.EmailDTO;
import com.events.application.Jwt.JwtService;
import com.events.application.Model.UserEntity;
import com.events.application.Repository.UserRepository;
import com.events.application.Service.MailService;
import com.events.application.Service.UserService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequestMapping("/api/users")
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private MailService mailService;


    @PostMapping("/send-otp-to-new-mail")
    public ResponseEntity<?> sendOtpToNewMail(@RequestHeader("Authorization") String token,@RequestBody EmailDTO emailDTO) {
        if(token==null || !token.startsWith("Bearer")){
            return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
        }
        return mailService.sendOtp(token,emailDTO.getEmail());
    }

    @PutMapping("/updateEmail")
    public ResponseEntity<?> updateUserEmail(@RequestHeader("Authorization") String token, @RequestBody String otp) {
        if(token==null || !token.startsWith("Bearer")){
            return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
        }
        String authToken = token.replace("Bearer ", "");
        return userService.validateOtpAndUpdateEmail(authToken,otp);
    }
}
