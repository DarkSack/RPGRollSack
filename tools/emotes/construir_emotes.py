"""Animaciones y catálogo de fábrica de RPGRoll-Emotes.

Toma la plantilla del jugador de BetterModel (plantilla/steve.bbmodel, MIT) y escribe:
  - animations/rpgroll_emotes.bbmodel: el modelo de extremidades con las 23 animaciones;
  - emotes/<categoría>/<id>.yml: un emote por animación.

    python -I tools/emotes/construir_emotes.py [--salida <carpeta>]

Sin --salida escribe en emotes/src/main/resources del repositorio. En el juego se prueban con
/bm play rpgroll_emotes <animación>.

    python -I tools/emotes/construir_emotes.py --pack mareas-malditas|dinastia-de-jade [--salida <carpeta>]

escribe los de un pack temático en packs/<pack>/RPGRoll-Emotes/: animations/<pack>_emotes.bbmodel (con
guiones bajos: mareas_malditas_emotes) y emotes/<id>.yml. BetterModel nombra el modelo por el archivo, así
que el «model» de esos emotes es ese mismo nombre. rpgroll_emotes.bbmodel queda reservado para el de fábrica.

Convenciones de ejes, probadas en el juego (rotación de Blockbench, en grados):
  - +z sube el brazo derecho por fuera (el izquierdo, -z).
  - +x lleva brazos y piernas al frente.
  - +y gira el brazo derecho hacia dentro; el izquierdo usa -y.
  - En los huesos del torso (cadera, cintura, pecho, cabeza), -x inclina hacia delante.
  - Codos y rodillas se doblan en x: el antebrazo hacia +x y la pierna de abajo (foreleg) hacia -x.
Las unidades de posición son píxeles (1/16 de bloque). Los bucles empiezan y acaban en la misma pose;
los emotes cortos empiezan y acaban en la pose neutra (todo a 0); los «hold» (sentarse) empiezan en la neutra
y se quedan en la última. Las poses mantenidas se interpolan en lineal (mesetas_lineales).
Los ángulos de manos y apoyos se midieron con cinemática directa (rotaciones X, luego Y, luego Z).
"""
import argparse
import copy
import json
import random
import uuid
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
PLANTILLA = Path(__file__).resolve().parent / "plantilla" / "steve.bbmodel"
MODELO = "rpgroll_emotes"

base = json.loads(PLANTILLA.read_text(encoding="utf-8"))
HUESO = {g["name"]: g["uuid"] for g in base["groups"]}

# uuid estables entre ejecuciones: el mismo script da el mismo archivo (y el mismo hash al instalarlo).
_azar = random.Random(20261119)


def nuevo_uuid():
    return str(uuid.UUID(int=_azar.getrandbits(128), version=4))


def kf(canal, t, x=0.0, y=0.0, z=0.0, interp="catmullrom"):
    return {"channel": canal, "data_points": [{"x": str(x), "y": str(y), "z": str(z)}],
            "uuid": nuevo_uuid(), "time": round(t, 5), "color": -1, "interpolation": interp}


def anim(nombre, largo, pistas, loop="once"):
    """pistas = {hueso: [(canal, t, x, y, z), ...]}"""
    animators = {}
    for hueso, claves in pistas.items():
        animators[HUESO[hueso]] = {
            "name": hueso, "type": "bone", "rotation_global": False, "quaternion_interpolation": False,
            "keyframes": [kf(*c) for c in claves],
        }
    return {"uuid": nuevo_uuid(), "name": nombre, "loop": loop, "override": False, "length": largo,
            "snapping": 20, "selected": False, "group_name": "", "anim_time_update": "", "blend_weight": "",
            "start_delay": "", "loop_delay": "", "animators": animators}


def rot(t, x=0, y=0, z=0):
    return ("rotation", t, x, y, z)


def pos(t, x=0, y=0, z=0):
    return ("position", t, x, y, z)


def lineal(clave):
    """La misma clave con interpolación lineal (movimientos secos, como el robot)."""
    return clave + ("linear",)


# ============================== prototipo probado en el juego (no tocar) ==============================

# --- saludar: brazo derecho arriba y el antebrazo va y viene ---
saludar = anim("saludar", 2.0, {
    "pra_right_arm": [rot(0), rot(0.3, 0, 0, 160), rot(1.7, 0, 0, 160), rot(2.0)],
    "prfa_right_forearm": [rot(0), rot(0.3), rot(0.5, 0, 0, 30), rot(0.75, 0, 0, -20), rot(1.0, 0, 0, 30),
                           rot(1.25, 0, 0, -20), rot(1.5, 0, 0, 30), rot(1.7), rot(2.0)],
    "h_ph_head": [rot(0), rot(0.4, 0, 0, -8), rot(1.6, 0, 0, -8), rot(2.0)],
})

# --- bailar: 8 tiempos de 0,5 s; cadera de lado a lado, brazos alternos arriba, rodillas y cabeza ---
B = 0.5
cadera, cuerpo, cabeza, bd, bi, abd, abi, pd, pi, rd, ri = ([] for _ in range(11))
for i in range(9):
    t = i * B
    lado = 1 if i % 2 == 0 else -1
    fin = i in (0, 8)
    cadera.append(rot(t, 0, 0, 0 if fin else 8 * lado))
    cuerpo.append(pos(t, 0, 0 if fin else -1.0, 0))
    cabeza.append(rot(t, 0, 0, 0 if fin else -10 * lado))
    bd.append(rot(t, 0, 0, 0 if fin else (150 if lado > 0 else 25)))
    bi.append(rot(t, 0, 0, 0 if fin else (-25 if lado > 0 else -150)))
    abd.append(rot(t, 0, 0, 0 if fin else (20 if lado > 0 else -60)))
    abi.append(rot(t, 0, 0, 0 if fin else (60 if lado > 0 else -20)))
    pd.append(rot(t, 0 if fin else (-25 if lado > 0 else 0), 0, 0))
    rd.append(rot(t, 0 if fin else (40 if lado > 0 else 0), 0, 0))
    pi.append(rot(t, 0 if fin else (0 if lado > 0 else -25), 0, 0))
    ri.append(rot(t, 0 if fin else (0 if lado > 0 else 40), 0, 0))
    if not fin:  # medio tiempo: vuelve al centro para que se note el ritmo
        m = t + B / 2
        cuerpo.append(pos(m, 0, 0, 0))
        cadera.append(rot(m, 0, 0, 0))
