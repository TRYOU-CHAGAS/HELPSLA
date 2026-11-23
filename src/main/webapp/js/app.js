const state = { user: null, csrf: null, tickets: [], categories: [], slas: [] };
const labels = { SOLICITANTE: 'Solicitante', TECNICO: 'Técnico', ADMIN: 'Administrador', BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica', ABERTO: 'Aberto', EM_TRIAGEM: 'Em triagem', EM_ATENDIMENTO: 'Em atendimento', AGUARDANDO_SOLICITANTE: 'Aguardando solicitante', RESOLVIDO: 'Resolvido', ENCERRADO: 'Encerrado', CANCELADO: 'Cancelado' };
const notificationTypes = { CHAMADO_NOVO: 'Chamado novo disponível', CHAMADO_ASSUMIDO: 'Técnico assumiu o chamado', STATUS_ALTERADO: 'Mudança de status', NOVA_MENSAGEM: 'Nova mensagem', CHAMADO_RESOLVIDO: 'Chamado resolvido' };
const $ = (selector) => document.querySelector(selector);

async function api(path, options = {}) {
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) };
  if (options.method && options.method !== 'GET' && state.csrf) headers['X-CSRF-Token'] = state.csrf;
  const response = await fetch(`api/${path}`, { ...options, headers });
  const data = response.status === 204 ? null : await response.json();
  if (response.status === 401) { sessionStorage.removeItem('helpsla-csrf'); window.location.href = 'login.html'; throw new Error('Sessão encerrada.'); }
  if (!response.ok) throw new Error(data?.erro || 'Não foi possível concluir a solicitação.');
  return data;
}

function escapeHtml(value = '') {
  return String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}
