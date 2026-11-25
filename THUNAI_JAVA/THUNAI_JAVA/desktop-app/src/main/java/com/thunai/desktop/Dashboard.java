package com.thunai.desktop;
import com.fasterxml.jackson.databind.JsonNode;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Dashboard extends JFrame {
  private final ApiClient api;
  private final DefaultTableModel contactModel = new DefaultTableModel(new String[]{"ID","Name","Phone","Priority"},0){ public boolean isCellEditable(int r,int c){ return false; } };
  private final DefaultTableModel alertModel = new DefaultTableModel(new String[]{"Time","Type","Status","Lat,Lng","Delivery (per contact)"},0){ public boolean isCellEditable(int r,int c){ return false; } };
  private final JTable contactTable = new JTable(contactModel);
  private final JTextField lat = new JTextField("0.0"), lng = new JTextField("0.0"), msg = new JTextField();
  private final JCheckBox test = new JCheckBox("Test mode (sends no SMS)", true);

  public Dashboard(ApiClient api, String who){
    super("THUNAI - " + who); this.api = api;
    setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(900, 600); setLocationRelativeTo(null);
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("SOS", sosPanel()); tabs.addTab("Trusted contacts", contactsPanel()); tabs.addTab("Alert history", historyPanel()); tabs.addTab("Nearby help", facilitiesPanel());
    add(tabs);
    refreshContacts(); refreshAlerts();
  }

  private JPanel sosPanel(){
    JPanel p = new JPanel(new BorderLayout(10,10)); p.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); p.setBackground(Theme.SAGE);
    JPanel loc = new JPanel(new GridLayout(0,2,6,6)); loc.setOpaque(false);
    loc.add(new JLabel("Latitude (desktop has no GPS - enter manually)")); loc.add(lat);
    loc.add(new JLabel("Longitude")); loc.add(lng);
    loc.add(new JLabel("Optional message")); loc.add(msg);
    loc.add(test); p.add(loc, BorderLayout.NORTH);
    JPanel btns = new JPanel(new GridLayout(1,3,12,12)); btns.setOpaque(false);
    JButton emergency = Main.button("EMERGENCY SOS", Theme.RED), harass = Main.button("HARASSMENT SOS", Theme.FOREST), med = Main.button("MEDICAL SOS", Theme.EMERALD);
    emergency.addActionListener(e -> send("EMERGENCY")); harass.addActionListener(e -> send("HARASSMENT")); med.addActionListener(e -> send("MEDICAL"));
    btns.add(emergency); btns.add(harass); btns.add(med); p.add(btns, BorderLayout.CENTER);
    return p;
  }

  private void send(String type){
    double la, lo;
    try { la = Double.parseDouble(lat.getText().trim()); lo = Double.parseDouble(lng.getText().trim()); }
    catch(NumberFormatException e){ JOptionPane.showMessageDialog(this,"Enter valid numeric coordinates.","Location needed",JOptionPane.WARNING_MESSAGE); return; }
    if(JOptionPane.showConfirmDialog(this,"Send "+type+" alert"+(test.isSelected()?" (TEST)":"")+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
    new SwingWorker<JsonNode,Void>(){
      protected JsonNode doInBackground() throws Exception { return api.sendAlert(type, la, lo, msg.getText(), test.isSelected()); }
      protected void done(){
        try { JsonNode r = get(); StringBuilder sb = new StringBuilder("Alert saved (status " + r.get("alert").get("status").asText() + ").\nDelivery results:\n");
          for(JsonNode d : r.get("deliveries")) sb.append(" - contact ").append(d.get("contactId").asText()).append(": ").append(d.get("status").asText()).append("\n");
          if(r.get("deliveries").isEmpty()) sb.append(" (no trusted contacts - nobody was notified)");
          refreshAlerts(); offerWhatsApp(type, la, lo, sb.toString());
        } catch(Exception ex){ fail(ex); }
      }
    }.execute();
  }

  private JPanel contactsPanel(){
    JPanel p = new JPanel(new BorderLayout(8,8)); p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
    p.add(new JScrollPane(contactTable), BorderLayout.CENTER);
    JPanel row = new JPanel(); JButton add = Main.button("Add", Theme.EMERALD), del = Main.button("Delete selected", Theme.RED), ref = Main.button("Refresh", Theme.FOREST);
    row.add(add); row.add(del); row.add(ref); p.add(row, BorderLayout.SOUTH);
    add.addActionListener(e -> {
      JTextField n = new JTextField(), ph = new JTextField(), pr = new JTextField("1");
      if(JOptionPane.showConfirmDialog(this, new Object[]{"Name",n,"Phone (e.g. +919876543210)",ph,"Priority (1 = first)",pr}, "Add contact", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
      run(() -> { api.addContact(n.getText().trim(), ph.getText().trim(), Integer.parseInt(pr.getText().trim())); }, this::refreshContacts);
    });
    del.addActionListener(e -> { int r = contactTable.getSelectedRow(); if(r<0) return; long id = Long.parseLong(contactModel.getValueAt(r,0).toString());
      run(() -> api.deleteContact(id), this::refreshContacts); });
    ref.addActionListener(e -> refreshContacts());
    return p;
  }

  private JPanel historyPanel(){
    JPanel p = new JPanel(new BorderLayout(8,8)); p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
    p.add(new JScrollPane(new JTable(alertModel)), BorderLayout.CENTER);
    JButton ref = Main.button("Refresh", Theme.FOREST); ref.addActionListener(e -> refreshAlerts()); p.add(ref, BorderLayout.SOUTH);
    return p;
  }

  private JPanel facilitiesPanel(){
    JPanel p = new JPanel(new GridLayout(0,1,10,10)); p.setBorder(BorderFactory.createEmptyBorder(24,24,24,24)); p.setBackground(Theme.SAGE);
    p.add(new JLabel("Opens Google Maps around the coordinates entered on the SOS tab. Results come from Google Maps, not from THUNAI."));
    String[][] items = {{"Nearby hospitals","hospital"},{"Nearby police stations","police station"},{"Nearby pharmacies","pharmacy"}};
    for(String[] it : items){
      JButton b = Main.button(it[0], Theme.FOREST);
      b.addActionListener(e -> { try { Links.open(Links.maps(it[1], Double.parseDouble(lat.getText().trim()), Double.parseDouble(lng.getText().trim()))); }
        catch(NumberFormatException n){ JOptionPane.showMessageDialog(this,"Enter valid coordinates on the SOS tab first."); }
        catch(Exception ex){ fail(ex); } });
      p.add(b);
    }
    return p;
  }

  /** Location is captured first (SOS tab), the alert is saved, then each saved contact gets a WhatsApp button. WhatsApp itself still needs the user to press Send. */
  private void offerWhatsApp(String type, double la, double lo, String summary){
    new SwingWorker<JsonNode,Void>(){
      protected JsonNode doInBackground() throws Exception { return api.contacts(); }
      protected void done(){
        try {
          JsonNode cs = get();
          String text = type + " alert from THUNAI. I need help. My location: " + Links.mapsLink(la, lo);
          JPanel p = new JPanel(new GridLayout(0,1,6,6));
          p.add(new JLabel("<html>" + summary.replace("\n","<br>") + "</html>"));
          if(cs.isEmpty()) p.add(new JLabel("No trusted contacts saved - add some in the Trusted contacts tab."));
          for(JsonNode c : cs){
            JButton b = Main.button("WhatsApp: " + c.get("name").asText(), Theme.EMERALD);
            b.addActionListener(e -> { try { Links.open(Links.whatsapp(c.get("phone").asText(), text)); } catch(Exception ex){ fail(ex); } });
            p.add(b);
          }
          JOptionPane.showMessageDialog(Dashboard.this, p, "Alert sent - notify contacts on WhatsApp", JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception ex){ fail(ex); }
      }
    }.execute();
  }

  interface Task { void run() throws Exception; }
  private void run(Task t, Runnable after){
    new SwingWorker<Void,Void>(){
      protected Void doInBackground() throws Exception { t.run(); return null; }
      protected void done(){ try { get(); after.run(); } catch(Exception ex){ fail(ex); } }
    }.execute();
  }
  private void fail(Exception ex){ Throwable c = ex.getCause()==null?ex:ex.getCause(); JOptionPane.showMessageDialog(this, c.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }

  private void refreshContacts(){
    new SwingWorker<JsonNode,Void>(){
      protected JsonNode doInBackground() throws Exception { return api.contacts(); }
      protected void done(){ try { contactModel.setRowCount(0);
        for(JsonNode c : get()) contactModel.addRow(new Object[]{c.get("id").asText(), c.get("name").asText(), c.get("phone").asText(), c.get("priority").asText()});
      } catch(Exception ex){ fail(ex); } }
    }.execute();
  }
  private void refreshAlerts(){
    new SwingWorker<JsonNode,Void>(){
      protected JsonNode doInBackground() throws Exception { return api.alerts(); }
      protected void done(){ try { alertModel.setRowCount(0);
        for(JsonNode v : get()){ JsonNode a = v.get("alert"); StringBuilder d = new StringBuilder();
          for(JsonNode x : v.get("deliveries")) d.append(x.get("status").asText()).append("; ");
          alertModel.addRow(new Object[]{a.get("createdAt").asText(), a.get("type").asText(), a.get("status").asText(), a.get("latitude").asText()+","+a.get("longitude").asText(), d.length()==0?"no contacts notified":d.toString()});
        }
      } catch(Exception ex){ fail(ex); } }
    }.execute();
  }
}
