import { productApi, memberApi, transactionApi } from "./api.js";
import {
  KATEGORI_LIST,
  JENIS_MAP,
  CATEGORY_ICONS,
  formatRp,
  getFinalPrice,
  getStatusStok,
  statusColor,
  memberDiscountRate,
  plusProgress,
  sisaMenujuPlus,
} from "./constants.js";

// ══════════════════════════════════════════════════════════════════
// STATE
// ══════════════════════════════════════════════════════════════════
const state = {
  activeTab: "dashboard",
  products: [],
  members: [],
  transactions: [],
  filterKat: "Semua",
  loading: true,
  serverError: false,
  trxCart: [],
  trxMember: null,
  trxUseMember: false,
  trxMemberPhone: "",
};

// ══════════════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════════════
function badgeHTML(label, color) {
  return `<span class="badge badge-${color}">${label}</span>`;
}

function escHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// ══════════════════════════════════════════════════════════════════
// TOAST — Enhanced dengan icon, subtitle, progress bar & stack
// ══════════════════════════════════════════════════════════════════
const TOAST_META = {
  success: { icon: "✓", label: "Berhasil"   },
  error:   { icon: "✕", label: "Gagal"      },
  warning: { icon: "⚠", label: "Perhatian"  },
  info:    { icon: "ℹ", label: "Info"       },
};

function showToast(msg, type = "success", subtitle = "") {
  const wrap = document.getElementById("toast-container");
  if (!wrap) return;

  const id   = "toast-" + Date.now() + Math.random().toString(36).slice(2);
  const meta = TOAST_META[type] ?? TOAST_META.success;

  const item = document.createElement("div");
  item.id        = id;
  item.className = `toast-item toast-${type}`;
  item.setAttribute("role", "alert");
  item.innerHTML = `
    <div class="toast-icon-circle" aria-hidden="true">${meta.icon}</div>
    <div class="toast-body">
      <p class="toast-title">${escHtml(msg)}</p>
      ${subtitle ? `<p class="toast-sub">${escHtml(subtitle)}</p>` : ""}
    </div>
    <button class="toast-x" aria-label="Tutup notifikasi" data-toast-id="${id}">×</button>
    <div class="toast-bar" aria-hidden="true"></div>`;

  wrap.appendChild(item);

  // Tombol close
  item.querySelector(".toast-x").addEventListener("click", () => removeToast(id));

  // Auto dismiss
  const timer = setTimeout(() => removeToast(id), 3600);
  item._toastTimer = timer;
}

function removeToast(id) {
  const el = document.getElementById(id);
  if (!el) return;
  clearTimeout(el._toastTimer);
  el.classList.add("toast-hiding");
  setTimeout(() => el.remove(), 320);
}

// ══════════════════════════════════════════════════════════════════
// MODAL
// ══════════════════════════════════════════════════════════════════
function showModal(titleHTML, bodyHTML) {
  const el = document.getElementById("modal-container");
  el.innerHTML = `
    <div class="modal-overlay" id="modal-overlay">
      <div class="modal" onclick="event.stopPropagation()">
        <div class="modal-header">
          <span class="modal-title">${titleHTML}</span>
          <button class="modal-close" id="modal-close-btn">×</button>
        </div>
        <div class="modal-body">${bodyHTML}</div>
      </div>
    </div>`;
  document.getElementById("modal-overlay").addEventListener("click", closeModal);
  document.getElementById("modal-close-btn").addEventListener("click", closeModal);
}

function closeModal() {
  document.getElementById("modal-container").innerHTML = "";
  state.trxCart = [];
  state.trxMember = null;
  state.trxUseMember = false;
  state.trxMemberPhone = "";
}

// ══════════════════════════════════════════════════════════════════
// DATA LOADING
// ══════════════════════════════════════════════════════════════════
async function refreshAll() {
  state.loading = true;
  state.serverError = false;
  renderApp();
  try {
    const [p, m, t] = await Promise.all([
      productApi.getAll(), memberApi.getAll(), transactionApi.getAll(),
    ]);
    state.products     = Array.isArray(p) ? p : [];
    state.members      = Array.isArray(m) ? m : [];
    state.transactions = Array.isArray(t) ? t : [];
  } catch {
    state.serverError = true;
  } finally {
    state.loading = false;
    renderApp();
  }
}

// ══════════════════════════════════════════════════════════════════
// RENDER — LOADING / ERROR
// ══════════════════════════════════════════════════════════════════
function renderLoading() {
  document.body.innerHTML = `<div class="loading-screen">Menghubungkan ke server Java...</div>`;
}

function renderServerError() {
  document.body.innerHTML = `
    <div class="server-error">
      <span class="server-error-icon">⚠️</span>
      <p class="server-error-title">Tidak dapat terhubung ke server Java</p>
      <p class="server-error-sub">Pastikan <strong>Main.java</strong> sudah dijalankan di IntelliJ (port 8080)</p>
      <button class="btn btn-primary" style="margin-top:0.5rem" onclick="retryConnect()">Coba Lagi</button>
    </div>`;
}

