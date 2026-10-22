// La GUI con dos jugadores reales contra wf-test: Admin (op) recorre los menús y hace cambios;
// Miron (solo worldforge.viewer) comprueba que cada clic pide su permiso. Lo lanza escenario-gui.sh.
// NODE_PATH = E:/Dev/SackitoNetwork/e2e/bot/node_modules
const mineflayer = require('mineflayer')
const { execFileSync } = require('child_process')

const sleep = ms => new Promise(r => setTimeout(r, ms))
const consola = cmd => execFileSync('docker', ['exec', 'wf-test', 'sh', '-c', 'printf "%s\\n" "$1" > /tmp/console', 'sh', cmd])
let fallos = 0
const ok = (cond, nombre, detalle) => {
  console.log(`WFTEST ${cond ? 'PASS' : 'FAIL'} ${nombre} :: ${String(detalle).slice(0, 220)}`)
  if (!cond) fallos++
}

function jugador (nombre) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: nombre, version: '26.1', auth: 'offline' })
  bot.chatLog = []
  bot.on('messagestr', m => bot.chatLog.push(m))
  return bot
}

const texto = x => JSON.stringify(x ?? '')
const deItem = it => it ? texto(it.customName) + texto(it.customLore) : ''
const titulo = w => texto(w?.title)

function ventana (bot, ms = 6000) {
  return new Promise(resolve => {
    const t = setTimeout(() => { bot.removeListener('windowOpen', on); resolve(null) }, ms)
    const on = w => { clearTimeout(t); resolve(w) }
    bot.once('windowOpen', on)
  })
}

async function chatCon (bot, re, ms = 8000) {
  for (let t = 0; t < ms / 100; t++) {
    const m = bot.chatLog.find(x => re.test(x))
    if (m) return m
    await sleep(100)
  }
  return null
}

async function abrir (bot) {
  const w = ventana(bot)
  bot.chat('/wf gui')
  return w
}

// Clic en el primer hueco del menú cuyo nombre o descripción contiene `que` (número = hueco exacto).
async function clic (bot, w, que, esperaVentana = true) {
  let slot = que
  if (typeof que === 'string') {
    slot = -1
    for (let i = 0; i < w.inventoryStart; i++) if (deItem(w.slots[i]).includes(que)) { slot = i; break }
    if (slot < 0) return { w: null, error: `no encuentro «${que}» en ${titulo(w)}` }
  }
  bot.chatLog.length = 0
  const siguiente = esperaVentana ? ventana(bot) : Promise.resolve(null)
  await bot.clickWindow(slot, 0, 0).catch(() => {})
  return { w: await siguiente, slot }
}

