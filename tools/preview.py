import sys, io; sys.path.insert(0, 'tools')
import glb, numpy as np
from PIL import Image, ImageDraw

def tri_colors(js, binc, P, group_fn=None):
    imgs = {}
    def img(i):
        if i not in imgs: imgs[i] = Image.open(io.BytesIO(glb.image_bytes(js, binc, i))).convert('RGB')
        return imgs[i]
    out = []
    pal = [(230,80,80),(80,200,90),(80,120,230),(230,200,60),(200,80,220),(70,210,210),(240,150,60),(150,150,150),(180,110,70),(120,230,150)]
    for p in P:
        col, ii = glb.material_info(js, p['material'])
        tris = p['idx'].reshape(-1, 3)
        cs = []
        for t in tris:
            if group_fn is not None:
                c = pal[group_fn(p, t) % len(pal)]
            elif ii is not None and p['uv'] is not None:
                u, v = p['uv'][t].mean(0); im = img(ii)
                c = im.getpixel((int(u % 1 * (im.width-1)), int(v % 1 * (im.height-1))))
            else:
                g = lambda x: int(255 * min(1, max(0, x)) ** (1/2.2))
                c = tuple(max(55, min(255, g(x) + 70)) for x in col[:3])   # oscuro pero visible
            cs.append(c)
        out.append(cs)
    return out

def render(P, cols, ax_h, ax_v, flip_h=False, size=900, depth_ax=None, path='/tmp/p.png', title=''):
    allp = np.vstack([p['pos'] for p in P])
    hs = allp[:, ax_h]; vs = allp[:, ax_v]
    sc = (size - 40) / max(hs.max() - hs.min(), vs.max() - vs.min())
    W = int((hs.max() - hs.min()) * sc) + 40; H = int((vs.max() - vs.min()) * sc) + 40
    im = Image.new('RGB', (W, H), (70, 74, 80)); d = ImageDraw.Draw(im)
    items = []
    for p, cs in zip(P, cols):
        tris = p['idx'].reshape(-1, 3)
        for t, c in zip(tris, cs):
            pts = p['pos'][t]
            depth = pts[:, depth_ax].mean() if depth_ax is not None else 0
            items.append((depth, pts, c))
    items.sort(key=lambda x: x[0])
    for _, pts, c in items:
        xy = []
        for q in pts:
            x = (q[ax_h] - hs.min()) * sc + 20
            if flip_h: x = W - x
            y = H - ((q[ax_v] - vs.min()) * sc + 20)
            xy.append((x, y))
        d.polygon(xy, fill=c)
    d.text((6, 4), title, fill=(255, 255, 255))
    im.save(path); return im.size

if __name__ == '__main__':
    U = '/mnt/user-data/uploads/'
    # rifle: eje largo X, alto Y
    js, b = glb.load(U + 'low_poly_colt_m4a1.glb'); P = glb.primitives(js, b)
    render(P, tri_colors(js, b, P), 0, 1, depth_ax=2, path='/tmp/rifle_side.png', title='rifle: horizontal=X vertical=Y')
    # mp7: eje largo Z, alto Y
    js, b = glb.load(U + 'minecraft_-_hl2_mp7.glb'); P = glb.primitives(js, b)
    render(P, tri_colors(js, b, P), 2, 1, depth_ax=0, path='/tmp/mp7_side.png', title='mp7: horizontal=Z vertical=Y')
    # pistola: eje largo Z, alto Y
    js, b = glb.load(U + 'minecraft_pistol.glb'); P = glb.primitives(js, b)
    render(P, tri_colors(js, b, P), 2, 1, depth_ax=0, path='/tmp/pistol_side.png', title='pistola: horizontal=Z vertical=Y')
    print('ok')
