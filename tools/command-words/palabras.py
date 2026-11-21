"""Escribe command-words.yml en cada módulo y comprueba que cada subcomando exista en su código.

Uso: python tools/command-words/palabras.py (reescribe los command-words.yml; editar la tabla T).

Formato por comando: (nombres {lang: nombre}, subcomandos {canónico: {lang: palabra}}).
"""
import os
import re

ROOT = r'E:\Dev\RPGRoll'

T = {
    'ascension': {
        'ascend': ({'es': 'ascender'}, {
            'race': {'es': 'raza', 'pt': 'raca'},
            'specialize': {'es': 'especializar', 'pt': 'especializar'},
            'talent': {'es': 'talento', 'pt': 'talento'},
            'prestige': {'es': 'prestigio', 'pt': 'prestigio'},
            'affinity': {'es': 'afinidad', 'pt': 'afinidade'},
            'reputation': {'es': 'reputacion', 'pt': 'reputacao'},
            'title': {'es': 'titulo', 'pt': 'titulo'},
            'titles': {'es': 'titulos', 'pt': 'titulos'},
            'achievements': {'es': 'logros', 'pt': 'conquistas'},
            'jobs': {'es': 'oficios', 'pt': 'profissoes'},
            'secrets': {'es': 'secretos', 'pt': 'segredos'},
            'secret': {'es': 'secreto', 'pt': 'segredo'},
            'legacy': {'es': 'legado', 'pt': 'legado'},
        }),
    },
    'chat': {
        'channel': ({'es': 'canal', 'pt': 'canal'}, {
            'join': {'es': 'unirse', 'pt': 'entrar'},
            'leave': {'es': 'salir', 'pt': 'sair'},
            'switch': {'es': 'cambiar', 'pt': 'trocar'},
        }),
        'ch': ({}, {
            'join': {'es': 'unirse', 'pt': 'entrar'},
            'leave': {'es': 'salir', 'pt': 'sair'},
            'switch': {'es': 'cambiar', 'pt': 'trocar'},
        }),
        'ignore': ({'es': 'ignorar', 'pt': 'ignorar'}, {
            'player': {'es': 'jugador', 'pt': 'jogador'},
            'guild': {'es': 'gremio', 'pt': 'guilda'},
            'channel': {'es': 'canal', 'pt': 'canal'},
        }),
        'language': ({'es': 'idioma', 'pt': 'idioma'}, {
            'learn': {'es': 'aprender', 'pt': 'aprender'},
            'speak': {'es': 'hablar', 'pt': 'falar'},
        }),
    },
    'core': {
        'rpg': ({}, {
            'help': {'es': 'ayuda', 'pt': 'ajuda'},
            'stats': {'es': 'estadisticas', 'pt': 'estatisticas'},
            'level': {'es': 'nivel', 'pt': 'nivel'},
            'class': {'es': 'clase', 'pt': 'classe'},
            'race': {'es': 'raza', 'pt': 'raca'},
            'create': {'es': 'crear', 'pt': 'criar'},
            'skills': {'es': 'habilidades', 'pt': 'habilidades'},
            'traits': {'es': 'rasgos', 'pt': 'tracos'},
            'mystats': {'es': 'detalles', 'pt': 'detalhes'},
            'jobs': {'es': 'trabajos', 'pt': 'profissoes'},
            'allocate': {'es': 'asignar', 'pt': 'atribuir'},
            'use': {'es': 'usar', 'pt': 'usar'},
        }),
    },
    'crafting': {
        'crafting': ({'es': 'artesania', 'pt': 'artesanato'}, {
            'book': {'es': 'libro', 'pt': 'livro'},
            'discover': {'es': 'descubrir', 'pt': 'descobrir'},
            'upgrade': {'es': 'mejorar', 'pt': 'melhorar'},
            'experiment': {'es': 'experimentar', 'pt': 'experimentar'},
            'proficiency': {'es': 'maestria', 'pt': 'maestria'},
        }),
    },
    'crates': {
        'tesoro': ({'en': 'treasure', 'pt': 'tesouro'}, {}),
    },
    'dungeons': {
        'dungeon': ({'es': 'mazmorra', 'pt': 'masmorra'}, {
            'list': {'es': 'lista', 'pt': 'lista'},
            'invite': {'es': 'invitar', 'pt': 'convidar'},
            'accept': {'es': 'aceptar', 'pt': 'aceitar'},
            'decline': {'es': 'rechazar', 'pt': 'recusar'},
            'leaveparty': {'es': 'salirgrupo', 'pt': 'sairgrupo'},
            'enter': {'es': 'entrar', 'pt': 'entrar'},
            'leave': {'es': 'salir', 'pt': 'sair'},
            'revive': {'es': 'revivir', 'pt': 'reviver'},
        }),
    },
    'economy': {
        'economy': ({'es': 'economia', 'pt': 'economia'}, {
            'balance': {'es': 'saldo', 'pt': 'saldo'},
            'pay': {'es': 'pagar', 'pt': 'pagar'},
            'bank': {'es': 'banco', 'pt': 'banco'},
            'shop': {'es': 'tienda', 'pt': 'loja'},
            'auction': {'es': 'subasta', 'pt': 'leilao'},
            'company': {'es': 'empresa', 'pt': 'empresa'},
        }),
        'tienda': ({'en': 'shop', 'pt': 'loja'}, {}),
        'subasta': ({'en': 'auction', 'pt': 'leilao'}, {
            'vender': {'en': 'sell', 'pt': 'vender'},
            'subastar': {'en': 'auction', 'pt': 'leiloar'},
            'buscar': {'en': 'search', 'pt': 'buscar'},
            'mis': {'en': 'mine', 'pt': 'meus'},
            'recoger': {'en': 'collect', 'pt': 'coletar'},
        }),
    },
    'extras': {
        'ordenar': ({'en': 'sort', 'pt': 'organizar'}, {
            'varita': {'en': 'wand', 'pt': 'varinha'},
            'on': {'es': 'activar', 'pt': 'ligar'},
            'off': {'es': 'desactivar', 'pt': 'desligar'},
        }),
        'mochila': ({'en': 'backpack', 'pt': 'mochila'}, {}),
        'warp': ({}, {'list': {'es': 'lista', 'pt': 'lista'}}),
        'warps': ({}, {'list': {'es': 'lista', 'pt': 'lista'}}),
        'death': ({'es': 'muerte', 'pt': 'morte'}, {}),
    },
    'fishing': {
        'fishing': ({'es': 'pesca', 'pt': 'pesca'}, {
            'encyclopedia': {'es': 'enciclopedia', 'pt': 'enciclopedia'},
            'stats': {'es': 'estadisticas', 'pt': 'estatisticas'},
        }),
    },
    'furniture': {
        'muebles': ({'en': 'furniture', 'pt': 'moveis'}, {
            'fabricar': {'en': 'craft', 'pt': 'fabricar'},
        }),
    },
    'guilds': {
        'guild': ({'es': 'gremio', 'pt': 'guilda'}, {
            'create': {'es': 'crear', 'pt': 'criar'},
            'disband': {'es': 'disolver', 'pt': 'dissolver'},
            'accept': {'es': 'aceptar', 'pt': 'aceitar'},
            'decline': {'es': 'rechazar', 'pt': 'recusar'},
            'leave': {'es': 'salir', 'pt': 'sair'},
            'vault': {'es': 'baul', 'pt': 'cofre'},
            'territory': {'es': 'territorio', 'pt': 'territorio'},
            'upgrade': {'es': 'mejoras', 'pt': 'melhorias'},
            'diplomacy': {'es': 'diplomacia', 'pt': 'diplomacia'},
            'quest': {'es': 'misiones', 'pt': 'missoes'},
            'achievements': {'es': 'logros', 'pt': 'conquistas'},
            'calendar': {'es': 'calendario', 'pt': 'calendario'},
            'customize': {'es': 'personalizar', 'pt': 'personalizar'},
            'members': {'es': 'miembros', 'pt': 'membros'},
            'browser': {'es': 'explorar', 'pt': 'explorar'},
            'color': {'es': 'color', 'pt': 'cor'},
        }),
        'team': ({'es': 'equipo', 'pt': 'equipe'}, {
            'invite': {'es': 'invitar', 'pt': 'convidar'},
            'accept': {'es': 'aceptar', 'pt': 'aceitar'},
            'decline': {'es': 'rechazar', 'pt': 'recusar'},
            'leave': {'es': 'salir', 'pt': 'sair'},
            'kick': {'es': 'expulsar', 'pt': 'expulsar'},
            'waypoint': {'es': 'marcador', 'pt': 'marcador'},
            'queue': {'es': 'cola', 'pt': 'fila'},
            'enemigo': {'en': 'enemy', 'pt': 'inimigo'},
            'objetivo': {'en': 'objective', 'pt': 'objetivo'},
            'loot': {'es': 'botin', 'pt': 'saque'},
        }),
    },
    'items': {
        'item': ({'es': 'objeto', 'pt': 'objeto'}, {
            'upgrade': {'es': 'mejorar', 'pt': 'melhorar'},
            'skin': {'es': 'aspecto', 'pt': 'visual'},
            'socket': {'es': 'engarzar', 'pt': 'engastar'},
        }),
    },
    'machines': {
        'machines': ({'es': 'maquinas', 'pt': 'maquinas'}, {
            'quarries': {'es': 'canteras', 'pt': 'pedreiras'},
        }),
    },
    'magic': {
        'magic': ({'es': 'magia', 'pt': 'magia'}, {
            'spellbook': {'es': 'grimorio', 'pt': 'grimorio'},
            'select': {'es': 'elegir', 'pt': 'escolher'},
            'cast': {'es': 'lanzar', 'pt': 'lancar'},
            'cooldowns': {'es': 'recargas', 'pt': 'recargas'},
        }),
    },
    'mobs': {
        'bestiario': ({'en': 'bestiary', 'pt': 'bestiario'}, {}),
    },
    'pass': {
        'pase': ({'pt': 'passe'}, {
            'misiones': {'en': 'missions', 'pt': 'missoes'},
            'reclamar': {'en': 'claim', 'pt': 'resgatar'},
        }),
    },
    'quests': {
        'quest': ({'es': 'mision', 'pt': 'missao'}, {
            'start': {'es': 'empezar', 'pt': 'iniciar'},
            'abandon': {'es': 'abandonar', 'pt': 'abandonar'},
            'active': {'es': 'activas', 'pt': 'ativas'},
            'continue': {'es': 'continuar', 'pt': 'continuar'},
            'completed': {'es': 'completadas', 'pt': 'concluidas'},
            'option': {'es': 'opcion', 'pt': 'opcao'},
        }),
    },
    'ranching': {
        'ranching': ({'es': 'rancho', 'pt': 'rancho'}, {
            'inspect': {'es': 'inspeccionar', 'pt': 'inspecionar'},
            'animales': {'en': 'animals', 'pt': 'animais'},
            'mercado': {'en': 'market', 'pt': 'mercado'},
            'reclamar': {'en': 'claim', 'pt': 'resgatar'},
            'nombrar': {'en': 'name', 'pt': 'nomear'},
            'vender': {'en': 'sell', 'pt': 'vender'},
            'comprar': {'en': 'buy', 'pt': 'comprar'},
            'llamar': {'en': 'call', 'pt': 'chamar'},
            'corral': {'en': 'pen', 'pt': 'curral'},
        }),
    },
    'seasons': {
        'seasons': ({'es': 'estaciones', 'pt': 'estacoes'}, {}),
    },
    'workers': {
        'workers': ({'es': 'trabajadores', 'pt': 'trabalhadores'}, {
            'inspect': {'es': 'inspeccionar', 'pt': 'inspecionar'},
            'hire': {'es': 'contratar', 'pt': 'contratar'},
            'fire': {'es': 'despedir', 'pt': 'demitir'},
            'list': {'es': 'lista', 'pt': 'lista'},
        }),
    },
}


