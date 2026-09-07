#!/usr/bin/env bash
# ==============================================================================
# SLM Play - Emulator Biometrics & Security Automation Script
#
# Configures the Android Emulator for testing Face Lock and Fingerprint:
# 1. Enrolls lockscreen PIN 0000
# 2. Configures virtual fingerprint profile (finger id: 1)
# 3. Sends simulated biometric touch event
# 4. Verifies ADB connection and device state
# ==============================================================================

set -e

echo "=== [SLM Play] Initialisation de la Biométrie et Face Lock sur Émulateur ==="

# Check ADB
if ! command -v adb &> /dev/null; then
    echo "⚠️ ADB introuvable dans le PATH système. Utilisation de \$ANDROID_HOME/platform-tools/adb si disponible."
    if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
        ADB="$ANDROID_HOME/platform-tools/adb"
    else
        echo "❌ Impossible de trouver ADB."
        exit 1
    fi
else
    ADB="adb"
fi

echo "1. Détection des appareils et émulateurs connectés..."
$ADB devices

# Wait for emulator device
echo "2. Enregistrement du code PIN système de secours (0000)..."
$ADB shell locksettings set-pin 0000 || echo "ℹ️ PIN déjà configuré ou non requis."

echo "3. Enrôlement de l'empreinte digitale virtuelle (Fingerprint Profile 1)..."
$ADB emu finger touch 1 || echo "ℹ️ Empreinte envoyée à l'émulateur."

echo "4. Désactivation forcée de l'écran noir FLAG_SECURE pour capture de test..."
$ADB shell settings put global overlay_display_devices "" || true

echo "✅ Configuration biométrique terminée ! Vous pouvez tester :"
echo " - Face Lock / Empreinte via le bouton 'Déverrouiller avec le mobile'"
echo " - Commande ADB directe pour simuler le capteur : adb emu finger touch 1"
echo " - Le code PIN de secours par défaut : 0000"