// ══════════════════════════════════════════════════════════════════
// RENDER — SHELL
// ══════════════════════════════════════════════════════════════════
function renderShell() {
  const TABS = [
    { id: "dashboard",    label: "Dashboard",  icon: "◈" },
    { id: "products",     label: "Produk",     icon: "▤" },
    { id: "members",      label: "Member",     icon: "◎" },
    { id: "transactions", label: "Transaksi",  icon: "≡" },
  ];

  document.body.innerHTML = `
    <header class="header">
      <div class="header-top">
        <div class="header-brand">
          <div class="brand-logo">M</div>
          <div>
            <div class="brand-title">McLAREN COLLECTION</div>
            <div class="brand-sub">MERCHANDISE MANAGEMENT SYSTEM</div>
          </div>
        </div>

        <button class="btn btn-primary header-trx-btn" id="header-btn-trx">
          + TRANSAKSI BARU
        </button>
      </div>

      <nav class="tabs">
        ${TABS.map(t => `
          <button class="tab-btn ${state.activeTab === t.id ? "active" : ""}"
                  data-tab="${t.id}">
            <span>${t.icon}</span> ${t.label}
          </button>`).join("")}
      </nav>
    </header>

    <main class="main" id="main-content"></main>

    <div id="modal-container"></div>

    <!-- Toast container — dirender di luar semua overlay -->
    <div id="toast-container" role="region" aria-live="polite" aria-label="Notifikasi"></div>`;

  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      state.activeTab = btn.dataset.tab;
      renderApp();
    });
  });

  document.getElementById("header-btn-trx")
    .addEventListener("click", openTransactionModal);
}

// ══════════════════════════════════════════════════════════════════
// RENDER — MAIN DISPATCHER
// ══════════════════════════════════════════════════════════════════
function renderApp() {
  if (state.loading)     { renderLoading(); return; }
  if (state.serverError) { renderServerError(); return; }

  if (!document.getElementById("main-content")) renderShell();

  // Re-bind header button setelah re-render
  const headerBtn = document.getElementById("header-btn-trx");
  if (headerBtn) {
    headerBtn.replaceWith(headerBtn.cloneNode(true));
    document.getElementById("header-btn-trx")
      .addEventListener("click", openTransactionModal);
  }

  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.tab === state.activeTab);
  });

  const main = document.getElementById("main-content");
  switch (state.activeTab) {
    case "dashboard":    main.innerHTML = renderDashboard();    bindDashboard();    break;
    case "products":     main.innerHTML = renderProducts();     bindProducts();     break;
    case "members":      main.innerHTML = renderMembers();      bindMembers();      break;
    case "transactions": main.innerHTML = renderTransactions(); bindTransactions(); break;
  }
}

// ══════════════════════════════════════════════════════════════════
// TAB: DASHBOARD
// ══════════════════════════════════════════════════════════════════
function renderDashboard() {
  const totalRevenue = state.transactions.reduce((s, t) => s + (t.total ?? 0), 0);

  const stats = [
    { label: "Total Produk",  value: state.products.length,        icon: "▤", color: "var(--blue)"   },
    { label: "Total Member",  value: state.members.length,         icon: "◎", color: "var(--purple)" },
    { label: "Transaksi",     value: state.transactions.length,    icon: "≡", color: "var(--green)"  },
    { label: "Total Revenue", value: formatRp(totalRevenue),       icon: "◈", color: "var(--accent)" },
  ];

  return `
    <section>
      <h2 class="page-title" style="margin-bottom:1.5rem">OVERVIEW</h2>

      <div class="grid-stats">
        ${stats.map(s => `
          <div class="stat-card">
            <div class="stat-icon" style="color:${s.color}">${s.icon}</div>
            <p class="stat-label">${s.label}</p>
            <p class="stat-value" style="color:${s.color}">${s.value}</p>
          </div>`).join("")}
      </div>

      <h3 class="section-title">BREAKDOWN KATEGORI</h3>
      <div class="grid-cats">
        ${KATEGORI_LIST.map(k => {
          const count = state.products.filter(p => p.category === k).length;
          const stok  = state.products.filter(p => p.category === k).reduce((s, p) => s + p.stock, 0);
          return `
            <div class="cat-card">
              <span class="cat-icon">${CATEGORY_ICONS[k]}</span>
              <div>
                <p class="cat-name">${k}</p>
                <p class="cat-meta">${count} produk · ${stok} stok</p>
              </div>
            </div>`;
        }).join("")}
      </div>

      <h3 class="section-title" style="margin-top:1.75rem">MEMBER TIER</h3>
      <div class="grid-tiers">
        ${["REGULAR", "PLUS"].map(tier => `
          <div class="tier-card">
            ${badgeHTML(tier, tier === "PLUS" ? "orange" : "blue")}
            <p class="tier-count">${state.members.filter(m => m.tier === tier).length}</p>
            <p class="tier-label">member</p>
          </div>`).join("")}
      </div>
    </section>`;
}
function bindDashboard() {}