function fmtDate(value) { return value ? new Date(value).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—'; }
function currentDeadline(ticket) {
  if (['ABERTO', 'EM_TRIAGEM'].includes(ticket.status)) return ticket.prazoSla;
  if (['EM_ATENDIMENTO', 'AGUARDANDO_SOLICITANTE'].includes(ticket.status)) return ticket.prazoResolucaoSla;
  return null;
}
function isOverdue(ticket) { const deadline = currentDeadline(ticket); return Boolean(deadline && new Date(deadline) < new Date()); }
function remaining(value) {
  if (!value) return 'Aguardando aceite do técnico';
  let ms = new Date(value).getTime() - Date.now();
  if (!Number.isFinite(ms)) return 'Prazo indisponível';
  const late = ms < 0; ms = Math.abs(ms);
  const days = Math.floor(ms / 86400000); const hours = Math.floor(ms % 86400000 / 3600000);
  const minutes = Math.floor(ms % 3600000 / 60000); const seconds = Math.floor(ms % 60000 / 1000);
  const clock = `${days ? `${days}d ` : ''}${String(hours).padStart(2, '0')}h ${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s`;
  return late ? `Atrasado há ${clock}` : clock;
}
function countdown(label, date) {
  if (!date) return `${label}: aguardando aceite do técnico`;
  return `${label}: <strong data-countdown="${escapeHtml(date)}">${remaining(date)}</strong>`;
}
function updateCountdowns() {
  document.querySelectorAll('[data-countdown]').forEach((el) => { el.textContent = remaining(el.dataset.countdown); });
}
function ticketCard(t) {
  const late = isOverdue(t);
  const timerLabel = ['ABERTO', 'EM_TRIAGEM'].includes(t.status) ? 'Aceitar em' : ['EM_ATENDIMENTO', 'AGUARDANDO_SOLICITANTE'].includes(t.status) ? 'Resolver em' : 'Prazo';
  const timerValue = currentDeadline(t);
  const timerMarkup = timerValue ? `<span class="ticket-countdown ${late ? 'overdue' : ''}" data-countdown="${escapeHtml(timerValue)}">${remaining(timerValue)}</span>` : '—';
  return `<article class="ticket-card" data-ticket="${t.id}" tabindex="0"><div><div class="ticket-code">${escapeHtml(t.codigo)}</div><div class="ticket-title">${escapeHtml(t.titulo)}</div><div class="ticket-meta">${escapeHtml(t.categoriaNome)} · ${fmtDate(t.abertoEm)}</div></div><span class="badge status-${t.status}">${labels[t.status] || t.status}</span><div class="ticket-date ${late ? 'overdue' : ''}"><small>${timerLabel}</small>${timerMarkup}</div><span class="badge priority-${t.prioridade}">${labels[t.prioridade] || t.prioridade}</span></article>`;
}
function toast(message) { const el = $('#toast'); el.textContent = message; el.classList.add('show'); setTimeout(() => el.classList.remove('show'), 2800); }
function showView(name) {
  document.querySelectorAll('.view').forEach((el) => el.classList.toggle('hidden', el.id !== `view-${name}`));
  document.querySelectorAll('.nav-item').forEach((el) => el.classList.toggle('active', el.dataset.view === name));
  if (name === 'tickets') renderTickets($('#ticket-list'));
  if (name === 'new-ticket') loadCategories();
  if (name === 'settings') loadAdmin();
  if (name === 'notifications') loadNotifications();
}

async function loadNotifications() {
  try {
    const [avisos, preferencias] = await Promise.all([api('notificacoes'), api('notificacoes/preferencias')]);
    $('#notification-options').innerHTML = Object.entries(notificationTypes).map(([tipo, titulo]) =>
      `<label><input type="checkbox" name="tipos" value="${tipo}" ${preferencias.tipos.includes(tipo) ? 'checked' : ''}> ${titulo}</label>`).join('');
    $('#notification-list').innerHTML = avisos.length ? avisos.map((aviso) =>
      `<article class="sla-row"><div><strong>${escapeHtml(aviso.titulo)}</strong><small>${escapeHtml(aviso.mensagem)} · ${fmtDate(aviso.criadaEm)}</small></div>${aviso.lida ? '<span class="badge">Lida</span>' : `<button class="button secondary" data-notification-read="${escapeHtml(aviso.id)}">Marcar como lida</button>`}</article>`).join('')
      : '<div class="empty-state">Você ainda não recebeu notificações.</div>';
  } catch (error) { toast(error.message); }
}

async function loadTickets() {
  state.tickets = await api('chamados');
  renderTickets($('#ticket-list'));
  renderTickets($('#recent-tickets'), state.tickets.slice(0, 5));
  const open = state.tickets.filter((t) => !['RESOLVIDO', 'ENCERRADO', 'CANCELADO'].includes(t.status)).length;
  $('#metric-open').textContent = open;
  $('#metric-done').textContent = state.tickets.filter((t) => t.status === 'RESOLVIDO' || t.status === 'ENCERRADO').length;
  $('#metric-late').textContent = state.tickets.filter(isOverdue).length;
}
function renderTickets(container, tickets = state.tickets) {
  if (!container) return;
  container.innerHTML = tickets.length ? tickets.map(ticketCard).join('') : '<div class="empty-state">Nenhum chamado encontrado.</div>';
}
async function loadCategories() {
  const [categories, slas] = await Promise.all([api('chamados/categorias'), api('chamados/slas')]);
  state.categories = categories;
  $('#ticket-category').innerHTML = state.categories.map((c) => `<option value="${c.id}">${escapeHtml(c.nome)}</option>`).join('');
  const prioritySelect = $('#ticket-priority');
  prioritySelect.innerHTML = slas.map((s) => `<option value="${s.prioridade}">${labels[s.prioridade]} · aceite ${formatDuration(s.prazoAceiteMinutos)} / solução ${formatDuration(s.prazoMinutos)}</option>`).join('');
}

async function openDetail(id) {
  const data = await api(`chamados/${id}`);
  const t = data.chamado;
  const canTech = state.user.perfil === 'TECNICO' && !['RESOLVIDO', 'ENCERRADO', 'CANCELADO'].includes(t.status);
  const canClose = state.user.perfil === 'SOLICITANTE' && t.status === 'RESOLVIDO';
  const canCancel = state.user.perfil === 'SOLICITANTE' && state.user.id === t.solicitanteId && !['RESOLVIDO', 'ENCERRADO', 'CANCELADO'].includes(t.status);
  const canRate = state.user.perfil === 'SOLICITANTE' && t.status === 'ENCERRADO';
  const conversaEncerrada = ['ENCERRADO', 'CANCELADO'].includes(t.status);
  const canMessage = !conversaEncerrada && (
    (state.user.perfil === 'SOLICITANTE' && state.user.id === t.solicitanteId)
    || (state.user.perfil === 'TECNICO' && state.user.id === t.tecnicoId && state.user.prestadoraId === t.prestadoraId)
  );
  let actions = '';
  if (canTech && ['ABERTO', 'EM_TRIAGEM'].includes(t.status) && !t.tecnicoId) actions += `<button class="button primary" data-action="assumir" data-id="${t.id}">Assumir chamado</button>`;
  if (canTech && t.tecnicoId === state.user.id && ['EM_ATENDIMENTO', 'AGUARDANDO_SOLICITANTE'].includes(t.status)) {
    if (t.status === 'AGUARDANDO_SOLICITANTE') actions += `<button class="button secondary" data-action="status" data-value="EM_ATENDIMENTO" data-id="${t.id}">Retomar atendimento</button>`;
    actions += `<button class="button secondary" data-action="status" data-value="AGUARDANDO_SOLICITANTE" data-id="${t.id}">Aguardar solicitante</button><button class="button primary" data-action="solucao" data-id="${t.id}">Registrar solução</button>`;
  }
  if (canClose) actions += `<button class="button primary" data-action="encerrar" data-id="${t.id}">Confirmar encerramento</button>`;
  if (canCancel) actions += `<button class="button danger" data-action="cancelar" data-id="${t.id}">Cancelar chamado</button>`;
  if (canRate) actions += `<button class="button primary" data-action="avaliar" data-id="${t.id}">Avaliar atendimento</button>`;
  const history = (data.historico || []).map((h) => `<div class="history-item"><strong>${escapeHtml(labels[h.acao] || h.acao.replaceAll('_', ' '))}</strong> · ${escapeHtml(h.ator)} <small>${fmtDate(h.data)}</small>${h.observacao ? `<div>${escapeHtml(h.observacao)}</div>` : ''}</div>`).join('');
  const messages = (data.mensagens || []).map((m) => {
    const own = Number(m.autorId) === Number(state.user.id);
    return `<article class="chat-message ${own ? 'mine' : ''}"><div class="chat-message-meta"><strong>${escapeHtml(m.autorNome)}</strong><span>${labels[m.autorPerfil] || escapeHtml(m.autorPerfil)} · ${fmtDate(m.criadoEm)}</span></div><p>${escapeHtml(m.mensagem)}</p></article>`;
  }).join('');
  const chatForm = canMessage
    ? `<form id="message-form" class="message-form" data-ticket-id="${t.id}"><label for="ticket-message-input">Escreva uma mensagem</label><textarea id="ticket-message-input" name="mensagem" rows="3" maxlength="2000" required placeholder="Digite sua mensagem para o outro participante..."></textarea><div class="form-actions"><button class="button primary" type="submit">Enviar mensagem</button></div><p class="form-message" data-message-feedback role="status"></p></form>`
    : `<p class="hint">${conversaEncerrada ? 'A conversa foi encerrada com o chamado.' : state.user.perfil === 'TECNICO' && !t.tecnicoId ? 'Assuma o chamado para participar da conversa.' : 'A conversa fica disponível para o solicitante e o técnico responsável.'}</p>`;
  const activeTimers = ['ABERTO', 'EM_TRIAGEM'].includes(t.status)
    ? countdown('Tempo para o técnico aceitar', t.prazoSla)
    : ['EM_ATENDIMENTO', 'AGUARDANDO_SOLICITANTE'].includes(t.status)
      ? countdown('Tempo para resolver após o aceite', t.prazoResolucaoSla)
      : `Aberto em: ${fmtDate(t.abertoEm)}`;
  const cancelReason = t.status === 'CANCELADO' ? (data.historico || []).find((h) => h.acao === 'CANCELADO')?.observacao : '';
  $('#ticket-detail').innerHTML = `<div class="detail-head"><span class="badge priority-${t.prioridade}">${labels[t.prioridade]}</span><span class="badge status-${t.status}">${labels[t.status]}</span><h2>${escapeHtml(t.titulo)}</h2><div class="ticket-code">${escapeHtml(t.codigo)} · ${escapeHtml(t.categoriaNome)}</div></div><div class="detail-grid"><div class="detail-cell"><small>Solicitante</small>${escapeHtml(t.solicitanteNome)}</div><div class="detail-cell"><small>Técnico responsável</small>${escapeHtml(t.tecnicoNome || 'Ainda não atribuído')}</div><div class="detail-cell"><small>Aberto em</small>${fmtDate(t.abertoEm)}</div><div class="detail-cell ${isOverdue(t) ? 'overdue' : ''}"><small>${['ABERTO', 'EM_TRIAGEM'].includes(t.status) ? 'Prazo para aceite' : 'Prazo para solução'}</small>${fmtDate(currentDeadline(t))}</div><div class="detail-cell countdown-cell">${activeTimers}</div>${t.prazoResolucaoSla ? `<div class="detail-cell"><small>Tempo dado após o aceite</small>${fmtDate(t.prazoResolucaoSla)}</div>` : ''}</div><h3>Descrição</h3><p class="detail-description">${escapeHtml(t.descricao)}</p>${t.solucao ? `<h3>Solução registrada</h3><p class="detail-description">${escapeHtml(t.solucao)}</p>` : ''}${cancelReason ? `<h3>Motivo do cancelamento</h3><p class="detail-description">${escapeHtml(cancelReason)}</p>` : ''}<div class="detail-actions">${actions}</div><section class="conversation"><div class="section-heading"><div><h3>Conversa do chamado</h3><p class="muted">Mensagens do solicitante e do técnico responsável.</p></div></div><div class="chat-message-list" id="chat-message-list" aria-live="polite">${messages || '<p class="empty-state">Nenhuma mensagem ainda. Comece a conversa.</p>'}</div>${chatForm}</section><div class="history-list"><h3>Histórico de alterações</h3>${history || '<p class="muted">Sem alterações registradas.</p>'}</div>`;
  const chatList = $('#chat-message-list');
  if (chatList) chatList.scrollTop = chatList.scrollHeight;
  updateCountdowns();
  $('#ticket-dialog').showModal();
}

async function loadAdmin() {
  const [slas] = await Promise.all([api('admin/slas'), api('admin/categorias')]);
  state.slas = slas;
  $('#sla-list').innerHTML = slas.map((s) => `<div class="sla-row"><div><strong>${escapeHtml(s.servico)} · ${labels[s.prioridade]}</strong><small>${s.ativa ? 'Regra ativa' : 'Regra anterior'} · aceite ${formatDuration(s.prazoAceiteMinutos)}</small></div><span class="badge">Resolver ${formatDuration(s.prazoMinutos)}</span></div>`).join('');
  const report = await api('admin/relatorios');
  $('#metric-open').textContent = report.abertos; $('#metric-done').textContent = report.resolvidos; $('#metric-late').textContent = report.atrasados;
}
function formatDuration(minutes) {
  if (minutes % 1440 === 0) return `${minutes / 1440} dia(s)`;
  if (minutes % 60 === 0) return `${minutes / 60} hora(s)`;
  return `${minutes} minuto(s)`;
}

document.addEventListener('click', async (event) => {
  const aviso = event.target.closest('[data-notification-read]');
  if (aviso) {
    try { await api(`notificacoes/${encodeURIComponent(aviso.dataset.notificationRead)}`, { method: 'PATCH' }); await loadNotifications(); }
    catch (error) { toast(error.message); }
    return;
  }
  const viewButton = event.target.closest('[data-view]');
  if (viewButton) { showView(viewButton.dataset.view); return; }
  const ticket = event.target.closest('[data-ticket]');
  if (ticket) { try { await openDetail(ticket.dataset.ticket); } catch (e) { toast(e.message); } return; }
  const action = event.target.closest('[data-action]');
  if (!action) return;
  try {
    const id = action.dataset.id;
    if (action.dataset.action === 'solucao') {
      const valor = window.prompt('Descreva a solução aplicada (mínimo de 5 caracteres):');
      if (!valor) return;
      await api(`chamados/${id}/solucao`, { method: 'POST', body: JSON.stringify({ valor }) });
    } else if (action.dataset.action === 'status') {
      await api(`chamados/${id}/status`, { method: 'PATCH', body: JSON.stringify({ valor: action.dataset.value }) });
    } else if (action.dataset.action === 'avaliar') {
      const nota = Number(window.prompt('Dê uma nota de 1 a 5 para o atendimento:'));
      if (!nota) return;
      const comentario = window.prompt('Comentário (opcional):') || '';
      await api(`chamados/${id}/avaliacao`, { method: 'POST', body: JSON.stringify({ nota, comentario }) });
    } else if (action.dataset.action === 'cancelar') {
      const motivo = window.prompt('Por que deseja cancelar o chamado? (mínimo de 5 caracteres)');
      if (motivo === null) return;
      if (motivo.trim().length < 5) { toast('Informe um motivo com pelo menos 5 caracteres.'); return; }
      await api(`chamados/${id}/cancelar`, { method: 'POST', body: JSON.stringify({ valor: motivo.trim() }) });
    } else {
      await api(`chamados/${id}/${action.dataset.action}`, { method: 'POST' });
    }
    $('#ticket-dialog').close(); await loadTickets(); toast('Ação concluída com sucesso.');
  } catch (error) { toast(error.message); }
});

document.addEventListener('submit', async (event) => {
  const form = event.target;
  if (!form.matches('#message-form')) return;
  event.preventDefault();
  const id = form.dataset.ticketId;
  const textarea = form.elements.mensagem;
  const feedback = form.querySelector('[data-message-feedback]');
  const mensagem = textarea.value.trim();
  if (!mensagem) { feedback.textContent = 'Escreva uma mensagem antes de enviar.'; return; }
  try {
    await api(`chamados/${id}/mensagens`, { method: 'POST', body: JSON.stringify({ valor: mensagem }) });
    $('#ticket-dialog').close();
    await openDetail(id);
    toast('Mensagem enviada.');
  } catch (error) { feedback.textContent = error.message; }
});

$('#ticket-form').addEventListener('submit', async (event) => {
  event.preventDefault(); const message = $('#ticket-message'); message.textContent = 'Enviando chamado...';
  try {
    const result = await api('chamados', { method: 'POST', body: JSON.stringify({ titulo: $('#ticket-title').value, descricao: $('#ticket-description').value, categoriaId: $('#ticket-category').value, prioridade: $('#ticket-priority').value }) });
    event.target.reset(); message.textContent = `${result.mensagem} Número: ${result.id}`; message.classList.add('success'); await loadTickets(); toast('Chamado aberto.');
  } catch (error) { message.textContent = error.message; message.classList.remove('success'); }
});

$('#sla-form').addEventListener('submit', async (event) => {
  event.preventDefault(); const message = $('#sla-message');
  try { await api('admin/slas', { method: 'POST', body: JSON.stringify({ prioridade: $('#sla-priority').value, prazoAceiteMinutos: Number($('#sla-accept-minutes').value), prazoMinutos: Number($('#sla-minutes').value) }) }); message.textContent = 'Regra SLA atualizada.'; message.classList.add('success'); await loadAdmin(); }
  catch (error) { message.textContent = error.message; message.classList.remove('success'); }
});
$('#category-form').addEventListener('submit', async (event) => {
  event.preventDefault(); const message = $('#category-message');
  try { await api('admin/categorias', { method: 'POST', body: JSON.stringify({ nome: $('#category-name').value, descricao: $('#category-description').value }) }); event.target.reset(); message.textContent = 'Categoria adicionada.'; message.classList.add('success'); }
  catch (error) { message.textContent = error.message; message.classList.remove('success'); }
});
$('#notification-preferences').addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = $('#notification-preferences-message');
  const tipos = [...event.currentTarget.querySelectorAll('input[name="tipos"]:checked')].map((checkbox) => checkbox.value);
  try {
    await api('notificacoes/preferencias', { method: 'PUT', body: JSON.stringify({ tipos }) });
    message.textContent = 'Preferências salvas.'; message.classList.add('success');
  } catch (error) { message.textContent = error.message; message.classList.remove('success'); }
});
$('#logout').addEventListener('click', async () => { try { await api('logout', { method: 'POST' }); } finally { sessionStorage.removeItem('helpsla-csrf'); window.location.href = 'login.html'; } });

async function iniciar() {
  try {
    const data = await api('me'); state.user = data.usuario; state.csrf = data.csrf;
    sessionStorage.setItem('helpsla-csrf', state.csrf);
    $('#user-name').textContent = state.user.nome; $('#user-role').textContent = labels[state.user.perfil]; $('#welcome-title').textContent = `Olá, ${state.user.nome.split(' ')[0]}!`;
    document.querySelectorAll('.requester-only').forEach((e) => e.classList.toggle('hidden', state.user.perfil !== 'SOLICITANTE'));
    document.querySelectorAll('.admin-only').forEach((e) => e.classList.toggle('hidden', state.user.perfil !== 'ADMIN'));
    if (state.user.perfil !== 'ADMIN') { $('#metric-grid').classList.add('hidden'); }
    else { $('#metric-grid').classList.remove('hidden'); }
    if (state.user.perfil === 'TECNICO') $('#welcome-title').textContent = 'Fila de atendimento';
    await loadTickets();
    if (state.user.perfil === 'ADMIN') await loadAdmin();
  } catch (error) {
    if (!window.location.pathname.endsWith('login.html')) window.location.href = 'login.html';
  }
}
iniciar();
setInterval(updateCountdowns, 1000);
