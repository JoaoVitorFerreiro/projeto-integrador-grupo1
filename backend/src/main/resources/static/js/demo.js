/* Ferramentas dos perfis de teste: cupons, catalogo por filial (preco, unidades, campanhas) e estoque.
 * Tudo fica no navegador (state.coupons / state.demoCatalog) e nao altera dados reais. */

export const DEFAULT_QTY = 20;
const BRANCH_IDS = ['jardins', 'pinheiros', 'moema'];
export const STATUS_LABEL = { DISPONIVEL: 'Disponível', ESGOTADO: 'Esgotado', EM_PRODUCAO: 'Em produção' };
const STATUS_FROM_LABEL = { 'Disponível': 'DISPONIVEL', 'Esgotado': 'ESGOTADO', 'Em produção': 'EM_PRODUCAO' };

/* ---------- sementes ---------- */
const seedCoupons = () => [
  { code: 'BEMVINDO10', percent: 10, active: true, holders: ['pessoa'] },
  { code: 'EMPRESA15', percent: 15, active: true, holders: ['empresa'] },
  { code: 'NATAL20', percent: 20, active: false, holders: [] },
];
const SEASONAL = [
  { id: 'demo-natal', name: 'Natal Maison (teste)', start_date: '2026-11-01', end_date: '2026-12-31', active: true, items: ['folhas-de-ouro', 'veludo-real', 'brioche'] },
  { id: 'demo-junina', name: 'Festa Junina (teste)', start_date: '2027-06-01', end_date: '2027-06-30', active: false, items: ['orgulho-mineiro', 'abraco-de-vo'] },
];

/** Garante as estruturas no estado (sem sobrescrever edicoes ja feitas pelo administrador). */
export function initDemo(state, PRODUCTS) {
  if (!Array.isArray(state.coupons)) state.coupons = seedCoupons();
  if (!state.demoCatalog || !Array.isArray(state.demoCatalog.products)) state.demoCatalog = { products: [], campaigns: [] };
  const catalog = state.demoCatalog;
  for (const c of SEASONAL) {
    if (catalog.campaigns.some(x => x.id === c.id)) continue;
    const { items, ...campaign } = c;
    catalog.campaigns.push(campaign);
    for (const id of items) for (const branchId of BRANCH_IDS) {
      if (catalog.products.some(p => p.productId === id && p.branchId === branchId)) continue;
      const base = PRODUCTS.find(p => p.id === id);
      if (base) catalog.products.push({ productId: id, branchId, price: base.price, hidden: false, stock_status: 'DISPONIVEL', campaign_id: c.id });
    }
  }
}

/* ---------- consultas ---------- */
export const rowOf = (state, id, branch) => state.demoCatalog.products.find(p => p.productId === id && p.branchId === branch);
export const qtyOf = (state, id, branch) => rowOf(state, id, branch)?.stock_qty ?? DEFAULT_QTY;
export const priceOf = (state, product, branch) => rowOf(state, product.id, branch)?.price ?? product.price;
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());

/** visible: aparece na vitrine; available: pode ser comprado agora. Zero unidades sempre significa esgotado. */
export function availability(state, id, branch, day = today()) {
  const row = rowOf(state, id, branch);
  const campaign = row?.campaign_id ? state.demoCatalog.campaigns.find(c => c.id === row.campaign_id) : undefined;
  const inCampaign = !row?.campaign_id || Boolean(campaign?.active && day >= campaign.start_date && day <= campaign.end_date);
  const visible = inCampaign && !row?.hidden;
  const qty = qtyOf(state, id, branch);
  const status = qty <= 0 ? 'ESGOTADO' : (row?.stock_status ?? 'DISPONIVEL');
  return { visible, available: visible && status === 'DISPONIVEL', status, label: STATUS_LABEL[status], qty, campaign };
}