// ══════════════════════════════════════════════════════════════════
// TAB: PRODUK
// ══════════════════════════════════════════════════════════════════
function productCardHTML(p) {
  const final = getFinalPrice(p);

  return `
    <div class="product-card">
      <div class="product-card-body">
        <div class="product-top">
          <span class="product-emoji">${CATEGORY_ICONS[p.category] ?? "📦"}</span>
          <div class="product-badges" style="display:flex; gap:6px; flex-wrap:wrap; justify-content:flex-end;">
            ${p.jenis ? `<span class="badge badge-blue">${escHtml(p.jenis)}</span>` : ""}
            ${p.hasDiscount ? `<span class="badge badge-orange">-${p.discountPercent}%</span>` : ""}
          </div>
        </div>
        <p class="product-id">${String(p.id).padStart(3, "0")}</p>
        <h3 class="product-name">${escHtml(p.name)}</h3>
        <p class="product-meta">${escHtml(p.category)} · Size: ${escHtml(p.size ?? "-")}</p>
        ${p.hasDiscount
          ? `<p class="product-orig">${formatRp(p.price)}</p>`
          : `<p class="product-orig" style="visibility:hidden;">-</p>`}
        <p class="product-price">${formatRp(final)}</p>
        <p class="product-stock">Stok: <span>${p.stock}</span></p>
      </div>
      <div class="product-card-footer">
        <button type="button" class="btn btn-secondary btn-sm" data-action="edit-product" data-id="${p.id}">Edit</button>
        <button type="button" class="btn btn-danger btn-sm" data-action="delete-product" data-id="${p.id}">Hapus</button>
      </div>
    </div>
  `;
}

function renderProducts() {
  const filtered = state.filterKat === "Semua"
    ? state.products
    : state.products.filter(p => p.category === state.filterKat);

  return `
    <section>
      <div class="page-header">
        <h2 class="page-title">PRODUK</h2>
        <button class="btn btn-primary" id="btn-add-product">+ TAMBAH PRODUK</button>
      </div>

      <div class="filter-bar">
        <button class="filter-btn ${state.filterKat === "Semua" ? "active" : ""}" data-filter="Semua">SEMUA</button>
        ${KATEGORI_LIST.map(k => `
          <button class="filter-btn ${state.filterKat === k ? "active" : ""}" data-filter="${k}">${k.toUpperCase()}</button>
        `).join("")}
      </div>

      ${filtered.length === 0
        ? `<p class="empty">Belum ada produk dalam kategori ini.</p>`
        : `<div class="grid-cards">${filtered.map(productCardHTML).join("")}</div>`}
    </section>`;
}

function bindProducts() {
  document.getElementById("btn-add-product")?.addEventListener("click", () => openProductModal(null));

  document.querySelectorAll(".filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      state.filterKat = btn.dataset.filter;
      renderApp();
    });
  });

  document.querySelectorAll("[data-action='edit-product']").forEach(btn => {
    btn.addEventListener("click", () => {
      const p = state.products.find(x => String(x.id) === btn.dataset.id);
      if (p) openProductModal(p);
    });
  });

  document.querySelectorAll("[data-action='delete-product']").forEach(btn => {
    btn.addEventListener("click", () => handleDeleteProduct(btn.dataset.id));
  });
}

// ── Product Form Modal ────────────────────────────────────────────
function productFormHTML(p) {
  const isEdit = !!p;
  const cats   = KATEGORI_LIST;
  const cat    = p?.category ?? cats[0];
  const jenis  = JENIS_MAP[cat] ?? [];

  return `
    <div id="form-error" class="form-error" style="display:none"></div>

    <div class="form-group">
      <label class="form-label">Nama Produk *</label>
      <input class="form-input" id="f-name" type="text" value="${escHtml(p?.name ?? "")}" placeholder="Nama produk">
    </div>

    <div class="form-group">
      <label class="form-label">Kategori</label>
      <select class="form-select" id="f-category">
        ${cats.map(c => `<option value="${c}" ${c === cat ? "selected" : ""}>${c}</option>`).join("")}
      </select>
    </div>

    <div class="form-group">
      <label class="form-label">Jenis</label>
      <select class="form-select" id="f-jenis">
        ${jenis.map(j => `<option value="${j}" ${j === p?.jenis ? "selected" : ""}>${j}</option>`).join("")}
      </select>
    </div>

    <div class="form-group">
      <label class="form-label">Harga (Rp) *</label>
      <input class="form-input" id="f-price" type="number" value="${p?.price ?? ""}" placeholder="0" min="0">
    </div>

    <div class="form-group">
      <label class="form-label">Stok *</label>
      <input class="form-input" id="f-stock" type="number" value="${p?.stock ?? ""}" placeholder="0" min="0">
    </div>

    <div class="form-group">
      <label class="form-label">Size / Varian</label>
      <input class="form-input" id="f-size" type="text" value="${escHtml(p?.size ?? "")}" placeholder="M, L, XL, Free Size, 42, ...">
    </div>

    <div class="form-checkbox-row">
      <input type="checkbox" id="f-hasDiscount" ${p?.hasDiscount ? "checked" : ""}>
      <label for="f-hasDiscount">Ada Diskon?</label>
    </div>

    <div class="form-group" id="discount-group" style="${p?.hasDiscount ? "" : "display:none"}">
      <label class="form-label">Diskon (%)</label>
      <input class="form-input" id="f-discountPercent" type="number" value="${p?.discountPercent ?? ""}" min="0" max="100" placeholder="0">
    </div>

    <div class="form-actions">
      <button class="btn btn-secondary" id="form-cancel">Batal</button>
      <button class="btn btn-primary" id="form-submit">${isEdit ? "Simpan" : "Tambah Produk"}</button>
    </div>`;
}

