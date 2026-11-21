"""Para cada comando de jugador, las palabras del primer nivel que sugiere su autocompletado.

Lee comandos.txt (salida de comandos.py), busca la clase y saca los literales de las listas
(List.of / Arrays.asList / constantes SUB*) y de los `case "x"` del onCommand.
"""
import os
import re

ROOT = r'E:\Dev\RPGRoll'
S = os.path.dirname(os.path.abspath(__file__))  # comandos.txt: python comandos.py > comandos.txt
classes = {}
for dirpath, _, files in os.walk(ROOT):
    if '\\build' in dirpath or '\\test' in dirpath:
        continue
    for f in files:
        if f.endswith('.java'):
            classes.setdefault(f[:-5], []).append(os.path.join(dirpath, f))

for line in open(os.path.join(S, 'comandos.txt'), encoding='utf-8'):
    parts = line.split()
    if len(parts) != 4 or 'admin' in parts[1] or parts[1] in ('srp', 'rpgfx', 'rpgeffects', 'renchant', 'crate', 'npc', 'setwarp', 'delwarp'):
        continue
    module, name, cls, _ = parts
    paths = classes.get(cls, [])
    if not paths:
        continue
    src = open(paths[0], encoding='utf-8', errors='replace').read()
    cases = re.findall(r'case\s+((?:"[\w-]+"\s*,?\s*)+)->', src)
    words = []
    for c in cases:
        words.append('|'.join(re.findall(r'"([\w-]+)"', c)))
    if not words:
        m = re.search(r'(SUB\w*|ARGS?\w*)\s*=\s*(?:List\.of|Arrays\.asList|Set\.of)\(([^)]*)\)', src)
        if m:
            words = re.findall(r'"([\w-]+)"', m.group(2))
    print('%-10s %-10s %-24s %s' % (module, name, cls, ' '.join(words[:40])))
