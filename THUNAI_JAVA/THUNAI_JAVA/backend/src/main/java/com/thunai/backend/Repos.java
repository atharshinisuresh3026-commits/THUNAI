package com.thunai.backend;
import com.thunai.backend.Entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;
public class Repos {
  public interface UserRepo extends JpaRepository<User,Long> { Optional<User> findByEmail(String e); boolean existsByEmail(String e); }
  public interface ContactRepo extends JpaRepository<Contact,Long> { List<Contact> findByUserIdOrderByPriorityAsc(Long u); Optional<Contact> findByIdAndUserId(Long id, Long u); }
  public interface AlertRepo extends JpaRepository<Alert,Long> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(Long u);
    Optional<Alert> findByIdAndUserId(Long id, Long u);
    Optional<Alert> findFirstByUserIdAndTypeAndStatusAndCreatedAtAfter(Long u, String t, String s, Instant after);
  }
  public interface DeliveryRepo extends JpaRepository<Delivery,Long> { List<Delivery> findByAlertId(Long a); }
}