/* ---------- cupons ---------- */
const cleanCode = code => String(code || '').trim().toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 20);
export function saveCoupon(state, role, code, percent) {
  const c = cleanCode(code);
  if (role !== 'admin') throw new Error('Somente o administrador cria cupons.');
  if (!c) throw new Error('Informe um código.');
  if (!(percent >= 1 && percent <= 50)) throw new Error('O desconto deve ficar entre 1% e 50%.');
  if (state.coupons.some(x => x.code === c)) throw new Error('Já existe um cupom com este código.');
  state.coupons.push({ code: c, percent: Math.round(percent), active: false, holders: [] });
}
export function toggleCoupon(state, role, code) {
  if (role !== 'admin') throw new Error('Somente o administrador ativa cupons.');
  const c = state.coupons.find(x => x.code === code); if (c) c.active = !c.active;
}
export function giveCoupon(state, role, code, customerId) {
  if (role !== 'employee' && role !== 'admin') throw new Error('Sem permissão para distribuir cupons.');
  const c = state.coupons.find(x => x.code === code);
  if (!c || !c.active) throw new Error('Só cupons ativos podem ser enviados.');
  if (!customerId) throw new Error('Escolha um cliente.');
  if (!c.holders.includes(customerId)) c.holders.push(customerId);
}
export const redeemable = (state, code, customerId) => { const c = state.coupons.find(x => x.code === cleanCode(code)); return c && c.active && c.holders.includes(customerId) ? c : null; };
export const couponsOf = (state, customerId) => state.coupons.filter(c => c.holders.includes(customerId));

/* ---------- catalogo / estoque ---------- */
export function saveProduct(state, account, input) {
  if (account?.role !== 'admin') throw new Error('Somente o administrador de teste pode alterar o catálogo.');
  const price = Number(input.price);
  if (!Number.isFinite(price) || price <= 0 || price > 1000000 || Math.abs(price * 100 - Math.round(price * 100)) > 1e-6) throw new Error('Informe um preço válido com até duas casas decimais.');
  const qty = Math.max(0, Math.floor(Number(input.qty) || 0));
  if (input.campaign_id && !state.demoCatalog.campaigns.some(c => c.id === input.campaign_id)) throw new Error('Escolha uma campanha existente.');
  const status = qty <= 0 ? 'ESGOTADO' : (STATUS_FROM_LABEL[input.status] || input.status || 'DISPONIVEL');
  const row = { productId: input.productId, branchId: input.branchId, price: Math.round(price * 100) / 100, hidden: Boolean(input.hidden), stock_status: status, campaign_id: input.campaign_id || null, stock_qty: qty };
  state.demoCatalog.products = [...state.demoCatalog.products.filter(p => !(p.productId === row.productId && p.branchId === row.branchId)), row];
}
export function saveCampaign(state, account, input) {
  if (account?.role !== 'admin') throw new Error('Somente o administrador de teste pode alterar campanhas.');
  const name = String(input.name || '').trim();
  if (!name || name.length > 100) throw new Error('Informe o nome da campanha (até 100 caracteres).');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(input.start_date) || !/^\d{4}-\d{2}-\d{2}$/.test(input.end_date)) throw new Error('Informe as datas da campanha.');
  if (input.end_date < input.start_date) throw new Error('A data final deve ser igual ou posterior à inicial.');
  const row = { id: input.id, name, start_date: input.start_date, end_date: input.end_date, active: Boolean(input.active) };
  state.demoCatalog.campaigns = [...state.demoCatalog.campaigns.filter(c => c.id !== row.id), row];
}
export function saveStock(state, account, PRODUCTS, productId, branchId, status, qtyRaw) {
  if (!account || !(account.role === 'admin' || (account.role === 'employee' && account.branchId === branchId))) throw new Error('Sem permissão para alterar o estoque desta filial.');
  const base = PRODUCTS.find(p => p.id === productId); if (!base) throw new Error('Produto inexistente.');
  const current = rowOf(state, productId, branchId);
  const qty = qtyRaw === '' || qtyRaw === undefined ? undefined : Math.max(0, Math.floor(Number(qtyRaw) || 0));
  const next = { productId, branchId, price: current?.price ?? base.price, hidden: current?.hidden ?? false, campaign_id: current?.campaign_id ?? null, stock_status: STATUS_FROM_LABEL[status] || status, stock_qty: qty ?? current?.stock_qty ?? DEFAULT_QTY };
  if (next.stock_qty <= 0) next.stock_status = 'ESGOTADO';
  else if (next.stock_status === 'ESGOTADO' && current?.stock_qty === 0 && qty !== undefined) next.stock_status = 'DISPONIVEL';
  state.demoCatalog.products = [...state.demoCatalog.products.filter(p => !(p.productId === productId && p.branchId === branchId)), next];
}
/** Pedido simulado: baixa as unidades vendidas; ao chegar a zero o item fica esgotado. */
export function consumeStock(state, items, branch, PRODUCTS) {
  for (const item of items) {
    if (item.cake) continue;
    const id = item.productId || item.id; const base = PRODUCTS.find(p => p.id === id); if (!base) continue;
    const current = rowOf(state, id, branch);
    const qty = Math.max(0, qtyOf(state, id, branch) - Number(item.quantity || 0));
    const row = { productId: id, branchId: branch, price: current?.price ?? base.price, hidden: current?.hidden ?? false, campaign_id: current?.campaign_id ?? null, stock_status: qty === 0 ? 'ESGOTADO' : (current?.stock_status ?? 'DISPONIVEL'), stock_qty: qty };
    state.demoCatalog.products = [...state.demoCatalog.products.filter(p => p !== current), row];
  }
}
export function cartBlocker(state, items, branch) {
  for (const i of items) {
    if (i.cake) continue;
    const id = i.productId || i.id; const a = availability(state, id, branch);
    if (!a.available) return 'Há itens indisponíveis na sua loja. Remova-os para continuar.';
    if (i.quantity > a.qty) return `${i.name}: restam apenas ${a.qty} unidade(s) na loja.`;
  }
  return null;
}

