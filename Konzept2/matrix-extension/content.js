// ===== SELECTION =====
let selectionActive = false;
let startX, startY, endX, endY;
let overlay = null;
let selectionRect = null;

// Listen for messages from popup / background
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'startSelection') {
    startSelectionMode();
    sendResponse({ status: 'Selection mode activated – drag to select' });
  } else if (message.action === 'fillInputs') {
    // Call async fill function and send response after completion
    fillInputsSequentially(message.cells).then(() => {
      sendResponse({ status: 'Filled ' + message.cells.length + ' inputs' });
    });
    return true; // keep channel open for async response
  }
  return true;
});

function startSelectionMode() {
  if (selectionActive) return;
  selectionActive = true;

  overlay = document.createElement('div');
  overlay.style.cssText = `
    position: fixed;
    top: 0; left: 0; width: 100%; height: 100%;
    z-index: 999999;
    cursor: crosshair;
    background: rgba(0,0,0,0.1);
  `;
  document.body.appendChild(overlay);

  selectionRect = document.createElement('div');
  selectionRect.style.cssText = `
    position: fixed;
    border: 2px dashed #00f;
    background: rgba(0,0,255,0.15);
    pointer-events: none;
    display: none;
    z-index: 1000000;
  `;
  document.body.appendChild(selectionRect);

  overlay.addEventListener('mousedown', onMouseDown);
  overlay.addEventListener('mousemove', onMouseMove);
  overlay.addEventListener('mouseup', onMouseUp);
}

function onMouseDown(e) {
  startX = e.clientX + window.scrollX;
  startY = e.clientY + window.scrollY;
  selectionRect.style.display = 'block';
  selectionRect.style.left = e.clientX + 'px';
  selectionRect.style.top = e.clientY + 'px';
  selectionRect.style.width = '0px';
  selectionRect.style.height = '0px';
}

function onMouseMove(e) {
  if (!selectionRect.style.display || selectionRect.style.display === 'none') return;
  const left = Math.min(e.clientX, startX - window.scrollX);
  const top = Math.min(e.clientY, startY - window.scrollY);
  const width = Math.abs(e.clientX - (startX - window.scrollX));
  const height = Math.abs(e.clientY - (startY - window.scrollY));
  selectionRect.style.left = left + 'px';
  selectionRect.style.top = top + 'px';
  selectionRect.style.width = width + 'px';
  selectionRect.style.height = height + 'px';

  endX = e.clientX + window.scrollX;
  endY = e.clientY + window.scrollY;
}

function onMouseUp(e) {
  overlay.removeEventListener('mousedown', onMouseDown);
  overlay.removeEventListener('mousemove', onMouseMove);
  overlay.removeEventListener('mouseup', onMouseUp);
  overlay.remove();
  selectionRect.remove();
  selectionActive = false;

  const x1 = Math.min(startX, endX);
  const y1 = Math.min(startY, endY);
  const x2 = Math.max(startX, endX);
  const y2 = Math.max(startY, endY);

  if (x2 - x1 < 5 || y2 - y1 < 5) {
    alert('Selection too small. Please drag a larger rectangle.');
    return;
  }

  chrome.runtime.sendMessage({
    action: 'processSelection',
    data: { x1, y1, x2, y2 }
  });
}

// ===== SEQUENTIAL FILLING WITH DELAY =====
const DELAY_MS = 200; // adjustable – 200ms between each input fill

async function fillInputsSequentially(cells) {
  for (let i = 0; i < cells.length; i++) {
    const { x, y, text } = cells[i];
    // Convert page to viewport coordinates
    const viewX = x - window.scrollX;
    const viewY = y - window.scrollY;
    const el = document.elementFromPoint(viewX, viewY);
    if (el && el.tagName === 'INPUT') {
      el.value = text;
      el.dispatchEvent(new Event('input', { bubbles: true }));
      // Optional: highlight the filled input briefly
      el.style.backgroundColor = '#2a3a5a';
      setTimeout(() => { el.style.backgroundColor = ''; }, 300);
    }
    // Wait before the next one
    await sleep(DELAY_MS);
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}