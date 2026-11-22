/* Contas reais de cliente (pessoa fisica / empresa) conversando com a API Java.
 * Perfis de demonstracao (@demo.maison.test) continuam locais; aqui tudo e salvo no servidor. */

const STATUS = { SCHEDULED: 'scheduled', PREPARING: 'preparing', DELIVERING: 'delivering', CANCELLED: 'cancelled' };
const dateBr = iso => String(iso || '').split('-').reverse().join('/');

/** Chamada autenticada. Sessao expirada (401 com token) limpa o login local. */
export async function call(ctx, path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = ctx.state.session?.token; if (token) headers.Authorization = `Bearer ${token}`;
  let response;
  try { response = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), cache: 'no-store' }); }
  catch { throw new Error('Não foi possível falar com o servidor. Tente novamente.'); }
  let data = null; if (response.status !== 204) { try { data = await response.json(); } catch { data = null; } }
  if (!response.ok) {
    if (response.status === 401 && token && !path.startsWith('/auth/login')) { ctx.state.session = null; ctx.state.realOrders = []; ctx.state.realCoupons = []; ctx.persist(); }
    const error = new Error(data?.erro || (response.status === 429 ? 'Muitas tentativas. Aguarde alguns minutos.' : `Erro ${response.status}.`)); error.status = response.status; throw error;
  }
  return data;
}

export const user = ctx => ctx.state.session?.user || null;
export const asAccount = ctx => { const u = user(ctx); return u ? { id: `real-${u.id}`, name: u.nome, email: u.email, role: u.tipo === 'PJ' ? 'company' : 'customer', branchId: ctx.state.branchId, real: true } : null; };

export function mapOrder(o) {
  return {
    id: o.id, real: true, total: Number(o.total), method: o.metodo, date: dateBr(o.data), time: o.horario, address: o.endereco || '', branchId: o.filialId,
    status: STATUS[o.status] || 'scheduled', coupon: o.cupom || '', discount: Number(o.desconto || 0), fee: Number(o.taxaEntrega || 0), payment: o.pagamento,
    items: (o.itens || []).map(i => ({ id: i.produtoId || String(i.id), name: i.nome, description: i.descricao || '', image: i.imagem || '', price: Number(i.preco), quantity: i.quantidade })),
    messages: (o.mensagens || []).map(m => ({ text: m.texto, customer: m.autor === 'CLIENTE' })),
    review: o.avaliacao ? { rating: o.avaliacao.nota, text: o.avaliacao.texto || '' } : null,
  };
}

async function afterLogin(ctx) { await Promise.allSettled([loadOrders(ctx), loadCoupons(ctx)]); }

export async function register(ctx, data) {
  const result = await call(ctx, '/auth/register', { method: 'POST', body: data });
  ctx.state.session = { token: result.token, user: result.usuario }; ctx.state.demoAccountId = null; ctx.persist(); await afterLogin(ctx); return result.usuario;
}
export async function login(ctx, email, senha) {
  const result = await call(ctx, '/auth/login', { method: 'POST', body: { email, senha } });
  ctx.state.session = { token: result.token, user: result.usuario }; ctx.state.demoAccountId = null; ctx.persist(); await afterLogin(ctx); return result.usuario;
}
export async function logout(ctx) {
  try { await call(ctx, '/auth/logout', { method: 'POST' }); } catch { /* sessao ja invalida */ }
  ctx.state.session = null; ctx.state.realOrders = []; ctx.state.realCoupons = []; ctx.persist();
}
/** Ao abrir o site: confirma que o token salvo ainda vale e recarrega pedidos/cupons. */
export async function restore(ctx) {
  if (!ctx.state.session?.token) return false;
  try { ctx.state.session.user = await call(ctx, '/auth/me'); ctx.persist(); await afterLogin(ctx); return true; } catch { return false; }
}
export async function loadOrders(ctx) { const list = await call(ctx, '/pedidos'); ctx.state.realOrders = list.map(mapOrder); ctx.persist(); return ctx.state.realOrders; }
export async function loadCoupons(ctx) { ctx.state.realCoupons = await call(ctx, '/meus-cupons'); ctx.persist(); return ctx.state.realCoupons; }
export async function loadReviews(ctx) { try { const list = await call(ctx, '/avaliacoes'); ctx.state.remoteReviews = list.map(r => ({ name: r.nome, rating: r.nota, text: r.texto || '', date: String(r.data || '').slice(0, 10) })); } catch { ctx.state.remoteReviews = ctx.state.remoteReviews || []; } }

