import { api, csrfHeaders, errorText, notify, on } from './util.js';

const authView = document.querySelector('#auth-view');
const appView = document.querySelector('#app-view');
const appBar = document.querySelector('#app-bar');
const usernameLabel = document.querySelector('#username');
const loginCard = document.querySelector('#login-card');
const registerCard = document.querySelector('#register-card');
const loginForm = document.querySelector('#login-form');
const registerForm = document.querySelector('#register-form');

function showLoginCard(show) {
  loginCard.hidden = !show;
  registerCard.hidden = show;
}

function showApp(user) {
  usernameLabel.textContent = user.username;
  authView.hidden = true;
  appView.hidden = false;
  appBar.hidden = false;
}

function showAuth() {
  authView.hidden = false;
  appView.hidden = true;
  appBar.hidden = true;
}

async function fetchCurrentUser() {
  try {
    const response = await fetch('/api/v1/auth/me');
    return response.ok ? await response.json() : null;
  } catch {
    notify('Cannot reach the server. Is the application running?', true);
    return null;
  }
}

async function login(username, password) {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...csrfHeaders() },
    body: new URLSearchParams({ username, password }),
  });
  return response.ok;
}

/** Wires up login/register/logout. Calls onAuthenticated() whenever a session is confirmed. */
export async function initAuth(onAuthenticated) {
  async function enter() {
    const user = await fetchCurrentUser();      // also hands the browser a fresh CSRF cookie
    if (!user) {
      showAuth();
      return;
    }
    showApp(user);
    await onAuthenticated();
  }

  document.querySelector('#show-register').addEventListener('click', () => showLoginCard(false));
  document.querySelector('#show-login').addEventListener('click', () => showLoginCard(true));

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      const username = loginForm.querySelector('#login-username').value;
      const password = loginForm.querySelector('#login-password').value;
      if (!(await login(username, password))) {
        notify('Wrong username or password', true);
        return;
      }
      loginForm.reset();
      await enter();
    } catch (error) {
      notify(errorText(error), true);
    }
  });

  registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    try {
      const username = registerForm.querySelector('#register-username').value;
      const password = registerForm.querySelector('#register-password').value;
      await api('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) });
      await login(username, password);
      registerForm.reset();
      notify('Account created');
      await enter();
    } catch (error) {
      notify(errorText(error), true);
    }
  });

  document.querySelector('#logout').addEventListener('click', async () => {
    try {
      await fetch('/api/v1/auth/logout', { method: 'POST', headers: csrfHeaders() });
    } finally {
      location.reload();             // clears every in-memory trace of the previous user
    }
  });

  on('unauthorized', () => {
    notify('Your session has expired. Please log in again.', true);
    setTimeout(() => location.reload(), 1500);
  });

  await enter();
}