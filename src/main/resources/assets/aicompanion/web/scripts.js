// AI Companion Web Interface Script

// Global variables
let authToken = null;
let webSocket = null;
let selectedCompanionId = null;
let selectedSkin = 'default';

// DOM References
const loginScreen = document.getElementById('login-screen');
const dashboard = document.getElementById('dashboard');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const loginError = document.getElementById('login-error');
const registerError = document.getElementById('register-error');
const registerSuccess = document.getElementById('register-success');
const authTabs = document.querySelectorAll('.auth-tab');
const authTabContents = document.querySelectorAll('.auth-tab-content');
const navLinks = document.querySelectorAll('.nav-link');
const panels = document.querySelectorAll('.panel');
const logoutLink = document.getElementById('logout-link');
const statusIndicator = document.querySelector('.status-indicator');
const statusText = document.getElementById('status-text');
const companionList = document.getElementById('companion-list');
const selectedCompanionDropdowns = document.querySelectorAll('#selected-companion, #inventory-companion, #settings-companion');
const settingsTabs = document.querySelectorAll('.settings-tab');
const settingsContents = document.querySelectorAll('.settings-content');

// Initialize the application
function init() {
    // Attach event listeners
    attachEventListeners();
    
    // Check for existing session
    checkExistingSession();
}

// Attach all event listeners
function attachEventListeners() {
    // Auth tabs
    authTabs.forEach(tab => {
        tab.addEventListener('click', handleAuthTabs);
    });
    
    // Login form submission
    loginForm.addEventListener('submit', handleLogin);
    
    // Register form submission
    registerForm.addEventListener('submit', handleRegister);
    
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
    document.getElementById('use-item').addEventListener('click', () => handleInventoryAction('use'));
    
    // Settings tabs
    settingsTabs.forEach(tab => {
        tab.addEventListener('click', handleSettingsTabs);
    });
    
    // Skin upload
    const skinUpload = document.getElementById('skin-upload');
    if (skinUpload) {
        skinUpload.addEventListener('change', handleSkinUpload);
    }
    
    // Default skin options
    document.querySelectorAll('.skin-option').forEach(option => {
        option.addEventListener('click', handleSkinSelection);
    });
}

// Handle auth tab switching
function handleAuthTabs() {
    const tabName = this.getAttribute('data-tab');
    
    // Update active tab
    authTabs.forEach(tab => {
        tab.classList.toggle('active', tab.getAttribute('data-tab') === tabName);
    });
    
    // Update active content
    authTabContents.forEach(content => {
        const contentId = content.id;
        content.classList.toggle('active', contentId === `${tabName}-tab`);
    });
    
    // Reset error messages
    loginError.style.display = 'none';
    registerError.style.display = 'none';
    registerSuccess.style.display = 'none';
}

// Handle register form submission
function handleRegister(e) {
    e.preventDefault();
    
    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;
    const confirmPassword = document.getElementById('reg-confirm-password').value;
    
    // Reset messages
    registerError.style.display = 'none';
    registerSuccess.style.display = 'none';
    
    // Validate password match
    if (password !== confirmPassword) {
        registerError.textContent = 'Passwords do not match';
        registerError.style.display = 'block';
        return;
    }
    
    // Send registration request
    fetch('/api/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || 'Registration failed');
            });
        }
        return response.json();
    })
    .then(data => {
        // Show success message
        registerSuccess.textContent = 'Account created successfully! You can now log in.';
        registerSuccess.style.display = 'block';
        
        // Clear form
        document.getElementById('reg-username').value = '';
        document.getElementById('reg-password').value = '';
        document.getElementById('reg-confirm-password').value = '';
        
        // Switch to login tab after 2 seconds
        setTimeout(() => {
            document.querySelector('.auth-tab[data-tab="login"]').click();
        }, 2000);
    })
    .catch(error => {
        console.error('Registration error:', error);
        registerError.textContent = error.message;
        registerError.style.display = 'block';
    });
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
// WebSocket reconnection variables
let reconnectAttempts = 0;
let reconnectInterval = 1000; // Start with 1 second
let maxReconnectInterval = 30000; // Max 30 seconds
let maxReconnectAttempts = 10;
let isReconnecting = false;
let reconnectTimer = null;
let pingInterval = null;

