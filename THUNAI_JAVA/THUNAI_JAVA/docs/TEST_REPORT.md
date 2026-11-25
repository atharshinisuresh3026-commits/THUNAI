# Test report
Written in a sandbox with a Java 21 runtime only: no javac, Maven, MySQL, Android SDK or network.
Compiled: NOTHING. Tests run: NONE. Expect compile errors; fix them and re-run.
Never verified: backend startup, MySQL connectivity, OTP flow, login, contacts, SOS persistence, history, authorization, desktop UI, Android UI/permissions, WhatsApp hand-off, Maps hand-off, Twilio SMS.
Not implemented: push notifications, live location sharing, journey/check-in timer, settings/language, in-app map (Maps is opened externally), profile screen, tests.
Known limits: token stored unencrypted on Android; cleartext http allowed for local testing; no rate limiting except 5 OTP attempts; SOS has no confirmation step; bare 10-digit contact numbers are assumed to be India (+91) for WhatsApp.

## Partial check that WAS done
All backend, desktop and Android .java files were run through the JDK compiler without Spring/Jackson/Android libraries. No syntax (parse) errors were reported; the only errors were "cannot find symbol / package does not exist / does not override" caused by those missing libraries. This is NOT a real build: type errors against the real libraries are still possible.