/* ---------- visual ---------- */
const money = amount => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount || 0);
const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
const dateBr = iso => String(iso || '').split('-').reverse().join('/');
const statusOptions = current => Object.values(STATUS_LABEL).map(l => `<option ${STATUS_LABEL[current] === l ? 'selected' : ''}>${l}</option>`).join('');

/** Painel de cupons conforme o perfil (administrador cria/ativa; funcionario distribui; cliente ve os seus). */
export function couponsPanel(ctx, account) {
  const { state, DEMOS } = ctx; const role = account.role;
  const customers = DEMOS.filter(a => a.role === 'customer' || a.role === 'company');
  const mine = couponsOf(state, account.id);
  const title = role === 'admin' ? 'Gerenciar cupons' : role === 'employee' ? 'Distribuir cupons' : 'Meus cupons';
  let body = '';
  if (role === 'customer' || role === 'company') {
    body = mine.length
      ? `<div class="coupon-wallet">${mine.map(c => `<div class="coupon-ticket ${c.active ? '' : 'is-off'}"><strong>${esc(c.code)}</strong><span>${c.percent}% de desconto</span><small>${c.active ? 'Digite este código no carrinho' : 'Cupom inativo no momento'}</small></div>`).join('')}</div>`
      : '<p class="muted">Você ainda não recebeu cupons. A equipe da loja pode enviar cupons ativos para você.</p>';
  }
  if (role === 'admin') body += `<form class="form-grid coupon-form" data-demo-form="coupon-create"><label class="field-label">Código<input name="code" maxlength="20" required placeholder="PRIMAVERA10" style="text-transform:uppercase"></label><label class="field-label">Desconto (%)<input name="percent" type="number" min="1" max="50" step="1" value="10" required></label><button class="button button-primary">Criar cupom</button></form>`;
  if (role === 'admin' || role === 'employee') {
    body += `<div class="coupon-list">${state.coupons.map(c => `<div class="coupon-row-item"><strong>${esc(c.code)}</strong><span>${c.percent}%</span><span class="pill-status ${c.active ? 'on' : 'off'}">${c.active ? 'Ativo' : 'Inativo'}</span><small>${c.holders.length} cliente(s)</small>${role === 'admin' ? `<button type="button" class="button button-outline small" data-demo-act="toggle-coupon" data-code="${esc(c.code)}">${c.active ? 'Desativar' : 'Ativar'}</button>` : ''}${c.active ? `<span class="give"><select aria-label="Cliente para ${esc(c.code)}" data-give-for="${esc(c.code)}"><option value="">Escolher cliente</option>${customers.filter(a => !c.holders.includes(a.id)).map(a => `<option value="${a.id}">${esc(a.name)}</option>`).join('')}</select><button type="button" class="button button-primary small" data-demo-act="give-coupon" data-code="${esc(c.code)}">Enviar</button></span>` : ''}</div>`).join('')}</div>`;
    if (role === 'employee') body += '<p class="demo-disclaimer">Somente cupons ativados pelo administrador podem ser enviados.</p>';
  }
  return `<section class="platform-panel demo-coupons" aria-label="Cupons"><h2>🎟 ${title}</h2>${body}</section>`;
}

