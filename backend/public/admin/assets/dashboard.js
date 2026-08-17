(() => {
  const token = sessionStorage.getItem('wassalni_admin_access_token');
  const savedUser = JSON.parse(sessionStorage.getItem('wassalni_admin_user') || 'null');
  if (!token || !savedUser || !['ADMIN', 'SUPER_ADMIN'].includes(savedUser.role)) {
    window.location.replace('/admin/login.html');
    return;
  }

  const $ = (id) => document.getElementById(id);
  const esc = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  $('adminName').textContent = `${savedUser.name || 'مدير النظام'} — ${savedUser.role}`;
  $('roleValue').textContent = savedUser.role === 'SUPER_ADMIN' ? 'مدير أعلى' : 'مدير';

  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('Authorization', `Bearer ${token}`);
    if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(`/api/admin${path}`, {...options, headers});
    const data = await response.json().catch(() => ({}));
    if (response.status === 401 || response.status === 403) {
      sessionStorage.clear();
      window.location.replace('/admin/login.html');
      throw new Error(data.error || 'انتهت جلسة الإدارة.');
    }
    if (!response.ok || data.success === false) throw new Error(data.error || 'تعذر تنفيذ العملية.');
    return data;
  }

  function showError(err) {
    $('statusText').textContent = err.message || 'حدث خطأ';
    $('statusText').className = 'danger';
  }

  async function loadUsers() {
    const search = encodeURIComponent(($('userSearch').value || '').trim());
    const data = await api(`/users${search ? `?search=${search}` : ''}`);
    const rows = data.data || [];
    $('usersCount').textContent = rows.length;
    $('usersBody').innerHTML = rows.map(u => `<tr><td>${esc(u.name)}</td><td>${esc(u.email)}</td><td>${esc(u.phone)}</td><td><span class="badge">${esc(u.role)}</span></td><td>${esc(u.wallet_points)}</td><td>${u.is_suspended ? '<span class="danger">موقوف</span>' : '<span class="success">نشط</span>'}</td></tr>`).join('') || '<tr><td colspan="6" class="empty">لا توجد نتائج</td></tr>';
  }

  async function loadRides() {
    const data = await api('/rides');
    const rows = data.data || [];
    $('ridesCount').textContent = rows.length;
    $('ridesBody').innerHTML = rows.map(r => `<tr><td>${esc(r.driver_name || r.driver_id)}</td><td>${esc(r.start_city)}</td><td>${esc(r.end_city)}</td><td>${esc(r.departure_date)}</td><td>${esc(r.status)}</td><td>${r.status !== 'CANCELLED' ? `<button class="action" data-cancel-ride="${esc(r.id)}">إلغاء</button>` : '—'}</td></tr>`).join('') || '<tr><td colspan="6" class="empty">لا توجد رحلات</td></tr>';
  }

  async function loadTopups() {
    const data = await api('/topup-requests?status=PENDING');
    const rows = data.data || [];
    $('topupsCount').textContent = rows.length;
    $('topupsBody').innerHTML = rows.map(t => `<tr><td>${esc(t.user_name || t.user_id)}</td><td>${esc(t.package_points)}</td><td>$${esc(t.package_price_usd)}</td><td><span class="badge">${esc(t.status)}</span></td><td><button class="action" data-approve="${esc(t.id)}">قبول</button> <button class="action danger" data-reject="${esc(t.id)}">رفض</button></td></tr>`).join('') || '<tr><td colspan="5" class="empty">لا توجد طلبات معلقة</td></tr>';
  }

  async function loadOverview() {
    try {
      await Promise.all([loadUsers(), loadRides(), loadTopups()]);
      $('statusText').textContent = 'متصل';
      $('statusText').className = 'muted';
    } catch (err) { showError(err); }
  }

  document.querySelectorAll('.nav button').forEach(button => button.addEventListener('click', async () => {
    document.querySelectorAll('.nav button').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    button.classList.add('active');
    const section = button.dataset.section;
    $(section).classList.add('active');
    $('pageTitle').textContent = button.textContent;
    try {
      if (section === 'users') await loadUsers();
      if (section === 'rides') await loadRides();
      if (section === 'topups') await loadTopups();
    } catch (err) { showError(err); }
  }));

  $('refreshUsers').addEventListener('click', () => loadUsers().catch(showError));
  $('refreshRides').addEventListener('click', () => loadRides().catch(showError));
  $('refreshTopups').addEventListener('click', () => loadTopups().catch(showError));
  $('userSearch').addEventListener('keydown', e => { if (e.key === 'Enter') loadUsers().catch(showError); });

  $('usersBody').addEventListener('click', () => {});
  $('ridesBody').addEventListener('click', async (event) => {
    const id = event.target.dataset.cancelRide;
    if (!id) return;
    const reason = prompt('سبب إلغاء الرحلة:') || 'قرار إداري';
    if (!confirm('هل تريد إلغاء هذه الرحلة؟')) return;
    try { await api(`/rides/${encodeURIComponent(id)}`, {method:'DELETE', body:JSON.stringify({reason})}); await loadRides(); } catch (err) { showError(err); }
  });

  $('topupsBody').addEventListener('click', async (event) => {
    const approveId = event.target.dataset.approve;
    const rejectId = event.target.dataset.reject;
    try {
      if (approveId && confirm('تأكيد قبول طلب الشحن؟')) {
        await api(`/topup-requests/${encodeURIComponent(approveId)}/approve`, {method:'POST'});
        await loadTopups();
      }
      if (rejectId && confirm('تأكيد رفض طلب الشحن؟')) {
        const reason = prompt('سبب الرفض:') || 'لم يتم التحقق من الدفع';
        await api(`/topup-requests/${encodeURIComponent(rejectId)}/reject`, {method:'POST', body:JSON.stringify({reason})});
        await loadTopups();
      }
    } catch (err) { showError(err); }
  });

  $('logout').addEventListener('click', () => {
    sessionStorage.removeItem('wassalni_admin_access_token');
    sessionStorage.removeItem('wassalni_admin_user');
    window.location.replace('/admin/login.html');
  });

  loadOverview();
})();
