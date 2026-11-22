/* Monte seu bolo — logica de fotos, precos e catalogo dinamico.
 * As fotos ficam em /bolos/<modelo>[-flores|-frutas|-lisa].webp. Qualquer foto nova que siga o padrao de nome
 *   bolo-<1|2>-andar(es)-<coracao|quadrado|redondo>-<massa>-<recheios...>-<branca|chocolate|naked>.webp
 * e detectada automaticamente (GET /api/bolos/modelos) e entra no construtor e na galeria. */

export const DOUGHS = ['Baunilha', 'Chocolate', 'Red Velvet'];
export const FILLINGS = ['Brigadeiro', 'Doce de Leite', 'Creme de Baunilha', 'Morango'];
export const SHAPES = ['Redondo', 'Quadrado', 'Coração'];
export const FROSTINGS = [
  { id: 'Branca', slug: 'branca', swatch: 'vanilla' },
  { id: 'Chocolate', slug: 'chocolate', swatch: 'chocolate' },
  { id: 'Naked', slug: 'naked', swatch: 'naked', label: 'Sem Cobertura (Naked)' },
  { id: 'Rosa delicado', slug: 'rosa', swatch: 'rose' },
];
export const SIZES = [{ size: 15, slices: 10, price: 120 }, { size: 20, slices: 20, price: 180 }, { size: 25, slices: 30, price: 240 }];
export const DEFAULT_CAKE = { tiers: 2, dough: 'Red Velvet', fillings: ['Doce de Leite', 'Morango'], shape: 'Coração', frosting: 'Branca', size: 20, berries: true, flowers: true, note: '' };

const FILLING_ORDER = ['Creme de Baunilha', 'Brigadeiro', 'Doce de Leite', 'Morango'];
const FILLING_SLUGS = ['creme-de-baunilha', 'doce-de-leite', 'brigadeiro', 'morango'];
const slug = value => String(value).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/\s+/g, '-');

/** Modelos fotografados que acompanham o projeto (usados se a API nao responder). */
const BASE_FILES = [
  'bolo-1-andar-coracao-baunilha-brigadeiro-chocolate', 'bolo-1-andar-coracao-baunilha-brigadeiro-doce-de-leite-branca',
  'bolo-1-andar-coracao-chocolate-brigadeiro-doce-de-leite-naked', 'bolo-1-andar-coracao-red-velvet-creme-de-baunilha-morango-naked',
  'bolo-1-andar-quadrado-baunilha-brigadeiro-doce-de-leite-naked', 'bolo-1-andar-quadrado-chocolate-doce-de-leite-chocolate',
  'bolo-1-andar-quadrado-red-velvet-doce-de-leite-branca', 'bolo-1-andar-redondo-baunilha-creme-de-baunilha-branca',
  'bolo-1-andar-redondo-chocolate-brigadeiro-chocolate', 'bolo-1-andar-redondo-red-velvet-brigadeiro-naked',
  'bolo-2-andares-coracao-chocolate-creme-de-baunilha-chocolate', 'bolo-2-andares-coracao-chocolate-morango-naked',
  'bolo-2-andares-coracao-red-velvet-creme-de-baunilha-naked', 'bolo-2-andares-quadrado-baunilha-doce-de-leite-naked',
  'bolo-2-andares-quadrado-baunilha-morango-chocolate', 'bolo-2-andares-quadrado-red-velvet-brigadeiro-doce-de-leite-branca',
  'bolo-2-andares-redondo-baunilha-morango-creme-de-baunilha-branca', 'bolo-2-andares-redondo-chocolate-creme-de-baunilha-morango-branca',
  'bolo-2-andares-redondo-red-velvet-brigadeiro-chocolate',
];

const NAME_RE = /^bolo-(\d)-andar(?:es)?-(coracao|quadrado|redondo)-(baunilha|chocolate|red-velvet)-(.+)-(branca|chocolate|naked)$/;
const lookupKey = (tiers, shape, dough, fillings, frosting) => [tiers, shape, dough, [...fillings].sort().join('+'), frosting].join('|');
let entries = [];
let exact = new Map();

