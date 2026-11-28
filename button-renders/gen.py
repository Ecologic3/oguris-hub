import pathlib
OUT = pathlib.Path('/home/hatch/workspace/hub-app/button-renders')
ICONS = '/home/hatch/workspace/hub-app/icon-proposals'

HOUSE = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="{c}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v9h13v-9"/></svg>'

CSS = """
* { margin:0; padding:0; box-sizing:border-box; font-family: Roboto, 'Segoe UI', Arial, sans-serif; }
body { width:412px; height:915px; overflow:hidden; }
.screen { width:412px; height:915px; padding:24px 20px; }
.header { display:flex; align-items:center; gap:10px; }
.header .title { font-size:30px; font-weight:700; flex:1; white-space:nowrap; }
.header img.icon { width:48px; height:48px; border-radius:12px; }
.hubbtn { display:flex; align-items:center; gap:5px; height:34px; padding:0 12px 0 10px;
  border-radius:999px; font-size:14px; font-weight:600; flex:none; }
.card { border-radius:16px; padding:16px 18px; margin-top:14px; }
"""

def header(title, text_color, icon, pill_fill, pill_stroke, pill_color):
    stroke = f'border:1px solid {pill_stroke};' if pill_stroke else ''
    return (f'<div class="header">'
            f'<div class="title" style="color:{text_color}">{title}</div>'
            f'<div class="hubbtn" style="background:{pill_fill};{stroke}color:{pill_color}">'
            f'{HOUSE.format(c=pill_color)}<span>Hub</span></div>'
            f'<img class="icon" src="file://{ICONS}/{icon}"></div>')

def page(name, bg, inner):
    (OUT / f'{name}.html').write_text(
        f'<html><head><meta charset="utf-8"><style>{CSS}</style></head>'
        f'<body><div class="screen" style="background:{bg}">{inner}</div></body></html>')

# ---------- Alarms ----------
alarms = header('Alarms', '#F5F3FF', 'alarms-final-small.png',
                'rgba(255,255,255,0.10)', 'rgba(167,139,250,0.45)', '#A78BFA')
alarms += """
<div class="card" style="background:rgba(21,10,46,0.55);border:1px solid rgba(167,139,250,0.25)">
  <div style="color:#A78BFA;font-size:16px;font-weight:600">Honor Band 4 &middot; Battery 60%</div>
  <div style="color:#C4B5FD;font-size:13px;margin-top:4px">Band connected &middot; alarms in sync</div>
</div>"""
for time, day in [('06:50', 'Tuesday'), ('08:50', 'Wednesday')]:
    alarms += f"""
<div class="card" style="background:rgba(21,10,46,0.55);border:1px solid rgba(167,139,250,0.25);display:flex;align-items:center">
  <div style="flex:1"><div style="color:#F5F3FF;font-size:30px;font-weight:700">{time}</div>
  <div style="color:#C4B5FD;font-size:13px;margin-top:2px">{day} &middot; Band alarm set</div></div>
  <div style="width:52px;height:30px;border-radius:999px;background:#8B5CF6;position:relative;flex:none">
    <div style="width:24px;height:24px;border-radius:50%;background:#fff;position:absolute;right:3px;top:3px"></div></div>
</div>"""
alarms += """
<div style="margin-top:16px;background:#8B5CF6;color:#fff;font-size:15px;text-align:center;
  padding:12px;border-radius:12px">Add alarm</div>"""
page('alarms', 'linear-gradient(160deg,#150A2E 0%,#2A1065 55%,#4C1D95 100%)', alarms)

# ---------- Steps ----------
steps = header('Steps', '#FFFFFF', 'steps-final-small.png', '#2A3347', None, '#3DDC97')
bars = ''.join(f'<div style="flex:1;background:{"#3DDC97" if i == 9 else "#2A6E50"};'
               f'height:{18 + (i * 37) % 64}px;border-radius:3px 3px 0 0"></div>' for i in range(12))
steps += f"""
<div style="margin-top:26px;color:#FFFFFF;font-size:56px;font-weight:700">8,432</div>
<div style="color:#9FB0CC;font-size:14px">steps today &middot; goal 10,000</div>
<div style="margin-top:12px;height:8px;border-radius:4px;background:#2A3347">
  <div style="width:84%;height:8px;border-radius:4px;background:#3DDC97"></div></div>
<div class="card" style="background:#12283D">
  <div style="display:flex;align-items:flex-end;gap:5px;height:90px">{bars}</div>
  <div style="color:#9FB0CC;font-size:12px;margin-top:8px">Last 12 hours</div>
</div>
<div style="display:flex;gap:8px;margin-top:14px">
  <div style="background:#3DDC97;color:#08130C;font-size:13px;font-weight:600;padding:7px 14px;border-radius:999px">Day</div>
  <div style="background:#2A3347;color:#FFFFFF;font-size:13px;padding:7px 14px;border-radius:999px">Week</div>
  <div style="background:#2A3347;color:#FFFFFF;font-size:13px;padding:7px 14px;border-radius:999px">Month</div>
</div>"""
page('steps', '#0D2030', steps)