function openProductModal(p) {
  showModal(p ? "Edit Produk" : "Tambah Produk Baru", productFormHTML(p));

  document.getElementById("f-category").addEventListener("change", function() {
    const jenis = JENIS_MAP[this.value] ?? [];
    const sel = document.getElementById("f-jenis");
    sel.innerHTML = jenis.map(j => `<option value="${j}">${j}</option>`).join("");
  });

  document.getElementById("f-hasDiscount").addEventListener("change", function() {
    document.getElementById("discount-group").style.display = this.checked ? "" : "none";
  });

  document.getElementById("form-cancel").addEventListener("click", closeModal);

  document.getElementById("form-submit").addEventListener("click", async () => {
    const data = {
      name:            document.getElementById("f-name").value.trim(),
      category:        document.getElementById("f-category").value,
      jenis:           document.getElementById("f-jenis").value,
      price:           Number(document.getElementById("f-price").value),
      stock:           Number(document.getElementById("f-stock").value),
      size:            document.getElementById("f-size").value.trim(),
      hasDiscount:     document.getElementById("f-hasDiscount").checked,
      discountPercent: Number(document.getElementById("f-discountPercent").value) || 0,
    };

    const errEl = document.getElementById("form-error");

    // ── Validasi dengan pesan spesifik ──────────────────────────
    if (!data.name) {
      errEl.textContent = "⚠ Nama produk wajib diisi.";
      errEl.style.display = "";
      showToast("Nama produk wajib diisi.", "warning", "Field bertanda * tidak boleh kosong.");
      return;
    }
    if (!data.price || data.price <= 0) {
      errEl.textContent = "⚠ Harga harus lebih dari 0.";
      errEl.style.display = "";
      showToast("Harga tidak valid.", "warning", "Masukkan harga produk lebih dari Rp 0.");
      return;
    }
    if (data.stock < 0) {
      errEl.textContent = "⚠ Stok tidak valid.";
      errEl.style.display = "";
      showToast("Stok tidak valid.", "warning", "Stok tidak boleh bernilai negatif.");
      return;
    }
    if (data.hasDiscount && (data.discountPercent <= 0 || data.discountPercent > 100)) {
      errEl.textContent = "⚠ Diskon harus antara 1–100%.";
      errEl.style.display = "";
      showToast("Diskon tidak valid.", "warning", "Masukkan nilai diskon antara 1 sampai 100.");
      return;
    }

    errEl.style.display = "none";

    const btn = document.getElementById("form-submit");
    btn.classList.add("btn-loading");
    try {
      if (p) {
        await productApi.update(p.id, data);
        closeModal();
        await refreshAll();
        showToast("Produk berhasil diupdate!", "success", `"${data.name}" sudah disimpan.`);
      } else {
        await productApi.create(data);
        closeModal();
        await refreshAll();
        showToast("Produk berhasil ditambahkan!", "success", `"${data.name}" kini tersedia di katalog.`);
      }
    } catch (e) {
      errEl.textContent = e.message;
      errEl.style.display = "";
      showToast("Gagal menyimpan produk.", "error", e.message);
    } finally {
      btn.classList.remove("btn-loading");
    }
  });
}

async function handleDeleteProduct(id) {
  const prod = state.products.find(x => String(x.id) === String(id));
  if (!confirm(`Hapus produk "${prod?.name ?? id}"?`)) return;
  try {
    await productApi.delete(id);
    await refreshAll();
    showToast("Produk dihapus.", "error", `"${prod?.name ?? id}" telah dihapus dari sistem.`);
  } catch (e) {
    showToast("Gagal menghapus produk.", "error", e.message);
  }
}

// ══════════════════════════════════════════════════════════════════
// TAB: MEMBER
// ══════════════════════════════════════════════════════════════════
function memberCardHTML(m) {
  const memberTrx    = state.transactions.filter(t => t.member && String(t.member.id) === String(m.id));
  const totalTrx     = memberTrx.length;
  const totalBelanja = memberTrx.reduce((s, t) => s + (t.total ?? 0), 0);

  const effectiveTier = totalBelanja >= 5_000_000 ? "PLUS" : m.tier;

  const prog = plusProgress(totalBelanja);
  const sisa = sisaMenujuPlus(totalBelanja);

  return `
    <div class="member-card">
      <div class="member-card-top">
        <div>
          <p class="member-id">${String(m.id).padStart(4, '0')}</p>
          <h3 class="member-name">${escHtml(m.name).toUpperCase()}</h3>
        </div>
        ${badgeHTML(effectiveTier, effectiveTier === "PLUS" ? "orange" : "blue")}
      </div>

      <div class="member-info">
        <p>📞 ${escHtml(m.phone)}</p>
        <p>✉️ ${escHtml(m.email ?? "-")}</p>
        <p>🛒 ${totalTrx} transaksi</p>
        <p>💰 Total belanja: <span class="val">${formatRp(totalBelanja)}</span></p>
      </div>

      ${effectiveTier !== "PLUS" ? `
      <div>
        <p class="progress-label">Menuju PLUS: <span style="font-family:var(--font-mono);">${formatRp(sisa)}</span> lagi</p>
        <div class="progress-bar">
          <div class="progress-fill" style="width:${prog}%"></div>
        </div>
      </div>` : `
      <div style="margin:0.5rem 0;">
        <p class="progress-label" style="color:var(--accent);"> Member PLUS aktif · Diskon 15%</p>
        <div class="progress-bar">
          <div class="progress-fill" style="width:100%; background:var(--accent);"></div>
        </div>
      </div>`}

      <div class="member-actions">
        <button type="button" class="btn btn-secondary btn-sm" data-action="edit-member" data-id="${m.id}">Edit</button>
        <button type="button" class="btn btn-danger btn-sm" data-action="delete-member" data-id="${m.id}">Hapus</button>
      </div>
    </div>`;
}

