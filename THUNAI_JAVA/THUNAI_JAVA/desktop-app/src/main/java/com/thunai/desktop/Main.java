package com.thunai.desktop;
import javax.swing.*;
import java.awt.*;

public class Main {
  public static void main(String[] args){
    SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
  }

  static JButton button(String text, Color bg){
    JButton b = new JButton(text); b.setBackground(bg); b.setForeground(Color.WHITE);
    b.setFocusPainted(false); b.setFont(b.getFont().deriveFont(Font.BOLD, 14f)); return b;
  }

  static class LoginFrame extends JFrame {
    final JTextField server = new JTextField(System.getenv().getOrDefault("THUNAI_API_URL","http://localhost:8080"));
    final JTextField email = new JTextField(), name = new JTextField(), phone = new JTextField();
    final JPasswordField password = new JPasswordField();
    LoginFrame(){
      super("THUNAI - Sign in");
      setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(420, 480); setLocationRelativeTo(null);
      JPanel p = new JPanel(new GridLayout(0,1,6,6)); p.setBorder(BorderFactory.createEmptyBorder(24,32,24,32)); p.setBackground(Theme.SAGE);
      JLabel title = new JLabel("THUNAI", SwingConstants.CENTER); title.setFont(title.getFont().deriveFont(Font.BOLD, 30f)); title.setForeground(Theme.FOREST);
      p.add(title);
      p.add(new JLabel("Server URL")); p.add(server);
      p.add(new JLabel("Email")); p.add(email);
      p.add(new JLabel("Password (min 8 characters)")); p.add(password);
      p.add(new JLabel("Name + phone (registration only)")); p.add(name); p.add(phone);
      JButton login = button("Sign in", Theme.FOREST), reg = button("Register", Theme.EMERALD);
      JPanel row = new JPanel(new GridLayout(1,2,8,0)); row.setOpaque(false); row.add(login); row.add(reg); p.add(row);
      add(p);
      login.addActionListener(e -> run(false)); reg.addActionListener(e -> run(true));
    }
    void run(boolean register){
      ApiClient api = new ApiClient(server.getText().trim());
      String em = email.getText().trim();
      new SwingWorker<String,Void>(){
        protected String doInBackground() throws Exception {
          return register ? api.register(name.getText().trim(), em, phone.getText().trim(), new String(password.getPassword()))
                          : api.login(em, new String(password.getPassword()));
        }
        protected void done(){
          try {
            String r = get();
            if(register) askCode(api, em, r); else { dispose(); new Dashboard(api, r).setVisible(true); }
          } catch(Exception ex){ error(ex, "Could not continue"); }
        }
      }.execute();
    }
    void askCode(ApiClient api, String em, String serverMsg){
      String code = JOptionPane.showInputDialog(this, serverMsg + "\nEnter the 6-digit code (leave empty and press OK to resend):", "Verify your phone", JOptionPane.QUESTION_MESSAGE);
      if(code == null) return;
      code = code.trim();
      final String c = code;
      new SwingWorker<String,Void>(){
        protected String doInBackground() throws Exception { return c.isEmpty() ? api.resend(em) : api.verify(em, c); }
        protected void done(){
          try {
            String r = get();
            if(c.isEmpty()) askCode(api, em, r); else { dispose(); new Dashboard(api, r).setVisible(true); }
          } catch(Exception ex){ error(ex, "Verification failed"); askCode(api, em, "Try again."); }
        }
      }.execute();
    }
    void error(Exception ex, String title){
      Throwable c = ex.getCause()==null?ex:ex.getCause();
      JOptionPane.showMessageDialog(this, c.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
  }
}
