// Comprueba límites de seguridad del cliente junto a las pruebas HTTP y de navegador.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { api, esc, query, session } from '../src/api.js';

const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key), setItem: (k,v) => values.set(k,v), removeItem: k => values.delete(k) };
globalThis.window = new EventTarget();

test('escapa consultas y codifica filtros', () => {
  assert.equal(esc('<img onerror="x">'), '&lt;img onerror=&quot;x&quot;&gt;');
  assert.equal(query({ tema: 'Álgebra & cálculo', estado: '', id: null }), 'tema=%C3%81lgebra+%26+c%C3%A1lculo');
});
test('un rechazo HTTP conserva el error del servidor', async () => {
  session.set('acceso-local');
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/alumno/dudas');
    assert.equal(options.headers.Authorization, 'Bearer acceso-local');
    return new Response(JSON.stringify({ detail: 'Sin matrícula vigente' }), { status: 403 });
  };
  await assert.rejects(api('/alumno/dudas'), /Sin matrícula vigente/);
});
test('sesión vencida limpia el JWT y comunica el cierre', async () => {
  let expired = false;
  window.addEventListener('session-expired', () => { expired = true; }, { once: true });
  globalThis.fetch = async () => new Response('', { status: 401 });
  await assert.rejects(api('/cuenta'), /sesión/);
  assert.equal(session.get(), '');
  assert.equal(expired, true);
});
