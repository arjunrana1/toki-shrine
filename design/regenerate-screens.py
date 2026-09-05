import re, os, pathlib, shutil
PROJ = pathlib.Path(os.path.expanduser("~/Claude Projects/Anti Doomscrolling/toki-shrine-ui-mockups/project"))
html = (PROJ/"TimeShrine Mocks.dc.html").read_text(encoding="utf-8")
style = re.search(r"<style>(.*?)</style>", html, re.S).group(1)
OUT = PROJ/"_render"
shutil.rmtree(OUT, ignore_errors=True); OUT.mkdir()

SCREENS=[("01",1,"welcome"),("02",2,"permission-checklist"),("03",3,"accessibility-explainer"),
("04",4,"battery-settings"),("05",5,"block-list-empty"),("06",6,"block-list-populated"),
("07",7,"create-add-apps-sites"),("07b",8,"create-app-search"),("08",9,"create-name-it"),
("09",10,"create-configure-friction"),("10",11,"create-review-save"),("11",12,"conflict-dialog"),
("12",13,"turn-on-confirmation"),("13",14,"block-detail-edit"),("14",15,"block-screen"),
("15",16,"walk-away-moment"),("16",17,"typing-challenge"),("16b",18,"typing-mistyped"),
("17",19,"delay-countdown"),("18",20,"floating-bubble"),("19",21,"ongoing-notification"),
("20",22,"turn-off-typing"),("21",23,"stats"),("22",24,"settings"),("23",25,"feedback")]
VARIANTS={"07":["apps","sites"],"09":["type","wait"]}

TPL="""<!doctype html><html><head><meta charset="utf-8">
<link rel="stylesheet" href="../_ds/nocturne-82412343-c17a-4e25-85de-f680ed74498c/styles.css">
<link rel="stylesheet" href="https://unpkg.com/@phosphor-icons/web@2.1.1/src/regular/style.css">
<link rel="stylesheet" href="https://unpkg.com/@phosphor-icons/web@2.1.1/src/fill/style.css">
<link rel="stylesheet" href="https://unpkg.com/@phosphor-icons/web@2.1.1/src/bold/style.css">
<style>%s</style>
<style>html,body{margin:0;padding:0;background:#0d0f1a;width:400px;height:800px;overflow:hidden}
body{display:grid;place-items:center}</style>
</head><body><div class="screen"><div class="inner">%s</div></div></body></html>"""

def span(s, start):
    d=1; j=start
    while d>0:
        o=s.find("<sc-if",j); c=s.find("</sc-if>",j)
        if c==-1: return len(s), len(s)
        if o!=-1 and o<c: d+=1; j=o+6
        else: d-=1; j=c+8
    return j-8, j

def clean(b):
    b=re.sub(r'</?sc-if[^>]*>','',b)
    return re.sub(r'\s(onClick|data-go|go)="[^"]*"','',b)

made=0
for sid,no,slug in SCREENS:
    m=re.search(r'<sc-if value="\{\{ vis\.s%s \}\}"[^>]*>'%re.escape(sid), html)
    if not m: continue
    e,_=span(html,m.end()); body=html[m.end():e]
    blocks=[]; pos=0
    while True:
        o=body.find("<sc-if",pos)
        if o==-1: break
        oe=body.index(">",o)+1; ce,fe=span(body,oe)
        blocks.append((o,oe,ce,fe)); pos=fe
    if sid in VARIANTS and len(blocks)==2:
        for k,vn in enumerate(VARIANTS[sid]):
            parts=[]; last=0
            for idx,(o,oe,ce,fe) in enumerate(blocks):
                parts.append(body[last:o])
                if idx==k: parts.append(body[oe:ce])
                last=fe
            parts.append(body[last:])
            (OUT/f"{no:02d}-{slug}-{vn}.html").write_text(TPL%(style,clean("".join(parts))),encoding="utf-8"); made+=1
    else:
        (OUT/f"{no:02d}-{slug}.html").write_text(TPL%(style,clean(body)),encoding="utf-8"); made+=1
print("generated",made)
