# Grameen-Light Firebase Setup

1. Open Firebase Console and create/select a project.
2. Add an Android app with package name:

```text
com.grameenlight.app
```

3. Download `google-services.json`.
4. Put it here:

```text
app/google-services.json
```

5. In Firebase Console, open Build > Realtime Database, create a database, and start in test mode for demo/student testing.
6. Rebuild and install:

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Reports are written under:

```text
villages/demo-village/complaints/{complaintId}
villages/demo-village/poles/{poleId}
```

Suggested demo rules:

```json
{
  "rules": {
    "villages": {
      "$villageId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

For a real deployment, replace open rules with authenticated Panchayat/admin access.

## Admin Mode

The demo app has a built-in Panchayat login:

```text
username: panchayat
password: light123
```

After login, tap a complaint row in the Panchayat Console to move it between `Assigned` and `Fixed`.
When a complaint is marked `Fixed`, the related pole turns back to `Working`.

For production, do not keep shared passwords in the APK. Move Panchayat users to Firebase Authentication or a protected `/admins` node with proper rules.