/** Registra (ou amplia) o catalogo a partir dos nomes de arquivo, sem extensao e sem sufixo de decoracao. */
export function registerModels(files) {
  const seen = new Set(entries.map(e => e.file));
  for (const raw of files) {
    const file = String(raw).replace(/\.webp$/, '').replace(/-(flores|frutas|lisa)$/, '');
    const m = file.match(NAME_RE);
    if (!m || seen.has(file)) continue;
    let rest = m[4]; const fillings = [];
    while (rest) { const found = FILLING_SLUGS.find(s => rest === s || rest.startsWith(`${s}-`)); if (!found) break; fillings.push(found); rest = rest.slice(found.length + 1); }
    if (!fillings.length || rest) continue;
    const entry = { file, tiers: Number(m[1]), shape: m[2], dough: m[3], fillings, frosting: m[5] };
    entries.push(entry); seen.add(file);
    exact.set(lookupKey(entry.tiers, entry.shape, entry.dough, fillings, entry.frosting), file);
  }
  return entries.length;
}
registerModels(BASE_FILES);

/** Busca modelos extras no servidor (fotos novas aparecem sem alterar codigo). Falhas sao silenciosas. */
export async function loadModels() {
  try {
    const response = await fetch('/api/bolos/modelos', { headers: { Accept: 'application/json' }, cache: 'no-store' });
    if (!response.ok) return entries.length;
    const data = await response.json();
    return registerModels(Array.isArray(data.modelos) ? data.modelos : []);
  } catch { return entries.length; }
}

const frostingSlug = c => FROSTINGS.find(f => f.id === c.frosting)?.slug || 'branca';
const doughSlug = c => slug(c.dough);
const shapeSlug = c => slug(c.shape);
const fillingSlugOf = name => slug(name);

export const supportsRose = c => c.tiers === 2 && Boolean(c.flowers && c.berries) && c.fillings.length === 2 && c.fillings.includes('Doce de Leite') && c.fillings.includes('Morango');
export function normalize(config) {
  const c = { ...DEFAULT_CAKE, ...config, fillings: [...(config?.fillings ?? DEFAULT_CAKE.fillings)] };
  c.tiers = Number(c.tiers) === 1 ? 1 : 2; c.size = [15, 20, 25].includes(Number(c.size)) ? Number(c.size) : 20;
  if (c.frosting === 'Rosa delicado' && !supportsRose(c)) c.frosting = 'Branca';
  return c;
}
export const priceOf = c => (SIZES.find(s => s.size === c.size)?.price ?? 120) + (c.shape === 'Coração' ? 20 : 0) + (c.dough === 'Red Velvet' ? 15 : 0) + c.fillings.length * 10;
export const dimensionsOf = c => c.shape === 'Redondo' ? `Ø ${c.size} cm` : `${c.size} × ${c.size} cm`;
export const tiersLabel = c => `${c.tiers} ${c.tiers === 2 ? 'andares' : 'andar'}`;
export const describe = c => `${tiersLabel(c)} · ${c.shape} · ${dimensionsOf(c)} · ${c.dough} · ${c.fillings.join(' + ') || 'sem recheio'} · Cobertura ${c.frosting}${c.flowers ? ' · Flores' : ''}${c.berries ? ' · Frutas vermelhas' : ''}`;

const decoration = c => c.flowers ? (c.berries ? '' : '-flores') : (c.berries ? '-frutas' : '-lisa');
function rosePhoto(c) {
  if (c.frosting !== 'Rosa delicado' || !supportsRose(c)) return null;
  const idx = FILLING_ORDER.flatMap((f, i) => c.fillings.includes(f) ? [i] : []).join('-');
  const shape = c.shape === 'Quadrado' ? 'square' : c.shape === 'Coração' ? 'heart' : 'round';
  const dough = c.dough === 'Chocolate' ? 'chocolate' : c.dough === 'Baunilha' ? 'vanilla' : 'velvet';
  return `/bolos/rosa/${shape}-2-${dough}-${idx}-11-rose.webp`;
}

