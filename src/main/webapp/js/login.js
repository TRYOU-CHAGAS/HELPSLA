const loginForm = document.querySelector('#login-form');
const loginMessage = document.querySelector('#login-message');

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  loginMessage.textContent = 'Entrando...';
  loginMessage.classList.remove('success');
  try {
    const response = await fetch('api/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: loginForm.email.value, senha: loginForm.senha.value })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.erro || 'Não foi possível entrar.');
    sessionStorage.setItem('helpsla-csrf', data.csrf);
    window.location.href = 'index.html';
  } catch (error) {
    loginMessage.textContent = error.message;
  }
});
