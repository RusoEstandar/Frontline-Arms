#!/usr/bin/env python3
"""Convierte los modelos GLB de tools/source_models en assets/frontline/guns/*.json + texturas.
Ejecutar desde la raiz del proyecto:  python3 tools/gen_models.py   (requiere numpy y pillow)
Ejes finales: +X canon, +Y arriba, +Z derecha; unidades de 1/16 de bloque; origen = empunadura."""
import os, sys, io, json
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE); os.chdir(os.path.dirname(HERE))
import numpy as np
from PIL import Image
import glb
import gen_guns as G

A = G.A; SRC = 'tools/source_models'; TEX = f'{A}/textures/gun'
os.makedirs(TEX, exist_ok=True); os.makedirs(f'{A}/guns', exist_ok=True)
SCALE = 0.45

R_ID = np.eye(3)
R_NZ = np.array([[0,0,-1],[0,1,0],[1,0,0]], float)   # canon hacia -Z  ->  X' = -z, Y' = y, Z' = x

# ---------- clasificacion de piezas (coordenadas originales del GLB) ----------
def part_rifle(p, mn, mx, n):
    cx = (mn[0] + mx[0]) / 2
    if -0.50 < cx < -0.39 and mx[1] < 0.0 and n > 150: return 'mag'      # cargador
    if -0.575 < cx < -0.54 and mn[1] > 0.05: return 'slide'              # palanca de montar
    return 'body'

def part_mp7(p, mn, mx, n):
    if 'Magazine_419' in p['path']: return 'mag'
    if 'lever_312' in p['path']: return 'slide'
    return 'body'

def part_pistol(p, mn, mx, n):
    if mn[1] >= 8.9: return 'slide'
    if mx[1] <= 3.85: return 'mag'
    return 'body'

def components(p):
    q = p['pos']; keys = np.round(q * 1e4).astype(np.int64)
    uniq = {}; vid = np.array([uniq.setdefault(tuple(k), len(uniq)) for k in keys])
    parent = list(range(len(uniq)))
    def f(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]; a = parent[a]
        return a
    tri = p['idx'].reshape(-1, 3)
    for t in tri:
        a = f(vid[t[0]]); parent[f(vid[t[1]])] = a; parent[f(vid[t[2]])] = a
    return np.array([f(vid[t[0]]) for t in tri])

def classify(p, part_fn, by_comp):
    tri = p['idx'].reshape(-1, 3); q = p['pos']
    if not by_comp:
        v = q[np.unique(tri)]; return [part_fn(p, v.min(0), v.max(0), len(tri))] * len(tri)
    roots = components(p); out = [''] * len(tri); info = {}
    for r in np.unique(roots):
        m = roots == r; v = q[np.unique(tri[m])]
        info[r] = part_fn(p, v.min(0), v.max(0), int(m.sum()))
    return [info[r] for r in roots]

def lift_dark(a):   # las texturas del MP7 son casi negras: aclarado leve para que se distinga el detalle
    rgb = a[..., :3].astype(float) / 255
    out = a.copy(); out[..., :3] = np.clip(rgb ** 0.55 * 255 + 8, 0, 255).astype(np.uint8); return out

# ---------- conversion ----------
def convert(name, src, R, length, pivot, part_fn, by_comp, flat=None, lift=None):
    js, b = glb.load(f'{SRC}/{src}'); prims = glb.primitives(js, b)
    for p in prims: p['tri_part'] = classify(p, part_fn, by_comp)
    for p in prims:
        p['pos'] = p['pos'] @ R.T
        if p['nrm'] is not None: p['nrm'] = p['nrm'] @ R.T
    allp = np.vstack([p['pos'] for p in prims]); s = length / (allp[:, 0].max() - allp[:, 0].min())
    piv = np.array(pivot, float)
    for p in prims: p['pos'] = (p['pos'] - piv) * s

    texmap = {}
    def tex_of(mi):
        if mi in texmap: return texmap[mi]
        col, ii = glb.material_info(js, mi); fn = f'{name}_{len(texmap)}.png'
        if ii is not None:
            a = np.array(Image.open(io.BytesIO(glb.image_bytes(js, b, ii))).convert('RGBA'))
            if a[..., 3].max() == 0: texmap[mi] = None; return None      # capa totalmente transparente
            if lift: a = lift(a)
            Image.fromarray(a).save(f'{TEX}/{fn}'); texmap[mi] = (f'frontline:textures/gun/{fn}', False, fn)
        else:
            Image.new('RGBA', (2, 2), tuple(flat[mi]) + (255,)).save(f'{TEX}/{fn}'); texmap[mi] = (f'frontline:textures/gun/{fn}', True, fn)
        return texmap[mi]

    parts = {}; bbox = {}
    for p in prims:
        t = tex_of(p['material'])
        if t is None: continue
        loc, is_flat, _ = t; tri = p['idx'].reshape(-1, 3); tp = np.array(p['tri_part'])
        for part in sorted(set(tp)):
            sel = tri[tp == part]; ids = np.unique(sel); remap = {int(v): k for k, v in enumerate(ids)}
            dst = parts.setdefault(part, {}).setdefault(loc, {'v': [], 'i': []}); base = len(dst['v']) // 8
            for v in ids:
                x, y, z = p['pos'][v]; u, w = (0.5, 0.5) if is_flat else (p['uv'][v][0], p['uv'][v][1])
                nx, ny, nz = p['nrm'][v] if p['nrm'] is not None else (0, 1, 0)
                dst['v'] += [round(float(x), 3), round(float(y), 3), round(float(z), 3), round(float(u), 4), round(float(w), 4),
                             round(float(nx), 2), round(float(ny), 2), round(float(nz), 2)]
            dst['i'] += [base + remap[int(v)] for v in sel.reshape(-1)]
            pts = p['pos'][ids]; bb = bbox.setdefault(part, [pts.min(0), pts.max(0)])
            bb[0] = np.minimum(bb[0], pts.min(0)); bb[1] = np.maximum(bb[1], pts.max(0))
    return parts, bbox, s, piv

