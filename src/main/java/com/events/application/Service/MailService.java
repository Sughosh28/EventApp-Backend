package com.events.application.Service;

import com.events.application.DTO.EmailDTO;
import com.events.application.Jwt.JwtService;
import com.events.application.Model.UserEntity;
import com.events.application.Repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class MailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    public void sendWelcomeMail(String email, String username) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("sughoshathreya1@gmail.com");
        helper.setSubject("Welcome to Event Management System");
        helper.setTo(email);
        helper.setReplyTo("sughoshathreya1@gmail.com");
        Context context = new Context();
        context.setVariable("username", username);
        String htmlContent = templateEngine.process("welcome", context);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendBookingConfirmationMail(String email, String eventName, String location, int noOfTickets, double totalPrice, Long bookingId, LocalDate eventDate, LocalTime eventTime) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("sughoshathreya1@gmail.com");
        helper.setSubject("Booking Confirmation");
        helper.setTo(email);
        helper.setReplyTo("sughoshathreya1@gmail.com");
        Context context = new Context();
        context.setVariable("eventName", eventName);
        context.setVariable("noOfTickets", noOfTickets);
        context.setVariable("location", location);
        context.setVariable("totalPrice", totalPrice);
        context.setVariable("bookingId", bookingId);
        context.setVariable("eventDate", eventDate);
        context.setVariable("eventTime", eventTime);
        String htmlContent = templateEngine.process("booking_success", context);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendBookingFailureMail(String email) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("sughoshathreya1@gmail.com");
        helper.setSubject("Booking Failure");
        helper.setTo(email);
        helper.setReplyTo("sughoshathreya1@gmail.com");
        Context context = new Context();
        String htmlContent = templateEngine.process("booking_failure", context);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendEventCancellationMail(String email, String eventName, LocalDate eventDate) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("sughoshathreya1@gmail.com");
        helper.setSubject("Event Cancellation");
        helper.setTo(email);
        helper.setReplyTo("sughoshathreya1@gmail.com");
        Context context = new Context();
        context.setVariable("eventName", eventName);
        context.setVariable("eventDate", eventDate);
        String htmlContent = templateEngine.process("event_cancellation.html", context);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }


    public ResponseEntity<?> sendOtp(String token,String email) {
        String username;
        try {
            String authToken = token.replace("Bearer ", "");
            username = jwtService.extractUsername(authToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        if(user.getEmail().equals(email)){
            return new ResponseEntity<>("User already exists with this email", HttpStatus.BAD_REQUEST);
        }
        String otp = String.valueOf(new SecureRandom().nextInt(900000) + 100000);
        user.setOtp(otp);
        user.setOtp_expiry(LocalDateTime.now().plusMinutes(5));
        user.setPending_email(email);
        userRepository.save(user);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("OTP for email verification");
        message.setFrom("sughoshathreya1@gmail.com");
        message.setTo(String.valueOf(email));
        message.setReplyTo("sughoshathreya1@gmail.com");
        message.setText("Your OTP for email verification is " + otp);
        mailSender.send(message);
        return new ResponseEntity<>("OTP has been sent to your entered email", HttpStatus.OK);

    }


}
