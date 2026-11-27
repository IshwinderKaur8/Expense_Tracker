import { api, el, errorText, money, notify, on, state } from './util.js';

const PALETTE = ['#4e79a7', '#f28e2b', '#e15759', '#76b7b2', '#59a14f',
                 '#edc948', '#b07aa1', '#ff9da7', '#9c755f', '#bab0ac'];

const summary = document.querySelector('#summary');
const warnings = document.querySelector('#budget-warnings');
const warningList = document.querySelector('#budget-warning-list');
const charts = document.querySelector('#charts');
const noData = document.querySelector('#no-data');
const tbody = document.querySelector('#category-table tbody');
const categoryCanvas = document.querySelector('#category-chart');
const dailyCanvas = document.querySelector('#daily-chart');

let categoryChart = null;
let dailyChart = null;

async function load() {
  const requestedMonth = state.month;
  try {
    const report = await api(`/reports/monthly?month=${requestedMonth}`);
    if (requestedMonth !== state.month) return;
    render(report);
  } catch (error) {
    notify(errorText(error), true);
  }
}

function statusBadge(status) {
  const normalized = String(status ?? 'NONE').toUpperCase();
  const labels = {
    NONE: 'No budget',
    OK: 'OK',
    WARNING: 'WARNING',
    EXCEEDED: 'EXCEEDED',
  };

  const badge = el('span', {
    class: `budget-status status-${normalized.toLowerCase()}`,
  }, labels[normalized] ?? normalized);

  return badge;
}

function stat(label, value, status = null, percent = null) {
  const content = el('div', { class: 'stat' },
    el('span', { class: 'label' }, label),
    el('strong', {}, value));

  if (percent != null) {
    content.appendChild(
      el('span', { class: 'stat-detail' }, `${Number(percent).toFixed(1)}% used`)
    );
  }

  if (status != null) {
    content.appendChild(statusBadge(status));
  }

  return content;
}

function renderWarnings(lines) {
  const alertLines = lines.filter((line) =>
    ['WARNING', 'EXCEEDED'].includes(String(line.budgetStatus).toUpperCase())
  );

  warningList.replaceChildren(...alertLines.map((line) => {
    const status = String(line.budgetStatus).toUpperCase();
    const percent = Number(line.budgetPercentUsed);

    const message = status === 'EXCEEDED'
      ? `${line.name}: budget exceeded (${percent.toFixed(1)}% used).`
      : `${line.name}: ${percent.toFixed(1)}% of budget used.`;

    return el('li', { class: `alert-${status.toLowerCase()}` },
      statusBadge(status), ' ', message);
  }));

  warnings.hidden = alertLines.length === 0;
}

function render(report) {
  summary.replaceChildren(
    stat('Total spent', money(report.totalSpent)),
    stat(
      'Overall budget',
      report.overallBudget == null ? 'Not set' : money(report.overallBudget),
      report.overallStatus,
      report.overallPercentUsed
    ),
    stat('Remaining', report.remaining == null ? '—' : money(report.remaining))
  );

  renderWarnings(report.byCategory);

  tbody.replaceChildren(...report.byCategory.map((line) => {
    const swatch = el('span', { class: 'swatch' });
    swatch.style.background = line.color ?? '#999999';

    return el('tr', {},
      el('td', {}, swatch, ' ', line.name),
      el('td', { class: 'num' }, money(line.spent)),
      el('td', { class: 'num' }, `${Number(line.percentOfTotal).toFixed(2)}%`),
      el('td', { class: 'num' }, line.budget == null ? '—' : money(line.budget)),
      el('td', {}, statusBadge(line.budgetStatus)));
  }));

  drawCharts(report);
}

function drawCharts(report) {
  categoryChart?.destroy();
  dailyChart?.destroy();

  const spending = report.byCategory.filter((line) => line.spent > 0);
  const hasSpending = spending.length > 0;
  charts.hidden = !hasSpending;
  noData.hidden = hasSpending;
  if (!hasSpending) return;

  categoryChart = new Chart(categoryCanvas, {
    type: 'doughnut',
    data: {
      labels: spending.map((line) => line.name),
      datasets: [{
        data: spending.map((line) => line.spent),
        backgroundColor: spending.map((line, i) => line.color ?? PALETTE[i % PALETTE.length]),
      }],
    },
    options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
  });

  dailyChart = new Chart(dailyCanvas, {
    type: 'bar',
    data: {
      labels: report.dailyTotals.map((day) => Number(day.date.slice(8))),
      datasets: [{
        label: 'Spent per day',
        data: report.dailyTotals.map((day) => day.total),
        backgroundColor: '#4e79a7',
      }],
    },
    options: {
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

on('month-changed', load);
on('data-changed', load);