/** Foto exata da combinacao (ou a mais proxima, indicada em exact:false). Sempre devolve uma foto. */
export function photoFor(config) {
  const c = normalize(config);
  const rose = rosePhoto(c);
  if (rose) return { url: rose, exact: true };
  const file = exact.get(lookupKey(c.tiers, shapeSlug(c), doughSlug(c), c.fillings.map(fillingSlugOf), frostingSlug(c)));
  if (file) return { url: `/bolos/${file}${decoration(c)}.webp`, exact: true };
  let best = entries[0], bestScore = -Infinity;
  const wanted = c.fillings.map(fillingSlugOf);
  for (const e of entries) {
    const score = (e.dough === doughSlug(c) ? 6 : 0) + (e.tiers === c.tiers ? 5 : 0) + (e.shape === shapeSlug(c) ? 4.5 : 0)
      + (e.frosting === frostingSlug(c) ? 4 : 0) + e.fillings.filter(f => wanted.includes(f)).length * 2 - e.fillings.filter(f => !wanted.includes(f)).length;
    if (score > bestScore) { best = e; bestScore = score; }
  }
  return { url: `/bolos/${best.file}${decoration(c)}.webp`, exact: false };
}

/** Configuracao que uma foto do catalogo representa (sempre com flores e frutas, como fotografada). */
export function configOfEntry(e) {
  const title = s => FILLING_ORDER.find(f => slug(f) === s) || s;
  return normalize({
    tiers: e.tiers, shape: e.shape === 'coracao' ? 'Coração' : e.shape === 'quadrado' ? 'Quadrado' : 'Redondo',
    dough: e.dough === 'chocolate' ? 'Chocolate' : e.dough === 'baunilha' ? 'Baunilha' : 'Red Velvet',
    fillings: e.fillings.map(title), frosting: FROSTINGS.find(f => f.slug === e.frosting)?.id || 'Branca', size: 20, berries: true, flowers: true, note: '',
  });
}
export const models = () => entries.map(e => ({ ...e, config: configOfEntry(e), image: `/bolos/${e.file}.webp` }));
export const modelByFile = file => entries.find(e => e.file === file);
export function randomModel() { const e = entries[Math.floor(Math.random() * entries.length)]; return e ? configOfEntry(e) : { ...DEFAULT_CAKE }; }

/** Aquece o cache do navegador para a troca de foto ser instantanea. */
export function preload(config) {
  const base = photoFor(config).url.replace(/-(frutas|flores|lisa)\.webp$/, '.webp');
  for (const suffix of ['', '-frutas', '-flores', '-lisa']) { const img = new Image(); img.decoding = 'async'; img.src = base.replace(/\.webp$/, `${suffix}.webp`); }
}

const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
const money = amount => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount || 0);
const pill = (key, value, label, on, extra = '') => `<button type="button" class="pill ${on ? 'is-on' : ''}" data-action="cake-option" data-key="${key}" data-value="${esc(value)}" aria-pressed="${on}" ${extra}>${label}</button>`;

