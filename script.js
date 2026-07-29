// DOM references
const userForm = document.getElementById('userForm');
const nameInput = document.getElementById('nameInput');
const formMessage = document.getElementById('formMessage');
const userList = document.getElementById('userList');
const refreshBtn = document.getElementById('refreshBtn');

// ---------- Helper: show a message under the form ----------
function setFormMessage(text, isError = false) {
  formMessage.textContent = text;
  formMessage.className = 'message' + (isError ? ' error' : '');
  // Clear after 4 seconds
  clearTimeout(window.messageTimeout);
  window.messageTimeout = setTimeout(() => {
    formMessage.textContent = '';
    formMessage.className = 'message';
  }, 4000);
}

// ---------- Fetch and render all users ----------
async function loadUsers() {
  try {
    const response = await fetch('/users');
    if (!response.ok) throw new Error('Failed to fetch users');
    const users = await response.json();
    renderUsers(users);
  } catch (err) {
    console.error(err);
    userList.innerHTML = `<li>❌ Could not load users</li>`;
  }
}

function renderUsers(users) {
  if (users.length === 0) {
    userList.innerHTML = '<li>No users yet.</li>';
    return;
  }
  userList.innerHTML = users.map(user =>
    `<li><span><span class="user-id">#${user.id}</span> ${user.name}</span></li>`
  ).join('');
}

// ---------- Add a new user (POST) ----------
async function addUser(name) {
  try {
    const response = await fetch('/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });

    const data = await response.json();

    if (!response.ok) {
      // Server responded with an error (e.g., 400)
      setFormMessage(`❌ ${data.error || 'Something went wrong'}`, true);
      return false;
    }

    setFormMessage(`✅ User "${data.name}" added (ID: ${data.id})`);
    return true;
  } catch (err) {
    setFormMessage('❌ Network error – is the server running?', true);
    console.error(err);
    return false;
  }
}

// ---------- Event: form submit ----------
userForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = nameInput.value.trim();
  if (!name) {
    setFormMessage('Please enter a name', true);
    return;
  }

  const success = await addUser(name);
  if (success) {
    nameInput.value = '';          // clear input
    await loadUsers();             // refresh the list
  }
});

// ---------- Event: refresh button ----------
refreshBtn.addEventListener('click', loadUsers);

// ---------- Initial load ----------
loadUsers();
// ... (existing code)

// ---------- SSE: listen for server‑pushed updates ----------
function setupSSE() {
  const eventSource = new EventSource('/events');

  eventSource.onmessage = (event) => {
    // The server sends a message when a new user is added
    console.log('SSE message:', event.data);
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'newUser') {
        // Refresh the user list automatically
        loadUsers();
      }
    } catch (e) {
      // Ignore non‑JSON messages
    }
  };

  eventSource.onerror = (err) => {
    console.error('SSE connection error:', err);
    // Optionally try to reconnect after a delay
    setTimeout(setupSSE, 3000);
  };
}

// Start listening for SSE updates
setupSSE();

// ... (existing loadUsers, addUser, etc.)