bailar = anim("bailar", 4.0, {
    "phip_hip": cadera, "player_root": cuerpo, "h_ph_head": cabeza,
    "pra_right_arm": bd, "pla_left_arm": bi, "prfa_right_forearm": abd, "plfa_left_forearm": abi,
    "prl_right_leg": pd, "prfl_right_foreleg": rd, "pll_left_leg": pi, "plfl_left_foreleg": ri,
})

# --- aplaudir: brazos al frente, se juntan y se separan 5 veces ---
# Probado en el juego: +x lleva el brazo al frente; +y gira el brazo derecho hacia dentro (el izquierdo con -y).
# Las manos se tocan con ~27° (hombro a 5,6 px del centro, brazo de ~11 px).
abre, cierra = 8, 27
dd, di = [rot(0), rot(0.25, 75, abre, 0)], [rot(0), rot(0.25, 75, -abre, 0)]
for i in range(5):
    t = 0.25 + i * 0.3
    dd += [rot(t + 0.15, 75, cierra, 0), rot(t + 0.3, 75, abre, 0)]
    di += [rot(t + 0.15, 75, -cierra, 0), rot(t + 0.3, 75, -abre, 0)]
dd.append(rot(2.0))
di.append(rot(2.0))
aplaudir = anim("aplaudir", 2.0, {
    "pra_right_arm": dd, "pla_left_arm": di,
    "h_ph_head": [rot(0), rot(0.3, -10, 0, 0), rot(1.8, -10, 0, 0), rot(2.0)],
})

# --- conjurar: para RPGRoll-Magic. Rápido (0,8 s) porque los hechizos se lanzan al instante.
# Carga: brazo derecho arriba y atrás, mano izquierda al pecho, torso hacia atrás.
# Lanza: los dos brazos al frente de golpe y el torso hacia delante. Vuelve.
# Huesos que apuntan hacia arriba (torso): -x los inclina hacia delante.
CONJURAR = {
    "pra_right_arm": [rot(0), rot(0.22, 150, -10, 15), rot(0.38, 85, 10, 0), rot(0.6, 85, 10, 0), rot(0.8)],
    "prfa_right_forearm": [rot(0), rot(0.22, 40, 0, 0), rot(0.38), rot(0.8)],
    "pla_left_arm": [rot(0), rot(0.22, 45, -35, 0), rot(0.38, 80, -12, 0), rot(0.6, 80, -12, 0), rot(0.8)],
    "plfa_left_forearm": [rot(0), rot(0.22, 60, 0, 0), rot(0.38), rot(0.8)],
    "pc_chest": [rot(0), rot(0.22, 8, 10, 0), rot(0.38, -10, -5, 0), rot(0.6, -6, 0, 0), rot(0.8)],
    "h_ph_head": [rot(0), rot(0.22, 8, 0, 0), rot(0.38, -5, 0, 0), rot(0.8)],
}
conjurar = anim("conjurar", 0.8, CONJURAR)

# ============================================ social ============================================

# --- reverencia: se inclina desde la cintura, mano derecha al vientre y la izquierda a la espalda ---
# Brazo izquierdo -50° atrás y +10° hacia dentro (para un brazo que va atrás, +y lo mete hacia el centro),
# antebrazo +50: la mano queda tras la cintura, a 0,2 px de ella (medido con cinemática directa).
reverencia = anim("reverencia", 2.5, {
    "pw_waist": [rot(0), rot(0.7, -35, 0, 0), rot(1.7, -35, 0, 0), rot(2.5)],
    "h_ph_head": [rot(0), rot(0.7, -10, 0, 0), rot(1.7, -10, 0, 0), rot(2.5)],
    "pra_right_arm": [rot(0), rot(0.7, 40, 30, 0), rot(1.7, 40, 30, 0), rot(2.5)],
    "prfa_right_forearm": [rot(0), rot(0.7, 70, 0, 0), rot(1.7, 70, 0, 0), rot(2.5)],
    "pla_left_arm": [rot(0), rot(0.7, -50, 10, 0), rot(1.7, -50, 10, 0), rot(2.5)],
    "plfa_left_forearm": [rot(0), rot(0.7, 50, 0, 0), rot(1.7, 50, 0, 0), rot(2.5)],
})

# --- asentir: dos cabezadas ---
asentir = anim("asentir", 1.5, {
    "h_ph_head": [rot(0), rot(0.25, -20, 0, 0), rot(0.5, 5, 0, 0), rot(0.75, -20, 0, 0), rot(1.0, 5, 0, 0),
                  rot(1.3), rot(1.5)],
    "pc_chest": [rot(0), rot(0.25, -3, 0, 0), rot(1.0, -3, 0, 0), rot(1.5)],
})

# --- negar: la cabeza de lado a lado, un poco gacha ---
negar = anim("negar", 1.6, {
    "h_ph_head": [rot(0), rot(0.25, -5, 25, 0), rot(0.55, -5, -25, 0), rot(0.85, -5, 25, 0),
                  rot(1.15, -5, -25, 0), rot(1.4), rot(1.6)],
})

# --- señalar: brazo derecho recto al frente; el pecho adelanta el hombro y la cabeza sigue mirando al frente ---
senalar = anim("senalar", 2.0, {
    "pra_right_arm": [rot(0), rot(0.35, 90, 5, 0), rot(0.6, 95, 5, 0), rot(1.6, 90, 5, 0), rot(2.0)],
    "pc_chest": [rot(0), rot(0.35, 0, 10, 0), rot(1.6, 0, 10, 0), rot(2.0)],
    "h_ph_head": [rot(0), rot(0.35, 0, -10, 0), rot(1.6, 0, -10, 0), rot(2.0)],
})

