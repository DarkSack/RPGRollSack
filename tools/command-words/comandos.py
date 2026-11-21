"""Inventario de comandos de RPGRoll: nombre, clase del executor y si autocompleta.

Busca registerCommand("x", new Y(...)) y las llamadas directas a BrigadierCommands.register.
Resuelve variables a su `new Clase(` del mismo archivo.
"""
import os
import re

ROOT = r'E:\Dev\RPGRoll'
SKIP = ('\\build', '\\test\\', '.gradle', '\\bin\\')

classes = {}
for dirpath, _, files in os.walk(ROOT):
    if any(s in dirpath + '\\' for s in SKIP):
        continue
    for f in files:
        if f.endswith('.java'):
            classes.setdefault(f[:-5], []).append(os.path.join(dirpath, f))


def completes(cls, depth=0):
    for path in classes.get(cls.split('.')[-1], []):
        src = open(path, encoding='utf-8', errors='replace').read()
        if re.search(r'TabCompleter|onTabComplete|TabExecutor', src):
            return 'si'
        m = re.search(r'class\s+\w+\s+extends\s+(\w+)', src)
        if m and depth < 4:
            return completes(m.group(1), depth + 1)
        return 'NO'
    return '?'


def resolve(var, text):
    m = re.search(r'\b' + re.escape(var) + r'\s*=\s*new\s+([\w.]+)\s*[(<]', text)
    return m.group(1).split('.')[-1] if m else None


def call_args(text, start):
    depth, i = 0, start
    while i < len(text):
        c = text[i]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
        i += 1
    return ''


rows = []
for module in sorted(os.listdir(ROOT)):
    src_dir = os.path.join(ROOT, module, 'src', 'main', 'java')
    if not os.path.isdir(src_dir) or module == 'common':
        continue
    for dirpath, _, files in os.walk(src_dir):
        for f in files:
            if not f.endswith('.java'):
                continue
            text = open(os.path.join(dirpath, f), encoding='utf-8', errors='replace').read()
            for m in re.finditer(r'(registerCommand|BrigadierCommands\.register)\s*\(', text):
                if re.search(r'(void|private|public)\s+\w*\s*$', text[max(0, m.start() - 40):m.start()]):
                    continue  # la propia definición del helper
                args = call_args(text, m.end() - 1)
                name = re.search(r'"([^"]+)"', args)
                if not name:
                    continue
                cls = re.search(r'new\s+([\w.]+)\s*\(', args)
                found = cls.group(1).split('.')[-1] if cls else None
                if not found:
                    for ident in re.findall(r'\b([a-z]\w*)\b', args):
                        if ident in ('this', 'plugin', 'null', 'name', 'info', 'aliases', 'executor', 'completer',
                                     'getConfig', 'getStringList', 'List', 'of', 'command'):
                            continue
                        r = resolve(ident, text)
                        if r:
                            found = r
                            break
                if not found and re.search(r'\bcommand\b', args):
                    found = resolve('command', text)
                rows.append((module, name.group(1), found or '?', completes(found) if found else '?'))
for r in rows:
    print('%-16s %-20s %-36s %s' % r)
print(len(rows), 'comandos;', sum(r[3] == 'NO' for r in rows), 'sin autocompletado;',
      sum(r[3] == '?' for r in rows), 'sin resolver')
