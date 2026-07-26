document.getElementById('selectBtn').addEventListener('click', async () => {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

  // Inject the content script if not already (or send a message)
  await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    files: ['content.js']
  });

  // Tell content script to start selection mode
  chrome.tabs.sendMessage(tab.id, { action: 'startSelection' });

  // Update status
  document.getElementById('status').textContent = '🔴 Selection mode active – click an element.';
});