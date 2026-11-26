# Oguri's Hub

All five Oguri apps in one: **Alarms, Steps, Chips, Printer and
VPN Switch**, each ported in full under the hub package
(`app.oguri.hub.<module>`), behind one dark home screen, plus
(since 1.8) a **Mnemosyne** module that frames the mnemosyne
dashboard in a WebView. Every module
keeps 100% of its standalone features: the alarms band sync and sound
backup, the step counting service and home-screen widget, the poker
chip engine, both printer transports (BLE and SPP), and the VPN
switch's foreground service with its WiFi gate and two-phase re-check.

Package: `app.oguri.hub`, version 1.12 (versionCode 13), minSdk 29,
targetSdk 34.

## Changelog

### 1.12

The motes move smoothly now, and there are more of them.
Adam's field report on 1.11: "very jagged and not smooth at
all", plus a wish for higher density. The jaggedness had two
causes, both in MotesView: the phase was computed by dividing
the raw uptime clock in float32, which quantises time into
visible steps once the phone has been up a while, and the
redraw loop used a plain 16 ms delayed invalidate, which
drifts against the display's vsync. The phase is now computed
in double precision from elapsed time, and the loop redraws
with postInvalidateOnAnimation. Density rises from 24 specks
to 44, same colours, sizes and rise times.

### 1.11

The hub home screen is alive: behind the module cards, tiny
specks in the six module accent colours drift slowly upward,
fading in at the bottom and out at the top, each with a gentle
sideways sway (a MotesView behind the transparent scroll
view). Adam picked this design ("Rising motes") from animated
renders on 26 Nov 2026; the shipped motion runs far slower
than the previews did, a full rise takes 7-13 seconds per
speck. The animation runs only while the home screen is
resumed, so it costs nothing in the background. The modules
themselves are unchanged.

### 1.10

Every module header now carries a Hub pill between the title
and the module icon (house glyph + "Hub"), in the module's own
surface and accent colours: one tap returns to the hub home,
clearing the activities above it, instead of swiping back
through the stack. The title keeps its exact place at the left
edge; the pill borrows the free space at the right of the
header row. The hub home itself has no pill (it is the
destination), and the Permissions screen carries one too, in
the hub shell's colours. Placement and colours were approved
from renders on 26 Nov 2026 before building.

### 1.9

