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
// TOAST
// ══════════════════════════════════════════════════════════════════
let toastTimer = null;
function showToast(msg, type = "success") {
  const el = document.getElementById("toast-container");
  el.innerHTML = `<div class="toast toast-${type}">${escHtml(msg)}</div>`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.innerHTML = ""; }, 3000);
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

        <!-- ✅ Tombol Transaksi Baru — menggantikan badge ADMIN -->
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
    <div id="toast-container"></div>`;

  // Tab events
  document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      state.activeTab = btn.dataset.tab;
      renderApp();
    });
  });

  // ✅ Tombol header selalu bisa membuka transaksi dari mana saja
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

  // Sync tombol header (selalu ada setelah shell)
  const headerBtn = document.getElementById("header-btn-trx");
  if (headerBtn) {
    // Re-bind untuk memastikan listener aktif setelah re-render
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

/* ═══════════════════════════════════════════════════════════════
   PRODUCTS TAB
   ═══════════════════════════════════════════════════════════════ */

function productCardHTML(p) {
  const final = getFinalPrice(p);

  return `
    <div class="product-card">
      <div class="product-card-body">

        <div class="product-top">
          <span class="product-emoji">
            ${CATEGORY_ICONS[p.category] ?? "📦"}
          </span>

          <div class="product-badges" style="display:flex; gap:6px;">
            ${p.jenis
              ? `<span class="badge badge-blue">${escHtml(p.jenis)}</span>`
              : ""}

            ${p.size
              ? `<span class="badge badge-green">${escHtml(p.size)}</span>`
              : ""}

            ${p.hasDiscount
              ? `<span class="badge badge-orange">-${p.discountPercent}%</span>`
              : ""}
          </div>
        </div>

        <p class="product-id">
          ${String(p.id).padStart(3, "0")}
        </p>

        <p class="product-name">
          ${escHtml(
            p.name.replace(/^McLaren\s+/i, "")
          ).toUpperCase()}
        </p>

        <p class="product-meta">
          ${escHtml(p.category)}
          ·
          Size: ${escHtml(p.size ?? "-")}
        </p>

        <div style="margin-top:12px; min-height:45px;">
          ${p.hasDiscount
            ? `<p class="product-orig">${formatRp(p.price)}</p>`
            : `<p class="product-orig" style="visibility:hidden;">-</p>`
          }

          <p class="product-price">
            ${formatRp(final)}
          </p>
        </div>

        <p class="product-stock">
          Stok: <span>${p.stock}</span>
        </p>
      </div>

      <div class="product-card-footer">
        <button
          type="button"
          class="btn btn-secondary btn-sm"
          data-action="edit-product"
          data-id="${p.id}"
        >
          EDIT
        </button>

        <button
          type="button"
          class="btn btn-danger btn-sm"
          data-action="delete-product"
          data-id="${p.id}"
        >
          HAPUS
        </button>
      </div>
    </div>
  `;
}

/* ───────────────────────────────────────────────────────────── */

function renderProducts() {

  const filtered =
    state.filterKat === "Semua"
      ? state.products
      : state.products.filter(
          p => p.category === state.filterKat
        );

  return `
    <section>

      <div class="page-header">
        <h2 class="page-title">PRODUK</h2>

        <button
          class="btn btn-primary"
          id="btn-add-product"
        >
          + TAMBAH PRODUK
        </button>
      </div>

      <div class="filter-bar">

        <button
          class="filter-btn ${
            state.filterKat === "Semua"
              ? "active"
              : ""
          }"
          data-filter="Semua"
        >
          SEMUA
        </button>

        ${KATEGORI_LIST.map(k => `
          <button
            class="filter-btn ${
              state.filterKat === k
                ? "active"
                : ""
            }"
            data-filter="${k}"
          >
            ${k.toUpperCase()}
          </button>
        `).join("")}

      </div>

      ${filtered.length === 0
        ? `
          <p class="empty">
            Belum ada produk dalam kategori ini.
          </p>
        `
        : `
          <div class="grid-cards">
            ${filtered.map(productCardHTML).join("")}
          </div>
        `
      }

    </section>
  `;
}

/* ───────────────────────────────────────────────────────────── */

function bindProducts() {

  document
    .getElementById("btn-add-product")
    ?.addEventListener("click", () => {
      openProductModal(null);
    });

  document
    .querySelectorAll(".filter-btn")
    .forEach(btn => {

      btn.addEventListener("click", () => {

        state.filterKat = btn.dataset.filter;

        renderApp();
      });
    });

  document
    .querySelectorAll("[data-action='edit-product']")
    .forEach(btn => {

      btn.addEventListener("click", () => {

        const p = state.products.find(
          x => String(x.id) === btn.dataset.id
        );

        if (p) openProductModal(p);
      });
    });

  document
    .querySelectorAll("[data-action='delete-product']")
    .forEach(btn => {

      btn.addEventListener("click", () => {
        handleDeleteProduct(btn.dataset.id);
      });
    });
}

/* ───────────────────────────────────────────────────────────── */

function productFormHTML(p) {

  const isEdit = !!p;

  const cats  = KATEGORI_LIST;

  const cat   = p?.category ?? cats[0];

  const jenis = JENIS_MAP[cat] ?? [];

  return `

    <div
      id="form-error"
      class="form-error"
      style="display:none"
    ></div>

    <!-- NAMA -->
    <div class="form-group">
      <label class="form-label">
        Nama Produk *
      </label>

      <input
        class="form-input"
        id="f-name"
        type="text"
        value="${escHtml(p?.name ?? "")}"
        placeholder="Nama produk"
      >
    </div>

    <!-- KATEGORI -->
    <div class="form-group">
      <label class="form-label">
        Kategori
      </label>

      <select
        class="form-select"
        id="f-category"
      >
        ${cats.map(c => `
          <option
            value="${c}"
            ${c === cat ? "selected" : ""}
          >
            ${c}
          </option>
        `).join("")}
      </select>
    </div>

    <!-- JENIS -->
    <div class="form-group">
      <label class="form-label">
        Jenis
      </label>

      <select
        class="form-select"
        id="f-jenis"
      >
        ${jenis.map(j => `
          <option
            value="${j}"
            ${j === p?.jenis ? "selected" : ""}
          >
            ${j}
          </option>
        `).join("")}
      </select>
    </div>

    <!-- HARGA -->
    <div class="form-group">
      <label class="form-label">
        Harga (Rp) *
      </label>

      <input
        class="form-input"
        id="f-price"
        type="number"
        value="${p?.price ?? ""}"
        placeholder="0"
        min="0"
      >
    </div>

    <!-- STOK -->
    <div class="form-group">
      <label class="form-label">
        Stok *
      </label>

      <input
        class="form-input"
        id="f-stock"
        type="number"
        value="${p?.stock ?? ""}"
        placeholder="0"
        min="0"
      >
    </div>

    <!-- SIZE -->
    <div class="form-group">
      <label class="form-label">
        Size / Varian
      </label>

      <input
        class="form-input"
        id="f-size"
        type="text"
        value="${escHtml(p?.size ?? "")}"
        placeholder="M, L, XL, Free Size, 42, ..."
      >
    </div>

    <!-- DISKON -->
    <div class="form-checkbox-row">

      <input
        type="checkbox"
        id="f-hasDiscount"
        ${p?.hasDiscount ? "checked" : ""}
      >

      <label for="f-hasDiscount">
        Ada Diskon?
      </label>
    </div>

    <!-- PERSEN DISKON -->
    <div
      class="form-group"
      id="discount-group"
      style="${p?.hasDiscount ? "" : "display:none"}"
    >
      <label class="form-label">
        Diskon (%)
      </label>

      <input
        class="form-input"
        id="f-discountPercent"
        type="number"
        value="${p?.discountPercent ?? ""}"
        min="0"
        max="100"
        placeholder="0"
      >
    </div>

    <!-- ACTION -->
    <div class="form-actions">

      <button
        class="btn btn-secondary"
        id="form-cancel"
      >
        Batal
      </button>

      <button
        class="btn btn-primary"
        id="form-submit"
      >
        ${isEdit ? "Simpan" : "Tambah Produk"}
      </button>

    </div>
  `;
}

/* ───────────────────────────────────────────────────────────── */

function openProductModal(p) {

  showModal(
    p
      ? "Edit Produk"
      : "Tambah Produk Baru",

    productFormHTML(p)
  );

  /* CATEGORY CHANGE */

  document
    .getElementById("f-category")
    .addEventListener("change", function () {

      const jenis =
        JENIS_MAP[this.value] ?? [];

      const sel =
        document.getElementById("f-jenis");

      sel.innerHTML = jenis.map(j => `
        <option value="${j}">
          ${j}
        </option>
      `).join("");
    });

  /* DISCOUNT TOGGLE */

  document
    .getElementById("f-hasDiscount")
    .addEventListener("change", function () {

      document.getElementById(
        "discount-group"
      ).style.display = this.checked
        ? ""
        : "none";
    });

  /* CANCEL */

  document
    .getElementById("form-cancel")
    .addEventListener("click", closeModal);

  /* SUBMIT */

  document
    .getElementById("form-submit")
    .addEventListener("click", async () => {

      const data = {

        name:
          document
            .getElementById("f-name")
            .value
            .trim(),

        category:
          document
            .getElementById("f-category")
            .value,

        jenis:
          document
            .getElementById("f-jenis")
            .value,

        price:
          Number(
            document
              .getElementById("f-price")
              .value
          ),

        stock:
          Number(
            document
              .getElementById("f-stock")
              .value
          ),

        size:
          document
            .getElementById("f-size")
            .value
            .trim(),

        hasDiscount:
          document
            .getElementById("f-hasDiscount")
            .checked,

        discountPercent:
          Number(
            document
              .getElementById("f-discountPercent")
              .value
          ) || 0,
      };

      const errEl =
        document.getElementById("form-error");

      /* VALIDASI */

      if (!data.name) {
        errEl.textContent =
          "Nama produk wajib diisi.";
        errEl.style.display = "";
        return;
      }

      if (!data.price || data.price <= 0) {
        errEl.textContent =
          "Harga harus lebih dari 0.";
        errEl.style.display = "";
        return;
      }

      if (!data.stock || data.stock < 0) {
        errEl.textContent =
          "Stok tidak valid.";
        errEl.style.display = "";
        return;
      }

      errEl.style.display = "none";

      const btn =
        document.getElementById("form-submit");

      btn.classList.add("btn-loading");

      try {

        if (p) {

          await productApi.update(
            p.id,
            data
          );

          showToast(
            "Produk berhasil diupdate!"
          );

        } else {

          await productApi.create(data);

          showToast(
            "Produk berhasil ditambahkan!"
          );
        }

        closeModal();

        await refreshAll();

      } catch (e) {

        errEl.textContent = e.message;

        errEl.style.display = "";

      } finally {

        btn.classList.remove("btn-loading");
      }
    });
}

/* ───────────────────────────────────────────────────────────── */

async function handleDeleteProduct(id) {

  if (!confirm("Hapus produk ini?"))
    return;

  try {

    await productApi.delete(id);

    showToast("Produk dihapus.");

    await refreshAll();

  } catch (e) {

    showToast(e.message, "error");
  }
}

// ══════════════════════════════════════════════════════════════════
// TAB: MEMBER
// ══════════════════════════════════════════════════════════════════
function memberCardHTML(m) {
  // ✅ Hitung dari state.transactions agar selalu sinkron
  const memberTrx = state.transactions.filter(t =>
    t.member && String(t.member.id) === String(m.id)
  );
  const totalTrx     = memberTrx.length;
  const totalBelanja = memberTrx.reduce((s, t) => s + (t.total ?? 0), 0);

  // ✅ Tier dihitung dinamis dari total belanja
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
        <p class="progress-label">
          Menuju PLUS: <span style="font-family:var(--font-mono);">${formatRp(sisa)}</span> lagi
        </p>
        <div class="progress-bar">
          <div class="progress-fill" style="width:${prog}%"></div>
        </div>
      </div>` : `
      <div style="margin: 0.5rem 0;">
        <p class="progress-label" style="color:var(--accent);">
          ✅ Member PLUS aktif · Diskon 15%
        </p>
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
    <div class="form-group">
      <label class="form-label">Tier</label>
      <select class="form-select" id="f-tier">
        <option value="REGULAR" ${m?.tier === "REGULAR" ? "selected" : ""}>Regular (diskon 10%)</option>
        <option value="PLUS" ${m?.tier === "PLUS" ? "selected" : ""}>Plus (diskon 15%)</option>
      </select>
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
      tier : document.getElementById("f-tier").value,
    };
    const errEl = document.getElementById("form-error");
    if (!data.name)  { errEl.textContent = "Nama wajib diisi.";   errEl.style.display = ""; return; }
    if (!data.phone) { errEl.textContent = "No. HP wajib diisi."; errEl.style.display = ""; return; }
    if (!data.email) { errEl.textContent = "Email wajib diisi.";  errEl.style.display = ""; return; }
    errEl.style.display = "none";

    const btn = document.getElementById("form-submit");
    btn.classList.add("btn-loading");
    try {
      if (m) { await memberApi.update(m.id, data); showToast("Member berhasil diupdate!"); }
      else   { await memberApi.create(data);        showToast("Member berhasil didaftarkan!"); }
      closeModal();
      await refreshAll();
    } catch (e) {
      errEl.textContent = e.message; errEl.style.display = "";
    } finally {
      btn.classList.remove("btn-loading");
    }
  });
}

async function handleDeleteMember(id) {
  if (!confirm("Hapus member ini?")) return;
  try { await memberApi.delete(id); showToast("Member dihapus."); await refreshAll(); }
  catch (e) { showToast(e.message, "error"); }
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
  const subtotal = (t.items ?? []).reduce((s, it) => {
    const p = state.products.find(x => x.id === (it.productId ?? it.product?.id));
    return s + (getFinalPrice(p ?? it.product ?? {}) * it.qty);
  }, 0);
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
          const p = state.products.find(x => x.id === (it.productId ?? it.product?.id)) ?? it.product ?? {};
          const price = getFinalPrice(p);
          return `
            <div class="struk-item-row">
              <div class="struk-item-left">
                <span class="struk-item-name">${escHtml(p.name ?? it.productName ?? "?")}</span>
                <span class="struk-item-qty">×${it.qty}</span>
              </div>
              <span class="struk-item-val">${formatRp(price * it.qty)}</span>
            </div>`;
        }).join("")}
      </div>
      <hr class="struk-divider dashed">
      <div class="struk-calc">
        <div class="struk-calc-row subtotal"><span>Subtotal</span><span>${formatRp(subtotal)}</span></div>
        ${disc > 0 ? `<div class="struk-calc-row discount"><span>Diskon Member (${disc * 100}%)</span><span>-${formatRp(discAmt)}</span></div>` : ""}
      </div>
      <hr class="struk-divider">
      <div class="struk-total-row">
        <span class="struk-total-label">TOTAL</span>
        <span class="struk-total-val">${formatRp(t.total)}</span>
      </div>
    </div>`;

  showModal("🧾 STRUK PEMBAYARAN", body);
}

