
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// GLOBAL STATE
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
const API = 'http://localhost:8080';
let currentUser = null;
let authToken = null;
let cart = [];
let allProducts = [];
let allDepartments = [];
let allRequests = [];
let notifications = [];
let stompClient = null;
let sidebarCollapsed = false;
let notifOpen = false;
let selectedRole = 'USER';
let recentlyOrderedId = null;

// Department budgets in Indian Rupees (â‚¹)
const DEPARTMENT_BUDGETS = {
  'Engineering': 650000,
  'Information Technology': 750000,
  'Operations & Facilities': 500000,
  'Human Resources': 300000,
  'Finance & Accounting': 450000,
  'Marketing & Communications': 350000,
  'Procurement': 800000,
  'Legal': 250000
};
let departmentBudgetRemaining = 750000;

// Modal & Feedback state
let modalSelectedProduct = null;
let modalQty = 1;
let feedbackCurrentReq = null;
let feedbackRatings = { quality: 5, speed: 5 };

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// BOOT
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
window.addEventListener('DOMContentLoaded', () => {
  try { if (window.lucide) lucide.createIcons(); } catch(e){}
  const saved = localStorage.getItem('eps_user');
  const savedToken = localStorage.getItem('eps_token');
  if (saved && savedToken) {
    try {
      currentUser = JSON.parse(saved);
      authToken = savedToken;
      bootApp();
    } catch(e) {
      console.error('Session parse error:', e);
      localStorage.removeItem('eps_user');
      localStorage.removeItem('eps_token');
      window.location.replace('login.html');
    }
  } else {
    window.location.replace('login.html');
  }
});

function formatINR(val) {
  const num = Number(val) || 0;
  return 'â‚¹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SESSION & LOGOUT
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function doLogout() {
  if (stompClient) {
    try { stompClient.deactivate(); } catch(e){}
  }
  localStorage.removeItem('eps_user');
  localStorage.removeItem('eps_token');
  currentUser = null;
  authToken = null;
  cart = [];
  window.location.replace('login.html');
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// BOOT APP & ROLE-AWARE SIDEBAR NAVIGATION
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function renderSidebarNav(role) {
  const container = document.getElementById('sidebar-nav');
  if (!container) return;

  if (role === 'MANAGER') {
    container.innerHTML = `
      <div class="nav-section-label">Management</div>
      <div class="nav-item active" onclick="showView('manager-overview')" id="nav-manager-overview">
        <i data-lucide="layout-dashboard" class="nav-item-icon"></i>
        <span class="nav-item-text">Dashboard</span>
      </div>
      <div class="nav-item" onclick="showView('manager-pending')" id="nav-manager-pending">
        <i data-lucide="clock" class="nav-item-icon"></i>
        <span class="nav-item-text">Pending Approvals</span>
        <span class="nav-badge" id="mgr-badge-pending" style="display:none">0</span>
      </div>
      <div class="nav-item" onclick="showView('manager-history')" id="nav-manager-history">
        <i data-lucide="history" class="nav-item-icon"></i>
        <span class="nav-item-text">Approval History</span>
      </div>
      <div class="nav-section-label" style="margin-top:8px">Procurement</div>
      <div class="nav-item" onclick="showView('manager-orders')" id="nav-manager-orders">
        <i data-lucide="package" class="nav-item-icon"></i>
        <span class="nav-item-text">Department Orders</span>
      </div>
      <div class="nav-item" onclick="showView('reports')" id="nav-reports">
        <i data-lucide="file-text" class="nav-item-icon"></i>
        <span class="nav-item-text">CSV Reports</span>
      </div>
    `;
  } else if (role === 'ADMIN') {
    container.innerHTML = `
      <div class="nav-section-label">Governance</div>
      <div class="nav-item active" onclick="showView('admin-overview')" id="nav-admin-overview">
        <i data-lucide="layout-dashboard" class="nav-item-icon"></i>
        <span class="nav-item-text">Admin Console</span>
      </div>
      <div class="nav-item" onclick="showView('admin-requests')" id="nav-admin-requests">
        <i data-lucide="inbox" class="nav-item-icon"></i>
        <span class="nav-item-text">Requisitions Queue</span>
        <span class="nav-badge" id="admin-badge-requests" style="display:none">0</span>
      </div>
      <div class="nav-item" onclick="showView('admin-orders')" id="nav-admin-orders">
        <i data-lucide="package" class="nav-item-icon"></i>
        <span class="nav-item-text">Purchase Orders</span>
      </div>
      <div class="nav-section-label" style="margin-top:8px">Operations</div>
      <div class="nav-item" onclick="showView('admin-suppliers')" id="nav-admin-suppliers">
        <i data-lucide="truck" class="nav-item-icon"></i>
        <span class="nav-item-text">Suppliers Directory</span>
      </div>
      <div class="nav-item" onclick="showView('admin-payments')" id="nav-admin-payments">
        <i data-lucide="credit-card" class="nav-item-icon"></i>
        <span class="nav-item-text">Payments &amp; Finance</span>
      </div>
      <div class="nav-item" onclick="showView('admin-users')" id="nav-admin-users">
        <i data-lucide="users" class="nav-item-icon"></i>
        <span class="nav-item-text">User Accounts</span>
      </div>
      <div class="nav-item" onclick="showView('reports')" id="nav-reports">
        <i data-lucide="file-text" class="nav-item-icon"></i>
        <span class="nav-item-text">CSV Reports</span>
      </div>
    `;
  } else if (role === 'SUPPLIER') {
    container.innerHTML = `
      <div class="nav-section-label">Fulfillment</div>
      <div class="nav-item active" onclick="showView('supplier-overview')" id="nav-supplier-overview">
        <i data-lucide="layout-dashboard" class="nav-item-icon"></i>
        <span class="nav-item-text">Supplier Portal</span>
      </div>
      <div class="nav-item" onclick="showView('supplier-orders')" id="nav-supplier-orders">
        <i data-lucide="package" class="nav-item-icon"></i>
        <span class="nav-item-text">Assigned Orders</span>
        <span class="nav-badge" id="sup-badge-orders" style="display:none">0</span>
      </div>
      <div class="nav-item" onclick="showView('supplier-shipments')" id="nav-supplier-shipments">
        <i data-lucide="truck" class="nav-item-icon"></i>
        <span class="nav-item-text">Dispatch &amp; Tracking</span>
      </div>
      <div class="nav-section-label" style="margin-top:8px">Inventory &amp; Products</div>
      <div class="nav-item" onclick="showView('supplier-products')" id="nav-supplier-products">
        <i data-lucide="boxes" class="nav-item-icon"></i>
        <span class="nav-item-text">Product Catalog</span>
      </div>
      <div class="nav-section-label" style="margin-top:8px">Financials</div>
      <div class="nav-item" onclick="showView('supplier-payments')" id="nav-supplier-payments">
        <i data-lucide="credit-card" class="nav-item-icon"></i>
        <span class="nav-item-text">Invoices &amp; Payments</span>
      </div>
      <div class="nav-item" onclick="showView('reports')" id="nav-reports">
        <i data-lucide="file-text" class="nav-item-icon"></i>
        <span class="nav-item-text">CSV Reports</span>
      </div>
    `;
  } else {
    // Standard USER role
    container.innerHTML = `
      <div class="nav-section-label">Main</div>
      <div class="nav-item active" onclick="showView('overview')" id="nav-overview">
        <i data-lucide="layout-dashboard" class="nav-item-icon"></i>
        <span class="nav-item-text">Dashboard</span>
      </div>
      <div class="nav-item" onclick="showView('catalog')" id="nav-catalog">
        <i data-lucide="package" class="nav-item-icon"></i>
        <span class="nav-item-text">Product Catalog</span>
      </div>
      <div class="nav-item" onclick="showCart()" id="nav-cart">
        <i data-lucide="shopping-cart" class="nav-item-icon"></i>
        <span class="nav-item-text">Cart &amp; Checkout</span>
        <span class="nav-badge" id="cart-badge" style="display:none">0</span>
      </div>
      <div class="nav-section-label" style="margin-top:8px">Procurement</div>
      <div class="nav-item" onclick="showView('tracking')" id="nav-tracking">
        <i data-lucide="map-pin" class="nav-item-icon"></i>
        <span class="nav-item-text">My Requests</span>
        <span class="nav-badge" id="req-badge" style="display:none">0</span>
      </div>
      <div class="nav-item" onclick="showView('reports')" id="nav-reports">
        <i data-lucide="file-text" class="nav-item-icon"></i>
        <span class="nav-item-text">CSV Reports</span>
      </div>
    `;
  }
  lucide.createIcons();
}

function bootApp() {
  const appEl = document.getElementById('app');
  if (appEl) appEl.style.display = 'block';

  if (!currentUser) return;

  const role = (currentUser.role || 'USER').replace('ROLE_', '').toUpperCase();

  try {
    const initials = (currentUser.fullName || '?').split(' ').map(w=>w[0]).join('').substring(0,2).toUpperCase();
    const avatarEl = document.getElementById('profile-avatar');
    if (avatarEl) avatarEl.textContent = initials;
    const nameEl = document.getElementById('profile-name');
    if (nameEl) nameEl.textContent = currentUser.fullName || currentUser.email;
    const deptEl = document.getElementById('profile-dept');
    if (deptEl) deptEl.textContent = currentUser.departmentName || role;
  } catch(e) { console.warn('Profile header setup issue', e); }

  try {
    const userName = currentUser.fullName || (currentUser.email ? currentUser.email.split('@')[0] : 'User');
    const welcomeTitle = document.getElementById('welcome-title');
    if (welcomeTitle) welcomeTitle.textContent = `Welcome, ${userName} ðŸ‘‹`;
    const welcomeDept = document.getElementById('welcome-dept-name');
    if (welcomeDept) welcomeDept.textContent = currentUser.departmentName || 'Information Technology';
    const roleBadge = document.getElementById('welcome-role-badge');
    if (roleBadge) roleBadge.textContent = role;
  } catch(e) { console.warn('Welcome title setup issue', e); }

  try {
    renderSidebarNav(role);
  } catch(e) { console.warn('Sidebar nav render issue', e); }

  try {
    const userName = currentUser.fullName || (currentUser.email ? currentUser.email.split('@')[0] : 'User');
    if (role === 'MANAGER') {
      const mgrTitle = document.getElementById('mgr-welcome-title');
      if (mgrTitle) mgrTitle.textContent = `Welcome, ${userName} ðŸ‘‹`;
      showView('manager-overview');
      loadManagerData();
    } else if (role === 'ADMIN') {
      const admTitle = document.getElementById('admin-welcome-title');
      if (admTitle) admTitle.textContent = `Welcome, ${userName} ðŸ‘‹`;
      showView('admin-overview');
      loadAdminData();
    } else if (role === 'SUPPLIER') {
      const supTitle = document.getElementById('sup-welcome-title');
      if (supTitle) supTitle.textContent = `Welcome, ${userName} ðŸ‘‹`;
      const supVendor = document.getElementById('sup-vendor-name');
      if (supVendor) supVendor.textContent = currentUser.fullName || 'Acme Industrial Supplies';
      showView('supplier-overview');
      loadSupplierData();
    } else {
      showView('overview');
      loadProducts();
      loadRequests();
      loadDepartments();
    }
  } catch(e) { console.warn('Role views setup issue', e); }

  try { buildReports(); } catch(e){}
  try { loadRealNotifications(); } catch(e){}
  try { connectWebSocket(); } catch(e){}
  try { if (window.lucide) lucide.createIcons(); } catch(e){}
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// WEBSOCKET (STOMP OVER SOCKJS)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function connectWebSocket() {
  try {
    const socket = new SockJS(`${API}/ws-procurement`);
    stompClient = new StompJs.Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: () => {}
    });
    stompClient.onConnect = () => {
      stompClient.subscribe('/topic/purchase-requests', (msg) => {
        try {
          const req = JSON.parse(msg.body);
          addRealTimeNotification(`ðŸ”” Real-time update: Requisition <strong>#${req.requestId || 'REQ'}</strong> is now <strong>${req.status || 'UPDATED'}</strong>`, 'info');
          const role = (currentUser?.role || 'USER').replace('ROLE_', '').toUpperCase();
          if (role === 'MANAGER') loadManagerData();
          else if (role === 'ADMIN') loadAdminData();
          else if (role === 'SUPPLIER') loadSupplierData();
          else loadRequests();
        } catch(e) {}
      });
      stompClient.subscribe('/topic/notifications', (msg) => {
        try {
          const n = JSON.parse(msg.body);
          addRealTimeNotification(n.message || 'Procurement notification received', 'info');
          loadRealNotifications();
        } catch(e) {}
      });
    };
    stompClient.activate();
  } catch(e) { /* WebSocket optional */ }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// REAL DATABASE NOTIFICATIONS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
async function loadRealNotifications() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/notifications`, { headers });
    if (res.ok) {
      const data = await res.json();
      notifications = (data || []).map(n => ({
        id: n.notificationId,
        msg: n.message,
        time: n.createdDate ? formatDate(n.createdDate) : 'Recently',
        color: '#6366f1',
        icon: 'bell',
        unread: !n.isRead
      }));
      renderNotifications();
    }
  } catch(e) {
    console.warn('Real notifications fetch error', e);
  }
}

function addRealTimeNotification(msg, type) {
  const colorMap = {info:'#06b6d4', success:'#10b981', warning:'#f59e0b', danger:'#ef4444'};
  notifications.unshift({id: Date.now(), msg, time: 'Just now', color: colorMap[type]||'#6366f1', icon:'bell', unread:true});
  renderNotifications();
  showToast(msg.replace(/<[^>]*>/g,''), type);
}

function renderNotifications() {
  const list = document.getElementById('notif-list');
  if (!list) return;
  const unread = notifications.filter(n=>n.unread).length;
  const dot = document.getElementById('notif-dot');
  if (dot) dot.style.display = unread > 0 ? 'block' : 'none';
  if (!notifications.length) {
    list.innerHTML = '<div style="padding:32px;text-align:center;color:var(--text-muted);font-size:13px">No notifications</div>';
    return;
  }
  list.innerHTML = notifications.map(n => `
    <div class="notif-item ${n.unread ? 'unread':''}" onclick="markRealRead(${n.id})">
      <div class="notif-icon-wrap" style="background:${n.color}20">
        <i data-lucide="${n.icon}" style="width:16px;height:16px;color:${n.color}"></i>
      </div>
      <div class="notif-body">
        <div class="notif-msg">${n.msg}</div>
        <div class="notif-time">${n.time}</div>
      </div>
    </div>
  `).join('');
  lucide.createIcons();
}

async function markRealRead(id) {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    await fetch(`${API}/api/notifications/${id}/read`, { method:'PUT', headers });
  } catch(e) {}
  notifications = notifications.map(n => n.id === id ? {...n, unread:false} : n);
  renderNotifications();
}

async function clearNotifications() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    await fetch(`${API}/api/notifications/read-all`, { method:'PUT', headers });
  } catch(e) {}
  notifications = [];
  renderNotifications();
}

function toggleNotif() {
  notifOpen = !notifOpen;
  document.getElementById('notif-panel').style.display = notifOpen ? 'block' : 'none';
}
document.addEventListener('click', e => {
  if (notifOpen && !e.target.closest('.notif-btn') && !e.target.closest('.notif-panel')) {
    notifOpen = false;
    document.getElementById('notif-panel').style.display = 'none';
  }
});

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SIDEBAR & VIEWS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function toggleSidebar() {
  sidebarCollapsed = !sidebarCollapsed;
  document.getElementById('sidebar').classList.toggle('collapsed', sidebarCollapsed);
}

function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`view-${name}`).classList.add('active');
  const nav = document.getElementById(`nav-${name}`);
  if (nav) nav.classList.add('active');
  if (name === 'tracking') loadRequests();
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// PRODUCTS / CATALOG (â‚¹ CURRENCY)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
async function loadProducts() {
  try {
    const res = await fetch(`${API}/api/products`);
    const data = await res.json();
    allProducts = data.data || [];
    document.getElementById('product-count').textContent = allProducts.length;
    buildCategoryFilters();
    renderProducts(allProducts);
  } catch(e) { console.error('Products load failed', e); }
}

function buildCategoryFilters() {
  const cats = ['All', ...new Set(allProducts.map(p => p.categoryName || 'General'))];
  const el = document.getElementById('category-filters');
  el.innerHTML = cats.map((c,i) => `
    <div class="filter-chip ${i===0?'active':''}" data-cat="${c}" onclick="filterCatalog('${c}',this)">${c}</div>
  `).join('');
}

function filterCatalog(cat, el) {
  document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
  el.classList.add('active');
  const filtered = cat === 'All' ? allProducts : allProducts.filter(p => (p.categoryName||'General') === cat);
  renderProducts(filtered);
}

let searchQuery = '';
function onSearch(q) {
  searchQuery = q.toLowerCase();
  if (!searchQuery) { renderProducts(allProducts); return; }
  const filtered = allProducts.filter(p =>
    (p.name||'').toLowerCase().includes(searchQuery) ||
    (p.description||'').toLowerCase().includes(searchQuery) ||
    (p.sku||'').toLowerCase().includes(searchQuery) ||
    (p.categoryName||'').toLowerCase().includes(searchQuery)
  );
  if (document.getElementById('view-catalog').classList.contains('active')) renderProducts(filtered);
}

function renderProducts(products) {
  const grid = document.getElementById('product-grid');
  if (!products.length) {
    grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:60px;color:var(--text-muted)"><i data-lucide="search-x" style="width:48px;height:48px;opacity:0.2;margin-bottom:16px"></i><div>No products found</div></div>';
    lucide.createIcons(); return;
  }
  grid.innerHTML = products.map(p => {
    const stock = p.numberOfQuantities || 0;
    const stockClass = stock > 20 ? 'in-stock' : 'low-stock';
    const stockLabel = stock > 20 ? `âœ“ In Stock (${stock})` : `âš  Low Stock (${stock})`;
    const img = p.imageUrl || `https://images.unsplash.com/photo-1497366216548-37526070297c?w=400`;
    const inCart = cart.find(c => c.productId === p.productId);
    return `
    <div class="product-card">
      <div class="product-img-wrap">
        <img class="product-img" src="${img}" alt="${p.name}" onerror="this.src='https://images.unsplash.com/photo-1497366216548-37526070297c?w=400'"/>
        <span class="product-cat-badge">${p.categoryName || 'General'}</span>
        <span class="stock-badge ${stockClass}">${stockLabel}</span>
      </div>
      <div class="product-body">
        <div class="product-sku">${p.sku || `PROD-${p.productId}`}</div>
        <div class="product-name">${p.name}</div>
        <div class="product-desc">${p.description || ''}</div>
        <div class="product-price-row">
          <div class="product-price">${formatINR(p.pricePerProduct)}<span>/unit</span></div>
        </div>
        <div class="product-actions">
          <button class="btn btn-secondary btn-sm" onclick="addToCart(${JSON.stringify(p).replace(/"/g,'&quot;')})">
            ${inCart ? 'âœ“ In Cart' : '+ Add to Cart'}
          </button>
          <button class="btn btn-primary btn-sm" onclick="openNewRequestModal(${JSON.stringify(p).replace(/"/g,'&quot;')})">
            Buy Now
          </button>
        </div>
      </div>
    </div>`;
  }).join('');
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CART (â‚¹ CURRENCY)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function addToCart(product) {
  const existing = cart.find(c => c.productId === product.productId);
  if (existing) { existing.qty++; }
  else { cart.push({...product, qty:1}); }
  updateCartUI();
  showToast(`<strong>${product.name}</strong> added to cart`, 'success');
}

