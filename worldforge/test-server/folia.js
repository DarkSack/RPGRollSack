// Lo que en Folia solo puede hacer un jugador (la consola no toca bloques) y una pasada por el menú.
// Lo lanza escenario-folia.sh:
//   node folia.js marca   → pone un bloque de oro en 165 64 165 (mundo principal)
//   node folia.js gui     → recorre menús; lo que Folia no permite debe decirlo, no romperse
//   node folia.js mira    → comprueba que la marca ya no está (regenerada al arrancar)
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
const texto = x => JSON.stringify(x ?? '')
const deItem = it => it ? texto(it.customName) + texto(it.customLore) : ''
const titulo = w => texto(w?.title)

async function entrar () {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: 'Admin', version: '26.1', auth: 'offline' })
  bot.chatLog = []
  bot.on('messagestr', m => bot.chatLog.push(m))
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('kicked', reject); bot.once('error', reject) })
  consola('op Admin')
  await sleep(1500)
  bot.chat('/gamemode creative')
  await sleep(1000)
  bot.creative.startFlying()
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

function ventana (bot, ms = 6000) {
  return new Promise(resolve => {
    const t = setTimeout(() => { bot.removeListener('windowOpen', on); resolve(null) }, ms)
    const on = w => { clearTimeout(t); resolve(w) }
    bot.once('windowOpen', on)
  })
}

async function abrir (bot) {
  const w = ventana(bot)
  bot.chat('/wf gui')
  return w
}

async function clic (bot, w, que, esperaVentana = true) {
  let slot = que
  if (typeof que === 'string') {
    slot = -1
    for (let i = 0; i < w.inventoryStart; i++) if (deItem(w.slots[i]).includes(que)) { slot = i; break }
    if (slot < 0) return { w: null }
  }
  bot.chatLog.length = 0
  const siguiente = esperaVentana ? ventana(bot) : Promise.resolve(null)
  await bot.clickWindow(slot, 0, 0).catch(() => {})
  return { w: await siguiente }
}

const nombre = (bot, x, y, z) => bot.blockAt(new Vec3(x, y, z))?.name ?? 'sin cargar'

async function ir (bot, x, y, z) {
  bot.chat(`/tp @s ${x} ${y} ${z}`)
  for (let i = 0; i < 40 && bot.entity.position.distanceTo(new Vec3(x, y, z)) > 3; i++) await sleep(250)
  await sleep(4000)
}

async function marca () {
  const bot = await entrar()
  await ir(bot, 165, 66, 165)
  bot.chatLog.length = 0
  bot.chat('/setblock 165 64 165 minecraft:gold_block')
  ok(await chatCon(bot, /Changed the block/), 'el jugador pone la marca en su región', bot.chatLog.join(' | '))
  await sleep(1000)
  ok(nombre(bot, 165, 64, 165) === 'gold_block', 'la marca está', nombre(bot, 165, 64, 165))
  bot.quit()
}

async function gui () {
  const bot = await entrar()
  let w = await abrir(bot)
  ok(w && titulo(w).includes('WorldForge'), 'Folia: abre el menú principal', titulo(w))
  let r = await clic(bot, w, 11)
  ok(r.w && titulo(r.w).includes('Presets'), 'Folia: lista de presets', titulo(r.w))
  r = await clic(bot, r.w, '"value":"flat"') // el nombre exacto: «flat» sale también en la descripción de otros
  ok(r.w && titulo(r.w).includes('Preset flat'), 'Folia: abre un preset', titulo(r.w))
  await clic(bot, r.w, 10, false)
  ok(await chatCon(bot, /es válido/), 'Folia: validar desde el menú', bot.chatLog.join(' | '))
  bot.closeWindow(bot.currentWindow ?? r.w)

  w = await abrir(bot)
  r = await clic(bot, w, 10)
  ok(r.w && titulo(r.w).includes('Mundos'), 'Folia: lista de mundos', titulo(r.w))
  r = await clic(bot, r.w, 'world')
  ok(r.w && titulo(r.w).includes('Mundo world'), 'Folia: abre el mundo principal', titulo(r.w))
  await clic(bot, r.w, 11, false)
  ok(await chatCon(bot, /Generador: WorldForgeGenerator/), 'Folia: inspeccionar desde el menú', bot.chatLog.join(' | '))
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow)

  w = await abrir(bot)
  await clic(bot, w, 14, false)
  ok(await chatCon(bot, /Plataforma: Folia/), 'Folia: diagnóstico desde el menú', bot.chatLog.join(' | '))
  bot.quit()
}

async function mira () {
  const bot = await entrar()
  await ir(bot, 165, 66, 165)
  ok(nombre(bot, 165, 64, 165) === 'air', 'la marca del principal desapareció', nombre(bot, 165, 64, 165))
  await ir(bot, 0, 66, 0)
  ok(nombre(bot, 0, 64, 0) === 'stone', 'la plataforma de aparición sigue', nombre(bot, 0, 64, 0))
  bot.quit()
}

const fases = { marca, gui, mira }
;(fases[process.argv[2]] ?? (() => Promise.reject(new Error('fase desconocida'))))()
  .catch(e => { ok(false, 'el bot no pudo jugar', e.stack) })
  .then(() => process.exit(fallos ? 1 : 0))
