import { api, el, emit, errorText, notify, state } from './util.js';

const form = document.querySelector('#category-form');
const nameInput = document.querySelector('#category-name');
const colorInput = document.querySelector('#category-color');
const list = document.querySelector('#category-list');

export async function loadCategories() {
  state.categories = await api('/categories');
  render();
  emit('categories-changed');
}

function render() {
  if (state.categories.length === 0) {
    list.replaceChildren(el('li', {}, 'No categories yet. Add one below.'));
    return;
  }
  list.replaceChildren(...state.categories.map((category) => {
    const swatch = el('span', { class: 'swatch' });
    swatch.style.background = category.color ?? '#999999';
    return el('li', {},
      swatch,
      el('span', { class: 'grow' }, category.name),
      el('button', { type: 'button', onclick: () => rename(category) }, 'Rename'),
      el('button', { type: 'button', class: 'danger', onclick: () => remove(category) }, 'Delete'));
  }));
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    await api('/categories', {
      method: 'POST',
      body: JSON.stringify({ name: nameInput.value, color: colorInput.value }),
    });
    form.reset();
    notify('Category added');
    await loadCategories();
  } catch (error) {
    notify(errorText(error), true);
  }
});

async function rename(category) {
  const name = prompt('New name for this category:', category.name);
  if (name === null || name.trim() === '') return;
  try {
    await api(`/categories/${category.id}`, {
      method: 'PUT',
      body: JSON.stringify({ name, color: category.color }),
    });
    notify('Category renamed');
    await loadCategories();
    emit('data-changed');                       // names appear in the report and list
  } catch (error) {
    notify(errorText(error), true);
  }
}

async function remove(category) {
  if (!confirm(`Delete category "${category.name}"?`)) return;
  try {
    await api(`/categories/${category.id}`, { method: 'DELETE' });
    notify('Category deleted');
    await loadCategories();
    emit('data-changed');                       // its budgets were removed too
  } catch (error) {
    notify(errorText(error), true);             // e.g. 409: category has expenses
  }
}