One fix: the Mnemosyne module no longer opens onto a black
void away from home. Adam's 26 Nov field report from eduroam in
Brno: the module header rendered, the WebView area stayed pure
black, no page and no error. The box side was verified healthy
(the dashboard answers on its addresses and its certificate
covers all of them); the fault was the 1.8 load strategy, which
loaded the home-LAN name first and only fell back on a WebView
error. From a foreign network that name can hang instead of
failing fast, leaving the WebView on its black background
indefinitely, with no loading or error state at all. The
module now loads exactly one address, https://mnemosyne:3467:
the bare name is the box's Tailscale MagicDNS name, which
resolves from anywhere, on the home LAN and over the tailnet
alike (Adam verified it live from Brno; the candidate-probing
design drafted for this fix was dropped at his word in favour
of the single name). While a load is in flight, a "Connecting
to mnemosyne..." label covers the WebView area; a failed load
replaces it with an error state ("Mnemosyne is not reachable
on this network.") and a Retry button, and the header Reload
button loads the address again through the same states. An
SSL error now cancels the load and shows the error state with
a certificate note instead of silently cancelling into the
same black page. Everything else about the module and the hub
is unchanged from 1.8.

### 1.8

Two changes. A sixth module, **Mnemosyne**: a WebView window onto
the mnemosyne dashboard, sitting at the top of the home screen,
above Alarms (Chips stays last). The dashboard itself stays on
the box; the module only frames the live page, login cookie and
all. It loads the home-LAN name (mnemosyne.home:3467) first and
falls back once to the tailnet address (100.100.181.48:3467) on a
connection-level failure, which is the route that works away from
home while Tailscale is up. The app gains the INTERNET permission
and bundles the Mnemosyne Home CA next to the system certificates
(network security config, cleartext off), so the page loads
without certificate warnings. The module reuses the hub launcher
art as its icon for now; a dedicated icon proposal comes later.

And the VPN module's monitoring no longer dies silently on app
updates. Adam's 26 Nov field report: healthy evaluations at home
until 01:48, then total silence, not even a mobile-data
evaluation while travelling, until the app was opened by hand at
12:09. Diagnosis: a package replacement kills the service, and
nothing restarted it (no MY_PACKAGE_REPLACED handler anywhere;
the only restart paths were BOOT_COMPLETED and opening the
module). The service already returned START_STICKY, which does
not cover package replacement. Fixes, lifecycle only, evaluation
logic untouched: the VPN boot receiver also handles
MY_PACKAGE_REPLACED and restarts monitoring when it was enabled
(logging "App updated, monitoring restarted"), and a new watchdog
(an inexact repeating AlarmManager alarm, about every 15 minutes)
restarts the service whenever monitoring is enabled but the
service is not running, logging each revival in the module log.
The watchdog is armed from the service's onCreate, re-armed on
boot and on update, and cancels itself while monitoring is off.

### 1.6

Three fixes from Adam's first use of 1.5. The Permissions
screen's cards now run in the hub home's module order (Alarms,
Steps, Printer, VPN Switch, Chips) instead of the render's.
Alarms runs its band sync on open: entering the module queues the
same syncBandAlarms a change triggers (set-all plus time, and the
set-all session re-reads the battery level), guarded by the same
start checks the band session itself makes, so with no Bluetooth
permission, no adapter or Bluetooth off it skips silently and
nothing is ever requested on open; when a session stores a fresh
battery level the open screen's status row re-reads it at once.
Chips pre-fills a new game's four seats from the remembered
player names in order whenever history exists, instead of
leaving every slot on "Tap to set name"; the name dialog's
quick-picks and the on-entry remembering are unchanged, and the
names still live only in the module's private on-device
preferences.

### 1.5

Adam's eight-item list, approved item by item. The launcher icon
is the approved orb scene (Oguri tending the hub's glowing ball),
processed with the family's make_icons recipe (full art as the
legacy icon, the art at 68% of the adaptive canvas with feathered
edges, background re-sampled from the plate; reproducible with
`python3 icon-src/make_hub_icon.py`), and the same art sits in the
home header at the top right, where every module header carries
its icon. The modules drop their "Oguri's" prefix everywhere
(headers, home cards, notifications, the Steps widget label and
history share subject): they are Alarms, Steps, Chips, Printer and
VPN Switch now, inside Oguri's Hub. Permissions consolidate onto
one shared screen, reached by a small pill button under the home
cards: one card per module with every permission it needs, each
row in the settled Alarms model (name, always-visible live status,
a Grant or Open button faded to 0.45 once granted, never hidden).
The modules' own permission sections are gone, and so are the
open-time auto-requests (Alarms notifications/Bluetooth, Steps
activity, VPN notifications): the shared screen owns every grant.
Action-triggered requests stay (Printer still asks when Scan or a
connect needs the permissions). Every button in the app now comes
from the shared UiKit factories (standard / small / pill roles,
one radius, one padding rhythm, shared pressed and disabled
states); the only differences left are each module's colours and
the role. The home cards run Alarms, Steps, Printer, VPN Switch,
Chips (Chips last), and the VPN Switch card reads "Tailscale
switching based on network". Chips' name entry is a themed dialog
in its own warm brown and gold instead of the stock Android one,
and Chips now remembers entered player names across games in its
private on-device preferences, offered as quick-pick chips in
that dialog. No module logic changed: the VPN service and prefs,
the alarms scheduling and band stack, step counting, the chip
engine and the printer transports are untouched.

### 1.4

New module icons across all five modules. Each icon is an original
normal-Oguri scene matched to its module's theme, individually
approved: VPN holding up a glowing cyan key on midnight indigo,
Alarms jolted awake slapping a ringing twin-bell clock on deep
violet, Steps mid-stride with a glowing green footprint trail on
slate teal, Chips leaning in over the poker felt on warm brown,
Printer holding the little Orico thermal printer on warm
orange-brown. The plates carry a soft radial glow and no sparkles.
Icons are processed with the family's established recipe (the
Alarms/Steps make_icons pipeline): full art as the legacy icon,
the art at 68% of the adaptive canvas with alpha-feathered edges
as the foreground, and each adaptive background colour re-sampled
from its new plate. No code changed.

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
./build.sh   # produces OguriHub-1.6.apk
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
