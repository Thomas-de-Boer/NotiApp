# Noti App 🔔

A simple, private Android app for our family. Tap a button such as **"Het eten is klaar"** (dinner's ready) and
every phone in the family gets a notification. If someone doesn't tap the notification, their phone starts
**ringing like an incoming call** after a short delay, until they press "OK".

The app itself is in Dutch. These setup instructions are in English to match the English Firebase / Google Cloud screens.

## How it works
- Every phone with the app listens to the same family channel (an FCM *topic* called `gezin`) via
  Firebase Cloud Messaging, Google's free push service.
- When you tap a button, your phone sends the message to that channel.
- No own server, no accounts and no credit card needed (Firebase free **Spark** plan).

## One-time setup

### 1. Create the Firebase project
1. Go to <https://console.firebase.google.com> and sign in with your Google account.
2. Click **Create a project** (or **Add project**) and give it a name, e.g. `noti-app`.
   Google Analytics can be turned off.
3. Stay on the free **Spark** plan. No credit card is needed.
4. In the project overview, click the **Android** icon to add an app:
   - **Android package name:** `com.notiapp`
   - Click **Register app**, then **Download google-services.json**.
   - You can skip the remaining "Add Firebase SDK" steps. The project is already set up for that.
5. Put the file in the `app/` folder of this project: `app/google-services.json`.

### 2. Create the key that lets the app send messages
The app sends messages using a *service account key*. We create one that is **only** allowed to send notifications.

1. Go to <https://console.cloud.google.com/iam-admin/serviceaccounts>.
2. At the top of the page, make sure the **same project** as your Firebase project is selected.
3. Click **+ Create service account**.
   - **Service account name:** `noti-app-sender`
   - Click **Create and continue**.
4. Under **Grant this service account access to project**, open the **Select a role** box, type
   `Firebase Cloud Messaging API Admin`, select it, and click **Continue**, then **Done**.
   (Don't add any other roles.)
5. In the list, click the new service account (`noti-app-sender@...`).
6. Open the **Keys** tab, then click **Add key**, then **Create new key**. Choose **JSON** and click **Create**.
   A `.json` file is downloaded.
7. Rename that file to `fcm_service_account.json` and put it in the folder `app/src/main/assets/`
   (create the `assets` folder if it doesn't exist). The full path is:
   `app/src/main/assets/fcm_service_account.json`

> If step 6 says key creation is disabled by an organization policy: that only happens with work or school
> Google accounts. Use a personal Google account (gmail.com) for the Firebase project instead.

> ⚠️ Both files are secret and are deliberately **not** stored in git (see `.gitignore`).
> Only share the app with your family.

### 3. Build and install the app
1. Open the project in Android Studio.
2. Build the **release** version: open the **Terminal** tab at the bottom of Android Studio and run
   `.\gradlew assembleRelease`
   (or choose **Build**, then **Select Build Variant…**, set it to **release**, then **Build**, then
   **Generate App Bundles or APKs**, then **Generate APKs**).
   The first release build can take several minutes. After that it's much faster.
3. Send the APK file (`app/build/outputs/apk/release/app-release.apk`) to each family phone (e.g. via WhatsApp
   or Google Drive) and install it. Allow "install unknown apps" when asked.

> The release version is signed with Android Studio's debug key (`C:\Users\<you>\.android\debug.keystore`).
> Keep a backup of that file: updates only install over the existing app when they're signed with the same key.

**Updating the app later:** increase `versionCode` (e.g. `1` to `2`) in `app/build.gradle.kts`, build the release
APK again, and install it over the old one. Names and settings on the phones are kept.
4. Open the app on each phone, enter your name and grant all requested permissions.

## Using the app
- **Tap a button:** the message is sent right away.
- **"Aan:"** (to): choose who gets it. **"Iedereen"** (everyone) is the default. Tap one or more names to send a
  personal or group message; only those phones get it. After sending it switches back to "Iedereen".
- **"Eigen bericht…"** (custom message): the button at the bottom lets you type any message.
- A message containing `…` (e.g. "We vertrekken naar …") asks you to fill in the rest when sending.
- **No response?** After 30 seconds the phone rings like a call until someone presses **"OK, gezien"**.
  - Phone on normal: ringtone and vibration.
  - Phone on **vibrate or silent: vibration only**.
- **"Gezien door …"** (seen by): you can see who has seen your message.
- **"Stop het bellen bij iedereen"**: stops the ringing on every phone.
- **Geschiedenis** (history, clock icon): all messages you received ("Ontvangen") and sent ("Verstuurd"), with
  whether they were seen, missed ("Gemist") or cancelled. Stored on the phone (last 200).
- **Instellingen** (settings, gear icon):
  - **"Wie heeft de app?"**: everyone in the family who has the app.
  - **"Beltoon"**: ringtone. Default is your phone's own ringtone.
  - **"Trilpatroon"**: vibration pattern, e.g. "Snel dubbel (brr brr)". Tap one to feel it.
  - Edit, add, reorder or remove the message buttons, and change your name.
  - **"Test het bellen"**: test the ringing on your own phone.

## Who has the app (members)
- A phone **joins automatically** when someone installs the app and enters their name. It then shows up for
  everyone under "Wie heeft de app?" and in the "Aan:" row within seconds.
- A phone is **removed automatically** when the app is uninstalled. An app can't do anything at the moment it's
  deleted, but Google marks it as gone. The other phones check this every 6 hours (or when you tap the refresh
  button on "Wie heeft de app?"), so the person usually disappears within a few hours. You can also remove
  someone by hand with the bin icon; they reappear if they open the app again.
- Brand-new phones sometimes need a minute before family messages reach them. If someone is missing,
  open the app on their phone and tap refresh on "Wie heeft de app?".

## If notifications don't arrive
- Set the app's **Battery** setting to **Unrestricted** / **Don't optimize**.
- Samsung: Settings, then Battery, then Background usage limits. Make sure Noti App is **not** in "Sleeping apps".
- Check that notifications and "full screen notifications" are allowed for the app.
- Phones that were off or offline for more than 15 minutes ignore the old message when they come back online.
