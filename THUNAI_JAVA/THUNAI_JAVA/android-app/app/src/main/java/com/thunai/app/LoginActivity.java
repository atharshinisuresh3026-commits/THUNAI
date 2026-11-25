package com.thunai.app;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;

public class LoginActivity extends Activity {
  private boolean registerMode = false;
  private EditText server, email, password, name, phone, code;
  private Button primary, verify, resend;
  private TextView toggle, info;

  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    if(Session.token(this) != null){ startActivity(new Intent(this, HomeActivity.class)); finish(); return; }
    LinearLayout p = Ui.page(this);
    p.addView(Ui.text(this, "THUNAI", 34, Ui.FOREST, true));
    p.addView(Ui.text(this, "Your safety, one tap away", 16, Ui.TEXT, false));
    server = Ui.input(this, "Server URL", Ui.T_TEXT); server.setText(Session.baseUrl(this));
    email = Ui.input(this, "Email", Ui.T_EMAIL);
    password = Ui.input(this, "Password (min 8 characters)", Ui.T_PASS);
    name = Ui.input(this, "Full name", Ui.T_TEXT);
    phone = Ui.input(this, "Your phone, e.g. +919876543210", Ui.T_PHONE);
    code = Ui.input(this, "6-digit code from SMS", Ui.T_NUM);
    primary = Ui.button(this, "Log in", Ui.FOREST, 56);
    verify = Ui.button(this, "Verify code", Ui.EMERALD, 56);
    resend = Ui.button(this, "Resend code", Ui.FOREST, 48);
    toggle = Ui.text(this, "New here? Create an account", 16, Ui.FOREST, true);
    info = Ui.text(this, "", 14, Ui.TEXT, false);
    for(View v : new View[]{server, email, password, name, phone, primary, toggle, code, verify, resend, info}) p.addView(v);
    showMode();
    primary.setOnClickListener(v -> submit());
    toggle.setOnClickListener(v -> { registerMode = !registerMode; showMode(); });
    verify.setOnClickListener(v -> doVerify());
    resend.setOnClickListener(v -> doResend());
    setContentView(Ui.scroll(this, p));
  }

  private void showMode(){
    int reg = registerMode ? View.VISIBLE : View.GONE;
    name.setVisibility(reg); phone.setVisibility(reg);
    code.setVisibility(View.GONE); verify.setVisibility(View.GONE); resend.setVisibility(View.GONE);
    primary.setText(registerMode ? "Create account" : "Log in");
    toggle.setText(registerMode ? "Already registered? Log in" : "New here? Create an account");
  }

  private JSONObject json(String... kv) throws Exception { JSONObject o = new JSONObject(); for(int i = 0; i < kv.length; i += 2) o.put(kv[i], kv[i+1]); return o; }

  private void submit(){
    Session.saveBase(this, server.getText().toString());
    final String em = email.getText().toString().trim(), pw = password.getText().toString();
    primary.setEnabled(false);
    if(registerMode){
      Async.run(() -> Api.obj(this, "POST", "/api/auth/register", json("fullName", name.getText().toString().trim(), "email", em, "phone", phone.getText().toString().trim(), "password", pw)),
        new Async.Done<JSONObject>(){
          public void ok(JSONObject r){ primary.setEnabled(true); info.setText(r.optString("message")); code.setVisibility(View.VISIBLE); verify.setVisibility(View.VISIBLE); resend.setVisibility(View.VISIBLE); }
          public void err(String m){ primary.setEnabled(true); Ui.toast(LoginActivity.this, m); }
        });
    } else {
      Async.run(() -> Api.obj(this, "POST", "/api/auth/login", json("email", em, "password", pw)), authDone());
    }
  }

  private void doVerify(){
    final String em = email.getText().toString().trim(), cd = code.getText().toString().trim();
    Async.run(() -> Api.obj(this, "POST", "/api/auth/verify", json("email", em, "code", cd)), authDone());
  }
  private void doResend(){
    final String em = email.getText().toString().trim();
    Async.run(() -> Api.obj(this, "POST", "/api/auth/resend-otp", json("email", em)), new Async.Done<JSONObject>(){
      public void ok(JSONObject r){ info.setText(r.optString("message")); }
      public void err(String m){ Ui.toast(LoginActivity.this, m); }
    });
  }
  private Async.Done<JSONObject> authDone(){
    return new Async.Done<JSONObject>(){
      public void ok(JSONObject r){ primary.setEnabled(true); Session.login(LoginActivity.this, r.optString("token"), r.optString("fullName")); startActivity(new Intent(LoginActivity.this, HomeActivity.class)); finish(); }
      public void err(String m){ primary.setEnabled(true); Ui.toast(LoginActivity.this, m); }
    };
  }
}