/** Estoque da filial do funcionario: situacao + quantidade de unidades. */
export function stockPanel(ctx, account) {
  const { state, PRODUCTS, BRANCHES } = ctx; const branchId = account.branchId; const branch = BRANCHES.find(b => b.id === branchId);
  return `<section class="platform-panel demo-stock"><div class="panel-head"><div><h2>Estoque da loja</h2><p class="muted">Maison ${esc(branch?.name)} · situação e quantidade de unidades</p></div><label class="field-label">Buscar item<input data-demo-filter=".stock-row" placeholder="Nome do produto"></label></div>
  <div class="stock-rows">${PRODUCTS.map(p => { const a = availability(state, p.id, branchId); return `<form class="stock-row" data-demo-form="stock" data-search="${esc(p.name.toLocaleLowerCase('pt-BR'))}"><input type="hidden" name="productId" value="${p.id}"><input type="hidden" name="branchId" value="${branchId}"><img src="${p.image}" alt="" width="48" height="48" loading="lazy"><span class="who"><strong>${esc(p.name)}</strong><small>${p.category} · ${a.qty} un. · ${a.label}${a.visible ? '' : ' · fora da vitrine'}</small></span><label class="field-label">Situação<select name="status">${statusOptions(a.status)}</select></label><label class="field-label">Unidades<input name="qty" type="number" min="0" step="1" value="${a.qty}" required></label><button class="button button-outline small">Salvar</button></form>`; }).join('')}</div></section>`;
}

