param(
    [string]$EnvFile = ".env"
)

# Génère un secret 48 bytes encodé en Base64 (256 bits de sécurité)
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)

if (-not (Test-Path $EnvFile)) {
    @"
# MongoDB
MONGO_USERNAME=admin
MONGO_PASSWORD=admin123

# JWT
JWT_SECRET=$secret

# Application
SPRING_PROFILES_ACTIVE=dev
"@ | Set-Content $EnvFile -Encoding UTF8
    Write-Host "Créé '$EnvFile' avec JWT_SECRET généré."
    exit 0
}

$content = Get-Content $EnvFile
$found = $false
$newContent = $content | ForEach-Object {
    if ($_ -match '^[ \t]*JWT_SECRET\s*=') {
        $found = $true
        "JWT_SECRET=$secret"
    } else {
        $_
    }
}

if (-not $found) {
    $newContent += "JWT_SECRET=$secret"
}

$newContent | Set-Content $EnvFile -Encoding UTF8
Write-Host "JWT_SECRET généré et appliqué dans '$EnvFile'."