async function admin () {
  const bot = jugador('Admin')
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('kicked', reject); bot.once('error', reject) })
  consola('op Admin')
  await sleep(1500)

  let w = await abrir(bot)
  ok(w && titulo(w).includes('WorldForge') && deItem(w.slots[10]).includes('Mundos'), 'abre el menú principal', titulo(w))

  // presets: duplicar, semilla, cambio ajeno a la vez, publicar
  let r = await clic(bot, w, 11)
  ok(r.w && titulo(r.w).includes('Presets'), 'abre Presets', titulo(r.w))
  r = await clic(bot, r.w, 'flat')
  ok(r.w && titulo(r.w).includes('Preset flat'), 'abre el preset flat', titulo(r.w))
  r = await clic(bot, r.w, 11, false)
  ok(await chatCon(bot, /Id del duplicado/), 'duplicar pide el id por chat', bot.chatLog.join(' | '))
  let sig = ventana(bot)
  bot.chat('gui-flat')
  w = await sig
  ok(w && titulo(w).includes('Borrador gui-flat'), 'crea el borrador y lo abre', titulo(w))
  r = await clic(bot, w, 12, false)
  sig = ventana(bot)
  bot.chat('777')
  w = await sig
  ok(w && deItem(w.slots[4]).includes('Semilla 777'), 'fija la semilla', deItem(w?.slots[4]))
  consola('wf preset seed gui-flat 888')
  await sleep(1500)
  r = await clic(bot, w, 13, false)
  ok(await chatCon(bot, /El borrador cambió desde que abriste/), 'no publica lo que no has visto (otro lo cambió)', bot.chatLog.join(' | '))
  bot.closeWindow(w)
  w = await abrir(bot)
  r = await clic(bot, w, 11)
  r = await clic(bot, r.w, 'gui-flat')
  ok(r.w && deItem(r.w.slots[4]).includes('Semilla 888'), 'al reabrir ve el cambio ajeno', deItem(r.w?.slots[4]))
  await clic(bot, r.w, 13, false)
  ok(await chatCon(bot, /gui-flat publicado/), 'publica', bot.chatLog.join(' | '))

  // un menú abierto se cierra al recargar
  w = await abrir(bot)
  const cerrado = new Promise(resolve => { bot.once('windowClose', () => resolve(true)); setTimeout(() => resolve(false), 6000) })
  consola('wf reload-config')
  ok(await cerrado, 'recargar cierra los menús abiertos', '-')

  // mundos: regenerar alrededor de uno mismo
  w = await abrir(bot)
  r = await clic(bot, w, 10)
  r = await clic(bot, r.w, 't_gui')
  ok(r.w && titulo(r.w).includes('Mundo t_gui'), 'abre el mundo t_gui', titulo(r.w))
  await clic(bot, r.w, 14, false)
  ok(await chatCon(bot, /Entra en «t_gui»/), 'regenerar exige estar en ese mundo', bot.chatLog.join(' | '))
  bot.closeWindow(r.w)
  consola('execute in minecraft:t_gui run tp Admin 100 -59 100')
  await sleep(2500)
  w = await abrir(bot)
  r = await clic(bot, w, 10)
  r = await clic(bot, r.w, 't_gui')
  r = await clic(bot, r.w, 14)
  ok(r.w && titulo(r.w).includes('Regenerar: radio'), 'elige radio', titulo(r.w))
  r = await clic(bot, r.w, 11)
  ok(r.w && titulo(r.w).includes('Plan job-') && deItem(r.w.slots[4]).includes('1 chunks'), 'muestra el plan', deItem(r.w?.slots[4]))
  r = await clic(bot, r.w, 11)
  ok(r.w && titulo(r.w).includes('Seguro'), 'pide una segunda confirmación', titulo(r.w))
  await clic(bot, r.w, 11, false)
  ok(await chatCon(bot, /Confirmado: se descarga el mundo/), 'confirma', bot.chatLog.join(' | '))
  ok(await chatCon(bot, /job-[0-9a-f]{8}: 1 chunks regenerados/, 60000), 'regenera y avisa al terminar', bot.chatLog.join(' | '))

  // protección desde el menú
  consola('execute in minecraft:t_gui run tp Admin 100 -59 100')
  await sleep(2500)
  w = await abrir(bot)
  r = await clic(bot, w, 10)
  r = await clic(bot, r.w, 't_gui')
  r = await clic(bot, r.w, 15)
  r = await clic(bot, r.w, 12, false)
  ok(await chatCon(bot, /Nombre de la zona/), 'proteger pide el nombre', bot.chatLog.join(' | '))
  bot.chatLog.length = 0
  bot.chat('casa-gui')
  ok(await chatCon(bot, /Zona casa-gui protegida/), 'crea la zona', bot.chatLog.join(' | '))
  w = await abrir(bot)
  r = await clic(bot, w, 13)
  r = await clic(bot, r.w, 'casa-gui')
  r = await clic(bot, r.w, 11)
  ok(r.w && titulo(r.w).includes('Quitar casa-gui'), 'quitar pide confirmación', titulo(r.w))
  await clic(bot, r.w, 11, false)
  ok(await chatCon(bot, /casa-gui ya no está protegida/), 'quita la zona', bot.chatLog.join(' | '))

  w = await abrir(bot)
  await clic(bot, w, 14, false)
  ok(await chatCon(bot, /SQLite: esquema 2/), 'diagnóstico en el chat', bot.chatLog.join(' | '))
  bot.quit()
}

async function miron () {
  const bot = jugador('Miron')
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('kicked', reject); bot.once('error', reject) })
  consola('lp user Miron permission set worldforge.viewer true')
  await sleep(2000)
  let w = await abrir(bot)
  ok(w, 'un viewer abre el menú', titulo(w))
  let r = await clic(bot, w, 10)
  r = await clic(bot, r.w, 't_gui')
  await clic(bot, r.w, 10, false)
  ok(await chatCon(bot, /No tienes permiso: worldforge\.worlds\.manage/), 'cada clic pide su permiso (descargar)', bot.chatLog.join(' | '))
  await clic(bot, r.w, 11, false)
  ok(await chatCon(bot, /Clave minecraft:t_gui/), 'pero inspeccionar sí', bot.chatLog.join(' | '))
  w = await abrir(bot)
  r = await clic(bot, w, 12)
  r = await clic(bot, r.w, 'COMPLETED')
  ok(r.w && titulo(r.w).includes('Trabajo job-'), 've un trabajo', titulo(r.w))
  await clic(bot, r.w, 13, false)
  ok(await chatCon(bot, /No tienes permiso: worldforge\.regeneration\.plan/), 'no puede restaurar', bot.chatLog.join(' | '))
  bot.quit()
}

async function main () {
  await admin()
  await sleep(1000)
  await miron()
  console.log(`FIN gui: ${fallos} fallo(s)`)
  await sleep(500)
  process.exit(fallos ? 1 : 0)
}

main().catch(e => { console.log(`WFTEST FAIL el bot no pudo jugar :: ${e.stack || e}`); process.exit(2) })
