package com.thunai.backend;
import com.thunai.backend.Entities.*;
import com.thunai.backend.Repos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Api {
  static final String PHONE = "^\\+?[0-9]{10,15}$";

  @RestControllerAdvice public static class Errors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException e){
      FieldError f=e.getBindingResult().getFieldError();
      return ResponseEntity.badRequest().body(Map.of("error", f==null?"Invalid input":f.getField()+": "+f.getDefaultMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException e){
      return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()==null?"Error":e.getReason()));
    }
  }

  @RestController @RequestMapping("/api/auth") public static class Auth {
    public record RegisterReq(@NotBlank @Size(max=120) String fullName, @NotBlank @Email String email, @Pattern(regexp=PHONE, message="invalid phone number") String phone, @Size(min=8,max=72) String password){}
    public record LoginReq(@NotBlank String email, @NotBlank String password){}
    public record VerifyReq(@NotBlank String email, @Pattern(regexp="^[0-9]{6}$", message="code must be 6 digits") String code){}
    public record EmailReq(@NotBlank String email){}
    public record AuthRes(String token, Long userId, String fullName){}
    public record OtpRes(String message, String smsStatus){}
    private static final java.security.SecureRandom RND = new java.security.SecureRandom();
    private final UserRepo users; private final PasswordEncoder enc; private final Security.Jwt jwt; private final NotificationService notif; private final boolean devLog;
    public Auth(UserRepo u, PasswordEncoder e, Security.Jwt j, NotificationService n, @Value("${thunai.dev-otp-log}") boolean devLog){ users=u; enc=e; jwt=j; notif=n; this.devLog=devLog; }

    /** Creates (or refreshes) an UNVERIFIED account and sends a 6-digit code. No token until the code is verified. */
    @PostMapping("/register") public OtpRes register(@Valid @RequestBody RegisterReq r){
      String email=r.email().trim().toLowerCase();
      User u=users.findByEmail(email).orElse(null);
      if(u!=null && u.verified) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered");
      if(u==null){ u=new User(); u.createdAt=Instant.now(); }
      u.fullName=r.fullName().trim(); u.email=email; u.phone=r.phone(); u.passwordHash=enc.encode(r.password()); u.verified=false;
      return issueOtp(u);
    }
    @PostMapping("/resend-otp") public OtpRes resend(@Valid @RequestBody EmailReq r){
      User u=users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
      if(u==null || u.verified) return new OtpRes("If this account exists and is unverified, a new code was sent.","N/A");
      return issueOtp(u);
    }
    @PostMapping("/verify") public AuthRes verify(@Valid @RequestBody VerifyReq r){
      User u=users.findByEmail(r.email().trim().toLowerCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid code"));
      if(u.verified) throw new ResponseStatusException(HttpStatus.CONFLICT,"Already verified - please log in");
      if(u.otpHash==null || u.otpExpires==null || u.otpExpires.isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Code expired - request a new one");
      if(u.otpAttempts>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many wrong attempts - request a new code");
      if(!enc.matches(r.code(), u.otpHash)){ u.otpAttempts++; users.save(u); throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid code"); }
      u.verified=true; u.otpHash=null; u.otpExpires=null; u.otpAttempts=0; users.save(u);
      return new AuthRes(jwt.issue(u.id), u.id, u.fullName);
    }
    @PostMapping("/login") public AuthRes login(@Valid @RequestBody LoginReq r){
      User u=users.findByEmail(r.email().trim().toLowerCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));
      if(!enc.matches(r.password(), u.passwordHash)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
      if(!u.verified) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Phone not verified - register again or use resend code");
      return new AuthRes(jwt.issue(u.id), u.id, u.fullName);
    }
    private OtpRes issueOtp(User u){
      String code=String.format("%06d", RND.nextInt(1_000_000));
      u.otpHash=enc.encode(code); u.otpExpires=Instant.now().plus(10, ChronoUnit.MINUTES); u.otpAttempts=0; users.save(u);
      var res=notif.sendSms(u.phone, "Your THUNAI verification code is "+code+". It expires in 10 minutes.");
      if("NOT_CONFIGURED".equals(res.status()) && devLog) System.out.println("[DEV ONLY] OTP for "+u.email+": "+code);
      String msg = switch(res.status()){
        case "SENT_TO_PROVIDER" -> "Verification code sent by SMS.";
        case "NOT_CONFIGURED" -> devLog ? "SMS not configured. DEV MODE: the code is printed in the backend console." : "SMS provider not configured - the code could not be delivered.";
        default -> "SMS sending failed - try Resend.";
      };
      return new OtpRes(msg, res.status());
    }
  }

  @RestController @RequestMapping("/api/contacts") public static class Contacts {
    public record ContactReq(@NotBlank @Size(max=120) String name, @Pattern(regexp=PHONE, message="invalid phone number") String phone, @Min(1) @Max(99) int priority){}
    private final ContactRepo repo; public Contacts(ContactRepo r){ repo=r; }
    @GetMapping public List<Contact> list(@AuthenticationPrincipal Long uid){ return repo.findByUserIdOrderByPriorityAsc(uid); }
    @PostMapping public Contact add(@AuthenticationPrincipal Long uid, @Valid @RequestBody ContactReq r){
      Contact c=new Contact(); c.userId=uid; c.name=r.name().trim(); c.phone=r.phone(); c.priority=r.priority(); return repo.save(c);
    }
    @PutMapping("/{id}") public Contact edit(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody ContactReq r){
      Contact c=repo.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Contact not found"));
      c.name=r.name().trim(); c.phone=r.phone(); c.priority=r.priority(); return repo.save(c);
    }
    @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id){
      repo.delete(repo.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Contact not found")));
    }
  }

  @RestController @RequestMapping("/api/alerts") public static class Alerts {
    public record AlertReq(@Pattern(regexp="EMERGENCY|HARASSMENT|MEDICAL", message="type must be EMERGENCY, HARASSMENT or MEDICAL") String type,
        @DecimalMin("-90") @DecimalMax("90") double latitude, @DecimalMin("-180") @DecimalMax("180") double longitude, @Size(max=500) String message, boolean testMode){}
    public record AlertView(Alert alert, List<Delivery> deliveries){}
    private final AlertRepo alerts; private final DeliveryRepo deliveries; private final ContactRepo contacts; private final NotificationService notif;
    public Alerts(AlertRepo a, DeliveryRepo d, ContactRepo c, NotificationService n){ alerts=a; deliveries=d; contacts=c; notif=n; }

    @PostMapping public AlertView create(@AuthenticationPrincipal Long uid, @Valid @RequestBody AlertReq r){
      if(alerts.findFirstByUserIdAndTypeAndStatusAndCreatedAtAfter(uid, r.type(), "ACTIVE", Instant.now().minus(2, ChronoUnit.MINUTES)).isPresent())
        throw new ResponseStatusException(HttpStatus.CONFLICT,"An active "+r.type()+" alert was already sent in the last 2 minutes");
      Alert a=new Alert(); a.userId=uid; a.type=r.type(); a.latitude=r.latitude(); a.longitude=r.longitude();
      a.message=r.message(); a.status=r.testMode()?"TEST":"ACTIVE"; a.createdAt=Instant.now(); alerts.save(a);
      List<Delivery> out=new ArrayList<>();
      for(Contact c: contacts.findByUserIdOrderByPriorityAsc(uid)){
        Delivery d=new Delivery(); d.alertId=a.id; d.contactId=c.id; d.channel="SMS";
        if(r.testMode()){ d.status="NOT_SENT_TEST_MODE"; d.detail="Test mode: no SMS sent"; }
        else {
          String body=(a.type)+" alert from THUNAI user. Location: https://maps.google.com/?q="+a.latitude+","+a.longitude+(a.message==null?"":" Note: "+a.message);
          var res=notif.sendSms(c.phone, body); d.status=res.status(); d.detail=res.detail();
        }
        out.add(deliveries.save(d));
      }
      return new AlertView(a,out);
    }
    @PostMapping("/{id}/cancel") public AlertView cancel(@AuthenticationPrincipal Long uid, @PathVariable Long id){
      Alert a=alerts.findByIdAndUserId(id,uid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Alert not found"));
      if(!"ACTIVE".equals(a.status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Alert is not active");
      a.status="CANCELLED"; alerts.save(a); return new AlertView(a, deliveries.findByAlertId(a.id)); // contacts already texted are NOT recalled
    }
    @GetMapping public List<AlertView> history(@AuthenticationPrincipal Long uid){
      return alerts.findByUserIdOrderByCreatedAtDesc(uid).stream().map(a->new AlertView(a, deliveries.findByAlertId(a.id))).toList();
    }
  }
}
