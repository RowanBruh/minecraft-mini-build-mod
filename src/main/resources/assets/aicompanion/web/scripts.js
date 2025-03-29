// AI Companion Web Interface Script

// Global variables
let authToken = null;
let webSocket = null;
let selectedCompanionId = null;

// DOM References
const loginScreen = document.getElementById('login-screen');
const dashboard = document.getElementById('dashboard');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const navLinks = document.querySelectorAll('.nav-link');
const panels = document.querySelectorAll('.panel');
const logoutLink = document.getElementById('logout-link');
const statusIndicator = document.querySelector('.status-indicator');
const statusText = document.getElementById('status-text');
const companionList = document.getElementById('companion-list');
const selectedCompanionDropdowns = document.querySelectorAll('#selected-companion, #inventory-companion, #settings-companion');

// Initialize the application
function init() {
    // Attach event listeners
    attachEventListeners();
    
    // Check for existing session
    checkExistingSession();
}

// Attach all event listeners
function attachEventListeners() {
    // Login form submission
    loginForm.addEventListener('submit', handleLogin);
    
    // Navigation links
    navLinks.forEach(link => {
        link.addEventListener('click', handleNavigation);
    });
    
    // Command buttons
    document.querySelectorAll('.command-button').forEach(button => {
        button.addEventListener('click', handleCommandButton);
    });
    
    // Custom command button
    document.getElementById('send-custom-command').addEventListener('click', handleCustomCommand);
    
    // Settings save button
    document.getElementById('save-settings').addEventListener('click', handleSaveSettings);
    
    // Inventory action buttons
    document.getElementById('request-items').addEventListener('click', () => handleInventoryAction('request'));
    document.getElementById('send-items').addEventListener('click', () => handleInventoryAction('send'));
}

// Handle login form submission
function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    loginError.style.display = 'none';
    
    // Send login request to the server
    fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Login failed');
        }
        return response.json();
    })
    .then(data => {
        authToken = data.token;
        localStorage.setItem('authToken', authToken);
        showDashboard();
        initWebSocket();
        loadCompanions();
    })
    .catch(error => {
        console.error('Login error:', error);
        loginError.style.display = 'block';
    });
}

// Check for existing token and try to reconnect
function checkExistingSession() {
    const token = localStorage.getItem('authToken');
    if (token) {
        authToken = token;
        // Validate token
        fetch('/api/auth/validate', {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        })
        .then(response => {
            if (response.ok) {
                showDashboard();
                initWebSocket();
                loadCompanions();
            } else {
                localStorage.removeItem('authToken');
            }
        })
        .catch(error => {
            console.error('Token validation error:', error);
            localStorage.removeItem('authToken');
        });
    }
}

