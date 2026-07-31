document.getElementById('selectBtn').addEventListener('click', async () => {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const statusEl = document.getElementById('status');

  try {
    // Try to send message – if content script is alive, this works
    await chrome.tabs.sendMessage(tab.id, { action: 'startSelection' });
    statusEl.textContent = 'Selection started';
  } catch (err) {
    // Content script not loaded → inject it now
    await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      files: ['content.js']
    });
    // Retry sending the message
    await chrome.tabs.sendMessage(tab.id, { action: 'startSelection' });
    statusEl.textContent = 'Selection started';
  }
});