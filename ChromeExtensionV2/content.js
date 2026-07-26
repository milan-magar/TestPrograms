let isSelectionMode = false;

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'startFill') {
    startFillMode(message.region);
  }
});

function startFillMode(region) {
  if (isSelectionMode) return;
  isSelectionMode = true;

  document.body.style.cursor = 'crosshair';

  const highlight = (e) => {
    const el = e.target;
    if (el.matches('input, textarea')) {
      el.style.outline = '3px solid #2563eb';
      el.style.outlineOffset = '2px';
      el.style.backgroundColor = '#f0f7ff';
    }
  };
  const unhighlight = (e) => {
    const el = e.target;
    if (el.matches('input, textarea')) {
      el.style.outline = '';
      el.style.outlineOffset = '';
      el.style.backgroundColor = '';
    }
  };

  const onClick = (e) => {
    const el = e.target;
    if (!el.matches('input, textarea')) {
      return; // keep selection mode active
    }

    e.preventDefault();
    e.stopPropagation();

    // Clean up listeners
    document.removeEventListener('mouseover', highlight);
    document.removeEventListener('mouseout', unhighlight);
    document.removeEventListener('click', onClick, true);
    document.body.style.cursor = 'default';
    isSelectionMode = false;

    // 1) Focus the element
    el.click();
    el.focus();

    // 2) Set the value
    el.value = 'Milan Magar';

    // 3) Fire input/change events to notify the page
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));

    // 4) Simulate pressing the Enter key
    simulateEnterKey(el);

    console.log(`✅ Filled with "Milan Magar" and pressed Enter (region: ${region})`);
  };

  document.addEventListener('mouseover', highlight);
  document.addEventListener('mouseout', unhighlight);
  document.addEventListener('click', onClick, true);

  // Cancel with Escape
  const cancelOnEscape = (e) => {
    if (e.key === 'Escape') {
      document.removeEventListener('mouseover', highlight);
      document.removeEventListener('mouseout', unhighlight);
      document.removeEventListener('click', onClick, true);
      document.removeEventListener('keydown', cancelOnEscape);
      document.body.style.cursor = 'default';
      isSelectionMode = false;
      console.log('Selection cancelled.');
    }
  };
  document.addEventListener('keydown', cancelOnEscape);
}

/**
 * Simulate a full Enter key press (down, press, up)
 * so that page‑side listeners (like onkeydown) react.
 */
function simulateEnterKey(element) {
  const eventTypes = ['keydown', 'keypress', 'keyup'];
  for (const type of eventTypes) {
    const ev = new KeyboardEvent(type, {
      key: 'Enter',
      code: 'Enter',
      keyCode: 13,
      which: 13,
      bubbles: true,
      cancelable: true,
      view: window,
      // These are sometimes needed for legacy sites
      charCode: type === 'keypress' ? 13 : 0,
    });
    element.dispatchEvent(ev);
  }
}