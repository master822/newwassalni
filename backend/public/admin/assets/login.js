(() => {
  const form = document.getElementById('loginForm');
  const button = document.getElementById('loginButton');
  const error = document.getElementById('error');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;

    const identity = document.getElementById('identity').value.trim();
    const password = document.getElementById('password').value;
    if (!identity || !password) {
      error.textContent = 'يرجى إدخال بيانات الدخول.';
      error.hidden = false;
      return;
    }

    button.disabled = true;
    button.textContent = 'جارٍ التحقق...';

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: identity, password })
      });
      const data = await response.json().catch(() => ({}));

      if (!response.ok || !data.success) {
        throw new Error(data.error || 'بيانات الدخول غير صحيحة.');
      }

      const role = data.user?.role;
      if (role !== 'ADMIN' && role !== 'SUPER_ADMIN') {
        throw new Error('هذا الحساب لا يملك صلاحية دخول لوحة الإدارة.');
      }

      sessionStorage.setItem('wassalni_admin_access_token', data.accessToken);
      sessionStorage.setItem('wassalni_admin_user', JSON.stringify(data.user));
      window.location.replace('/admin/dashboard.html');
    } catch (err) {
      error.textContent = err.message || 'تعذر تسجيل الدخول.';
      error.hidden = false;
    } finally {
      button.disabled = false;
      button.textContent = 'دخول إلى لوحة التحكم';
    }
  });
})();
