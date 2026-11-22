import { api, el, emit, errorText, keepSelection, notify, on, state } from './util.js';

const form = document.querySelector('#budget-form');
const select = document.querySelector('#budget-category');
const amount = document.querySelector('#budget-amount');

function fillCategories() {
  const previous = select.value;
  select.replaceChildren(
    el('option', { value: '' }, 'Overall budget'),
    ...state.categories.map((c) => el('option', { value: c.id }, c.name)));
  keepSelection(select, previous);
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    await api('/budgets', {
      method: 'PUT',
      body: JSON.stringify({
        month: state.month,
        categoryId: select.value === '' ? null : Number(select.value),
        amount: Number(amount.value),
      }),
    });
    notify(`Budget saved for ${state.month}`);
    amount.value = '';
    emit('data-changed');
  } catch (error) {
    notify(errorText(error), true);
  }
});

on('categories-changed', fillCategories);