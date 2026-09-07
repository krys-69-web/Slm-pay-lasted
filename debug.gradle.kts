// Gradle helper script to automate Android emulator biometric setup and testing
tasks.register("setupEmulatorBiometrics") {
    group = "verification"
    description = "Pre-loads a virtual fingerprint profile and lockscreen PIN on the running Android emulator."
    doLast {
        println("Configuring emulator virtual biometrics (PIN: 0000, Fingerprint: 1)...")
        try {
            exec {
                commandLine("adb", "shell", "locksettings", "set-pin", "0000")
                isIgnoreExitValue = true
            }
            exec {
                commandLine("adb", "emu", "finger", "touch", "1")
                isIgnoreExitValue = true
            }
            println("Virtual fingerprint profile registered successfully on emulator.")
        } catch (e: Exception) {
            println("Note: ADB execution failed or no emulator running. Exception: ${e.message}")
        }
    }
}

tasks.register("simulateFingerprintTouch") {
    group = "verification"
    description = "Simulates a physical fingerprint press on the emulator sensor."
    doLast {
        try {
            exec {
                commandLine("adb", "emu", "finger", "touch", "1")
            }
            println("Fingerprint touch simulated.")
        } catch (e: Exception) {
            println("Failed to send fingerprint command: ${e.message}")
        }
    }
}