function renderMembers() {
  return `
    <section>
      <div class="page-header">
        <h2 class="page-title">MEMBER</h2>
        <button class="btn btn-primary" id="btn-add-member">+ Daftar Member</button>
      </div>
      ${state.members.length === 0
        ? `<p class="empty">Belum ada member.</p>`
        : `<div class="grid-members">${state.members.map(memberCardHTML).join("")}</div>`}
    </section>`;
}

function bindMembers() {
  document.getElementById("btn-add-member")?.addEventListener("click", () => openMemberModal(null));

  document.querySelectorAll("[data-action='edit-member']").forEach(btn => {
    btn.addEventListener("click", () => {
      const m = state.members.find(x => String(x.id) === btn.dataset.id);
      if (m) openMemberModal(m);
    });
  });

  document.querySelectorAll("[data-action='delete-member']").forEach(btn => {
    btn.addEventListener("click", () => handleDeleteMember(btn.dataset.id));
  });
}

function memberFormHTML(m) {
  const isEdit = !!m;
  return `
    <div id="form-error" class="form-error" style="display:none"></div>

    <div class="form-group">
      <label class="form-label">Nama *</label>
      <input class="form-input" id="f-name" type="text" value="${escHtml(m?.name ?? "")}" placeholder="Nama lengkap">
    </div>

    <div class="form-group">
      <label class="form-label">No. Telepon *</label>
      <input class="form-input" id="f-phone" type="text" value="${escHtml(m?.phone ?? "")}" placeholder="08xxxxxxxxxx">
    </div>

    <div class="form-group">
      <label class="form-label">Email</label>
      <input class="form-input" id="f-email" type="email" value="${escHtml(m?.email ?? "")}" placeholder="email@contoh.com">
    </div>

    <div class="form-actions">
      <button type="button" class="btn btn-secondary" id="form-cancel">Batal</button>
      <button type="button" class="btn btn-primary" id="form-submit">${isEdit ? "Simpan Perubahan" : "Daftar Member"}</button>
    </div>`;
}

function openMemberModal(m) {
  showModal(m ? "Edit Member" : "Daftar Member Baru", memberFormHTML(m));
  document.getElementById("form-cancel").addEventListener("click", closeModal);
  document.getElementById("form-submit").addEventListener("click", async () => {
    const data = {
      name:  document.getElementById("f-name").value.trim(),
      phone: document.getElementById("f-phone").value.trim(),
      email: document.getElementById("f-email").value.trim(),
      tier:  m?.tier ?? "REGULAR",
    };

    const errEl = document.getElementById("form-error");

    // ── Validasi member ─────────────────────────────────────────
    if (!data.name) {
      errEl.textContent = "⚠ Nama wajib diisi.";
      errEl.style.display = "";
      showToast("Nama member wajib diisi.", "warning", "Field bertanda * tidak boleh kosong.");
      return;
    }
    if (!data.phone) {
      errEl.textContent = "⚠ No. HP wajib diisi.";
      errEl.style.display = "";
      showToast("Nomor HP wajib diisi.", "warning", "Masukkan nomor telepon yang aktif.");
      return;
    }
    if (!/^[0-9+\-\s]{8,15}$/.test(data.phone)) {
      errEl.textContent = "⚠ Format nomor telepon tidak valid.";
      errEl.style.display = "";
      showToast("Format nomor HP tidak valid.", "warning", "Contoh: 08123456789");
      return;
    }

    errEl.style.display = "none";

    const btn = document.getElementById("form-submit");
    btn.classList.add("btn-loading");
    try {
      if (m) {
        await memberApi.update(m.id, data);
        closeModal();
        await refreshAll();
        showToast("Member berhasil diupdate!", "success", `Data ${data.name} sudah disimpan.`);
      } else {
        await memberApi.create(data);
        closeModal();
        await refreshAll();
        showToast("Member berhasil didaftarkan!", "success", `${data.name} terdaftar sebagai member REGULAR.`);
      }
    } catch (e) {
      errEl.textContent = e.message;
      errEl.style.display = "";
      showToast("Gagal menyimpan member.", "error", e.message);
    } finally {
      btn.classList.remove("btn-loading");
    }
  });
}

