#!/bin/bash
# Firebase Setup Helper for Grameen-Light
# This script helps initialize the Firebase Realtime Database structure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Grameen-Light Firebase Setup Helper ===${NC}\n"

# Function to generate SHA-256 hash
generate_hash() {
    local password=$1
    echo -n "$password" | sha256sum | cut -d' ' -f1
}

# Function to create panchayat JSON
create_panchayat_json() {
    local panchayat_id=$1
    local panchayat_name=$2
    local password=$3
    local village_id=$4
    
    local password_hash=$(generate_hash "$password")
    local timestamp=$(date +%s)000
    
    cat > "panchayat_${panchayat_id}.json" << EOF
{
  "panchayats": {
    "${panchayat_id}": {
      "id": "${panchayat_id}",
      "name": "${panchayat_name}",
      "villageId": "${village_id}",
      "passwordHash": "${password_hash}",
      "createdAt": ${timestamp}
    }
  }
}
EOF
    
    echo -e "${GREEN}✓ Created panchayat_${panchayat_id}.json${NC}"
    echo -e "  Panchayat ID: ${panchayat_id}"
    echo -e "  Name: ${panchayat_name}"
    echo -e "  Password Hash: ${password_hash}\n"
}

# Function to create Firebase rules JSON
create_firebase_rules() {
    cat > "firebase_rules.json" << 'EOF'
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
      ".write": true,
      "$villageId": {
        "complaints": {
          ".read": true,
          ".write": true
        },
        "poles": {
          ".read": true,
          ".write": true
        }
      }
    }
  }
}
EOF
    echo -e "${GREEN}✓ Created firebase_rules.json (for Test Mode)${NC}\n"
}

# Create demo panchayat
echo -e "${YELLOW}Creating Demo Panchayat...${NC}"
create_panchayat_json "panchayat" "Demo Panchayat" "light123" "demo-village"

# Create production rules
echo -e "${YELLOW}Creating Firebase Rules...${NC}"
create_firebase_rules

# Create multiple panchayats example
echo -e "${YELLOW}Creating Multiple Panchayats Example...${NC}"
cat > "panchayats_bulk.json" << 'EOF'
{
  "panchayats": {
    "panchayat-001": {
      "id": "panchayat-001",
      "name": "Panchayat Head - Village A",
      "villageId": "demo-village",
      "passwordHash": "REPLACE_WITH_HASH_1",
      "createdAt": 1234567890000
    },
    "panchayat-002": {
      "id": "panchayat-002",
      "name": "Panchayat Secretary - Village A",
      "villageId": "demo-village",
      "passwordHash": "REPLACE_WITH_HASH_2",
      "createdAt": 1234567890000
    },
    "panchayat-003": {
      "id": "panchayat-003",
      "name": "Panchayat Head - Village B",
      "villageId": "village-b",
      "passwordHash": "REPLACE_WITH_HASH_3",
      "createdAt": 1234567890000
    }
  }
}
EOF
echo -e "${GREEN}✓ Created panchayats_bulk.json (template)${NC}\n"

echo -e "${YELLOW}=== Setup Complete ===${NC}\n"

echo -e "${GREEN}Files Created:${NC}"
echo "  1. panchayat.json - Demo panchayat ready to import"
echo "  2. firebase_rules.json - Security rules for Test Mode"
echo "  3. panchayats_bulk.json - Template for multiple panchayats\n"

echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Go to Firebase Console → Your Project → Realtime Database"
echo -e "  2. Click 'Rules' tab and paste the content from ${GREEN}firebase_rules.json${NC}"
echo -e "  3. Click 'Data' tab, then import ${GREEN}panchayat.json${NC}"
echo -e "  4. Download ${GREEN}google-services.json${NC} from Firebase Console"
echo -e "  5. Copy it to: ${GREEN}app/${NC} folder"
echo -e "  6. Rebuild and reinstall the app\n"

echo -e "${YELLOW}To generate password hashes manually:${NC}"
echo "  echo -n 'your-password' | sha256sum | cut -d' ' -f1\n"
