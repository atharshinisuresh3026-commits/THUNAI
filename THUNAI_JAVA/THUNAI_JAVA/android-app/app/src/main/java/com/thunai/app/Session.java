package com.thunai.app;
import android.content.Context;
import android.content.SharedPreferences;
/** NOTE: the token is kept in plain SharedPreferences. For production use EncryptedSharedPreferences/Keystore. */
public class Session {
  private static SharedPreferences sp(Context c){ return c.getApplicationContext().getSharedPreferences("thunai", Context.MODE_PRIVATE); }
  public static String baseUrl(Context c){ return sp(c).getString("base", "http://10.0.2.2:8080"); } // 10.0.2.2 = host PC from the emulator
  public static String token(Context c){ return sp(c).getString("token", null); }
  public static String name(Context c){ return sp(c).getString("name", ""); }
  public static void saveBase(Context c, String b){ sp(c).edit().putString("base", b.trim().replaceAll("/+$","")).apply(); }
  public static void login(Context c, String token, String name){ sp(c).edit().putString("token", token).putString("name", name).apply(); }
  public static void logout(Context c){ sp(c).edit().remove("token").remove("name").apply(); }
}
