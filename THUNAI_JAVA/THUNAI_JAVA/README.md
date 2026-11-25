# THUNAI (Java) - UNVERIFIED SOURCE (nothing here has been compiled or run)
Parts: backend (Spring Boot + MySQL), desktop-app (Java Swing), android-app (Java, no XML layouts), database, scripts, docs.

## What the SOS flow does
1. You tap HARASSMENT / MEDICAL / EMERGENCY SOS.
2. App asks for location permission (if denied, the alert goes out without a location and says so).
3. Location is captured, the alert is saved to MySQL via the backend (and the backend texts contacts via Twilio only if configured).
4. WhatsApp opens with the message + Google Maps link for your first trusted contact; a dialog lists WhatsApp buttons for every contact.
   WhatsApp does not allow silent sending: you must press Send in WhatsApp for each contact.
5. Nearby hospitals/police/pharmacies open in Google Maps (real results, nothing invented).

## Verified login
Register -> a 6-digit code is sent by SMS (Twilio) -> enter the code -> account verified -> JWT login.
Without Twilio credentials the code CANNOT be delivered. For local testing only, set THUNAI_DEV_OTP_LOG=true and the code prints in the backend console.

## Setup (Windows)
1. Install JDK 17+, Maven 3.9+, MySQL 8, Android Studio (for the Android app).
2. mysql -u root -p < database\schema.sql   (if you ran the older schema: run database\migration_001_verified.sql instead)
3. Set environment variables from backend\.env.template, then scripts\run-backend.ps1
4. Desktop: scripts\build-desktop.ps1 then scripts\run-desktop.ps1
5. Android: open android-app in Android Studio, sync, run on emulator (backend URL http://10.0.2.2:8080) or on a phone (use your PC's LAN IP, same Wi-Fi).