# --- encogerse: hombros arriba, antebrazos al frente con las palmas abiertas y la cabeza ladeada ---
# Los hombros suben moviendo los brazos 1 px (posición), no girándolos.
_hombros = [pos(0), pos(0.35, 0, 1, 0), pos(1.2, 0, 1, 0), pos(1.8)]
encogerse = anim("encogerse", 1.8, {
    "pra_right_arm": [rot(0), rot(0.35, 10, 0, 20), rot(1.2, 10, 0, 20), rot(1.8)] + _hombros,
    "pla_left_arm": [rot(0), rot(0.35, 10, 0, -20), rot(1.2, 10, 0, -20), rot(1.8)] + _hombros,
    "prfa_right_forearm": [rot(0), rot(0.35, 60, 0, 0), rot(1.2, 60, 0, 0), rot(1.8)],
    "plfa_left_forearm": [rot(0), rot(0.35, 60, 0, 0), rot(1.2, 60, 0, 0), rot(1.8)],
    "h_ph_head": [rot(0), rot(0.35, 5, 0, 10), rot(1.2, 5, 0, 10), rot(1.8)],
})

# --- facepalm: la mano derecha a la frente (brazo al frente y hacia dentro, antebrazo arriba) ---
# La mano queda a ~1,1 px de la cara, sin entrar en la cabeza (medido con cinemática directa).
facepalm = anim("facepalm", 2.2, {
    "pra_right_arm": [rot(0), rot(0.4, 70, 25, 0), rot(1.8, 70, 25, 0), rot(2.2)],
    "prfa_right_forearm": [rot(0), rot(0.4, 75, 0, 0), rot(1.8, 75, 0, 0), rot(2.2)],
    "h_ph_head": [rot(0), rot(0.4, -10, 0, 0), rot(0.9, -10, 5, 0), rot(1.3, -10, -5, 0), rot(1.8, -10, 0, 0),
                  rot(2.2)],
    "pc_chest": [rot(0), rot(0.4, -5, 0, 0), rot(1.8, -5, 0, 0), rot(2.2)],
})

# --- reír: cabeza atrás, mano al vientre y el pecho se sacude con la risa ---
_risa = [rot(0), rot(0.3, 6, 0, 0)]
for i in range(4):
    _risa += [rot(0.45 + i * 0.3, 2, 0, 0), rot(0.6 + i * 0.3, 6, 0, 0)]
_risa += [rot(1.6), rot(2.0)]
reir = anim("reir", 2.0, {
    "pc_chest": _risa,
    "h_ph_head": [rot(0), rot(0.3, 15, 0, 0), rot(1.6, 15, 0, 0), rot(2.0)],
    "pra_right_arm": [rot(0), rot(0.3, 30, 20, 0), rot(1.6, 30, 20, 0), rot(2.0)],
    "prfa_right_forearm": [rot(0), rot(0.3, 60, 0, 0), rot(1.6, 60, 0, 0), rot(2.0)],
    "pla_left_arm": [rot(0), rot(0.3, 0, 0, -10), rot(1.6, 0, 0, -10), rot(2.0)],
})

# --- llorar: las dos manos a los ojos, cabeza gacha y sollozos en el pecho ---
_sollozo = [rot(0), rot(0.4, -10, 0, 0)]
for i in range(4):
    _sollozo += [rot(0.6 + i * 0.35, -6, 0, 0), rot(0.75 + i * 0.35, -10, 0, 0)]
_sollozo += [rot(2.1, -10, 0, 0), rot(2.5)]
llorar = anim("llorar", 2.5, {
    "pc_chest": _sollozo,
    # Manos a ~1,1 px de la cara (cinemática directa).
    "h_ph_head": [rot(0), rot(0.4, -10, 0, 0), rot(2.1, -10, 0, 0), rot(2.5)],
    "pra_right_arm": [rot(0), rot(0.4, 70, 20, 0), rot(2.1, 70, 20, 0), rot(2.5)],
    "pla_left_arm": [rot(0), rot(0.4, 70, -20, 0), rot(2.1, 70, -20, 0), rot(2.5)],
    "prfa_right_forearm": [rot(0), rot(0.4, 75, 0, 0), rot(2.1, 75, 0, 0), rot(2.5)],
    "plfa_left_forearm": [rot(0), rot(0.4, 75, 0, 0), rot(2.1, 75, 0, 0), rot(2.5)],
})

# --- pensar: mano derecha al mentón, el brazo izquierdo cruzado la sostiene y la cabeza mira arriba ---
# Mano a ~1,3 px del mentón; el antebrazo izquierdo, a ~0,4 px del pecho (cinemática directa).
pensar = anim("pensar", 2.5, {
    "pra_right_arm": [rot(0), rot(0.5, 45, 20, 0), rot(2.0, 45, 20, 0), rot(2.5)],
    "prfa_right_forearm": [rot(0), rot(0.5, 95, 0, 0), rot(2.0, 95, 0, 0), rot(2.5)],
    "pla_left_arm": [rot(0), rot(0.5, 55, -30, 0), rot(2.0, 55, -30, 0), rot(2.5)],
    "plfa_left_forearm": [rot(0), rot(0.5, 60, 0, 0), rot(2.0, 60, 0, 0), rot(2.5)],
    "h_ph_head": [rot(0), rot(0.5, 8, 0, 8), rot(1.5, 10, -10, 8), rot(2.0, 8, 0, 8), rot(2.5)],
})

# ============================================ bailes ============================================

# --- robot: poses secas cada 0,4 s (0,1 s de transición, lineal), ida y vuelta ---
_P = [
    {"pra_right_arm": (90, 0, 0), "h_ph_head": (0, 30, 0)},
    {"pra_right_arm": (90, 0, 0), "prfa_right_forearm": (90, 0, 0), "pla_left_arm": (90, 0, 0)},
    {"pra_right_arm": (90, 0, 0), "prfa_right_forearm": (90, 0, 0), "pla_left_arm": (90, 0, 0),
     "plfa_left_forearm": (90, 0, 0), "h_ph_head": (0, -30, 0), "pc_chest": (0, 15, 0)},
    {"pra_right_arm": (0, 0, 90), "prfa_right_forearm": (90, 0, 0), "pla_left_arm": (0, 0, -90),
     "plfa_left_forearm": (90, 0, 0)},
]
_SECUENCIA = [0, 1, 2, 3, 2, 1, 0]
_huesos_robot = sorted({h for p in _P for h in p})
_robot = {h: [lineal(rot(0))] for h in _huesos_robot}
for k, indice in enumerate(_SECUENCIA):
    for h in _huesos_robot:
        x, y, z = _P[indice].get(h, (0, 0, 0))
        _robot[h] += [lineal(rot(0.1 + 0.4 * k, x, y, z)), lineal(rot(0.4 + 0.4 * k, x, y, z))]