function controls(c) {
  const roseOk = supportsRose(c);
  return `
  <section class="builder-section"><h3>Andares</h3><div class="pill-row">${[1, 2].map(t => pill('tiers', t, `${t} ${t === 1 ? 'andar' : 'andares'}`, c.tiers === t)).join('')}</div></section>
  <section class="builder-section"><h3>Massa <small>Red Velvet + R$ 15</small></h3><div class="pill-row">${DOUGHS.map(d => pill('dough', d, d, c.dough === d)).join('')}</div></section>
  <section class="builder-section"><h3>Recheio <small>${c.fillings.length}/2 · + R$ 10 por sabor</small></h3><div class="pill-row">${FILLINGS.map(f => pill('fillings', f, f, c.fillings.includes(f))).join('')}</div></section>
  <section class="builder-section"><h3>Formato <small>Coração + R$ 20</small></h3><div class="pill-row">${SHAPES.map(s => pill('shape', s, s, c.shape === s)).join('')}</div></section>
  <section class="builder-section"><h3>Cobertura</h3><div class="pill-row">${FROSTINGS.map(f => { const off = f.id === 'Rosa delicado' && !roseOk; return pill('frosting', f.id, `<i class="swatch swatch-${f.swatch}" aria-hidden="true"></i>${f.label || f.id}`, c.frosting === f.id, off ? 'disabled title="Rosa delicado: 2 andares, Doce de Leite + Morango, flores e frutas."' : ''); }).join('')}</div></section>
  <section class="builder-section"><h3>Tamanho <small>${SIZES.find(s => s.size === c.size)?.slices} fatias</small></h3><div class="pill-row">${SIZES.map(s => pill('size', s.size, c.shape === 'Redondo' ? `Ø ${s.size} cm` : `${s.size} × ${s.size} cm`, c.size === s.size)).join('')}</div></section>
  <section class="builder-section"><h3>Decoração</h3><div class="pill-row"><button type="button" class="pill ${c.berries ? 'is-on' : ''}" data-action="cake-toggle" data-key="berries" aria-pressed="${c.berries}">Frutas Vermelhas</button><button type="button" class="pill ${c.flowers ? 'is-on' : ''}" data-action="cake-toggle" data-key="flowers" aria-pressed="${c.flowers}">Flores</button></div></section>`;
}

export function previewMarkup(c) {
  const photo = photoFor(c);
  const alt = `Bolo ${c.shape}, ${tiersLabel(c)}, massa ${c.dough}, recheio ${c.fillings.join(' e ') || 'não selecionado'}, cobertura ${c.frosting}${c.flowers ? ', flores' : ''}${c.berries ? ', frutas vermelhas' : ''}`;
  return `<div class="cake-studio" aria-label="Prévia do bolo personalizado">
    <div class="studio-heading"><span class="eyebrow">Ateliê Maison</span><span class="studio-live"><span></span>Sua criação</span></div>
    <div class="cake-stage cake-size-${c.size} cake-tiers-${c.tiers}"><div class="cake-composition"><img class="cake-render" data-cake-render src="${photo.url}" alt="${esc(alt)}" width="1024" height="1024"></div>
      <div class="cake-measure"><span></span><p data-cake-dim>${dimensionsOf(c)}</p><span></span></div><div class="cake-height"><span></span><b data-cake-tiers>${tiersLabel(c)}</b><span></span></div></div>
    <p class="studio-note" data-cake-note>${photo.exact ? 'Imagem ilustrativa' : 'Foto ilustrativa mais próxima da sua escolha'}</p>
    <div class="studio-caption"><span class="eyebrow red">Feito para celebrar</span><h2 class="serif">Uma doce obra-prima.</h2><p data-cake-flavors>${esc(c.dough)} · ${esc(c.fillings.join(' + ') || 'Sem recheio selecionado')}</p><p data-cake-frosting>Cobertura · ${esc(c.frosting)}</p></div>
    <div class="studio-specs"><span>◫ ${tiersLabel(c)}</span><span>⌖ ${dimensionsOf(c)}</span>${c.flowers ? '<span>✿ Flores</span>' : ''}${c.berries ? '<span>● Frutas vermelhas</span>' : ''}</div></div>`;
}

/** Atualiza so a foto (com fade) sem recriar a pagina inteira. */
export function swapPhoto(c) {
  const img = document.querySelector('[data-cake-render]');
  if (!img) return false;
  const photo = photoFor(c);
  if (!img.src.endsWith(photo.url)) {
    const next = new Image(); next.decoding = 'async';
    next.onload = () => { img.classList.add('is-swapping'); setTimeout(() => { img.src = photo.url; img.alt = `Bolo ${c.shape}, ${tiersLabel(c)}, massa ${c.dough}`; img.classList.remove('is-swapping'); }, 120); };
    next.src = photo.url;
  }
  const set = (sel, text) => { const el = document.querySelector(sel); if (el) el.textContent = text; };
  set('[data-cake-note]', photo.exact ? 'Imagem ilustrativa' : 'Foto ilustrativa mais próxima da sua escolha');
  preload(c);
  return true;
}

