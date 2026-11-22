// ---------- dates (LOCAL time, never toISOString, which is UTC) ----------
function pad(n) { return String(n).padStart(2, '0'); }

export function todayLocal() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function currentMonth() { return todayLocal().slice(0, 7); }

export function monthRange(month) {                    // "2026-11" -> from/to dates
  const [year, m] = month.split('-').map(Number);
  const lastDay = new Date(year, m, 0).getDate();      // day 0 of next month = last day
  return { from: `${month}-01`, to: `${month}-${pad(lastDay)}` };
}

// ---------- shared state and tiny event bus ----------
// (declared after the date helpers, because currentMonth() uses them)
export const state = { month: currentMonth(), categories: [] };

const bus = new EventTarget();
export const emit = (name) => bus.dispatchEvent(new Event(name));
export const on = (name, handler) => bus.addEventListener(name, handler);

// ---------- API access ----------
export class ApiError extends Error {
  constructor(message, details = []) {
    super(message);
    this.details = details;
  }
}

export async function api(path, options = {}) {
  const response = await fetch('/api/v1' + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (response.status === 204) return null;                       // DELETE: no body
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(body?.message ?? `Request failed (${response.status})`, body?.details ?? []);
  }
  return body;
}

export function errorText(error) {
  if (error instanceof ApiError) {
    const details = error.details.map((d) => `${d.field} ${d.message}`).join('; ');
    return details ? `${error.message}: ${details}` : error.message;
  }
  return 'Cannot reach the server. Is the application running?';
}

// ---------- display helpers ----------
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
export const money = (value) => inr.format(value);

// Builds a DOM element safely: strings become TEXT nodes, never parsed as HTML.
export function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value);
  }
  node.append(...children);
  return node;
}

export function notify(text, isError = false) {
  const box = document.querySelector('#message');
  box.textContent = text;
  box.className = isError ? 'error' : 'success';
  box.hidden = false;
  clearTimeout(notify.timer);
  notify.timer = setTimeout(() => { box.hidden = true; }, 5000);
}

// Keep the user's selection when a <select> is rebuilt, if that option still exists
export function keepSelection(select, previousValue) {
  if ([...select.options].some((o) => o.value === previousValue)) {
    select.value = previousValue;
  }
}