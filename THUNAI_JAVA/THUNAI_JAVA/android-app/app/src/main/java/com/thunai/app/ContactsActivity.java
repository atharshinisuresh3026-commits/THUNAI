package com.thunai.app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactsActivity extends Activity {
  private LinearLayout list; private EditText name, phone, prio;

  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p = Ui.page(this);
    p.addView(Ui.text(this, "Trusted contacts", 26, Ui.FOREST, true));
    p.addView(Ui.text(this, "Use full numbers with country code, e.g. +919876543210. Priority 1 is contacted first.", 14, Ui.TEXT, false));
    name = Ui.input(this, "Name", Ui.T_TEXT); phone = Ui.input(this, "Phone (+91...)", Ui.T_PHONE); prio = Ui.input(this, "Priority (1-99)", Ui.T_NUM); prio.setText("1");
    Button add = Ui.button(this, "Add contact", Ui.EMERALD, 54);
    add.setOnClickListener(v -> addContact());
    p.addView(name); p.addView(phone); p.addView(prio); p.addView(add);
    list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); p.addView(list);
    setContentView(Ui.scroll(this, p));
    refresh();
  }

  private void addContact(){
    final String n = name.getText().toString().trim(), ph = phone.getText().toString().trim().replace(" ", "");
    int pr; try { pr = Integer.parseInt(prio.getText().toString().trim()); } catch(Exception e){ Ui.toast(this, "Priority must be a number"); return; }
    if(n.isEmpty()){ Ui.toast(this, "Enter a name"); return; }
    if(!ph.matches("^\\+?[0-9]{10,15}$")){ Ui.toast(this, "Enter a valid phone number (10-15 digits)"); return; }
    final int fp = pr;
    Async.run(() -> Api.obj(this, "POST", "/api/contacts", new JSONObject().put("name", n).put("phone", ph).put("priority", fp)), new Async.Done<JSONObject>(){
      public void ok(JSONObject r){ name.setText(""); phone.setText(""); refresh(); }
      public void err(String m){ Ui.toast(ContactsActivity.this, m); }
    });
  }

  private void refresh(){
    Async.run(() -> Api.arr(this, "GET", "/api/contacts", null), new Async.Done<JSONArray>(){
      public void ok(JSONArray a){
        list.removeAllViews();
        if(a.length() == 0) list.addView(Ui.text(ContactsActivity.this, "No contacts yet.", 15, Ui.TEXT, false));
        for(int i = 0; i < a.length(); i++){
          JSONObject c = a.optJSONObject(i); if(c == null) continue;
          LinearLayout card = Ui.card(ContactsActivity.this);
          card.addView(Ui.text(ContactsActivity.this, c.optString("name") + "  (priority " + c.optInt("priority") + ")", 18, Ui.FOREST, true));
          card.addView(Ui.text(ContactsActivity.this, c.optString("phone"), 16, Ui.TEXT, false));
          final long id = c.optLong("id");
          Button del = Ui.button(ContactsActivity.this, "Delete", Ui.RED, 44);
          del.setOnClickListener(v -> Async.run(() -> Api.call(ContactsActivity.this, "DELETE", "/api/contacts/" + id, null), new Async.Done<Object>(){
            public void ok(Object r){ refresh(); } public void err(String m){ Ui.toast(ContactsActivity.this, m); } }));
          card.addView(del); list.addView(card);
        }
      }
      public void err(String m){ Ui.toast(ContactsActivity.this, m); }
    });
  }
}