async function handleDeleteMember(id) {
  const mem = state.members.find(x => String(x.id) === String(id));
  if (!confirm(`Hapus member "${mem?.name ?? id}"?`)) return;
  try {
    await memberApi.delete(id);
    await refreshAll();
    showToast("Member dihapus.", "error", `${mem?.name ?? id} telah dihapus dari sistem.`);
  } catch (e) {
    showToast("Gagal menghapus member.", "error", e.message);
  }
}

// ══════════════════════════════════════════════════════════════════
// TAB: TRANSAKSI
// ══════════════════════════════════════════════════════════════════
function renderTransactions() {
  const sorted = [...state.transactions].reverse();
  return `
    <section>
      <div class="page-header">
        <h2 class="page-title">RIWAYAT TRANSAKSI</h2>
        <button type="button" class="btn btn-primary" id="btn-add-trx">+ Transaksi Baru</button>
      </div>
      ${sorted.length === 0 ? `<p class="empty">Belum ada transaksi.</p>` : `
        <div class="trx-list">
          <div class="trx-list-header">
            <span>Invoice</span>
            <span>Tanggal</span>
            <span>Pelanggan</span>
            <span style="text-align:center">Item</span>
            <span>Total</span>
          </div>
          ${sorted.map(t => `
            <div class="trx-list-row">
              <span class="trx-list-id">${escHtml(String(t.id))}</span>
              <span class="trx-list-date">${escHtml(t.date ?? "")}</span>
              <span class="trx-list-member">
                ${t.member
                  ? `<span class="trx-member-name">${escHtml(t.member.name)}</span>
                     <span class="trx-tier-tag ${t.member.tier === "PLUS" ? "plus" : "regular"}">${t.member.tier}</span>`
                  : `<span class="trx-nonmember">Non-Member</span>`}
              </span>
              <span class="trx-list-items" style="text-align:center">${t.items?.length ?? 0} item</span>
              <span class="trx-list-total-col">
                <span class="trx-list-total">${formatRp(t.total)}</span>
                <button class="trx-detail-btn" data-action="view-struk" data-id="${t.id}">Lihat Detail →</button>
              </span>
            </div>`).join("")}
        </div>`}
    </section>`;
}

function bindTransactions() {
  document.getElementById("btn-add-trx")?.addEventListener("click", openTransactionModal);

  document.querySelectorAll("[data-action='view-struk']").forEach(btn => {
    btn.addEventListener("click", () => {
      const t = state.transactions.find(x => String(x.id) === btn.dataset.id);
      if (t) showStruk(t);
    });
  });
}

// ── Struk Popup ───────────────────────────────────────────────────
function showStruk(t) {
  const disc = t.member ? memberDiscountRate(t.member.tier) : 0;

  const subtotal = (t.items ?? []).reduce(
    (s, it) => s + ((it.pricePerItem ?? 0) * it.qty),
    0
  );
  const discAmt = subtotal * disc;

  const body = `
    <div class="struk-wrap">
      <div class="struk-header">
        <div class="struk-logo-row">
          <span class="struk-flag">🏎️</span>
          <div>
            <div class="struk-brand">McLAREN COLLECTION</div>
            <div class="struk-brand-sub">MERCHANDISE MANAGEMENT SYSTEM</div>
          </div>
        </div>
      </div>
      <hr class="struk-divider">
      <div class="struk-meta">
        <div class="struk-meta-row"><span class="struk-meta-key">Invoice</span><span class="struk-meta-val">${escHtml(String(t.id))}</span></div>
        <div class="struk-meta-row"><span class="struk-meta-key">Tanggal</span><span class="struk-meta-val">${escHtml(t.date ?? "")}</span></div>
        <div class="struk-meta-row"><span class="struk-meta-key">Pelanggan</span><span class="struk-meta-val">${t.member ? escHtml(t.member.name) : "Non-Member"}</span></div>
        ${t.member ? `<div class="struk-meta-row"><span class="struk-meta-key">Tier</span><span class="struk-meta-val">${t.member.tier}</span></div>` : ""}
      </div>
      <hr class="struk-divider dashed">
      <div class="struk-items">
        ${(t.items ?? []).map(it => {
          const itemName = it.productName ?? "?";
          const price    = it.pricePerItem ?? 0;
          return `
            <div class="struk-item-row">
              <div class="struk-item-left">
                <span class="struk-item-name">${escHtml(itemName)}</span>
                <span class="struk-item-qty">×${it.qty}</span>
              </div>
              <span class="struk-item-val">${formatRp(price * it.qty)}</span>
            </div>`;
        }).join("")}
      </div>
      <hr class="struk-divider dashed">
      <div class="struk-calc">
        <div class="struk-calc-row subtotal">
          <span>Subtotal</span>
          <span>${formatRp(subtotal)}</span>
        </div>
        ${disc > 0 ? `
        <div class="struk-calc-row discount">
          <span>Diskon Member (${disc * 100}%)</span>
          <span>-${formatRp(discAmt)}</span>
        </div>` : ""}
      </div>
      <hr class="struk-divider">
      <div class="struk-total-row">
        <span class="struk-total-label">TOTAL</span>
        <span class="struk-total-val">${formatRp(t.total)}</span>
      </div>
    </div>`;

  showModal("🧾 STRUK PEMBAYARAN", body);
}

