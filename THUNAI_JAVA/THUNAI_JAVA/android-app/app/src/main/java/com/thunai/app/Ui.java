package com.thunai.app;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Small helpers so every screen is built in Java (no XML layouts). */
public class Ui {
  public static final int FOREST=0xFF1B5E3A, EMERALD=0xFF2ECC71, MINT=0xFFD8F3E3, SAGE=0xFFE8EFE6, WHITE=0xFFFFFFFF, RED=0xFFD32F2F, TEXT=0xFF1F2D25;
  public static int dp(Context c,int v){ return (int)(v*c.getResources().getDisplayMetrics().density+0.5f); }
  public static GradientDrawable bg(Context c,int color,int radiusDp){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(c,radiusDp)); return g; }
  public static LinearLayout page(Context c){
    LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(SAGE);
    l.setPadding(dp(c,20),dp(c,28),dp(c,20),dp(c,20)); return l;
  }
  public static ScrollView scroll(Context c, View content){ ScrollView s=new ScrollView(c); s.setFillViewport(true); s.setBackgroundColor(SAGE); s.addView(content); return s; }
  public static LinearLayout card(Context c){
    LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); l.setBackground(bg(c,WHITE,16));
    l.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(c,6),0,dp(c,6)); l.setLayoutParams(lp); return l;
  }
  public static Button button(Context c,String text,int color,int heightDp){
    Button b=new Button(c); b.setText(text); b.setAllCaps(false); b.setTextColor(WHITE); b.setTextSize(18); b.setTypeface(Typeface.DEFAULT_BOLD);
    b.setBackground(bg(c,color,18));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(c,heightDp)); lp.setMargins(0,dp(c,6),0,dp(c,6)); b.setLayoutParams(lp); return b;
  }
  public static TextView text(Context c,String t,int sp,int color,boolean bold){
    TextView v=new TextView(c); v.setText(t); v.setTextSize(sp); v.setTextColor(color); if(bold) v.setTypeface(Typeface.DEFAULT_BOLD);
    v.setPadding(0,dp(c,4),0,dp(c,4)); return v;
  }
  public static EditText input(Context c,String hint,int type){
    EditText e=new EditText(c); e.setHint(hint); e.setInputType(type); e.setTextSize(17); e.setTextColor(TEXT); e.setSingleLine(true);
    e.setBackground(bg(c,WHITE,12)); e.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(c,6),0,dp(c,6)); e.setLayoutParams(lp); return e;
  }
  public static final int T_TEXT=InputType.TYPE_CLASS_TEXT, T_EMAIL=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
      T_PASS=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD, T_PHONE=InputType.TYPE_CLASS_PHONE, T_NUM=InputType.TYPE_CLASS_NUMBER;
  public static void toast(Context c,String m){ Toast.makeText(c,m,Toast.LENGTH_LONG).show(); }
  public static LinearLayout row(Context c){ LinearLayout r=new LinearLayout(c); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); r.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); return r; }
}
