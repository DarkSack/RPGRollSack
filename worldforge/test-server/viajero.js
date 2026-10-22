// Un jugador que solo está conectado: escenario-mv.sh lo mueve entre mundos desde la consola y mira
// sus atributos. Se queda hasta recibir SIGTERM; reaparece solo si muere.
// NODE_PATH = E:/Dev/SackitoNetwork/e2e/bot/node_modules
const mineflayer = require('mineflayer')
const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: 'Viajero', version: '26.1', auth: 'offline' })
bot.once('spawn', () => console.log('VIAJERO dentro'))
bot.on('death', () => console.log('VIAJERO murió'))
bot.on('kicked', r => { console.log('VIAJERO expulsado ' + JSON.stringify(r)); process.exit(1) })
bot.on('error', e => { console.log('VIAJERO error ' + e.message); process.exit(1) })
process.on('SIGTERM', () => { bot.quit(); setTimeout(() => process.exit(0), 500) })
