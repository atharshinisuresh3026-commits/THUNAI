package com.thunai.app;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Blocking HTTP calls - always call through Async.run(). Throws Exception(message) with the server's error text. */
public class Api {
  public static Object call(Context c, String method, String path, JSONObject body) throws Exception {
    HttpURLConnection h = (HttpURLConnection) new URL(Session.baseUrl(c) + path).openConnection();
    h.setConnectTimeout(15000); h.setReadTimeout(20000); h.setRequestMethod(method);
    h.setRequestProperty("Content-Type", "application/json");
    String t = Session.token(c); if(t != null) h.setRequestProperty("Authorization", "Bearer " + t);
    if(body != null){ h.setDoOutput(true); try(OutputStream o = h.getOutputStream()){ o.write(body.toString().getBytes(StandardCharsets.UTF_8)); } }
    int code = h.getResponseCode();
    InputStream in = code >= 400 ? h.getErrorStream() : h.getInputStream();
    String text = in == null ? "" : read(in);
    Object parsed = text.isEmpty() ? new JSONObject() : new JSONTokener(text).nextValue();
    if(code >= 400){
      String m = (parsed instanceof JSONObject && ((JSONObject) parsed).has("error")) ? ((JSONObject) parsed).getString("error") : (code == 401 ? "Session expired - please log in again" : "Server error " + code);
      throw new Exception(m);
    }
    return parsed;
  }
  public static JSONObject obj(Context c, String m, String p, JSONObject b) throws Exception { return (JSONObject) call(c, m, p, b); }
  public static JSONArray arr(Context c, String m, String p, JSONObject b) throws Exception { return (JSONArray) call(c, m, p, b); }
  private static String read(InputStream in) throws IOException {
    try(BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){ StringBuilder sb = new StringBuilder(); String l; while((l = r.readLine()) != null) sb.append(l); return sb.toString(); }
  }
}