export function galleryMarkup(filter = {}) {
  const all = models();
  const list = all.filter(m => (!filter.tiers || m.tiers === filter.tiers) && (!filter.dough || m.config.dough === filter.dough));
  const chip = (key, value, label, on) => `<button type="button" class="pill small ${on ? 'is-on' : ''}" data-action="cake-filter" data-key="${key}" data-value="${value ?? ''}" aria-pressed="${on}">${label}</button>`;
  return `<section class="wrap cake-catalog" aria-labelledby="catalogo-bolos"><span class="eyebrow red">INSPIRE-SE</span>
    <h2 class="serif" id="catalogo-bolos">Nosso catálogo de bolos</h2><p>Toque em um bolo para montá-lo acima. ${all.length} modelos fotografados.</p>
    <div class="pill-row gallery-filters">${chip('tiers', '', 'Todos', !filter.tiers && !filter.dough)}${[1, 2].map(t => chip('tiers', t, `${t} ${t === 1 ? 'andar' : 'andares'}`, filter.tiers === t)).join('')}${DOUGHS.map(d => chip('dough', d, d, filter.dough === d)).join('')}</div>
    <div class="cake-gallery">${list.map(m => `<button type="button" class="model-card" data-action="cake-model" data-file="${m.file}"><img src="${m.image}" alt="Bolo ${esc(m.config.dough)}, ${esc(m.config.shape)}, ${tiersLabel(m.config)}" loading="lazy" decoding="async"><span>${tiersLabel(m.config)} · ${m.config.shape}</span><small>${esc(m.config.dough)} · ${esc(m.config.fillings.join(' + '))}</small><b>${money(priceOf(m.config))}</b></button>`).join('') || '<p class="muted">Nenhum modelo com esse filtro.</p>'}</div></section>`;
}

export function builderMarkup(config, filter = {}) {
  const c = normalize(config);
  preload(c);
  return `<main id="main" class="cake-page"><div class="wrap builder-heading"><a class="back-link" href="/">← Nossa vitrine</a><div><span class="eyebrow red">SEU MOMENTO. SEU BOLO.</span><h1 class="serif">Monte seu bolo<span class="red">.</span></h1><p>Camadas de carinho, do seu jeito.</p></div></div>
  <div class="wrap builder-layout"><section class="builder-controls"><div class="builder-title"><h2>Os detalhes fazem a diferença</h2><span class="builder-tools"><button class="icon-button" data-action="cake-surprise" aria-label="Surpreenda-me" title="Surpreenda-me">✦</button><button class="icon-button" data-action="cake-reset" aria-label="Restaurar escolhas" title="Restaurar escolhas">↺</button></span></div>
  ${controls(c)}
  <details class="builder-note"><summary>Uma mensagem especial <span>⌄</span></summary><label for="cake-note">Mensagem ou observação</label><textarea id="cake-note" maxlength="300" placeholder="Uma mensagem para um dia especial…">${esc(c.note)}</textarea></details>
  <div class="builder-total"><span>Seu bolo personalizado<small>Feito sob encomenda</small></span><strong data-cake-price>${money(priceOf(c))}</strong></div>
  <button class="button button-primary button-wide" data-action="add-cake" ${c.fillings.length ? '' : 'disabled'}>♧ &nbsp; Adicionar ao carrinho <span>→</span></button>${c.fillings.length ? '' : '<p class="tiny center muted">Escolha pelo menos um recheio para continuar.</p>'}</section>
  <aside class="builder-preview">${previewMarkup(c)}</aside></div>${galleryMarkup(filter)}</main>`;
}