for h in _huesos_robot:
    _robot[h] += [lineal(rot(2.9)), lineal(rot(3.0))]
robot = anim("robot", 3.0, _robot)

# --- saltitos (bucle de 1 s): dos saltos; al caer flexiona las rodillas y los brazos suben en el aire ---
_caida = {"root": -0.5, "muslo": 15, "rodilla": -30, "brazo": 5}
_aire = {"root": 3, "muslo": 0, "rodilla": 0, "brazo": 25}
_tiempos = [(0, _caida), (0.25, _aire), (0.5, _caida), (0.75, _aire), (1.0, _caida)]
saltitos = anim("saltitos", 1.0, {
    "player_root": [pos(t, 0, f["root"], 0) for t, f in _tiempos],
    "prl_right_leg": [rot(t, f["muslo"]) for t, f in _tiempos],
    "pll_left_leg": [rot(t, f["muslo"]) for t, f in _tiempos],
    "prfl_right_foreleg": [rot(t, f["rodilla"]) for t, f in _tiempos],
    "plfl_left_foreleg": [rot(t, f["rodilla"]) for t, f in _tiempos],
    "pra_right_arm": [rot(t, 0, 0, f["brazo"]) for t, f in _tiempos],
    "pla_left_arm": [rot(t, 0, 0, -f["brazo"]) for t, f in _tiempos],
}, loop="loop")

# --- bailar_lento (bucle de 4 s): brazos como abrazando a la pareja y balanceo suave de lado a lado ---
bailar_lento = anim("bailar_lento", 4.0, {
    "pra_right_arm": [rot(0, 60, 0, 20), rot(2.0, 64, 0, 20), rot(4.0, 60, 0, 20)],
    "prfa_right_forearm": [rot(0, 60), rot(4.0, 60)],
    "pla_left_arm": [rot(0, 70, 0, -30), rot(2.0, 66, 0, -30), rot(4.0, 70, 0, -30)],
    "plfa_left_forearm": [rot(0, 40), rot(4.0, 40)],
    # El balanceo va en todo el cuerpo (player_root), no en la cadera: así las piernas la siguen y no se
    # abre un hueco entre la cadera y los muslos.
    "h_ph_head": [rot(0), rot(1.0, 0, 0, -4), rot(2.0), rot(3.0, 0, 0, 4), rot(4.0)],
    # El giro z baja un pie ~0,3 px: el cuerpo sube 0,35 px en esos tiempos para no pisar bajo el suelo.
    "player_root": [rot(0), rot(1.0, 0, 10, 3), rot(2.0), rot(3.0, 0, -10, -3), rot(4.0),
                    pos(0), pos(1.0, 0, 0.35, 0), pos(2.0), pos(3.0, 0, 0.35, 0), pos(4.0)],
    "prl_right_leg": [rot(0), rot(1.0, 10), rot(2.0), rot(3.0, -10), rot(4.0)],
    "pll_left_leg": [rot(0), rot(1.0, -10), rot(2.0), rot(3.0, 10), rot(4.0)],
}, loop="loop")

# ============================================ acción ============================================

# --- victoria: los dos brazos en V, dos puñetazos al cielo y la cabeza arriba ---
victoria = anim("victoria", 2.5, {
    "pra_right_arm": [rot(0), rot(0.4, 0, 0, 150), rot(0.7, 0, 0, 165), rot(1.0, 0, 0, 150), rot(1.3, 0, 0, 165),
                      rot(1.6, 0, 0, 150), rot(2.0, 0, 0, 150), rot(2.5)],
    "pla_left_arm": [rot(0), rot(0.4, 0, 0, -150), rot(0.7, 0, 0, -165), rot(1.0, 0, 0, -150),
                     rot(1.3, 0, 0, -165), rot(1.6, 0, 0, -150), rot(2.0, 0, 0, -150), rot(2.5)],
    "h_ph_head": [rot(0), rot(0.4, 15, 0, 0), rot(2.0, 15, 0, 0), rot(2.5)],
    "pc_chest": [rot(0), rot(0.4, 6, 0, 0), rot(2.0, 6, 0, 0), rot(2.5)],
})

# --- flexionar: doble bíceps (brazos a los lados, antebrazos arriba) con dos apretones ---
# x=90 lleva el brazo al frente y luego y=∓90 lo abre al costado: el antebrazo flexionado queda hacia arriba.
flexionar = anim("flexionar", 2.5, {
    "pra_right_arm": [rot(0), rot(0.5, 90, -90, 0), rot(2.0, 90, -90, 0), rot(2.5)],
    "pla_left_arm": [rot(0), rot(0.5, 90, 90, 0), rot(2.0, 90, 90, 0), rot(2.5)],
    "prfa_right_forearm": [rot(0), rot(0.5, 90), rot(1.0, 110), rot(1.3, 90), rot(1.6, 110), rot(2.0, 90),
                           rot(2.5)],
    "plfa_left_forearm": [rot(0), rot(0.5, 90), rot(1.0, 110), rot(1.3, 90), rot(1.6, 110), rot(2.0, 90),
                          rot(2.5)],
    "pc_chest": [rot(0), rot(0.5, 5, 0, 0), rot(2.0, 5, 0, 0), rot(2.5)],
    "h_ph_head": [rot(0), rot(0.5, 0, 20, 0), rot(1.3, 0, -20, 0), rot(2.0, 0, -20, 0), rot(2.5)],
})

