import { api, el, errorText, notify, state, todayLocal, on } from './util.js';

const form = document.querySelector('#subscription-form');
const amountInput = document.querySelector('#subscription-amount');
const categorySelect = document.querySelector('#subscription-category');
const frequencySelect = document.querySelector('#subscription-frequency');
const startDateInput = document.querySelector('#subscription-start-date');
const descriptionInput = document.querySelector('#subscription-description');
const tableBody = document.querySelector('#subscription-table tbody');

let subscriptions = [];

startDateInput.value = todayLocal();

function populateCategories() {
  const previousValue = categorySelect.value;

  categorySelect.replaceChildren(
    ...state.categories.map((category) =>
      el('option', { value: category.id }, category.name)
    )
  );

  if ([...categorySelect.options].some((option) => option.value === previousValue)) {
    categorySelect.value = previousValue;
  }
}

function categoryName(id) {
  return state.categories.find((category) => category.id === id)?.name ?? 'Unknown';
}

function render() {
  if (subscriptions.length === 0) {
    tableBody.replaceChildren(
      el('tr', {},
        el('td', { colspan: '7' }, 'No subscriptions yet.')
      )
    );
    return;
  }

  tableBody.replaceChildren(...subscriptions.map((subscription) => {
    const status = subscription.active ? 'Active' : 'Paused';

    const pauseResumeButton = el('button', {
      type: 'button',
      onclick: () => toggleStatus(subscription),
    }, subscription.active ? 'Pause' : 'Resume');

    const deleteButton = el('button', {
      type: 'button',
      class: 'danger',
      onclick: () => remove(subscription),
    }, 'Delete');

    return el('tr', {},
      el('td', {}, subscription.description || 'Subscription'),
      el('td', {}, categoryName(subscription.categoryId)),
      el('td', { class: 'num' }, Number(subscription.amount).toFixed(2)),
      el('td', {}, subscription.frequency),
      el('td', {}, subscription.nextDueDate),
      el('td', {}, status),
      el('td', {}, pauseResumeButton, deleteButton)
    );
  }));
}

async function loadSubscriptions() {
  try {
    subscriptions = await api('/subscriptions');
    render();
  } catch (error) {
    notify(errorText(error), true);
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  try {
    await api('/subscriptions', {
      method: 'POST',
      body: JSON.stringify({
        amount: Number(amountInput.value),
        categoryId: Number(categorySelect.value),
        frequency: frequencySelect.value,
        startDate: startDateInput.value,
        description: descriptionInput.value.trim() || null,
      }),
    });

    form.reset();
    startDateInput.value = todayLocal();
    notify('Subscription added');
    await loadSubscriptions();
  } catch (error) {
    notify(errorText(error), true);
  }
});

async function toggleStatus(subscription) {
  const action = subscription.active ? 'pause' : 'resume';

  try {
    await api(`/subscriptions/${subscription.id}/${action}`, {
      method: 'PATCH',
    });

    notify(`Subscription ${action}d`);
    await loadSubscriptions();
  } catch (error) {
    notify(errorText(error), true);
  }
}

async function remove(subscription) {
  if (!confirm(`Delete "${subscription.description || 'Subscription'}"?`)) return;

  try {
    await api(`/subscriptions/${subscription.id}`, {
      method: 'DELETE',
    });

    notify('Subscription deleted');
    await loadSubscriptions();
  } catch (error) {
    notify(errorText(error), true);
  }
}

export async function initSubscriptions() {
  populateCategories();
  await loadSubscriptions();
}

on('categories-changed', populateCategories);