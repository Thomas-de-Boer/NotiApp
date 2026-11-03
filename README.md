# Gezinsbel 🔔

Een eenvoudige, privé Android-app voor ons gezin. Druk op een knop zoals **"Het eten is klaar"** en alle
telefoons in het gezin krijgen een melding. Tikt iemand de melding niet aan, dan gaat zijn of haar telefoon
na een tijdje **bellen als een inkomend gesprek** totdat er op "OK" gedrukt wordt.

## Hoe werkt het?
- Elke telefoon met de app luistert naar hetzelfde gezinskanaal via Firebase Cloud Messaging (gratis, van Google).
- Als je op een knop drukt, stuurt jouw telefoon het bericht naar dat kanaal.
- Er is geen eigen server, geen account en geen creditcard nodig.

## Eenmalige installatie (Firebase)

### 1. Firebase-project aanmaken
1. Ga naar <https://console.firebase.google.com> en log in met je Google-account.
2. Klik **Project toevoegen**, kies een naam (bijv. `gezinsbel`). Google Analytics mag uit.
3. Blijf op het gratis **Spark**-abonnement, een creditcard is niet nodig.
4. Klik in het project op het **Android-icoon** om een app toe te voegen:
   - Pakketnaam: `com.notiapp`
   - Klik **App registreren** en download **`google-services.json`**.
5. Zet dat bestand in de map `app/` van dit project (dus `app/google-services.json`).

### 2. Sleutel om berichten te versturen
1. Ga naar <https://console.cloud.google.com/iam-admin/serviceaccounts> en kies hetzelfde project.
2. Klik **Serviceaccount maken**, met als naam `gezinsbel-zender`.
3. Geef het **alleen** de rol **Firebase Cloud Messaging API Admin** en klik op **Gereed**.
4. Klik op het nieuwe serviceaccount, ga naar **Sleutels**, kies **Sleutel toevoegen**, dan **Nieuwe sleutel maken**, dan **JSON**.
5. Hernoem het gedownloade bestand naar `fcm_service_account.json` en zet het in
   `app/src/main/assets/` (dus `app/src/main/assets/fcm_service_account.json`).

> ⚠️ Deze twee bestanden zijn geheim en staan bewust **niet** in git (zie `.gitignore`).
> Deel de app alleen binnen het gezin.

### 3. App bouwen en installeren
1. Open het project in Android Studio.
2. Kies **Build**, dan **Generate App Bundles or APKs**, dan **Build APK(s)**.
3. Stuur het APK-bestand (`app/build/outputs/apk/debug/app-debug.apk`) naar elke telefoon in het gezin
   (bijv. via WhatsApp of Google Drive) en installeer het. Sta "installeren uit onbekende bronnen" toe.
4. Open de app op elke telefoon, vul je naam in en geef alle gevraagde toestemmingen.

## Gebruik
- **Knop indrukken**: alle andere gezinsleden krijgen meteen een melding.
- **Niet gereageerd?** Na 30 seconden (instelbaar) gaat de telefoon bellen tot er op **OK** gedrukt wordt.
- **Gezien door**: je ziet wie je bericht heeft gezien.
- **Annuleren**: stop het bellen op alle telefoons.
- **Instellingen**: berichten aanpassen, toevoegen of verwijderen, je naam en de belvertraging wijzigen.

## Tips als meldingen niet aankomen
- Zet bij **Batterij** voor Gezinsbel de optie op **Onbeperkt** / **Niet optimaliseren**.
- Samsung: Instellingen, dan Batterij, dan Achtergrondgebruikslimieten. Zet Gezinsbel **niet** bij "slapende apps".
- Controleer dat meldingen en "volledig scherm meldingen" voor de app aan staan.