# ---------- Chips ----------
chips = header('Chips', '#FFF7ED', 'chips-final-small.png', '#332619', '#3A2C20', '#D9A441')
rows = ''
for i, nm in [(1, 'Adam'), (2, 'Kuba'), (3, 'Filip'), (4, 'Bor')]:
    rows += f"""
  <div style="display:flex;align-items:center;gap:10px;margin-top:12px">
    <div style="width:34px;height:34px;border-radius:50%;background:#2FA866;color:#08130C;
      font-weight:700;display:flex;align-items:center;justify-content:center">{nm[0]}</div>
    <div style="flex:1;color:#FFF7ED;font-size:15px">{nm}</div>
    <div style="background:#332619;color:#D9A441;font-size:13px;padding:6px 10px;border-radius:10px">Seat {i} &#9662;</div>
  </div>"""
chips += f"""
<div class="card" style="background:#241B16;border:1px solid #3A2C20">
  <div style="color:#FFF7ED;font-size:18px;font-weight:700">New session</div>{rows}
  <div style="color:#C79A3F;font-size:12px;font-weight:700;margin-top:16px">Ante per player / hand</div>
  <div style="background:#1A130E;border:1px solid #3A2C20;border-radius:10px;color:#FFF7ED;
    padding:9px 12px;margin-top:6px;font-size:15px">10</div>
</div>
<div style="margin-top:14px;background:#D9A441;color:#231A02;font-size:16px;font-weight:600;
  text-align:center;padding:13px;border-radius:12px">Start session</div>"""
page('chips', '#14100E', chips)

# ---------- Printer ----------
printer = header('Printer', '#FFF7ED', 'printer-final-small.png', '#3A3129', None, '#F5A83D')
printer += """
<div style="color:#F5A83D;font-size:15px;margin-top:18px">No printer connected</div>
<div class="card" style="background:#221C17">
  <div style="color:#FFF7ED;font-size:18px;font-weight:700">Text</div>
  <div style="background:#171310;border-radius:10px;height:120px;margin-top:12px;padding:12px;
    color:#8A7B6B;font-size:15px">Type something to print&hellip;</div>
</div>
<div style="display:flex;gap:10px;margin-top:14px">
  <div style="flex:1;background:#3A3129;color:#FFF7ED;font-size:15px;text-align:center;padding:12px;border-radius:12px">Scan / connect</div>
  <div style="flex:1;background:#F5A83D;color:#231A02;font-size:15px;font-weight:600;text-align:center;padding:12px;border-radius:12px">Print</div>
</div>"""
page('printer', '#171310', printer)

# ---------- VPN ----------
vpn = header('VPN Switch', '#EDF2FA', 'vpn-final-small.png', '#232F4D', '#2C3B5F', '#45D8FF')
vpn += """
<div class="card" style="background:#1A2338;border:1px solid #2C3B5F">
  <div style="color:#EDF2FA;font-size:18px;font-weight:700">Monitoring on</div>
  <div style="color:#9FB0CC;font-size:14px;margin-top:4px">Current network: eduroam</div>
  <div style="color:#45D8FF;font-size:14px;margin-top:4px">Tailscale connected by the hub</div>
</div>
<div class="card" style="background:#1A2338;border:1px solid #2C3B5F">
  <div style="color:#EDF2FA;font-size:18px;font-weight:700">Home networks</div>
  <div style="display:flex;margin-top:12px"><div style="flex:1;color:#EDF2FA;font-size:15px">TP-Link_7841_5GHz</div><div style="color:#FF8A8A;font-size:15px">&#10005;</div></div>
  <div style="display:flex;margin-top:10px"><div style="flex:1;color:#EDF2FA;font-size:15px">AdamHome_5G</div><div style="color:#FF8A8A;font-size:15px">&#10005;</div></div>
</div>
<div style="margin-top:14px;background:#45D8FF;color:#08222E;font-size:15px;font-weight:600;
  text-align:center;padding:12px;border-radius:12px">Add current network</div>"""
page('vpn', '#0D2030', vpn)

# ---------- Mnemosyne ----------
mne = header('Mnemosyne', '#EBF2F7', 'hub-proposal-3-small.png', '#16324A', '#264A66', '#FF00AE')
spark = '0,34 30,30 60,33 90,22 120,28 150,12 180,26 210,18 240,30 270,8 300,20 330,15'
mne += f"""
<div style="margin-top:12px"><span style="background:#16324A;color:#FF00AE;font-size:13px;
  padding:7px 14px;border-radius:10px">Reload</span></div>
<div style="margin-top:14px;border:1px solid #264A66;border-radius:16px;background:#0A1826;padding:14px">
  <div style="display:flex;gap:10px">
    <div style="flex:1;background:#10222F;border-radius:12px;padding:12px">
      <div style="color:#8FA6B8;font-size:12px">CPU</div>
      <div style="color:#EBF2F7;font-size:22px;font-weight:700;margin-top:4px">12%</div></div>
    <div style="flex:1;background:#10222F;border-radius:12px;padding:12px">
      <div style="color:#8FA6B8;font-size:12px">Memory</div>
      <div style="color:#EBF2F7;font-size:22px;font-weight:700;margin-top:4px">2.3 / 14 G</div></div>
  </div>
  <div style="background:#10222F;border-radius:12px;padding:12px;margin-top:10px">
    <div style="color:#8FA6B8;font-size:12px">Power</div>
    <svg width="330" height="42" style="margin-top:6px"><polyline points="{spark}" fill="none" stroke="#FF00AE" stroke-width="2"/></svg>
    <div style="color:#EBF2F7;font-size:15px">6.1 W</div>
  </div>
  <div style="color:#5F7194;font-size:12px;margin-top:10px">Live dashboard, shown inside the hub</div>
</div>"""
page('mnemosyne', '#0D2030', mne)
print('written')
