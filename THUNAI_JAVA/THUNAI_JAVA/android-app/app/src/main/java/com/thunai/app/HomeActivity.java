package com.thunai.app;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class HomeActivity extends Activity {
  private Switch testMode;
  private Runnable afterGranted, afterDenied;

  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    if(Session.token(this) == null){ startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
    LinearLayout p = Ui.page(this);
    p.addView(Ui.text(this, "Hi, " + Session.name(this), 24, Ui.FOREST, true));
    p.addView(Ui.text(this, "Tap an alert. Location is captured first, then your trusted contacts are opened on WhatsApp.", 14, Ui.TEXT, false));

    Button sos = Ui.button(this, "EMERGENCY SOS", Ui.RED, 120);
    Button harass = Ui.button(this, "HARASSMENT SOS", Ui.FOREST, 84);
    Button med = Ui.button(this, "MEDICAL SOS", Ui.EMERALD, 84);
    sos.setOnClickListener(v -> startSos("EMERGENCY")); harass.setOnClickListener(v -> startSos("HARASSMENT")); med.setOnClickListener(v -> startSos("MEDICAL"));
    p.addView(sos); p.addView(harass); p.addView(med);
    testMode = new Switch(this); testMode.setText("Test mode (marks the alert as TEST, no SMS)"); p.addView(testMode);

    LinearLayout near = Ui.card(this);
    near.addView(Ui.text(this, "Find help nearby (opens Google Maps)", 17, Ui.FOREST, true));
    Button h = Ui.button(this, "Hospitals", Ui.FOREST, 52), po = Ui.button(this, "Police stations", Ui.FOREST, 52), ph = Ui.button(this, "Pharmacies", Ui.FOREST, 52);
    h.setOnClickListener(v -> nearby("hospital")); po.setOnClickListener(v -> nearby("police station")); ph.setOnClickListener(v -> nearby("pharmacy"));
    near.addView(h); near.addView(po); near.addView(ph); p.addView(near);

    LinearLayout call = Ui.card(this);
    call.addView(Ui.text(this, "Call shortcuts (opens the dialer; you press call). India numbers - check yours.", 14, Ui.TEXT, false));
    String[][] nums = {{"112 Emergency","112"},{"100 Police","100"},{"108 Ambulance","108"},{"1091 Women helpline","1091"}};
    for(String[] n : nums){ Button bt = Ui.button(this, n[0], Ui.EMERALD, 48); bt.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + n[1])))); call.addView(bt); }
    p.addView(call);

    Button contacts = Ui.button(this, "Trusted contacts", Ui.FOREST, 56), hist = Ui.button(this, "Alert history", Ui.FOREST, 56), out = Ui.button(this, "Log out", Ui.RED, 48);
    contacts.setOnClickListener(v -> startActivity(new Intent(this, ContactsActivity.class)));
    hist.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    out.setOnClickListener(v -> { Session.logout(this); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    p.addView(contacts); p.addView(hist); p.addView(out);
    setContentView(Ui.scroll(this, p));
  }

  // ---- permission handling ----
  private boolean hasLocation(){ return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; }
  private void ensureLocation(Runnable granted, Runnable denied){
    if(hasLocation()){ granted.run(); return; }
    afterGranted = granted; afterDenied = denied;
    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 7);
  }
  @Override public void onRequestPermissionsResult(int rc, String[] perms, int[] res){
    super.onRequestPermissionsResult(rc, perms, res);
    if(rc != 7) return;
    if(hasLocation()){ if(afterGranted != null) afterGranted.run(); } else { if(afterDenied != null) afterDenied.run(); }
    afterGranted = null; afterDenied = null;
  }

  // ---- SOS flow: location first -> save alert -> WhatsApp contacts ----
  private void startSos(String type){
    ensureLocation(() -> { Ui.toast(this, "Getting your location..."); LocationHelper.get(this, loc -> proceed(type, loc)); },
      () -> { Ui.toast(this, "Location permission denied - the alert will go out WITHOUT a location."); proceed(type, null); });
  }

  private void proceed(String type, Location loc){
    final boolean test = testMode.isChecked();
    Async.run(() -> {
      JSONArray contacts = Api.arr(this, "GET", "/api/contacts", null);
      String serverMsg;
      if(loc == null) serverMsg = "Not saved on server: no location available.";
      else {
        try {
          JSONObject body = new JSONObject().put("type", type).put("latitude", loc.getLatitude()).put("longitude", loc.getLongitude()).put("message", "").put("testMode", test);
          JSONObject res = Api.obj(this, "POST", "/api/alerts", body);
          StringBuilder sb = new StringBuilder("Saved on server (" + res.getJSONObject("alert").optString("status") + "). SMS: ");
          JSONArray ds = res.getJSONArray("deliveries");
          if(ds.length() == 0) sb.append("no contacts to text");
          for(int i = 0; i < ds.length(); i++) sb.append(ds.getJSONObject(i).optString("status")).append(i < ds.length() - 1 ? ", " : "");
          serverMsg = sb.toString();
        } catch(Exception e){ serverMsg = "Server: " + e.getMessage(); }
      }
      return new Object[]{contacts, serverMsg};
    }, new Async.Done<Object[]>(){
      public void ok(Object[] r){ showContactsDialog(type, loc, (JSONArray) r[0], (String) r[1]); }
      public void err(String m){ Ui.toast(HomeActivity.this, m); }
    });
  }

  private void showContactsDialog(String type, Location loc, JSONArray contacts, String serverMsg){
    String where = loc == null ? "Location unavailable." : "My location: https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
    final String msg = "\uD83D\uDEA8 " + type + " alert from THUNAI. I need help. " + where;
    LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 8));
    l.addView(Ui.text(this, serverMsg, 14, Ui.TEXT, false));
    l.addView(Ui.text(this, "WhatsApp opens with the message ready - press Send in WhatsApp for each contact.", 14, Ui.TEXT, true));
    if(contacts.length() == 0) l.addView(Ui.text(this, "No trusted contacts saved. Add some under Trusted contacts.", 15, Ui.RED, true));
    String firstPhone = null;
    for(int i = 0; i < contacts.length(); i++){
      JSONObject c = contacts.optJSONObject(i); if(c == null) continue;
      final String phone = c.optString("phone");
      if(firstPhone == null) firstPhone = phone;
      Button bt = Ui.button(this, "WhatsApp: " + c.optString("name"), Ui.EMERALD, 52);
      bt.setOnClickListener(v -> openWhatsApp(phone, msg)); l.addView(bt);
    }
    new AlertDialog.Builder(this).setTitle(type + " alert").setView(l).setPositiveButton("Close", null).show();
    if(firstPhone != null && !testMode.isChecked()) openWhatsApp(firstPhone, msg);
  }

  private void openWhatsApp(String phone, String msg){
    String d = phone.replaceAll("[^0-9]", ""); if(d.length() == 10) d = "91" + d; // assumes India for bare 10-digit numbers
    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + d + "?text=" + Uri.encode(msg)))); }
    catch(ActivityNotFoundException e){ Ui.toast(this, "WhatsApp (or a browser) is not available on this phone."); }
  }

  // ---- nearby facilities via Google Maps ----
  private void nearby(String q){
    ensureLocation(() -> LocationHelper.get(this, loc -> openMaps(q, loc)), () -> openMaps(q, null));
  }
  private void openMaps(String q, Location l){
    String uri = l != null ? "geo:" + l.getLatitude() + "," + l.getLongitude() + "?q=" + Uri.encode(q) : "geo:0,0?q=" + Uri.encode(q + " near me");
    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))); }
    catch(ActivityNotFoundException e){
      try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/" + Uri.encode(q)))); }
      catch(ActivityNotFoundException e2){ Ui.toast(this, "No maps app or browser found."); }
    }
  }
}