# --- saltar_alegria: se agacha, salta con las rodillas recogidas y los brazos arriba, cae y celebra ---
saltar_alegria = anim("saltar_alegria", 2.0, {
    "player_root": [pos(0), pos(0.35, 0, -2, 0), pos(0.65, 0, 7, 0), pos(0.9, 0, 6, 0), pos(1.15, 0, -1.5, 0),
                    pos(1.45), pos(2.0)],
    "pc_chest": [rot(0), rot(0.35, -12, 0, 0), rot(0.65, 5, 0, 0), rot(1.15, -8, 0, 0), rot(1.45), rot(2.0)],
    "h_ph_head": [rot(0), rot(0.35, -10, 0, 0), rot(0.65, 15, 0, 0), rot(1.15), rot(2.0)],
    "pra_right_arm": [rot(0), rot(0.35, -35, 0, 0), rot(0.65, 0, 0, 160), rot(0.9, 0, 0, 165),
                      rot(1.15, 0, 0, 120), rot(1.6, 0, 0, 140), rot(2.0)],
    "pla_left_arm": [rot(0), rot(0.35, -35, 0, 0), rot(0.65, 0, 0, -160), rot(0.9, 0, 0, -165),
                     rot(1.15, 0, 0, -120), rot(1.6, 0, 0, -140), rot(2.0)],
    "prl_right_leg": [rot(0), rot(0.35, 35), rot(0.65, 45), rot(0.9, 30), rot(1.15, 30), rot(1.45), rot(2.0)],
    "pll_left_leg": [rot(0), rot(0.35, 35), rot(0.65, 45), rot(0.9, 30), rot(1.15, 30), rot(1.45), rot(2.0)],
    "prfl_right_foreleg": [rot(0), rot(0.35, -60), rot(0.65, -80), rot(0.9, -50), rot(1.15, -50), rot(1.45),
                           rot(2.0)],
    "plfl_left_foreleg": [rot(0), rot(0.35, -60), rot(0.65, -80), rot(0.9, -50), rot(1.15, -50), rot(1.45),
                          rot(2.0)],
})

# --- sentarse («hold»): baja al suelo en 0,5 s y se queda sentado, piernas al frente y manos en los muslos ---
# «hold»: BetterModel la reproduce una vez y mantiene la última pose (el motor usa HOLD_ON_LAST) hasta que se
# corta, así la bajada no se repite. Medido con cinemática directa: talones a 0,05 px del suelo, muslos a 0,7,
# manos a 0,2 px sobre los muslos.
_sentado = 0.5
sentarse = anim("sentarse", 0.6, {
    "player_root": [pos(0), pos(_sentado, 0, -8.3, 0), pos(0.6, 0, -8.3, 0)],
    "prl_right_leg": [rot(0), rot(_sentado, 88, 0, 5), rot(0.6, 88, 0, 5)],
    "pll_left_leg": [rot(0), rot(_sentado, 88, 0, -5), rot(0.6, 88, 0, -5)],
    "prfl_right_foreleg": [rot(0), rot(_sentado, -5), rot(0.6, -5)],
    "plfl_left_foreleg": [rot(0), rot(_sentado, -5), rot(0.6, -5)],
    "pra_right_arm": [rot(0), rot(_sentado, -5, 5, 0), rot(0.6, -5, 5, 0)],
    "pla_left_arm": [rot(0), rot(_sentado, -5, -5, 0), rot(0.6, -5, -5, 0)],
    "prfa_right_forearm": [rot(0), rot(_sentado, 80), rot(0.6, 80)],
    "plfa_left_forearm": [rot(0), rot(_sentado, 80), rot(0.6, 80)],
    "h_ph_head": [rot(0), rot(_sentado, -5, 0, 0), rot(0.6, -5, 0, 0)],
}, loop="hold")

# --- meditar (bucle de 4 s): sentado sobre los talones, manos en los muslos, respiración lenta ---
# Muslos a 70° y espinillas a -160° (rodilla muy doblada pero dentro de lo natural): las espinillas quedan
# tumbadas en el suelo y las rodillas apoyadas (0,0-0,1 px, cinemática directa). El cuerpo no sube con la
# respiración, que va en el pecho, para que las rodillas no se despeguen.
_quieto = (0, 4.0)
meditar = anim("meditar", 4.0, {
    "player_root": [pos(t, 0, -7.35, 0) for t in _quieto],
    "prl_right_leg": [rot(t, 70, 0, 3) for t in _quieto],
    "pll_left_leg": [rot(t, 70, 0, -3) for t in _quieto],
    "prfl_right_foreleg": [rot(t, -160) for t in _quieto],
    "plfl_left_foreleg": [rot(t, -160) for t in _quieto],
    "pc_chest": [rot(0), rot(2.0, 2, 0, 0), rot(4.0)],
    "pra_right_arm": [rot(t, -10, 35, 0) for t in _quieto],
    "pla_left_arm": [rot(t, -10, -35, 0) for t in _quieto],
    "prfa_right_forearm": [rot(t, 45) for t in _quieto],
    "plfa_left_forearm": [rot(t, 45) for t in _quieto],
    "h_ph_head": [rot(t, -8) for t in _quieto],
}, loop="loop")

# ============================================= magia =============================================

# --- conjurar_caminando: arriba, conjurar; abajo, un paso (piernas ±30° en 0,8 s) para no congelarse al andar ---
conjurar_caminando = anim("conjurar_caminando", 0.8, {
    **CONJURAR,
    "prl_right_leg": [rot(0), rot(0.2, 30), rot(0.6, -30), rot(0.8)],
    "pll_left_leg": [rot(0), rot(0.2, -30), rot(0.6, 30), rot(0.8)],
    # La rodilla de la pierna que va atrás se dobla más, como al despegar el pie.
    "prfl_right_foreleg": [rot(0), rot(0.2, -10), rot(0.6, -35), rot(0.8)],
    "plfl_left_foreleg": [rot(0), rot(0.2, -35), rot(0.6, -10), rot(0.8)],
    "phip_hip": [rot(0), rot(0.2, 0, -4, 0), rot(0.6, 0, 4, 0), rot(0.8)],
    "player_root": [pos(0), pos(0.2, 0, -0.5, 0), pos(0.4), pos(0.6, 0, -0.5, 0), pos(0.8)],
})

# --- canalizar (bucle de 1,6 s): las dos manos al frente a la altura del pecho, suben y bajan; cabeza baja ---
canalizar = anim("canalizar", 1.6, {
    "pra_right_arm": [rot(0, 60, 15, 0), rot(0.8, 68, 15, 0), rot(1.6, 60, 15, 0)],
    "pla_left_arm": [rot(0, 60, -15, 0), rot(0.8, 68, -15, 0), rot(1.6, 60, -15, 0)],
    "prfa_right_forearm": [rot(0, 40), rot(0.8, 35), rot(1.6, 40)],
    "plfa_left_forearm": [rot(0, 40), rot(0.8, 35), rot(1.6, 40)],
    "h_ph_head": [rot(0, -10), rot(0.8, -12), rot(1.6, -10)],
    "pc_chest": [rot(t, -3) for t in (0, 1.6)],
}, loop="loop")