function initWebSocket() {
    // Prevent multiple reconnection attempts running simultaneously
    if (isReconnecting) return;
    isReconnecting = true;
    
    // Clean up any existing connection
    if (webSocket) {
        try {
            webSocket.close();
        } catch (error) {
            console.error('Error closing existing WebSocket:', error);
        }
    }
    
    // Clear any existing intervals/timers
    if (pingInterval) clearInterval(pingInterval);
    if (reconnectTimer) clearTimeout(reconnectTimer);
    
    // Build WebSocket URL with authentication token
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws?token=${authToken}`;
    
    console.log(`Connecting to WebSocket (Attempt ${reconnectAttempts + 1})...`);
    
    try {
        webSocket = new WebSocket(wsUrl);
        
        webSocket.onopen = function() {
            console.log('WebSocket connection established');
            updateConnectionStatus(true);
            
            // Reset reconnection parameters on successful connection
            reconnectAttempts = 0;
            reconnectInterval = 1000;
            isReconnecting = false;
            
            // Set up ping interval to keep connection alive
            pingInterval = setInterval(() => {
                if (webSocket && webSocket.readyState === WebSocket.OPEN) {
                    try {
                        // Send a ping message every 20 seconds to keep the connection alive
                        webSocket.send(JSON.stringify({
                            type: 'ping',
                            timestamp: Date.now()
                        }));
                    } catch (error) {
                        console.error('Error sending ping:', error);
                        tryReconnect();
                    }
                }
            }, 20000);
        };
        
        webSocket.onmessage = function(event) {
            try {
                const message = JSON.parse(event.data);
                console.log('Received WebSocket message:', message);
                handleWebSocketMessage(message);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        };
        
        webSocket.onclose = function(event) {
            console.log('WebSocket connection closed', event);
            updateConnectionStatus(false);
            
            // Clear ping interval
            if (pingInterval) {
                clearInterval(pingInterval);
                pingInterval = null;
            }
            
            // Try to reconnect with exponential backoff
            tryReconnect();
        };
        
        webSocket.onerror = function(error) {
            console.error('WebSocket error:', error);
            updateConnectionStatus(false);
            // Don't try to reconnect here - the close handler will be called next
        };
    } catch (error) {
        console.error('Error initializing WebSocket:', error);
        isReconnecting = false;
        tryReconnect();
    }
}

// Try to reconnect with exponential backoff
function tryReconnect() {
    // Check if we've exceeded max attempts
    if (reconnectAttempts >= maxReconnectAttempts) {
        console.error(`Failed to reconnect after ${maxReconnectAttempts} attempts`);
        
        // Show a message to the user
        const connectionError = document.getElementById('connection-error');
        if (connectionError) {
            connectionError.textContent = 'Connection lost. Please reload the page.';
            connectionError.style.display = 'block';
        } else {
            alert('Connection lost. Please reload the page to reconnect.');
        }
        return;
    }
    
    // Increment attempts and compute next delay with exponential backoff
    reconnectAttempts++;
    isReconnecting = false;
    
    // Calculate next delay with exponential backoff
    const delay = Math.min(reconnectInterval * Math.pow(1.5, reconnectAttempts - 1), maxReconnectInterval);
    console.log(`WebSocket reconnecting in ${delay}ms (Attempt ${reconnectAttempts})`);
    
    // Try to reconnect after the delay
    reconnectTimer = setTimeout(() => {
        initWebSocket();
    }, delay);
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
        case 'command_result':
            handleCommandResult(message);
            break;
        case 'pong':
            // Server responded to our ping, connection is healthy
            console.log('Received pong from server, latency:', Date.now() - message.timestamp, 'ms');
            break;
        case 'update':
            // Handle broadcasted updates
            if (message.updateType === 'skin' && message.companionId) {
                // Update the UI if this companion is selected
                if (message.companionId === selectedCompanionId) {
                    // Update skin preview
                    const skinPreview = document.getElementById('skin-preview');
                    if (skinPreview && message.data) {
                        if (message.data.skinPath) {
                            skinPreview.src = message.data.skinPath;
                        } else if (message.data.skinType) {
                            skinPreview.src = `skins/${message.data.skinType}.png`;
                        }
                    }
                }
                
                // Refresh companion list to show updated skin
                loadCompanions();
            }
            break;
        case 'connection':
            console.log('Connection status:', message.status);
            break;
        case 'error':
            console.error('Server error:', message.message);
            // Show error to user if needed
            if (message.critical) {
                alert('Server error: ' + message.message);
            }
            break;
        default:
            console.log('Unknown message type:', message.type);
    }
}

// Handle specific command results
function handleCommandResult(message) {
    console.log('Command result received:', message);
    
    // Handle skin command results
    if (message.command === 'skin') {
        // Hide loading indicator
        const loadingIndicator = document.getElementById('skin-loading');
        if (loadingIndicator) {
            loadingIndicator.style.display = 'none';
        }
        
        if (message.success) {
            // Show success message
            const successMessage = document.getElementById('skin-success');
            if (successMessage) {
                successMessage.textContent = message.message || 'Skin updated successfully!';
                successMessage.style.display = 'block';
                
                // Hide after 3 seconds
                setTimeout(() => {
                    successMessage.style.display = 'none';
                }, 3000);
            } else {
                // Fallback to alert if element doesn't exist
                alert('Skin updated successfully!');
            }
            
            // Update skin preview if available
            if (message.data && (message.data.skinType || message.data.skinPath)) {
                const skinPreview = document.getElementById('skin-preview');
                if (skinPreview) {
                    if (message.data.skinPath) {
                        skinPreview.src = message.data.skinPath;
                    } else if (message.data.skinType) {
                        skinPreview.src = `skins/${message.data.skinType}.png`;
                    }
                }
            }
        } else {
            // Show error message
            const errorMessage = document.getElementById('skin-error');
            if (errorMessage) {
                errorMessage.textContent = message.message || 'Failed to update skin';
                errorMessage.style.display = 'block';
                
                // Hide after 5 seconds
                setTimeout(() => {
                    errorMessage.style.display = 'none';
                }, 5000);
            } else {
                // Fallback to alert if element doesn't exist
                alert('Failed to update skin: ' + (message.message || 'Unknown error'));
            }
        }
    }
    
    // Handle other command results
    // ...
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
    
    // Special handling for "use" action
    if (action === 'use') {
        if (selectedItems.length === 0) {
            alert('Please select an item to use');
            return;
        }
        
        if (selectedItems.length > 1) {
            alert('Please select only one item to use at a time');
            return;
        }
        
        // Send WebSocket command to use the item
        sendUseItemCommand(companionId, selectedItems[0]);
        return;
    }
    
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

// Handle settings tabs
function handleSettingsTabs() {
    const tabName = this.getAttribute('data-tab');
    
    // Update active tab
    settingsTabs.forEach(tab => {
        tab.classList.toggle('active', tab.getAttribute('data-tab') === tabName);
    });
    
    // Update active content
    settingsContents.forEach(content => {
        const contentId = content.id;
        content.classList.toggle('active', contentId === `${tabName}-settings`);
    });
}

// Handle skin upload
function handleSkinUpload(e) {
    const file = e.target.files[0];
    
    if (!file) {
        return;
    }
    
    if (!file.type.match('image.*')) {
        alert('Please select an image file');
        return;
    }
    
    // Check file size (less than 200KB)
    if (file.size > 200 * 1024) {
        alert('File size too large. Please select a file under 200KB');
        return;
    }
    
    const reader = new FileReader();
    
    reader.onload = function(fileEvent) {
        // Preview the uploaded skin
        const skinPreview = document.getElementById('skin-preview');
        skinPreview.src = fileEvent.target.result;
        
        // Set the selected skin to 'custom'
        selectedSkin = 'custom';
        
        // Clear any previously selected default skins
        document.querySelectorAll('.skin-option.selected').forEach(option => {
            option.classList.remove('selected');
        });
    };
    
    reader.readAsDataURL(file);
}

// Handle default skin selection
function handleSkinSelection() {
    const skinType = this.getAttribute('data-skin');
    
    // Clear previous selection
    document.querySelectorAll('.skin-option.selected').forEach(option => {
        option.classList.remove('selected');
    });
    
    // Select this skin
    this.classList.add('selected');
    
    // Update the preview
    const skinPreview = document.getElementById('skin-preview');
    skinPreview.src = `skins/${skinType}.png`;
    
    // Set the selected skin
    selectedSkin = skinType;
    
    // Clear any custom upload
    document.getElementById('skin-upload').value = '';
}

// Save skin settings
function saveSkinSettings(companionId) {
    const formData = new FormData();
    let customSkinPath = '';
    
    // If we have a custom skin, add the file
    if (selectedSkin === 'custom') {
        const skinUpload = document.getElementById('skin-upload');
        if (skinUpload.files.length > 0) {
            formData.append('skin', skinUpload.files[0]);
        } else {
            // If 'custom' is selected but no file, revert to default
            selectedSkin = 'default';
        }
    }
    
    // Add the selected skin type
    formData.append('skinType', selectedSkin);
    
    // First, upload any custom skin file if needed
    if (selectedSkin === 'custom' && formData.has('skin')) {
        // Send file to server
        fetch(`/api/companions/${companionId}/skin`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`
            },
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to upload skin file');
            }
            return response.json();
        })
        .then(data => {
            console.log('Skin file uploaded:', data);
            
            if (data.skinPath) {
                customSkinPath = data.skinPath;
                // Now send the WebSocket command to apply the skin
                sendSkinCommand(companionId, selectedSkin, customSkinPath);
            } else {
                throw new Error('No skin path returned from server');
            }
        })
        .catch(error => {
            console.error('Error uploading skin file:', error);
            alert('Failed to upload skin file: ' + error.message);
        });
    } else {
        // If it's not a custom skin or we don't have a file, just send the WebSocket command
        sendSkinCommand(companionId, selectedSkin, customSkinPath);
    }
}

