package com.thunai.app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends Activity {
  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout p = Ui.page(this);
    p.addView(Ui.text(this, "Alert history", 26, Ui.FOREST, true));
    LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); p.addView(list);
    setContentView(Ui.scroll(this, p));
    Async.run(() -> Api.arr(this, "GET", "/api/alerts", null), new Async.Done<JSONArray>(){
      public void ok(JSONArray a){
        if(a.length() == 0) list.addView(Ui.text(HistoryActivity.this, "No alerts yet.", 15, Ui.TEXT, false));
        for(int i = 0; i < a.length(); i++){
          JSONObject v = a.optJSONObject(i); if(v == null) continue; JSONObject al = v.optJSONObject("alert"); if(al == null) continue;
          LinearLayout card = Ui.card(HistoryActivity.this);
          card.addView(Ui.text(HistoryActivity.this, al.optString("type") + " - " + al.optString("status"), 18, "ACTIVE".equals(al.optString("status")) ? Ui.RED : Ui.FOREST, true));
          card.addView(Ui.text(HistoryActivity.this, al.optString("createdAt"), 14, Ui.TEXT, false));
          card.addView(Ui.text(HistoryActivity.this, "Location: " + al.optDouble("latitude") + ", " + al.optDouble("longitude"), 14, Ui.TEXT, false));
          JSONArray ds = v.optJSONArray("deliveries"); StringBuilder sb = new StringBuilder("SMS delivery: ");
          if(ds == null || ds.length() == 0) sb.append("no contacts notified");
          else for(int k = 0; k < ds.length(); k++) sb.append(String.valueOf(ds.optJSONObject(k) == null ? "?" : ds.optJSONObject(k).optString("status"))).append(k < ds.length() - 1 ? ", " : "");
          card.addView(Ui.text(HistoryActivity.this, sb.toString(), 14, Ui.TEXT, false)); list.addView(card);
        }
      }
      public void err(String m){ Ui.toast(HistoryActivity.this, m); }
    });
  }
}