function removeFromCart(productId) {
  cart = cart.filter(c => c.productId !== productId);
  updateCartUI();
}

function updateQty(productId, delta) {
  const item = cart.find(c => c.productId === productId);
  if (!item) return;
  item.qty = Math.max(1, item.qty + delta);
  updateCartUI();
}

function updateCartUI() {
  const count = cart.reduce((s, c) => s + c.qty, 0);
  const badge = document.getElementById('cart-badge');
  badge.textContent = count;
  badge.style.display = count > 0 ? 'block' : 'none';
  document.getElementById('cart-count').textContent = count;
  const quickCount = document.getElementById('quick-cart-count');
  if (quickCount) quickCount.textContent = count;
  renderCartItems();
  renderProducts(allProducts);
}

function renderCartItems() {
  const list = document.getElementById('cart-items-list');
  const footer = document.getElementById('cart-footer');
  if (!cart.length) {
    list.innerHTML = `<div class="cart-empty"><i data-lucide="shopping-cart" class="cart-empty-icon" style="width:56px;height:56px"></i><div>Your cart is empty</div><div style="font-size:12px;margin-top:6px">Browse the catalog to add items</div></div>`;
    footer.style.display = 'none'; lucide.createIcons(); return;
  }
  footer.style.display = 'block';
  list.innerHTML = cart.map(item => {
    const img = item.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=100';
    const lineTotal = Number(item.pricePerProduct) * item.qty;
    return `
    <div class="cart-item">
      <img class="cart-item-img" src="${img}" alt="${item.name}" onerror="this.src='https://images.unsplash.com/photo-1497366216548-37526070297c?w=100'"/>
      <div class="cart-item-info">
        <div class="cart-item-name">${item.name}</div>
        <div class="cart-item-sku">${item.sku || `PROD-${item.productId}`} Â· ${item.categoryName||'General'}</div>
        <div class="cart-item-price">${formatINR(lineTotal)}</div>
        <div class="qty-control">
          <button class="qty-btn" onclick="updateQty(${item.productId},-1)">âˆ’</button>
          <span class="qty-display">${item.qty}</span>
          <button class="qty-btn" onclick="updateQty(${item.productId},+1)">+</button>
          <button class="btn-icon" style="margin-left:auto" onclick="removeFromCart(${item.productId})">
            <i data-lucide="trash-2" style="width:13px;height:13px;color:var(--danger)"></i>
          </button>
        </div>
      </div>
    </div>`;
  }).join('');

  // Totals in â‚¹
  const subtotal = cart.reduce((s,c) => s + Number(c.pricePerProduct)*c.qty, 0);
  const tax = subtotal * 0.08;
  const total = subtotal + tax;
  document.getElementById('cart-subtotal').textContent = formatINR(subtotal);
  document.getElementById('cart-tax').textContent = formatINR(tax);
  document.getElementById('cart-total').textContent = formatINR(total);

  // Live Department Budget Check Alert
  const alertEl = document.getElementById('cart-budget-alert');
  if (alertEl) {
    if (total > departmentBudgetRemaining) {
      alertEl.style.display = 'block';
      alertEl.innerHTML = `<div style="background:rgba(239,68,68,0.15);color:var(--danger);border:1px solid rgba(239,68,68,0.3);border-radius:10px;padding:10px 12px;font-size:12px;display:flex;align-items:center;gap:8px;line-height:1.4"><i data-lucide="alert-triangle" style="width:16px;height:16px;flex-shrink:0"></i><span>âš ï¸ <strong>Over Department Budget:</strong> Requisition total (${formatINR(total)}) exceeds remaining balance (${formatINR(departmentBudgetRemaining)}). VP approval will be required.</span></div>`;
    } else {
      alertEl.style.display = 'block';
      alertEl.innerHTML = `<div style="background:rgba(16,185,129,0.1);color:var(--success);border:1px solid rgba(16,185,129,0.25);border-radius:10px;padding:10px 12px;font-size:12px;display:flex;align-items:center;gap:8px;line-height:1.4"><i data-lucide="shield-check" style="width:16px;height:16px;flex-shrink:0"></i><span>âœ“ Requisition is within Department Budget limit (<strong>${formatINR(departmentBudgetRemaining)}</strong> available)</span></div>`;
    }
  }

  lucide.createIcons();
}

