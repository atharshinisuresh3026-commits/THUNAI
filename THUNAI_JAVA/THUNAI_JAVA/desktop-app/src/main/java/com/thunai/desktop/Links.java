package com.thunai.desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
public class Links {
  public static String whatsapp(String phone, String text){
    String d = phone.replaceAll("[^0-9]", ""); if(d.length()==10) d = "91" + d; // assumes India for bare 10-digit numbers
    return "https://wa.me/" + d + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+","%20");
  }
  public static String maps(String query, double lat, double lng){
    return "https://www.google.com/maps/search/" + URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+","%20") + "/@" + lat + "," + lng + ",14z";
  }
  public static String mapsLink(double lat, double lng){ return "https://maps.google.com/?q=" + lat + "," + lng; }
  public static void open(String url) throws Exception {
    if(!java.awt.Desktop.isDesktopSupported()) throw new IllegalStateException("Cannot open links on this system");
    java.awt.Desktop.getDesktop().browse(new URI(url));
  }
}
