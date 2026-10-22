// Un jugador sin op (Visitante) contra wf-test: permisos por subcomando y autocompletado.
// NODE_PATH = E:/Dev/SackitoNetwork/e2e/bot/node_modules
const mineflayer = require('mineflayer')
const { execFileSync } = require('child_process')

const sleep = ms => new Promise(r => setTimeout(r, ms))
const consola = cmd => execFileSync('docker', ['exec', 'wf-test', 'sh', '-c', 'printf "%s\n" "$1" > /tmp/console', 'sh', cmd])
const chat = []
let fallos = 0
const ok = (cond, nombre, detalle) => {
  console.log(`WFTEST ${cond ? 'PASS' : 'FAIL'} ${nombre} :: ${detalle}`)
  if (!cond) fallos++
}

async function main () {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: 'Visitante', version: '26.1', auth: 'offline' })
  await new Promise((resolve, reject) => { bot.once('spawn', resolve); bot.once('kicked', reject); bot.once('error', reject) })
  bot.on('messagestr', m => chat.push(m))

  const paso = async (nombre, cmd, re) => {
    chat.length = 0
    bot.chat(cmd)
    for (let t = 0; t < 50; t++) {
      const m = chat.find(x => re.test(x))
      if (m) return ok(true, nombre, m)
      await sleep(100)
    }
    ok(false, nombre, `esperaba ${re}; llegó: ${chat.join(' | ') || 'nada'}`)
  }

  await paso('sin permisos no ve los mundos', '/wf world list', /No tienes permiso: worldforge\.worlds\.view/)

  consola('lp user Visitante permission set worldforge.viewer true')
  await sleep(2000)
  await paso('viewer ve los mundos', '/wf world list', /Mundos/)
  await paso('viewer ve el diagnóstico', '/wf diagnostics', /SQLite: esquema/)
  await paso('viewer no crea mundos', '/wf world create t_bot empty', /No tienes permiso: worldforge\.worlds\.create/)
  await paso('viewer no edita presets', '/wf preset duplicate flat bot-flat', /No tienes permiso: worldforge\.presets\.edit/)
  const tab = await bot.tabComplete('/wf world ', true, false, 5000).catch(e => [{ match: 'ERROR ' + e.message }])
  const nombres = tab.map(x => x.match ?? x)
  ok(nombres.includes('list') && !nombres.includes('create'), 'el autocompletado solo ofrece lo permitido', JSON.stringify(nombres))

  consola('lp user Visitante permission set worldforge.operator true')
  await sleep(2000)
  await paso('operator edita borradores', '/wf preset duplicate flat bot-flat', /Borrador bot-flat creado/)
  await paso('operator no importa', '/wf preset import algo.yml', /No tienes permiso: worldforge\.presets\.import/)
  await paso('operator no aplica generación', '/wf generation apply t_flat flat', /No tienes permiso: worldforge\.generation\.apply/)

  console.log(`FIN jugador: ${fallos} fallo(s)`)
  bot.quit()
  await sleep(500)
  process.exit(fallos ? 1 : 0)
}

main().catch(e => { console.log(`WFTEST FAIL el bot no pudo jugar :: ${e.stack || e}`); process.exit(2) })
