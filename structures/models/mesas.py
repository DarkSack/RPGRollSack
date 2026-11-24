"""
Modelos de las tres mesas de RPGRoll-Structures, para la skill minecraft-models.

    python <skill>/scripts/build.py structures/models/mesas.py --out build/mesas --targets vanilla

Se ven sobre un ItemDisplay sin transformación, centrado en el bloque (WorkbenchDisplay): el
bloque va de 0 a 16 y el suelo está en y = 0. El ítem de la mesa usa el mismo modelo. El display
no gira hacia el jugador, así que el frente (+z, sur) lleva el detalle y el resto se ve bien
desde cualquier lado.

    Constructor  taller de madera: banco de carpintero con herramientas y un plano
    Ingeniero    pizarra y redstone: banco de pizarra con circuito, lámpara y grúa
    Arquitecto   runas arcanas: pedestal rúnico con un cristal flotante y su halo
"""
from mcmodel import *

NAMESPACE = 'rpgroll_structures'
TITLE = 'RPGRoll-Structures'
AUTHOR = 'RPGRoll'

# Taller de madera
oak = material('wood', ramp('#4a2e17', '#a87a45', '#e0b77a'))
log = material('wood', ramp('#241509', '#5e3b1d', '#8a5a30'), tone=0.3)
iron = material('metal', ramp('#2a2f38', '#8b939e', '#eef2f6'))
copper = material('metal', ramp('#4a2412', '#c46c3c', '#ffc29a'))
blueprint = material('flat', ramp('#173a6e', '#2f62a8', '#9cc4ff'))
paper = material('flat', ramp('#bfae8a', '#efe4c8', '#fffaf0'))

# Pizarra y redstone
slate = material('plate', ramp('#17181d', '#3d3f48', '#6d707c'), panel=8)
slate_top = material('metal', ramp('#22242b', '#4c4f5a', '#8a8e9a'), flat=True)
redstone = material('energy', ramp('#3a0000', '#d11a1a', '#ff9a7a'), glow=9, cycles=1.0)
lamp = material('gem', ramp('#5a1a05', '#ff8a2a', '#fff0b0'), pulse=True, glow=13)

# Runas arcanas
runestone = material('rune', ramp('#120e1c', '#2e2442', '#55467a'), energy=ramp('#0f4a52', '#3fe0d0', '#d8fff8'))
gold = material('trim', ramp('#4a2c05', '#d9a520', '#fff1a8'))
crystal = material('crystal', ramp('#0d4a5a', '#5ad8e6', '#f0ffff'), glow=14)
amethyst = material('gem', ramp('#2a1240', '#9a5ad8', '#ecd8ff'))

MATS = {'oak': oak, 'log': log, 'iron': iron, 'copper': copper, 'blueprint': blueprint, 'paper': paper,
        'slate': slate, 'slate_top': slate_top, 'redstone': redstone, 'lamp': lamp,
        'runestone': runestone, 'gold': gold, 'crystal': crystal, 'amethyst': amethyst}


def constructor():
    p = [
        box('top', (8, 13.4, 8), (16, 2.4, 13), 'oak'),
        box('apron_front', (8, 11.3, 13.6), (14, 1.8, 1), 'log'),
        box('apron_back', (8, 11.3, 2.4), (14, 1.8, 1), 'log'),
        box('shelf', (8, 3.6, 8), (13, 1, 10), 'oak'),
        box('planks', (7, 4.9, 8), (9, 1.6, 5.5), 'log'),
        # tornillo de banco al frente
        box('vise_jaw', (12.4, 12.4, 15.1), (3.4, 2.2, 1.2), 'iron'),
        box('vise_screw', (12.4, 12, 16.1), (0.8, 0.8, 1.4), 'iron'),
        # herramientas sobre el tablero
        box('hammer_handle', (5, 15.1, 10.6), (6, 0.8, 0.8), 'log', r=(0, 22.5, 0)),
        box('hammer_head', (2.4, 15.4, 11.7), (1.4, 1.4, 2.8), 'iron', r=(0, 22.5, 0)),
        box('saw_blade', (10.6, 14.75, 5.2), (6.4, 0.3, 2.4), 'iron', r=(0, -22.5, 0)),
        box('saw_handle', (14.2, 15.0, 3.8), (1.8, 0.8, 2.2), 'oak', r=(0, -22.5, 0)),
        box('blueprint', (9.6, 14.7, 10.6), (5.6, 0.2, 4), 'blueprint'),
        box('blueprint_roll', (9.6, 15.1, 8.4), (5.8, 0.8, 0.8), 'paper'),
    ]
    for x in (1.8, 14.2):
        for z in (2.6, 13.4):
            p.append(box('leg', (x, 6.1, z), (2.4, 12.2, 2.4), 'log'))
            p.append(box('bracket', (x, 13.9, z), (2.8, 1.4, 2.8), 'copper'))
    return Item('workbench/constructor', p, MATS, style='furniture', base='paper',
                name='Mesa de construcción: Constructor')


