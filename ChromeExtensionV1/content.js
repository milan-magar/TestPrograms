let isSelectionMode = false;

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'startSelection') {
    startSelectionMode();
  }
});

function startSelectionMode() {
  if (isSelectionMode) return;
  isSelectionMode = true;

  document.body.style.cursor = 'crosshair';

  const highlight = (e) => {
    e.target.style.outline = '3px solid #ff5722';
    e.target.style.outlineOffset = '2px';
  };
  const unhighlight = (e) => {
    e.target.style.outline = '';
    e.target.style.outlineOffset = '';
  };

  const onClick = (e) => {
    e.preventDefault();
    e.stopPropagation();

    const selectedElement = e.target;

    // Clean up
    document.removeEventListener('mouseover', highlight);
    document.removeEventListener('mouseout', unhighlight);
    document.removeEventListener('click', onClick, true);
    document.body.style.cursor = 'default';
    isSelectionMode = false;

    // 🔥 FIXED: click exactly 5 times — only ONE event per iteration
    for (let i = 0; i < 5; i++) {
      setTimeout(() => {
        const evt = new MouseEvent('click', {
          view: window,
          bubbles: true,
          cancelable: true
        });
        selectedElement.dispatchEvent(evt);
      }, i * 100); // 100ms apart
    }

    console.log(`✅ 5 clicks sent to`, selectedElement);
  };

  document.addEventListener('mouseover', highlight);
  document.addEventListener('mouseout', unhighlight);
  document.addEventListener('click', onClick, true);

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