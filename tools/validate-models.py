"""Valida recursos Java, Blockbench y Bedrock exportados. Python 3.8+, sin dependencias.

python tools/validate-models.py [--network ../sackito-network] [--out informe.json]
La validación estática no sustituye una revisión dentro de Java/Bedrock.
"""
import argparse
import collections
import itertools
import json
import math
import struct
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1])
parser.add_argument('--network', type=Path)
parser.add_argument('--out', type=Path)
args = parser.parse_args()
ROOTS = [args.repo / 'packs'] + list(args.repo.glob('*/src/main/resources'))
if args.network:
    ROOTS.append(args.network / 'config/prod')
if any(not root.is_dir() for root in ROOTS):
    parser.error('Falta un directorio de recursos solicitado')
findings, counts = [], collections.Counter()

def issue(kind, path, detail):
    findings.append(dict(kind=kind, path=str(path), detail=detail))

def read(p):
    return json.loads(p.read_text(encoding='utf-8-sig'))

def dimensions(data):
    return struct.unpack('>II', data[16:24])

def vertices(e):
    r = [math.radians(v) for v in e.get('rotation', [0, 0, 0])]
    o = e.get('origin', [0, 0, 0])
    out = []
    for p in itertools.product(*zip(e['from'], e['to'])):
        x, y, z = [v - c for v, c in zip(p, o)]
        y, z = y * math.cos(r[0]) - z * math.sin(r[0]), y * math.sin(r[0]) + z * math.cos(r[0])
        x, z = x * math.cos(r[1]) + z * math.sin(r[1]), -x * math.sin(r[1]) + z * math.cos(r[1])
        x, y = x * math.cos(r[2]) - y * math.sin(r[2]), x * math.sin(r[2]) + y * math.cos(r[2])
        out.append([x + o[0], y + o[1], z + o[2]])
    return out

for root in ROOTS:
    for p in root.rglob('*.json'):
        counts['json'] += 1
        try:
            d = read(p)
        except Exception as e:
            issue('invalid_json', p, str(e))
            continue
        if '/models/' in p.as_posix() and 'elements' in d:
            counts['java_models'] += 1
            for e in d['elements']:
                if any(v < -16.0001 or v > 32.0001 for v in e.get('from', []) + e.get('to', [])):
                    issue('java_range', p, e.get('name', e.get('from')))
                r = e.get('rotation', {})
                if 'axis' in r and abs(r['angle']) > 45:
                    issue('java_angle', p, r)
            # Resolve locally owned textures in this assets tree, leaving vanilla refs alone.
            try:
                assets = next(a for a in p.parents if a.name == 'assets')
            except StopIteration:
                continue
            for key, tex in d.get('textures', {}).items():
                if not isinstance(tex, str) or tex.startswith('#') or ':' not in tex:
                    continue
                ns, name = tex.split(':', 1)
                if ns != 'minecraft' and not (assets / ns / 'textures' / (name + '.png')).is_file():
                    issue('missing_java_texture', p, tex)
        if '/items/' in p.as_posix() and isinstance(d.get('model'), dict):
            def refs(v):
                if isinstance(v, dict):
                    if v.get('type') == 'minecraft:model':
                        yield v.get('model', '')
                    for x in v.values():
                        yield from refs(x)
                elif isinstance(v, list):
                    for x in v:
                        yield from refs(x)
            try:
                assets = next(a for a in p.parents if a.name == 'assets')
            except StopIteration:
                continue
            for ref in refs(d):
                if ':' in ref:
                    ns, name = ref.split(':', 1)
                    if ns != 'minecraft' and not (assets / ns / 'models' / (name + '.json')).is_file():
                        issue('missing_java_model', p, ref)
    for p in root.rglob('*.bbmodel'):
        counts['bbmodels'] += 1
        d = read(p)
        if d.get('meta', {}).get('model_format') != 'free':
            continue
        es = {e['uuid']: e for e in d.get('elements', [])}
        groups = set()
        def walk(g):
            if not isinstance(g, dict):
                return
            groups.add(g['uuid'])
            vs = []
            for c in g.get('children', []):
                if isinstance(c, dict):
                    walk(c)
                elif c in es:
                    vs.extend(vertices(es[c]))
            if vs and g['name'] != 'hitbox':
                extent = max(max(v[i] for v in vs) - min(v[i] for v in vs) for i in range(3))
                if extent > 48.01:
                    issue('wide_creature_bone', p, dict(bone=g['name'], extent=round(extent, 3)))
        for g in d.get('outliner', []):
            walk(g)
        for a in d.get('animations', []):
            if not set(a.get('animators', {})) <= groups:
                issue('missing_animation_bone', p, a['name'])
    for p in root.rglob('*.mcpack'):
        counts['mcpacks'] += 1
        with zipfile.ZipFile(p) as z:
            names = set(z.namelist())
            docs = {n: json.loads(z.read(n)) for n in names if n.endswith('.json')}
            docs = {n: d for n, d in docs.items() if isinstance(d, dict)}
            counts['bedrock_json'] += len(docs)
            geos = {g['description']['identifier'] for d in docs.values() for g in d.get('minecraft:geometry', [])}
            ctrls = {k for d in docs.values() for k in d.get('render_controllers', {})}
            man = docs.get('manifest.json', {})
            if any(v > 65535 for v in man.get('header', {}).get('version', [])):
                issue('bedrock_version', p, man['header']['version'])
            for n in names:
                if len(n) >= 80:
                    issue('bedrock_long_path', p, n)
            for n, d in docs.items():
                desc = d.get('minecraft:attachable', {}).get('description')
                if not desc:
                    continue
                counts['attachables'] += 1
                for alias, ref in desc.get('geometry', {}).items():
                    if ref not in geos and not ref.startswith('geometry.player.armor.'):
                        issue('missing_bedrock_geometry', str(p) + '!' + n, ref)
                for alias, ref in desc.get('textures', {}).items():
                    if ref in ('textures/misc/enchanted_item_glint', 'textures/misc/enchanted_actor_glint'):
                        continue
                    if ref + '.png' not in names:
                        issue('missing_bedrock_texture', str(p) + '!' + n, ref)
                    elif '/armadura/' in ref or alias == 'worn':
                        w, h = dimensions(z.read(ref + '.png'))
                        if w != 2 * h:
                            issue('bedrock_armor_strip', str(p) + '!' + n, dict(texture=ref, size=[w, h]))
                for rc in desc.get('render_controllers', []):
                    if isinstance(rc, str) and rc not in ctrls and rc not in ('controller.render.item_default', 'controller.render.armor'):
                        issue('missing_bedrock_controller', str(p) + '!' + n, rc)

result = dict(counts=counts, totals=collections.Counter(f['kind'] for f in findings), findings=findings)
if args.out:
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({k: result[k] for k in ('counts', 'totals')}, indent=2))
for kind in result['totals']:
    print(kind, json.dumps(next(f for f in findings if f['kind'] == kind), ensure_ascii=False))

raise SystemExit(1 if findings else 0)
