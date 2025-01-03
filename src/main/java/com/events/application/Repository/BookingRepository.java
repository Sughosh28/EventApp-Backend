package com.events.application.Repository;

import com.events.application.Model.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    Optional<BookingEntity> findByEventId(Long eventId);

    @Query("SELECT COALESCE(SUM(b.no_of_tickets), 0) FROM BookingEntity b WHERE b.event.id = :eventId")
    Long getTotalTicketsBookedForEvent(@Param("eventId") Long eventId);
    List<BookingEntity> findAllByEventId(Long eventId);


    @Query("SELECT b from BookingEntity b where b.user.id=:userId ")
    List<BookingEntity> findBookingsByUserId(@Param("userId") Long userId);


}