function showCart() {
  document.getElementById('cart-panel').classList.add('open');
  document.getElementById('cart-overlay').classList.add('open');
}
function hideCart() {
  document.getElementById('cart-panel').classList.remove('open');
  document.getElementById('cart-overlay').classList.remove('open');
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// DEPARTMENTS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
async function loadDepartments() {
  try {
    const res = await fetch(`${API}/api/departments`);
    const data = await res.json();
    allDepartments = data.data || [];
    const sel = document.getElementById('checkout-dept');
    if (sel) {
      allDepartments.forEach(d => {
        const opt = document.createElement('option');
        opt.value = d.departmentId;
        opt.textContent = d.departmentName;
        if (currentUser && currentUser.departmentId && d.departmentId == currentUser.departmentId) opt.selected = true;
        sel.appendChild(opt);
      });
    }
  } catch(e) {}
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// SUBMIT PURCHASE REQUEST FROM CART (LIFO 1ST POSITION POP)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
async function submitPurchaseRequest() {
  if (!cart.length) { showToast('Cart is empty!', 'warning'); return; }
  const deptEl = document.getElementById('checkout-dept');
  const deptId = deptEl.value || null;
  const address = document.getElementById('checkout-address')?.value || currentUser?.address || 'Corporate Desk';

  let successCount = 0;
  for (const item of cart) {
    const body = {
      userId: currentUser.userId,
      productId: item.productId,
      quantity: item.qty,
      departmentId: deptId ? parseInt(deptId) : (currentUser.departmentId || null),
      address: address
    };
    try {
      const headers = {'Content-Type':'application/json'};
      if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
      const res = await fetch(`${API}/api/purchase-requests`, {
        method:'POST', headers, body: JSON.stringify(body)
      });
      const data = await res.json();
      if (res.ok && data.success) {
        successCount++;
        // Pop newly created item directly to the 1st position (index 0)
        allRequests.unshift(data.data);
        recentlyOrderedId = data.data.requestId;
      }
    } catch(e) { console.error(e); }
  }

  if (successCount > 0) {
    cart = [];
    updateCartUI();
    hideCart();

    // Instantly refresh and re-render dashboard with new order at the 1st spot
    updateStats(allRequests);
    renderRequests(allRequests);
    renderActiveOrders(allRequests);
    renderActivityFeed(allRequests);

    showToast(`ðŸŽ‰ ${successCount} item(s) ordered! Popped to 1st place in your dashboard.`, 'success');
    addRealTimeNotification(`ðŸ“‹ You ordered <strong>${successCount} item(s)</strong> â€” pending manager review.`, 'info');
  } else {
    showToast('Failed to submit request. Please try again.', 'error');
  }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// INTERACTIVE "CREATE PURCHASE REQUEST" MODAL
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function openNewRequestModal(product = null) {
  modalSelectedProduct = product || allProducts[0] || null;
  modalQty = 1;
  document.getElementById('modal-qty-val').textContent = modalQty;

  const sel = document.getElementById('modal-product-select');
  sel.innerHTML = allProducts.map(p => `
    <option value="${p.productId}" ${modalSelectedProduct && modalSelectedProduct.productId === p.productId ? 'selected':''}>
      ${p.name} Â· ${p.sku || ''} (${formatINR(p.pricePerProduct)})
    </option>
  `).join('');

  const addrEl = document.getElementById('modal-delivery-address');
  if (addrEl && (!addrEl.value || addrEl.value.trim() === '')) {
    addrEl.value = currentUser?.address || 'Building 3, Floor 4, Desk 42B';
  }

  const deptSel = document.getElementById('modal-dept-select');
  if (deptSel && allDepartments.length) {
    deptSel.innerHTML = allDepartments.map(d => `
      <option value="${d.departmentId}" ${currentUser?.departmentId === d.departmentId ? 'selected':''}>
        ${d.departmentName}
      </option>
    `).join('');
  }

  updateModalProductPreview();
  document.getElementById('request-modal-overlay').classList.add('open');
  lucide.createIcons();
}

function closeNewRequestModal() {
  document.getElementById('request-modal-overlay').classList.remove('open');
}

function onModalProductChanged() {
  const pid = parseInt(document.getElementById('modal-product-select').value);
  modalSelectedProduct = allProducts.find(p => p.productId === pid) || allProducts[0];
  updateModalProductPreview();
}

function adjustModalQty(delta) {
  modalQty = Math.max(1, modalQty + delta);
  document.getElementById('modal-qty-val').textContent = modalQty;
  updateModalProductPreview();
}

function updateModalProductPreview() {
  if (!modalSelectedProduct) return;
  document.getElementById('modal-preview-img').src = modalSelectedProduct.imageUrl || 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400';
  document.getElementById('modal-preview-name').textContent = modalSelectedProduct.name;
  document.getElementById('modal-preview-meta').textContent = `${modalSelectedProduct.sku || 'SKU'} Â· ${modalSelectedProduct.categoryName || 'General'}`;
  document.getElementById('modal-preview-price').textContent = formatINR(modalSelectedProduct.pricePerProduct);

  const total = Number(modalSelectedProduct.pricePerProduct) * modalQty;
  document.getElementById('modal-calc-total').textContent = formatINR(total);

  // Department Budget Guardian validation
  const alertEl = document.getElementById('modal-budget-alert');
  if (alertEl) {
    if (total > departmentBudgetRemaining) {
      alertEl.innerHTML = `<div style="background:rgba(239,68,68,0.15);color:var(--danger);border:1px solid rgba(239,68,68,0.3);border-radius:10px;padding:10px 12px;font-size:12px;display:flex;align-items:center;gap:8px"><i data-lucide="alert-triangle" style="width:16px;height:16px;flex-shrink:0"></i><span>âš ï¸ <strong>Budget Overrun Warning:</strong> Requisition total (${formatINR(total)}) exceeds remaining department balance (${formatINR(departmentBudgetRemaining)}). Requires VP override.</span></div>`;
    } else {
      alertEl.innerHTML = `<div style="background:rgba(16,185,129,0.1);color:var(--success);border:1px solid rgba(16,185,129,0.25);border-radius:10px;padding:10px 12px;font-size:12px;display:flex;align-items:center;gap:8px"><i data-lucide="shield-check" style="width:16px;height:16px;flex-shrink:0"></i><span>âœ“ <strong>Department Budget:</strong> Within authorized limit (${formatINR(departmentBudgetRemaining)} remaining). Approved for submission.</span></div>`;
    }
  }
  lucide.createIcons();
}

async function submitModalPurchaseRequest() {
  if (!modalSelectedProduct) return;
  const address = document.getElementById('modal-delivery-address').value.trim();
  const notes = document.getElementById('modal-delivery-notes').value.trim();
  const deptId = document.getElementById('modal-dept-select').value || currentUser?.departmentId;

  if (!address) {
    showToast('Delivery address is mandatory.', 'warning');
    document.getElementById('modal-delivery-address').focus();
    return;
  }

  const payload = {
    userId: currentUser?.userId,
    productId: modalSelectedProduct.productId,
    quantity: modalQty,
    departmentId: deptId ? parseInt(deptId) : (currentUser?.departmentId || null),
    address: address
  };

  try {
    const headers = {'Content-Type': 'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/purchase-requests`, {
      method: 'POST', headers, body: JSON.stringify(payload)
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Submission failed');

    const createdReq = result.data;
    closeNewRequestModal();

    // POP TO 1ST PLACE (INDEX 0):
    allRequests.unshift(createdReq);
    recentlyOrderedId = createdReq.requestId;

    updateStats(allRequests);
    renderRequests(allRequests);
    renderActiveOrders(allRequests);
    renderActivityFeed(allRequests);

    showToast(`ðŸŽ‰ Requisition REQ #${createdReq.requestId} submitted! Popped to 1st in dashboard.`, 'success');
    addRealTimeNotification(`ðŸ“‹ You requested <strong>${modalSelectedProduct.name} Ã— ${modalQty}</strong>. Placed 1st in pipeline.`, 'info');
  } catch(e) {
    showToast(`Error submitting request: ${e.message}`, 'error');
  }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// REQUESTS / TRACKING (LIFO ORDERING & INSTANT UPDATE)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
async function loadRequests() {
  const el = document.getElementById('requests-list');
  el.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)"><i data-lucide="loader" style="width:28px;height:28px;animation:spin 1s linear infinite"></i></div>';
  lucide.createIcons();
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/purchase-requests`, {headers});
    const data = await res.json();
let userFeedbacks = {};

async function fetchUserDeliveredFeedbacks(requests) {
  const delivered = (requests || []).filter(r => (r.status || '').toUpperCase() === 'DELIVERED');
  if (!delivered.length) return;
  const headers = {};
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
  await Promise.all(delivered.map(async r => {
    try {
      const res = await fetch(`${API}/api/users/requests/${r.requestId}/feedback`, { headers });
      if (res.ok) {
        const data = await res.json();
        if (data && data.feedbackId) {
          userFeedbacks[r.requestId] = data;
          localStorage.setItem(`eps_fb_${r.requestId}`, JSON.stringify(data));
        }
      }
    } catch(err){}
  }));
}

    // LIFO: newest first
    allRequests = (data.data || []).reverse();
    await fetchUserDeliveredFeedbacks(allRequests);
    updateStats(allRequests);
    renderRequests(allRequests);
    renderActiveOrders(allRequests);
  } catch(e) {
    el.innerHTML = '<div style="text-align:center;padding:40px;color:var(--danger)">Failed to load requests.</div>';
  }
}

function updateStats(requests) {
  document.getElementById('stat-requests').textContent = requests.length;
  document.getElementById('stat-pending').textContent = requests.filter(r=>r.status==='PENDING').length;
  document.getElementById('stat-delivered').textContent = requests.filter(r=>r.status==='DELIVERED').length;
  const spend = requests.reduce((s,r) => s + (Number(r.totalPrice)||0), 0);
  document.getElementById('stat-spend').textContent = formatINR(spend);
  document.getElementById('req-badge').textContent = requests.filter(r=>r.status==='PENDING').length;
  document.getElementById('req-badge').style.display = requests.filter(r=>r.status==='PENDING').length > 0 ? 'block':'none';

  // Department Budget Guardian
  const dept = currentUser?.departmentName || 'Information Technology';
  const cap = DEPARTMENT_BUDGETS[dept] || 750000;
  const used = spend;
  departmentBudgetRemaining = Math.max(0, cap - used);
  const pct = Math.min(100, Math.round((used / cap) * 100));

  const deptEl = document.getElementById('budget-dept-name');
  if (deptEl) deptEl.textContent = `Department Budget Guardian: ${dept}`;
  const totalEl = document.getElementById('budget-total');
  if (totalEl) totalEl.textContent = formatINR(cap);
  const remEl = document.getElementById('budget-remaining');
  if (remEl) remEl.textContent = formatINR(departmentBudgetRemaining);
  const usedEl = document.getElementById('budget-used');
  if (usedEl) usedEl.textContent = formatINR(used);
  const pctEl = document.getElementById('budget-pct');
  if (pctEl) pctEl.textContent = `${pct}%`;

  const bar = document.getElementById('budget-progress-bar');
  if (bar) {
    bar.style.width = `${Math.max(4, pct)}%`;
    if (pct > 90) {
      bar.style.background = 'linear-gradient(90deg,var(--danger),#f87171)';
    } else if (pct > 70) {
      bar.style.background = 'linear-gradient(90deg,var(--warning),#fbbf24)';
    } else {
      bar.style.background = 'linear-gradient(90deg,var(--accent),#818cf8)';
    }
  }

  const badge = document.getElementById('budget-status-badge');
  if (badge) {
    if (pct > 90) {
      badge.textContent = 'â— Near Cap (Exceeding)';
      badge.style.background = 'rgba(239,68,68,0.15)';
      badge.style.color = 'var(--danger)';
      badge.style.borderColor = 'rgba(239,68,68,0.3)';
    } else if (pct > 70) {
      badge.textContent = 'â— High Utilization';
      badge.style.background = 'rgba(245,158,11,0.15)';
      badge.style.color = 'var(--warning)';
      badge.style.borderColor = 'rgba(245,158,11,0.3)';
    } else {
      badge.textContent = 'â— Budget Healthy';
      badge.style.background = 'rgba(16,185,129,0.15)';
      badge.style.color = 'var(--success)';
      badge.style.borderColor = 'rgba(16,185,129,0.3)';
    }
  }

  // Update Recent Activity & Active Orders
  renderActivityFeed(requests);
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// DEDICATED ACTIVE ORDERED PRODUCTS & REAL-TIME TRACKING
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function renderActiveOrders(requests) {
  const container = document.getElementById('active-orders-container');
  if (!container) return;
  const activeList = requests.filter(r => r.status !== 'REJECTED');
  if (!activeList.length) {
    container.innerHTML = `
      <div style="text-align:center;padding:48px 20px;background:var(--bg-card);border:1px dashed var(--border);border-radius:var(--radius);color:var(--text-muted)">
        <i data-lucide="package-search" style="width:44px;height:44px;opacity:0.3;margin-bottom:12px"></i>
        <div style="font-weight:600;font-size:14px;color:var(--text-primary)">No Active Requisitions Currently</div>
        <div style="font-size:12.5px;margin-top:4px">Browse our enterprise catalog and click "Create Purchase Request" to track it live!</div>
        <button class="btn btn-primary btn-sm" onclick="showView('catalog')" style="margin-top:16px">
          <i data-lucide="shopping-bag" style="width:14px;height:14px"></i>Explore Catalog
        </button>
      </div>`;
    lucide.createIcons();
    return;
  }

  container.innerHTML = activeList.slice(0, 4).map(r => {
    const isNew = r.requestId === recentlyOrderedId;
    const isDelivered = r.status === 'DELIVERED';
    const hasFeedback = localStorage.getItem(`eps_fb_${r.requestId}`);
    const img = r.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=100';

    const statusSteps = ['PENDING','APPROVED','PO_ISSUED','ORDERED','DISPATCHED','DELIVERED'];
    const statusLabels = ['Submitted','Mgr Approved','PO Issued','Paid / Sourced','In Transit','Delivered'];
    const stepIcons = ['file-text','check-circle','file-check','credit-card','truck','package-check'];
    const normalizedStatus = (r.status === 'PAID') ? 'ORDERED' : r.status;
    const currentStep = statusSteps.indexOf(normalizedStatus);

    const trackSteps = statusSteps.map((s, idx) => {
      let cls = '';
      if (idx < currentStep) cls = 'done';
      else if (idx === currentStep) cls = 'active';
      return `
      <div class="track-step">
        <div class="track-dot ${cls}">
          <i data-lucide="${stepIcons[idx]}" style="width:12px;height:12px;color:${cls?'#fff':'var(--text-muted)'}"></i>
        </div>
        <div class="track-label ${cls}">${statusLabels[idx]}</div>
      </div>`;
    }).join('');

    return `
    <div class="active-order-card ${isNew ? 'just-ordered':''}">
      ${isNew ? '<div style="position:absolute;top:14px;right:18px;background:linear-gradient(135deg,var(--accent),#10b981);color:#fff;font-size:10px;font-weight:800;padding:3px 10px;border-radius:20px;letter-spacing:0.5px">âœ¨ JUST ORDERED (1st)</div>' : ''}
      <div class="active-order-header">
        <img class="active-order-img" src="${img}" alt="${r.productName}" onerror="this.src='https://images.unsplash.com/photo-1497366216548-37526070297c?w=100'"/>
        <div class="active-order-meta">
          <div style="font-size:11.5px;color:var(--text-muted);font-weight:700">REQ #${r.requestId} Â· ${r.sku || 'SKU-001'}</div>
          <div class="active-order-title">${r.productName || 'Product'}</div>
          <div class="active-order-sub">Qty: <strong>${r.quantity}</strong> Â· Total: <strong style="color:var(--success)">${formatINR(r.totalPrice)}</strong> Â· ${r.departmentName || 'Dept'}</div>
        </div>
        <span class="status-badge status-${r.status||'PENDING'}" style="margin-left:auto">${(r.status||'PENDING').replace(/_/g,' ')}</span>
      </div>

      <!-- Real-Time Progress Tracker -->
      <div class="tracking-bar" style="margin:16px 0">${trackSteps}</div>

      <!-- Live Fulfillment Pills -->
      <div class="active-order-pills">
        <div class="tracking-pill"><i data-lucide="file-check" style="width:14px;height:14px;color:var(--accent-light)"></i><span>PO #: <strong>${r.poNumber || 'PO-Awaiting'}</strong></span></div>
        <div class="tracking-pill"><i data-lucide="truck" style="width:14px;height:14px;color:var(--info)"></i><span>Carrier: <strong>${r.carrier || 'FedEx Priority Freight'}</strong></span></div>
        <div class="tracking-pill"><i data-lucide="map-pin" style="width:14px;height:14px;color:var(--warning)"></i><span>Tracking #: <strong>${r.trackingNumber || 'FEDEX-' + (88000000 + r.requestId)}</strong></span></div>
        <div class="tracking-pill"><i data-lucide="calendar" style="width:14px;height:14px;color:var(--success)"></i><span>ETA: <strong>${r.estimatedDelivery || '3-5 Business Days'}</strong></span></div>
        ${isDelivered ? (
          hasFeedback ?
          '<div class="tracking-pill" style="margin-left:auto;background:rgba(16,185,129,0.15);color:var(--success);border-color:rgba(16,185,129,0.3)"><span>âœ“ Rated â˜… 5.0</span></div>' :
          `<button class="btn btn-warning btn-sm" style="margin-left:auto;padding:6px 14px" onclick="openFeedbackModal(${JSON.stringify(r).replace(/"/g,'&quot;')})">â­ Rate Delivered Order</button>`
        ) : ''}
      </div>
    </div>`;
  }).join('');

  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// RECENT ACTIVITY FEED (LIFO & 1ST POSITION HIGHLIGHT)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function renderActivityFeed(requests) {
  const feed = document.getElementById('activity-feed');
  if (!requests.length) {
    feed.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)"><i data-lucide="inbox" style="width:40px;height:40px;opacity:0.3;margin-bottom:12px"></i><div>No activity yet.</div></div>';
    lucide.createIcons(); return;
  }
  const iconMap = {PENDING:{i:'clock',c:'#f59e0b'}, APPROVED:{i:'check-circle',c:'#10b981'}, REJECTED:{i:'x-circle',c:'#ef4444'}, DISPATCHED:{i:'truck',c:'#6366f1'}, DELIVERED:{i:'package-check',c:'#10b981'}, PO_ISSUED:{i:'file-check',c:'#06b6d4'}};
  const labelMap = {PENDING:'Pending Manager Review',APPROVED:'Managfunction openFeedbackModal(req) {
  feedbackCurrentReq = req;
  const existing = userFeedbacks[req.requestId] || {};
  feedbackRatings = { quality: existing.qualityRating || 5, speed: existing.speedRating || 5 };
  document.getElementById('fb-product-img').src = req.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=100';
  document.getElementById('fb-product-name').textContent = req.productName || 'Delivered Product';
  document.getElementById('fb-request-id').textContent = `Requisition #${req.requestId} · Delivered`;
  document.getElementById('fb-comments').value = existing.comments || '';
  setStarRating('quality', feedbackRatings.quality);
  setStarRating('speed', feedbackRatings.speed);
  document.getElementById('feedback-modal-overlay').classList.add('open');
  lucide.createIcons();
}

function closeFeedbackModal() {
  document.getElementById('feedback-modal-overlay').classList.remove('open');
}

function setStarRating(type, val) {
  feedbackRatings[type] = val;
  const group = document.getElementById(`star-${type}-group`);
  if (!group) return;
  const btns = group.querySelectorAll('.star-btn');
  btns.forEach((b, idx) => {
    b.classList.toggle('active', idx < val);
  });
  const textEl = document.getElementById(`${type}-rating-text`);
  const descriptors = {
    quality: ['1.0 - Poor Quality','2.0 - Below Average','3.0 - Acceptable','4.0 - Good Quality','5.0 - Excellent Quality'],
    speed: ['1.0 - Severely Delayed','2.0 - Delayed','3.0 - Average Delivery','4.0 - Fast Fulfillment','5.0 - Prompt On-Time Delivery']
  };
  if (textEl) textEl.textContent = `${descriptors[type][val - 1]} (${val}.0 / 5.0)`;
}

async function submitFeedback() {
  if (!feedbackCurrentReq) return;
  const comments = document.getElementById('fb-comments').value.trim() || 'Verified and approved.';
  const payload = {
    qualityRating: feedbackRatings.quality,
    speedRating: feedbackRatings.speed,
    comments
  };
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers.Authorization = `Bearer ${authToken}`;
    const response = await fetch(`${API}/api/users/requests/${feedbackCurrentReq.requestId}/feedback`, {method:'POST', headers, body:JSON.stringify(payload)});
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || 'Feedback could not be saved');
    if (result.data) {
      userFeedbacks[feedbackCurrentReq.requestId] = result.data;
      localStorage.setItem(`eps_fb_${feedbackCurrentReq.requestId}`, JSON.stringify(result.data));
    }
    closeFeedbackModal();
    showToast(`⭐ Thank you! Feedback saved for ${feedbackCurrentReq.productName}.`, 'success');
    loadRequests();
  } catch (error) {
    showToast(`Feedback error: ${error.message}`, 'error');
  }
}�â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function renderRequests(requests) {
  const el = document.getElementById('requests-list');
  if (!requests.length) {
    el.innerHTML = '<div style="text-align:center;padding:60px;color:var(--text-muted)"><i data-lucide="map-pin" style="width:48px;height:48px;opacity:0.2;margin-bottom:16px"></i><div>No purchase requests yet</div></div>';
    lucide.createIcons(); return;
  }
  el.innerHTML = requests.map((r, idx) => renderRequestCard(r, idx)).join('');
  lucide.createIcons();
}

function renderRequestCard(r, idx) {
  const isNew = r.requestId === recentlyOrderedId;
  const isDelivered = r.status === 'DELIVERED';
  const hasFeedback = localStorage.getItem(`eps_fb_${r.requestId}`);
  const statusSteps = ['PENDING','APPROVED','PO_ISSUED','ORDERED','DISPATCHED','DELIVERED'];
  const statusLabels = ['Submitted','Mgr Approved','PO Issued','Paid / Sourced','In Transit','Delivered'];
  const stepIcons = ['file-text','check-circle','file-check','credit-card','truck','package-check'];
  const normalizedStatus = (r.status === 'PAID') ? 'ORDERED' : r.status;
  const currentStep = statusSteps.indexOf(normalizedStatus);
  const img = r.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=56';

  const trackSteps = statusSteps.map((s,i) => {
    let cls = '';
    if (i < currentStep) cls = 'done';
    else if (i === currentStep) cls = 'active';
    return `
    <div class="track-step">
      <div class="track-dot ${cls}">
        <i data-lucide="${stepIcons[i]}" style="width:12px;height:12px;color:${cls?'#fff':'var(--text-muted)'}"></i>
      </div>
      <div class="track-label ${cls}">${statusLabels[i]}</div>
    </div>`;
  }).join('');

  return `
  <div class="request-card ${isNew ? 'just-ordered':''}">
    <div class="request-header" onclick="toggleRequest('req-${r.requestId}')">
      <img class="request-img" src="${img}" alt="${r.productName}" onerror="this.src='https://images.unsplash.com/photo-1497366216548-37526070297c?w=56'"/>
      <div class="request-meta">
        <div class="request-id" style="display:flex;align-items:center;gap:8px">
          <span>REQ #${r.requestId} Â· ${r.sku||'â€”'}</span>
          ${isNew ? '<span style="font-size:10px;background:rgba(99,102,241,0.25);color:var(--accent-light);padding:2px 8px;border-radius:10px;font-weight:700">âœ¨ NEW (1st)</span>' : ''}
        </div>
        <div class="request-name">${r.productName}</div>
        <div class="request-details">${r.categoryName||'â€”'} Â· Qty: ${r.quantity} Â· ${formatINR(r.totalPrice)} Â· ${formatDate(r.createdDate)}</div>
      </div>
      <span class="status-badge status-${r.status||'PENDING'}">${(r.status||'PENDING').replace(/_/g,' ')}</span>
      <i data-lucide="chevron-down" style="width:16px;height:16px;color:var(--text-muted);flex-shrink:0"></i>
    </div>
    <div class="request-expand" id="req-${r.requestId}" style="display:none">
      <div class="tracking-bar">${trackSteps}</div>
      <div class="request-info-grid">
        <div class="info-cell">
          <div class="info-cell-label">PO Number</div>
          <div class="info-cell-value">${r.poNumber||'Not issued yet'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Manager Review</div>
          <div class="info-cell-value">${r.managerApproval||'Awaiting Manager Review'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">FedEx Tracking #</div>
          <div class="info-cell-value">${r.trackingNumber||'Awaiting Dispatch'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Carrier</div>
          <div class="info-cell-value">${r.carrier||'FedEx Priority Freight'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Unit Price</div>
          <div class="info-cell-value">${formatINR(r.unitPrice||r.pricePerProduct)}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Est. Delivery</div>
          <div class="info-cell-value">${r.estimatedDelivery||'3-5 Business Days'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Department</div>
          <div class="info-cell-value">${r.departmentName||'â€”'}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Total Amount</div>
          <div class="info-cell-value" style="color:var(--accent-light)">${formatINR(r.totalPrice)}</div>
        </div>
        <div class="info-cell">
          <div class="info-cell-label">Submitted On</div>
          <div class="info-cell-value">${formatDate(r.createdDate)}</div>
        </div>
      </div>
      ${isDelivered ? `
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        ${hasFeedback ?
          '<span style="font-size:12px;color:var(--success);font-weight:700">âœ“ Feedback Submitted (â˜… 5.0)</span>' :
          `<button class="btn btn-warning btn-sm" onclick="openFeedbackModal(${JSON.stringify(r).replace(/"/g,'&quot;')})">â­ Rate Delivered Order</button>`
        }
      </div>` : ''}
    </div>
  </div>`;
}

function toggleRequest(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.style.display = el.style.display === 'none' ? 'block' : 'none';
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// POST-DELIVERY FEEDBACK & STAR RATING
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function openFeedbackModal(req) {
  feedbackCurrentReq = req;
  feedbackRatings = { quality: 5, speed: 5 };
  document.getElementById('fb-product-img').src = req.imageUrl || 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=100';
  document.getElementById('fb-product-name').textContent = req.productName || 'Delivered Product';
  document.getElementById('fb-request-id').textContent = `Requisition #${req.requestId} Â· Delivered`;
  document.getElementById('fb-comments').value = '';
  setStarRating('quality', 5);
  setStarRating('speed', 5);
  document.getElementById('feedback-modal-overlay').classList.add('open');
  lucide.createIcons();
}

function closeFeedbackModal() {
  document.getElementById('feedback-modal-overlay').classList.remove('open');
}

function setStarRating(type, val) {
  feedbackRatings[type] = val;
  const group = document.getElementById(`star-${type}-group`);
  if (!group) return;
  const btns = group.querySelectorAll('.star-btn');
  btns.forEach((b, idx) => {
    b.classList.toggle('active', idx < val);
  });
  const textEl = document.getElementById(`${type}-rating-text`);
  const descriptors = {
    quality: ['1.0 - Poor Quality','2.0 - Below Average','3.0 - Acceptable','4.0 - Good Quality','5.0 - Excellent Quality'],
    speed: ['1.0 - Severely Delayed','2.0 - Delayed','3.0 - Average Delivery','4.0 - Fast Fulfillment','5.0 - Prompt On-Time Delivery']
  };
  if (textEl) textEl.textContent = `${descriptors[type][val - 1]} (${val}.0 / 5.0)`;
}

function submitFeedback() {
  if (!feedbackCurrentReq) return;
  const comments = document.getElementById('fb-comments').value.trim() || 'Verified and approved.';
  const payload = {
    requestId: feedbackCurrentReq.requestId,
    productName: feedbackCurrentReq.productName,
    qualityRating: feedbackRatings.quality,
    speedRating: feedbackRatings.speed,
    comments,
    timestamp: new Date().toISOString()
  };
  localStorage.setItem(`eps_fb_${feedbackCurrentReq.requestId}`, JSON.stringify(payload));
  closeFeedbackModal();
  showToast(`â­ Thank you! Feedback submitted for ${feedbackCurrentReq.productName}.`, 'success');
  renderActiveOrders(allRequests);
  renderRequests(allRequests);
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// ROLE-AWARE CSV REPORTS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function buildReports() {
  const role = (currentUser?.role || 'USER').replace('ROLE_', '').toUpperCase();
  const email = currentUser?.email || '';
  let reports = [];

  if (role === 'MANAGER') {
    reports = [
      {
        title:'Department Requisitions CSV',
        desc:'Itemized list of all requisitions submitted within your authorized department scope with status and quantities.',
        icon:'file-text', color:'#6366f1', tags:['CSV','Manager','Department'],
        url:`${API}/api/reports/csv/manager/requests`
      },
      {
        title:'Manager Approval History CSV',
        desc:'Complete audit trail of all purchase requests approved or rejected by you, including remarks and timestamps.',
        icon:'check-circle', color:'#10b981', tags:['CSV','Audit','Decisions'],
        url:`${API}/api/reports/csv/manager/approvals`
      }
    ];
  } else if (role === 'ADMIN') {
    reports = [
      {
        title:'All Enterprise Requisitions CSV',
        desc:'Company-wide procurement requisitions across all departments with approval metadata and financial totals.',
        icon:'file-text', color:'#6366f1', tags:['CSV','Admin','Requisitions'],
        url:`${API}/api/reports/csv/admin/requests`
      },
      {
        title:'All Purchase Orders CSV',
        desc:'Confirmed POs with assigned vendors, logistics tracking numbers, carrier details, and fulfillment statuses.',
        icon:'package', color:'#10b981', tags:['CSV','Admin','Orders'],
        url:`${API}/api/reports/csv/admin/orders`
      },
      {
        title:'Active Industrial Suppliers CSV',
        desc:'Master directory of all registered vendors, assigned contracts, contact data, and compliance status.',
        icon:'truck', color:'#f59e0b', tags:['CSV','Vendors','Suppliers'],
        url:`${API}/api/reports/csv/admin/suppliers`
      },
      {
        title:'Financial Settlements & Disbursements CSV',
        desc:'Complete accounts payable records including transaction references, bank transfers, and paid invoices.',
        icon:'indian-rupee', color:'#06b6d4', tags:['CSV','Finance','Settlements'],
        url:`${API}/api/reports/csv/admin/payments`
      },
      {
        title:'Transaction Ledger CSV',
        desc:'Itemized ledger of all financial movements, PO disbursements, and budget charges.',
        icon:'credit-card', color:'#8b5cf6', tags:['CSV','Ledger','Compliance'],
        url:`${API}/api/reports/csv/admin/transactions`
      },
      {
        title:'Global Tracking Pipeline CSV',
        desc:'End-to-end supply chain visibility from requisition submission to final warehouse delivery.',
        icon:'map-pin', color:'#ec4899', tags:['CSV','Supply Chain','Logistics'],
        url:`${API}/api/reports/csv/admin/tracking`
      }
    ];
  } else if (role === 'SUPPLIER') {
    reports = [
      {
        title:'Assigned Orders CSV',
        desc:'Procurement orders awarded to your company for fulfillment with product specifications and quantities.',
        icon:'package', color:'#06b6d4', tags:['CSV','Supplier','Orders'],
        url:`${API}/api/reports/csv/supplier/orders`
      },
      {
        title:'Supplier Payment Remittances CSV',
        desc:'Bank transfers and settled payments for delivered goods with invoice numbers and reference IDs.',
        icon:'indian-rupee', color:'#10b981', tags:['CSV','Remittance','Invoices'],
        url:`${API}/api/reports/csv/supplier/payments`
      },
      {
        title:'Fulfillment & Delivery Schedule CSV',
        desc:'Dispatched shipments, carrier tracking numbers, and delivery confirmation records.',
        icon:'truck', color:'#f59e0b', tags:['CSV','Logistics','Delivery'],
        url:`${API}/api/reports/csv/supplier/delivery`
      },
      {
        title:'Order Tracking Status CSV',
        desc:'Current state of all active and completed orders in your vendor pipeline.',
        icon:'map-pin', color:'#6366f1', tags:['CSV','Status','Audit'],
        url:`${API}/api/reports/csv/supplier/tracking`
      }
    ];
  } else {
    reports = [
      {
        title:'My Purchase Requests', desc:'All purchase requests submitted by you with status, quantities, pricing in â‚¹, and department breakdowns.',
        icon:'file-text', color:'#6366f1', tags:['CSV','Requests','â‚¹ INR'],
        url:`${API}/api/reports/csv/user/requests?email=${email}`
      },
      {
        title:'My Purchase Orders', desc:'Export confirmed POs with supplier details, tracking numbers, and delivery status in â‚¹.',
        icon:'package', color:'#10b981', tags:['CSV','Orders','Tracking'],
        url:`${API}/api/reports/csv/user/orders?email=${email}`
      },
      {
        title:'My Payment History', desc:'Itemized payment records including transaction references, amounts in â‚¹, and PO numbers.',
        icon:'indian-rupee', color:'#f59e0b', tags:['CSV','Payments','Finance'],
        url:`${API}/api/reports/csv/user/payments?email=${email}`
      },
      {
        title:'Order Tracking Report', desc:'Complete delivery tracking report with carrier, tracking numbers, and estimated delivery dates.',
        icon:'truck', color:'#06b6d4', tags:['CSV','Tracking','Delivery'],
        url:`${API}/api/reports/csv/user/tracking?email=${email}`
      },
    ];
  }

  const grid = document.getElementById('report-grid');
  if (!grid) return;
  grid.innerHTML = reports.map(r => `
    <div class="report-card" onclick="downloadReport('${r.url}','${r.title}')">
      <div class="report-icon" style="background:${r.color}20">
        <i data-lucide="${r.icon}" style="width:22px;height:22px;color:${r.color}"></i>
      </div>
      <div class="report-title">${r.title}</div>
      <div class="report-desc">${r.desc}</div>
      <div class="report-meta">${r.tags.map(t=>`<span class="report-tag">${t}</span>`).join('')}</div>
      <button class="btn btn-secondary btn-sm" style="width:100%">
        <i data-lucide="download" style="width:13px;height:13px"></i>Download CSV
      </button>
    </div>
  `).join('');
  lucide.createIcons();
}

async function downloadReport(url, title) {
  showToast(`â³ Generating <strong>${title}</strong> reportâ€¦`, 'info');
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(url, {headers});
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${title.replace(/\s+/g,'_')}_${new Date().toISOString().slice(0,10)}.csv`;
    a.click();
    showToast(`âœ… <strong>${title}</strong> downloaded!`, 'success');
  } catch(e) {
    showToast(`Download failed: ${e.message}`, 'error');
  }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// 1. MANAGER CONTROLLER JS (100% REAL DATABASE)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
let managerPendingRequests = [];
let managerHistoryRequests = [];
let managerOrders = [];
let mgrRejectTargetId = null;

async function loadManagerData() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    // 1. Dashboard Stats
    const statsRes = await fetch(`${API}/api/manager/dashboard-stats`, { headers });
    if (statsRes.ok) {
      const statsJson = await statsRes.json();
      const s = statsJson.data || {};
      document.getElementById('mgr-stat-total').textContent = s.totalRequests || 0;
      document.getElementById('mgr-stat-pending').textContent = s.pendingApproval || 0;
      document.getElementById('mgr-stat-approved').textContent = s.approved || 0;
      document.getElementById('mgr-stat-rejected').textContent = s.rejected || 0;
      document.getElementById('mgr-quick-pending-count').textContent = s.pendingApproval || 0;
      const deptEl = document.getElementById('mgr-dept-name');
      if (deptEl && s.departmentName) deptEl.textContent = s.departmentName;

      const badge = document.getElementById('mgr-badge-pending');
      if (badge) {
        badge.textContent = s.pendingApproval || 0;
        badge.style.display = (s.pendingApproval > 0) ? 'block' : 'none';
      }
    }

    // 2. Pending Requests
    const pendRes = await fetch(`${API}/api/manager/pending-requests`, { headers });
    if (pendRes.ok) {
      const pendJson = await pendRes.json();
      managerPendingRequests = pendJson.data || [];
      renderManagerPendingTables();
    }

    // 3. Approval History
    const histRes = await fetch(`${API}/api/manager/approval-history`, { headers });
    if (histRes.ok) {
      const histJson = await histRes.json();
      managerHistoryRequests = histJson.data || [];
      renderManagerHistoryTable();
    }

    // 4. Department Orders
    const ordRes = await fetch(`${API}/api/manager/department-orders`, { headers });
    if (ordRes.ok) {
      managerOrders = await ordRes.json() || [];
      renderManagerOrdersTable();
    }
  } catch(e) {
    console.error('Manager data load failed', e);
  }
}

function renderManagerPendingTables() {
  const overviewEl = document.getElementById('mgr-overview-pending-list');
  const fullEl = document.getElementById('mgr-full-pending-table');

  const rows = managerPendingRequests.map(r => `
    <tr>
      <td><strong>#${r.requestId}</strong></td>
      <td>${r.user ? r.user.fullName : 'Employee'}</td>
      <td>${r.product ? r.product.name : 'Item'}</td>
      <td>${r.department ? r.department.departmentName : 'Dept'}</td>
      <td><strong>${r.quantity}</strong></td>
      <td style="font-weight:700;color:var(--accent-light)">${formatINR(r.totalPrice)}</td>
      <td><span class="status-badge status-PENDING">PENDING</span></td>
      <td>${formatDate(r.createdDate)}</td>
      <td>
        <div class="table-actions">
          <button class="btn btn-success btn-sm" onclick="approveManagerRequest(${r.requestId})">
            <i data-lucide="check" style="width:13px;height:13px"></i>Approve
          </button>
          <button class="btn btn-danger btn-sm" onclick="openMgrRejectModal(${r.requestId}, '${(r.product ? r.product.name : 'Item').replace(/'/g,"\\'")}')">
            <i data-lucide="x" style="width:13px;height:13px"></i>Reject
          </button>
        </div>
      </td>
    </tr>
  `).join('');

  const tableHtml = managerPendingRequests.length ? `
    <table class="data-table">
      <thead>
        <tr>
          <th>Req #</th>
          <th>Requester</th>
          <th>Product</th>
          <th>Department</th>
          <th>Qty</th>
          <th>Total</th>
          <th>Status</th>
          <th>Date</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  ` : `<div style="text-align:center;padding:48px;color:var(--text-muted)"><i data-lucide="check-circle" style="width:36px;height:36px;opacity:0.3;margin-bottom:8px"></i><div>No pending requisitions awaiting approval. All clear!</div></div>`;

  if (overviewEl) overviewEl.innerHTML = tableHtml;
  if (fullEl) fullEl.innerHTML = tableHtml;
  lucide.createIcons();
}

function filterManagerPending() {
  const term = (document.getElementById('mgr-pending-search')?.value || '').toLowerCase();
  const filtered = managerPendingRequests.filter(r =>
    String(r.requestId).includes(term) ||
    (r.user && r.user.fullName.toLowerCase().includes(term)) ||
    (r.product && r.product.name.toLowerCase().includes(term))
  );

  const fullEl = document.getElementById('mgr-full-pending-table');
  if (!fullEl) return;
  if (!filtered.length) {
    fullEl.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">No matching requisitions found</div>';
    return;
  }
  const rows = filtered.map(r => `
    <tr>
      <td><strong>#${r.requestId}</strong></td>
      <td>${r.user ? r.user.fullName : 'Employee'}</td>
      <td>${r.product ? r.product.name : 'Item'}</td>
      <td>${r.department ? r.department.departmentName : 'Dept'}</td>
      <td><strong>${r.quantity}</strong></td>
      <td style="font-weight:700;color:var(--accent-light)">${formatINR(r.totalPrice)}</td>
      <td><span class="status-badge status-PENDING">PENDING</span></td>
      <td>${formatDate(r.createdDate)}</td>
      <td>
        <div class="table-actions">
          <button class="btn btn-success btn-sm" onclick="approveManagerRequest(${r.requestId})">
            <i data-lucide="check" style="width:13px;height:13px"></i>Approve
          </button>
          <button class="btn btn-danger btn-sm" onclick="openMgrRejectModal(${r.requestId}, '${(r.product ? r.product.name : 'Item').replace(/'/g,"\\'")}')">
            <i data-lucide="x" style="width:13px;height:13px"></i>Reject
          </button>
        </div>
      </td>
    </tr>
  `).join('');

  fullEl.innerHTML = `<table class="data-table"><thead><tr><th>Req #</th><th>Requester</th><th>Product</th><th>Department</th><th>Qty</th><th>Total</th><th>Status</th><th>Date</th><th>Actions</th></tr></thead><tbody>${rows}</tbody></table>`;
  lucide.createIcons();
}

async function approveManagerRequest(id) {
  showToast(`Processing approval for Requisition #${id}â€¦`, 'info');
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/manager/requests/${id}/approve`, {
      method: 'POST', headers,
      body: JSON.stringify({ remarks: 'Authorized by Department Manager' })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Approval failed');
    showToast(`âœ… Requisition #${id} APPROVED! Passed to Procurement.`, 'success');
    loadManagerData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function openMgrRejectModal(id, product) {
  mgrRejectTargetId = id;
  document.getElementById('mgr-reject-req-id').textContent = id;
  document.getElementById('mgr-reject-req-product').textContent = product;
  document.getElementById('mgr-reject-reason').value = '';
  document.getElementById('mgr-reject-modal').classList.add('open');
}

function closeMgrRejectModal() {
  document.getElementById('mgr-reject-modal').classList.remove('open');
  mgrRejectTargetId = null;
}

async function submitMgrReject() {
  if (!mgrRejectTargetId) return;
  const reason = document.getElementById('mgr-reject-reason').value.trim();
  if (!reason) {
    showToast('Rejection reason is mandatory.', 'warning');
    document.getElementById('mgr-reject-reason').focus();
    return;
  }

  showToast(`Submitting rejection for Requisition #${mgrRejectTargetId}â€¦`, 'info');
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/manager/requests/${mgrRejectTargetId}/reject`, {
      method: 'POST', headers,
      body: JSON.stringify({ remarks: reason })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Rejection failed');
    closeMgrRejectModal();
    showToast(`Requisition #${mgrRejectTargetId} has been REJECTED.`, 'warning');
    loadManagerData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function renderManagerHistoryTable() {
  const el = document.getElementById('mgr-history-table');
  if (!el) return;
  if (!managerHistoryRequests.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No past decision history found</div>';
    return;
  }
  const rows = managerHistoryRequests.map(r => `
    <tr>
      <td><strong>#${r.requestId}</strong></td>
      <td>${r.user ? r.user.fullName : 'Employee'}</td>
      <td>${r.product ? r.product.name : 'Item'}</td>
      <td>${formatINR(r.totalPrice)}</td>
      <td><span class="status-badge status-${r.status}">${r.status}</span></td>
      <td>${r.approvedBy || r.rejectedBy || 'Manager'}</td>
      <td>${formatDate(r.approvalDate || r.rejectionDate || r.createdDate)}</td>
      <td style="font-size:12px;color:var(--text-secondary)">${r.managerComments || 'â€”'}</td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Req #</th>
          <th>Requester</th>
          <th>Product</th>
          <th>Total</th>
          <th>Decision</th>
          <th>Decided By</th>
          <th>Decision Date</th>
          <th>Comments / Reason</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function renderManagerOrdersTable() {
  const el = document.getElementById('mgr-orders-table');
  if (!el) return;
  if (!managerOrders.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No department orders found</div>';
    return;
  }
  const rows = managerOrders.map(o => `
    <tr>
      <td><strong>ORD-${o.orderId}</strong></td>
      <td>#${o.purchaseRequest ? o.purchaseRequest.requestId : 'â€”'}</td>
      <td>${o.purchaseRequest && o.purchaseRequest.product ? o.purchaseRequest.product.name : 'Goods'}</td>
      <td>${o.supplier ? o.supplier.name : '<span style="color:var(--warning)">Awaiting Sourcing</span>'}</td>
      <td style="font-weight:700;color:var(--success)">${formatINR(o.totalAmount)}</td>
      <td><span class="status-badge status-${o.status||'CREATED'}">${o.status||'CREATED'}</span></td>
      <td>${o.trackingNumber || 'Awaiting dispatch'}</td>
      <td>${formatDate(o.createdDate)}</td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Order #</th>
          <th>Req #</th>
          <th>Product</th>
          <th>Supplier</th>
          <th>Amount</th>
          <th>Status</th>
          <th>Tracking</th>
          <th>Created</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// 2. ADMIN CONTROLLER JS (100% REAL DATABASE)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
let adminRequests = [];
let adminOrders = [];
let adminSuppliers = [];
let adminPayments = [];
let adminUsers = [];
let adminAssignTargetOrderId = null;
let adminPayTargetOrderId = null;

async function loadAdminData() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    // 1. Admin Stats
    const sRes = await fetch(`${API}/api/admin/dashboard-stats`, { headers });
    if (sRes.ok) {
      const sJson = await sRes.json();
      const s = sJson.data || {};
      document.getElementById('admin-stat-users').textContent = s.totalUsers || 0;
      document.getElementById('admin-stat-managers').textContent = s.totalManagers || 0;
      document.getElementById('admin-stat-suppliers').textContent = s.totalSuppliers || 0;
      document.getElementById('admin-stat-requests').textContent = s.totalPurchaseRequests || 0;
      document.getElementById('admin-stat-pending').textContent = s.pendingApprovals || 0;
      document.getElementById('admin-stat-orders').textContent = s.activeOrders || 0;
      document.getElementById('admin-stat-completed').textContent = s.completedOrders || 0;
      document.getElementById('admin-stat-spend').textContent = formatINR(s.totalProcurementValue || 0);
      document.getElementById('admin-stat-payments').textContent = s.pendingPayments || 0;

      const badge = document.getElementById('admin-badge-requests');
      if (badge) {
        badge.textContent = s.totalPurchaseRequests || 0;
        badge.style.display = (s.totalPurchaseRequests > 0) ? 'block' : 'none';
      }
    }

    // 2. All Requisitions
    const rRes = await fetch(`${API}/api/admin/requests`, { headers });
    if (rRes.ok) {
      const rJson = await rRes.json();
      adminRequests = rJson.data || [];
      renderAdminRequestsTables();
    }

    // 3. All Orders
    const oRes = await fetch(`${API}/api/admin/orders`, { headers });
    if (oRes.ok) {
      const oJson = await oRes.json();
      adminOrders = oJson.data || [];
      renderAdminOrdersTable();
    }

    // 4. All Suppliers
    const supRes = await fetch(`${API}/api/admin/suppliers`, { headers });
    if (supRes.ok) {
      const supJson = await supRes.json();
      adminSuppliers = supJson.data || [];
      renderAdminSuppliersTable();
      populateAdminSupplierDropdown();
    }

    // 5. All Payments
    const payRes = await fetch(`${API}/api/admin/payments`, { headers });
    if (payRes.ok) {
      const payJson = await payRes.json();
      adminPayments = payJson.data || [];
      renderAdminPaymentsTable();
    }

    // 6. Real Users
    const uRes = await fetch(`${API}/api/admin/users`, { headers });
    if (uRes.ok) {
      const uJson = await uRes.json();
      adminUsers = uJson.data || [];
      renderAdminUsersTable();
    }
  } catch(e) {
    console.error('Admin data load failed', e);
  }
}

function buildAdminRequestActionMenu(r) {
  const reqEscaped = {
    id: r.requestId,
    price: r.totalPrice,
    name: (r.productName || 'Goods').replace(/'/g, "\\'"),
    user: (r.userName || 'Employee').replace(/'/g, "\\'")
  };
  
  if (r.status === 'APPROVED') {
    return `
    <div class="sup-status-dropdown" id="adm-act-drop-${r.requestId}">
      <button class="btn btn-primary btn-sm sup-status-toggle" onclick="toggleAdminReqDropdown(${r.requestId})" style="min-width:110px">
        Actions <span style="font-size:10px;margin-left:2px">▼</span>
      </button>
      <div class="sup-status-menu" id="adm-act-menu-${r.requestId}" role="menu">
        <div class="sup-status-option" onclick="processAdminRequest(${r.requestId}); closeAdminReqDropdowns();">
          <i data-lucide="play" style="width:12px;height:12px"></i> Issue PO
        </div>
      </div>
    </div>`;
  }
  
  if (r.status === 'PO_ISSUED') {
    return `
    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
      <span style="font-size:11px;color:var(--info);font-weight:700;display:inline-flex;align-items:center;gap:3px">
        <i data-lucide="check-circle" style="width:12px;height:12px"></i>PO Issued ✓
      </span>
      <div class="sup-status-dropdown" id="adm-act-drop-${r.requestId}">
        <button class="btn btn-success btn-sm sup-status-toggle" onclick="toggleAdminReqDropdown(${r.requestId})" style="min-width:95px;background:linear-gradient(135deg,rgba(16,185,129,0.2) 0%,rgba(16,185,129,0.4) 100%);border-color:rgba(16,185,129,0.5)">
          Actions <span style="font-size:10px;margin-left:2px">▼</span>
        </button>
        <div class="sup-status-menu" id="adm-act-menu-${r.requestId}" role="menu">
          <div class="sup-status-option" onclick="openAdminPayModalForReq(${reqEscaped.id}, ${reqEscaped.price}, '${reqEscaped.name}', '${reqEscaped.user}'); closeAdminReqDropdowns();">
            <i data-lucide="zap" style="width:12px;height:12px"></i> Pay
          </div>
        </div>
      </div>
    </div>`;
  }
  
  const isPaidOrBeyond = ['ORDERED', 'PAID', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DISPATCHED'].includes((r.status || '').toUpperCase());
  if (isPaidOrBeyond) {
    return `<span style="font-size:11.5px;color:var(--success);font-weight:700;white-space:nowrap"><i data-lucide="check-check" style="width:13px;height:13px;vertical-align:middle;margin-right:2px"></i>PO Issued ✓ | Payment Completed ✓</span>`;
  }
  
  return `<span style="font-size:11px;color:var(--text-muted)">—</span>`;
}

function toggleAdminReqDropdown(reqId) {
  closeAdminReqDropdowns(`adm-act-menu-${reqId}`);
  closeStatusDropdowns();
  const menu = document.getElementById(`adm-act-menu-${reqId}`);
  if (menu) menu.classList.toggle('open');
}

function closeAdminReqDropdowns(exceptId) {
  document.querySelectorAll('[id^="adm-act-menu-"].open').forEach(m => {
    if (exceptId && m.id === exceptId) return;
    m.classList.remove('open');
  });
}

function renderAdminRequestsTables() {
  const overviewEl = document.getElementById('admin-overview-requests-table');
  const fullEl = document.getElementById('admin-full-requests-table');

  const pendingAdminList = adminRequests.filter(r => r.status === 'APPROVED' || r.status === 'PO_ISSUED');
  const overviewRows = pendingAdminList.slice(0, 8).map(r => `
    <tr>
      <td><strong>#${r.requestId}</strong></td>
      <td>${r.userName || 'Requester'}</td>
      <td>${r.departmentName || 'Dept'}</td>
      <td>${r.productName || 'Product'} × ${r.quantity}</td>
      <td style="font-weight:700;color:var(--accent-light)">${formatINR(r.totalPrice)}</td>
      <td><span class="status-badge status-${r.status}">${r.status}</span></td>
      <td>
        <div class="table-actions">
          ${buildAdminRequestActionMenu(r)}
        </div>
      </td>
    </tr>
  `).join('');

  if (overviewEl) {
    overviewEl.innerHTML = pendingAdminList.length ? `
      <table class="data-table">
        <thead>
          <tr><th>Req #</th><th>Requester</th><th>Department</th><th>Item &amp; Qty</th><th>Total</th><th>Status</th><th>Action</th></tr>
        </thead>
        <tbody>${overviewRows}</tbody>
      </table>
    ` : `<div style="text-align:center;padding:40px;color:var(--text-muted)">No approved requisitions awaiting sourcing</div>`;
  }

  renderAdminFullRequestsTable(adminRequests);
  lucide.createIcons();
}

function renderAdminFullRequestsTable(list) {
  const fullEl = document.getElementById('admin-full-requests-table');
  if (!fullEl) return;
  if (!list.length) {
    fullEl.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No requisitions found in database</div>';
    return;
  }
  const rows = list.map(r => `
    <tr>
      <td><strong>#${r.requestId}</strong></td>
      <td>${r.userName || 'Employee'} (${r.userEmail || '—'})</td>
      <td>${r.departmentName || 'Dept'}</td>
      <td>${r.productName || 'Item'} × ${r.quantity}</td>
      <td style="font-weight:700;color:var(--accent-light)">${formatINR(r.totalPrice)}</td>
      <td><span class="status-badge status-${r.status}">${r.status}</span></td>
      <td>${r.approvedBy || r.rejectedBy || '—'}</td>
      <td>${formatDate(r.createdDate)}</td>
      <td>
        <div class="table-actions">
          ${buildAdminRequestActionMenu(r)}
        </div>
      </td>
    </tr>
  `).join('');

  fullEl.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Req #</th>
          <th>Requester</th>
          <th>Department</th>
          <th>Product</th>
          <th>Total</th>
          <th>Status</th>
          <th>Manager Review</th>
          <th>Submitted</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function filterAdminRequests() {
  const term = (document.getElementById('admin-req-search')?.value || '').toLowerCase();
  const statusFilter = document.getElementById('admin-req-filter-status')?.value || 'ALL';

  const filtered = adminRequests.filter(r => {
    const matchText = String(r.requestId).includes(term) ||
      (r.userName && r.userName.toLowerCase().includes(term)) ||
      (r.productName && r.productName.toLowerCase().includes(term));
    const matchStatus = statusFilter === 'ALL' || r.status === statusFilter;
    return matchText && matchStatus;
  });

  renderAdminFullRequestsTable(filtered);
}

async function processAdminRequest(id) {
  showToast(`Creating Purchase Order for Requisition #${id}â€¦`, 'info');
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/admin/requests/${id}/process`, {
      method: 'POST', headers
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Processing failed');
    showToast(`ðŸŽ‰ Requisition #${id} processed into Purchase Order! Assign supplier next.`, 'success');
    loadAdminData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function renderAdminOrdersTable() {
  const el = document.getElementById('admin-full-orders-table');
  if (!el) return;
  if (!adminOrders.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No purchase orders created yet</div>';
    return;
  }
  const rows = adminOrders.map(o => `
    <tr>
      <td><strong>ORD-${o.orderId}</strong></td>
      <td>#${o.requestId || 'â€”'}</td>
      <td>${o.userName || 'Employee'} (${o.departmentName || 'Dept'})</td>
      <td>${o.productName || 'Goods'} Ã— ${o.quantity || 1}</td>
      <td style="font-weight:700;color:var(--success)">${formatINR(o.totalAmount)}</td>
      <td>${o.supplierName ? `<strong style="color:var(--info)">${o.supplierName}</strong>` : '<span style="color:var(--warning)">Unassigned</span>'}</td>
      <td><span class="status-badge status-${o.status||'CREATED'}">${o.status||'CREATED'}</span></td>
      <td>${o.trackingNumber || 'Pending dispatch'}</td>
      <td>
        <div class="table-actions">
          ${!o.supplierName ? `
            <button class="btn btn-primary btn-sm" onclick="openAdminAssignModal(${o.orderId})">
              <i data-lucide="user-plus" style="width:12px;height:12px"></i>Assign Supplier
            </button>
          ` : ''}
          ${o.status === 'PAID' ? `
            <span style="color:var(--success);font-weight:700;font-size:12px">âœ“ Paid</span>
          ` : `
            <button class="btn btn-success btn-sm" onclick="openAdminPayModalForOrder(${o.orderId}, ${o.totalAmount}, '${(o.productName || 'Goods').replace(/'/g, "\\'")}', '${(o.supplierName || 'Vendor').replace(/'/g, "\\'")}')">
              <i data-lucide="zap" style="width:12px;height:12px"></i>Pay (UPI/Card)
            </button>
          `}
        </div>
      </td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Order #</th>
          <th>Req #</th>
          <th>Requester</th>
          <th>Product</th>
          <th>Amount</th>
          <th>Supplier</th>
          <th>Status</th>
          <th>Tracking</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function populateAdminSupplierDropdown() {
  const select = document.getElementById('admin-assign-supplier-select');
  if (!select) return;
  select.innerHTML = adminSuppliers.map(s => `
    <option value="${s.supplierId}">${s.name} (${s.email || 'Vendor'}) Â· Status: ${s.status}</option>
  `).join('');
}

function openAdminAssignModal(orderId) {
  adminAssignTargetOrderId = orderId;
  document.getElementById('admin-assign-order-id').textContent = orderId;
  populateAdminSupplierDropdown();
  document.getElementById('admin-assign-modal').classList.add('open');
}

function closeAdminAssignModal() {
  document.getElementById('admin-assign-modal').classList.remove('open');
  adminAssignTargetOrderId = null;
}

async function submitAdminAssignSupplier() {
  if (!adminAssignTargetOrderId) return;
  const supplierId = document.getElementById('admin-assign-supplier-select').value;
  if (!supplierId) {
    showToast('Please select a supplier.', 'warning');
    return;
  }

  showToast(`Assigning supplier to Order ORD-${adminAssignTargetOrderId}â€¦`, 'info');
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/admin/orders/${adminAssignTargetOrderId}/assign-supplier`, {
      method: 'POST', headers,
      body: JSON.stringify({ supplierId: parseInt(supplierId) })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Assignment failed');
    closeAdminAssignModal();
    showToast(`âœ… Supplier assigned! Vendor has received procurement notification.`, 'success');
    loadAdminData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

let adminPayTargetOrderId = null;
let adminPayTargetRequestId = null;
let adminPayAmount = 0;
let adminPayCurrentMethod = 'UPI';
let adminPaySelectedUpiApp = 'Google Pay';
let currentMpin = '';

function openAdminPayModalForOrder(orderId, amount, productName, supplierName) {
  adminPayTargetOrderId = orderId;
  adminPayTargetRequestId = null;
  adminPayAmount = amount;
  
  document.getElementById('admin-pay-amount').textContent = formatINR(amount);
  document.getElementById('admin-pay-target-desc').textContent = `Order ORD-${orderId} Â· ${productName || 'Procurement Goods'} Â· Supplier: ${supplierName || 'Vendor'}`;
  resetPayGatewayScreens();
  document.getElementById('admin-pay-modal').classList.add('open');
  lucide.createIcons();
}

function openAdminPayModalForReq(requestId, amount, productName, requesterName) {
  adminPayTargetRequestId = requestId;
  adminPayTargetOrderId = null;
  adminPayAmount = amount;
  
  document.getElementById('admin-pay-amount').textContent = formatINR(amount);
  document.getElementById('admin-pay-target-desc').textContent = `Requisition #${requestId} Â· ${productName || 'Goods'} Â· Requester: ${requesterName || 'Employee'}`;
  resetPayGatewayScreens();
  document.getElementById('admin-pay-modal').classList.add('open');
  lucide.createIcons();
}

function openAdminPayModal(orderId, amount) {
  openAdminPayModalForOrder(orderId, amount, 'Enterprise Goods', 'Supplier');
}

function resetPayGatewayScreens() {
  currentMpin = '';
  updateMpinUI();
  document.getElementById('pay-screen-methods').style.display = 'block';
  document.getElementById('pay-screen-mpin').style.display = 'none';
  document.getElementById('pay-screen-processing').style.display = 'none';
  document.getElementById('pay-screen-success').style.display = 'none';
  switchPayTab('upi');
}

function closeAdminPayModal() {
  document.getElementById('admin-pay-modal').classList.remove('open');
  adminPayTargetOrderId = null;
  adminPayTargetRequestId = null;
}

function switchPayTab(tab) {
  adminPayCurrentMethod = tab.toUpperCase();
  document.querySelectorAll('.pay-tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.pay-tab-content').forEach(c => c.classList.remove('active'));

  const btn = document.getElementById(`tabbtn-${tab}`);
  const content = document.getElementById(`tabcontent-${tab}`);
  if (btn) btn.classList.add('active');
  if (content) content.classList.add('active');

  const proceedBtn = document.getElementById('btn-proceed-text');
  if (tab === 'cash') {
    proceedBtn.textContent = 'Record Cash Disbursement';
  } else if (tab === 'qr') {
    proceedBtn.textContent = 'Authorize via QR Scanner';
  } else {
    proceedBtn.textContent = `Proceed to Enter MPIN (${formatINR(adminPayAmount)})`;
  }
}

function selectUpiApp(app) {
  adminPaySelectedUpiApp = app;
  document.querySelectorAll('.upi-app-card').forEach(c => c.classList.remove('selected'));
  if (app === 'Google Pay') document.getElementById('app-card-gpay')?.classList.add('selected');
  else if (app === 'PhonePe') document.getElementById('app-card-phonepe')?.classList.add('selected');
  else if (app === 'Paytm') document.getElementById('app-card-paytm')?.classList.add('selected');
  else if (app === 'BHIM UPI') document.getElementById('app-card-bhim')?.classList.add('selected');
  showToast(`Switched to ${app} Instant UPI`, 'info');
}

function selectBank(bankName) {
  document.getElementById('pay-netbank-name').value = `${bankName} - Corporate Banking`;
  showToast(`Selected ${bankName}`, 'info');
}

function simulateQrScanAndPay() {
  showToast('ðŸ“± Scanning UPI QR with PhonePe / GPayâ€¦', 'info');
  setTimeout(() => {
    proceedToMpin();
  }, 700);
}

function proceedToMpin() {
  if (adminPayCurrentMethod === 'CASH') {
    // Direct settlement without MPIN for cash voucher
    executePaymentSettlement('CASH', document.getElementById('pay-cash-voucher').value || `VCHR-${Date.now()}`);
    return;
  }
  document.getElementById('pay-screen-methods').style.display = 'none';
  document.getElementById('pay-screen-mpin').style.display = 'block';
  currentMpin = '';
  updateMpinUI();
  lucide.createIcons();
}

function backToMethods() {
  document.getElementById('pay-screen-mpin').style.display = 'none';
  document.getElementById('pay-screen-methods').style.display = 'block';
  lucide.createIcons();
}

function enterMpinDigit(d) {
  if (currentMpin.length < 4) {
    currentMpin += d;
    updateMpinUI();
    if (currentMpin.length === 4) {
      setTimeout(() => { submitMpinAndAuthorize(); }, 250);
    }
  }
}

function backspaceMpin() {
  if (currentMpin.length > 0) {
    currentMpin = currentMpin.slice(0, -1);
    updateMpinUI();
  }
}

function quickFillMpin() {
  currentMpin = '1234';
  updateMpinUI();
  setTimeout(() => { submitMpinAndAuthorize(); }, 250);
}

function updateMpinUI() {
  for (let i = 1; i <= 4; i++) {
    const el = document.getElementById(`mpin-${i}`);
    if (!el) continue;
    if (i <= currentMpin.length) {
      el.textContent = 'â€¢';
      el.classList.add('filled');
    } else {
      el.textContent = '';
      el.classList.remove('filled');
    }
  }
}

async function submitMpinAndAuthorize() {
  if (currentMpin.length < 4) {
    showToast('Please enter all 4 digits of your UPI MPIN.', 'warning');
    return;
  }

  // Determine method label & reference
  let methodLabel = 'UPI';
  let refPrefix = 'TXN-UPI';
  if (adminPayCurrentMethod === 'UPI') {
    methodLabel = `UPI (${adminPaySelectedUpiApp})`;
    refPrefix = `TXN-${adminPaySelectedUpiApp.replace(/\s+/g,'').toUpperCase()}`;
  } else if (adminPayCurrentMethod === 'QR') {
    methodLabel = 'UPI (QR Scan)';
    refPrefix = 'TXN-QRSCAN';
  } else if (adminPayCurrentMethod === 'CARD') {
    methodLabel = 'DEBIT/CREDIT CARD';
    refPrefix = 'TXN-CARD';
  } else if (adminPayCurrentMethod === 'NETBANKING') {
    methodLabel = `NET BANKING (${document.getElementById('pay-netbank-name').value.split(' - ')[0]})`;
    refPrefix = 'TXN-NETBANK';
  }

  const txnRef = `${refPrefix}-${Date.now().toString().slice(-8)}`;

  // Show Razorpay / PhonePe processing animation
  document.getElementById('pay-screen-mpin').style.display = 'none';
  document.getElementById('pay-screen-processing').style.display = 'block';
  
  const procTitle = document.getElementById('proc-title');
  const procSub = document.getElementById('proc-sub');

  procTitle.textContent = 'Connecting to Bank Gatewayâ€¦';
  procSub.textContent = 'Authorizing MPIN with NPCI Switch';

  await new Promise(r => setTimeout(r, 600));
  procTitle.textContent = `Authorizing ${formatINR(adminPayAmount)}â€¦`;
  procSub.textContent = 'Transferring funds from EPS Corporate Account to Supplier';

  await new Promise(r => setTimeout(r, 600));

  await executePaymentSettlement(methodLabel, txnRef);
}

async function executePaymentSettlement(method, ref) {
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    let endpoint = '';
    if (adminPayTargetOrderId) {
      endpoint = `${API}/api/admin/orders/${adminPayTargetOrderId}/pay`;
    } else if (adminPayTargetRequestId) {
      endpoint = `${API}/api/admin/requests/${adminPayTargetRequestId}/pay`;
    } else {
      throw new Error('No target order or requisition specified for payment.');
    }

    const payload = {
      paymentMethod: method,
      transactionReference: ref,
      amount: adminPayAmount
    };

    const res = await fetch(endpoint, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload)
    });

    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Payment recording failed.');

    // Show Success Screen
    document.getElementById('pay-screen-processing').style.display = 'none';
    document.getElementById('pay-screen-methods').style.display = 'none';
    document.getElementById('pay-screen-success').style.display = 'block';

    document.getElementById('receipt-amount').textContent = formatINR(adminPayAmount);
    document.getElementById('receipt-ref').textContent = ref;
    document.getElementById('receipt-method').textContent = method;
    document.getElementById('receipt-supplier').textContent = result.data?.invoice?.supplier?.name || 'Verified Supplier';

    showToast(`ðŸŽ‰ Payment of ${formatINR(adminPayAmount)} settled via ${method}!`, 'success');
    lucide.createIcons();

    // Auto update in the background for all roles
    loadAdminData();
    loadRealNotifications();

  } catch (err) {
    document.getElementById('pay-screen-processing').style.display = 'none';
    document.getElementById('pay-screen-methods').style.display = 'block';
    showToast(`Payment error: ${err.message}`, 'error');
  }
}

function finishPaymentAndClose() {
  closeAdminPayModal();
  loadAdminData();
  loadRealNotifications();
  if (currentUser) {
    const role = (currentUser.role || 'USER').replace('ROLE_', '').toUpperCase();
    if (role === 'MANAGER') loadManagerData();
    else if (role === 'SUPPLIER') loadSupplierData();
    else if (role === 'USER') loadRequests();
  }
}

function renderAdminSuppliersTable() {
  const el = document.getElementById('admin-suppliers-table');
  if (!el) return;
  if (!adminSuppliers.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No suppliers registered in database</div>';
    return;
  }
  const rows = adminSuppliers.map(s => `
    <tr>
      <td><strong>SUP-${s.supplierId}</strong></td>
      <td><strong>${s.name}</strong></td>
      <td>${s.email || 'â€”'}</td>
      <td>${s.phone || 'â€”'}</td>
      <td><span class="status-badge status-APPROVED">${s.status || 'ACTIVE'}</span></td>
      <td>â­ ${s.rating || '5.0'}</td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Supplier ID</th>
          <th>Company / Vendor Name</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Status</th>
          <th>Rating</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function renderAdminPaymentsTable() {
  const el = document.getElementById('admin-payments-table');
  if (!el) return;
  if (!adminPayments.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No financial settlement records yet</div>';
    return;
  }
  const rows = adminPayments.map(p => `
    <tr>
      <td><strong>PAY-${p.paymentId}</strong></td>
      <td>${p.invoiceNumber || 'INV-Auto'}</td>
      <td>ORD-${p.orderId || 'â€”'}</td>
      <td>${p.supplierName || 'Vendor'}</td>
      <td style="font-weight:700;color:var(--success)">${formatINR(p.amount)}</td>
      <td>${p.paymentMethod || 'BANK_TRANSFER'}</td>
      <td><code>${p.transactionReference}</code></td>
      <td><span class="status-badge status-DELIVERED">${p.status}</span></td>
      <td>${formatDate(p.paymentDate)}</td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Payment #</th>
          <th>Invoice #</th>
          <th>Order #</th>
          <th>Supplier</th>
          <th>Amount</th>
          <th>Method</th>
          <th>Reference</th>
          <th>Status</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function renderAdminUsersTable() {
  const el = document.getElementById('admin-users-table');
  if (!el) return;
  if (!adminUsers.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No users found</div>';
    return;
  }
  const rows = adminUsers.map(u => `
    <tr>
      <td><strong>#${u.userId}</strong></td>
      <td><strong>${u.fullName}</strong></td>
      <td>${u.email}</td>
      <td><span class="role-pill" style="font-size:11px;font-weight:700;padding:2px 8px;border-radius:12px;background:rgba(99,102,241,0.2);color:var(--accent-light)">${u.role}</span></td>
      <td>${u.departmentName || 'â€”'}</td>
      <td>${u.phone || 'â€”'}</td>
      <td><span class="status-badge status-APPROVED">${u.enabled ? 'ACTIVE' : 'DISABLED'}</span></td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>User ID</th>
          <th>Full Name</th>
          <th>Corporate Email</th>
          <th>Role</th>
          <th>Department</th>
          <th>Phone</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// 3. SUPPLIER PORTAL JS (100% REAL DATABASE & ISOLATION)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
let supplierOrders = [];
let supplierPayments = [];
let supShipTargetOrderId = null;

async function loadSupplierData() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    // 1. Dashboard Stats (Independent try-catch)
    try {
      const sRes = await fetch(`${API}/api/supplier/dashboard-stats`, { headers });
      if (sRes.ok) {
        const sJson = await sRes.json();
        const s = sJson.data || {};
        const pendingTotal = (s.pendingOrders || 0) + (s.paidReadyOrders || s.paidReadyCount || 0);
        const setSafeText = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = (val !== undefined && val !== null) ? val : 0;
        };
        setSafeText('sup-stat-assigned', s.assignedOrders || 0);
        setSafeText('sup-stat-pending', pendingTotal);
        setSafeText('sup-stat-processing', s.ordersProcessing || 0);
        setSafeText('sup-stat-shipped', s.shippedOrders || 0);
        setSafeText('sup-stat-delivered', s.deliveredOrders || 0);
        setSafeText('sup-stat-payments', s.completedPayments || 0);
        setSafeText('sup-quick-orders-count', s.assignedOrders || 0);

        const badge = document.getElementById('sup-badge-orders');
        if (badge) {
          badge.textContent = pendingTotal;
          badge.style.display = (pendingTotal > 0) ? 'block' : 'none';
        }

        const vendorEl = document.getElementById('sup-vendor-name');
        if (vendorEl && s.supplierName) vendorEl.textContent = s.supplierName;
      }
    } catch(errStats) {
      console.warn('Failed to load supplier dashboard stats:', errStats);
    }

    // 2. Orders fetch & render (Independent try-catch)
    try {
      const oRes = await fetch(`${API}/api/supplier/orders`, { headers });
      if (oRes.ok) {
        const oJson = await oRes.json();
        supplierOrders = Array.isArray(oJson) ? oJson : (oJson.data || []);
        renderSupplierOrdersTables();
      }
    } catch(errOrders) {
      console.error('Failed to load supplier orders:', errOrders);
    }

    // 3. Supplier Payments (Independent try-catch)
    try {
      const pRes = await fetch(`${API}/api/supplier/payments`, { headers });
      if (pRes.ok) {
        const pJson = await pRes.json();
        supplierPayments = pJson.data || [];
        renderSupplierPaymentsTable();
      }
    } catch(errPayments) {
      console.warn('Failed to load supplier payments:', errPayments);
    }

    // 4. Supplier Feedback (Independent try-catch)
    try {
      const feedbackRes = await fetch(`${API}/api/supplier/feedback`, { headers });
      if (feedbackRes.ok) {
        supplierFeedback = await feedbackRes.json();
        renderSupplierFeedbackTable();
      }
    } catch(errFeedback) {
      console.warn('Failed to load supplier feedback:', errFeedback);
    }

    // 5. Supplier Products
    try {
      loadSupplierProducts();
    } catch(errProd) {
      console.warn('Failed to load supplier products:', errProd);
    }
  } catch(e) {
    console.error('Supplier data load overall error:', e);
  }
}

function renderSupplierFeedbackTable() {
  try {
    const el = document.getElementById('sup-feedback-table');
    if (!el) return;
    if (!supplierFeedback || !supplierFeedback.length) {
      el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)"><i data-lucide="message-square" style="width:40px;height:40px;opacity:0.3;margin-bottom:12px"></i><div>No customer feedback received yet</div></div>';
      if (window.lucide) { try { lucide.createIcons(); } catch(e){} }
      return;
    }

    const totalRatings = supplierFeedback.length;
    const avgQuality = (supplierFeedback.reduce((sum, f) => sum + (Number(f.qualityRating) || 0), 0) / totalRatings).toFixed(1);
    const avgSpeed = (supplierFeedback.reduce((sum, f) => sum + (Number(f.speedRating) || 0), 0) / totalRatings).toFixed(1);
    const overallAvg = (((parseFloat(avgQuality) + parseFloat(avgSpeed)) / 2)).toFixed(1);

    const summaryCards = `
      <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:14px;margin-bottom:20px">
        <div style="background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:12px;padding:16px;display:flex;align-items:center;gap:14px">
          <div style="width:42px;height:42px;border-radius:10px;background:rgba(245,158,11,0.15);color:var(--warning);display:flex;align-items:center;justify-content:center;font-size:20px">⭐</div>
          <div>
            <div style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Average Rating</div>
            <div style="font-size:20px;font-weight:800;color:#fff">${overallAvg} <span style="font-size:12px;color:var(--text-muted);font-weight:500">/ 5.0</span></div>
          </div>
        </div>
        <div style="background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:12px;padding:16px;display:flex;align-items:center;gap:14px">
          <div style="width:42px;height:42px;border-radius:10px;background:rgba(99,102,241,0.15);color:var(--accent-light);display:flex;align-items:center;justify-content:center;font-size:20px">📊</div>
          <div>
            <div style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Total Ratings</div>
            <div style="font-size:20px;font-weight:800;color:#fff">${totalRatings} <span style="font-size:12px;color:var(--text-muted);font-weight:500">${totalRatings === 1 ? 'Review' : 'Reviews'}</span></div>
          </div>
        </div>
        <div style="background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:12px;padding:16px;display:flex;align-items:center;gap:14px">
          <div style="width:42px;height:42px;border-radius:10px;background:rgba(16,185,129,0.15);color:var(--success);display:flex;align-items:center;justify-content:center;font-size:20px">✨</div>
          <div>
            <div style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Quality Score</div>
            <div style="font-size:20px;font-weight:800;color:var(--success)">${avgQuality} <span style="font-size:12px;color:var(--text-muted);font-weight:500">/ 5.0</span></div>
          </div>
        </div>
        <div style="background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:12px;padding:16px;display:flex;align-items:center;gap:14px">
          <div style="width:42px;height:42px;border-radius:10px;background:rgba(6,182,212,0.15);color:var(--info);display:flex;align-items:center;justify-content:center;font-size:20px">🚀</div>
          <div>
            <div style="font-size:11px;color:var(--text-muted);font-weight:600;text-transform:uppercase">Delivery Speed</div>
            <div style="font-size:20px;font-weight:800;color:var(--info)">${avgSpeed} <span style="font-size:12px;color:var(--text-muted);font-weight:500">/ 5.0</span></div>
          </div>
        </div>
      </div>
    `;

    const rows = supplierFeedback.map(f => {
      const q = Number(f.qualityRating) || 5;
      const s = Number(f.speedRating) || 5;
      const avg = ((q + s) / 2).toFixed(1);
      const stars = '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg));
      return `
      <tr>
        <td><strong>ORD-${f.orderId}</strong></td>
        <td>${f.userName || 'Customer'}</td>
        <td>${f.productName || 'Product'}</td>
        <td>
          <div style="display:flex;flex-direction:column;gap:2px">
            <span style="color:var(--warning);font-weight:700;font-size:13px">${stars} (${avg}/5)</span>
            <span style="font-size:11px;color:var(--text-muted)">Quality: ${q}★ · Speed: ${s}★</span>
          </div>
        </td>
        <td style="max-width:320px;white-space:normal;line-height:1.4">
          <span style="color:var(--text-primary);font-style:italic">"${f.comments || '—'}"</span>
        </td>
        <td>${formatDate(f.createdDate)}</td>
      </tr>`;
    }).join('');

    el.innerHTML = `
      ${summaryCards}
      <table class="data-table">
        <thead>
          <tr>
            <th>Order #</th>
            <th>Customer</th>
            <th>Product</th>
            <th>⭐ Rating</th>
            <th>User Feedback</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;
    if (window.lucide) { try { lucide.createIcons(); } catch(e){} }
  } catch(e) {
    console.error('Error rendering feedback table:', e);
  }
}

function renderSupplierOrdersTables() {
  try {
    const overviewEl = document.getElementById('sup-overview-orders-table');
    const fullEl = document.getElementById('sup-full-orders-table');
    const shipEl = document.getElementById('sup-shipments-table');

    const pendingAcceptList = supplierOrders.filter(o => {
      const st = (o.status || '').toUpperCase();
      return st !== 'DELIVERED' && st !== 'DECLINED' && st !== 'CANCELLED';
    });

    const overviewRows = pendingAcceptList.map(o => {
      const reqId = o.purchaseRequest ? (o.purchaseRequest.requestId || '—') : '—';
      const prodName = (o.purchaseRequest && o.purchaseRequest.product && o.purchaseRequest.product.name) ? o.purchaseRequest.product.name : 'Procured Goods';
      const qty = (o.purchaseRequest && o.purchaseRequest.quantity) ? o.purchaseRequest.quantity : 1;
      const amt = (typeof formatINR === 'function') ? formatINR(o.totalAmount || 0) : ('₹' + (o.totalAmount || 0));
      const status = (o.status || 'CREATED').toUpperCase();
      return `
      <tr>
        <td><strong>ORD-${o.orderId}</strong></td>
        <td>#${reqId}</td>
        <td>${prodName} × ${qty}</td>
        <td style="font-weight:700;color:var(--accent-light)">${amt}</td>
        <td><span class="status-badge status-${status}">${status}</span></td>
        <td>
          <div class="table-actions">
            ${buildStatusDropdown(o.orderId, status)}
          </div>
        </td>
      </tr>`;
    }).join('');

    if (overviewEl) {
      overviewEl.innerHTML = pendingAcceptList.length ? `
        <table class="data-table">
          <thead>
            <tr><th>Order #</th><th>Req #</th><th>Product</th><th>Amount</th><th>Status</th><th>Action</th></tr>
          </thead>
          <tbody>${overviewRows}</tbody>
        </table>
      ` : `<div style="text-align:center;padding:40px;color:var(--text-muted)">No active orders requiring acceptance or dispatch</div>`;
    }

    // Full table
    if (fullEl) {
      const fullRows = supplierOrders.map(o => {
        const reqId = o.purchaseRequest ? (o.purchaseRequest.requestId || '—') : '—';
        const prodName = (o.purchaseRequest && o.purchaseRequest.product && o.purchaseRequest.product.name) ? o.purchaseRequest.product.name : 'Procured Goods';
        const qty = (o.purchaseRequest && o.purchaseRequest.quantity) ? o.purchaseRequest.quantity : 1;
        const amt = (typeof formatINR === 'function') ? formatINR(o.totalAmount || 0) : ('₹' + (o.totalAmount || 0));
        const status = (o.status || 'CREATED').toUpperCase();
        const dateStr = (typeof formatDate === 'function') ? formatDate(o.createdDate) : (o.createdDate || '—');
        return `
        <tr>
          <td><strong>ORD-${o.orderId}</strong></td>
          <td>#${reqId}</td>
          <td>${prodName}</td>
          <td><strong>${qty}</strong></td>
          <td style="font-weight:700;color:var(--success)">${amt}</td>
          <td><span class="status-badge status-${status}">${status}</span></td>
          <td>${dateStr}</td>
          <td>
            <div class="table-actions">
              ${buildStatusDropdown(o.orderId, status)}
            </div>
          </td>
        </tr>`;
      }).join('');

      fullEl.innerHTML = supplierOrders.length ? `
        <table class="data-table">
          <thead>
            <tr><th>Order #</th><th>Req #</th><th>Product</th><th>Qty</th><th>Amount</th><th>Status</th><th>Assigned Date</th><th>Actions</th></tr>
          </thead>
          <tbody>${fullRows}</tbody>
        </table>
      ` : `<div style="text-align:center;padding:48px;color:var(--text-muted)">No orders currently assigned to your account</div>`;
    }

    // Shipments table
    if (shipEl) {
      const shipRows = supplierOrders.map(o => {
        const prodName = (o.purchaseRequest && o.purchaseRequest.product && o.purchaseRequest.product.name) ? o.purchaseRequest.product.name : 'Procured Goods';
        const status = (o.status || 'CREATED').toUpperCase();
        return `
        <tr>
          <td><strong>ORD-${o.orderId}</strong></td>
          <td>${prodName}</td>
          <td><span class="status-badge status-${status}">${status}</span></td>
          <td>${o.trackingNumber ? `<code>${o.trackingNumber}</code>` : '<span style="color:var(--warning)">Not Dispatched</span>'}</td>
          <td>${o.shipmentDetails || 'Standard Shipping'}</td>
          <td>
            <div class="table-actions">
              <button class="btn btn-secondary btn-sm" onclick="openSupplierShipModal(${o.orderId})">
                <i data-lucide="edit-3" style="width:12px;height:12px"></i>Update Logistics
              </button>
              ${status !== 'DELIVERED' && status !== 'COMPLETED' ? `
                <button class="btn btn-success btn-sm" onclick="markSupplierDelivered(${o.orderId})">
                  <i data-lucide="check" style="width:12px;height:12px"></i>Delivered
                </button>
              ` : '<span style="font-size:12px;color:var(--success);font-weight:700">✓ Delivered</span>'}
            </div>
          </td>
        </tr>`;
      }).join('');

      shipEl.innerHTML = supplierOrders.length ? `
        <table class="data-table">
          <thead>
            <tr><th>Order #</th><th>Product</th><th>Status</th><th>Tracking #</th><th>Logistics Details</th><th>Actions</th></tr>
          </thead>
          <tbody>${shipRows}</tbody>
        </table>
      ` : `<div style="text-align:center;padding:48px;color:var(--text-muted)">No shipment records</div>`;
    }

    if (window.lucide) {
      try { lucide.createIcons(); } catch(e){}
    }
  } catch(renderErr) {
    console.error('Error rendering supplier orders tables:', renderErr);
    const overviewEl = document.getElementById('sup-overview-orders-table');
    if (overviewEl) {
      overviewEl.innerHTML = `<div style="padding:20px;color:var(--danger);font-size:13px">Error displaying active orders. Please refresh the page.</div>`;
    }
  }
}

async function acceptSupplierOrder(id) {
  showToast(`Confirming acceptance for Order ORD-${id}â€¦`, 'info');
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/orders/${id}/accept`, {
      method: 'PUT', headers
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Acceptance failed');
    showToast(`âœ… Order ORD-${id} ACCEPTED! Production/fulfillment in progress.`, 'success');
    loadSupplierData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

async function declineSupplierOrder(id) {
  const reason = prompt('Reason for declining this procurement order:');
  if (reason === null) return;

  showToast(`Declining Order ORD-${id}â€¦`, 'info');
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/orders/${id}/reject`, {
      method: 'PUT', headers,
      body: JSON.stringify({ remarks: reason || 'Inventory unavailable' })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Decline failed');
    showToast(`Order ORD-${id} declined. Admin notified.`, 'warning');
    loadSupplierData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function openSupplierShipModal(orderId) {
  supShipTargetOrderId = orderId;
  document.getElementById('sup-ship-order-id').textContent = orderId;
  document.getElementById('sup-ship-carrier').value = 'FedEx Priority Freight';
  document.getElementById('sup-ship-tracking').value = `FDX-${Math.floor(10000000 + Math.random()*90000000)}`;
  document.getElementById('sup-ship-notes').value = 'In Transit - Estimated delivery in 2-3 business days';
  document.getElementById('supplier-ship-modal').classList.add('open');
}

function closeSupplierShipModal() {
  document.getElementById('supplier-ship-modal').classList.remove('open');
  supShipTargetOrderId = null;
}

async function submitSupplierShipment() {
  if (!supShipTargetOrderId) return;
  const carrier = document.getElementById('sup-ship-carrier').value.trim();
  const tracking = document.getElementById('sup-ship-tracking').value.trim();
  const notes = document.getElementById('sup-ship-notes').value.trim();

  if (!carrier || !tracking) {
    showToast('Carrier and tracking number are mandatory.', 'warning');
    return;
  }

  showToast(`Updating dispatch for Order ORD-${supShipTargetOrderId}â€¦`, 'info');
  try {
    const headers = {'Content-Type':'application/json'};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/orders/${supShipTargetOrderId}/shipment`, {
      method: 'PUT', headers,
      body: JSON.stringify({ carrier, trackingNumber: tracking, shipmentDetails: notes })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Shipment update failed');
    closeSupplierShipModal();
    showToast(`ðŸš€ Order ORD-${supShipTargetOrderId} DISPATCHED! Tracking: ${tracking}`, 'success');
    loadSupplierData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

async function markSupplierDelivered(orderId) {
  if (!confirm(`Confirm that Order ORD-${orderId} has arrived at destination and was handed over to customer?`)) return;

  showToast(`Marking Order ORD-${orderId} as DELIVEREDâ€¦`, 'info');
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/orders/${orderId}/deliver`, {
      method: 'PUT', headers
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Delivery mark failed');
    showToast(`âœ… Order ORD-${orderId} verified and DELIVERED! Customer notified.`, 'success');
    loadSupplierData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

/* ─── Status Dropdown Helpers ───────────────────────────────────────────── */

function getNextStatusOptions(status) {
  switch ((status || '').toUpperCase()) {
    case 'ASSIGNED':
    case 'CREATED':          return [{ label: '✅ Accept Order', action: 'accept' },
                                     { label: '❌ Decline Order', action: 'decline' }];
    case 'PAID':
    case 'ORDERED':          return [{ label: 'Accept Order', action: 'accept' }];
    case 'ACCEPTED':         return [{ label: 'Processing', action: 'PROCESSING' }];
    case 'PROCESSING':       return [{ label: 'Shipped', action: 'ship' }];
    case 'SHIPPED':          return [{ label: 'Out for Delivery', action: 'OUT_FOR_DELIVERY' }];
    case 'OUT_FOR_DELIVERY': return [{ label: 'Delivered', action: 'deliver' }];
    case 'DELIVERED':        return [];
    case 'DECLINED':         return [];
    default:                 return [];
  }
}

function buildStatusDropdown(orderId, status) {
  const opts = getNextStatusOptions(status);
  if (opts.length === 0) {
    const isDone = status === 'DELIVERED' || status === 'DECLINED';
    return isDone
      ? `<span style="font-size:12px;color:var(--success);font-weight:700">✓ ${status}</span>`
      : `<span style="font-size:12px;color:var(--text-muted)">${status}</span>`;
  }
  const items = opts.map(opt =>
    `<div class="sup-status-option" onclick="handleSupplierStatusAction(${orderId}, '${opt.action}'); closeStatusDropdowns();" role="menuitem">${opt.label}</div>`
  ).join('');
  return `
  <div class="sup-status-dropdown" id="sup-sdrop-${orderId}">
    <button class="btn btn-sm sup-status-toggle" onclick="toggleStatusDropdown(${orderId})" style="min-width:130px">
      <i data-lucide="settings-2" style="width:12px;height:12px;vertical-align:middle"></i>
      Update Status <span style="font-size:10px;margin-left:2px">▾</span>
    </button>
    <div class="sup-status-menu" id="sup-smenu-${orderId}" role="menu">${items}</div>
  </div>`;
}

function toggleStatusDropdown(orderId) {
  closeStatusDropdowns(`sup-smenu-${orderId}`);
  const menu = document.getElementById(`sup-smenu-${orderId}`);
  if (menu) menu.classList.toggle('open');
}

function closeStatusDropdowns(exceptId) {
  document.querySelectorAll('.sup-status-menu.open').forEach(m => {
    if (exceptId && m.id === exceptId) return;
    m.classList.remove('open');
  });
}

document.addEventListener('click', function(e) {
  if (!e.target.closest('.sup-status-dropdown')) {
    closeStatusDropdowns();
    closeAdminReqDropdowns();
  }
});

async function handleSupplierStatusAction(orderId, action) {
  switch (action) {
    case 'accept':          return acceptSupplierOrder(orderId);
    case 'decline':         return declineSupplierOrder(orderId);
    case 'ship':            return openSupplierShipModal(orderId);
    case 'deliver':         return markSupplierDelivered(orderId);
    default:                return updateSupplierOrderStatus(orderId, action);
  }
}

async function updateSupplierOrderStatus(orderId, newStatus) {
  showToast(`Updating Order ORD-${orderId} → ${newStatus}…`, 'info');
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/orders/${orderId}/status`, {
      method: 'PUT', headers,
      body: JSON.stringify({ status: newStatus })
    });
    const result = await res.json();
    if (!res.ok || !result.success) throw new Error(result.message || 'Status update failed');
    const labels = { PROCESSING: 'Processing', OUT_FOR_DELIVERY: 'Out for Delivery' };
    showToast(`✅ Order ORD-${orderId} moved to ${labels[newStatus] || newStatus}!`, 'success');
    loadSupplierData();
    loadRealNotifications();
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

function renderSupplierPaymentsTable() {
  const el = document.getElementById('sup-payments-table');
  if (!el) return;
  if (!supplierPayments.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No payment records found for your account</div>';
    return;
  }
  const rows = supplierPayments.map(p => `
    <tr>
      <td><strong>PAY-${p.paymentId}</strong></td>
      <td>${p.invoiceNumber || 'INV-1001'}</td>
      <td>ORD-${p.orderId || 'â€”'}</td>
      <td style="font-weight:700;color:var(--success)">${formatINR(p.amount)}</td>
      <td>${p.paymentMethod || 'BANK_TRANSFER'}</td>
      <td><code>${p.transactionReference}</code></td>
      <td><span class="status-badge status-DELIVERED">${p.status}</span></td>
      <td>${formatDate(p.paymentDate)}</td>
    </tr>
  `).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Payment #</th>
          <th>Invoice #</th>
          <th>Order #</th>
          <th>Settled Amount</th>
          <th>Method</th>
          <th>Bank Reference</th>
          <th>Status</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// 4. SUPPLIER PRODUCT CATALOG MANAGEMENT JS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
let supplierCatalogProducts = [];

async function loadSupplierProducts() {
  try {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    const res = await fetch(`${API}/api/supplier/products`, { headers });
    if (res.ok) {
      const json = await res.json();
      supplierCatalogProducts = json.data || [];
      renderSupplierProductsTable();
    }
  } catch(e) {
    console.error('Failed to load supplier products', e);
  }
}

function renderSupplierProductsTable() {
  const el = document.getElementById('sup-products-table');
  if (!el) return;
  if (!supplierCatalogProducts.length) {
    el.innerHTML = '<div style="text-align:center;padding:48px;color:var(--text-muted)">No products found in catalog. Add your first product!</div>';
    return;
  }
  const rows = supplierCatalogProducts.map(p => {
    const img = p.imageUrl || 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=100';
    return `
      <tr>
        <td><strong>PROD-${p.productId}</strong></td>
        <td>
          <div style="display:flex;align-items:center;gap:10px">
            <img src="${img}" style="width:36px;height:36px;border-radius:6px;object-fit:cover" onerror="this.src='https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=100'"/>
            <div>
              <div style="font-weight:700">${p.name}</div>
              <div style="font-size:11.5px;color:var(--text-muted)"><code>${p.sku || 'â€”'}</code> Â· ${p.categoryName || 'General'}</div>
            </div>
          </div>
        </td>
        <td style="font-weight:700;color:var(--accent-light);font-size:14px">${formatINR(p.pricePerProduct)}</td>
        <td>
          <span style="font-weight:700;color:${p.numberOfQuantities > 15 ? 'var(--success)' : 'var(--warning)'}">
            ${p.numberOfQuantities || 0} units
          </span>
        </td>
        <td><span class="status-badge status-APPROVED">${p.status || 'ACTIVE'}</span></td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="openSupplierEditProductModal(${p.productId}, '${p.name.replace(/'/g,"\\'")}', ${p.pricePerProduct}, ${p.numberOfQuantities}, '${(p.sku||'').replace(/'/g,"\\'")}', '${(p.imageUrl||'').replace(/'/g,"\\'")}', '${(p.description||'').replace(/'/g,"\\'")}')">
            <i data-lucide="edit-3" style="width:12px;height:12px"></i>Edit Price &amp; Stock
          </button>
        </td>
      </tr>
    `;
  }).join('');

  el.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Product #</th>
          <th>Item Details &amp; SKU</th>
          <th>Unit Price (â‚¹)</th>
          <th>Inventory Stock</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  lucide.createIcons();
}

function openSupplierAddProductModal() {
  document.getElementById('sup-prod-modal-title').textContent = 'Add New Product to Enterprise Catalog';
  document.getElementById('sup-prod-edit-id').value = '';
  document.getElementById('sup-prod-name').value = '';
  document.getElementById('sup-prod-sku').value = `SKU-VEN-${Math.floor(1000 + Math.random()*9000)}`;
  document.getElementById('sup-prod-price').value = '';
  document.getElementById('sup-prod-stock').value = '50';
  document.getElementById('sup-prod-image').value = 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400';
  document.getElementById('sup-prod-desc').value = '';
  document.getElementById('supplier-product-modal').classList.add('open');
}

function openSupplierEditProductModal(id, name, price, stock, sku, img, desc) {
  document.getElementById('sup-prod-modal-title').textContent = `Edit Product PROD-${id}`;
  document.getElementById('sup-prod-edit-id').value = id;
  document.getElementById('sup-prod-name').value = name;
  document.getElementById('sup-prod-sku').value = sku || '';
  document.getElementById('sup-prod-price').value = price;
  document.getElementById('sup-prod-stock').value = stock;
  document.getElementById('sup-prod-image').value = img || '';
  document.getElementById('sup-prod-desc').value = desc || '';
  document.getElementById('supplier-product-modal').classList.add('open');
}

function closeSupplierProductModal() {
  document.getElementById('supplier-product-modal').classList.remove('open');
}

async function submitSupplierProduct() {
  const editId = document.getElementById('sup-prod-edit-id').value;
  const name = document.getElementById('sup-prod-name').value.trim();
  const sku = document.getElementById('sup-prod-sku').value.trim();
  const category = document.getElementById('sup-prod-category').value;
  const price = parseFloat(document.getElementById('sup-prod-price').value);
  const stock = parseInt(document.getElementById('sup-prod-stock').value) || 0;
  const image = document.getElementById('sup-prod-image').value.trim();
  const desc = document.getElementById('sup-prod-desc').value.trim();

  if (!name || isNaN(price) || price <= 0) {
    showToast('Product name and a valid unit price in â‚¹ are required.', 'warning');
    return;
  }

  showToast('Saving catalog product changesâ€¦', 'info');
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

    const payload = {
      name, sku, categoryName: category,
      pricePerProduct: price, numberOfQuantities: stock,
      imageUrl: image, description: desc
    };

    let res;
    if (editId) {
      res = await fetch(`${API}/api/supplier/products/${editId}`, {
        method: 'PUT', headers, body: JSON.stringify(payload)
      });
    } else {
      res = await fetch(`${API}/api/supplier/products`, {
        method: 'POST', headers, body: JSON.stringify(payload)
      });
    }

    const data = await res.json();
    if (!res.ok || !data.success) throw new Error(data.message || 'Operation failed');

    closeSupplierProductModal();
    showToast(`âœ… Product "${name}" saved in catalog at ${formatINR(price)}!`, 'success');
    loadSupplierProducts();
    loadProducts(); // also refreshes User catalog
  } catch(e) {
    showToast(`Error: ${e.message}`, 'error');
  }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// TOAST
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
function showToast(msg, type='info') {
  const iconMap = {success:'check-circle',error:'x-circle',warning:'alert-triangle',info:'info'};
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<i data-lucide="${iconMap[type]||'info'}" style="width:16px;height:16px;flex-shrink:0"></i><span class="toast-msg">${msg}</span>`;
  container.appendChild(el);
  lucide.createIcons(el);
  setTimeout(() => {
    el.classList.add('toast-out');
    setTimeout(() => el.remove(), 300);
  }, 3800);
}

// CSS keyframe for spin
const style = document.createElement('style');
style.textContent = '@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}';
document.head.appendChild(style);
