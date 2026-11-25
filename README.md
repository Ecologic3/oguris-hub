# Oguri's Hub

All five Oguri apps in one: **Alarms, Steps, Chips, Printer and
VPN Switch**, each ported in full under the hub package
(`app.oguri.hub.<module>`), behind one dark home screen. Every module
keeps 100% of its standalone features: the alarms band sync and sound
backup, the step counting service and home-screen widget, the poker
chip engine, both printer transports (BLE and SPP), and the VPN
switch's foreground service with its WiFi gate and two-phase re-check.

Package: `app.oguri.hub`, version 1.3 (versionCode 4), minSdk 29,
targetSdk 34.

## Changelog

### 1.3

UI unification across all five modules, from the consistency
sweep of 25 Nov 2026. A shared UI kit (`app.oguri.hub.UiKit`) now
carries the geometry every module used to re-implement privately:
one background (the hub shell's #0D2030, which Steps and VPN also
use now instead of two near-identical private navies), one card
(16dp radius, shared padding and spacing), one section heading
(18sp), one module header, one root-padding rhythm, and system
bars set on every screen. One permission model everywhere, the
Alarms one: a status line that always shows where things stand
and granted buttons faded to 0.45, never removed. VPN gives up
its disappear-on-grant behaviour (and the code comment that
wrongly claimed it matched Alarms), Steps' permission rows keep
their buttons and fade them, and Printer gains a visible
permission status line and buttons where it previously requested
silently at Scan time. Chips' setup header joins the convention
(30sp near-white, was 26sp gold; gold remains its accent) and its
in-game top bar follows on title size, colour and icon size;
Chips also loses the emoji in its button labels. The hub home
cards are named exactly like the module headers and their accent
bars now take each module's own accent colour, and the "All five
Oguri apps, one home." tagline is gone. Text rule throughout:
status lines yes, paragraphs no (VPN's permission paragraph is a
status line now, Steps' permanent footer is removed). No module
logic changed: the band stack, counting engine, game engine,
printer transports and the VPN service are untouched.

### 1.2

The VPN module catches up with the standalone app's final UI pass
(Oguri's VPN Switch 1.10, which landed after the hub's 1.0 port).
The module header now follows the hub's module convention (name on
the left, the module's own icon at the top right) and reads
"Oguri's VPN Switch", as does the home-screen card. The "Tailscale
off on your home WiFi, on everywhere else." subheader is gone, all
buttons are sentence case instead of all caps, and the permission
buttons phase out once granted: each disappears when its
permission is in place, and the whole permissions card goes once
both are granted. The module's switching logic, its midnight
indigo colors and every other module are unchanged.

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
./build.sh   # produces OguriHub-1.3.apk
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