/** Catalogo do administrador: preco, unidades, situacao, campanha, retirar do catalogo + campanhas sazonais. */
export function catalogAdminPanel(ctx) {
  const { state, PRODUCTS, BRANCHES } = ctx; const ui = state.demoUi || (state.demoUi = {}); const branch = ui.adminBranch || 'jardins'; const c = state.demoCatalog;
  const editing = ui.editCampaign ? (c.campaigns.find(x => x.id === ui.editCampaign) || { id: ui.editCampaign, name: '', start_date: today(), end_date: today(), active: false }) : null;
  const rows = PRODUCTS.map(p => {
    const row = rowOf(state, p.id, branch); const a = availability(state, p.id, branch);
    return `<form class="admin-product" data-demo-form="product" data-search="${esc((p.name + ' ' + p.description).toLocaleLowerCase('pt-BR'))}"><input type="hidden" name="productId" value="${p.id}"><input type="hidden" name="branchId" value="${branch}"><input type="hidden" name="hidden" value="${row?.hidden ? '1' : ''}">
      <div class="who"><img src="${p.image}" alt="${esc(p.name)}" width="64" height="64" loading="lazy"><span><strong>${esc(p.name)}</strong><small>${p.category} · ${a.qty} un. · ${row?.hidden ? 'Fora do catálogo' : money(row?.price ?? p.price)}${a.campaign ? ' · ' + esc(a.campaign.name) : ''}</small></span></div>
      <div class="fields"><label class="field-label">Preço (R$)<input name="price" type="number" min="0.01" max="1000000" step="0.01" required value="${row?.price ?? p.price}"></label><label class="field-label">Unidades<input name="qty" type="number" min="0" step="1" required value="${a.qty}"></label><label class="field-label">Situação<select name="status">${statusOptions(a.status)}</select></label><label class="field-label">Campanha<select name="campaign_id"><option value="">Nenhuma</option>${c.campaigns.map(x => `<option value="${x.id}" ${row?.campaign_id === x.id ? 'selected' : ''}>${esc(x.name)}</option>`).join('')}</select></label>
      <span class="actions"><button type="button" class="button button-outline small" data-demo-act="toggle-hidden">${row?.hidden ? '👁 Voltar ao catálogo' : '⊘ Retirar do catálogo'}</button><button class="button button-primary small">💾 Salvar alterações</button></span></div></form>`;
  }).join('');
  const campaignForm = editing ? `<form class="form-grid campaign-form" data-demo-form="campaign"><input type="hidden" name="id" value="${esc(editing.id)}"><label class="field-label">Nome da campanha<input name="name" maxlength="100" required value="${esc(editing.name)}"></label><label class="field-label">Início<input type="date" name="start_date" required value="${editing.start_date}"></label><label class="field-label">Fim<input type="date" name="end_date" required value="${editing.end_date}"></label><label class="check"><input type="checkbox" name="active" ${editing.active ? 'checked' : ''}> Ativa</label><span class="actions"><button class="button button-primary small">Salvar campanha</button><button type="button" class="button button-outline small" data-demo-act="cancel-campaign">Cancelar</button></span></form>` : '';
  const campaigns = c.campaigns.map(x => { const n = c.products.filter(p => p.campaign_id === x.id).map(p => p.productId); const items = [...new Set(n)].map(id => PRODUCTS.find(p => p.id === id)?.name).filter(Boolean); return `<div class="campaign-row"><div><strong>${esc(x.name)}</strong><small>${dateBr(x.start_date)} — ${dateBr(x.end_date)} · ${x.active ? 'Ativa' : 'Pausada'}${items.length ? ' · ' + esc(items.join(', ')) : ' · sem produtos'}</small></div><span class="actions"><button type="button" class="button button-outline small" data-demo-act="edit-campaign" data-id="${esc(x.id)}">Editar campanha</button><button type="button" class="button button-outline small" data-demo-act="toggle-campaign" data-id="${esc(x.id)}">${x.active ? 'Pausar' : 'Ativar'}</button></span></div>`; }).join('');
  return `<section class="platform-panel demo-catalog"><div class="panel-head"><h2>Catálogo e estoque</h2><div class="panel-tools"><label class="field-label">Filial<select data-demo-branch>${BRANCHES.map(b => `<option value="${b.id}" ${b.id === branch ? 'selected' : ''}>${b.name}</option>`).join('')}</select></label><label class="field-label">Buscar item<input data-demo-filter=".admin-product" placeholder="Nome do produto"></label></div></div>
  <p class="muted">Altere preço, unidades, situação e campanha de cada item por filial. Os ajustes ficam neste navegador.</p><div class="admin-products">${rows}</div>
  <section class="campaigns-block"><div class="panel-head"><h2>Campanhas sazonais</h2><button type="button" class="button button-outline" data-demo-act="new-campaign">+ Nova campanha</button></div>${campaignForm}<div class="campaign-list">${campaigns || '<p class="muted">Nenhuma campanha cadastrada.</p>'}</div></section></section>`;
}

