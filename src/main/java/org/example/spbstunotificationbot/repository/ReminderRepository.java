package org.example.spbstunotificationbot.repository;

import jakarta.persistence.LockModeType;
import org.example.spbstunotificationbot.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByStatusOrderByRemindAtAsc(Reminder.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reminder r WHERE r.id = :id")
    Optional<Reminder> findByIdForUpdate(@Param("id") Long id);

    List<Reminder> findByStatusAndRemindAtLessThanEqual(Reminder.Status status, LocalDateTime dateTime);
}