// Initialize WebSocket connection
function initWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws?token=${authToken}`;
    
    webSocket = new WebSocket(wsUrl);
    
    webSocket.onopen = function() {
        console.log('WebSocket connection established');
        updateConnectionStatus(true);
    };
    
    webSocket.onmessage = function(event) {
        const message = JSON.parse(event.data);
        handleWebSocketMessage(message);
    };
    
    webSocket.onclose = function() {
        console.log('WebSocket connection closed');
        updateConnectionStatus(false);
        // Try to reconnect after 5 seconds
        setTimeout(initWebSocket, 5000);
    };
    
    webSocket.onerror = function(error) {
        console.error('WebSocket error:', error);
        updateConnectionStatus(false);
    };
}

// Handle incoming WebSocket messages
function handleWebSocketMessage(message) {
    console.log('Received message:', message);
    
    switch (message.type) {
        case 'companion_update':
            updateCompanionData(message.data);
            break;
        case 'inventory_update':
            updateInventoryData(message.data);
            break;
        case 'command_response':
            handleCommandResponse(message.data);
            break;
        default:
            console.log('Unknown message type:', message.type);
    }
}

// Handle command response
function handleCommandResponse(data) {
    // Display response to user if needed
    if (data.success) {
        console.log('Command executed successfully:', data.message);
    } else {
        console.error('Command failed:', data.message);
        alert(`Command failed: ${data.message}`);
    }
}

// Load companions from the server
function loadCompanions() {
    fetch('/api/companions', {
        headers: {
            'Authorization': `Bearer ${authToken}`
        }
    })
    .then(response => response.json())
    .then(data => {
        updateCompanionData(data);
    })
    .catch(error => {
        console.error('Error loading companions:', error);
    });
}

// Update companion data in the UI
function updateCompanionData(companions) {
    // Update companion list
    renderCompanionList(companions);
    // Update dropdowns
    updateCompanionDropdowns(companions);
}

// Update inventory data
function updateInventoryData(inventoryData) {
    const inventoryContent = document.getElementById('inventory-content');
    
    if (!inventoryData || !inventoryData.items || inventoryData.items.length === 0) {
        inventoryContent.innerHTML = '<p>No items in inventory</p>';
        return;
    }
    
    let html = '<div class="inventory-grid">';
    
    inventoryData.items.forEach(item => {
        html += `
            <div class="inventory-slot" data-item-id="${item.id}">
                <img src="${item.iconUrl || 'placeholder.png'}" alt="${item.name}">
                <span class="item-name">${item.name}</span>
                <span class="item-count">x${item.count}</span>
            </div>
        `;
    });
    
    html += '</div>';
    inventoryContent.innerHTML = html;
    
    // Add click handlers for inventory slots
    document.querySelectorAll('.inventory-slot').forEach(slot => {
        slot.addEventListener('click', function() {
            const itemId = this.getAttribute('data-item-id');
            selectInventoryItem(itemId);
        });
    });
}

// Select an inventory item
function selectInventoryItem(itemId) {
    // Clear previous selection
    document.querySelectorAll('.inventory-slot.selected').forEach(slot => {
        slot.classList.remove('selected');
    });
    
    // Select new item
    const slot = document.querySelector(`.inventory-slot[data-item-id="${itemId}"]`);
    if (slot) {
        slot.classList.add('selected');
    }
}

// Render the companion list
function renderCompanionList(companions) {
    companionList.innerHTML = '';
    
    if (!companions || companions.length === 0) {
        companionList.innerHTML = '<p>No companions available</p>';
        return;
    }
    
    companions.forEach(companion => {
        const li = document.createElement('li');
        li.className = 'companion-item';
        
        const statusClass = companion.status === 'online' ? 'status-online' : 'status-offline';
        const healthPercent = Math.round((companion.health / companion.maxHealth) * 100);
        
        li.innerHTML = `
            <div class="companion-info">
                <h3>
                    <span class="status-indicator ${statusClass}"></span>
                    ${companion.name}
                </h3>
                <p>Health: ${companion.health}/${companion.maxHealth} (${healthPercent}%)</p>
                <div class="health-bar">
                    <div class="health-bar-fill" style="width: ${healthPercent}%"></div>
                </div>
            </div>
            <div class="companion-actions">
                <button data-id="${companion.id}" class="select-companion">Select</button>
                <button data-id="${companion.id}" class="teleport-companion">Teleport</button>
            </div>
        `;
        
        companionList.appendChild(li);
    });
    
    // Add event listeners to the buttons
    document.querySelectorAll('.select-companion').forEach(button => {
        button.addEventListener('click', function() {
            const companionId = this.getAttribute('data-id');
            selectCompanion(companionId);
        });
    });
    
    document.querySelectorAll('.teleport-companion').forEach(button => {
        button.addEventListener('click', function() {
            const companionId = this.getAttribute('data-id');
            teleportCompanion(companionId);
        });
    });
}

// Update companion dropdowns
function updateCompanionDropdowns(companions) {
    selectedCompanionDropdowns.forEach(dropdown => {
        dropdown.innerHTML = '';
        
        if (!companions || companions.length === 0) {
            dropdown.innerHTML = '<option value="">No companions available</option>';
            return;
        }
        
        companions.forEach(companion => {
            const option = document.createElement('option');
            option.value = companion.id;
            option.textContent = companion.name;
            dropdown.appendChild(option);
        });
    });
}

// Handle navigation click
function handleNavigation(e) {
    e.preventDefault();
    
    if (this.id === 'logout-link') {
        logout();
        return;
    }
    
    const panelId = this.getAttribute('data-panel');
    activatePanel(panelId);
}

// Handle command button click
function handleCommandButton() {
    const command = this.getAttribute('data-command');
    const companionId = document.getElementById('selected-companion').value;
    
    if (!companionId) {
        alert('Please select a companion first');
        return;
    }
    
    sendCommand(companionId, command);
}

// Handle custom command
function handleCustomCommand() {
    const command = document.getElementById('custom-command').value.trim();
    const companionId = document.getElementById('selected-companion').value;
    
    if (!companionId) {
        alert('Please select a companion first');
        return;
    }
    
    if (!command) {
        alert('Please enter a command');
        return;
    }
    
    sendCommand(companionId, 'custom', { text: command });
}

// Handle save settings
function handleSaveSettings() {
    const companionId = document.getElementById('settings-companion').value;
    const name = document.getElementById('companion-name').value.trim();
    const behaviorMode = document.querySelector('input[name="behavior-mode"]:checked')?.value;
    
    if (!companionId) {
        alert('Please select a companion first');
        return;
    }
    
    if (!name) {
        alert('Please enter a name');
        return;
    }
    
    saveCompanionSettings(companionId, { name, behaviorMode });
}

// Handle inventory actions
function handleInventoryAction(action) {
    const companionId = document.getElementById('inventory-companion').value;
    
    if (!companionId) {
        alert('Please select a companion first');
        return;
    }
    
    // Get selected items if applicable
    const selectedItems = [];
    document.querySelectorAll('.inventory-slot.selected').forEach(slot => {
        selectedItems.push(slot.getAttribute('data-item-id'));
    });
    
    fetch(`/api/companions/${companionId}/inventory/${action}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({ items: selectedItems })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`${action} failed`);
        }
        return response.json();
    })
    .then(data => {
        console.log(`${action} successful:`, data);
    })
    .catch(error => {
        console.error(`${action} error:`, error);
        alert(`Failed to ${action} items: ` + error.message);
    });
}