# ---------- brazos / fogonazo con z centrada en 0 ----------
def arm_r(x0, x1, y0, y1):
    return [(x0-0.5, y0+0.4, -1.3, x1+0.7, y1+0.3, 1.4, G.GLOVE), (x0-12, y0-3.0, -0.8, x0-0.4, y0+1.6, 2.2, G.SLEEVE)]
def arm_l(x0, x1, y0, y1, z0, z1):
    """Guante + muneca vertical + antebrazo bajo: queda por delante y por debajo del cargador."""
    return [(x0, y0, z0, x1, y1, z1, G.GLOVE),
            (x0+0.5, y0-5.0, z0-1.4, x0+3.0, y0+0.4, z0+0.4, G.SLEEVE),
            (x0-12, y0-9.4, z0-4.6, x0+3.0, y0-4.6, z0-0.8, G.SLEEVE)]
def flash_c(mx, my):
    k = 0.7
    return [(mx+0.1, my-0.9*k, -0.9*k, mx+2.3*k, my+0.9*k, 0.9*k, G.FY), (mx+0.1, my-0.15, -2.2*k, mx+1.5*k, my+0.15, 2.2*k, G.FO),
            (mx+0.1, my-2.2*k, -0.15, mx+1.5*k, my+2.2*k, 0.15, G.FO)]

# ---------- especificaciones ----------
SPECS = {
 'ar14_rifle': dict(src='low_poly_colt_m4a1.glb', R=R_ID, length=24, pivot=(-0.565, -0.075, 0.0), part_fn=part_rifle, by_comp=True,
    flat={0: (46,48,52), 1: (35,36,39), 2: (75,78,84), 3: (60,62,66)},
    sight_o=(-0.43, 0.062, 0.0), muzzle_o=(0.0, 0.0, 0.0), ads_d=0.30,
    rhand=(-1.8, 0.8, -2.1, 0.8), lhand=(5.8, 9.8, -1.4, 1.6, -1.9, 1.9), hand_t=(-4.1, -4.6, 0),
    g=dict(shoot_len=4, kick=1.6, kp=4.5, travel=1.2, reload=55, drop=6, kind='mag')),
 'vx9_smg': dict(src='minecraft_-_hl2_mp7.glb', R=R_NZ, length=19, pivot=(-0.055, 0.11, -0.017), part_fn=part_mp7, by_comp=False,
    lift=lift_dark, sight_o=(-0.098, 0.29, -0.017), muzzle_o=(0.334, 0.196, -0.017), ads_d=0.30,
    rhand=(-1.2, 1.1, -3.0, 1.2), lhand=(6.3, 9.5, -3.0, 0.2, -1.5, 1.5), hand_t=(-8, -3.7, 0),
    g=dict(shoot_len=3, kick=1.0, kp=3.5, travel=1.5, reload=45, drop=7, kind='mag')),
 'p9_pistol': dict(src='minecraft_pistol.glb', R=R_NZ, length=12, pivot=(-16.0, 6.0, 8.0), part_fn=part_pistol, by_comp=False,
    sight_o=(-16.3, 15.0, 8.0), muzzle_o=(5.0, 10.5, 8.0), ads_d=0.28,
    rhand=(-1.8, 1.8, -2.0, 1.2), lhand=(-1.8, 1.6, -2.0, 1.0, -2.3, -0.8), hand_t=(0, -2.7, 1.5),
    mag_box=[(-1.2, -2.4, -0.6, 1.2, 1.4, 0.6, G.MID)], lock_travel=2.0,
    g=dict(shoot_len=4, kick=1.4, kp=6.0, travel=2.0, reload=40, drop=6, kind='mag')),
}

