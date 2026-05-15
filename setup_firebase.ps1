param()

# Generate SHA-256 hash for password
function Get-SHA256 {
    param([string]$Password)
    $hasher = [System.Security.Cryptography.SHA256]::Create()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Password)
    $hash = $hasher.ComputeHash($bytes)
    return [BitConverter]::ToString($hash).Replace("-", "").ToLower()
}

$demoHash = Get-SHA256 "light123"
$timestamp = [long](Get-Date -UFormat %s) * 1000

# Create panchayat JSON
$panchayatJson = @{
    panchayats = @{
        panchayat = @{
            id = "panchayat"
            name = "Demo Panchayat"
            villageId = "demo-village"
            passwordHash = $demoHash
            createdAt = $timestamp
        }
    }
} | ConvertTo-Json -Depth 10

$panchayatJson | Out-File -FilePath "panchayat.json" -Encoding UTF8

Write-Host "Created panchayat.json"
Write-Host "Password Hash: $demoHash"
Write-Host ""

# Create Firebase rules JSON
$rulesJson = @{
    rules = @{
        panchayats = @{
            ".read" = $false
            ".write" = $false
        }
        villages = @{
            ".read" = $true
            ".write" = $true
        }
    }
} | ConvertTo-Json -Depth 10

$rulesJson | Out-File -FilePath "firebase_rules.json" -Encoding UTF8

Write-Host "Created firebase_rules.json"
Write-Host ""
Write-Host "Next: Import panchayat.json to Firebase and update rules"