// Select a companion
function selectCompanion(companionId) {
    selectedCompanionId = companionId;
    
    selectedCompanionDropdowns.forEach(dropdown => {
        dropdown.value = companionId;
    });
    
    // Load companion-specific data
    loadCompanionDetails(companionId);
    
    // Navigate to command panel
    activatePanel('command');
}

// Load companion details
function loadCompanionDetails(companionId) {
    fetch(`/api/companions/${companionId}`, {
        headers: {
            'Authorization': `Bearer ${authToken}`
        }
    })
    .then(response => response.json())
    .then(data => {
        // Update settings form with companion data
        document.getElementById('companion-name').value = data.name;
        
        const behaviorMode = data.behaviorMode || 'passive';
        document.querySelector(`input[name="behavior-mode"][value="${behaviorMode}"]`).checked = true;
        
        // Load inventory if on inventory panel
        if (document.getElementById('inventory-panel').classList.contains('active')) {
            loadCompanionInventory(companionId);
        }
    })
    .catch(error => {
        console.error('Error loading companion details:', error);
    });
}

// Load companion inventory
function loadCompanionInventory(companionId) {
    fetch(`/api/companions/${companionId}/inventory`, {
        headers: {
            'Authorization': `Bearer ${authToken}`
        }
    })
    .then(response => response.json())
    .then(data => {
        updateInventoryData(data);
    })
    .catch(error => {
        console.error('Error loading inventory:', error);
    });
}

// Teleport a companion to the player
function teleportCompanion(companionId) {
    fetch(`/api/companions/${companionId}/teleport`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${authToken}`
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Teleport failed');
        }
        return response.json();
    })
    .then(data => {
        console.log('Teleport successful:', data);
    })
    .catch(error => {
        console.error('Teleport error:', error);
        alert('Failed to teleport companion: ' + error.message);
    });
}

// Send command to the server
function sendCommand(companionId, command, params = {}) {
    const data = {
        command,
        ...params
    };
    
    fetch(`/api/companions/${companionId}/command`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify(data)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Command failed');
        }
        return response.json();
    })
    .then(data => {
        console.log('Command sent successfully:', data);
    })
    .catch(error => {
        console.error('Command error:', error);
        alert('Failed to send command: ' + error.message);
    });
}

// Save companion settings
function saveCompanionSettings(companionId, settings) {
    fetch(`/api/companions/${companionId}/settings`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify(settings)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to save settings');
        }
        return response.json();
    })
    .then(data => {
        console.log('Settings saved successfully:', data);
        alert('Settings saved successfully');
        loadCompanions(); // Refresh companion data
    })
    .catch(error => {
        console.error('Settings error:', error);
        alert('Failed to save settings: ' + error.message);
    });
}

// Activate a specific panel
function activatePanel(panelId) {
    // Remove active class from all links and panels
    navLinks.forEach(link => link.classList.remove('active'));
    panels.forEach(panel => panel.classList.remove('active'));
    
    // Add active class to selected link and panel
    document.querySelector(`.nav-link[data-panel="${panelId}"]`).classList.add('active');
    document.getElementById(`${panelId}-panel`).classList.add('active');
    
    // Load panel-specific data
    if (panelId === 'inventory' && selectedCompanionId) {
        loadCompanionInventory(selectedCompanionId);
    }
}

// Update connection status in the UI
function updateConnectionStatus(connected) {
    if (connected) {
        statusIndicator.className = 'status-indicator status-online';
        statusText.textContent = 'Connected';
    } else {
        statusIndicator.className = 'status-indicator status-offline';
        statusText.textContent = 'Disconnected';
    }
}

// Show dashboard and hide login screen
function showDashboard() {
    loginScreen.classList.add('hidden');
    dashboard.classList.remove('hidden');
}

// Logout function
function logout() {
    authToken = null;
    selectedCompanionId = null;
    localStorage.removeItem('authToken');
    
    if (webSocket) {
        webSocket.close();
    }
    
    dashboard.classList.add('hidden');
    loginScreen.classList.remove('hidden');
    loginError.style.display = 'none';
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);