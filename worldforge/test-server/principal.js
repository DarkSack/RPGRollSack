// El mundo principal con plantillas y menas desde bukkit.yml, comprobado por un jugador (en Folia la
// consola no puede leer ni poner bloques: lo hace el jugador en su región). Lo lanza escenario-principal.sh.
//   node principal.js construir   → construye la plantilla junto a la aparición y la guarda
//   node principal.js comprobar   → va lejos, donde todo es nuevo, y mira lo generado
// NODE_PATH = E:/Dev/SackitoNetwork/e2e/bot/node_modules
const mineflayer = require('mineflayer')
const { Vec3 } = require('vec3')
const { execFileSync } = require('child_process')

const sleep = ms => new Promise(r => setTimeout(r, ms))
const consola = cmd => execFileSync('docker', ['exec', 'wf-test', 'sh', '-c', 'printf "%s\\n" "$1" > /tmp/console', 'sh', cmd])
let fallos = 0
const ok = (cond, nombre, detalle) => {
  console.log(`WFTEST ${cond ? 'PASS' : 'FAIL'} ${nombre} :: ${String(detalle).slice(0, 220)}`)
  if (!cond) fallos++
}

async function entrar () {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: 'Admin', version: '26.1', auth: 'offline' })
  bot.chatLog = []
  bot.on('messagestr', m => bot.chatLog.push(m))
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('kicked', reject); bot.once('error', reject) })
  consola('op Admin')
  await sleep(1500)
  bot.chat('/gamemode creative')
  await sleep(1000)
  return bot
}

async function chatCon (bot, re, ms = 10000) {
  for (let t = 0; t < ms / 100; t++) {
    const m = bot.chatLog.find(x => re.test(x))
    if (m) return m
    await sleep(100)
  }
  return null
}

async function orden (bot, cmd, re, ms) {
  bot.chatLog.length = 0
  bot.chat(cmd)
  return chatCon(bot, re, ms)
}

const nombre = (bot, x, y, z) => bot.blockAt(new Vec3(x, y, z))?.name ?? 'sin cargar'

async function construir () {
  const bot = await entrar()
  // La plantilla: 3×2×3 de ladrillo, oro en su esquina 0,0,0 y diamante en la 2,0,2.
  ok(await orden(bot, '/fill 3 66 3 5 67 5 minecraft:stone_bricks', /Successfully filled|filled/i), 'el jugador construye en su región', bot.chatLog.join(' | '))
  await orden(bot, '/setblock 3 66 3 minecraft:gold_block', /Changed the block/)
  ok(await orden(bot, '/setblock 5 66 5 minecraft:diamond_block', /Changed the block/), 'marcas puestas', bot.chatLog.join(' | '))
  ok(await orden(bot, '/wf structure save fol 3 66 3 5 67 5', /Plantilla fol guardada: 3×2×3/), 'guarda la plantilla desde el jugador', bot.chatLog.join(' | '))
  bot.quit()
}

async function comprobar () {
  const bot = await entrar()
  bot.creative.startFlying()
  bot.chat('/tp @s 1000 0 1000')
  await sleep(15000) // que se generen los chunks de alrededor
  const insp = await orden(bot, '/wf world inspect world', /Plantilla islas: \d+ colocadas/, 15000)
  const n = Number((insp ?? '').match(/islas: (\d+) colocadas/)?.[1] ?? 0)
  ok(n > 0, 'el mundo principal coloca plantillas en chunks nuevos', insp)
  await sleep(1500)
  const ultimas = bot.chatLog.filter(l => l.includes('última: islas')).map(l => l.trim().split(/\s+/))
  // "última: islas world x y z ROTACIÓN"
  let comprobadas = 0
  for (const u of ultimas.slice(0, 2)) {
    const [x, y, z] = [Number(u[3]), Number(u[4]), Number(u[5])]
    bot.chat(`/tp @s ${x} ${y + 3} ${z}`)
    await sleep(4000)
    const oro = nombre(bot, x, y, z)
    const dia = nombre(bot, x + 2, y, z + 2)
    ok(oro === 'gold_block' && dia === 'diamond_block' && u[6] === 'NONE', `plantilla en ${x} ${y} ${z}: oro y diamante donde tocan`, `${oro} / ${dia} / ${u[6]}`)
    comprobadas++
  }
  ok(comprobadas > 0, 'hay colocaciones que comprobar', ultimas.map(u => u.join(' ')).join(' | '))
  bot.chat('/tp @s 1000 0 1000')
  await sleep(5000)
  const menas = bot.findBlocks({ matching: bot.registry.blocksByName.diamond_ore.id, maxDistance: 64, count: 5000 }).length
  // Las capas del preset son piedra pura: cualquier diamante lo ha puesto WorldForge.
  ok(menas > 0, 'menas de WorldForge en la piedra del mundo principal', `${menas} bloques de diamante a la vista`)
  bot.quit()
}

const fase = process.argv[2]
;(fase === 'construir' ? construir() : comprobar())
  .catch(e => { ok(false, 'el bot no pudo jugar', e.stack); })
  .then(() => process.exit(fallos ? 1 : 0))