# ============================================ catálogo ============================================


def sonido(clave, volumen, tono):
    return {"id": clave, "volume": volumen, "pitch": tono}


def particulas(tipo, cantidad):
    return {"type": tipo, "count": cantidad}


# (animación, categoría, icono, rareza, desbloqueo, sonido, partículas)
EMOTES = [
    (saludar, "social", "FEATHER", "common", "free", None, None),
    (aplaudir, "social", "WHITE_WOOL", "common", "free", sonido("minecraft:block.wool.place", 0.6, 1.3), None),
    (reverencia, "social", "NAME_TAG", "common", "free", None, None),
    (asentir, "social", "LIME_DYE", "common", "free", None, None),
    (negar, "social", "RED_DYE", "common", "free", None, None),
    (senalar, "social", "ARROW", "common", "free", None, None),
    (encogerse, "social", "LEATHER", "common", "free", None, None),
    (facepalm, "social", "SLIME_BALL", "uncommon", "free",
     sonido("minecraft:entity.player.attack.nodamage", 0.5, 1.2), None),
    (reir, "social", "SUNFLOWER", "uncommon", "free", sonido("minecraft:entity.witch.celebrate", 0.5, 1.3),
     particulas("NOTE", 5)),
    (llorar, "social", "GHAST_TEAR", "uncommon", "free", None, particulas("FALLING_WATER", 8)),
    (pensar, "social", "WRITABLE_BOOK", "common", "free", None, particulas("ENCHANT", 8)),
    (bailar, "baile", "JUKEBOX", "common", "free", None, particulas("NOTE", 4)),
    (robot, "baile", "COMPARATOR", "rare", "locked", sonido("minecraft:block.piston.extend", 0.4, 1.6), None),
    (saltitos, "baile", "RABBIT_FOOT", "common", "free", None, None),
    (bailar_lento, "baile", "MUSIC_DISC_CAT", "rare", "locked", None, particulas("HEART", 3)),
    (victoria, "accion", "GOLDEN_SWORD", "uncommon", "free", sonido("minecraft:entity.player.levelup", 0.5, 1.2),
     particulas("TOTEM_OF_UNDYING", 12)),
    (flexionar, "accion", "IRON_CHESTPLATE", "rare", "locked", sonido("minecraft:item.armor.equip_iron", 0.7, 0.9),
     particulas("CRIT", 8)),
    (saltar_alegria, "accion", "FIREWORK_ROCKET", "uncommon", "free",
     sonido("minecraft:entity.firework_rocket.twinkle", 0.4, 1.2), particulas("FIREWORK", 10)),
    (sentarse, "accion", "OAK_STAIRS", "common", "free", None, None),
    (meditar, "accion", "AMETHYST_SHARD", "epic", "locked", sonido("minecraft:block.amethyst_block.chime", 0.6, 1.0),
     particulas("ENCHANT", 10)),
    (conjurar, "magia", "BLAZE_POWDER", "common", "free", None, None),
    (conjurar_caminando, "magia", "BLAZE_POWDER", "common", "free", None, None),
    (canalizar, "magia", "AMETHYST_CLUSTER", "common", "free", None, None),
]

# ===================================== packs temáticos (--pack) =====================================
# Van después de los de fábrica a propósito: así los uuid del modelo de fábrica no cambian.

# --- pirata_hornpipe (bucle de 2 s): baile marinero; patadas al frente alternas con saltito y brazos al pecho ---
# Tiempos de 0,5 s: en cada uno cae flexionando las rodillas y entre medias salta con una pierna al frente.
# Brazos x50/y±30 y antebrazos a 80: las manos quedan juntas delante del pecho, a ±1,3 px del centro, y los
# codos 1,6 px por delante del pecho (cinemática directa).
_cae = {"root": -0.6, "md": 10, "rd": -20, "mi": 10, "ri": -20, "pecho": 0, "cabeza": 0}
_patada_d = {"root": 1.2, "md": 45, "rd": -5, "mi": -5, "ri": -10, "pecho": -8, "cabeza": 6}
_patada_i = {"root": 1.2, "md": -5, "rd": -10, "mi": 45, "ri": -5, "pecho": 8, "cabeza": -6}
_hornpipe = [(0, _cae), (0.25, _patada_d), (0.5, _cae), (0.75, _patada_i), (1.0, _cae), (1.25, _patada_d),
             (1.5, _cae), (1.75, _patada_i), (2.0, _cae)]
pirata_hornpipe = anim("pirata_hornpipe", 2.0, {
    "player_root": [pos(t, 0, f["root"], 0) for t, f in _hornpipe],
    "prl_right_leg": [rot(t, f["md"]) for t, f in _hornpipe],
    "prfl_right_foreleg": [rot(t, f["rd"]) for t, f in _hornpipe],
    "pll_left_leg": [rot(t, f["mi"]) for t, f in _hornpipe],
    "plfl_left_foreleg": [rot(t, f["ri"]) for t, f in _hornpipe],
    "pc_chest": [rot(t, 0, f["pecho"], 0) for t, f in _hornpipe],
    "h_ph_head": [rot(t, 0, 0, f["cabeza"]) for t, f in _hornpipe],
    "pra_right_arm": [rot(t, 50, 30, 0) for t in (0, 2.0)],
    "pla_left_arm": [rot(t, 50, -30, 0) for t in (0, 2.0)],
    "prfa_right_forearm": [rot(t, 80) for t in (0, 2.0)],
    "plfa_left_forearm": [rot(t, 80) for t in (0, 2.0)],
}, loop="loop")

