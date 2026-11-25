package com.thunai.app;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Async {
  private static final ExecutorService EX = Executors.newFixedThreadPool(3);
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  public interface Job<T> { T run() throws Exception; }
  public interface Done<T> { void ok(T result); void err(String message); }
  public static <T> void run(Job<T> job, Done<T> done){
    EX.execute(() -> {
      try { T r = job.run(); MAIN.post(() -> done.ok(r)); }
      catch(Exception e){ String m = e.getMessage()==null ? e.toString() : e.getMessage(); MAIN.post(() -> done.err(m)); }
    });
  }
}
