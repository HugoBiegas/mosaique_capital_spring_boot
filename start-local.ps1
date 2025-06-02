# =================================================================
# SCRIPT DE DÉMARRAGE LOCAL WINDOWS - MOSAÏQUE CAPITAL
# =================================================================
# Usage: .\start-local.ps1

param(
    [switch]$SkipCompile
)

# Configuration des couleurs
$Host.UI.RawUI.WindowTitle = "Mosaïque Capital - Démarrage Local"

Write-Host "===================================================================" -ForegroundColor Blue
Write-Host "              MOSAÏQUE CAPITAL - DÉMARRAGE LOCAL" -ForegroundColor Blue
Write-Host "===================================================================" -ForegroundColor Blue

# Vérifier si le fichier .env existe
if (-not (Test-Path ".env")) {
    Write-Host "❌ Fichier .env non trouvé !" -ForegroundColor Red
    Write-Host "💡 Copiez .env.example vers .env et configurez vos variables" -ForegroundColor Yellow
    exit 1
}

# Charger les variables d'environnement depuis .env
Write-Host "🔧 Chargement des variables d'environnement..." -ForegroundColor Blue

Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#].*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()

        # Enlever les guillemets si présents
        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "   ✓ $name" -ForegroundColor Green
    }
}

Write-Host "✅ Variables d'environnement chargées" -ForegroundColor Green

# Vérifier les variables critiques
Write-Host "🔍 Vérification des variables critiques..." -ForegroundColor Blue
$criticalVars = @("APP_JWT_SECRET", "SPRING_DATASOURCE_PASSWORD")
$missingVars = @()

foreach ($var in $criticalVars) {
    $value = [System.Environment]::GetEnvironmentVariable($var, "Process")
    if ([string]::IsNullOrEmpty($value)) {
        $missingVars += $var
    }
}

if ($missingVars.Count -gt 0) {
    Write-Host "❌ Variables manquantes dans .env : $($missingVars -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Variables critiques validées" -ForegroundColor Green

# Afficher la configuration
Write-Host "📋 Configuration détectée :" -ForegroundColor Blue
$profile = [System.Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "Process")
$port = [System.Environment]::GetEnvironmentVariable("SERVER_PORT", "Process")
$dbUrl = [System.Environment]::GetEnvironmentVariable("SPRING_DATASOURCE_URL", "Process")
$redisHost = [System.Environment]::GetEnvironmentVariable("SPRING_REDIS_HOST", "Process")
$redisPort = [System.Environment]::GetEnvironmentVariable("SPRING_REDIS_PORT", "Process")

Write-Host "   - Profil Spring: $($profile ?? 'dev')" -ForegroundColor Cyan
Write-Host "   - Port serveur: $($port ?? '9999')" -ForegroundColor Cyan
Write-Host "   - Base de données: $($dbUrl ?? 'Non définie')" -ForegroundColor Cyan
Write-Host "   - Redis: $($redisHost ?? 'localhost'):$($redisPort ?? '6379')" -ForegroundColor Cyan

# Nettoyer et compiler si demandé
if (-not $SkipCompile) {
    Write-Host "🧹 Nettoyage et compilation..." -ForegroundColor Blue
    & .\mvnw.cmd clean compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erreur lors de la compilation" -ForegroundColor Red
        exit 1
    }
}

# Démarrer l'application
Write-Host "🚀 Démarrage de l'application Spring Boot..." -ForegroundColor Green
Write-Host "   Pour arrêter : Ctrl+C" -ForegroundColor Yellow
Write-Host ""

& .\mvnw.cmd spring-boot:run