/**
 * Enterprise Knowledge Assistant - Frontend Controller
 * Groq LPU + pgvector Hybrid Search RAG
 */

document.addEventListener('DOMContentLoaded', () => {
  // ==========================================
  // App State & Config
  // ==========================================
  // Load authenticated session from localStorage
  let savedUser = null;
  try {
    const raw = localStorage.getItem('rag_user');
    if (raw) savedUser = JSON.parse(raw);
  } catch (e) {
    console.error('Failed to parse saved user', e);
  }

  if (!savedUser) {
    window.location.replace('/login.html');
    return;
  }

  const state = {
    user: savedUser,
    currentConversationId: null,
    activeView: 'chat',
    documents: [],
    threads: [],
    tuning: {
      topK: 15,
      vectorWeight: 0.6,
      keywordWeight: 0.4
    },
    selectedDocId: null
  };

  // Configure marked for safe, clean rendering
  if (window.marked) {
    marked.setOptions({
      breaks: true,
      gfm: true
    });
  }

  // ==========================================
  // DOM Elements
  // ==========================================
  const elements = {
    // Navigation & Views
    navChat: document.getElementById('navChat'),
    navAdmin: document.getElementById('navAdmin'),
    viewChat: document.getElementById('viewChat'),
    viewAdmin: document.getElementById('viewAdmin'),
    sidebar: document.getElementById('sidebar'),
    btnToggleSidebar: document.getElementById('btnToggleSidebar'),

    // Chat
    chatMessages: document.getElementById('chatMessages'),
    welcomeHero: document.getElementById('welcomeHero'),
    chatForm: document.getElementById('chatForm'),
    queryInput: document.getElementById('queryInput'),
    btnSend: document.getElementById('btnSend'),
    btnNewChat: document.getElementById('btnNewChat'),
    threadList: document.getElementById('threadList'),
    activeScopeBadge: document.getElementById('activeScopeBadge'),
    documentFilterSelect: document.getElementById('documentFilterSelect'),

    // Tuning Sliders
    toggleTuning: document.getElementById('toggleTuning'),
    tuningContent: document.getElementById('tuningContent'),
    topKInput: document.getElementById('topKInput'),
    topKValue: document.getElementById('topKValue'),
    vectorWeightInput: document.getElementById('vectorWeightInput'),
    vectorWeightValue: document.getElementById('vectorWeightValue'),
    keywordWeightInput: document.getElementById('keywordWeightInput'),
    keywordWeightValue: document.getElementById('keywordWeightValue'),

    // Admin Dashboard
    metricTotalDocs: document.getElementById('metricTotalDocs'),
    metricCompletedDocs: document.getElementById('metricCompletedDocs'),
    metricTotalChunks: document.getElementById('metricTotalChunks'),
    metricTotalQueries: document.getElementById('metricTotalQueries'),
    metricTotalCitations: document.getElementById('metricTotalCitations'),
    metricLlm: document.getElementById('metricLlm'),
    metricModel: document.getElementById('metricModel'),
    chipModelName: document.getElementById('chipModelName'),
    documentsTableBody: document.getElementById('documentsTableBody'),
    btnRefreshDocs: document.getElementById('btnRefreshDocs'),

    // Upload Modal
    uploadModal: document.getElementById('uploadModal'),
    btnOpenUpload: document.getElementById('btnOpenUpload'),
    btnUploadModal2: document.getElementById('btnUploadModal2'),
    btnCloseUpload: document.getElementById('btnCloseUpload'),
    btnCancelUpload: document.getElementById('btnCancelUpload'),
    uploadForm: document.getElementById('uploadForm'),
    dropzone: document.getElementById('dropzone'),
    fileInput: document.getElementById('fileInput'),
    selectedFileInfo: document.getElementById('selectedFileInfo'),
    docTitleInput: document.getElementById('docTitleInput'),
    uploadProgressContainer: document.getElementById('uploadProgressContainer'),
    uploadProgressBar: document.getElementById('uploadProgressBar'),
    uploadProgressText: document.getElementById('uploadProgressText'),
    btnSubmitUpload: document.getElementById('btnSubmitUpload'),

    // User & Toasts
    userEmailDisplay: document.getElementById('userEmailDisplay'),
    userRoleDisplay: document.getElementById('userRoleDisplay'),
    userAvatar: document.getElementById('userAvatar'),
    scopeRoleBadge: document.getElementById('scopeRoleBadge'),
    btnLogout: document.getElementById('btnLogout'),
    toastContainer: document.getElementById('toastContainer')
  };

  // ==========================================
  // Helper: Basic Auth Headers
  // ==========================================
  function getAuthHeader() {
    if (state.user && state.user.authHeader) {
      return state.user.authHeader;
    }
    if (state.user && state.user.email && state.user.password) {
      return `Basic ${btoa(`${state.user.email}:${state.user.password}`)}`;
    }
    return '';
  }

  async function apiFetch(endpoint, options = {}) {
    const defaultHeaders = {
      'Authorization': getAuthHeader()
    };

    if (!(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
    }

    const mergedOptions = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    const res = await fetch(endpoint, mergedOptions);

    if (res.status === 401) {
      localStorage.removeItem('rag_user');
      window.location.replace('/login.html');
      return null;
    }

    const data = await res.json().catch(() => null);

    if (!res.ok) {
      const errorMsg = (data && data.message) || `HTTP ${res.status}: ${res.statusText}`;
      throw new Error(errorMsg);
    }

    return data;
  }

  // ==========================================
  // View Switching
  // ==========================================
  function switchView(target) {
    state.activeView = target;
    if (target === 'chat') {
      elements.navChat.classList.add('active');
      elements.navAdmin.classList.remove('active');
      elements.viewChat.classList.add('active');
      elements.viewAdmin.classList.remove('active');
    } else {
      elements.navChat.classList.remove('active');
      elements.navAdmin.classList.add('active');
      elements.viewChat.classList.remove('active');
      elements.viewAdmin.classList.add('active');
      loadAdminStats();
      loadDocumentsTable();
    }
  }

  elements.navChat.addEventListener('click', () => switchView('chat'));
  elements.navAdmin.addEventListener('click', () => switchView('admin'));

  if (elements.btnToggleSidebar) {
    elements.btnToggleSidebar.addEventListener('click', () => {
      elements.sidebar.classList.toggle('open');
    });
  }

  // ==========================================
  // User Profile & Role-Based UI
  // ==========================================
  function initUserInterface() {
    if (!state.user) return;

    if (elements.userEmailDisplay) {
      elements.userEmailDisplay.textContent = state.user.email;
    }

    if (elements.userRoleDisplay) {
      elements.userRoleDisplay.textContent = state.user.role;
      if (state.user.role === 'ROLE_ADMIN') {
        elements.userRoleDisplay.style.color = '#c084fc';
        elements.userRoleDisplay.style.borderColor = 'rgba(139, 92, 246, 0.4)';
      } else {
        elements.userRoleDisplay.style.color = '#38bdf8';
        elements.userRoleDisplay.style.borderColor = 'rgba(6, 182, 212, 0.4)';
      }
    }

    if (elements.userAvatar) {
      const initial = (state.user.fullName || state.user.email || 'U').charAt(0).toUpperCase();
      elements.userAvatar.textContent = initial;
    }

    if (elements.scopeRoleBadge) {
      if (state.user.role === 'ROLE_ADMIN') {
        elements.scopeRoleBadge.className = 'scope-badge-admin';
        elements.scopeRoleBadge.textContent = 'ADMIN (ALL DOCS)';
      } else {
        elements.scopeRoleBadge.className = 'scope-badge-user';
        elements.scopeRoleBadge.textContent = 'USER (MY DOCS ONLY)';
      }
    }

    // Role-based visibility for Admin View tab
    if (state.user.role !== 'ROLE_ADMIN') {
      if (elements.navAdmin) {
        elements.navAdmin.style.display = 'none';
      }
    } else {
      if (elements.navAdmin) {
        elements.navAdmin.style.display = 'flex';
      }
    }
  }

  // Logout / Switch User
  if (elements.btnLogout) {
    elements.btnLogout.addEventListener('click', () => {
      if (confirm('Are you sure you want to sign out?')) {
        localStorage.removeItem('rag_user');
        window.location.replace('/login.html');
      }
    });
  }

  // ==========================================
  // Hybrid Tuning Controls
  // ==========================================
  elements.toggleTuning.addEventListener('click', () => {
    elements.toggleTuning.classList.toggle('open');
    elements.tuningContent.classList.toggle('show');
  });

  elements.topKInput.addEventListener('input', (e) => {
    state.tuning.topK = parseInt(e.target.value, 10);
    elements.topKValue.textContent = state.tuning.topK;
  });

  elements.vectorWeightInput.addEventListener('input', (e) => {
    state.tuning.vectorWeight = parseFloat(e.target.value);
    elements.vectorWeightValue.textContent = state.tuning.vectorWeight.toFixed(2);
  });

  elements.keywordWeightInput.addEventListener('input', (e) => {
    state.tuning.keywordWeight = parseFloat(e.target.value);
    elements.keywordWeightValue.textContent = state.tuning.keywordWeight.toFixed(2);
  });

  elements.documentFilterSelect.addEventListener('change', (e) => {
    state.selectedDocId = e.target.value || null;
    const selectedText = e.target.options[e.target.selectedIndex].text;
    const isAdmin = state.user && state.user.role === 'ROLE_ADMIN';
    if (state.selectedDocId) {
      elements.activeScopeBadge.textContent = `Scope: ${selectedText}`;
    } else {
      elements.activeScopeBadge.textContent = isAdmin 
        ? 'Scope: All Documents (System-wide)' 
        : 'Scope: All My Ingested Documents';
    }
  });

  // ==========================================
  // Documents & Scope Management
  // ==========================================
  async function loadDocuments() {
    try {
      const res = await apiFetch('/api/documents');
      if (res && res.data) {
        state.documents = res.data;
        populateScopeDropdown();
      }
    } catch (err) {
      console.warn('Failed to load documents list:', err);
    }
  }

  function populateScopeDropdown() {
    const select = elements.documentFilterSelect;
    const currentVal = select.value;
    const isAdmin = state.user && state.user.role === 'ROLE_ADMIN';

    select.innerHTML = '';

    if (isAdmin) {
      const defaultOpt = document.createElement('option');
      defaultOpt.value = '';
      defaultOpt.textContent = `All Ingested Documents (${state.documents.length} available)`;
      select.appendChild(defaultOpt);
    } else {
      const defaultOpt = document.createElement('option');
      defaultOpt.value = '';
      if (state.documents.length === 0) {
        defaultOpt.textContent = 'No documents ingested yet (Upload below)';
      } else {
        defaultOpt.textContent = `All My Ingested Documents (${state.documents.length} indexed)`;
      }
      select.appendChild(defaultOpt);
    }

    state.documents.forEach(doc => {
      const opt = document.createElement('option');
      opt.value = doc.id;
      const ownerLabel = (isAdmin && doc.uploadedBy) ? ` [by ${doc.uploadedBy}]` : '';
      opt.textContent = `${doc.title || doc.filename}${ownerLabel} (${doc.chunkCount || 0} chunks)`;
      select.appendChild(opt);
    });

    select.value = currentVal;

    // Update active badge text on initial load
    if (!state.selectedDocId) {
      elements.activeScopeBadge.textContent = isAdmin 
        ? 'Scope: All Documents (System-wide)' 
        : (state.documents.length === 0 ? 'Scope: No Documents Yet' : 'Scope: All My Documents');
    }
  }

  // ==========================================
  // Chat Logic & Streaming UX
  // ==========================================
  elements.btnNewChat.addEventListener('click', () => {
    startNewChat();
  });

  function startNewChat() {
    state.currentConversationId = null;
    elements.chatMessages.innerHTML = '';
    elements.chatMessages.appendChild(elements.welcomeHero);
    elements.welcomeHero.style.display = 'flex';
    elements.queryInput.value = '';
    elements.queryInput.focus();
    updateActiveThreadUI(null);
  }

  // Quick Prompt Chips
  document.querySelectorAll('.prompt-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const prompt = chip.dataset.prompt;
      if (prompt) {
        elements.queryInput.value = prompt;
        submitQuery(prompt);
      }
    });
  });

  // Textarea auto-resize & keyboard submission
  elements.queryInput.addEventListener('input', () => {
    elements.queryInput.style.height = 'auto';
    elements.queryInput.style.height = `${Math.min(elements.queryInput.scrollHeight, 180)}px`;
  });

  elements.queryInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      elements.chatForm.dispatchEvent(new Event('submit'));
    }
  });

  elements.chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const query = elements.queryInput.value.trim();
    if (!query) return;
    submitQuery(query);
  });

  async function submitQuery(queryText) {
    // Hide welcome hero on first message
    if (elements.welcomeHero.style.display !== 'none') {
      elements.welcomeHero.style.display = 'none';
    }

    // Append User Bubble
    appendUserMessage(queryText);

    // Reset input
    elements.queryInput.value = '';
    elements.queryInput.style.height = 'auto';
    elements.btnSend.disabled = true;

    // Append Typing Indicator
    const typingId = appendTypingIndicator();
    scrollChatToBottom();

    const startTime = Date.now();

    try {
      const payload = {
        query: queryText,
        conversationId: state.currentConversationId,
        documentIdFilter: state.selectedDocId,
        topK: state.tuning.topK,
        vectorWeight: state.tuning.vectorWeight,
        keywordWeight: state.tuning.keywordWeight
      };

      const res = await apiFetch('/api/chat/query', {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      removeTypingIndicator(typingId);

      if (res && res.data) {
        const chatData = res.data;
        state.currentConversationId = chatData.conversationId;
        const latency = chatData.latencyMs || (Date.now() - startTime);

        appendAiMessage(chatData.answer, chatData.citations || [], latency);
        addThreadHistoryItem(queryText, chatData.conversationId);
      } else {
        throw new Error('Received empty response from RAG service');
      }
    } catch (err) {
      removeTypingIndicator(typingId);
      appendErrorMessage(err.message || 'Failed to generate answer from Groq');
      showToast('Query error: ' + err.message, 'error');
    } finally {
      elements.btnSend.disabled = false;
      scrollChatToBottom();
    }
  }

  function appendUserMessage(text) {
    const row = document.createElement('div');
    row.className = 'message-row user-row';

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = 'U';

    const content = document.createElement('div');
    content.className = 'message-content';

    const bubble = document.createElement('div');
    bubble.className = 'user-bubble';
    bubble.textContent = text;

    content.appendChild(bubble);
    row.appendChild(content);
    row.appendChild(avatar);

    elements.chatMessages.appendChild(row);
  }

  function appendAiMessage(answerMarkdown, citations, latencyMs) {
    const row = document.createElement('div');
    row.className = 'message-row ai-row';

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
      <polyline points="2 17 12 22 22 17"></polyline>
      <polyline points="2 12 12 17 22 12"></polyline>
    </svg>`;

    const content = document.createElement('div');
    content.className = 'message-content';

    const bubble = document.createElement('div');
    bubble.className = 'ai-bubble';

    // Header with Groq badge, latency, and copy button
    const header = document.createElement('div');
    header.className = 'ai-bubble-header';
    header.innerHTML = `
      <div class="ai-header-left">
        <span class="groq-badge">Groq LPU</span>
        <span class="ai-latency">${(latencyMs / 1000).toFixed(2)}s</span>
      </div>
      <button class="btn-copy" title="Copy answer">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
        </svg>
        <span>Copy</span>
      </button>
    `;

    const copyBtn = header.querySelector('.btn-copy');
    copyBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(answerMarkdown);
      copyBtn.querySelector('span').textContent = 'Copied!';
      setTimeout(() => copyBtn.querySelector('span').textContent = 'Copy', 2000);
    });

    // Render Markdown text
    const textDiv = document.createElement('div');
    textDiv.className = 'ai-text';
    textDiv.innerHTML = window.marked ? marked.parse(answerMarkdown) : answerMarkdown;

    bubble.appendChild(header);
    bubble.appendChild(textDiv);

    // Citations Accordion / Badges
    if (citations && citations.length > 0) {
      const citationsBox = document.createElement('div');
      citationsBox.className = 'citations-box';

      const citationsTitle = document.createElement('div');
      citationsTitle.className = 'citations-header';
      citationsTitle.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
        </svg>
        <span>VERIFIED CITATIONS (${citations.length})</span>
      `;

      const cardsContainer = document.createElement('div');
      cardsContainer.className = 'citation-cards';

      citations.forEach(c => {
        const pill = document.createElement('div');
        pill.className = 'citation-pill';

        const scorePercent = Math.round((c.relevanceScore || 0) * 100);

        pill.innerHTML = `
          <div class="citation-pill-header">
            <div class="citation-meta">
              <span class="citation-tag">${escapeHtml(c.documentTitle || 'Document')}</span>
              <span>Page ${c.pageNumber || 1} • Chunk ${c.chunkIndex || 0}</span>
            </div>
            <span class="citation-score">${scorePercent}% relevance</span>
          </div>
          <div class="citation-snippet">"${escapeHtml(c.snippet || '')}"</div>
        `;

        // Click to expand/collapse snippet
        pill.addEventListener('click', () => {
          pill.classList.toggle('expanded');
        });

        cardsContainer.appendChild(pill);
      });

      citationsBox.appendChild(citationsTitle);
      citationsBox.appendChild(cardsContainer);
      bubble.appendChild(citationsBox);
    }

    content.appendChild(bubble);
    row.appendChild(avatar);
    row.appendChild(content);

    elements.chatMessages.appendChild(row);
  }

  function appendTypingIndicator() {
    const id = 'typing-' + Date.now();
    const row = document.createElement('div');
    row.className = 'message-row ai-row';
    row.id = id;

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
      <polyline points="2 17 12 22 22 17"></polyline>
      <polyline points="2 12 12 17 22 12"></polyline>
    </svg>`;

    const content = document.createElement('div');
    content.className = 'message-content';

    const bubble = document.createElement('div');
    bubble.className = 'ai-bubble typing-bubble';
    bubble.innerHTML = `
      <span class="typing-dot"></span>
      <span class="typing-dot"></span>
      <span class="typing-dot"></span>
      <span style="font-size:0.8rem; color:var(--text-muted); margin-left: 8px;">Groq LPU reasoning over documents...</span>
    `;

    content.appendChild(bubble);
    row.appendChild(avatar);
    row.appendChild(content);

    elements.chatMessages.appendChild(row);
    return id;
  }

  function removeTypingIndicator(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
  }

  function appendErrorMessage(errorText) {
    const row = document.createElement('div');
    row.className = 'message-row ai-row';

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.style.background = 'var(--danger)';
    avatar.innerHTML = '!';

    const content = document.createElement('div');
    content.className = 'message-content';

    const bubble = document.createElement('div');
    bubble.className = 'ai-bubble';
    bubble.style.borderColor = 'rgba(239, 68, 68, 0.4)';
    bubble.style.background = 'rgba(239, 68, 68, 0.05)';
    bubble.innerHTML = `
      <strong style="color:var(--danger)">Service Error:</strong>
      <p style="margin-top:6px; color:#fca5a5;">${escapeHtml(errorText)}</p>
    `;

    content.appendChild(bubble);
    row.appendChild(avatar);
    row.appendChild(content);
    elements.chatMessages.appendChild(row);
  }

  function scrollChatToBottom() {
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
  }

  // Thread History list
  function addThreadHistoryItem(queryText, convId) {
    if (!convId) return;
    if (state.threads.some(t => t.conversationId === convId)) return;

    state.threads.unshift({ query: queryText, conversationId: convId });
    renderThreadList();
  }

  function renderThreadList() {
    const list = elements.threadList;
    list.innerHTML = '';

    if (state.threads.length === 0) {
      list.innerHTML = '<div class="empty-threads">No previous queries</div>';
      return;
    }

    state.threads.slice(0, 15).forEach(t => {
      const item = document.createElement('div');
      item.className = 'thread-item' + (t.conversationId === state.currentConversationId ? ' active' : '');
      item.textContent = t.query;
      item.title = t.query;

      item.addEventListener('click', () => {
        loadConversationThread(t.conversationId);
      });

      list.appendChild(item);
    });
  }

  function updateActiveThreadUI(convId) {
    document.querySelectorAll('.thread-item').forEach(el => {
      el.classList.toggle('active', el.textContent === convId);
    });
  }

  async function loadConversationThread(convId) {
    try {
      const res = await apiFetch(`/api/chat/history/${convId}`);
      if (res && res.data && res.data.length > 0) {
        state.currentConversationId = convId;
        elements.welcomeHero.style.display = 'none';
        elements.chatMessages.innerHTML = '';

        res.data.forEach(item => {
          appendUserMessage(item.question);
          appendAiMessage(item.answer, item.citations || [], 0);
        });

        renderThreadList();
        scrollChatToBottom();
      }
    } catch (err) {
      showToast('Failed to load conversation history: ' + err.message, 'error');
    }
  }

  async function loadPastHistory() {
    try {
      const res = await apiFetch('/api/chat/history');
      if (res && res.data && res.data.length > 0) {
        // Group by conversation ID
        const seen = new Set();
        state.threads = [];
        res.data.forEach(item => {
          if (item.conversationId && !seen.has(item.conversationId)) {
            seen.add(item.conversationId);
            state.threads.push({
              query: item.question,
              conversationId: item.conversationId
            });
          }
        });
        renderThreadList();
      }
    } catch (err) {
      console.warn('Could not load user chat history:', err);
    }
  }

  // ==========================================
  // Admin Dashboard & Document Operations
  // ==========================================
  async function loadAdminStats() {
    try {
      const res = await apiFetch('/api/admin/stats');
      if (res && res.data) {
        const stats = res.data;
        elements.metricTotalDocs.textContent = stats.totalDocuments || 0;
        elements.metricCompletedDocs.textContent = `${stats.completedDocuments || 0} indexed`;
        elements.metricTotalChunks.textContent = stats.totalChunks || 0;
        elements.metricTotalQueries.textContent = stats.totalQueries || 0;
        elements.metricTotalCitations.textContent = `${stats.totalCitations || 0} citations`;
        elements.metricLlm.textContent = stats.llmProvider ? stats.llmProvider.toUpperCase() : 'GROQ';
      }
    } catch (err) {
      console.warn('Failed to load admin stats:', err);
    }
  }

  async function loadDocumentsTable() {
    elements.documentsTableBody.innerHTML = '<tr><td colspan="8" class="table-loading">Loading indexed documents...</td></tr>';
    try {
      const res = await apiFetch('/api/documents');
      if (res && res.data) {
        state.documents = res.data;
        populateScopeDropdown();
        renderDocumentsTable(res.data);
      }
    } catch (err) {
      elements.documentsTableBody.innerHTML = `<tr><td colspan="8" class="table-loading" style="color:var(--danger)">Error: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  function renderDocumentsTable(docs) {
    const tbody = elements.documentsTableBody;
    tbody.innerHTML = '';

    if (!docs || docs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="table-loading">No documents indexed yet. Upload one to get started!</td></tr>';
      return;
    }

    docs.forEach(doc => {
      const tr = document.createElement('tr');
      const sizeKb = doc.fileSize ? Math.round(doc.fileSize / 1024) + ' KB' : '-';
      const dateStr = doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '-';
      const statusClass = (doc.status || 'INDEXED').toLowerCase();
      const owner = doc.uploadedBy || 'System';

      tr.innerHTML = `
        <td>
          <strong style="color:var(--text-primary)">${escapeHtml(doc.title || doc.filename)}</strong>
          <div style="font-size:0.75rem; color:var(--text-muted)">${escapeHtml(doc.filename)}</div>
        </td>
        <td><span class="citation-tag">${escapeHtml((doc.fileType || 'PDF').toUpperCase())}</span></td>
        <td style="font-family:var(--font-mono)">${sizeKb}</td>
        <td style="font-family:var(--font-mono); font-weight:600; color:var(--cyan)">${doc.chunkCount || 0}</td>
        <td><span style="font-size:0.75rem; color:var(--text-secondary); font-family:var(--font-mono)">${escapeHtml(owner)}</span></td>
        <td><span class="badge-status status-${statusClass}">${escapeHtml(doc.status || 'INDEXED')}</span></td>
        <td>${dateStr}</td>
        <td>
          <button class="btn-delete-doc" data-id="${doc.id}" title="Delete document & chunks">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </td>
      `;

      tr.querySelector('.btn-delete-doc').addEventListener('click', () => {
        deleteDocument(doc.id, doc.title || doc.filename);
      });

      tbody.appendChild(tr);
    });
  }

  async function deleteDocument(docId, docTitle) {
    if (!confirm(`Are you sure you want to delete "${docTitle}" and all its vector chunks?`)) {
      return;
    }

    try {
      await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' });
      showToast(`Document "${docTitle}" deleted successfully`, 'success');
      loadDocumentsTable();
      loadDocuments();
      loadAdminStats();
    } catch (err) {
      showToast('Failed to delete document: ' + err.message, 'error');
    }
  }

  elements.btnRefreshDocs.addEventListener('click', () => {
    loadDocumentsTable();
    loadAdminStats();
  });

  // ==========================================
  // Document Upload Modal Logic
  // ==========================================
  function openUploadModal() {
    elements.uploadModal.classList.add('open');
    elements.fileInput.value = '';
    elements.docTitleInput.value = '';
    elements.selectedFileInfo.textContent = '';
    elements.uploadProgressContainer.classList.remove('show');
    elements.uploadProgressBar.style.width = '0%';
    elements.btnSubmitUpload.disabled = false;
  }

  function closeUploadModal() {
    elements.uploadModal.classList.remove('open');
  }

  elements.btnOpenUpload.addEventListener('click', openUploadModal);
  elements.btnUploadModal2.addEventListener('click', openUploadModal);
  elements.btnCloseUpload.addEventListener('click', closeUploadModal);
  elements.btnCancelUpload.addEventListener('click', closeUploadModal);

  // Close on backdrop click
  elements.uploadModal.addEventListener('click', (e) => {
    if (e.target === elements.uploadModal) closeUploadModal();
  });

  // Dropzone drag-and-drop
  elements.dropzone.addEventListener('click', () => elements.fileInput.click());

  ['dragenter', 'dragover'].forEach(name => {
    elements.dropzone.addEventListener(name, (e) => {
      e.preventDefault();
      elements.dropzone.classList.add('dragover');
    });
  });

  ['dragleave', 'drop'].forEach(name => {
    elements.dropzone.addEventListener(name, (e) => {
      e.preventDefault();
      elements.dropzone.classList.remove('dragover');
    });
  });

  elements.dropzone.addEventListener('drop', (e) => {
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      elements.fileInput.files = e.dataTransfer.files;
      handleFileSelected();
    }
  });

  elements.fileInput.addEventListener('change', handleFileSelected);

  function handleFileSelected() {
    const file = elements.fileInput.files[0];
    if (file) {
      const sizeMb = (file.size / (1024 * 1024)).toFixed(2);
      elements.selectedFileInfo.textContent = `Selected: ${file.name} (${sizeMb} MB)`;
      if (!elements.docTitleInput.value) {
        // Pre-fill clean title
        const baseName = file.name.replace(/\.[^/.]+$/, "");
        elements.docTitleInput.value = baseName;
      }
    }
  }

  elements.uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const file = elements.fileInput.files[0];
    if (!file) {
      showToast('Please select a file to upload', 'error');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    if (elements.docTitleInput.value.trim()) {
      formData.append('title', elements.docTitleInput.value.trim());
    }

    elements.uploadProgressContainer.classList.add('show');
    elements.btnSubmitUpload.disabled = true;
    elements.uploadProgressBar.style.width = '35%';
    elements.uploadProgressText.textContent = 'Uploading and extracting text...';

    try {
      setTimeout(() => {
        elements.uploadProgressBar.style.width = '70%';
        elements.uploadProgressText.textContent = 'Generating 1536d embeddings and indexing into pgvector...';
      }, 700);

      const res = await apiFetch('/api/documents/upload', {
        method: 'POST',
        body: formData
      });

      elements.uploadProgressBar.style.width = '100%';
      elements.uploadProgressText.textContent = 'Successfully indexed!';

      setTimeout(() => {
        closeUploadModal();
        showToast(`Document "${res.data.title || file.name}" processed with ${res.data.chunkCount} chunks!`, 'success');
        loadDocuments();
        if (state.activeView === 'admin') {
          loadDocumentsTable();
          loadAdminStats();
        }
      }, 500);

    } catch (err) {
      elements.uploadProgressContainer.classList.remove('show');
      elements.btnSubmitUpload.disabled = false;
      showToast('Upload failed: ' + err.message, 'error');
    }
  });

  // ==========================================
  // Toast Notifications
  // ==========================================
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.2s ease';
      setTimeout(() => toast.remove(), 200);
    }, 4000);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // ==========================================
  // Initialize App
  // ==========================================
  initUserInterface();
  loadDocuments();
  loadPastHistory();
});
