import {
  api, el, emit, errorText, keepSelection, money, monthRange, notify, on, state, todayLocal,
} from './util.js';

const PAGE_SIZE = 10;

const form = document.querySelector('#expense-form');
const amount = document.querySelector('#expense-amount');
const category = document.querySelector('#expense-category');
const date = document.querySelector('#expense-date');
const description = document.querySelector('#expense-description');
const submitButton = document.querySelector('#expense-submit');
const cancelButton = document.querySelector('#expense-cancel');
const title = document.querySelector('#expense-form-title');
const tbody = document.querySelector('#expense-table tbody');
const pageInfo = document.querySelector('#page-info');
const prevButton = document.querySelector('#prev-page');
const nextButton = document.querySelector('#next-page');

let page = 0;
let totalPages = 0;
let editingId = null;

function resetForm() {
  editingId = null;
  form.reset();
  date.value = todayLocal();
  date.max = todayLocal();                      // the API also rejects future dates
  title.textContent = 'Add expense';
  submitButton.textContent = 'Add';
  cancelButton.hidden = true;
}

function fillCategories() {
  const previous = category.value;
  const options = state.categories.map((c) => el('option', { value: c.id }, c.name));
  category.replaceChildren(
    ...(options.length ? options : [el('option', { value: '' }, 'Create a category first')]));
  keepSelection(category, previous);
}

async function load() {
  const requestedMonth = state.month;
  const { from, to } = monthRange(requestedMonth);
  try {
    const result = await api(
      `/expenses?from=${from}&to=${to}&page=${page}&size=${PAGE_SIZE}`);
    if (requestedMonth !== state.month) return;              // stale answer, user moved on

    if (result.items.length === 0 && page > 0) {             // e.g. deleted the last row of the last page
      page = Math.max(result.totalPages - 1, 0);
      return load();
    }
    render(result);
  } catch (error) {
    notify(errorText(error), true);
  }
}

function render(result) {
  totalPages = result.totalPages;
  if (result.items.length === 0) {
    tbody.replaceChildren(el('tr', {}, el('td', { colspan: 5 }, 'No expenses in this month.')));
  } else {
    tbody.replaceChildren(...result.items.map((expense) => el('tr', {},
      el('td', {}, expense.expenseDate),
      el('td', {}, expense.categoryName),
      el('td', {}, expense.description ?? ''),
      el('td', { class: 'num' }, money(expense.amount)),
      el('td', { class: 'actions' },
        el('button', { type: 'button', onclick: () => startEdit(expense) }, 'Edit'),
        el('button', { type: 'button', class: 'danger', onclick: () => remove(expense) }, 'Delete')))));
  }
  pageInfo.textContent = totalPages === 0 ? '' : `Page ${page + 1} of ${totalPages}`;
  prevButton.disabled = page === 0;
  nextButton.disabled = page + 1 >= totalPages;
}

function startEdit(expense) {
  editingId = expense.id;
  amount.value = expense.amount;
  category.value = expense.categoryId;
  date.value = expense.expenseDate;
  description.value = expense.description ?? '';
  title.textContent = 'Edit expense';
  submitButton.textContent = 'Save changes';
  cancelButton.hidden = false;
  form.scrollIntoView({ behavior: 'smooth' });
  amount.focus();
}

async function remove(expense) {
  const label = expense.description || expense.categoryName;
  if (!confirm(`Delete ${money(expense.amount)} (${label}) on ${expense.expenseDate}?`)) return;
  try {
    await api(`/expenses/${expense.id}`, { method: 'DELETE' });
    notify('Expense deleted');
    if (editingId === expense.id) resetForm();
    emit('data-changed');
  } catch (error) {
    notify(errorText(error), true);
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const body = JSON.stringify({
    amount: Number(amount.value),
    categoryId: Number(category.value),
    expenseDate: date.value,
    description: description.value,
  });
  try {
    if (editingId) {
      await api(`/expenses/${editingId}`, { method: 'PUT', body });
      notify('Expense updated');
    } else {
      await api('/expenses', { method: 'POST', body });
      notify('Expense added');
    }
    resetForm();
    emit('data-changed');
  } catch (error) {
    notify(errorText(error), true);
  }
});

cancelButton.addEventListener('click', resetForm);
prevButton.addEventListener('click', () => { page -= 1; load(); });
nextButton.addEventListener('click', () => { page += 1; load(); });

on('categories-changed', fillCategories);
on('month-changed', () => { page = 0; load(); });
on('data-changed', load);

resetForm();