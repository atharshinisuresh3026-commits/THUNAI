package com.thunai.backend;
import jakarta.persistence.*;
import java.time.Instant;
public class Entities {
  @Entity @Table(name="users") public static class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public String fullName; public String email; public String phone; public String passwordHash; public Instant createdAt;
    public boolean verified; public String otpHash; public Instant otpExpires; public int otpAttempts;
  }
  @Entity @Table(name="trusted_contacts") public static class Contact {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long userId; public String name; public String phone; public int priority;
  }
  @Entity @Table(name="sos_alerts") public static class Alert {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long userId; public String type; public double latitude; public double longitude;
    public String message; public String status; public Instant createdAt;
  }
  @Entity @Table(name="alert_deliveries") public static class Delivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long alertId; public Long contactId; public String channel; public String status; public String detail;
  }
}
