package com.thunai.desktop;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.Map;

/** Talks to the Spring Boot backend. Throws RuntimeException with the server's error message. */
public class ApiClient {
  private final String base; private String token;
  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http = HttpClient.newHttpClient();
  public ApiClient(String base){ this.base = base.replaceAll("/+$",""); }
  public boolean loggedIn(){ return token != null; }

  public String login(String email, String password) throws Exception {
    JsonNode r = call("POST","/api/auth/login", Map.of("email",email,"password",password));
    token = r.get("token").asText(); return r.get("fullName").asText();
  }
  /** Returns the server's message about the verification code. No token is issued until verify(). */
  public String register(String name, String email, String phone, String password) throws Exception {
    return call("POST","/api/auth/register", Map.of("fullName",name,"email",email,"phone",phone,"password",password)).get("message").asText();
  }
  public String verify(String email, String code) throws Exception {
    JsonNode r = call("POST","/api/auth/verify", Map.of("email",email,"code",code));
    token = r.get("token").asText(); return r.get("fullName").asText();
  }
  public String resend(String email) throws Exception { return call("POST","/api/auth/resend-otp", Map.of("email",email)).get("message").asText(); }
  public JsonNode contacts() throws Exception { return call("GET","/api/contacts",null); }
  public void addContact(String name, String phone, int priority) throws Exception { call("POST","/api/contacts", Map.of("name",name,"phone",phone,"priority",priority)); }
  public void deleteContact(long id) throws Exception { call("DELETE","/api/contacts/"+id,null); }
  public JsonNode alerts() throws Exception { return call("GET","/api/alerts",null); }
  public JsonNode sendAlert(String type, double lat, double lng, String msg, boolean test) throws Exception {
    return call("POST","/api/alerts", Map.of("type",type,"latitude",lat,"longitude",lng,"message",msg==null?"":msg,"testMode",test));
  }

  private JsonNode call(String method, String path, Object body) throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/json");
    if(token != null) b.header("Authorization","Bearer "+token);
    b.method(method, body==null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)));
    HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    String text = r.body()==null ? "" : r.body();
    JsonNode json = text.isBlank() ? om.createObjectNode() : om.readTree(text);
    if(r.statusCode() >= 400) throw new RuntimeException(json.has("error") ? json.get("error").asText() : "HTTP "+r.statusCode());
    return json;
  }
}