def ingeniero():
    p = [
        box('base', (8, 4.6, 8), (15, 9.2, 15), 'slate'),
        box('plinth', (8, 0.6, 8), (16, 1.2, 16), 'slate_top'),
        box('top', (8, 9.8, 8), (16, 1.2, 16), 'slate_top'),
        box('front_strip', (8, 6.2, 15.6), (11, 0.9, 0.4), 'redstone'),
        # circuito sobre el tablero
        box('wire_a', (8, 10.5, 12.5), (12, 0.25, 0.8), 'redstone'),
        box('wire_b', (3, 10.5, 8), (0.8, 0.25, 9.8), 'redstone'),
        box('wire_c', (13, 10.5, 10), (0.8, 0.25, 5.8), 'redstone'),
        # lámpara en su marco
        box('lamp', (8, 12.6, 7.4), (4.6, 4.4, 4.6), 'lamp'),
        box('lamp_cap', (8, 15.1, 7.4), (5.6, 0.8, 5.6), 'iron'),
        # grúa pequeña al fondo
        box('crane_post', (2.4, 13.2, 2.4), (1.6, 6.8, 1.6), 'iron'),
        box('crane_arm', (5.6, 15.9, 2.4), (7.4, 1, 1), 'iron'),
        box('crane_hook', (8.6, 14.8, 2.4), (0.6, 1.6, 0.6), 'copper'),
        # antorchas de redstone delante
        box('torch_l', (2.6, 11.6, 13.4), (0.8, 2.4, 0.8), 'log'),
        box('torch_l_head', (2.6, 13.1, 13.4), (1.1, 1.1, 1.1), 'lamp'),
        box('torch_r', (13.4, 11.6, 13.4), (0.8, 2.4, 0.8), 'log'),
        box('torch_r_head', (13.4, 13.1, 13.4), (1.1, 1.1, 1.1), 'lamp'),
    ]
    for x in (5.6, 10.4):
        for z in (5, 9.8):
            p.append(box('lamp_post', (x, 12.6, z), (0.7, 4.4, 0.7), 'iron'))
    return Item('workbench/ingeniero', p, MATS, style='furniture', base='paper',
                name='Mesa de construcción: Ingeniero')


def frame(name, y, size, thick, height, mat):
    """Cuatro listones en el borde de un cuadrado centrado en (8, y, 8): un ribete, no una tapa."""
    h = size / 2 - thick / 2
    return [box(name, (8, y, 8 - h), (size, height, thick), mat), box(name, (8, y, 8 + h), (size, height, thick), mat),
            box(name, (8 - h, y, 8), (thick, height, size - 2 * thick), mat),
            box(name, (8 + h, y, 8), (thick, height, size - 2 * thick), mat)]


def arquitecto():
    p = [
        box('plinth', (8, 0.8, 8), (16, 1.6, 16), 'runestone'),
        box('base', (8, 3.6, 8), (13, 4.4, 13), 'runestone'),
        box('column', (8, 8.2, 8), (6.4, 4.2, 6.4), 'runestone'),
        box('top', (8, 10.8, 8), (14, 1.2, 14), 'runestone'),
        box('crystal', (8, 14.4, 8), (3.4, 4.2, 3.4), 'crystal', r=(0, 45, 0)),
        box('crystal_tip', (8, 14.4, 8), (2.2, 6.4, 2.2), 'crystal', r=(0, 45, 0)),
    ]
    p += frame('base_trim', 5.95, 13.4, 0.8, 0.5, 'gold')
    p += frame('top_trim', 11.55, 14.4, 0.8, 0.5, 'gold')
    p += ring('halo', (8, 14.4, 8), 4.6, 12, (0.7, 0.5, 2.2), 'gold')
    for x in (2.4, 13.6):
        for z in (2.4, 13.6):
            p.append(box('shard', (x, 13, z), (1.4, 2.8, 1.4), 'amethyst', r=(0, 0, 22.5 if x < 8 else -22.5)))
    return Item('workbench/arquitecto', p, MATS, style='furniture', base='paper',
                name='Mesa de construcción: Arquitecto')


MODELS = [constructor(), ingeniero(), arquitecto()]
