chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'processSelection') {
    const { x1, y1, x2, y2 } = message.data;
    fetch('http://localhost:3000/select', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ x1, y1, x2, y2 })
    })
    .then(res => res.json())
    .then(data => {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        chrome.tabs.sendMessage(tabs[0].id, {
          action: 'fillInputs',
          cells: data.cells
        });
      });
    })
    .catch(err => {
      console.error('Server error:', err);
      alert('Failed to contact server. Make sure it runs on localhost:3000');
    });
    return true;
  }
});