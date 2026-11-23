import './expenses.js';
import './budgets.js';
import './report.js';
import { initAuth } from './auth.js';
import { loadCategories } from './categories.js';
import { emit, errorText, notify, state } from './util.js';

const monthInput = document.querySelector('#month');
monthInput.value = state.month;

monthInput.addEventListener('change', () => {
  if (!monthInput.value) return;                 // the user cleared the field
  state.month = monthInput.value;
  emit('month-changed');
});

async function start() {                         // runs once a login session is confirmed
  try {
    await loadCategories();                      // fills the dropdowns first
  } catch (error) {
    notify(errorText(error), true);
  }
  emit('month-changed');                         // first load of the list and report
}

await initAuth(start);