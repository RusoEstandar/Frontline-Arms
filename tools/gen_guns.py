#!/usr/bin/env python3
"""Genera assets/frontline/guns/*.json y la paleta. Ejecutar desde la raiz del proyecto:  python3 tools/gen_guns.py
Ejes del modelo: +X canon, +Y arriba, +Z derecha. Unidades 1/16 de bloque. Todo el contenido es original."""
import json, os
from PIL import Image

A = 'src/main/resources/assets/frontline'
os.makedirs(f'{A}/guns', exist_ok=True)

# ---------- paleta (indices fijos; no reordenar) ----------
COL = [(0x2b,0x2b,0x2e),(0x55,0x57,0x5c),(0x8c,0x8f,0x96),(0x6b,0x4a,0x2b),(0x4a,0x6b,0x3a),
       (0x40,0x80,0xe0),(0xee,0xee,0xee),(0x7a,0x7d,0x84),(0xc9,0xa2,0x27),
       (0xff,0xe0,0x7a),(0xff,0x9a,0x2a),(0x1c,0x1c,0x1f),(0x3b,0x4a,0x35)]
DARK,MID,LIGHT,BROWN,GREEN,BLUE,WHITE,GREY,GOLD,FY,FO,GLOVE,SLEEVE = range(13)
pal = Image.new('RGBA', (16,16), (0,0,0,255))
for i,c in enumerate(COL):
    x, y = 2*(i%8), 2*(i//8)
    for dx in range(2):
        for dy in range(2): pal.putpixel((x+dx,y+dy), c+(255,))
pal.save(f'{A}/textures/item/palette.png')

# ---------- helpers de piezas ----------
def r_arm(gx0, gx1, gy0, gy1):
    """Mano derecha en la empunadura + antebrazo hacia atras."""
    return [(gx0-0.5, gy0+0.4, 6.7, gx1+0.7, gy1+0.3, 9.4, GLOVE),
            (gx0-12, gy0-3.0, 7.2, gx0-0.4, gy0+1.6, 10.2, SLEEVE)]

def l_arm(x0, x1, y0, y1, z0=6.2, z1=9.8):
    """Mano de apoyo + antebrazo hacia atras y a la izquierda."""
    return [(x0, y0, z0, x1, y1, z1, GLOVE),
            (x0-14, y0-3.2, z0-2.6, x0+0.6, y1-1.8, z0+0.4, SLEEVE)]

def flash(mx, my):
    return [(mx+0.1, my-0.9, 7.1, mx+2.3, my+0.9, 8.9, FY),
            (mx+0.1, my-0.15, 5.8, mx+1.5, my+0.15, 10.2, FO),
            (mx+0.1, my-2.2, 7.85, mx+1.5, my+2.2, 8.15, FO)]

def part(boxes, pivot=(0,0,0), lock=None):
    d = {"boxes": [list(b) for b in boxes]}
    if any(pivot): d["pivot"] = list(pivot)
    if lock: d["lock"] = list(lock)
    return d

# ---------- definiciones de armas ----------
GUNS = {
 'p9_pistol': dict(
    pivot=(4.2,5,8), sight=(3.5,10.9,8), ads_d=0.30, reload=40, shoot_len=4, kick=1.4, kp=6.0, travel=2.4,
    body=[(3,7,7.2,9,8,8.8,DARK),(3,3,7.2,5.5,7.5,8.8,DARK),(6,6.5,7.4,7,7,8.6,DARK),(12,8.5,7.5,13.5,9.5,8.5,DARK)],
    slide=[(3,8,7,12,10.5,9,MID),(3,10.5,7.7,4,11.2,8.3,LIGHT),(11,10.5,7.7,12,11.2,8.3,LIGHT)],
    mag=[(3.4,1.8,7.4,5.1,5.5,8.6,MID)], drop=8, muzzle=(13.5,9.0),
    rhand=(3,5.5,3,7.5), lhand=l_arm(3.2,6.4,3.4,7.4,5.0,7.3), hand_t=(1.0,-4.5,1.2), kind='mag'),
 'vx9_smg': dict(
    pivot=(3.9,5,8), sight=(2.8,10.9,8), ads_d=0.30, reload=45, shoot_len=3, kick=1.0, kp=3.5, travel=3.0,
    body=[(2,7,6.8,11,10,9.2,MID),(2,10,7.2,11,10.6,8.8,LIGHT),(11,8,7.6,15,9,8.4,DARK),(3,3.5,7.4,4.8,7,8.6,DARK),
          (-1,7.5,7.4,2,9,8.6,BROWN),(11,7.5,7.2,13,9.6,8.8,DARK),(2.4,10.6,7.7,3.2,11.2,8.3,LIGHT),(14,9,7.7,14.8,11.2,8.3,LIGHT)],
    slide=[(4.5,8.6,9.0,6.5,9.6,9.9,LIGHT)],
    mag=[(6,2.5,7.4,7.8,7,8.6,DARK)], drop=10, muzzle=(15,8.5),
    rhand=(3,4.8,3.5,7), lhand=l_arm(9,12.5,5.4,7.4), hand_t=(-3.9,-3.2,0), kind='mag'),
 'ar14_rifle': dict(
    pivot=(3.9,5,8), sight=(3.5,11.1,8), ads_d=0.30, reload=55, shoot_len=4, kick=1.6, kp=4.5, travel=3.0,
    body=[(-3,6,7.2,2,9.5,8.8,BROWN),(2,7,6.9,10,10,9.1,MID),(2,10,7.3,10,10.6,8.7,LIGHT),(10,7.5,7.1,15,9.8,8.9,DARK),
          (15,8.3,7.6,18,9.3,8.4,DARK),(3,3.5,7.4,4.8,7,8.6,DARK),(11,9.8,7.7,12,11.4,8.3,LIGHT),(3,10.6,7.7,4,11.4,8.3,LIGHT)],
    slide=[(3.5,8.8,9.1,5.5,9.8,9.9,LIGHT)],
    mag=[(6,2,7.4,8,7,8.6,DARK)], drop=10, muzzle=(18,8.8),
    rhand=(3,4.8,3.5,7), lhand=l_arm(10.5,14.5,5.8,8.0), hand_t=(-5.5,-4.5,0), kind='mag'),
 'm12_shotgun': dict(
    pivot=(4,5.5,8), sight=(3,10.1,8), ads_d=0.28, reload=70, shoot_len=18, kick=3.6, kp=9.0, travel=4.0,
    body=[(-4,5.5,7.1,2,9.5,8.9,BROWN),(2,7,7,8,9.8,9,MID),(8,8.6,7.2,19,9.7,8.8,DARK),(8,7.3,7.2,19,8.4,8.8,MID),
          (3,4,7.3,5,7,8.7,BROWN),(18,9.7,7.7,19,10.4,8.3,LIGHT),(2.5,9.8,7.8,3.5,10.4,8.2,LIGHT)],
    slide=[(11,6.6,6.9,15,8.4,9.1,BROWN)], mag=None, drop=0, muzzle=(19,9.1),
    rhand=(3,5,4,7), lhand=l_arm(10.6,15.4,5.8,8.6), hand_t=(-8,-3.0,0), kind='pump'),
 'kr50_sniper': dict(
    pivot=(4.3,5.8,8), sight=(3.6,10.8,8), ads_d=0.13, reload=65, shoot_len=25, kick=3.0, kp=7.0, travel=4.5,
    body=[(-4,5.5,7.2,2,9.5,8.8,BROWN),(2,7.2,7,9,9.8,9,MID),(9,8,7.6,22,9.2,8.4,DARK),(4,10.2,7.4,11,11.4,8.6,DARK),
          (3.6,10,7.2,4,11.6,8.8,LIGHT),(11,10,7.2,11.4,11.6,8.8,LIGHT),(5,9.8,7.6,6,10.2,8.4,MID),(9,9.8,7.6,10,10.2,8.4,MID),
          (22,7.8,7.4,23.4,9.4,8.6,DARK),(3.3,4.4,7.4,5.3,7.2,8.6,DARK)],
    slide=[(3.8,8.4,7.6,7.5,9.2,8.4,MID),(6.4,8.3,8.4,7.4,9.0,10.2,LIGHT),(6.4,8.2,10.0,7.4,9.6,10.9,DARK)],
    slide_pivot=(5.5,8.8,8), mag=[(5.5,5,7.5,7.5,7.4,8.5,DARK)], drop=8, muzzle=(23.4,8.6),
    rhand=(3.3,5.3,4.4,7.2), lhand=l_arm(9.5,13.5,6.0,8.0), hand_t=(-5.0,-3.0,0), kind='bolt'),
}

# ---------- animaciones ----------
def tr(pos=None, rot=None):
    d = {}
    if pos: d["pos"] = pos
    if rot: d["rot"] = rot
    return d

def anim(length, tracks): return {"length": length, "loop": False, "tracks": tracks}

def add(a, b): return [a[0]+b[0], a[1]+b[1], a[2]+b[2]]

def k(t, v): return [round(t,3)] + [round(x,3) for x in v]

def draw_anim():
    return anim(10, {
        "root": tr(pos=[k(0,(3,-10,5)), k(10,(0,0,0))], rot=[k(0,(-45,15,18)), k(10,(0,0,0))]),
        "hand_l": tr(pos=[k(0,(0,-3,0)), k(10,(0,0,0))])})

def shoot_anim(g):
    L, kick, kp = g['shoot_len'], g['kick'], g['kp']
    tail = 6 if g['kind'] in ('pump','bolt') else L
    tracks = {"root": tr(
        pos=[k(0,(0,0,0)), k(0.6,(0,0,kick)), k(tail,(0,0,0))] + ([k(L,(0,0,0))] if tail < L else []),
        rot=[k(0,(0,0,0)), k(0.6,(kp,0,kp*0.2)), k(tail,(0,0,0))] + ([k(L,(0,0,0))] if tail < L else []))}
    if g['kind'] == 'mag':
        tracks["slide"] = tr(pos=[k(0,(0,0,0)), k(0.5,(-g['travel'],0,0)), k(min(L,2.5),(0,0,0))])
    elif g['kind'] == 'pump':
        cyc = [k(0,(0,0,0)), k(6,(0,0,0)), k(9,(-g['travel'],0,0)), k(13,(-g['travel'],0,0)), k(16,(0,0,0)), k(L,(0,0,0))]
        tracks["slide"] = tr(pos=cyc); tracks["hand_l"] = tr(pos=cyc)
    elif g['kind'] == 'bolt':
        tracks["slide"] = tr(
            pos=[k(0,(0,0,0)), k(10,(0,0,0)), k(14,(-g['travel'],0,0)), k(16,(-g['travel'],0,0)), k(19,(0,0,0)), k(L,(0,0,0))],
            rot=[k(0,(0,0,0)), k(8,(0,0,0)), k(10,(-65,0,0)), k(19,(-65,0,0)), k(23,(0,0,0)), k(L,(0,0,0))])
    return anim(L, tracks)

def reload_anim(g, empty):
    L = g['reload']; ht = g['hand_t']; drop = g['drop']; kind = g['kind']
    root_pos = [k(0,(0,0,0)), k(0.12*L,(-1,-3,0.5)), k(0.8*L,(-1,-3,0.5)), k(L,(0,0,0))]
    root_rot = [k(0,(0,0,0)), k(0.12*L,(-8,0,14)), k(0.8*L,(-8,0,14)), k(L,(0,0,0))]
    tracks = {"root": tr(pos=root_pos, rot=root_rot)}
    if kind == 'pump':   # recarga cartucho a cartucho: la mano va y viene al puerto de carga
        fetch = add(ht, (1.5,-5,0)); z = (0,0,0)
        hl = [k(0,z), k(0.12*L,ht), k(0.2*L,fetch), k(0.27*L,ht), k(0.35*L,fetch), k(0.42*L,ht),
              k(0.5*L,fetch), k(0.57*L,ht), k(0.7*L,z), k(0.78*L,(-g['travel'],0,0)), k(0.86*L,(-g['travel'],0,0)),
              k(0.94*L,z), k(L,z)]
        tracks["hand_l"] = tr(pos=hl)
        tracks["slide"] = tr(pos=[k(0,z), k(0.7*L,z), k(0.78*L,(-g['travel'],0,0)), k(0.86*L,(-g['travel'],0,0)), k(0.94*L,z), k(L,z)])
        return anim(L, tracks)
    down = (0,-drop,0); out = (0,-drop-8,0)
    tracks["hand_l"] = tr(pos=[k(0,(0,0,0)), k(0.18*L,ht), k(0.22*L,ht), k(0.38*L,add(ht,down)), k(0.46*L,add(ht,out)),
                              k(0.5*L,add(ht,out)), k(0.7*L,ht), k(0.74*L,ht), k(0.86*L,(0,0,0)), k(L,(0,0,0))])
    tracks["mag"] = tr(pos=[k(0,(0,0,0)), k(0.22*L,(0,0,0)), k(0.38*L,down), k(0.46*L,out), k(0.5*L,out), k(0.7*L,(0,0,0)), k(L,(0,0,0))])
    if empty and kind == 'mag':   # suelta la corredera al final
        lk = (-g['travel'],0,0)
        tracks["slide"] = tr(pos=[k(0,lk), k(0.74*L,lk), k(0.8*L,(0,0,0)), k(L,(0,0,0))])
    if empty and kind == 'bolt':  # ciclo de cerrojo al final
        tracks["slide"] = tr(
            pos=[k(0,(0,0,0)), k(0.82*L,(0,0,0)), k(0.87*L,(-g['travel'],0,0)), k(0.91*L,(-g['travel'],0,0)), k(0.95*L,(0,0,0)), k(L,(0,0,0))],
            rot=[k(0,(0,0,0)), k(0.76*L,(0,0,0)), k(0.8*L,(-65,0,0)), k(0.95*L,(-65,0,0)), k(0.99*L,(0,0,0)), k(L,(0,0,0))])
    return anim(L, tracks)

def inspect_anim():
    return anim(60, {"root": tr(
        pos=[k(0,(0,0,0)), k(8,(-5,1,1)), k(28,(-5,1,1)), k(48,(-5,1,1)), k(60,(0,0,0))],
        rot=[k(0,(0,0,0)), k(8,(-6,-35,12)), k(28,(4,-55,-10)), k(48,(-3,-20,8)), k(60,(0,0,0))])})

# ---------- ensamblado ----------
SCALE = 0.45

def build_box_guns():
  for name, g in GUNS.items():
      s = 0.0625 * SCALE
      px, py, pz = g['pivot']; sx, sy, sz = g['sight']
      cam = ((sz-pz)*s, (sy-py)*s, -(sx-px)*s)                  # modelo -> camara (rotacion Y 90)
      ads = [-cam[0], -cam[1], -g['ads_d']-cam[2], 0, 0, 0]       # mira trasera centrada a ads_d metros del ojo
      hip = [0.28, -0.28, -0.50, 0, 3, 0]
      sprint = [0.22, -0.30, -0.35, -30, 28, -8]

      parts = {"body": part(g['body'])}
      parts["slide"] = part(g['slide'], pivot=g.get('slide_pivot', (0,0,0)), lock=(-g['travel'],0,0) if g['kind']=='mag' else None)
      if g['mag']: parts["mag"] = part(g['mag'])
      parts["hand_r"] = part(r_arm(*g['rhand']))
      parts["hand_l"] = part(g['lhand'])
      parts["flash"] = part(flash(*g['muzzle']))

      anims = {"draw": draw_anim(), "shoot": shoot_anim(g), "reload": reload_anim(g, False), "inspect": inspect_anim()}
      if g['kind'] in ('mag','bolt'): anims["reload_empty"] = reload_anim(g, True)

      out = {"pivot": list(g['pivot']), "scale": SCALE,
             "hip": hip, "ads": [round(v,4) for v in ads], "sprint": sprint,
             "parts": parts, "anims": anims}
      json.dump(out, open(f'{A}/guns/{name}.json','w'), separators=(',',':'))
      print(name, 'ads=', [round(v,3) for v in ads[:3]], 'anims=', list(anims))


if __name__ == '__main__':
    build_box_guns()