// ── Transaction Modal ─────────────────────────────────────────────
function trxCartHTML() {
  const disc     = state.trxMember ? memberDiscountRate(state.trxMember.tier) : 0;
  const subtotal = state.trxCart.reduce((s, it) => s + (getFinalPrice(it.product) * it.qty), 0);
  const discAmt  = subtotal * disc;
  const total    = subtotal - discAmt;

  return state.trxCart.length === 0
    ? `<p style="color:var(--text-muted);font-size:12px;padding:0.5rem 0">Belum ada produk dipilih.</p>`
    : `<div class="cart">
        ${state.trxCart.map((it, idx) => `
          <div class="cart-item">
            <div class="cart-item-info">
              <p class="cart-item-name">${escHtml(it.product.name)}</p>
              <p class="cart-item-sub">${formatRp(getFinalPrice(it.product))} × ${it.qty} = ${formatRp(getFinalPrice(it.product) * it.qty)}</p>
            </div>
            <div class="cart-item-qty">
              <button class="qty-btn" data-cart-action="dec" data-idx="${idx}">−</button>
              <span class="qty-num">${it.qty}</span>
              <button class="qty-btn" data-cart-action="inc" data-idx="${idx}">+</button>
              <button class="qty-del" data-cart-action="del" data-idx="${idx}">×</button>
            </div>
          </div>`).join("")}
        <div class="cart-summary">
          <div class="summary-row">
            <span>Subtotal</span>
            <span style="font-family: var(--font-mono);">${formatRp(subtotal)}</span>
          </div>
          ${disc > 0 ? `
          <div class="summary-row discount">
            <span>Diskon Member (${disc * 100}%)</span>
            <span style="font-family: var(--font-mono);">-${formatRp(discAmt)}</span>
          </div>` : ""}
          <div class="summary-total">
            <span class="summary-total-label">TOTAL</span>
            <span class="summary-total-val">${formatRp(total)}</span>
          </div>
        </div>
      </div>`;
}

function renderTrxModalBody() {
  const productList = state.products
    .filter(p => p.stock > 0)
    .map(p => {
      const inCart = state.trxCart.find(it => it.product.id === p.id);
      return `
        <div class="product-pick-item">
          <div>
            <p class="product-pick-name">${escHtml(p.name)}</p>
            <p class="product-pick-price">
              ${formatRp(getFinalPrice(p))}
              <span class="product-pick-stok"> · Stok: ${p.stock}</span>
            </p>
          </div>
          <button type="button" class="btn btn-primary btn-sm" data-action="pick-product" data-id="${p.id}" ${inCart ? "disabled" : ""}>
            + Tambah
          </button>
        </div>`;
    }).join("");

  const suggestList = (!state.trxMember && state.trxMemberPhone.length > 0)
    ? (() => {
        const hasil = state.members.filter(m =>
          m.phone.includes(state.trxMemberPhone) ||
          m.name.toLowerCase().includes(state.trxMemberPhone.toLowerCase())
        );
        return hasil.length > 0 ? `
          <div class="member-suggest-list">
            ${hasil.map(m => `
              <button class="member-suggest-item" type="button" data-action="select-member" data-id="${m.id}">
                <span class="suggest-phone">${escHtml(m.phone)}</span>
                <span class="suggest-name"><strong>${escHtml(m.name)}</strong> · ${m.tier}</span>
              </button>`).join("")}
          </div>` : `<p style="font-size:12px;color:var(--text-muted);padding:0.4rem 0">Member tidak ditemukan.</p>`;
      })()
    : "";

  const memberSection = `
      <div class="trx-member-block">
        <label class="member-question-label" for="is-member-chk">Apakah pelanggan ini terdaftar sebagai member?</label>
        <div class="member-question-row">
          <input type="checkbox" id="is-member-chk" class="member-chk" ${state.trxUseMember ? "checked" : ""}>
          <span class="member-chk-text">Ya, pelanggan ini adalah member</span>
        </div>

        ${state.trxUseMember ? `
        <div class="member-search-wrap" style="position:relative; margin-top:0.75rem;">
          ${!state.trxMember ? `
            <div class="member-search-field">
              <span class="member-search-icon">📞</span>
              <input class="member-search-input" id="trx-phone-input"
                placeholder="Ketik nomor telepon atau nama member..."
                value="${escHtml(state.trxMemberPhone)}">
            </div>
            ${suggestList}
          ` : `
            <div class="member-status found">
              <span class="member-status-icon">✅</span>
              <div>
                <span>Member terpilih: <strong class="member-status-name">${escHtml(state.trxMember.name)}</strong></span>
                <span class="member-status-tier"> · ${state.trxMember.tier} (diskon ${memberDiscountRate(state.trxMember.tier) * 100}%)</span>
              </div>
              <button type="button" style="margin-left:auto; background:none; border:none; color:var(--text-muted); font-size:18px; cursor:pointer; line-height:1; padding:0 4px;" data-action="clear-member">×</button>
            </div>
          `}
        </div>` : ""}
      </div>`;

  return `
    <div class="trx-form">
      <div class="trx-block">
        <p class="trx-block-title">Pilih Produk</p>
        <div class="product-pick-list">
          ${productList || '<p style="padding:10px; color:var(--text-muted)">Stok habis.</p>'}
        </div>
      </div>

      <div class="trx-block">
        <p class="trx-block-title">Keranjang</p>
        ${trxCartHTML()}
      </div>

      ${memberSection}

      <div class="trx-checkout-row">
        <button type="button" class="btn btn-primary" id="btn-checkout" ${state.trxCart.length === 0 ? "disabled" : ""}>
          🏁 Checkout
        </button>
      </div>
    </div>`;
}