def build(name, sp):
    meshes, bbox, s, piv = convert(name, sp['src'], sp['R'], sp['length'], sp['pivot'], sp['part_fn'], sp['by_comp'], sp.get('flat'), sp.get('lift'))
    fin = lambda o: (np.array(o, float) - piv) * s
    sight = fin(sp['sight_o']); muzzle = fin(sp['muzzle_o'])
    parts = {}
    for pn in ('body', 'slide', 'mag'):
        if pn in meshes:
            d = {'meshes': [{'tex': loc, 'v': m['v'], 'i': m['i']} for loc, m in meshes[pn].items()]}
            if pn == 'slide' and sp.get('lock_travel'): d['lock'] = [-sp['lock_travel'], 0, 0]
            if pn == 'mag' and sp.get('mag_box'): d['boxes'] = [list(bx) for bx in sp['mag_box']]
            parts[pn] = d
    parts['hand_r'] = {'boxes': [list(bx) for bx in arm_r(*sp['rhand'])]}
    parts['hand_l'] = {'boxes': [list(bx) for bx in arm_l(*sp['lhand'])]}
    parts['flash'] = {'boxes': [list(bx) for bx in flash_c(float(muzzle[0]), float(muzzle[1]))]}

    g = dict(sp['g']); g['hand_t'] = sp['hand_t']
    sc = 0.0625 * SCALE
    ads = [round(float(-sight[2] * sc), 4), round(float(-sight[1] * sc), 4), round(float(-sp['ads_d'] + sight[0] * sc), 4), 0, 0, 0]
    out = {'pivot': [0, 0, 0], 'scale': SCALE, 'hip': [0.28, -0.28, -0.50, 0, 3, 0], 'ads': ads, 'sprint': [0.22, -0.30, -0.35, -30, 28, -8],
           'parts': parts, 'anims': {'draw': G.draw_anim(), 'shoot': G.shoot_anim(g), 'reload': G.reload_anim(g, False),
                                    'reload_empty': G.reload_anim(g, True), 'inspect': G.inspect_anim()}}
    json.dump(out, open(f'{A}/guns/{name}.json', 'w'), separators=(',', ':'))
    kb = os.path.getsize(f'{A}/guns/{name}.json') / 1024
    print(f'{name}: {kb:.0f} KB, escala {s:.2f}, ads={ads[:3]}, muzzle={np.round(muzzle,1)}, sight={np.round(sight,1)}')
    for pn, (mn, mx) in bbox.items(): print(f'   {pn:5s} x {mn[0]:6.1f}..{mx[0]:5.1f}  y {mn[1]:6.1f}..{mx[1]:5.1f}  z {mn[2]:5.1f}..{mx[2]:5.1f}')
    return out

def preview(name, out, path):
    from PIL import ImageDraw
    items = []
    texcache = {}
    def sample(loc, u, v):
        fn = loc.split('/')[-1]
        if fn not in texcache: texcache[fn] = Image.open(f'{TEX}/{fn}').convert('RGBA')
        im = texcache[fn]; return im.getpixel((int(min(max(u, 0), 1) * (im.width - 1)), int(min(max(v, 0), 1) * (im.height - 1))))
    for pn, part in out['parts'].items():
        for m in part.get('meshes', []):
            v = np.array(m['v']).reshape(-1, 8); ix = np.array(m['i']).reshape(-1, 3)
            for t in ix:
                pts = v[t]; c = sample(m['tex'], pts[:, 3].mean(), pts[:, 4].mean())
                if c[3] < 40: continue
                items.append((-pts[:, 2].mean(), pts[:, :2], c[:3]))
        for bx in part.get('boxes', []):
            x0, y0, z0, x1, y1, z1, ci = bx; col = G.COL[int(ci)]
            items.append((-(z0 + z1) / 2, np.array([[x0, y0], [x1, y0], [x1, y1], [x0, y1]]), col))
    items.sort(key=lambda t: t[0])
    S = 26; ox, oy = 16 * S, 14 * S; im = Image.new('RGB', (36 * S, 24 * S), (70, 74, 80)); d = ImageDraw.Draw(im)
    for _, xy, c in items: d.polygon([(ox + x * S, oy - y * S) for x, y in xy], fill=tuple(int(k) for k in c))
    d.line([(ox - 8, oy), (ox + 8, oy)], fill=(255, 0, 0)); d.line([(ox, oy - 8), (ox, oy + 8)], fill=(255, 0, 0))
    d.text((6, 4), name + '  (cruz roja = empunadura)', fill=(255, 255, 255)); im.save(path)

if __name__ == '__main__':
    for n, sp in SPECS.items():
        o = build(n, sp); preview(n, o, f'/tmp/prev_{n}.png')