# --- mareas_catalejo: un catalejo imaginario al ojo derecho; el pecho barre el horizonte de un lado a otro ---
# Mano derecha (x82/y15, antebrazo 78) junto al ojo derecho; la izquierda (x100/y-35, antebrazo 20) sujeta el
# otro extremo 1,5 px más adelante (cinemática directa). Brazos y cabeza cuelgan del pecho: al girarlo, el
# catalejo sigue pegado al ojo.
_vista = [rot(0), rot(0.5, -5, 0, 0), rot(1.2, -5, 20, 0), rot(1.9, -5, -20, 0), rot(2.4, -5, 0, 0), rot(3.0)]
mareas_catalejo = anim("mareas_catalejo", 3.0, {
    "pc_chest": _vista,
    "pra_right_arm": [rot(0), rot(0.5, 82, 15, 0), rot(2.4, 82, 15, 0), rot(3.0)],
    "prfa_right_forearm": [rot(0), rot(0.5, 78), rot(2.4, 78), rot(3.0)],
    "pla_left_arm": [rot(0), rot(0.5, 100, -35, 0), rot(2.4, 100, -35, 0), rot(3.0)],
    "plfa_left_forearm": [rot(0), rot(0.5, 20), rot(2.4, 20), rot(3.0)],
    "h_ph_head": [rot(0), rot(0.5, 5, 0, 0), rot(2.4, 5, 0, 0), rot(3.0)],
})

# --- jade_saludo_marcial: puño contra la palma delante del pecho y una inclinación corta ---
# Brazos x35/y±33 y antebrazos a 70: las manos se juntan a ±0,7 px del centro (como en aplaudir) a la altura
# del pecho (cinemática directa). Durante la inclinación las manos se adelantan un poco (x45).
jade_saludo_marcial = anim("jade_saludo_marcial", 2.2, {
    "pra_right_arm": [rot(0), rot(0.4, 35, 33, 0), rot(0.8, 45, 33, 0), rot(1.5, 45, 33, 0), rot(1.8, 35, 33, 0),
                      rot(2.2)],
    "pla_left_arm": [rot(0), rot(0.4, 35, -33, 0), rot(0.8, 45, -33, 0), rot(1.5, 45, -33, 0),
                     rot(1.8, 35, -33, 0), rot(2.2)],
    "prfa_right_forearm": [rot(0), rot(0.4, 70), rot(1.8, 70), rot(2.2)],
    "plfa_left_forearm": [rot(0), rot(0.4, 70), rot(1.8, 70), rot(2.2)],
    "pw_waist": [rot(0), rot(0.4), rot(0.8, -15, 0, 0), rot(1.5, -15, 0, 0), rot(1.8), rot(2.2)],
    "h_ph_head": [rot(0), rot(0.4), rot(0.8, -10, 0, 0), rot(1.5, -10, 0, 0), rot(1.8), rot(2.2)],
})

# --- jade_postura_grulla (bucle de 3 s): sobre la pierna izquierda, rodilla derecha arriba y brazos en alas ---
# Muslo derecho a 80 y espinilla a -85: el pie cuelga bajo la rodilla. Las alas suben y bajan despacio
# (z 95-110) y el pecho respira; la pierna de apoyo no se mueve, así el pie no se despega del suelo.
jade_postura_grulla = anim("jade_postura_grulla", 3.0, {
    "prl_right_leg": [rot(0, 80), rot(1.5, 84), rot(3.0, 80)],
    "prfl_right_foreleg": [rot(t, -85) for t in (0, 3.0)],
    "pra_right_arm": [rot(0, 0, 0, 95), rot(1.5, 0, 0, 110), rot(3.0, 0, 0, 95)],
    "pla_left_arm": [rot(0, 0, 0, -95), rot(1.5, 0, 0, -110), rot(3.0, 0, 0, -95)],
    "prfa_right_forearm": [rot(0, 20), rot(1.5, 30), rot(3.0, 20)],
    "plfa_left_forearm": [rot(0, 20), rot(1.5, 30), rot(3.0, 20)],
    "pc_chest": [rot(0), rot(1.5, 3, 0, 0), rot(3.0)],
    "h_ph_head": [rot(t, 5) for t in (0, 3.0)],
}, loop="loop")

# pack -> [(animación, categoría, icono, rareza, nombre, cómo conseguirlo, partículas)]
# Todos «locked»: los da una misión del pack con «emotesadmin give {player} <id>».
PACKS = {
    "mareas-malditas": [
        (pirata_hornpipe, "baile", "NAUTILUS_SHELL", "rare", "&3Hornpipe Pirata",
         "Completa la misión «La Flota del Capitán»", particulas("NOTE", 4)),
        (mareas_catalejo, "social", "SPYGLASS", "uncommon", "&3Catalejo",
         "Completa la misión «La Cartógrafa»", None),
    ],
    "dinastia-de-jade": [
        (jade_saludo_marcial, "social", "BAMBOO", "rare", "&aSaludo Marcial",
         "Completa la misión «El General Sin Sombra»", None),
        (jade_postura_grulla, "accion", "FEATHER", "rare", "&aPostura de la Grulla",
         "Completa la misión «Las Grullas de Tinta»", None),
    ],
}


def modelo_de_pack(pack):
    """El limb key de BetterModel es el nombre del archivo: mareas-malditas -> mareas_malditas_emotes."""
    return pack.replace("-", "_") + "_emotes"


ANTEBRAZOS = ("prfa_right_forearm", "plfa_left_forearm")
RODILLAS = ("prfl_right_foreleg", "plfl_left_foreleg")
# Prototipo probado en el juego que se conserva tal cual aunque dobla la rodilla hacia +x.
RODILLA_EXENTA = {"bailar"}


# Del prototipo probado en el juego: no se tocan. conjurar_caminando comparte las pistas de conjurar y se
# deja igual para que la parte de arriba sea idéntica.
PROTOTIPO = {"saludar", "bailar", "aplaudir", "conjurar", "conjurar_caminando"}


def mesetas_lineales(a):
    """Dos claves seguidas iguales (una pose mantenida) pasan a lineal: catmullrom se pasaba ~12 % en la espera."""
    for animador in a["animators"].values():
        for canal in ("rotation", "position"):
            claves = sorted((k for k in animador["keyframes"] if k["channel"] == canal), key=lambda k: k["time"])
            for k0, k1 in zip(claves, claves[1:]):
                if k0["data_points"] == k1["data_points"]:
                    k0["interpolation"] = k1["interpolation"] = "linear"


