# Oguri Hub

All five Oguri apps in one: **Alarms, Steps, Chips, Printer and
VPN Switch**, each ported in full under the hub package
(`app.oguri.hub.<module>`), behind one dark home screen. Every module
keeps 100% of its standalone features: the alarms band sync and sound
backup, the step counting service and home-screen widget, the poker
chip engine, both printer transports (BLE and SPP), and the VPN
switch's foreground service with its WiFi gate and two-phase re-check.

Package: `app.oguri.hub`, version 1.0 (versionCode 1), minSdk 29,
targetSdk 34.

## Building

No Gradle: plain Java on the Android framework, built with the family
pipeline (aapt2, javac, d8, zipalign, apksigner; JDK 17 from the
workspace toolchain). Sources use anonymous inner classes only (no
lambdas; they break the d8 step in this pipeline), and `classes.dex`
must sit at the APK archive root, hence the zip from inside
`build/dex`.

```sh
./build.sh   # produces OguriHub-1.0.apk
```

The signing keystore (`oguris-hub.keystore`) is generated on first
build and is never committed.

## Moving your data from the standalone apps

Android keeps each app's data in its own sandbox, so the hub cannot
read the standalone apps' data directly. Steps and Alarms have
one-time migration releases with an export; the other three modules
start fresh (details below).

### Steps

1. Install `OgurisSteps-1.2.5-export.apk` (in `~/workspace/steps-app`)
   over your current Oguri's Steps. It is signed with the same key,
   so it installs in place and your history stays put.
2. Open Steps, scroll to Setup, tap **Export data (JSON)** and save
   the file (suggested name `oguris-steps-export.json`).
3. Install `OguriHub-1.0.apk`, open the hub, tap
   **IMPORT STEPS DATA** and pick the file. The hub reports how many
   days it imported. Days merge with any hub history, keeping the
   higher count per day; your paused setting comes across too.

### Alarms

1. Install `OgurisAlarms-1.8-export.apk` (in `~/workspace/alarm-app`)
   over your current Oguri's Alarms (same key, installs in place).
2. Open Alarms, go to Setup, tap **Export data (for Oguri Hub)** and
   save the file. Note: the file contains your band's bond secret
   key, so keep it somewhere private and delete it after importing.
3. In the hub, tap **IMPORT ALARMS DATA** and pick the file. The hub
   restores every alarm exactly as exported and re-registers the
   switched-on ones with the system alarm service immediately; it
   also attempts one band sync so the band matches.

### The other three modules

- **VPN Switch:** starts with an empty home-network list. Open the
  module at home and use **Add current network** for each home WiFi.
  Monitoring is on by default, as in the standalone app.
- **Chips:** an in-progress session in the standalone app does not
  transfer (it lives in that app's sandbox). The hub starts fresh;
  the saved-game format is unchanged, so the hub saves and resumes
  its own sessions exactly like the standalone app did.
- **Printer:** the last-used printer does not transfer. Pick your
  printer in the hub's device list once; from then on it is
  remembered the same way as before.

The standalone apps keep working untouched; uninstall them whenever
you are satisfied the hub has everything.
