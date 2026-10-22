// Un jugador que se queda conectado (Visitante) para que haya alguien dentro del mundo que se
// regenera. Se va solo a los N segundos. Uso: node visitante.js 120
const mineflayer = require('mineflayer')

const segundos = Number(process.argv[2] || 120)
const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25602, username: 'Visitante', version: '26.1', auth: 'offline' })
bot.once('spawn', () => console.log(`WFBOT dentro en ${bot.game.dimension}`))
bot.on('respawn', () => console.log(`WFBOT cambió a ${bot.game.dimension}`))
bot.on('messagestr', m => console.log(`WFBOT chat: ${m}`))
bot.on('kicked', r => console.log(`WFBOT expulsado: ${JSON.stringify(r)}`))
bot.on('error', e => console.log(`WFBOT error: ${e.message}`))
setTimeout(() => { bot.quit(); setTimeout(() => process.exit(0), 500) }, segundos * 1000)
