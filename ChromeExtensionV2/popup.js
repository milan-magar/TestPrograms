document.getElementById('fillBtn').addEventListener('click', async () => {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const region = document.getElementById('regionSelect').value;

  // Inject content script if needed
  await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    files: ['content.js']
  });

  // Send message to content script with the chosen region
  chrome.tabs.sendMessage(tab.id, {
    action: 'startFill',
    region: region
  });

  document.getElementById('status').textContent = '🔴 Click a text box on the page.';
});