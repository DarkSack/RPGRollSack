#!/usr/bin/env node
'use strict';

// Servidor de licencias para las ventas propias de RPGRoll (Ko-fi, Patreon,
// transferencia). Implementa el contrato que espera SelfHostedLicenseProvider:
//
//   POST /verify   (application/x-www-form-urlencoded)  license=<clave>&resource=<producto>
//   -> { "valid": true,  "status": "active"  }
//   -> { "valid": false, "status": "revoked" | "unknown" | "not-covered", "message": "..." }
//
// Sin dependencias: solo módulos nativos de Node. Corre igual en un VPS, en
// Render/Railway/Fly, o detrás de nginx.
//
// Uso:
//   node server.js                      arranca el servidor (PORT, default 8080)
//   node server.js issue <comprador> [producto...]   emite una licencia nueva
//   node server.js revoke <clave> [motivo]           revoca una licencia
//   node server.js list                              lista todas las licencias
//
// Un producto "*" cubre todo el ecosistema. Si no se pasa ninguno, se asume "*".

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const DB_PATH = process.env.LICENSE_DB || path.join(__dirname, 'licenses.json');
const PORT = Number(process.env.PORT) || 8080;

function loadDb() {
    if (!fs.existsSync(DB_PATH)) {
        return { licenses: [] };
    }
    return JSON.parse(fs.readFileSync(DB_PATH, 'utf8'));
}

function saveDb(db) {
    // Escritura atómica: si el proceso muere a mitad, el archivo original
    // queda intacto en vez de truncado.
    const temporary = DB_PATH + '.tmp';
    fs.writeFileSync(temporary, JSON.stringify(db, null, 2) + '\n', 'utf8');
    fs.renameSync(temporary, DB_PATH);
}

function findLicense(db, key) {
    return db.licenses.find((license) => license.key === key) || null;
}

// Comparación en tiempo constante: sin esto, el tiempo de respuesta filtra
// cuántos caracteres iniciales de una clave son correctos.
function keysMatch(a, b) {
    const bufferA = Buffer.from(a, 'utf8');
    const bufferB = Buffer.from(b, 'utf8');
    return bufferA.length === bufferB.length && crypto.timingSafeEqual(bufferA, bufferB);
}

function lookup(db, key) {
    for (const license of db.licenses) {
        if (keysMatch(license.key, key)) {
            return license;
        }
    }
    return null;
}

function covers(license, resource) {
    if (!resource) {
        return true;
    }
    const products = license.products || ['*'];
    return products.includes('*') || products.includes(resource);
}

function verify(key, resource) {
    const license = lookup(loadDb(), key);

    if (!license) {
        return { valid: false, status: 'unknown', message: 'Licencia no encontrada.' };
    }

    if (license.revoked) {
        return {
            valid: false,
            status: 'revoked',
            message: license.revokedReason || 'Esta licencia fue revocada.',
        };
    }

    if (!covers(license, resource)) {
        return {
            valid: false,
            status: 'not-covered',
            message: `Tu licencia no incluye el producto "${resource}".`,
        };
    }

    return { valid: true, status: 'active', message: `Compra verificada (${license.buyer}).` };
}

// ---------------------------------------------------------------------------
// HTTP
// ---------------------------------------------------------------------------

function readBody(request) {
    return new Promise((resolve, reject) => {
        let body = '';
        request.on('data', (chunk) => {
            body += chunk;
            // Un cuerpo enorme solo puede ser abuso: cortamos temprano.
            if (body.length > 8192) {
                request.destroy();
                reject(new Error('cuerpo demasiado grande'));
            }
        });
        request.on('end', () => resolve(body));
        request.on('error', reject);
    });
}

function sendJson(response, statusCode, payload) {
    const body = JSON.stringify(payload);
    response.writeHead(statusCode, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
    });
    response.end(body);
}

function startServer() {
    const server = http.createServer(async (request, response) => {
        if (request.method !== 'POST' || !request.url.startsWith('/verify')) {
            return sendJson(response, 404, { error: 'not found' });
        }

        let body;
        try {
            body = await readBody(request);
        } catch {
            return sendJson(response, 413, { error: 'body too large' });
        }

        const params = new URLSearchParams(body);
        const key = params.get('license');
        const resource = params.get('resource');

        if (!key) {
            // 400 y NO {valid:false}: un error de forma no es una licencia
            // inválida. El plugin lo lee como UNKNOWN y aplica gracia, que es
            // lo correcto ante un problema nuestro, no del comprador.
            return sendJson(response, 400, { error: "falta el parámetro 'license'" });
        }

        try {
            sendJson(response, 200, verify(key, resource));
        } catch (error) {
            console.error('verify falló:', error);
            sendJson(response, 500, { error: 'internal error' });
        }
    });

    server.listen(PORT, () => {
        console.log(`Servidor de licencias escuchando en :${PORT} (db: ${DB_PATH})`);
    });
}

// ---------------------------------------------------------------------------
// CLI de administración
// ---------------------------------------------------------------------------

function generateKey() {
    // 20 bytes en base32-ish, agrupados para que sea legible al dictarla.
    const raw = crypto.randomBytes(20).toString('base64url').toUpperCase().replace(/[^A-Z0-9]/g, '');
    return 'RPGR-' + (raw.slice(0, 20).match(/.{1,5}/g) || []).join('-');
}

function issue(buyer, products) {
    if (!buyer) {
        console.error('Uso: node server.js issue <comprador> [producto...]');
        process.exit(1);
    }

    const db = loadDb();
    const license = {
        key: generateKey(),
        buyer,
        products: products.length ? products : ['*'],
        issuedAt: new Date().toISOString(),
        revoked: false,
    };

    db.licenses.push(license);
    saveDb(db);

    console.log(`Licencia emitida para ${buyer} (${license.products.join(', ')}):\n\n  ${license.key}\n`);
}

function revoke(key, reason) {
    const db = loadDb();
    const license = findLicense(db, key);

    if (!license) {
        console.error(`No existe ninguna licencia con la clave ${key}`);
        process.exit(1);
    }

    license.revoked = true;
    license.revokedAt = new Date().toISOString();
    if (reason) {
        license.revokedReason = reason;
    }

    saveDb(db);
    console.log(`Licencia ${key} revocada.`);
}

function list() {
    const db = loadDb();

    if (!db.licenses.length) {
        console.log('No hay licencias emitidas todavía.');
        return;
    }

    for (const license of db.licenses) {
        const state = license.revoked ? 'REVOCADA' : 'activa';
        console.log(`${license.key}  ${state.padEnd(9)}  ${license.buyer}  [${(license.products || ['*']).join(', ')}]`);
    }
}

const [command, ...args] = process.argv.slice(2);

switch (command) {
    case undefined:
    case 'serve':
        startServer();
        break;
    case 'issue':
        issue(args[0], args.slice(1));
        break;
    case 'revoke':
        revoke(args[0], args.slice(1).join(' '));
        break;
    case 'list':
        list();
        break;
    default:
        console.error(`Comando desconocido: ${command}`);
        console.error('Comandos: serve (default) | issue | revoke | list');
        process.exit(1);
}