def source(module):
    out = []
    for d, _, fs in os.walk(os.path.join(ROOT, module, 'src', 'main', 'java')):
        for f in fs:
            if f.endswith('.java'):
                out.append(open(os.path.join(d, f), encoding='utf-8', errors='replace').read())
    return '\n'.join(out)


problems = 0
all_names = {}
for module, commands in T.items():
    src = source(module)
    lines = ['# Nombres de los comandos y sus subcomandos en cada idioma (lo lee BrigadierCommands de la Lib).',
             '# El nombre de cada clave es el de siempre; el comando recibe esa palabra escriba lo que escriba el jugador.',
             'commands:']
    for cmd, (names, subs) in commands.items():
        if f'"{cmd}"' not in src:
            print('FALTA comando', module, cmd); problems += 1
        for n in names.values():
            if n != cmd and all_names.setdefault(n, cmd) != cmd:
                print('NOMBRE REPETIDO', n, cmd, all_names[n]); problems += 1
        lines.append(f'  {cmd}:')
        if names:
            lines.append('    names: {' + ', '.join(f'{k}: {v}' for k, v in names.items()) + '}')
        if subs:
            lines.append('    subcommands:')
            seen = {}
            for key, words in subs.items():
                if not re.search(r'"' + re.escape(key) + r'"', src):
                    print('FALTA sub', module, cmd, key); problems += 1
                for w in words.values():
                    if w != key and seen.setdefault(w, key) != key:
                        print('PALABRA REPETIDA', cmd, w, key, seen[w]); problems += 1
                lines.append(f'      {key}: {{' + ', '.join(f'{k}: {v}' for k, v in words.items()) + '}')
    path = os.path.join(ROOT, module, 'src', 'main', 'resources', 'command-words.yml')
    open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
print('problemas:', problems)
