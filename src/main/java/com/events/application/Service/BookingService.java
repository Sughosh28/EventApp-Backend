package com.events.application.Service;

import com.events.application.Jwt.JwtService;
import com.events.application.Model.BookingEntity;
import com.events.application.Model.EventEntity;
import com.events.application.Model.UserEntity;
import com.events.application.Repository.BookingRepository;
import com.events.application.Repository.EventRepository;
import com.events.application.Repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MailService mailService;


    public ResponseEntity<?> getAllBookings() {
       List<BookingEntity> bookings= bookingRepository.findAll();
       if(bookings.isEmpty()){
           return new ResponseEntity<>("No bookings found", HttpStatus.NOT_FOUND);
       }
       return new ResponseEntity<>(bookings, HttpStatus.OK);
    }


    public ResponseEntity<?> getBookingsByEventId(Long eventId) {
        Optional<BookingEntity> bookings= bookingRepository.findByEventId(eventId);
        if(bookings.isEmpty()){
            return new ResponseEntity<>("No bookings found for this event.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(bookings.get(), HttpStatus.OK);
    }

    public ResponseEntity<?> bookTicket(Long event_id, BookingEntity bookingEntity, String authToken) throws MessagingException {
        String username;
        Long userId;
        userId = jwtService.extractUserId(authToken);
        username = jwtService.extractUsername(authToken);
        Optional<UserEntity> eventUser = userRepository.findById(userId);
        if (eventUser.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        UserEntity userEntity = eventUser.get();
        if (!Objects.equals(username, userEntity.getUsername())) {
            return new ResponseEntity<>("User not authorized", HttpStatus.UNAUTHORIZED);
        }
        Optional<EventEntity> events = eventRepository.findById(event_id);
        if (events.isEmpty()) {
            return new ResponseEntity<>("Event not found", HttpStatus.NOT_FOUND);
        }
        EventEntity eventEntity = events.get();
        try {
            if (bookingEntity.getNo_of_tickets() <= 0) {
                return ResponseEntity.badRequest().body("Number of tickets must be greater than 0");
            }
            if (eventEntity.getCapacity() < bookingEntity.getNo_of_tickets()) {
                return ResponseEntity.badRequest().body("Not enough tickets available");
            }
            BookingEntity bookEvent = new BookingEntity();
            bookEvent.setEvent(eventEntity);
            bookEvent.setUser(userEntity);
            bookEvent.setBooking_date(LocalDate.now());
            bookEvent.setBooking_time(LocalTime.now());
            bookEvent.setNo_of_tickets(bookingEntity.getNo_of_tickets());
            bookEvent.setTotal_price(getTicketPrice(bookingEntity.getNo_of_tickets(), eventEntity.getEvent_price()));
            eventEntity.setCapacity(eventEntity.getCapacity() - bookingEntity.getNo_of_tickets());
            eventRepository.save(eventEntity);
            bookingRepository.save(bookEvent);
            mailService.sendBookingConfirmationMail(userEntity.getEmail(), eventEntity.getEvent_name(), eventEntity.getEvent_location(), bookEvent.getNo_of_tickets(), bookEvent.getTotal_price(), bookEvent.getBooking_id(), bookEvent.getBooking_date(), bookEvent.getBooking_time());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Booking successful");
            response.put("event_name", eventEntity.getEvent_name());
            response.put("event_date", eventEntity.getEvent_date().toString());
            response.put("event_time", eventEntity.getEvent_time().toString());
            response.put("booking", bookEvent);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            mailService.sendBookingFailureMail(userEntity.getEmail());
            return new ResponseEntity<>("Booking failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getBookingsByBookingId(Long bookingId) {
        try {
            BookingEntity booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("No bookings found with ID: " + bookingId));

            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + ex.getMessage());
        }
    }

    public Double getTicketPrice(int no_of_tickets,double event_price){
        return no_of_tickets*event_price;
    }

    public ResponseEntity<?> getTotalTicketsBooked(Long eventId) {
        try {
            Long totalTickets = bookingRepository.getTotalTicketsBookedForEvent(eventId);

            if (totalTickets == null) {
                totalTickets = 0L;
            }
            Map<String, Object> response = new HashMap<>();
            response.put("eventId", eventId);
            response.put("totalTickets", totalTickets);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Some error occurred", HttpStatus.OK);
        }
    }

    public ResponseEntity<?> cancelBooking(Long bookingId) {
        try {
            Optional<BookingEntity> booking = bookingRepository.findById(bookingId);
            if (booking.isPresent()) {
                bookingRepository.deleteById(bookingId);
                return new ResponseEntity<>("Your tickets have been cancelled.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Booking not found.", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while cancelling the booking.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public ResponseEntity<?> getUserBookingHistory(Long userId) {
       Optional<UserEntity> user= userRepository.findById(userId);
       if(user.isEmpty()){
           return new ResponseEntity<>("User does not exist", HttpStatus.BAD_REQUEST);
       }
       List<BookingEntity> bookings= bookingRepository.findBookingsByUserId(userId);
       if(bookings.isEmpty()){
           return new ResponseEntity<>("No bookings found for this user", HttpStatus.NOT_FOUND);
       }
       return new ResponseEntity<>(bookings, HttpStatus.OK);
    }


    public ResponseEntity<?> checkTicketAvailability(Long eventId, Integer requestedTickets) {
        try {
            EventEntity event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            Long totalBookedTickets = bookingRepository.getTotalTicketsBookedForEvent(eventId);

            Integer availableTickets = (int) (event.getCapacity() - totalBookedTickets);

            Map<String, Object> response = new HashMap<>();
            response.put("eventId", eventId);
            response.put("eventName", event.getEvent_name());
            response.put("totalCapacity", event.getCapacity());
            response.put("bookedTickets", totalBookedTickets);
            response.put("availableTickets", availableTickets);
            response.put("requestedTickets", requestedTickets);
            response.put("isAvailable", availableTickets >= requestedTickets);

            if (availableTickets >= requestedTickets) {
                response.put("message", "Tickets are available for booking");
            } else {
                response.put("message", "Not enough tickets available");
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking ticket availability: " + e.getMessage());
        }
    }

    public ResponseEntity<?> sendEventCancellationMailToAllRegisteredUsers(Long eventId) {
        List<BookingEntity> bookings = bookingRepository.findAllByEventId(eventId);
        if (bookings.isEmpty()) {
            return new ResponseEntity<>("No bookings found for the event", HttpStatus.NOT_FOUND);
        }

        List<String> failedEmails = new ArrayList<>();

        for (BookingEntity booking : bookings) {
            try {
                UserEntity user = booking.getUser();
                EventEntity event = booking.getEvent();
                mailService.sendEventCancellationMail(user.getEmail(), event.getEvent_name(), event.getEvent_date());
            } catch (MessagingException e) {
                failedEmails.add(booking.getUser().getEmail());
            }
        }

        if (!failedEmails.isEmpty()) {
            return new ResponseEntity<>("Failed to send emails to: " + String.join(", ", failedEmails), HttpStatus.PARTIAL_CONTENT);
        }
        return new ResponseEntity<>("Cancellation emails sent successfully to all registered users", HttpStatus.OK);
    }

}