function bindTrxModal() {
  document.querySelectorAll("[data-action='pick-product']").forEach(el => {
    el.addEventListener("click", () => {
      const p = state.products.find(x => String(x.id) === el.dataset.id);
      if (!p) return;
      if (!state.trxCart.find(it => it.product.id === p.id))
        state.trxCart.push({ product: p, qty: 1 });
      refreshTrxModal();
    });
  });

  document.querySelectorAll("[data-cart-action]").forEach(btn => {
    btn.addEventListener("click", () => {
      const idx    = Number(btn.dataset.idx);
      const action = btn.dataset.cartAction;
      if (action === "inc") {
        const it = state.trxCart[idx];
        if (it.qty < it.product.stock) {
          it.qty++;
        } else {
          showToast("Stok tidak mencukupi.", "warning", `Maksimal ${it.product.stock} unit untuk produk ini.`);
        }
      } else if (action === "dec") {
        if (state.trxCart[idx].qty > 1) state.trxCart[idx].qty--;
        else state.trxCart.splice(idx, 1);
      } else if (action === "del") {
        state.trxCart.splice(idx, 1);
      }
      refreshTrxModal();
    });
  });

  document.getElementById("is-member-chk")?.addEventListener("change", function() {
    state.trxUseMember = this.checked;
    if (!this.checked) {
      state.trxMember = null;
      state.trxMemberPhone = "";
    }
    refreshTrxModal();
  });

  document.querySelectorAll("[data-action='select-member']").forEach(btn => {
    btn.addEventListener("click", () => {
      const m = state.members.find(x => String(x.id) === btn.dataset.id);
      if (m) {
        state.trxMember = m;
        state.trxMemberPhone = m.phone;
        state.trxUseMember = true;
        const chk = document.getElementById("is-member-chk");
        if (chk) chk.checked = true;
        refreshTrxModal();
        showToast("Member terpilih.", "info", `${m.name} · ${m.tier} (diskon ${memberDiscountRate(m.tier) * 100}%)`);
      }
    });
  });

  document.querySelector("[data-action='clear-member']")?.addEventListener("click", () => {
    state.trxMember = null;
    state.trxMemberPhone = "";
    state.trxUseMember = false;
    refreshTrxModal();
  });

  let memberSearchTimer = null;
  document.getElementById("trx-phone-input")?.addEventListener("input", function() {
    state.trxMemberPhone = this.value;
    state.trxMember = null;
    clearTimeout(memberSearchTimer);
    memberSearchTimer = setTimeout(() => refreshTrxModal(), 200);
  });

  document.getElementById("btn-checkout")?.addEventListener("click", handleCheckout);
}

function refreshTrxModal() {
  const body = document.querySelector("#modal-container .modal-body");
  if (!body) return;

  const input    = document.getElementById("trx-phone-input");
  const selStart = input?.selectionStart;
  const selEnd   = input?.selectionEnd;

  body.innerHTML = renderTrxModalBody();
  bindTrxModal();

  const newInput = document.getElementById("trx-phone-input");
  if (newInput && selStart !== undefined) {
    newInput.focus();
    newInput.setSelectionRange(selStart, selEnd);
  }
}

function openTransactionModal() {
  state.trxCart = [];
  state.trxMember = null;
  state.trxUseMember = false;
  state.trxMemberPhone = "";
  showModal("Transaksi Baru", renderTrxModalBody());
  bindTrxModal();
}

async function handleCheckout() {
  // Validasi cart kosong
  if (state.trxCart.length === 0) {
    showToast("Keranjang masih kosong.", "warning", "Tambahkan minimal satu produk sebelum checkout.");
    return;
  }

  const btn = document.getElementById("btn-checkout");
  if (btn) { btn.classList.add("btn-loading"); btn.textContent = "Memproses..."; }

  const payload = {
    memberPhone: state.trxMember?.phone ?? null,
    items: state.trxCart.map(it => ({ productId: it.product.id, qty: it.qty })),
  };

  try {
    const result = await transactionApi.create(payload);
    closeModal();
    await refreshAll();
    showToast("Transaksi berhasil!", "success", "Struk pembayaran siap ditampilkan.");
    if (result) setTimeout(() => showStruk(result), 200);
  } catch (e) {
    showToast("Transaksi gagal.", "error", e.message);
    if (btn) { btn.classList.remove("btn-loading"); btn.innerHTML = "🏁 Checkout"; }
  }
}

// ══════════════════════════════════════════════════════════════════
// GLOBAL
// ══════════════════════════════════════════════════════════════════
window.retryConnect = async function() {
  showToast("Mencoba terhubung ulang...", "info", "Menghubungi server Java di port 8080.");
  await refreshAll();
};

// ══════════════════════════════════════════════════════════════════
// INIT
// ══════════════════════════════════════════════════════════════════
document.addEventListener("DOMContentLoaded", () => refreshAll());