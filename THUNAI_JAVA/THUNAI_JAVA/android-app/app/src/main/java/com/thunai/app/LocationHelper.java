package com.thunai.app;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

/** Framework LocationManager only (no Google Play services needed). Caller must hold a location permission. */
public class LocationHelper {
  public interface Cb { void got(Location location /* may be null */); }

  @SuppressLint("MissingPermission")
  public static void get(Context c, Cb cb){
    LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = lm.getProviders(true);
    long now = System.currentTimeMillis(); Location best = null;
    for(String p : providers){ Location l = lm.getLastKnownLocation(p); if(l != null && now - l.getTime() < 120000 && (best == null || l.getAccuracy() < best.getAccuracy())) best = l; }
    if(best != null){ cb.got(best); return; }
    if(providers.isEmpty()){ cb.got(null); return; }
    final boolean[] done = {false};
    LocationListener ll = new LocationListener(){
      public void onLocationChanged(Location l){ if(!done[0]){ done[0] = true; lm.removeUpdates(this); cb.got(l); } }
      public void onProviderEnabled(String p){} public void onProviderDisabled(String p){} public void onStatusChanged(String p, int s, Bundle b){}
    };
    for(String p : providers) lm.requestLocationUpdates(p, 0, 0, ll, Looper.getMainLooper());
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      if(done[0]) return; done[0] = true; lm.removeUpdates(ll);
      Location any = null; for(String p : providers){ Location l = lm.getLastKnownLocation(p); if(l != null && (any == null || l.getTime() > any.getTime())) any = l; }
      cb.got(any); // may be an older fix, or null
    }, 15000);
  }
}