def comprobar(a):
    """Autocomprobación: tiempos, poses de inicio y fin, y codos y rodillas hacia su lado natural."""
    nombre, largo, bucle = a["name"], a["length"], a["loop"] == "loop"
    mantiene = a["loop"] == "hold"  # empieza en la pose neutra y se queda en la última
    for animador in a["animators"].values():
        for canal in ("rotation", "position"):
            claves = sorted((k for k in animador["keyframes"] if k["channel"] == canal), key=lambda k: k["time"])
            if not claves:
                continue
            valores = [tuple(float(k["data_points"][0][e]) for e in "xyz") for k in claves]
            donde = f"{nombre}/{animador['name']}/{canal}"
            assert all(0 <= k["time"] <= largo for k in claves), f"{donde}: clave fuera de 0..{largo}"
            assert claves[0]["time"] == 0 and claves[-1]["time"] == largo, f"{donde}: no cubre de 0 a {largo}"
            if bucle:
                assert valores[0] == valores[-1], f"{donde}: el bucle no acaba en la pose del inicio"
            elif mantiene:
                assert valores[0] == (0, 0, 0), f"{donde}: no empieza en la pose neutra"
            else:
                assert valores[0] == valores[-1] == (0, 0, 0), f"{donde}: no empieza y acaba en la pose neutra"
            if canal == "rotation" and animador["name"] in ANTEBRAZOS:
                assert all(v[0] >= 0 for v in valores), f"{donde}: el codo se dobla hacia atrás"
            if canal == "rotation" and animador["name"] in RODILLAS and nombre not in RODILLA_EXENTA:
                assert all(v[0] <= 0 for v in valores), f"{donde}: la rodilla se dobla hacia delante"


def yaml_emote(a, categoria, icono, rareza, desbloqueo, snd, part):
    lineas = [
        "# De fábrica: generado por tools/emotes/construir_emotes.py.",
        f"id: {a['name']}",
        f'name: "lang:emotes.{a["name"]}"',
        f"icon: {icono}",
        f"category: {categoria}",
        f"animation: {a['name']}",
        f"model: {MODELO}",
        f"loop: {'true' if a['loop'] in ('loop', 'hold') else 'false'}",
        f"unlock: {desbloqueo}",
        f"rarity: {rareza}",
    ]
    if snd:
        lineas.append(f'sound: {{ id: "{snd["id"]}", volume: {snd["volume"]}, pitch: {snd["pitch"]} }}')
    if part:
        lineas.append(f"particles: {{ type: {part['type']}, count: {part['count']} }}")
    # Los de magia no salen en el menú: los lanza RPGRoll-Magic.
    lineas.append(f"hidden: {'true' if categoria == 'magia' else 'false'}")
    return "\n".join(lineas) + "\n"


def yaml_emote_pack(a, pack, categoria, icono, rareza, nombre, como, part):
    lineas = [
        f"# Pack {pack}: generado por tools/emotes/construir_emotes.py --pack {pack}.",
        f"id: {a['name']}",
        f'name: "{nombre}"',
        f"icon: {icono}",
        f"category: {categoria}",
        f"animation: {a['name']}",
        f"model: {modelo_de_pack(pack)}",
        f"loop: {'true' if a['loop'] in ('loop', 'hold') else 'false'}",
        "unlock: locked",
        f"rarity: {rareza}",
        f'how-to-unlock: "{como}"',
    ]
    if part:
        lineas.append(f"particles: {{ type: {part['type']}, count: {part['count']} }}")
    lineas.append("hidden: false")
    return "\n".join(lineas) + "\n"


def escribir_modelo(nombre, animaciones, destino):
    modelo = copy.deepcopy(base)
    modelo["name"] = nombre
    modelo["model_identifier"] = nombre
    modelo["animations"] = animaciones
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(json.dumps(modelo, ensure_ascii=False), encoding="utf-8")


def construir_pack(pack, salida):
    """packs/<pack>/RPGRoll-Emotes/: animations/<modelo>.bbmodel y emotes/<id>.yml."""
    emotes = PACKS[pack]
    animaciones = [e[0] for e in emotes]
    for a in animaciones:
        mesetas_lineales(a)
        comprobar(a)
    nombre = modelo_de_pack(pack)
    assert nombre != MODELO, f"{MODELO} es el nombre reservado del modelo de fábrica"
    escribir_modelo(nombre, animaciones, salida / "animations" / f"{nombre}.bbmodel")
    for a, *resto in emotes:
        archivo = salida / "emotes" / f"{a['name']}.yml"
        archivo.parent.mkdir(parents=True, exist_ok=True)
        archivo.write_text(yaml_emote_pack(a, pack, *resto), encoding="utf-8", newline="\n")
    print(f"{nombre}.bbmodel: " + ", ".join(a["name"] for a in animaciones))


def main():
    parser = argparse.ArgumentParser(description="Genera las animaciones y el catálogo de fábrica de Emotes.")
    parser.add_argument("--salida", type=Path, default=None)
    parser.add_argument("--pack", choices=sorted(PACKS), help="genera los emotes de un pack temático")
    args = parser.parse_args()
    if args.pack:
        construir_pack(args.pack, args.salida or RAIZ / "packs" / args.pack / "RPGRoll-Emotes")
        return
    salida = args.salida or RAIZ / "emotes" / "src" / "main" / "resources"

    animaciones = [e[0] for e in EMOTES]
    assert len({a["name"] for a in animaciones}) == len(animaciones) == 23, "se esperan 23 animaciones distintas"
    for a in animaciones:
        if a["name"] not in PROTOTIPO:
            mesetas_lineales(a)
        comprobar(a)

    destino = salida / "animations" / f"{MODELO}.bbmodel"
    escribir_modelo(MODELO, animaciones, destino)

    for a, categoria, *resto in EMOTES:
        archivo = salida / "emotes" / categoria / f"{a['name']}.yml"
        archivo.parent.mkdir(parents=True, exist_ok=True)
        archivo.write_text(yaml_emote(a, categoria, *resto), encoding="utf-8", newline="\n")

    print(f"{destino.name}: {len(animaciones)} animaciones")
    print(", ".join(a["name"] for a in animaciones))


if __name__ == "__main__":
    main()
