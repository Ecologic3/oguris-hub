# Oguri's Hub

All five Oguri apps in one: **Alarms, Steps, Chips, Printer and
VPN Switch**, each ported in full under the hub package
(`app.oguri.hub.<module>`), behind one dark home screen. Every module
keeps 100% of its standalone features: the alarms band sync and sound
backup, the step counting service and home-screen widget, the poker
chip engine, both printer transports (BLE and SPP), and the VPN
switch's foreground service with its WiFi gate and two-phase re-check.

Package: `app.oguri.hub`, version 1.1 (versionCode 2), minSdk 29,
targetSdk 34.

## Changelog

### 1.1

Icons, the app name, and one removal. The launcher icon is
now Oguri herself: a head-and-shoulders portrait from her real
avatar artwork (the master render behind the dashboard lurker
sprite), background cut away and laid over the hub's deep teal-navy
plate. Each module also carries its own standalone app's launcher
icon again: the home-screen cards and the module headers show the
module's original icon (`ic_mod_*` resources) instead of the hub
icon. Icon assets are reproducible with
`python3 icon-src/generate_icon.py`; the avatar source art lives in
`icon-src/oguri-avatar-source.png`. The app is renamed from
"Oguri Hub" to "Oguri's Hub" (launcher label and home-screen
title). And the one-time migration import is gone: the import
activity, its home-screen buttons and the module-side JSON restore
paths are removed, the data migration being done. The Steps
module's own CSV history export/import is a long-standing feature
of the original app and stays.

### 1.0

Initial release: all five standalone apps ported into one hub, with
Steps and Alarms data import.

## Building

No Gradle: plain Java on the Android framework, built with the family
pipeline (aapt2, javac, d8, zipalign, apksigner; JDK 17 from the
workspace toolchain). Sources use anonymous inner classes only (no
lambdas; they break the d8 step in this pipeline), and `classes.dex`
must sit at the APK archive root, hence the zip from inside
`build/dex`.

```sh
./build.sh   # produces OguriHub-1.1.apk
```

The signing keystore (`oguris-hub.keystore`) is generated on first
build and is never committed.

## Data from the standalone apps

Version 1.0 carried a one-time migration path (JSON exports from
the Steps 1.2.5 and Alarms 1.8 migration releases, imported in the
hub); that migration is complete, and 1.1 removes the import side
again. Fresh-start behaviour, unchanged: the VPN module starts
with an empty home-network list (re-add each home WiFi with
**Add current network**), an in-progress Chips session in the
standalone app does not transfer, and the Printer module does not
remember the standalone app's last-used printer (pick it once in
the hub).