// Send a skin change command via WebSocket
function sendSkinCommand(companionId, skinType, skinPath = '') {
    if (!webSocket || webSocket.readyState !== WebSocket.OPEN) {
        console.error('WebSocket not connected');
        alert('Cannot send command: disconnected from server');
        return;
    }
    
    // Generate a unique command ID for tracking the response
    const commandId = Date.now().toString();
    
    const message = {
        type: 'skin',
        commandId: commandId,
        companionId: companionId,
        skinType: skinType,
        skinPath: skinPath
    };
    
    try {
        webSocket.send(JSON.stringify(message));
        console.log('Skin command sent:', message);
        
        // Show loading indicator
        document.getElementById('skin-loading').style.display = 'block';
    } catch (error) {
        console.error('Error sending skin command:', error);
        alert('Failed to send skin command: ' + error.message);
    }
}

// Send a use item command via WebSocket
function sendUseItemCommand(companionId, itemId, targetPosition = null, targetEntity = null) {
    if (!webSocket || webSocket.readyState !== WebSocket.OPEN) {
        console.error('WebSocket not connected');
        alert('Cannot send command: disconnected from server');
        return;
    }
    
    // Get item details
    const itemName = document.querySelector(`.inventory-slot[data-item-id="${itemId}"]`) ?
        document.querySelector(`.inventory-slot[data-item-id="${itemId}"]`).getAttribute('data-item-name') : 
        'Unknown Item';
    
    // Generate a unique command ID
    const commandId = Date.now().toString();
    
    const message = {
        type: 'command',
        commandId: commandId,
        companionId: companionId,
        command: 'use',
        itemId: itemId,
        itemName: itemName,
        targetPosition: targetPosition,
        targetEntity: targetEntity
    };
    
    try {
        webSocket.send(JSON.stringify(message));
        console.log('Use item command sent:', message);
    } catch (error) {
        console.error('Error sending use item command:', error);
        alert('Failed to send use item command: ' + error.message);
    }
}

// Update the saveCompanionSettings function to include skin settings
const originalSaveCompanionSettings = saveCompanionSettings;
saveCompanionSettings = function(companionId, settings) {
    // First, save behavior settings
    originalSaveCompanionSettings(companionId, settings);
    
    // Then, if we're in the skin tab, save skin settings too
    if (document.getElementById('skin-settings').classList.contains('active')) {
        saveSkinSettings(companionId);
    }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);