export async function placeOrder(ctx, { branchId, method, date, time, address, payment, coupon, items }) {
  const body = {
    filialId: branchId, metodo: method, data: date, horario: time, endereco: method === 'delivery' ? address : undefined, pagamento: payment, cupom: coupon || undefined,
    itens: items.map(i => ({ produtoId: String(i.productId || i.id), nome: i.name, descricao: i.description || '', imagem: i.image || '', preco: Number(i.price), quantidade: Number(i.quantity), preparo: i.preparation || null, bolo: i.cake || undefined })),
  };
  const order = await call(ctx, '/pedidos', { method: 'POST', body });
  await loadOrders(ctx); return order;
}
export async function sendMessage(ctx, orderId, text) { await call(ctx, `/pedidos/${encodeURIComponent(orderId)}/mensagens`, { method: 'POST', body: { texto: text } }); await loadOrders(ctx); }
export async function review(ctx, orderId, rating, text) { await call(ctx, '/avaliacoes', { method: 'POST', body: { pedidoId: orderId, nota: rating, texto: text || undefined } }); await Promise.all([loadOrders(ctx), loadReviews(ctx)]); }

const esc = v => String(v ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));

/** Campos extras exibidos so no cadastro. */
export const signupFields = () => `<div class="signup-extra" hidden>
  <label class="field-label">Tipo de conta<select name="tipo"><option value="PF">Pessoa física</option><option value="PJ">Empresa</option></select></label>
  <label class="field-label"><span data-doc-label>CPF</span><input name="documento" inputmode="numeric" maxlength="18" autocomplete="off" placeholder="000.000.000-00"></label>
  <label class="field-label">Telefone com DDD<input name="telefone" inputmode="tel" maxlength="20" autocomplete="tel" placeholder="(11) 99999-0000"></label>
  <label class="field-label">Endereço completo<input name="endereco" maxlength="200" autocomplete="street-address" placeholder="Rua, número, bairro, cidade"></label></div>`;

/** Tela "Meu cadastro" de conta real. */
export function profileMarkup(ctx) {
  const u = user(ctx); const coupons = ctx.state.realCoupons || [];
  return `<section class="platform-panel"><h2>Seus dados</h2><p class="muted">Cadastro salvo com segurança no servidor da Maison.</p><dl class="profile-list"><div><dt>Nome</dt><dd>${esc(u.nome)}</dd></div><div><dt>E-mail</dt><dd>${esc(u.email)}</dd></div><div><dt>${u.tipo === 'PJ' ? 'CNPJ' : 'CPF'}</dt><dd>${esc(u.documentoMascarado)}</dd></div><div><dt>Telefone</dt><dd>${esc(u.telefone)}</dd></div><div><dt>Endereço</dt><dd>${esc(u.endereco)}</dd></div></dl><button class="button button-outline" data-action="logout">Sair da conta</button></section>
  <section class="platform-panel demo-coupons" aria-label="Cupons"><h2>🎟 Meus cupons</h2>${coupons.length ? `<div class="coupon-wallet">${coupons.map(c => `<div class="coupon-ticket ${c.ativo ? '' : 'is-off'}"><strong>${esc(c.codigo)}</strong><span>${c.percentual}% de desconto</span><small>${c.ativo ? 'Digite este código no carrinho' : 'Cupom inativo no momento'}</small></div>`).join('')}</div>` : '<p class="muted">Você ainda não tem cupons.</p>'}</section>`;
}
