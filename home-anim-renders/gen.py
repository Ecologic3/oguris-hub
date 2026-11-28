import pathlib, random
OUT = pathlib.Path('/home/hatch/workspace/hub-app/home-anim-renders')
ICONS = '/home/hatch/workspace/hub-app/icon-proposals'
random.seed(11)

MODULES = [
    ('Mnemosyne', 'Live mnemosyne dashboard', '#FF00AE', 'hub-proposal-3-small.png'),
    ('Alarms', 'Band alarms, backups and sync', '#8B5CF6', 'alarms-final-small.png'),
    ('Steps', 'Step counting, history and widget', '#3DDC97', 'steps-final-small.png'),
    ('Printer', 'Thermal printer over BLE and SPP', '#F5A83D', 'printer-final-small.png'),
    ('VPN Switch', 'Tailscale switching based on network', '#45D8FF', 'vpn-final-small.png'),
    ('Chips', 'Poker chip table and game engine', '#D9A441', 'chips-final-small.png'),
]

cards = ''
for name, desc, accent, icon in MODULES:
    cards += f"""
  <div class="card">
    <div class="bar" style="background:{accent}"></div>
    <img class="mic" src="file://{ICONS}/{icon}">
    <div class="texts"><div class="mname">{name}</div><div class="mdesc">{desc}</div></div>
  </div>"""

HOME = f"""
<div class="content">
  <div class="header"><div class="title">Oguri's Hub</div>
    <img class="hicon" src="file://{ICONS}/hub-proposal-3-small.png"></div>
  {cards}
  <div class="pillrow"><div class="pill">Permissions</div></div>
</div>"""

CSS = """
* { margin:0; padding:0; box-sizing:border-box; font-family: Roboto, 'Segoe UI', Arial, sans-serif; }
body { width:412px; height:915px; overflow:hidden; background:#0D2030; position:relative; }
.bg { position:absolute; inset:0; overflow:hidden; }
.content { position:absolute; inset:0; padding:24px 20px; }
.header { display:flex; align-items:center; }
.header .title { font-size:30px; font-weight:700; color:#FFD166; flex:1; }
.header img.hicon { width:48px; height:48px; border-radius:12px; }
.card { display:flex; align-items:center; background:#16324A; border:1px solid #264A66;
  border-radius:16px; padding:16px; margin-top:14px; }
.card .bar { width:6px; height:46px; border-radius:3px; flex:none; }
.card img.mic { width:46px; height:46px; border-radius:10px; margin-left:14px; }
.card .texts { margin-left:14px; }
.card .mname { color:#EBF2F7; font-size:19px; font-weight:700; }
.card .mdesc { color:#9EB6C7; font-size:14px; margin-top:2px; }
.pillrow { display:flex; justify-content:center; margin-top:18px; }
.pill { background:#16324A; border:1px solid #264A66; color:#9EB6C7; font-size:14px;
  height:40px; line-height:40px; padding:0 24px; border-radius:999px; }
"""

FREEZE = """
<script>
  const t = parseFloat(new URLSearchParams(location.search).get('t') || '0');
  window.addEventListener('load', () => {
    document.getAnimations().forEach(a => { a.pause(); a.currentTime = t; });
  });
</script>"""

def page(name, bg_html, extra_css):
    (OUT / f'{name}.html').write_text(
        f'<html><head><meta charset="utf-8"><style>{CSS}{extra_css}</style></head>'
        f'<body><div class="bg">{bg_html}</div>{HOME}{FREEZE}</body></html>')

# 1. Aurora drift: soft coloured glows wandering behind the cards
aurora_css = """
.blob { position:absolute; border-radius:50%; filter:blur(50px); }
.b1 { width:340px; height:340px; background:rgba(255,0,174,0.16); left:-90px; top:-70px;
  animation: drift1 1.2s ease-in-out infinite alternate; }
.b2 { width:380px; height:380px; background:rgba(69,216,255,0.13); right:-120px; top:300px;
  animation: drift2 1.2s ease-in-out infinite alternate; }
.b3 { width:300px; height:300px; background:rgba(139,92,246,0.15); left:40px; bottom:-110px;
  animation: drift1 1.2s ease-in-out infinite alternate-reverse; }
@keyframes drift1 { from { transform:translate(0,0) scale(1); } to { transform:translate(46px,30px) scale(1.13); } }
@keyframes drift2 { from { transform:translate(0,0) scale(1.1); } to { transform:translate(-40px,-26px) scale(0.98); } }
"""
page('aurora', '<div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>', aurora_css)

# 2. Rising motes: tiny accent-coloured specks floating up
accents = ['#FF00AE', '#8B5CF6', '#3DDC97', '#F5A83D', '#45D8FF', '#D9A441']
motes = ''
for i in range(24):
    x = random.uniform(2, 97); size = random.choice([2, 3, 3, 4])
    delay = -random.uniform(0, 2.4); col = accents[i % len(accents)]
    motes += (f'<div class="mote" style="left:{x:.1f}%;width:{size}px;height:{size}px;'
              f'background:{col};animation-delay:{delay:.2f}s"></div>')
motes_css = """
.mote { position:absolute; bottom:-10px; border-radius:50%; opacity:0;
  animation: rise 2.4s linear infinite; }
@keyframes rise {
  0% { transform:translate(0,0); opacity:0; }
  12% { opacity:.55; }
  55% { transform:translate(9px,-470px); opacity:.5; }
  88% { opacity:.45; }
  100% { transform:translate(-6px,-930px); opacity:0; } }
"""
page('motes', motes, motes_css)

# 3. Slow waves along the bottom
wave_svg = ('<svg width="824" height="200" viewBox="0 0 824 200" preserveAspectRatio="none">'
            '<path d="M0,80 C103,40 206,120 309,80 C412,40 515,120 618,80 C721,40 824,120 824,80 L824,200 L0,200 Z" '
            'fill="{fill}"/></svg>')
waves = (f'<div class="wave w1">{wave_svg.format(fill="rgba(255,0,174,0.10)")}</div>'
         f'<div class="wave w2">{wave_svg.format(fill="rgba(38,74,102,0.55)")}</div>'
         f'<div class="wave w3">{wave_svg.format(fill="rgba(22,50,74,0.85)")}</div>')
waves_css = """
.wave { position:absolute; left:0; width:824px; height:200px; }
.w1 { bottom:64px; animation: slide 2.4s linear infinite; }
.w2 { bottom:30px; animation: slide 2.4s linear infinite reverse; opacity:.9; }
.w3 { bottom:-16px; animation: slide 1.2s linear infinite; }
@keyframes slide { from { transform:translateX(0); } to { transform:translateX(-412px); } }
"""
page('waves', waves, waves_css)

# 4. Twinkle field: faint specks breathing in and out
stars = ''
for i in range(70):
    x = random.uniform(1, 99); y = random.uniform(1, 99)
    size = random.choice([1, 1, 2]); dur = random.choice([0.8, 1.2, 2.4])
    delay = -random.uniform(0, dur); col = random.choice(['#FFFFFF', '#FFFFFF', '#FFD166', '#FF9ADF', '#9FE8FF'])
    stars += (f'<div class="star" style="left:{x:.1f}%;top:{y:.1f}%;width:{size}px;height:{size}px;'
              f'background:{col};animation-duration:{dur}s;animation-delay:{delay:.2f}s"></div>')
stars_css = """
.star { position:absolute; border-radius:50%; opacity:.06; animation: tw ease-in-out infinite alternate; }
@keyframes tw { from { opacity:.05; } to { opacity:.6; } }
"""
page('twinkle', stars, stars_css)
print('written')