// ══════════════════════════════════════════════════════════════════
// TRANSACTION MODAL
// ══════════════════════════════════════════════════════════════════
function trxCartHTML() {
  // ✅ Hitung tier efektif dari riwayat belanja
  const memberTrx = state.trxMember
    ? state.transactions.filter(t => t.member && String(t.member.id) === String(state.trxMember.id))
    : [];
  const totalBelanjaSekarang = memberTrx.reduce((s, t) => s + (t.total ?? 0), 0);
  const effectiveTier = totalBelanjaSekarang >= 5_000_000 ? "PLUS" : (state.trxMember?.tier ?? "");
  const disc = state.trxMember ? memberDiscountRate(effectiveTier) : 0;
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
          <div class="summary-row"><span>Subtotal</span><span style="font-family:var(--font-mono)">${formatRp(subtotal)}</span></div>
          ${disc > 0 ? `<div class="summary-row discount"><span>Diskon Member (${disc*100}%)</span><span style="font-family:var(--font-mono)">-${formatRp(discAmt)}</span></div>` : ""}
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
            <p class="product-pick-price">${formatRp(getFinalPrice(p))}<span class="product-pick-stok"> · Stok: ${p.stock}</span></p>
          </div>
          <button type="button" class="btn btn-primary btn-sm" data-action="pick-product" data-id="${p.id}" ${inCart ? "disabled" : ""}>
            + Tambah
          </button>
        </div>`;
    }).join("");

  const memberSection = `
    <div class="trx-member-block">
      <label class="member-question-label" for="is-member-chk">Apakah pelanggan ini terdaftar sebagai member?</label>
      <div class="member-question-row">
        <input type="checkbox" id="is-member-chk" class="member-chk" ${state.trxUseMember ? "checked" : ""}>
        <span class="member-chk-text">Ya, pelanggan ini adalah member</span>
      </div>

      ${state.trxUseMember && !state.trxMember ? `
        <div class="member-search-wrap" style="position:relative">
          <div class="member-search-field">
            <span class="member-search-icon">📞</span>
            <input class="member-search-input" id="trx-phone-input"
              placeholder="Ketik nomor telepon atau nama member..."
              value="${escHtml(state.trxMemberPhone)}">
          </div>
          ${state.trxMemberPhone.length > 0 ? `
            <div class="member-suggest-list">
              ${state.members
                .filter(m => m.phone.includes(state.trxMemberPhone) || m.name.toLowerCase().includes(state.trxMemberPhone.toLowerCase()))
                .map(m => `
                  <button class="member-suggest-item" type="button" data-action="select-member" data-id="${m.id}">
                    <span class="suggest-phone">${escHtml(m.phone)}</span>
                    <span class="suggest-name"><strong>${escHtml(m.name)}</strong> · ${m.tier}</span>
                  </button>`).join("")
              }
            </div>` : ""}
        </div>` : ""}

      ${state.trxMember ? `
        <div class="member-status found" style="margin-top:0.6rem">
          <span class="member-status-icon">✅</span>
          <div>
            <span class="member-status-name">${escHtml(state.trxMember.name)}</span>
            <span class="member-status-tier"> — Tier <strong>${state.trxMember.tier}</strong> · Diskon ${memberDiscountRate(state.trxMember.tier)*100}%</span>
          </div>
          <button type="button" style="margin-left:auto;background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:16px" data-action="clear-member">×</button>
        </div>` : ""}
    </div>`;

  return `
    <div class="trx-form">
      <div class="trx-block">
        <p class="trx-block-title">Pilih Produk</p>
        <div class="product-pick-list">
          ${productList || '<p style="padding:10px;color:var(--text-muted)">Stok habis.</p>'}
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
      const existing = state.trxCart.find(it => it.product.id === p.id);
      if (!existing) state.trxCart.push({ product: p, qty: 1 });
      refreshTrxModal();
    });
  });

  document.querySelectorAll("[data-cart-action]").forEach(btn => {
    btn.addEventListener("click", () => {
      const idx = Number(btn.dataset.idx);
      const action = btn.dataset.cartAction;
      if (action === "inc") {
        const it = state.trxCart[idx];
        if (it.qty < it.product.stock) it.qty++;
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
    if (!this.checked) { state.trxMember = null; state.trxMemberPhone = ""; }
    refreshTrxModal();
  });

  document.querySelectorAll("[data-action='select-member']").forEach(btn => {
    btn.addEventListener("click", () => {
      const m = state.members.find(x => String(x.id) === btn.dataset.id);
      if (m) { state.trxMember = m; state.trxMemberPhone = m.phone; refreshTrxModal(); }
    });
  });

  document.querySelector("[data-action='clear-member']")?.addEventListener("click", () => {
    state.trxMember = null; state.trxMemberPhone = ""; refreshTrxModal();
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
  body.innerHTML = renderTrxModalBody();
  bindTrxModal();
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
    showToast("Transaksi berhasil!");
    // ✅ Tampilkan struk otomatis setelah checkout
    if (result) setTimeout(() => showStruk(result), 200);
  } catch (e) {
    showToast(e.message, "error");
    if (btn) { btn.classList.remove("btn-loading"); btn.innerHTML = "🏁 Checkout"; }
  }
}

// ══════════════════════════════════════════════════════════════════
// GLOBAL
// ══════════════════════════════════════════════════════════════════
window.retryConnect = async function() { await refreshAll(); };

// ══════════════════════════════════════════════════════════════════
// INIT
// ══════════════════════════════════════════════════════════════════
document.addEventListener("DOMContentLoaded", () => refreshAll());
