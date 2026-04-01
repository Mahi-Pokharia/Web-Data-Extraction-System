/**
 * WebExtract — app.js
 * Handles: API calls, result rendering, link filtering, CSV export.
 */

/* ── State ──────────────────────────────────────────────────────────────── */
let currentData  = null;   // Holds the last successful WebData response
let allLinks     = [];     // Unfiltered link list for client-side filter

/* ── DOM refs ───────────────────────────────────────────────────────────── */
const urlInput    = document.getElementById('urlInput');
const scrapeBtn   = document.getElementById('scrapeBtn');
const statusBar   = document.getElementById('statusBar');
const statusText  = document.getElementById('statusText');
const errorBanner = document.getElementById('errorBanner');
const errorCode   = document.getElementById('errorCode');
const errorMsg    = document.getElementById('errorMsg');
const resultsEl   = document.getElementById('results');

/* ── Event listeners ────────────────────────────────────────────────────── */
scrapeBtn.addEventListener('click', startScrape);
urlInput.addEventListener('keydown', e => { if (e.key === 'Enter') startScrape(); });

/* ── Core: scrape workflow ──────────────────────────────────────────────── */
async function startScrape() {
  const url = urlInput.value.trim();

  if (!url) {
    showError('VALIDATION', 'Please enter a URL before clicking Extract.');
    return;
  }

  dismissError();
  setLoading(true, 'Connecting to target…');
  hideResults();

  try {
    const apiUrl  = `/api/scrape?url=${encodeURIComponent(url)}`;
    const res     = await fetch(apiUrl);
    const payload = await res.json();

    if (!res.ok) {
      // Backend returned a structured error body
      showError(
        payload.error  || `HTTP ${res.status}`,
        payload.message || 'An unknown error occurred.'
      );
      return;
    }

    // Success — render results
    currentData = payload;
    renderResults(payload);

  } catch (err) {
    // Network-level failure (e.g. Spring Boot not running)
    showError('NETWORK_ERROR', 'Could not reach the backend. Is Spring Boot running on port 8080?');
    console.error('[WebExtract]', err);
  } finally {
    setLoading(false);
  }
}

/* ── Render results ─────────────────────────────────────────────────────── */
function renderResults(data) {

  // Meta bar
  const metaUrl = document.getElementById('metaUrl');
  metaUrl.textContent = data.url;
  metaUrl.href        = data.url;

  // Stat chips
  document.getElementById('statTitle').textContent    = truncate(data.title || '(no title)', 28);
  document.getElementById('statHeadings').textContent = data.headings.length;
  document.getElementById('statLinks').textContent    = data.links.length;

  // Title card
  document.getElementById('titleContent').textContent = data.title || '(no title found)';

  // Headings list
  const headingsList = document.getElementById('headingsList');
  const headingCount = document.getElementById('headingCount');
  headingsList.innerHTML = '';
  headingCount.textContent = data.headings.length;

  if (data.headings.length === 0) {
    headingsList.appendChild(emptyItem('No headings found on this page'));
  } else {
    data.headings.forEach(h => {
      const li = document.createElement('li');
      // Expect format "[H1] Some text" — extract level for color-coding
      const match = h.match(/^\[([A-Z0-9]+)\]\s*(.*)/);
      if (match) {
        li.dataset.level = match[1];
        li.textContent   = h;
      } else {
        li.textContent = h;
      }
      headingsList.appendChild(li);
    });
  }

  // Links list
  allLinks = data.links;
  renderLinks(data.links);

  // Show section
  resultsEl.classList.remove('hidden');
  resultsEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderLinks(links) {
  const linksList  = document.getElementById('linksList');
  const linkCount  = document.getElementById('linkCount');
  linksList.innerHTML = '';
  linkCount.textContent = links.length;

  if (links.length === 0) {
    linksList.appendChild(emptyItem('No links found (or all were filtered)'));
    return;
  }

  links.forEach(href => {
    const li = document.createElement('li');
    const a  = document.createElement('a');
    a.href   = href;
    a.target = '_blank';
    a.rel    = 'noopener noreferrer';
    a.title  = href;
    a.textContent = href;
    li.appendChild(a);
    linksList.appendChild(li);
  });
}

function emptyItem(msg) {
  const li  = document.createElement('li');
  li.style.color  = 'var(--text-dim)';
  li.style.border = 'none';
  li.style.background = 'transparent';
  li.textContent  = msg;
  return li;
}

/* ── Link filter ────────────────────────────────────────────────────────── */
function filterLinks() {
  const q = document.getElementById('linkFilter').value.toLowerCase();
  const filtered = q
    ? allLinks.filter(l => l.toLowerCase().includes(q))
    : allLinks;
  renderLinks(filtered);
}

/* ── Export to CSV ──────────────────────────────────────────────────────── */
function exportCSV() {
  if (!currentData) return;

  const rows = [];

  // Header row
  rows.push(['Type', 'Value']);

  // URL
  rows.push(['URL', currentData.url]);

  // Title
  rows.push(['Title', currentData.title || '']);

  // Headings
  (currentData.headings || []).forEach(h => rows.push(['Heading', h]));

  // Links
  (currentData.links || []).forEach(l => rows.push(['Link', l]));

  const csv     = rows.map(r => r.map(csvEscape).join(',')).join('\r\n');
  const blob    = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const objUrl  = URL.createObjectURL(blob);
  const anchor  = document.createElement('a');

  anchor.href     = objUrl;
  anchor.download = `webextract-${sanitizeFilename(currentData.url)}.csv`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(objUrl);
}

/** Escape a CSV cell value (RFC 4180). */
function csvEscape(val) {
  const str = String(val ?? '');
  if (str.includes(',') || str.includes('"') || str.includes('\n')) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

/** Strip non-filename characters from a URL for use as a download name. */
function sanitizeFilename(url) {
  return url.replace(/^https?:\/\//, '').replace(/[^a-z0-9]/gi, '-').slice(0, 60);
}

/* ── UI helpers ─────────────────────────────────────────────────────────── */
function setLoading(on, message = '') {
  scrapeBtn.disabled = on;
  if (on) {
    statusText.textContent = message;
    statusBar.classList.remove('hidden');
  } else {
    statusBar.classList.add('hidden');
  }
}

function showError(code, message) {
  errorCode.textContent = code;
  errorMsg.textContent  = message;
  errorBanner.classList.remove('hidden');
}

function dismissError() {
  errorBanner.classList.add('hidden');
}

function hideResults() {
  resultsEl.classList.add('hidden');
  currentData = null;
  allLinks    = [];
}

function clearResults() {
  hideResults();
  dismissError();
  urlInput.value = '';
  urlInput.focus();
  document.getElementById('linkFilter').value = '';
}

function truncate(str, max) {
  return str.length > max ? str.slice(0, max) + '…' : str;
}