/* ---------- eventos ---------- */
let bound = false;
export function bindDemo(ctx) {
  if (bound) return; bound = true;
  const keepScroll = fn => { const y = window.scrollY; fn(); window.scrollTo({ top: y }); };
  const run = (fn, ok) => { try { fn(); ctx.persist(); if (ok) ctx.toast(ok); keepScroll(() => ctx.render()); } catch (e) { ctx.toast(e instanceof Error ? e.message : 'Não foi possível concluir.'); } };
  document.addEventListener('submit', event => {
    const form = event.target.closest?.('[data-demo-form]'); if (!form) return;
    event.preventDefault(); const f = new FormData(form); const account = ctx.account(); const kind = form.dataset.demoForm;
    if (kind === 'coupon-create') run(() => saveCoupon(ctx.state, account?.role, f.get('code'), Number(f.get('percent'))), 'Cupom criado. Ative-o para distribuir.');
    else if (kind === 'stock') run(() => saveStock(ctx.state, account, ctx.PRODUCTS, f.get('productId'), f.get('branchId'), f.get('status'), f.get('qty')), 'Estoque atualizado.');
    else if (kind === 'product') run(() => saveProduct(ctx.state, account, { productId: f.get('productId'), branchId: f.get('branchId'), price: f.get('price'), qty: f.get('qty'), status: f.get('status'), campaign_id: f.get('campaign_id'), hidden: f.get('hidden') === '1' }), 'Alterações salvas.');
    else if (kind === 'campaign') run(() => { saveCampaign(ctx.state, account, { id: f.get('id'), name: f.get('name'), start_date: f.get('start_date'), end_date: f.get('end_date'), active: f.get('active') === 'on' }); ctx.state.demoUi.editCampaign = null; }, 'Campanha salva.');
  });
  document.addEventListener('click', event => {
    const btn = event.target.closest?.('[data-demo-act]'); if (!btn) return; const act = btn.dataset.demoAct; const account = ctx.account(); const ui = ctx.state.demoUi || (ctx.state.demoUi = {});
    if (act === 'toggle-coupon') run(() => toggleCoupon(ctx.state, account?.role, btn.dataset.code));
    else if (act === 'give-coupon') { const sel = btn.parentElement.querySelector('select'); run(() => giveCoupon(ctx.state, account?.role, btn.dataset.code, sel?.value), 'Cupom enviado ao cliente.'); }
    else if (act === 'toggle-hidden') { const form = btn.closest('form'); const input = form.querySelector('[name=hidden]'); input.value = input.value ? '' : '1'; form.requestSubmit(); }
    else if (act === 'new-campaign') { ui.editCampaign = (crypto.randomUUID?.() || `c-${Date.now()}`); ctx.persist(); ctx.render(); }
    else if (act === 'edit-campaign') { ui.editCampaign = btn.dataset.id; ctx.persist(); ctx.render(); }
    else if (act === 'cancel-campaign') { ui.editCampaign = null; ctx.persist(); ctx.render(); }
    else if (act === 'toggle-campaign') run(() => { const c = ctx.state.demoCatalog.campaigns.find(x => x.id === btn.dataset.id); if (c) c.active = !c.active; });
  });
  document.addEventListener('change', event => {
    if (event.target.matches?.('[data-demo-branch]')) { const ui = ctx.state.demoUi || (ctx.state.demoUi = {}); ui.adminBranch = event.target.value; ctx.persist(); keepScroll(() => ctx.render()); }
  });
  document.addEventListener('input', event => {
    const input = event.target.closest?.('[data-demo-filter]'); if (!input) return;
    const q = input.value.trim().toLocaleLowerCase('pt-BR');
    document.querySelectorAll(input.dataset.demoFilter).forEach(el => { el.hidden = Boolean(q) && !(el.dataset.search || '').includes(q); });
  });
}
