"""Lector minimo de GLB (glTF 2.0 binario) -> primitivas en espacio mundo. Sin dependencias salvo numpy."""
import json, struct, io
import numpy as np

_CT = {5120:('b',1),5121:('B',1),5122:('h',2),5123:('H',2),5125:('I',4),5126:('f',4)}
_NC = {'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}
_NP = {5120:np.int8,5121:np.uint8,5122:np.int16,5123:np.uint16,5125:np.uint32,5126:np.float32}

def load(path):
    b = open(path,'rb').read()
    off = 12; js = None; binc = b''
    while off < len(b):
        cl, ct = struct.unpack('<II', b[off:off+8]); ch = b[off+8:off+8+cl]
        if ct == 0x4E4F534A: js = json.loads(ch)
        elif ct == 0x004E4942: binc = ch
        off += 8 + cl
    return js, binc

def accessor(js, binc, idx):
    a = js['accessors'][idx]; bv = js['bufferViews'][a['bufferView']]
    n = _NC[a['type']]; dt = _NP[a['componentType']]; cnt = a['count']
    start = bv.get('byteOffset',0) + a.get('byteOffset',0)
    stride = bv.get('byteStride') or n * np.dtype(dt).itemsize
    if stride == n*np.dtype(dt).itemsize:
        arr = np.frombuffer(binc, dtype=dt, count=cnt*n, offset=start).reshape(cnt, n)
    else:
        arr = np.zeros((cnt,n), dtype=dt)
        for i in range(cnt):
            arr[i] = np.frombuffer(binc, dtype=dt, count=n, offset=start+i*stride)
    return arr.astype(np.float64) if dt == np.float32 else arr

def node_matrix(n):
    if 'matrix' in n:
        return np.array(n['matrix'], dtype=np.float64).reshape(4,4).T   # glTF es column-major
    T = np.eye(4); R = np.eye(4); S = np.eye(4)
    if 'translation' in n: T[:3,3] = n['translation']
    if 'rotation' in n:
        x,y,z,w = n['rotation']
        R[:3,:3] = [[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]]
    if 'scale' in n: S[0,0],S[1,1],S[2,2] = n['scale']
    return T @ R @ S

def primitives(js, binc):
    """Devuelve lista de dicts: path (nombres de nodos), pos, nrm, uv, idx, material (indice) en espacio mundo."""
    out = []
    def walk(ni, parentM, path):
        n = js['nodes'][ni]
        M = parentM @ node_matrix(n)
        path = path + [n.get('name', f'node{ni}')]
        if 'mesh' in n:
            for p in js['meshes'][n['mesh']]['primitives']:
                at = p['attributes']
                pos = accessor(js, binc, at['POSITION'])
                ph = np.hstack([pos, np.ones((len(pos),1))]) @ M.T
                nrm = None
                if 'NORMAL' in at:
                    N = np.linalg.inv(M[:3,:3]).T
                    nrm = accessor(js, binc, at['NORMAL']) @ N.T
                    l = np.linalg.norm(nrm, axis=1, keepdims=True); l[l==0] = 1; nrm = nrm / l
                uv = accessor(js, binc, at['TEXCOORD_0']) if 'TEXCOORD_0' in at else None
                idx = accessor(js, binc, p['indices']).reshape(-1).astype(np.int64) if 'indices' in p else np.arange(len(pos))
                mode = p.get('mode', 4)
                if mode == 5:      # triangle strip -> lista
                    idx = np.array([[idx[i], idx[i+1], idx[i+2]] if i % 2 == 0 else [idx[i+1], idx[i], idx[i+2]]
                                    for i in range(len(idx)-2)], dtype=np.int64).reshape(-1)
                elif mode == 6:    # triangle fan -> lista
                    idx = np.array([[idx[0], idx[i], idx[i+1]] for i in range(1, len(idx)-1)], dtype=np.int64).reshape(-1)
                elif mode != 4:
                    continue
                out.append(dict(path=path, pos=ph[:,:3], nrm=nrm, uv=uv, idx=idx, material=p.get('material')))
        for c in n.get('children', []): walk(c, M, path)
    for r in js['scenes'][js.get('scene',0)]['nodes']: walk(r, np.eye(4), [])
    return out

def image_bytes(js, binc, img_idx):
    im = js['images'][img_idx]; bv = js['bufferViews'][im['bufferView']]
    s = bv.get('byteOffset',0); return binc[s:s+bv['byteLength']]

def material_info(js, mi):
    """(color RGBA 0..1 lineal, indice de imagen o None)."""
    if mi is None: return (0.6,0.6,0.6,1.0), None
    m = js['materials'][mi]; pbr = m.get('pbrMetallicRoughness', {})
    ext = m.get('extensions', {}).get('KHR_materials_pbrSpecularGlossiness', {})
    col = pbr.get('baseColorFactor', ext.get('diffuseFactor', [1,1,1,1]))
    tex = pbr.get('baseColorTexture', ext.get('diffuseTexture'))
    img = js['textures'][tex['index']]['source'] if tex else None
    return tuple(col), img
