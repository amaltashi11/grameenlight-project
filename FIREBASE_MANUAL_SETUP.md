# Firebase Manual Setup Instructions

## Files Generated

Two configuration files have been automatically generated:

1. **panchayat.json** - Demo panchayat user with credentials
2. **firebase_rules.json** - Database security rules for testing

## Manual Steps Required (You must do these)

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Add an Android app with package: `com.grameenlight.app`
4. Download `google-services.json`

### Step 2: Setup Realtime Database

1. In Firebase Console → Build → **Realtime Database**
2. Click **Create Database**
3. Choose region (any region works)
4. Start in **Test Mode**

### Step 3: Import Database Rules

1. Click **Rules** tab in Realtime Database
2. Replace all content with this:

```json
{
  "rules": {
    "panchayats": {
      ".read": false,
      ".write": false
    },
    "villages": {
      ".read": true,
      ".write": true
    }
  }
}
```

3. Click **Publish**

### Step 4: Import Panchayat Credentials

1. Click **Data** tab in Realtime Database
2. Click the three-dot menu (⋮)
3. Click **Import JSON**
4. Select and upload **panchayat.json**

### Step 5: Setup App

1. Copy `google-services.json` to: `app/` folder
2. Run: `.\gradlew.bat clean assembleDebug`
3. Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`

## Test Credentials

After setup:
- Tap **Panchayat** on login screen
- Panchayat ID: `panchayat`
- Password: `light123`

## Demo Panchayat Details

```json
{
  "id": "panchayat",
  "name": "Demo Panchayat",
  "villageId": "demo-village",
  "passwordHash": "8a1830988d50ab17f87b4bf9389ef0894255676954af312b45e6913dab3d5a45",
  "createdAt": 1778172654000
}
```

## Create Your Own Panchayat

To add more panchayat users with custom passwords:

```powershell
# Generate password hash
$password = "your-new-password"
$hash = [System.Security.Cryptography.SHA256]::HashData([System.Text.Encoding]::UTF8.GetBytes($password))
[BitConverter]::ToString($hash).Replace("-", "").ToLower()
```

Then add to Firebase `/panchayats/` with structure:

```json
{
  "panchayat-001": {
    "id": "panchayat-001",
    "name": "Your Panchayat Name",
    "villageId": "demo-village",
    "passwordHash": "YOUR_GENERATED_HASH_HERE",
    "createdAt": 1778172654000
  }
}
```

## Files Location

Generated files are in project root:
- `panchayat.json` - Ready to import
- `firebase_rules.json` - Security rules
- `setup_firebase.ps1` - This script (can re-run to update credentials)

## Troubleshooting

**"Firebase not configured" on app:**
- google-services.json not in app/ folder
- Rebuild: `.\gradlew.bat clean assembleDebug`

**Login fails:**
- Check panchayat user exists in Firebase
- Verify password hash is correct

**Data not syncing:**
- Check Firebase Rules are published
- Ensure database is in Test Mode
- Check internet connection

## What's Working Now

✅ App installed on Vivo device
✅ Login screen with Citizen/Panchayat mode
✅ Demo credentials generated
✅ Firebase config templates ready

Next: Complete manual Firebase setup steps above
