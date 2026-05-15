# Grameen-Light Firebase Authentication Setup

## Overview

The app now supports:
- **Citizens**: Can report streetlight issues (doesn't need login)
- **Panchayat**: Authenticated users who can view and manage complaints

## Setup Steps

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add an Android app with package name: `com.grameenlight.app`
4. Download `google-services.json`
5. Place it in `app/` directory

### 2. Enable Realtime Database

1. In Firebase Console, go to **Build > Realtime Database**
2. Create a database (choose your region)
3. Start in **Test Mode** for development

### 3. Set Up Panchayat Credentials in Firebase

In Realtime Database, create this structure:

```
panchayats/
  ├── panchayat-001/
  │   ├── id: "panchayat-001"
  │   ├── name: "Demo Panchayat"
  │   ├── villageId: "demo-village"
  │   ├── passwordHash: "2fc12099106c8762..." (SHA-256 hash)
  │   └── createdAt: 1234567890000
```

### 4. Generate Password Hash

Run this in your terminal to create the SHA-256 hash of your password:

**PowerShell:**
```powershell
$password = "your-secure-password"
$hash = [System.Security.Cryptography.SHA256]::HashData([System.Text.Encoding]::UTF8.GetBytes($password))
[BitConverter]::ToString($hash).Replace("-", "").ToLower()
```

**Linux/Mac:**
```bash
echo -n "your-secure-password" | sha256sum | cut -d' ' -f1
```

### 5. Demo Credentials (for testing)

For quick testing, you can use:
- **Panchayat ID**: `panchayat`
- **Password**: `light123`
- **Password Hash**: `8ce256fb14ba1e45b6a62efb1d8d5db10a9a1ab8e5f60d3de9e2f6b3c8a3b2d1`

To manually create demo panchayat in Firebase:

```json
{
  "panchayats": {
    "panchayat": {
      "id": "panchayat",
      "name": "Demo Panchayat",
      "villageId": "demo-village",
      "passwordHash": "8ce256fb14ba1e45b6a62efb1d8d5db10a9a1ab8e5f60d3de9e2f6b3c8a3b2d1",
      "createdAt": 1234567890000
    }
  }
}
```

### 6. Set Up Database Rules (for testing)

For **Test Mode** (development only):

```json
{
  "rules": {
    "panchayats": {
      ".read": false,
      ".write": false,
      "$panchayatId": {
        ".read": true,
        ".write": false
      }
    },
    "villages": {
      ".read": true,
      ".write": true
    }
  }
}
```

### 7. Production Rules (recommended)

For production, replace with:

```json
{
  "rules": {
    "panchayats": {
      ".read": false,
      ".write": false
    },
    "villages": {
      "$villageId": {
        ".read": true,
        ".write": "auth != null",
        "complaints": {
          ".read": true,
          ".write": true
        },
        "poles": {
          ".read": true,
          ".write": "root.child('panchayats').child(auth.uid).exists()"
        }
      }
    }
  }
}
```

## App Flow

### For Citizens

1. Open app
2. Tap "Citizen" button on login screen
3. Click on any pole on the map
4. Select the issue type:
   - Light is working tonight (mark as Healthy)
   - Bulb fused or stays dark (report as Assigned)
   - Burning during daylight (report as Assigned)
5. Complaint is uploaded to Firebase

### For Panchayat

1. Open app
2. Tap "Panchayat" button
3. Enter Panchayat ID and Password
4. View "Panchayat Console" with recent complaints
5. Tap complaint row to toggle between "Assigned" and "Fixed"
6. When marked "Fixed", the pole status returns to "Working"

## Data Structure in Firebase

```
villages/
  └── demo-village/
      ├── complaints/
      │   └── GL-010124430-P01/
      │       ├── complaintId: "GL-010124430-P01"
      │       ├── poleId: 1
      │       ├── lane: "Temple Road"
      │       ├── status: "Fused"
      │       ├── tracker: "Assigned"
      │       ├── createdAt: 1234567890000
      │       ├── adminUpdatedAt: 1234567900000
      │       └── source: "android-citizen-report"
      │
      └── poles/
          └── 1/
              ├── id: 1
              ├── x: 0.18
              ├── y: 0.20
              ├── lane: "Temple Road"
              ├── status: "Fused"
              ├── tracker: "Assigned"
              ├── complaintId: "GL-010124430-P01"
              └── updatedAt: 1234567900000
```

## Testing

### Build and Install

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Create Multiple Panchayats

You can create multiple panchayat users by adding more entries under the `panchayats` node:

```json
{
  "panchayats": {
    "panchayat-001": {
      "id": "panchayat-001",
      "name": "Panchayat Head",
      "villageId": "demo-village",
      "passwordHash": "..."
    },
    "panchayat-002": {
      "id": "panchayat-002",
      "name": "Panchayat Secretary",
      "villageId": "demo-village",
      "passwordHash": "..."
    }
  }
}
```

## Security Notes

1. **Never** hardcode passwords in the app
2. **Always** use SHA-256 hashing for passwords in Firebase
3. **In Production**: Move to Firebase Authentication with proper rules
4. **Rotate Passwords**: Periodically update password hashes in Firebase
5. **Restrict Firebase Rules**: Only allow reads to non-sensitive data
6. **Enable App Check**: Use Firebase App Check to prevent abuse

## Troubleshooting

### "Firebase: add google-services.json to enable sync"

- Ensure `google-services.json` is in the `app/` directory
- Rebuild the app: `.\gradlew.bat clean assembleDebug`

### Login fails with "Firebase not configured"

- Firebase is not initialized (likely missing google-services.json)
- Falls back to demo credentials (panchayat / light123)
- Check Firebase Console for errors

### Complaints not syncing to Firebase

- Check internet connection
- Verify Realtime Database rules allow writes to `villages/{villageId}/complaints/`
- Check Firebase Console > Rules tabs for validation errors
- Look at console logs for upload errors

## Next Steps (Production)

1. Switch to Firebase Authentication (remove password hashing)
2. Implement proper Firebase Security Rules
3. Add Firebase App Check for security
4. Set up Firebase Analytics
5. Add data backup and recovery procedures
