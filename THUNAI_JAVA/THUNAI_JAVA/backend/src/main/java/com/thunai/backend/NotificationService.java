package com.thunai.backend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** SMS via Twilio REST API. Never reports more than what the provider actually told us. */
@Service
public class NotificationService {
  public record Result(String status, String detail) {}
  private final String sid, token, from;
  private final HttpClient http = HttpClient.newHttpClient();
  public NotificationService(@Value("${thunai.twilio.sid}") String sid, @Value("${thunai.twilio.token}") String token, @Value("${thunai.twilio.from}") String from){
    this.sid=sid; this.token=token; this.from=from;
  }
  public boolean smsConfigured(){ return !sid.isBlank() && !token.isBlank() && !from.isBlank(); }
  /** SENT_TO_PROVIDER = accepted by Twilio. It does NOT prove the phone received it. */
  public Result sendSms(String to, String body){
    if(!smsConfigured()) return new Result("NOT_CONFIGURED","SMS provider credentials not set");
    try {
      String form = "To="+enc(to)+"&From="+enc(from)+"&Body="+enc(body);
      String auth = Base64.getEncoder().encodeToString((sid+":"+token).getBytes(StandardCharsets.UTF_8));
      HttpRequest r = HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+sid+"/Messages.json"))
        .header("Authorization","Basic "+auth).header("Content-Type","application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form)).build();
      HttpResponse<String> resp = http.send(r, HttpResponse.BodyHandlers.ofString());
      if(resp.statusCode()/100==2) return new Result("SENT_TO_PROVIDER","HTTP "+resp.statusCode());
      return new Result("FAILED","Provider HTTP "+resp.statusCode());
    } catch(Exception e){ return new Result("FAILED", e.getClass().getSimpleName()); }
  }
  private static String enc(String s){ return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
