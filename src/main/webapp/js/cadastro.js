const form = document.querySelector('#register-form');
const message = document.querySelector('#register-message');
form.addEventListener('submit', async (event) => {
  event.preventDefault(); message.textContent = 'Criando sua conta...';
  try {
    const response = await fetch('api/cadastro', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nome: form.nome.value,
        email: form.email.value,
        senha: form.senha.value,
        notificacoes: [...form.querySelectorAll('input[name="notificacoes"]:checked')].map((item) => item.value)
      })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.erro || 'Não foi possível criar a conta.');
    message.textContent = `${data.mensagem} Redirecionando para o login...`; message.classList.add('success');
    setTimeout(() => { window.location.href = 'login.html'; }, 1000);
  } catch (error) { message.textContent = error.message; message.classList.remove('success'); }
});
