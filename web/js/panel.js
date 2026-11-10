/* =========================================================================
 * panel.js — điều khiển giao diện panel quản trị
 *
 * Gồm 4 phần:
 *   1. NAV      : cấu hình menu sidebar (thêm page mới = thêm 1 dòng ở đây)
 *   2. Router   : đổi view theo hash, đồng bộ breadcrumb + active menu
 *   3. Settings : load/save/validate form "Cài Đặt Website"
 *   4. Extras   : theme sáng/tối, kéo thả logo, toast, trạng thái server
 * ========================================================================= */
(function () {
    'use strict';

    /* =============================== 1. NAV =============================== */
    var NAV = [
        {
            label: 'Báo cáo & Phân tích',
            items: [
                { key: 'revenue', label: 'Doanh Thu & CSDL', badge: 'CSDL' }
            ]
        },
        {
            label: 'Máy Chủ Trực Tuyến',
            items: [
                { key: 'servers', label: 'Máy Chủ Trực Tuyến', badge: 'LIVE', badgeClass: 'nav-badge--live' }
            ]
        },
        {
            label: 'Quản Lý Dữ Liệu Game',
            items: [
                { key: 'giftcodes', label: 'Quản Lý Giftcodes', badge: 'CODE' },
                { key: 'top-gift', label: 'Quản Lý Top', badge: 'TOP' },
                { key: 'map', label: 'Quản Lý Map', badge: 'MAP' },
                { key: 'mob', label: 'Quản Lý Mob', badge: 'MOB' },
                { key: 'boss', label: 'Quản Lý Boss', badge: 'BOSS' },
                { key: 'drop', label: 'Quản Lý Drop', badge: 'DROP' },
                { key: 'boss-drop', label: 'Quản Lý Drop Boss', badge: 'DROP' },
                { key: 'combine-rates', label: 'Tỉ Lệ Đập Đồ', badge: 'RATE' },
                { key: 'intrinsic', label: 'Quản Lý Nội Tại', badge: 'INTRINSIC' },
                { key: 'player', label: 'Quản Lý Nhân Vật', badge: 'PLAYER' },
                { key: 'account', label: 'Quản Lý Tài Khoản', badge: 'ACCOUNT' },
                { key: 'shop-npc', label: 'Cửa Hàng Shop NPC', badge: 'SHOP' },
                { key: 'donate-package', label: 'Quản Lý Gói Donate', badge: 'DONATE' },
                { key: 'economy', label: 'Kinh Tế & Mini Game', badge: 'ECON' },
                { key: 'lucky-round', label: 'Vòng Quay Thượng Đế', badge: 'QUAY' }
            ]
        },
        {
            label: 'Cộng Đồng & Diễn Đàn',
            items: [
                { key: 'community', label: 'Diễn Đàn & Thông Báo', badge: 'COMM' }
            ]
        },
        {
            label: 'Hệ Thống & Thanh Toán',
            items: [
                { key: 'payment', label: 'Cấu Hình Nạp Tiền', badge: 'PAY' },
                { key: 'website-settings', label: 'Cài Đặt Website', badge: 'WEB' },
                { key: 'cloudflare', label: 'Quản Lý DNS Cloudflare', dot: true },
                { key: 'database-reset', label: 'Reset Dữ Liệu', badge: 'RESET' }
            ]
        }
    ];

    var DEFAULT_ROUTE = 'revenue';

    /* Danh sách field của form "Cài Đặt Website" (id === key lưu trong settings.json) */
    var SETTING_FIELDS = [
        'appName', 'fname', 'ftitle', 'domain', 'supportEmail', 'fgmail', 'fpgmail',
        'zalo', 'zalo2'
    ];

    /* Các field bắt buộc phải là URL hợp lệ nếu có nhập */
    var URL_FIELDS = ['zalo', 'zalo2', 'domain'];

    var DEFAULT_BRAND = 'Ngọc Rồng';
    var THEME_KEY = 'webpanel.theme';

    var state = {
        settings: {},
        logo: '',
        dirty: false
    };

    /* =============================== 2. ROUTER ============================ */
    var sidebarNav = document.getElementById('sidebarNav');
    var breadcrumb = document.getElementById('breadcrumb');
    var content = document.getElementById('content');

    function flattenNav() {
        var flat = {};
        NAV.forEach(function (group) {
            group.items.forEach(function (item) {
                flat[item.key] = { item: item, group: group };
            });
        });
        return flat;
    }

    var NAV_FLAT = flattenNav();

    function renderSidebar() {
        sidebarNav.innerHTML = '';

        NAV.forEach(function (group) {
            var wrap = document.createElement('div');
            wrap.className = 'nav-group';

            var title = document.createElement('div');
            title.className = 'nav-group__title';
            title.textContent = group.label;
            wrap.appendChild(title);

            group.items.forEach(function (item) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'nav-item';
                btn.dataset.route = item.key;
                btn.title = item.label;

                var label = document.createElement('span');
                label.className = 'nav-item__label';
                label.textContent = item.label;
                btn.appendChild(label);

                if (item.badge) {
                    var badge = document.createElement('span');
                    badge.className = 'nav-badge' + (item.badgeClass ? ' ' + item.badgeClass : '');
                    badge.textContent = item.badge;
                    btn.appendChild(badge);
                } else if (item.dot) {
                    var dot = document.createElement('i');
                    dot.className = 'nav-dot';
                    btn.appendChild(dot);
                }

                btn.addEventListener('click', function () { navigate(item.key); });
                wrap.appendChild(btn);
            });

            sidebarNav.appendChild(wrap);
        });
    }

    function currentRoute() {
        var hash = (location.hash || '').replace(/^#\/?/, '').trim();
        return NAV_FLAT[hash] ? hash : DEFAULT_ROUTE;
    }

    function navigate(route, replace) {
        if (replace) {
            history.replaceState(null, '', '#/' + route);
        } else {
            location.hash = '#/' + route;
        }
        routeChanged();
    }

    function routeChanged() {
        var route = currentRoute();
        var found = NAV_FLAT[route];

        // active menu
        Array.prototype.forEach.call(sidebarNav.querySelectorAll('.nav-item'), function (el) {
            el.classList.toggle('is-active', el.dataset.route === route);
        });

        // breadcrumb
        breadcrumb.innerHTML = '';
        var groupSpan = document.createElement('span');
        groupSpan.textContent = found ? found.group.label : 'Hệ Thống';
        var sep = document.createElement('span');
        sep.className = 'breadcrumb__sep';
        sep.textContent = '/';
        var strong = document.createElement('strong');
        strong.textContent = found ? found.item.label : 'Cài Đặt Website';
        breadcrumb.appendChild(groupSpan);
        breadcrumb.appendChild(sep);
        breadcrumb.appendChild(strong);

        // view
        var view = document.getElementById('view-' + route);
        if (!view) {
            view = document.getElementById('view-placeholder');
            document.getElementById('placeholderTitle').textContent =
                found ? found.item.label : 'Chức năng đang phát triển';
            document.getElementById('placeholderDesc').textContent =
                'Khung giao diện cho mục "' + (found ? found.item.label : route) +
                '" đã sẵn sàng. Phần chức năng sẽ được bổ sung theo từng task.';
            document.getElementById('placeholderKey').textContent = route;
        }

        Array.prototype.forEach.call(content.querySelectorAll('.view'), function (el) {
            el.classList.remove('is-active');
        });
        view.classList.add('is-active');

        document.title = (found ? found.item.label : 'Quản Trị Hệ Thống') + ' — Quản Trị Hệ Thống';
        content.scrollTop = 0;
        window.scrollTo({ top: 0 });

        if (!PanelAPI.served || !auth || !auth.username) return;

        if (route === 'giftcodes') {
            giftcodes.load();
        }

        // Trang Máy Chủ Trực Tuyến tự làm mới khi đang mở
        if (route === 'servers') {
            servers.start();
            loginAccessControl.start();
        } else {
            servers.stop();
            loginAccessControl.stop();
        }

        if (route === 'revenue') {
            revenue.start();
        } else {
            revenue.stop();
        }

        if (route === 'shop-npc') {
            shop.start();
        } else {
            shop.stop();
        }

        if (route === 'player') {
            player.start();
        } else {
            player.stop();
        }

        if (route === 'map') {
            mapPanel.start();
        } else {
            mapPanel.stop();
        }

        if (route === 'mob') {
            mobPanel.start();
        } else {
            mobPanel.stop();
        }

        if (window.DropPanel) {
            if (route === 'drop') window.DropPanel.start();
            else window.DropPanel.stop();
        }
        if (window.BossPanel) {
            if (route === 'boss') window.BossPanel.start();
            else window.BossPanel.stop();
        }
        if (window.BossDropPanel) {
            if (route === 'boss-drop') window.BossDropPanel.start();
            else window.BossDropPanel.stop();
        }
        if (window.CombineRatesPanel) {
            if (route === 'combine-rates') window.CombineRatesPanel.start();
            else window.CombineRatesPanel.stop();
        }
        if (window.TopPanel) {
            if (route === 'top-gift') window.TopPanel.start();
            else window.TopPanel.stop();
        }
        if (window.LuckyRoundPanel) {
            if (route === 'lucky-round') window.LuckyRoundPanel.start();
            else window.LuckyRoundPanel.stop();
        }
        if (window.DatabaseResetPanel) {
            if (route === 'database-reset') window.DatabaseResetPanel.start();
            else window.DatabaseResetPanel.stop();
        }
        if (window.DonatePackagePanel) {
            if (route === 'donate-package') window.DonatePackagePanel.start();
            else window.DonatePackagePanel.stop();
        }

        if (route === 'intrinsic') {
            intrinsicPanel.start();
        } else {
            intrinsicPanel.stop();
        }

        if (route === 'account') {
            accountPanel.start();
        } else {
            accountPanel.stop();
        }

        // Hai trang cùng đọc cấu hình Java panel; không sửa website/PHP bên ngoài.
        if (route === 'website-settings' || route === 'payment') {
            serverConfig.start();
        } else {
            serverConfig.stop();
        }

        if (route === 'cloudflare') {
            cloudflarePanel.start();
        } else {
            cloudflarePanel.stop();
        }
    }

    window.addEventListener('hashchange', routeChanged);

    /* ============================ 3. SETTINGS ============================= */
    var form = document.getElementById('settingsForm');
    var saveStatus = document.getElementById('saveStatus');
    var logoInput = document.getElementById('logoInput');
    var logoDropzone = document.getElementById('logoDropzone');
    var logoThumb = document.getElementById('logoThumb');

    function readForm() {
        var data = {};
        SETTING_FIELDS.forEach(function (id) {
            var el = document.getElementById(id);
            data[id] = el ? el.value.trim() : '';
        });
        data.logo = state.logo || '';
        return data;
    }

    function fillForm(data) {
        data = data || {};
        SETTING_FIELDS.forEach(function (id) {
            var el = document.getElementById(id);
            if (el) el.value = data[id] || '';
        });
        state.logo = data.logo || '';
        renderLogoThumb();
        renderPreview();
    }

    function renderLogoThumb() {
        if (state.logo) {
            var image = document.createElement('img');
            image.src = state.logo;
            image.alt = 'logo';
            logoThumb.replaceChildren(image);
        } else {
            logoThumb.innerHTML =
                '<svg viewBox="0 0 24 24" class="dropzone__thumb-icon">' +
                '<rect x="3" y="3" width="18" height="18" rx="3"/>' +
                '<circle cx="8.5" cy="8.5" r="1.8"/>' +
                '<path d="M4 17l5-5 4 4 3-3 4 4"/></svg>';
        }
    }

    function renderPreview() {
        var brand = document.getElementById('previewBrand');
        var domain = document.getElementById('previewDomain');

        var fname = valueOf('fname') || valueOf('appName') || DEFAULT_BRAND;
        brand.textContent = fname;
        domain.textContent = valueOf('domain') || DEFAULT_BRAND;

        Array.prototype.forEach.call(document.querySelectorAll('[data-preview-out]'), function (el) {
            var v = valueOf(el.dataset.previewOut);
            if (v) {
                el.textContent = v;
                el.classList.remove('is-empty');
                el.title = v;
            } else {
                el.textContent = 'Chưa Có Link';
                el.classList.add('is-empty');
                el.removeAttribute('title');
            }
        });
    }

    function valueOf(id) {
        var el = document.getElementById(id);
        return el ? el.value.trim() : '';
    }

    function markDirty(dirty) {
        state.dirty = dirty;
        saveStatus.classList.toggle('is-dirty', dirty);
        saveStatus.classList.toggle('is-saved', !dirty && !!saveStatus.dataset.saved);
        saveStatus.textContent = dirty ? 'Có thay đổi chưa lưu' : (saveStatus.dataset.saved ? 'Đã lưu cấu hình' : 'Chưa có thay đổi');
    }

    function isUrl(value) {
        try {
            var u = new URL(value);
            return u.protocol === 'http:' || u.protocol === 'https:';
        } catch (e) {
            return false;
        }
    }

    function validate() {
        var ok = true;
        URL_FIELDS.forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) return;
            var v = el.value.trim();
            el.classList.remove('is-invalid');
            if (v && !isUrl(v)) {
                el.classList.add('is-invalid');
                ok = false;
            }
        });
        if (!ok) toast('Có liên kết chưa đúng định dạng http(s)://', 'error');
        return ok;
    }

    async function loadSettings() {
        if (!PanelAPI.served) return;
        try {
            state.settings = await PanelAPI.loadSettings();
            fillForm(state.settings);
            markDirty(false);
        } catch (e) {
            toast('Không tải được cấu hình panel: ' + e.message, 'error');
        }
        applyStatusPill();
    }

    async function saveSettings() {
        if (!validate()) return;
        var btn = document.getElementById('btnSave');
        btn.disabled = true;
        var old = btn.textContent;
        btn.textContent = 'Đang lưu...';
        try {
            var saved = await PanelAPI.saveSettings(readForm());
            state.settings = saved || readForm();
            saveStatus.dataset.saved = '1';
            markDirty(false);
            toast('Đã lưu cấu hình website', 'success');
        } catch (e) {
            toast('Lưu thất bại: ' + e.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = old;
        }
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        saveSettings();
    });

    form.addEventListener('input', function (e) {
        if (e.target.matches('input')) {
            e.target.classList.remove('is-invalid');
            renderPreview();
            markDirty(true);
        }
    });

    document.getElementById('btnReset').addEventListener('click', function () {
        if (!window.confirm('Khôi phục toàn bộ cấu hình về mặc định?')) return;
        var empty = {};
        SETTING_FIELDS.forEach(function (id) { empty[id] = ''; });
        empty.logo = '';
        fillForm(empty);
        markDirty(true);
        toast('Đã khôi phục giá trị mặc định (chưa lưu)', 'success');
    });

    /* ---- logo: click + kéo thả ---- */
    logoInput.addEventListener('change', function () {
        if (logoInput.files && logoInput.files[0]) handleLogo(logoInput.files[0]);
    });

    ['dragenter', 'dragover'].forEach(function (evt) {
        logoDropzone.addEventListener(evt, function (e) {
            e.preventDefault();
            logoDropzone.classList.add('is-dragover');
        });
    });

    ['dragleave', 'drop'].forEach(function (evt) {
        logoDropzone.addEventListener(evt, function (e) {
            e.preventDefault();
            logoDropzone.classList.remove('is-dragover');
        });
    });

    logoDropzone.addEventListener('drop', function (e) {
        var file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
        if (file) handleLogo(file);
    });

    async function handleLogo(file) {
        if (file.size > 2 * 1024 * 1024) {
            toast('Ảnh logo vượt quá 2MB', 'error');
            return;
        }
        try {
            state.logo = await PanelAPI.uploadLogo(file);
            renderLogoThumb();
            markDirty(true);
            toast('Đã chọn logo mới (bấm Lưu để áp dụng)', 'success');
        } catch (err) {
            toast(err.message, 'error');
        }
    }

    /* ---- nút mở liên kết nhanh ---- */
    Array.prototype.forEach.call(document.querySelectorAll('[data-open]'), function (btn) {
        btn.addEventListener('click', function () {
            var input = document.querySelector(btn.dataset.open);
            var v = input ? input.value.trim() : '';
            if (!v || !isUrl(v)) {
                toast('Chưa có liên kết hợp lệ để mở', 'error');
                return;
            }
            window.open(v, '_blank', 'noopener');
        });
    });

    /* ============================ 4. GIFTCODE ============================ */
    var giftcodeBody = document.getElementById('giftcodeBody');
    var giftcodeEmpty = document.getElementById('giftcodeEmpty');
    var giftcodeModal = document.getElementById('giftcodeModal');
    var giftcodeForm = document.getElementById('giftcodeForm');

    var giftcodes = {
        data: [],
        batchMode: false,
        saving: false,

        load: async function () {
            try {
                var list = await PanelAPI.giftcodes.list();
                this.render(list || []);
            } catch (e) {
                this.render([]);
                toast('Không tải được giftcode: ' + e.message, 'error');
            }
        },

        render: function (list) {
            this.data = list;
            giftcodeEmpty.hidden = list.length > 0;
            giftcodeBody.innerHTML = list.map(function (gc) {
                var expired = String(gc.expired || '').replace('T', ' ').split(' ');
                return '<tr>'
                    + '<td><span class="id-chip">' + gc.id + '</span></td>'
                    + '<td><span class="gc-code">' + escapeHtml(gc.code) + '</span></td>'
                    + '<td><span class="id-chip">' + gc.countLeft + ' lượt còn lại</span></td>'
                    + '<td><div class="chips">' + chips(gc.items) + '</div></td>'
                    + '<td class="expired-cell"><b>' + (expired[0] || '—') + '</b>' + (expired[1] || '') + '</td>'
                    + '<td><div class="row-actions">'
                    + '<button class="btn-mini" type="button" data-edit="' + gc.id + '">Sửa</button>'
                    + '<button class="btn-mini btn-mini--danger" type="button" data-remove="' + gc.id + '">Xóa</button>'
                    + '</div></td>'
                    + '</tr>';
            }).join('');
        },

        open: function (id, batch) {
            if (this.saving) return;
            var gc = id ? this.data.filter(function (g) { return g.id === id; })[0] : null;
            this.batchMode = !gc && batch === true;
            document.getElementById('giftcodeModalTitle').textContent = this.batchMode ? 'Tạo 50 giftcode dùng 1 lần' : gc ? 'Sửa Giftcode #' + gc.id : 'Tạo Giftcode Mới';
            document.getElementById('gcId').value = gc ? gc.id : '';
            var code = document.getElementById('gcCode');
            code.value = gc ? gc.code : '';
            code.required = !this.batchMode;
            code.maxLength = this.batchMode ? 32 : 128;
            code.placeholder = this.batchMode ? 'Tuỳ chọn, ví dụ: NROHAN' : 'Ví dụ: tanthu';
            document.getElementById('gcCodeLabel').textContent = this.batchMode ? 'TIỀN TỐ (TUỲ CHỌN)' : 'MÃ CODE';
            document.getElementById('gcBatchHint').hidden = !this.batchMode;
            document.getElementById('gcCount').value = this.batchMode ? 1 : gc ? gc.countLeft : 100;
            document.getElementById('gcCount').readOnly = this.batchMode;
            document.getElementById('btnSaveGiftcode').textContent = this.batchMode ? 'Tạo 50 mã' : 'Lưu Giftcode';
            document.getElementById('gcExpired').value = gc ? toInputTime(gc.expired) : '';

            var wrap = document.getElementById('gcItems');
            wrap.innerHTML = '';
            var items = gc && gc.items && gc.items.length ? gc.items : [{ id: '', quantity: 1 }];
            items.forEach(function (item) { addItemRow(item); });

            giftcodeModal.hidden = false;
            document.getElementById('gcCode').focus();
        },

        close: function () { if (!this.saving) giftcodeModal.hidden = true; },

        save: async function () {
            if (this.saving) return;
            var id = document.getElementById('gcId').value;
            var batch = this.batchMode;
            var expiredValue = document.getElementById('gcExpired').value;
            var data = {
                code: document.getElementById('gcCode').value.trim(),
                countLeft: parseInt(document.getElementById('gcCount').value, 10) || 0,
                items: collectItems()
            };

            if (!batch && !data.code) { toast('Chưa nhập mã code', 'error'); return; }
            if (batch && !/^[A-Za-z0-9_-]{0,32}$/.test(data.code)) { toast('Tiền tố chỉ gồm chữ không dấu, số, dấu - hoặc _', 'error'); return; }
            if (!data.items.length) { toast('Chưa có quà tặng nào', 'error'); return; }
            if (expiredValue) {
                data.expired = expiredValue.replace('T', ' ');
            }

            try {
                this.saving = true;
                document.getElementById('btnSaveGiftcode').disabled = true;
                var result;
                if (batch) {
                    var payload = { prefix: data.code, items: data.items };
                    if (data.expired) payload.expired = data.expired;
                    result = await PanelAPI.giftcodes.createBatch(payload);
                } else if (id) {
                    data.id = parseInt(id, 10);
                    await PanelAPI.giftcodes.update(data);
                } else {
                    if (!expiredValue) delete data.expired;
                    await PanelAPI.giftcodes.create(data);
                }
                this.saving = false;
                this.close();
                if (batch) {
                    document.getElementById('gcBatchCodes').value = result.codes.join('\n');
                    document.getElementById('giftcodeBatchResult').hidden = false;
                }
                toast(batch ? result.message : id ? 'Đã cập nhật giftcode' : 'Đã tạo giftcode mới', 'success');
                this.load();
            } catch (e) {
                toast(e.message, 'error');
            } finally {
                this.saving = false;
                document.getElementById('btnSaveGiftcode').disabled = false;
            }
        },

        remove: async function (id) {
            var gc = this.data.filter(function (g) { return g.id === id; })[0];
            if (!window.confirm('Xoá giftcode "' + (gc ? gc.code : id) + '"?')) return;
            try {
                await PanelAPI.giftcodes.remove(id);
                toast('Đã xoá giftcode', 'success');
                this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        sync: async function () {
            try {
                var res = await PanelAPI.giftcodes.sync();
                toast((res && res.message) || 'Đã đồng bộ máy chủ', 'success');
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /* --------------------------- helper giftcode --------------------------- */

    function typeClass(type) {
        return /vip|mtv/i.test(type || '') ? 'type-badge type-badge--vip' : 'type-badge';
    }

    function chips(items) {
        return (items || []).map(function (item) {
            // icon theo iconID: /game-data/icon/x1/<iconID>.png (đúng như data/icon/x1 của game)
            var icon = item.icon > 0
                ? '<img src="/game-data/icon/x1/' + item.icon + '.png" alt="" onerror="this.remove()">'
                : '';
            return '<span class="chip">' + icon + escapeHtml(item.name) + ' <b>x' + item.quantity + '</b></span>';
        }).join('');
    }

    function toInputTime(value) {
        var raw = String(value || '').replace('T', ' ').trim();
        if (!raw) return '';
        if (raw.length <= 10) return raw + 'T23:59';
        return raw.substring(0, 16).replace(' ', 'T');
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    /** Một dòng quà: ID, icon, tên, số lượng, nút mở phần option riêng của item đó. */
    function addItemRow(item) {
        item = item || {};

        var wrap = document.createElement('div');
        wrap.className = 'gc-item-wrap';
        wrap.innerHTML =
            '<div class="gc-item">'
            + '<input type="number" class="gc-item__id" placeholder="ID item" value="' + (item.id || '') + '">'
            + '<span class="gc-item__thumb"></span>'
            + '<span class="gc-item__name"></span>'
            + '<input type="number" class="gc-item__qty" placeholder="Số lượng" min="1" value="' + (item.quantity || 1) + '">'
            + '<button type="button" class="gc-item__toggle">Option <b>0</b></button>'
            + '<button class="gc-item__remove" type="button" title="Xoá">×</button>'
            + '</div>'
            + '<div class="gc-item__box" hidden>'
            + '<div class="gc-item__opts"></div>'
            + '<div class="gc-item__note" hidden></div>'
            + '<div class="gc-item__add">'
            + '<input type="number" class="gc-opt-id" placeholder="ID option">'
            + '<input type="number" class="gc-opt-param" placeholder="Param" value="0">'
            + '<button type="button" class="btn-mini" data-add-option>+ Thêm</button>'
            + '<button type="button" class="btn-mini" data-pick-option>Chọn từ bảng</button>'
            + '</div>'
            + '</div>';

        var mainRow = wrap.querySelector('.gc-item');
        var optionsBox = wrap.querySelector('.gc-item__opts');
        var optionsPanel = wrap.querySelector('.gc-item__box');
        var toggle = wrap.querySelector('.gc-item__toggle');

        // Gắn vào DOM trước để refreshOptionCounts() đếm được
        document.getElementById('gcItems').appendChild(wrap);

        // Option server tự áp khi món này không có option riêng
        var inherited = item.inherited || [];
        wrap.dataset.inherited = inherited.length;
        if (inherited.length) {
            wrap.querySelector('.gc-item__note').textContent =
                (item.inheritedType === 'default' ? 'Server mặc định áp: ' : 'Đang dùng option chung của code: ')
                + optionSummary(inherited);
        }

        setItemInfo(mainRow, item.name, item.icon);
        (item.option || []).forEach(function (option) { addOptionRow(optionsBox, option); });
        refreshOptionCounts();

        wrap.querySelector('.gc-item__remove').addEventListener('click', function () { wrap.remove(); });

        toggle.addEventListener('click', function () {
            optionsPanel.hidden = !optionsPanel.hidden;
            toggle.classList.toggle('is-open', !optionsPanel.hidden);
        });

        // Tự nhập ID option
        wrap.querySelector('[data-add-option]').addEventListener('click', function () {
            var idInput = wrap.querySelector('.gc-opt-id');
            var paramInput = wrap.querySelector('.gc-opt-param');
            if (idInput.value === '') { toast('Chưa nhập ID option', 'error'); return; }
            addOptionRow(optionsBox, { id: parseInt(idInput.value, 10), param: parseInt(paramInput.value, 10) || 0 });
            idInput.value = '';
            paramInput.value = 0;
            refreshOptionCounts();
        });

        // Hoặc chọn từ bảng option
        wrap.querySelector('[data-pick-option]').addEventListener('click', function () {
            openPicker('option', function (rows) {
                var existed = collectOptions(optionsBox).map(function (option) { return option.id; });
                rows.forEach(function (row) {
                    if (existed.indexOf(row.id) >= 0) {
                        return;
                    }
                    existed.push(row.id);
                    addOptionRow(optionsBox, { id: row.id, param: 0, name: row.name });
                });
                refreshOptionCounts();
            });
        });

        var idInput = wrap.querySelector('.gc-item__id');
        idInput.addEventListener('change', async function () {
            var id = parseInt(idInput.value, 10);
            if (!id) { setItemInfo(mainRow, '', 0); return; }
            try {
                var found = await PanelAPI.catalog.item(id);
                setItemInfo(mainRow, found.name, found.icon);
            } catch (e) {
                setItemInfo(mainRow, 'Không tìm thấy item', 0);
            }
        });
    }

    /** Hiện tên + icon của item trong dòng quà (icon lấy theo iconID). */
    function setItemInfo(row, name, icon) {
        row.querySelector('.gc-item__name').textContent = name || '';
        row.querySelector('.gc-item__thumb').innerHTML = icon > 0
            ? '<img src="/game-data/icon/x1/' + icon + '.png" alt="" onerror="this.remove()">'
            : '';
    }

    function collectItems() {
        var items = [];
        Array.prototype.forEach.call(document.querySelectorAll('#gcItems .gc-item-wrap'), function (wrap) {
            var id = parseInt(wrap.querySelector('.gc-item__id').value, 10);
            var quantity = parseInt(wrap.querySelector('.gc-item__qty').value, 10);
            if (!(id > 0) || !(quantity > 0)) return;

            var item = { id: id, quantity: quantity };
            var options = collectOptions(wrap.querySelector('.gc-item__opts'));
            if (options.length) item.option = options;
            items.push(item);
        });
        return items;
    }

    /* ----------------------------- option ----------------------------- */

    /** Thêm một dòng option vào container (option chung của code, hoặc option của 1 item). */
    function addOptionRow(container, option) {
        option = option || {};

        var row = document.createElement('div');
        row.className = 'gc-item gc-item--option';
        row.innerHTML = '<input type="number" class="gc-item__id" placeholder="ID option" value="' + (option.id == null ? '' : option.id) + '">'
            + '<span class="gc-item__name"></span>'
            + '<input type="number" class="gc-item__param" placeholder="Param" value="' + (option.param || 0) + '">'
            + '<button class="gc-item__remove" type="button" title="Xoá">×</button>';

        setOptionInfo(row, option.name);

        row.querySelector('.gc-item__remove').addEventListener('click', function () {
            row.remove();
            refreshOptionCounts();
        });

        var idInput = row.querySelector('.gc-item__id');
        idInput.addEventListener('change', async function () {
            if (idInput.value === '') { setOptionInfo(row, ''); return; }
            try {
                var found = await PanelAPI.catalog.option(idInput.value);
                setOptionInfo(row, found.name);
            } catch (e) {
                setOptionInfo(row, 'Không tìm thấy option');
            }
        });

        container.appendChild(row);
    }

    function setOptionInfo(row, name) {
        row.querySelector('.gc-item__name').textContent = name || '';
    }

    function collectOptions(container) {
        var options = [];
        Array.prototype.forEach.call(container.querySelectorAll('.gc-item--option'), function (row) {
            var raw = row.querySelector('.gc-item__id').value;
            if (raw === '') return;
            var id = parseInt(raw, 10);
            if (id >= 0) {
                options.push({ id: id, param: parseInt(row.querySelector('.gc-item__param').value, 10) || 0 });
            }
        });
        return options;
    }

    /**
     * Số option thực tế server sẽ áp cho từng món: option riêng đang khai báo,
     * còn không có thì tính cả option server tự áp (mặc định / option chung của code).
     */
    function refreshOptionCounts() {
        Array.prototype.forEach.call(document.querySelectorAll('#gcItems .gc-item-wrap'), function (wrap) {
            var own = wrap.querySelectorAll('.gc-item__opts .gc-item--option').length;
            var inherited = own > 0 ? 0 : Number(wrap.dataset.inherited || 0);
            wrap.querySelector('.gc-item__toggle b').textContent = own + inherited;

            var note = wrap.querySelector('.gc-item__note');
            if (note) note.hidden = inherited === 0;
        });
    }

    function optionSummary(options) {
        return options.map(function (option) {
            return '#' + option.id + ' ' + (option.name || '') + ' (param ' + option.param + ')';
        }).join(', ');
    }

    /* ------------------------ tìm & thêm hàng loạt ------------------------ */

    /**
     * Bảng chọn item / option trong modal riêng.
     * Tải cả danh sách một lần rồi lọc và vẽ dần ở client cho nhanh.
     */
    var pickerModal = document.getElementById('pickerModal');
    var pickerList = document.getElementById('pickerList');
    var pickerSearch = document.getElementById('pickerSearch');
    var pickerTotal = document.getElementById('pickerTotal');
    var pickerCount = document.getElementById('pickerCount');

    var CHUNK = 200;
    var pickerCache = { item: null, option: null };
    var picker = { kind: 'item', filtered: [], selected: {}, rendered: 0, onAdd: null };

    async function openPicker(kind, onAdd) {
        picker.kind = kind;
        picker.onAdd = onAdd;
        picker.selected = {};
        pickerSearch.value = '';
        pickerList.scrollTop = 0;
        document.getElementById('pickerTitle').textContent = kind === 'item' ? 'Chọn vật phẩm' : 'Chọn option';
        pickerModal.hidden = false;
        pickerSearch.focus();

        if (!pickerCache[kind]) {
            pickerList.innerHTML = '<div class="pick__empty">Đang tải danh sách...</div>';
            try {
                pickerCache[kind] = (kind === 'item'
                    ? await PanelAPI.catalog.searchItems('')
                    : await PanelAPI.catalog.searchOptions('')) || [];
            } catch (e) {
                pickerCache[kind] = [];
                toast('Không tải được danh sách: ' + e.message, 'error');
            }
        }
        applyPickerFilter();
    }

    function closePicker() {
        pickerModal.hidden = true;
    }

    function applyPickerFilter() {
        var needle = pickerSearch.value.trim().toLowerCase();
        var all = pickerCache[picker.kind] || [];
        picker.filtered = needle
            ? all.filter(function (row) { return row.name.toLowerCase().indexOf(needle) >= 0; })
            : all.slice();
        picker.rendered = 0;
        pickerList.innerHTML = '';
        pickerList.scrollTop = 0;
        appendPickerRows();
    }

    /** Vẽ thêm một đoạn kết quả (cuộn tới đáy sẽ tự vẽ tiếp). */
    function appendPickerRows() {
        if (!picker.filtered.length) {
            pickerList.innerHTML = '<div class="pick__empty">Không tìm thấy kết quả</div>';
            updatePickerFooter();
            return;
        }
        var slice = picker.filtered.slice(picker.rendered, picker.rendered + CHUNK);
        var html = slice.map(function (row) {
            var icon = picker.kind === 'item' && row.icon > 0
                ? '<img src="/game-data/icon/x1/' + row.icon + '.png" alt="" onerror="this.remove()">'
                : '';
            return '<label class="pick">'
                + '<input type="checkbox" data-id="' + row.id + '"' + (picker.selected[row.id] ? ' checked' : '') + '>'
                + icon + '<span>' + escapeHtml(row.name) + '</span><b>#' + row.id + '</b>'
                + '</label>';
        }).join('');

        if (picker.rendered === 0) {
            pickerList.innerHTML = html;
        } else {
            pickerList.insertAdjacentHTML('beforeend', html);
        }
        picker.rendered += slice.length;
        updatePickerFooter();
    }

    function updatePickerFooter() {
        pickerTotal.textContent = 'Hiện ' + picker.rendered + ' / ' + picker.filtered.length + ' kết quả';
        pickerCount.textContent = 'Đã chọn ' + Object.keys(picker.selected).length;
    }

    pickerSearch.addEventListener('input', applyPickerFilter);

    pickerList.addEventListener('scroll', function () {
        if (picker.rendered >= picker.filtered.length) return;
        if (pickerList.scrollTop + pickerList.clientHeight >= pickerList.scrollHeight - 150) {
            appendPickerRows();
        }
    });

    pickerList.addEventListener('change', function (e) {
        var box = e.target.closest('input[type="checkbox"]');
        if (!box) return;
        if (box.checked) {
            picker.selected[box.dataset.id] = true;
        } else {
            delete picker.selected[box.dataset.id];
        }
        updatePickerFooter();
    });

    document.getElementById('pickerToggleAll').addEventListener('click', function () {
        var all = Object.keys(picker.selected).length === picker.filtered.length;
        picker.selected = {};
        if (!all) {
            picker.filtered.forEach(function (row) { picker.selected[row.id] = true; });
        }
        Array.prototype.forEach.call(pickerList.querySelectorAll('input[type="checkbox"]'), function (box) {
            box.checked = !!picker.selected[box.dataset.id];
        });
        updatePickerFooter();
    });

    document.getElementById('pickerAdd').addEventListener('click', function () {
        var rows = picker.filtered.filter(function (row) { return picker.selected[row.id]; });
        if (!rows.length) {
            toast('Chưa chọn mục nào', 'error');
            return;
        }
        if (picker.onAdd) picker.onAdd(rows);
        closePicker();
    });

    pickerModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-pick-close]')) closePicker();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !pickerModal.hidden) closePicker();
    });

    document.getElementById('itemPickOpen').addEventListener('click', function () {
        openPicker('item', function (rows) {
            var existed = collectItems().map(function (item) { return item.id; });
            rows.forEach(function (row) {
                if (existed.indexOf(row.id) >= 0) {
                    return;
                }
                existed.push(row.id);
                addItemRow({ id: row.id, quantity: 1, name: row.name, icon: row.icon });
            });
        });
    });

    document.getElementById('btnCreateGiftcode').addEventListener('click', function () { giftcodes.open(null); });
    document.getElementById('btnCreateGiftcodeBatch').addEventListener('click', function () { giftcodes.open(null, true); });
    document.getElementById('giftcodeBatchResult').addEventListener('click', function (e) {
        if (e.target.closest('[data-batch-close]')) this.hidden = true;
    });
    document.getElementById('gcBatchCopy').addEventListener('click', async function () {
        var codes = document.getElementById('gcBatchCodes');
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) await navigator.clipboard.writeText(codes.value);
            else { codes.focus(); codes.select(); if (!document.execCommand('copy')) throw new Error(); }
            toast('Đã sao chép 50 mã', 'success');
        } catch (e) { codes.focus(); codes.select(); toast('Nhấn Ctrl+C để sao chép danh sách đã chọn', 'error'); }
    });
    document.getElementById('gcBatchDownload').addEventListener('click', function () {
        var url = URL.createObjectURL(new Blob([document.getElementById('gcBatchCodes').value + '\n'], { type: 'text/plain;charset=utf-8' }));
        var link = document.createElement('a');
        link.href = url; link.download = 'giftcode-50-ma.txt';
        document.body.appendChild(link); link.click(); link.remove();
        setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    });
    document.getElementById('btnSyncGiftcode').addEventListener('click', function () { giftcodes.sync(); });
    document.getElementById('btnAddGcItem').addEventListener('click', function () { addItemRow(); });

    giftcodeForm.addEventListener('submit', function (e) {
        e.preventDefault();
        giftcodes.save();
    });

    giftcodeModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-close]')) giftcodes.close();
    });

    giftcodeBody.addEventListener('click', function (e) {
        var edit = e.target.closest('[data-edit]');
        var remove = e.target.closest('[data-remove]');
        if (edit) giftcodes.open(parseInt(edit.dataset.edit, 10));
        if (remove) giftcodes.remove(parseInt(remove.dataset.remove, 10));
    });

    /* ============================ 4b. MÁY CHỦ ============================= */
    var SERVER_POLL_MS = 3000;

    /** Ghi text vào 1 ô theo id (không có thì bỏ qua). */
    function setText(id, value) {
        var el = document.getElementById(id);
        if (el) el.textContent = value == null ? '—' : String(value);
    }

    function formatPower(value) {
        return Number(value || 0).toLocaleString('vi-VN');
    }

    /**
     * Trang "Máy Chủ Trực Tuyến": đọc số liệu thật từ server rồi tự cập nhật 3 giây/lần.
     * Bảng danh sách chỉ được vẽ lại khi nội dung thay đổi (khỏi nhảy khi đang xem).
     */
    var servers = {
        data: null,
        timer: null,
        drawn: {},

        start: function () {
            clearInterval(this.timer);
            var self = this;
            this.load();
            this.timer = setInterval(function () { self.load(); }, SERVER_POLL_MS);
        },

        stop: function () {
            clearInterval(this.timer);
            this.timer = null;
        },

        load: async function () {
            try {
                this.data = await PanelAPI.server.info();
                this.render(this.data || {});
            } catch (e) {
                this.stop();
                toast('Không đọc được thông số máy chủ: ' + e.message, 'error');
            }
        },

        playerSearch: '',
        bossSearch: '',
        bots: null,

        render: function (d) {
            this.data = d;
            var boss = d.bosses || {};
            var botStats = d.botStats || {};
            var event = d.event || {};

            setText('stPlayers', d.players);
            setText('stRam', (d.ramUsedMb || 0) + '/' + (d.ramTotalMb || 0) + ' MB');
            setText('stCpu', (d.cpuPercent || 0) + '%');
            setText('stThreads', d.threads);
            setText('stSessions', d.sessions);

            setText('srvUptime', d.uptimeText);
            setText('srvExp', 'x' + d.expRate);
            setText('srvBoss', (boss.alive || 0) + ' alive / ' + (boss.dead || 0) + ' respawn / ' + (boss.resting || 0) + ' wait');
            setText('srvGiftcode', d.giftcodes);
            setText('srvConsign', d.consignItems);
            setText('srvAuto', d.autoMaintenance ? 'Bật (' + d.autoMaintenanceTime + ')' : 'Tắt');
            setText('srvUpdated', 'Cập nhật lúc ' + new Date().toLocaleTimeString('vi-VN'));

            // Thẻ thống kê bot
            setText('btTotal', botStats.total);
            setText('btClassic', botStats.classic);
            setText('btPlayer', botStats.player);
            setText('btEvent', botStats.event);
            setText('btAlive', botStats.alive);
            setText('btDead', botStats.dead);

            // Tab Thiết Lập Server: chỉ điền khi người dùng không đang sửa
            setInput('srvExpRate', d.expRate);
            setInput('srvMaintHour', hourOf(d.autoMaintenanceTime));
            setInput('srvMaintMinute', minuteOf(d.autoMaintenanceTime));
            setCheck('srvMaintEnable', !!d.autoMaintenance);
            setText('srvAutoSaveState', d.autoSave ? 'Đang bật' : 'Đang tắt');
            syncEvents(d.events || [], event.id || 0, event.name);
            syncSelect('srvCurrency', d.currencyInt ? '1' : '0');

            this.renderPlayers(d.playerList || []);
            this.renderBosses(d.bossList || []);
        },

        /** Bảng người chơi online, lọc theo tên ngay trên trình duyệt. */
        renderPlayers: function (all) {
            var needle = this.playerSearch.trim().toLowerCase();
            var list = needle
                ? all.filter(function (p) { return String(p.name).toLowerCase().indexOf(needle) >= 0; })
                : all;

            setText('srvPlayerCount', list.length + '/' + all.length + ' người');
            this.draw('players', 'srvPlayerBody', 'srvPlayerEmpty', list.length, function () {
                return list.map(function (p) {
                    return '<tr>'
                        + '<td><span class="id-chip">' + p.id + '</span></td>'
                        + '<td><b>' + escapeHtml(p.name) + '</b></td>'
                        + '<td class="num">' + formatPower(p.power) + '</td>'
                        + '<td>' + escapeHtml(p.mapName || '—') + (p.zoneId >= 0 ? ' (khu ' + p.zoneId + ')' : '') + '</td>'
                        + '<td>' + escapeHtml(p.task || '—') + '</td>'
                        + '<td><button class="btn-mini btn-mini--danger" type="button" data-kick="' + p.id + '">Kick</button></td>'
                        + '</tr>';
                }).join('');
            });
        },

        /** Bảng boss, lọc theo ô tìm kiếm ngay trên trình duyệt. */
        renderBosses: function (all) {
            var needle = this.bossSearch.trim().toLowerCase();
            var list = needle
                ? all.filter(function (b) { return String(b.name).toLowerCase().indexOf(needle) >= 0; })
                : all;

            setText('srvBossCount', list.length + '/' + all.length + ' boss');
            this.draw('boss', 'srvBossBody', 'srvBossEmpty', list.length, function () {
                return list.map(function (b) {
                    return '<tr>'
                        + '<td><div class="name-cell">' + bossAvatar(b)
                            + '<span><b>' + escapeHtml(b.name) + '</b><br><span class="id-chip">' + b.id + '</span></span></div></td>'
                        + '<td><span class="' + bossClass(b.status) + '">' + escapeHtml(b.status) + '</span></td>'
                        + '<td>' + escapeHtml(b.mapName || '—') + (b.zoneId >= 0 ? ' (khu ' + b.zoneId + ')' : '') + '</td>'
                        + '<td class="num">' + formatPower(b.hpMax) + ' / ' + formatPower(b.dame) + '</td>'
                        + '<td><button class="btn-mini btn-mini--dark" type="button" data-summon="' + b.id + '">Triệu Hồi</button></td>'
                        + '</tr>';
                }).join('');
            });
        },

        /** Bảng bot được tải theo yêu cầu (server có thể có hàng nghìn bot). */
        loadBots: async function (reset) {
            if (reset) {
                this.bots = null;
            }
            var filters = {
                group: valueOf_('botFilterGroup'),
                type: valueOf_('botFilterType'),
                q: valueOf_('botFilterSearch')
            };
            setText('srvBotCount', 'Đang tải...');
            try {
                this.bots = await PanelAPI.server.bots(filters);
            } catch (e) {
                this.bots = null;
                setText('srvBotCount', 'Không tải được danh sách bot');
                toast('Không tải được bot: ' + e.message, 'error');
                return;
            }
            this.renderBots();
        },

        renderBots: function () {
            var data = this.bots;
            if (!data) {
                document.getElementById('srvBotBody').innerHTML = '';
                document.getElementById('srvBotEmpty').hidden = true;
                setText('srvBotCount', 'Chọn tab Bot để tải dữ liệu.');
                this.drawn.bot = '';
                return;
            }
            var rows = data.rows || [];
            setText('srvBotCount', 'Hiện ' + data.shown + '/' + data.total + ' bot (tối đa ' + data.limit + ' dòng)');
            this.draw('bot', 'srvBotBody', 'srvBotEmpty', rows.length, function () {
                return rows.map(function (b) {
                    return '<tr>'
                        + '<td>' + escapeHtml(b.groupName) + '<br><small class="muted">loại ' + b.type + '</small></td>'
                        + '<td><span class="id-chip">' + b.id + '</span> ' + escapeHtml(b.name) + '</td>'
                        + '<td class="num">' + formatPower(b.power) + '</td>'
                        + '<td class="num">' + formatPower(b.hp) + ' / ' + formatPower(b.mp) + '</td>'
                        + '<td>' + escapeHtml(b.mapName || '—') + (b.zoneId >= 0 ? ' (khu ' + b.zoneId + ')' : '') + '</td>'
                        + '<td class="num">' + b.x + ' : ' + b.y + '</td>'
                        + '<td><span class="' + (b.dead ? 'type-badge type-badge--dead' : 'type-badge type-badge--live') + '">'
                            + (b.dead ? 'Đã chết' : 'Còn sống') + '</span></td>'
                        + '<td><button class="btn-mini btn-mini--danger" type="button" data-bot-del="' + b.id
                            + '" data-bot-group="' + b.group + '">Xóa</button></td>'
                        + '</tr>';
                }).join('');
            });
        },

        /** Vẽ bảng nếu nội dung khác lần trước. */
        draw: function (key, bodyId, emptyId, count, buildHtml) {
            var html = count ? buildHtml() : '';
            if (this.drawn[key] === html) return;
            this.drawn[key] = html;
            document.getElementById(bodyId).innerHTML = html;
            document.getElementById(emptyId).hidden = count > 0;
        },

        /** Chạy tác vụ thật trên server rồi đọc lại số liệu. */
        task: async function (action, body, confirmText) {
            if (confirmText && !window.confirm(confirmText)) return;
            try {
                var res = await PanelAPI.server.run(action, body);
                toast((res && res.message) || 'Đã thực hiện xong', 'success');
                if (action === 'set-event') {
                    document.getElementById('srvEventSelect').dataset.dirty = '';
                }
                if (action === 'currency') {
                    document.getElementById('srvCurrency').dataset.dirty = '';
                }
                this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        removeBot: async function (group, id) {
            if (!window.confirm('Xóa bot #' + id + ' khỏi máy chủ?')) return;
            try {
                var res = await PanelAPI.server.run('bot-delete', { id: id, group: group });
                toast((res && res.message) || 'Đã xóa bot', 'success');
            } catch (e) {
                toast(e.message, 'error');
            }
            this.loadBots(true);
        },

        createBots: async function () {
            var type = parseInt(valueOf_('botCreateType'), 10);
            var quantity = parseInt(valueOf_('botCreateQty'), 10);
            if (!(quantity >= 1) || quantity > 500) {
                toast('Số lượng bot phải trong khoảng 1 - 500', 'error');
                return;
            }
            await this.task('bot-create', { type: type, quantity: quantity },
                'Tạo ' + quantity + ' bot vào máy chủ đang chạy?');
            this.loadBots(true);
        },

        deleteBots: async function (filter, extra) {
            var body = filter || {
                group: valueOf_('botFilterGroup'),
                type: valueOf_('botFilterType'),
                keyword: valueOf_('botFilterSearch')
            };
            if (extra) {
                Object.keys(extra).forEach(function (key) { body[key] = extra[key]; });
            }
            if (!body.group && !body.keyword && (body.type === undefined || body.type === '')) {
                toast('Hãy chọn nhóm, loại hoặc nhập từ khóa trước khi xóa', 'error');
                return;
            }
            if (!window.confirm('Xóa bot theo bộ lọc đang chọn? Hành động này không thể hoàn tác.')) return;
            try {
                var res = await PanelAPI.server.run('bot-delete', body);
                toast((res && res.message) || 'Đã xóa bot', 'success');
            } catch (e) {
                toast(e.message, 'error');
            }
            this.loadBots(true);
        }
    };

    /* ============================ 4c. DOANH THU ========================== */
    var REVENUE_POLL_MS = 15000;

    /**
     * Trang "Doanh Thu & CSDL": đọc số liệu tổng hợp từ database, vẽ biểu đồ doanh thu 7 ngày
     * và cho phép cộng/trừ VND, mở/khoá tài khoản, chặn IP clone.
     */
    var revenue = {
        data: null,
        timer: null,
        chartDrawn: '',

        start: function () {
            clearInterval(this.timer);
            var self = this;
            this.load();
            this.timer = setInterval(function () { self.load(); }, REVENUE_POLL_MS);
        },

        stop: function () {
            clearInterval(this.timer);
            this.timer = null;
        },

        load: async function () {
            try {
                this.data = await PanelAPI.revenue.info();
                this.render(this.data || {});
            } catch (e) {
                this.stop();
                toast('Không đọc được số liệu doanh thu: ' + e.message, 'error');
            }
        },

        render: function (d) {
            this.data = d;
            var accounts = d.accounts || {};
            var money = d.revenue || {};

            setText('rvAccounts', formatPower(accounts.total));
            setText('rvRegisterToday', formatPower(accounts.today));
            setText('rvRegisterSub', 'Tài khoản tạo mới trong ngày · ' + formatPower(accounts.week) + ' trong 7 ngày');
            setText('rvRevenueToday', money.today == null ? 'Chưa có dữ liệu' : formatPower(money.today) + ' VNĐ');
            setText('rvRevenueWeek', money.week == null ? 'Chưa có dữ liệu' : formatPower(money.week) + ' VNĐ');
            setText('rvRevenueTotal', formatPower(money.total) + ' VNĐ');
            setText('rvRevenueTotalSub', accounts.topupAccounts === undefined
                ? 'Tổng nạp tích luỹ của mọi tài khoản'
                : 'Tổng nạp tích luỹ · ' + formatPower(accounts.topupAccounts) + '/' + formatPower(accounts.total)
                    + ' tài khoản đã nạp');
            setText('rvMtv', formatPower(accounts.mtv));
            setText('rvLocked', formatPower(accounts.locked));
            setText('rvLockedSub', 'Tài khoản đang bị tạm ngưng');
            setText('rvAdmins', formatPower(accounts.adminTotal));
            setText('rvAdminsSub', 'Tài khoản admin');
            setText('rvWeekChip', money.weekLabel || 'Thứ 2 - Chủ nhật');
            setText('rvChartHint', 'Cập nhật lúc ' + new Date().toLocaleTimeString('vi-VN')
                + ' · Tổng nạp tích luỹ ' + formatPower(money.total) + ' VNĐ');

            this.renderChart(d.chart || [], d.recharge || {}, accounts.error || '');
            this.renderTopups(d.topupToday || {});
            this.renderClones(d.cloneLimit || 5);
        },

        renderChart: function (bars, recharge, accountsError) {
            var max = 0;
            bars.forEach(function (bar) {
                if (bar.revenue > max) max = bar.revenue;
            });

            var html = bars.map(function (bar) {
                var available = typeof bar.revenue === 'number' && Number.isFinite(bar.revenue);
                var height = available && max > 0 ? Math.round(bar.revenue / max * 100) : 0;
                return '<div class="chart__col' + (available && bar.revenue > 0 ? '' : ' chart__col--empty')
                    + '" title="' + escapeHtml(bar.date) + ': ' + (available ? formatPower(bar.revenue) + ' VNĐ' : 'Chưa có dữ liệu doanh thu') + ' · '
                    + bar.accounts + ' tài khoản đăng ký mới">'
                    + '<span class="chart__value">' + (available ? formatPower(bar.revenue) : '—') + '</span>'
                    + '<div class="chart__track"><div class="chart__fill" style="height: ' + height + '%"></div></div>'
                    + '<span class="chart__date">' + bar.date + '</span>'
                    + '<span class="chart__accounts">' + bar.accounts + ' Tk mới</span>'
                    + '</div>';
            }).join('');

            // Chỉ vẽ lại khi số liệu đổi (tránh nhấp nháy do poll)
            if (this.chartDrawn !== html) {
                this.chartDrawn = html;
                document.getElementById('rvChart').innerHTML = html;
            }

            // Dòng "tình trạng dữ liệu": nói rõ vì sao số liệu có thể đang là 0
            var notes = [];
            if (accountsError) {
                notes.push('Không đọc được số liệu bảng account: ' + accountsError);
            }
            if (!recharge.ready) {
                notes.push(recharge.reason || 'Chưa cấu hình nguồn giao dịch có trạng thái thành công xác thực; doanh thu theo ngày chưa có dữ liệu.');
            } else if (recharge.reason) {
                notes.push(recharge.reason);
            }
            var note = document.getElementById('rvRechargeNote');
            note.textContent = notes.join(' · ');
            note.hidden = !notes.length;
        },

        renderTopups: function (topup) {
            var rows = topup.rows || [];
            setText('rvTopupCount', topup.available === false ? 'Chưa có dữ liệu' : formatPower(topup.count || 0) + ' giao dịch');
            document.getElementById('rvTopupEmpty').textContent = topup.available === false
                ? (topup.reason || 'Chưa cấu hình nguồn lịch sử nạp được xác thực.') : 'Không có giao dịch nạp hôm nay';
            document.getElementById('rvTopupBody').innerHTML = rows.map(function (row, index) {
                return '<tr>'
                    + '<td>' + (index + 1) + '</td>'
                    + '<td><b>' + escapeHtml(row.username) + '</b></td>'
                    + '<td>' + escapeHtml(row.source) + '</td>'
                    + '<td class="num">' + formatPower(row.amount) + '</td>'
                    + '</tr>';
            }).join('');
            document.getElementById('rvTopupEmpty').hidden = rows.length > 0;
        },

        renderClones: function (limit) {
            setText('rvCloneBan', 'Khóa Toàn Bộ Clone (>' + limit + ' Acc/IP)');
        },

        /** Cộng / trừ VND trong ví tài khoản. */
        money: async function (add) {
            var username = valueOf_('rvMoneyUser');
            var amount = parseInt(valueOf_('rvMoneyAmount'), 10);
            if (!username) { toast('Chưa nhập username người chơi', 'error'); return; }
            if (!(amount > 0)) { toast('Số tiền phải là số dương', 'error'); return; }
            var confirmText = (add ? 'Cộng ' : 'Trừ ') + formatPower(amount)
                + ' Cash trong số dư game của tài khoản "' + username + '"?';
            await this.run(add ? 'money-add' : 'money-sub', { username: username, amount: amount }, confirmText);
        },

        /** Mở/huỷ thành viên, khoá/mở khoá tài khoản (nút gửi key ngắn: mtv-on, mtv-off, lock, unlock). */
        account: async function (key) {
            var username = valueOf_('rvStateUser');
            if (!username) { toast('Chưa nhập username người chơi', 'error'); return; }
            var action = 'account-' + key;
            var confirmText = {
                'account-mtv-on': 'Mở quyền thành viên (MTV) cho tài khoản "' + username + '"?',
                'account-mtv-off': 'Huỷ quyền thành viên (MTV) của tài khoản "' + username + '"?',
                'account-lock': 'Khoá tài khoản "' + username + '"? Người chơi sẽ bị ngắt kết nối.',
                'account-unlock': 'Mở khoá tài khoản "' + username + '"?',
                'account-admin-on': 'Cấp quyền game admin (is_admin = 1) cho tài khoản "' + username + '"? Không cấp quyền đăng nhập panel.',
                'account-admin-off': 'Thu hồi quyền game admin (is_admin = 0) của tài khoản "' + username + '"?'
            }[action] || ('Thực hiện tác vụ ' + action + ' cho tài khoản "' + username + '"?');
            await this.run(action, { username: username }, confirmText);
        },

        /** Khoá / mở khoá mọi tài khoản của một địa chỉ IP. */
        ip: async function (ban) {
            var ip = valueOf_('rvIp');
            if (!ip) { toast('Chưa nhập địa chỉ IP', 'error'); return; }
            var confirmText = (ban ? 'Khoá toàn bộ tài khoản dùng IP ' : 'Mở khoá toàn bộ tài khoản dùng IP ') + ip + '?';
            await this.run(ban ? 'ip-ban' : 'ip-unban', { ip: ip }, confirmText);
        },

        /** Khoá mọi tài khoản thuộc các IP có nhiều tài khoản hơn ngưỡng (chặn clone). */
        cloneBan: async function () {
            var limit = (this.data && this.data.cloneLimit) || 5;
            await this.run('clone-ban', { limit: limit },
                'Khoá TOÀN BỘ tài khoản thuộc các IP có hơn ' + limit + ' tài khoản? Hành động này không thể hoàn tác.');
        },

        /** Chạy tác vụ rồi đọc lại số liệu. */
        run: async function (action, body, confirmText) {
            if (confirmText && !window.confirm(confirmText)) return;
            try {
                var res = await PanelAPI.revenue.run(action, body);
                toast((res && res.message) || 'Đã thực hiện xong', 'success');
                this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    document.getElementById('view-revenue').addEventListener('click', function (e) {
        var money = e.target.closest('[data-money]');
        if (money) {
            revenue.money(money.dataset.money === 'add');
            return;
        }

        var account = e.target.closest('[data-account]');
        if (account) {
            revenue.account(account.dataset.account);
            return;
        }

        var ip = e.target.closest('[data-ip]');
        if (ip) {
            revenue.ip(ip.dataset.ip === 'ban');
            return;
        }

        if (e.target.closest('#rvCloneBan')) {
            revenue.cloneBan();
        }
    });

    /* ========================= 4d. CỬA HÀNG SHOP NPC ===================== */

    /** Chọn theo id trong mảng [{id, name}] của API shop. */
    function shopFind(list, id) {
        var found = null;
        (list || []).forEach(function (row) { if (row.id === id) found = row; });
        return found;
    }

    /** Đổ dữ liệu vào &lt;select&gt; (chỉ 1 lần, trừ khi ép lại). */
    function fillSelect(id, rows, force) {
        var el = document.getElementById(id);
        if (!el || (el.dataset.filled && !force)) return;
        el.innerHTML = rows.map(function (row) {
            return '<option value="' + row.value + '">' + escapeHtml(row.label) + '</option>';
        }).join('');
        el.dataset.filled = '1';
    }

    function iconImg(icon) {
        return icon > 0
            ? '<img src="/game-data/icon/x1/' + icon + '.png" alt="" onerror="this.remove()">'
            : '';
    }

    /**
     * Trang "Cửa Hàng Shop NPC": cột trái là danh sách shop theo NPC, cột phải là
     * tab của shop đang chọn và bảng vật phẩm bán trong tab đó.
     */
    var shop = {
        index: null,
        detail: null,
        shopId: 0,
        tabId: 0,
        active: false,

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
        },

        /* ----------------------------- tải dữ liệu ---------------------------- */

        load: async function () {
            try {
                this.index = (await PanelAPI.shop.list()) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách shop: ' + e.message, 'error');
                return;
            }

            fillSelect('shopNpc', (this.index.npcs || []).map(function (npc) {
                return { value: npc.id, label: 'NPC ' + npc.id + ' — ' + npc.name };
            }));
            fillSelect('shopType', (this.index.typeShops || []).map(function (type) {
                return { value: type.id, label: type.name };
            }));
            fillSelect('shopItemTypeSell', (this.index.typeSells || []).map(function (type) {
                return { value: type.id, label: type.name };
            }));

            var shops = this.index.shops || [];
            var keep = shopFind(shops, this.shopId) || shops[0];
            if (keep) {
                await this.select(keep.id);
            } else {
                this.shopId = 0;
                this.detail = null;
                this.tabId = 0;
                this.renderList();
                this.renderTabs();
                this.renderItems();
            }
        },

        select: async function (id) {
            this.shopId = id;
            try {
                var res = await PanelAPI.shop.detail(id);
                this.detail = (res && res.shop) || null;
            } catch (e) {
                this.detail = null;
                toast('Không đọc được shop #' + id + ': ' + e.message, 'error');
            }

            var tabs = (this.detail && this.detail.tabs) || [];
            var keep = shopFind(tabs, this.tabId) || tabs[0];
            this.tabId = keep ? keep.id : 0;

            this.renderList();
            this.renderTabs();
            this.renderItems();
        },

        findShop: function (id) {
            return shopFind(this.index && this.index.shops, id);
        },

        currentTab: function () {
            return shopFind(this.detail && this.detail.tabs, this.tabId);
        },

        currentItems: function () {
            var tab = this.currentTab();
            return (tab && tab.items) || [];
        },

        /* -------------------------------- vẽ -------------------------------- */

        renderList: function () {
            var list = (this.index && this.index.shops) || [];
            document.getElementById('shopListEmpty').hidden = list.length > 0;
            document.getElementById('shopList').innerHTML = list.map(function (s) {
                return '<div class="shop-item' + (s.id === shop.shopId ? ' is-active' : '')
                    + '" data-shop-select="' + s.id + '">'
                    + '<span class="shop-item__info">'
                    + '<b class="shop-item__npc">NPC ID: ' + s.npcId + '</b>'
                    + '<span class="shop-item__tag">Tag: ' + escapeHtml(s.tagName || '—') + '</span>'
                    + '<span class="shop-item__meta">#' + s.id + ' · ' + escapeHtml(s.npcName)
                        + ' · ' + s.tabCount + ' tab · ' + s.itemCount + ' món</span>'
                    + '</span>'
                    + '<span class="shop-item__acts">'
                    + '<button class="btn-mini" type="button" data-shop-edit="' + s.id + '">Sửa</button>'
                    + '<button class="btn-mini btn-mini--danger" type="button" data-shop-del="' + s.id + '">Xóa</button>'
                    + '</span></div>';
            }).join('');
        },

        renderTabs: function () {
            var box = document.getElementById('shopTabs');
            var tabs = (this.detail && this.detail.tabs) || [];
            if (!tabs.length) {
                box.innerHTML = '<span class="muted">Shop này chưa có tab nào</span>';
                return;
            }
            box.innerHTML = tabs.map(function (tab) {
                var label = String(tab.name || '').replace(/[\r\n]+/g, ' ').trim() || ('Tab #' + tab.id);
                return '<button class="tab' + (tab.id === shop.tabId ? ' is-active' : '') + '" type="button"'
                    + ' data-shop-tab="' + tab.id + '" title="' + escapeHtml(label)
                    + ' (ID: ' + tab.id + ' · ' + tab.itemCount + ' món) — bấm đúp để đổi tên">'
                    + escapeHtml(label) + ' (ID:' + tab.id + ')</button>';
            }).join('');

            Array.prototype.forEach.call(box.querySelectorAll('[data-shop-tab]'), function (btn) {
                btn.addEventListener('dblclick', function () {
                    shop.openTab(parseInt(btn.dataset.shopTab, 10));
                });
            });
        },

        renderItems: function () {
            var items = this.currentItems();
            document.getElementById('shopItemEmpty').hidden = items.length > 0;
            document.getElementById('shopItemBody').innerHTML = items.map(shopItemRow).join('');

            // Nói rõ cách máy chủ tính giá, vì mỗi loại shop dùng cột Giá bán khác nhau
            var hint = document.getElementById('shopTypeHint');
            var type = this.detail ? this.detail.typeShop : -1;
            var notes = {
                1: 'Shop học kỹ năng: máy chủ tự tính giá theo tiềm năng của kỹ năng nên cột Giá bán chỉ là dữ liệu lưu trong DB.',
                2: 'Shop điểm: người chơi trả bằng điểm tích luỹ, số điểm lấy ở cột Giá bán.',
                3: 'Shop đặc biệt: giá = Giá bán × vật phẩm có icon ở cột Icon bổ sung.'
            };
            hint.textContent = notes[type] || '';
            hint.hidden = !notes[type];
        },

        /* ------------------------------ shop NPC ----------------------------- */

        openShop: function (id) {
            var edit = id ? this.findShop(id) : null;
            document.getElementById('shopModalTitle').textContent =
                edit ? ('Sửa Shop #' + edit.id + ' — ' + edit.npcName) : 'Thêm Shop NPC';
            document.getElementById('shopId').value = edit ? edit.id : '';
            setSelect('shopNpc', String(edit ? edit.npcId : (this.index && this.index.npcs && this.index.npcs[0]
                ? this.index.npcs[0].id : '')));
            document.getElementById('shopTag').value = edit ? (edit.tagName || '') : '';
            setSelect('shopType', String(edit ? edit.typeShop : 0));
            document.getElementById('shopModal').hidden = false;
            document.getElementById('shopTag').focus();
        },

        closeShop: function () {
            document.getElementById('shopModal').hidden = true;
        },

        saveShop: async function () {
            var id = parseInt(valueOf_('shopId'), 10) || 0;
            var npcId = parseInt(valueOf_('shopNpc'), 10);
            var body = {
                npcId: npcId,
                tagName: valueOf_('shopTag'),
                typeShop: parseInt(valueOf_('shopType'), 10) || 0
            };
            // NPC 0 (Ông Gôhan) vẫn là NPC hợp lệ nên chỉ chặn khi chưa chọn được gì
            if (isNaN(npcId) || npcId < 0) { toast('Chưa chọn NPC cho shop', 'error'); return; }
            if (!body.tagName) { toast('Nhập tag name cho shop', 'error'); return; }

            try {
                var res = id
                    ? await PanelAPI.shop.run('shop-update', Object.assign({ id: id }, body))
                    : await PanelAPI.shop.run('shop-create', body);
                toast(res.message || 'Đã lưu shop', 'success');
                this.closeShop();
                if (!id) this.shopId = (res && res.id) || 0;
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        deleteShop: async function (id) {
            var target = this.findShop(id);
            var label = target ? ('shop #' + id + ' của NPC ' + target.npcName + ' (tag ' + target.tagName + ')') : ('shop #' + id);
            if (!window.confirm('Xoá ' + label + '? Toàn bộ tab và vật phẩm bên trong cũng bị xoá.')) return;
            try {
                var res = await PanelAPI.shop.run('shop-delete', { id: id });
                toast(res.message || 'Đã xoá shop', 'success');
                if (this.shopId === id) this.shopId = 0;
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        /* -------------------------------- tab -------------------------------- */

        openTab: function (id) {
            var edit = id ? shopFind(this.detail && this.detail.tabs, id) : null;
            if (!edit && !this.detail) {
                toast('Hãy chọn một shop trước', 'error');
                return;
            }
            document.getElementById('shopTabModalTitle').textContent = edit ? ('Đổi Tên Tab #' + edit.id) : 'Thêm Tab';
            document.getElementById('shopTabId').value = edit ? edit.id : '';
            document.getElementById('shopTabName').value = edit ? (edit.name || '') : '';
            document.getElementById('shopTabModal').hidden = false;
            document.getElementById('shopTabName').focus();
        },

        closeTab: function () {
            document.getElementById('shopTabModal').hidden = true;
        },

        saveTab: async function () {
            var id = parseInt(valueOf_('shopTabId'), 10) || 0;
            var name = document.getElementById('shopTabName').value.trim();
            if (!name) { toast('Nhập tên tab', 'error'); return; }
            try {
                var res = id
                    ? await PanelAPI.shop.run('tab-update', { id: id, name: name })
                    : await PanelAPI.shop.run('tab-create', { shopId: this.shopId, name: name });
                toast(res.message || 'Đã lưu tab', 'success');
                this.closeTab();
                if (!id) this.tabId = (res && res.id) || 0;
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        deleteTab: async function () {
            var tab = this.currentTab();
            if (!tab) { toast('Shop này chưa có tab nào để xoá', 'error'); return; }
            var label = String(tab.name || '').replace(/[\r\n]+/g, ' ').trim() || ('Tab #' + tab.id);
            if (!window.confirm('Xoá tab "' + label + '" cùng ' + tab.itemCount + ' vật phẩm bên trong?')) return;
            try {
                var res = await PanelAPI.shop.run('tab-delete', { id: tab.id });
                toast(res.message || 'Đã xoá tab', 'success');
                this.tabId = 0;
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        showTab: function (id) {
            this.tabId = id;
            this.renderTabs();
            this.renderItems();
        },

        /* ----------------------------- vật phẩm ------------------------------ */

        openItem: function (id) {
            var tab = this.currentTab();
            if (!tab) { toast('Hãy chọn một tab trước khi thêm vật phẩm', 'error'); return; }

            var item = null;
            if (id) {
                this.currentItems().forEach(function (row) { if (row.id === id) item = row; });
            }

            document.getElementById('shopItemModalTitle').textContent =
                item ? ('Sửa Vật Phẩm #' + item.id) : ('Thêm Vật Phẩm Vào Tab #' + tab.id);
            document.getElementById('shopItemId').value = item ? item.id : '';
            document.getElementById('shopItemTabId').value = tab.id;
            document.getElementById('shopItemVersion').value = tab.itemsVersion || '';
            document.getElementById('shopItemTemp').value = item ? item.tempId : '';
            shopItemPreview(item ? item.name : '', item ? item.icon : 0);
            document.getElementById('shopItemCost').value = item ? item.cost : 0;
            var currency = this.detail && this.detail.fixedCurrency;
            fillSelect('shopItemTypeSell', currency ? [{ value: 4, label: currency.name }]
                : (this.index.typeSells || []).map(function (type) { return { value: type.id, label: type.name }; }), true);
            setSelect('shopItemTypeSell', String(currency ? 4 : item ? item.typeSell : 1));
            document.getElementById('shopItemTypeSell').disabled = !!currency;
            document.getElementById('shopItemIconSpec').value = currency ? currency.icon : item ? item.iconSpec : 0;
            document.getElementById('shopItemIconSpec').disabled = !!currency;
            document.getElementById('shopItemSpecPick').disabled = !!currency;
            document.querySelector('label[for="shopItemCost"]').textContent = currency ? 'GIÁ BÁN (SỐ ĐÁ NGŨ SẮC)' : 'GIÁ BÁN';
            shopSpecPreview(currency ? currency.icon : item ? item.iconSpec : 0, currency ? currency.name : item ? item.specName : '');
            document.getElementById('shopItemNew').checked = !!(item && item.isNew);
            document.getElementById('shopItemSell').checked = item ? !!item.isSell : true;

            var box = document.getElementById('shopItemOptions');
            box.innerHTML = '';
            ((item && item.options) || []).forEach(function (option) { addOptionRow(box, option); });

            document.getElementById('shopItemModal').hidden = false;
            document.getElementById('shopItemTemp').focus();
        },

        closeItem: function () {
            document.getElementById('shopItemModal').hidden = true;
        },

        saveItem: async function () {
            var id = parseInt(valueOf_('shopItemId'), 10) || 0;
            var tabId = parseInt(valueOf_('shopItemTabId'), 10) || 0;
            var tempId = parseInt(valueOf_('shopItemTemp'), 10);
            if (!Number.isInteger(tempId) || tempId < 0) { toast('Nhập mã template (ID vật phẩm, kể cả 0)', 'error'); return; }

            var body = {
                tempId: tempId,
                cost: parseInt(valueOf_('shopItemCost'), 10) || 0,
                tabId: tabId,
                itemsVersion: valueOf_('shopItemVersion'),
                typeSell: parseInt(valueOf_('shopItemTypeSell'), 10) || 0,
                iconSpec: parseInt(valueOf_('shopItemIconSpec'), 10) || 0,
                isNew: document.getElementById('shopItemNew').checked,
                isSell: document.getElementById('shopItemSell').checked,
                options: collectOptions(document.getElementById('shopItemOptions'))
            };

            try {
                var res = id
                    ? await PanelAPI.shop.run('item-update', Object.assign({ id: id }, body))
                    : await PanelAPI.shop.run('item-create', Object.assign({ tabId: tabId }, body));
                toast(res.message || 'Đã lưu vật phẩm', 'success');
                this.closeItem();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        removeItem: async function (id) {
            var target = null;
            this.currentItems().forEach(function (row) { if (row.id === id) target = row; });
            var label = target ? ('"' + target.name + '" (item #' + target.id + ')') : ('vật phẩm #' + id);
            if (!window.confirm('Xoá ' + label + ' khỏi shop?')) return;
            try {
                var tab = this.currentTab();
                if (!tab) throw new Error('Hãy tải lại tab shop trước khi xoá.');
                var res = await PanelAPI.shop.run('item-delete', { id: id, tabId: tab.id, itemsVersion: tab.itemsVersion });
                toast(res.message || 'Đã xoá vật phẩm', 'success');
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        /* ------------------------------- đồng bộ ----------------------------- */

        sync: async function () {
            try {
                var res = await PanelAPI.shop.run('sync');
                toast(res.message || 'Đã đồng bộ shop vào máy chủ', 'success');
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /** Một dòng vật phẩm trong bảng shop. */
    function shopItemRow(item) {
        var unit = item.unitName || item.typeSellName || '';
        var price = 'Giá: ' + formatPower(item.cost) + (unit ? ' ' + escapeHtml(unit) : '');

        var spec = item.iconSpec > 0
            ? '<span class="shop-cell">' + iconImg(item.iconSpec) + '<span>' + item.iconSpec + '</span></span>'
            : '<span class="muted">—</span>';

        var options = (item.options && item.options.length)
            ? '<div class="shop-options">' + item.options.map(function (option) {
                return '<span title="Option #' + option.id + ' · param ' + option.param + '">'
                    + escapeHtml(option.text) + '</span>';
            }).join('') + '</div>'
            : '<span class="muted">Không có</span>';

        return '<tr>'
            + '<td><span class="id-chip shop-id">' + item.tempId + '</span></td>'
            + '<td><div class="shop-cell">' + iconImg(item.icon)
                + '<span><span class="shop-cell__name">' + escapeHtml(item.name) + '</span>'
                + '<span class="shop-cell__sub">item #' + item.id + (item.isNew ? ' · MỚI' : '') + '</span></span></div></td>'
            + '<td>' + price + '</td>'
            + '<td>' + escapeHtml(item.typeSellName) + '</td>'
            + '<td>' + (item.isSell
                ? '<span class="type-badge">Đang Bán</span>'
                : '<span class="type-badge type-badge--off">Ngừng Bán</span>') + '</td>'
            + '<td>' + spec + '</td>'
            + '<td>' + options + '</td>'
            + '<td><span class="row-actions">'
                + '<button class="btn-mini" type="button" data-item-edit="' + item.id + '">Sửa</button>'
                + '<button class="btn-mini btn-mini--danger" type="button" data-item-del="' + item.id + '">Xóa</button>'
            + '</span></td>'
            + '</tr>';
    }

    function shopItemPreview(name, icon) {
        document.getElementById('shopItemTempIcon').innerHTML = iconImg(icon);
        document.getElementById('shopItemTempName').textContent = name || 'Không tìm thấy vật phẩm này';
    }

    function shopSpecPreview(iconSpec, name) {
        document.getElementById('shopItemSpecIcon').innerHTML = iconImg(iconSpec);
        document.getElementById('shopItemSpecName').textContent =
            iconSpec > 0 ? (name || ('Icon #' + iconSpec)) : 'Không dùng icon bổ sung';
    }

    /* ---- Sự kiện trang shop ---- */

    var shopModal = document.getElementById('shopModal');
    var shopTabModal = document.getElementById('shopTabModal');
    var shopItemModal = document.getElementById('shopItemModal');

    document.getElementById('shopAdd').addEventListener('click', function () { shop.openShop(null); });
    document.getElementById('shopTabAdd').addEventListener('click', function () { shop.openTab(null); });
    document.getElementById('shopTabDelete').addEventListener('click', function () { shop.deleteTab(); });
    document.getElementById('shopItemAdd').addEventListener('click', function () { shop.openItem(null); });
    document.getElementById('shopSync').addEventListener('click', function () { shop.sync(); });

    document.getElementById('shopForm').addEventListener('submit', function (e) {
        e.preventDefault();
        shop.saveShop();
    });

    document.getElementById('shopTabForm').addEventListener('submit', function (e) {
        e.preventDefault();
        shop.saveTab();
    });

    document.getElementById('shopItemForm').addEventListener('submit', function (e) {
        e.preventDefault();
        shop.saveItem();
    });

    shopModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-shop-close]')) shop.closeShop();
    });

    shopTabModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-tab-close]')) shop.closeTab();
    });

    shopItemModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-item-close]')) shop.closeItem();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        if (!shopModal.hidden) shop.closeShop();
        if (!shopTabModal.hidden) shop.closeTab();
        if (!shopItemModal.hidden) shop.closeItem();
        if (!playerModal.hidden) player.close();
        if (!mapModal.hidden) mapPanel.close();
        if (!mobModal.hidden) mobPanel.close();
        if (!intrinsicModal.hidden) intrinsicPanel.close();
        if (!accountModal.hidden) accountPanel.close();
        if (!accountCreateModal.hidden) accountCreateModal.hidden = true;
        if (!cfRecordModal.hidden) cloudflarePanel.close();
    });

    document.getElementById('view-shop-npc').addEventListener('click', function (e) {
        // Nút bên trong thẻ shop phải được kiểm tra trước thẻ bao ngoài
        var edit = e.target.closest('[data-shop-edit]');
        if (edit) { shop.openShop(parseInt(edit.dataset.shopEdit, 10)); return; }

        var remove = e.target.closest('[data-shop-del]');
        if (remove) { shop.deleteShop(parseInt(remove.dataset.shopDel, 10)); return; }

        var pick = e.target.closest('[data-shop-select]');
        if (pick) { shop.select(parseInt(pick.dataset.shopSelect, 10)); return; }

        var tab = e.target.closest('[data-shop-tab]');
        if (tab) { shop.showTab(parseInt(tab.dataset.shopTab, 10)); return; }

        var itemEdit = e.target.closest('[data-item-edit]');
        if (itemEdit) { shop.openItem(parseInt(itemEdit.dataset.itemEdit, 10)); return; }

        var itemDel = e.target.closest('[data-item-del]');
        if (itemDel) { shop.removeItem(parseInt(itemDel.dataset.itemDel, 10)); }
    });

    document.getElementById('shopItemTempPick').addEventListener('click', function () {
        openPicker('item', function (rows) {
            if (!rows.length) return;
            document.getElementById('shopItemTemp').value = rows[0].id;
            shopItemPreview(rows[0].name, rows[0].icon);
        });
    });

    document.getElementById('shopItemSpecPick').addEventListener('click', function () {
        openPicker('item', function (rows) {
            if (!rows.length) return;
            var icon = rows[0].icon || 0;
            document.getElementById('shopItemIconSpec').value = icon;
            shopSpecPreview(icon, rows[0].name);
        });
    });

    document.getElementById('shopItemTemp').addEventListener('change', async function () {
        var id = parseInt(this.value, 10);
        if (!(id > 0)) { shopItemPreview('', 0); return; }
        try {
            var found = await PanelAPI.catalog.item(id);
            shopItemPreview(found.name, found.icon);
        } catch (e) {
            shopItemPreview('Không tìm thấy vật phẩm #' + id, 0);
        }
    });

    document.getElementById('shopItemIconSpec').addEventListener('change', async function () {
        var icon = parseInt(this.value, 10) || 0;
        if (icon <= 0) { shopSpecPreview(0, ''); return; }
        try {
            var rows = await PanelAPI.catalog.searchItems('');
            var found = null;
            (rows || []).forEach(function (row) { if (!found && row.icon === icon) found = row; });
            shopSpecPreview(icon, found ? found.name : '');
        } catch (e) {
            shopSpecPreview(icon, '');
        }
    });

    document.getElementById('shopOptAdd').addEventListener('click', function () {
        var idInput = document.getElementById('shopOptId');
        if (idInput.value === '') { toast('Chưa nhập ID option', 'error'); return; }
        addOptionRow(document.getElementById('shopItemOptions'), {
            id: parseInt(idInput.value, 10),
            param: parseInt(valueOf_('shopOptParam'), 10) || 0
        });
        idInput.value = '';
        document.getElementById('shopOptParam').value = 0;
    });

    document.getElementById('shopOptPick').addEventListener('click', function () {
        openPicker('option', function (rows) {
            var box = document.getElementById('shopItemOptions');
            var existed = collectOptions(box).map(function (option) { return option.id; });
            rows.forEach(function (row) {
                if (existed.indexOf(row.id) >= 0) return;
                existed.push(row.id);
                addOptionRow(box, { id: row.id, param: 0, name: row.name });
            });
        });
    });

    /* ======================= 4e. QUẢN LÝ NHÂN VẬT ======================= */

    /** Chỉ số đệ tử (petPoint) theo đúng thứ tự PlayerApi ghi. */
    var PET_POINT_FIELDS = [
        ['power', 'Sức mạnh'],
        ['tiemNang', 'Tiềm năng'],
        ['stamina', 'Thể lực'],
        ['maxStamina', 'Thể lực tối đa'],
        ['hpg', 'HP gốc +%'],
        ['mpg', 'KI gốc +%'],
        ['dameg', 'Sát thương +%'],
        ['defg', 'Giáp +%'],
        ['critg', 'Chí mạng +%'],
        ['hp', 'HP'],
        ['mp', 'KI'],
        ['dame', 'Sát thương']
    ];

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    /** Mili giây -> giá trị cho thẻ <input type="datetime-local"> theo giờ máy. */
    function msToLocal(ms) {
        if (!ms) return '';
        var d = new Date(Number(ms));
        if (isNaN(d.getTime())) return '';
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
            + 'T' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
    }

    /** Giá trị datetime-local -> mili giây. Bỏ trống (đã xoá) trả về 0. */
    function localToMs(value) {
        if (!value) return 0;
        var time = new Date(value).getTime();
        return isNaN(time) ? 0 : time;
    }

    /** Mili giây -> chuỗi ngắn kiểu "22:06:17 26/9/26". */
    function fmtMoment(ms) {
        if (!ms) return '—';
        var d = new Date(Number(ms));
        if (isNaN(d.getTime())) return '—';
        return pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds())
            + ' ' + d.getDate() + '/' + (d.getMonth() + 1) + '/' + String(d.getFullYear()).slice(-2);
    }

    /** Một dòng trong bảng danh sách nhân vật. */
    function playerRow(row) {
        var state = row.online
            ? '<span class="type-badge type-badge--live">Đang online</span>'
            : '<span class="type-badge type-badge--off">Offline</span>';
        return '<tr>'
            + '<td class="num">' + row.id + '</td>'
            + '<td><div class="player-cell"><b class="player-cell__name">' + escapeHtml(row.name) + '</b>'
            + '<span class="player-cell__sub">Head ' + row.head + ' · Rank ' + row.rank
            + ' · PvP ' + formatPower(row.pointPvp) + '</span></div></td>'
            + '<td><div class="player-cell"><b class="player-cell__name">' + escapeHtml(row.username || '—') + '</b>'
            + '<span class="player-cell__sub">Account #' + row.accountId
            + (row.banned ? ' · ĐANG BỊ KHOÁ' : '') + '</span></div></td>'
            + '<td>' + escapeHtml(row.genderName) + '</td>'
            + '<td class="num">' + formatPower(row.power) + '</td>'
            + '<td><div class="player-money">'
            + '<span class="player-money__row"><b>' + formatPower(row.gold) + '</b><span>Vàng</span></span>'
            + '<span class="player-money__row"><b>' + formatPower(row.gem) + '</b><span>Ngọc</span></span>'
            + '<span class="player-money__row"><b>' + formatPower(row.ruby) + '</b><span>Hồng ngọc</span></span>'
            + '</div></td>'
            + '<td>' + state + '</td>'
            + '<td>' + escapeHtml(row.createTime || '—') + '</td>'
            + '<td><div class="player-acts">'
            + '<button class="btn-mini btn-mini--dark" type="button" data-player-edit="' + row.id + '">Sửa</button>'
            + (row.online
                ? '<button class="btn-mini btn-mini--danger" type="button" data-player-kick="' + row.id + '">Kick</button>'
                : '')
            + '</div></td>'
            + '</tr>';
    }

    /**
     * Trang "Quản Lý Nhân Vật": bảng danh sách + modal 4 tab sửa toàn bộ dữ liệu
     * của một nhân vật. Dữ liệu nằm trong các cột JSON nên panel chỉ thay đúng ô
     * cần đổi (xem PlayerApi).
     */
    var player = {
        active: false,
        index: null,
        detail: null,
        catalog: null,
        playerId: 0,
        activeTab: 'basic',

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        /* ----------------------------- tải dữ liệu ---------------------------- */

        load: async function () {
            try {
                this.index = (await PanelAPI.player.list({
                    q: valueOf_('playerSearch'),
                    filter: valueOf_('playerFilter')
                })) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách nhân vật: ' + e.message, 'error');
                return;
            }
            this.renderList();
        },

        findPlayer: function (id) {
            var found = null;
            ((this.index && this.index.players) || []).forEach(function (row) {
                if (row.id === id) found = row;
            });
            return found;
        },

        renderList: function () {
            var data = this.index || {};
            var rows = data.players || [];
            var limit = data.limit || 0;

            setText('plTotal', formatPower(data.total || 0));
            setText('plOnline', formatPower(data.online || 0));
            setText('plShown', rows.length + ' / ' + limit);
            setText('plReadInt', 'Vàng long · tối đa 2.000 tỷ');

            document.getElementById('playerEmpty').hidden = rows.length > 0;
            document.getElementById('playerBody').innerHTML = rows.map(playerRow).join('');

            var notes = [];
            if ((data.total || 0) > rows.length) {
                notes.push('Bảng có ' + data.total + ' nhân vật nhưng chỉ hiện ' + rows.length
                    + ' dòng đầu (giới hạn ' + limit + ' dòng/lần) — hãy dùng ô tìm kiếm để lọc bớt.');
            }
            if (data.readInt) {
                notes.push('Máy chủ đang đọc vàng / ngọc bằng kiểu int nên giá trị trên 2.147.483.647'
                    + ' sẽ bị cắt khi nạp vào game (Util.CrisGH).');
            }
            var hint = document.getElementById('playerHint');
            hint.textContent = notes.join(' · ');
            hint.hidden = !notes.length;
        },

        /* -------------------------- mở / đóng modal -------------------------- */

        open: async function (id, tab) {
            try {
                var res = await PanelAPI.player.detail(id);
                this.detail = (res && res.player) || null;
                this.catalog = (res && res.catalog) || {};
            } catch (e) {
                toast('Không đọc được nhân vật #' + id + ': ' + e.message, 'error');
                return;
            }
            if (!this.detail) {
                toast('Không đọc được nhân vật #' + id, 'error');
                return;
            }

            this.playerId = id;
            this.fillCatalog();
            this.fillBasic();
            this.renderPoints();
            this.fillPet();
            this.fillAdvanced();
            this.fillTasks();

            document.getElementById('playerModalTitle').textContent = 'CHỈNH SỬA NHÂN VẬT #' + id;
            document.getElementById('playerModal').hidden = false;
            this.showTab(tab || this.activeTab || 'basic');
        },

        close: function () {
            document.getElementById('playerModal').hidden = true;
        },

        showTab: function (name) {
            this.activeTab = name;
            Array.prototype.forEach.call(document.querySelectorAll('#playerTabs .tab'), function (btn) {
                btn.classList.toggle('is-active', btn.dataset.playerTab === name);
            });
            Array.prototype.forEach.call(document.querySelectorAll('#playerModal [data-player-pane]'), function (pane) {
                pane.classList.toggle('is-active', pane.dataset.playerPane === name);
            });
        },

        /* ------------------------------ điền dữ liệu ------------------------- */

        /** Các select dùng chung cho mọi nhân vật: hệ, nội tại, nhiệm vụ, cột JSON. */
        fillCatalog: function () {
            var cat = this.catalog || {};

            document.getElementById('pbGender').innerHTML = (cat.genders || []).map(function (g) {
                return '<option value="' + g.id + '">' + escapeHtml(g.name) + '</option>';
            }).join('');

            document.getElementById('piId').innerHTML = '<option value="0">Không mang nội tại</option>'
                + (cat.intrinsics || []).map(function (i) {
                    return '<option value="' + i.id + '">#' + i.id + ' — ' + escapeHtml(i.name) + '</option>';
                }).join('');

            function taskOptions(rows, noneValue) {
                // Nhiệm vụ phụ / bang dùng -1 cho "không có nhiệm vụ", đúng quy ước của game.
                return (noneValue === undefined ? '' : '<option value="' + noneValue + '">Không có nhiệm vụ</option>')
                    + (rows || []).map(function (t) {
                        return '<option value="' + t.id + '">#' + t.id + ' — ' + escapeHtml(t.name) + '</option>';
                    }).join('');
            }

            // Nhiệm vụ chính luôn phải có (GodGK đọc thẳng theo id) nên chỉ liệt kê mẫu thật
            document.getElementById('ptnTask').innerHTML = taskOptions(cat.taskMains);
            document.getElementById('pstTask').innerHTML = taskOptions(cat.sideTasks, -1);
            document.getElementById('ptcTask').innerHTML = taskOptions(cat.clanTasks, -1);

            document.getElementById('prColumn').innerHTML = (cat.rawColumns || []).map(function (column) {
                return '<option value="' + escapeHtml(column) + '">' + escapeHtml(column) + '</option>';
            }).join('');

            // 10 ô kỹ năng dùng nhanh — chỉ hiện kỹ năng của đúng hệ nhân vật
            var options = '<option value="-1">Không gán</option>' + (cat.skills || []).map(function (s) {
                return '<option value="' + s.id + '">#' + s.id + ' — ' + escapeHtml(s.name) + '</option>';
            }).join('');
            var slots = cat.shortcutSlots || 10;
            var box = document.getElementById('pcShortcut');
            box.innerHTML = '';
            for (var i = 0; i < slots; i++) {
                var field = document.createElement('div');
                field.className = 'field';
                field.innerHTML = '<label for="pcSlot' + i + '">Ô NHANH ' + (i + 1) + '</label>'
                    + '<select id="pcSlot' + i + '" class="pc-slot" data-slot="' + i + '">'
                    + options + '</select>';
                box.appendChild(field);
            }
        },

        fillBasic: function () {
            var p = this.detail || {};
            var info = p.info || {};

            setInput('pbName', info.name);
            setSelect('pbGender', String(info.gender));
            setInput('pbHead', info.head);
            setInput('pbRank', info.rank);
            setText('pbAccount', 'Tài khoản ' + (info.username || '—') + ' · Account #' + info.accountId
                + ' · Tạo ' + (info.createTime || '—'));

            var money = p.inventory || {};
            setInput('pmGold', money.gold);
            setInput('pmGem', money.gem);
            setInput('pmRuby', money.ruby);

            var location = p.location || {};
            setInput('plMap', location.mapId);
            setInput('plX', location.x);
            setInput('plY', location.y);

            var tree = p.magicTree || {};
            setInput('ptLevel', tree.level);
            setInput('ptPeas', tree.peas);
            setSelect('ptUpgrading', tree.upgrading ? '1' : '0');

            document.getElementById('playerModalState').innerHTML = p.online
                ? '<span class="is-live">● Đang online</span> — mọi thay đổi sẽ tự ngắt kết nối'
                : '○ Đang offline';
        },

        renderPoints: function () {
            var fields = (this.catalog || {}).pointFields || [];
            var point = (this.detail || {}).point || {};

            document.getElementById('ppFields').innerHTML = fields.map(function (f, i) {
                var value = point[f.key] == null ? 0 : point[f.key];
                return '<div class="field">'
                    + '<label for="ppF' + i + '">' + escapeHtml(String(f.label).toUpperCase()) + '</label>'
                    + '<input type="number" class="pp-field" id="ppF' + i + '" data-key="' + f.key + '"'
                    + ' value="' + value + '" min="0"'
                    + (f.key === 'unused' ? ' readonly title="Ô trống của data_point, không sửa được"' : '') + '>'
                    + '</div>';
            }).join('');

            setText('ppHint', 'DATA_POINT · ' + fields.length + ' ô');
        },

        fillPet: function () {
            var pet = (this.detail || {}).pet || {};
            var has = !!pet.has;

            document.getElementById('petNone').hidden = has;
            document.getElementById('petBox').hidden = !has;
            setText('petState', has ? 'Đang sở hữu đệ tử' : 'Không có đệ tử');
            if (!has) return;

            setInput('petName', pet.name);
            setInput('petType', pet.typePet);
            setInput('petStatus', pet.status);
            setInput('petLimitPower', pet.limitPower);

            document.getElementById('petFields').innerHTML = PET_POINT_FIELDS.map(function (f, i) {
                return '<div class="field">'
                    + '<label for="pf' + i + '">' + escapeHtml(String(f[1]).toUpperCase()) + '</label>'
                    + '<input type="number" class="pet-field" id="pf' + i + '" data-key="' + f[0] + '"'
                    + ' value="' + (pet[f[0]] || 0) + '" min="0">'
                    + '</div>';
            }).join('');

            var rows = [
                ['Hệ đệ tử', this.genderLabel(pet.gender)],
                ['Type đệ tử', pet.typePet],
                ['Type hợp thể', pet.typeFusion],
                ['Thời gian hợp thể còn lại', (pet.timeLeftFusion || 0) + ' ms'],
                ['Số phần thân (petBody)', pet.bodyCount],
                ['Số kỹ năng (petSkill)', pet.skillCount]
            ];
            document.getElementById('petInfo').innerHTML = rows.map(function (row) {
                return '<div class="kv__row"><span>' + escapeHtml(row[0]) + '</span><b>'
                    + escapeHtml(String(row[1] == null ? '—' : row[1])) + '</b></div>';
            }).join('');
        },

        genderLabel: function (gender) {
            var found = null;
            (((this.catalog || {}).genders) || []).forEach(function (g) {
                if (g.id === gender) found = g.name;
            });
            return found || ('Hệ ' + gender);
        },

        fillAdvanced: function () {
            var p = this.detail || {};
            var cat = this.catalog || {};
            var intrinsic = p.intrinsic || {};

            setSelect('piId', String(intrinsic.id || 0));
            setInput('piParam1', intrinsic.param1);
            setInput('piParam2', intrinsic.param2);
            setInput('piCountOpen', intrinsic.countOpen);

            var found = null;
            (cat.intrinsics || []).forEach(function (i) { if (i.id === intrinsic.id) found = i; });
            setText('piHint', found
                ? ('Nội tại #' + found.id + ' — ' + found.name + ' · tham số 1: ' + found.from1 + '–' + found.to1
                    + ' · tham số 2: ' + found.from2 + '–' + found.to2)
                : 'Nhân vật đang không mang nội tại nào.');

            // Kỹ năng: chỉ sửa điểm và cấp để không phá cấu trúc mảng `skills`
            var skills = p.skills || [];
            document.getElementById('pskEmpty').hidden = skills.length > 0;
            document.getElementById('pskBody').innerHTML = skills.map(function (s, i) {
                return '<tr>'
                    + '<td><b>#' + s.tempId + '</b> — ' + escapeHtml(s.name) + '</td>'
                    + '<td><input type="number" class="psk-point input-tiny" data-index="' + i
                        + '" min="0" max="127" value="' + s.point + '"></td>'
                    + '<td><input type="number" class="psk-level input-tiny" data-index="' + i
                        + '" min="0" max="32767" value="' + s.currLevel + '"></td>'
                    + '<td>' + (s.lastTimeUse ? escapeHtml(fmtMoment(s.lastTimeUse)) : '—') + '</td>'
                    + '</tr>';
            }).join('');
            setText('pskHint', 'SKILLS · ' + skills.length + ' kỹ năng');

            (p.shortcut || []).forEach(function (value, i) {
                setSelect('pcSlot' + i, String(value));
            });

            this.renderCharms();
        },

        renderCharms: function () {
            var charms = (this.catalog || {}).charms || [];
            var now = Date.now();
            var live = 0;

            document.getElementById('pcList').innerHTML = charms.map(function (c) {
                var expired = !c.time || c.time <= now;
                if (!expired) live++;
                var state = !c.time
                    ? 'Chưa kích hoạt'
                    : (expired ? 'Đã hết hạn ' + fmtMoment(c.time) : 'Còn hiệu lực đến ' + fmtMoment(c.time));
                return '<div class="charm' + (expired ? ' is-expired' : '') + '" data-pos="' + c.pos + '">'
                    + '<div class="charm__info">'
                    + '<b class="charm__name">' + escapeHtml(c.itemName) + '</b>'
                    + '<span class="charm__meta">Vị trí ' + c.pos + ' · Item #' + c.itemId
                    + ' · ' + escapeHtml(c.field) + '</span>'
                    + '</div>'
                    + '<input type="datetime-local" class="charm__time" data-pos="' + c.pos
                    + '" value="' + msToLocal(c.time) + '">'
                    + '<span class="charm__state' + (expired ? '' : ' is-live') + '">' + escapeHtml(state) + '</span>'
                    + '<button class="btn-mini btn-mini--danger" type="button" data-charm-clear="' + c.pos
                    + '">Xóa</button>'
                    + '</div>';
            }).join('');

            setText('pcCount', live + ' bùa đang hoạt động / ' + charms.length + ' ô');
        },

        clearCharm: function (pos) {
            var row = document.querySelector('.charm[data-pos="' + pos + '"]');
            if (!row) return;
            var input = row.querySelector('.charm__time');
            if (input) input.value = '';
            var state = row.querySelector('.charm__state');
            if (state) {
                state.className = 'charm__state';
                state.textContent = 'Sẽ xóa khi lưu';
            }
        },

        fillTasks: function () {
            var p = this.detail || {};

            var task = p.task || {};
            setSelectValue('ptnTask', String(task.taskMainId || 0),
                '#' + (task.taskMainId || 0) + ' — không có trong task_main_template');
            setInput('ptnIndex', task.index);
            setInput('ptnCount', task.count);
            setInput('ptnTime', msToLocal(task.lastTime));

            var side = p.sideTask || {};
            setSelectValue('pstTask', String(side.templateId > 0 ? side.templateId : -1),
                '#' + side.templateId + ' — không có trong side_task_template');
            setInput('pstLevel', side.level || 1);
            setInput('pstReceived', msToLocal(side.receivedTime));
            setInput('pstCount', side.count);
            setInput('pstMax', side.maxCount);
            setInput('pstLeft', side.leftTask);

            var clan = p.clanTask || {};
            setSelectValue('ptcTask', String(clan.templateId > 0 ? clan.templateId : -1),
                '#' + clan.templateId + ' — không có trong side_task_template');
            setInput('ptcLevel', clan.level || 1);
            setInput('ptcReceived', msToLocal(clan.receivedTime));
            setInput('ptcCount', clan.count);
            setInput('ptcMax', clan.maxCount);
            setInput('ptcLeft', clan.leftTask);
        },

        /* -------------------------------- ghi dữ liệu ----------------------- */

        /** Gửi 1 thao tác ghi, KHÔNG nạp lại (dùng khi gộp nhiều thao tác). */
        post: async function (action, body) {
            if (!this.playerId) return null;
            try {
                return await PanelAPI.player.run(action,
                    Object.assign({ id: this.playerId }, body || {}));
            } catch (e) {
                toast(e.message, 'error');
                return null;
            }
        },

        /** Gửi 1 thao tác ghi rồi nạp lại chi tiết nhân vật + bảng danh sách. */
        save: async function (action, body, successMessage) {
            var res = await this.post(action, body);
            if (!res) return null;
            toast(res.message || successMessage || 'Đã lưu', 'success');
            await this.refresh();
            return res;
        },

        /** Nạp lại chi tiết nhân vật và bảng danh sách, giữ nguyên tab đang mở. */
        refresh: async function () {
            await this.open(this.playerId, this.activeTab);
            if (this.active) await this.load();
            return true;
        },

        saveBasic: function () {
            this.save('basic', {
                name: valueOf_('pbName'),
                gender: parseInt(valueOf_('pbGender'), 10),
                head: parseInt(valueOf_('pbHead'), 10) || 0,
                rank: parseInt(valueOf_('pbRank'), 10) || 0
            }, 'Đã lưu thông tin nhân vật');
        },

        savePoints: function () {
            var body = {};
            Array.prototype.forEach.call(document.querySelectorAll('.pp-field'), function (el) {
                if (el.dataset.key === 'unused') return;
                var value = parseInt(el.value, 10);
                body[el.dataset.key] = isNaN(value) ? 0 : value;
            });
            this.save('point', body, 'Đã lưu chỉ số nhân vật');
        },

        saveMoney: function () {
            this.save('inventory', {
                gold: parseInt(valueOf_('pmGold'), 10) || 0,
                gem: parseInt(valueOf_('pmGem'), 10) || 0,
                ruby: parseInt(valueOf_('pmRuby'), 10) || 0
            }, 'Đã lưu tiền tệ');
        },

        saveLocation: function () {
            this.save('location', {
                mapId: parseInt(valueOf_('plMap'), 10) || 0,
                x: parseInt(valueOf_('plX'), 10) || 0,
                y: parseInt(valueOf_('plY'), 10) || 0
            }, 'Đã lưu vị trí nhân vật');
        },

        saveTree: function () {
            this.save('magic-tree', {
                level: parseInt(valueOf_('ptLevel'), 10) || 0,
                peas: parseInt(valueOf_('ptPeas'), 10) || 0,
                upgrading: valueOf_('ptUpgrading') === '1'
            }, 'Đã lưu đậu thần');
        },

        intrinsicBody: function () {
            return {
                intrinsicId: parseInt(valueOf_('piId'), 10) || 0,
                param1: parseInt(valueOf_('piParam1'), 10) || 0,
                param2: parseInt(valueOf_('piParam2'), 10) || 0,
                countOpen: parseInt(valueOf_('piCountOpen'), 10) || 0
            };
        },

        maxStats: function () {
            if (!window.confirm('Đặt HP / KI / sát thương và mức tối đa của nhân vật #'
                    + this.playerId + ' lên 2.147.483.647?')) return;
            this.save('max-stats', {}, 'Đã đặt chỉ số tối đa');
        },

        clearCooldown: function () {
            this.save('cooldown-clear', {}, 'Đã xóa hiệu ứng cooldown kỹ năng');
        },

        clearIntrinsic: function () {
            if (!window.confirm('Xóa nội tại của nhân vật #' + this.playerId + '?')) return;
            this.save('intrinsic-clear', {}, 'Đã xóa nội tại');
        },

        /** Điểm / cấp của từng kỹ năng (không thêm hay bớt kỹ năng). */
        saveSkills: function () {
            var rows = [];
            Array.prototype.forEach.call(document.querySelectorAll('#pskBody tr'), function (tr, i) {
                var point = tr.querySelector('.psk-point');
                var level = tr.querySelector('.psk-level');
                rows[i] = {
                    point: parseInt(point ? point.value : '0', 10) || 0,
                    currLevel: parseInt(level ? level.value : '0', 10) || 0
                };
            });
            this.save('skills', { skills: rows }, 'Đã lưu kỹ năng');
        },

        /** Giá trị 10 ô dùng nhanh (-1 = không gán). */
        shortcutValues: function () {
            var slots = [];
            Array.prototype.forEach.call(document.querySelectorAll('.pc-slot'), function (el) {
                slots[parseInt(el.dataset.slot, 10)] = parseInt(el.value, 10);
            });
            return slots;
        },

        /** Chỉ những ô bùa thực sự đổi mới gửi lên (API từ chối khi không có ô nào đổi). */
        charmChanges: function () {
            var catalog = (this.catalog && this.catalog.charms) || [];
            var entries = [];
            Array.prototype.forEach.call(document.querySelectorAll('.charm'), function (row) {
                var pos = parseInt(row.dataset.pos, 10);
                var input = row.querySelector('.charm__time');
                var time = localToMs(input ? input.value : '');
                var original = (catalog[pos] && catalog[pos].time) || 0;
                if (time !== original) {
                    entries.push({ pos: pos, time: time });
                }
            });
            return entries;
        },

        saveCharms: function () {
            var entries = this.charmChanges();
            if (!entries.length) {
                toast('Không có ô bùa nào thay đổi', 'error');
                return;
            }
            this.save('charms', { charms: entries }, 'Đã lưu ' + entries.length + ' ô bùa');
        },

        /**
         * Nút "Lưu Dữ Liệu Nâng Cao" gom 3 phần của tab Nâng Cao vào một lần nạp lại
         * (nếu lưu từng phần riêng sẽ mất các ô bùa chưa lưu).
         */
        saveAdvanced: async function () {
            var entries = this.charmChanges();
            var results = [
                await this.post('intrinsic', this.intrinsicBody()),
                await this.post('shortcut', { slots: this.shortcutValues() })
            ];
            if (entries.length) {
                results.push(await this.post('charms', { charms: entries }));
            }
            if (results.indexOf(null) >= 0) {
                toast('Một phần dữ liệu nâng cao chưa lưu được', 'error');
                return;
            }
            toast('Đã lưu dữ liệu nâng cao', 'success');
            await this.refresh();
        },

        loadRaw: function () {
            var column = valueOf_('prColumn');
            var raw = (this.detail && this.detail.raw) || {};
            document.getElementById('prValue').value = column ? (raw[column] || '') : '';
        },

        toggleRaw: function () {
            var box = document.getElementById('prBox');
            var button = document.getElementById('prToggle');
            box.hidden = !box.hidden;
            button.textContent = box.hidden ? '▶ Xem / sửa JSON thô' : '▼ Ẩn JSON thô';
            if (!box.hidden) this.loadRaw();
        },

        /** Kiểm tra JSON ngay ở client để báo lỗi rõ ràng trước khi gọi server. */
        saveRaw: function () {
            var column = valueOf_('prColumn');
            var value = valueOf_('prValue');
            var parsed;
            try {
                parsed = JSON.parse(value || '[]');
            } catch (e) {
                toast('Nội dung không phải JSON hợp lệ: ' + e.message, 'error');
                return;
            }
            if (!Array.isArray(parsed)) {
                toast('Cột ' + column + ' phải là một mảng JSON, ví dụ [1,2,3]', 'error');
                return;
            }
            this.save('raw', { column: column, value: JSON.stringify(parsed) },
                'Đã ghi lại cột ' + column);
        },

        savePet: function () {
            var body = {
                name: valueOf_('petName'),
                typePet: parseInt(valueOf_('petType'), 10) || 0,
                status: parseInt(valueOf_('petStatus'), 10) || 0,
                limitPower: parseInt(valueOf_('petLimitPower'), 10) || 0
            };
            Array.prototype.forEach.call(document.querySelectorAll('.pet-field'), function (el) {
                var value = parseInt(el.value, 10);
                body[el.dataset.key] = isNaN(value) ? 0 : value;
            });
            this.save('pet', body, 'Đã lưu đệ tử');
        },

        deletePet: function () {
            if (!window.confirm('Xóa đệ tử của nhân vật #' + this.playerId
                    + '? Thao tác không thể hoàn tác.')) return;
            this.save('pet-delete', {}, 'Đã xóa đệ tử');
        },

        saveMainTask: function () {
            var taskMainId = parseInt(valueOf_('ptnTask'), 10);
            if (!(taskMainId > 0)) { toast('Chưa chọn nhiệm vụ chính', 'error'); return; }
            this.save('task', {
                taskMainId: taskMainId,
                index: parseInt(valueOf_('ptnIndex'), 10) || 0,
                count: parseInt(valueOf_('ptnCount'), 10) || 0,
                lastTime: localToMs(valueOf_('ptnTime'))
            }, 'Đã lưu nhiệm vụ chính');
        },

        saveSideTask: function (clan) {
            var prefix = clan ? 'ptc' : 'pst';
            this.save(clan ? 'clan-task' : 'side-task', {
                templateId: parseInt(valueOf_(prefix + 'Task'), 10) || 0,
                level: parseInt(valueOf_(prefix + 'Level'), 10) || 1,
                receivedTime: localToMs(valueOf_(prefix + 'Received')),
                count: parseInt(valueOf_(prefix + 'Count'), 10) || 0,
                maxCount: parseInt(valueOf_(prefix + 'Max'), 10) || 0,
                leftTask: parseInt(valueOf_(prefix + 'Left'), 10) || 0
            }, clan ? 'Đã lưu nhiệm vụ bang' : 'Đã lưu nhiệm vụ hàng ngày');
        },

        kick: async function (id) {
            var target = this.findPlayer(id);
            var label = target ? ('nhân vật "' + target.name + '"') : ('nhân vật #' + id);
            if (!window.confirm('Ngắt kết nối ' + label + '?')) return;
            try {
                var res = await PanelAPI.player.run('kick', { id: id });
                toast(res.message || 'Đã ngắt kết nối', 'success');
                this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /* ---- Sự kiện trang nhân vật ---- */

    var playerModal = document.getElementById('playerModal');

    document.getElementById('view-player').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-player-edit]');
        if (edit) { player.open(parseInt(edit.dataset.playerEdit, 10)); return; }

        var kick = e.target.closest('[data-player-kick]');
        if (kick) { player.kick(parseInt(kick.dataset.playerKick, 10)); }
    });

    document.getElementById('playerReload').addEventListener('click', function () { player.load(); });
    document.getElementById('playerFilter').addEventListener('change', function () { player.load(); });

    var playerSearchTimer;
    document.getElementById('playerSearch').addEventListener('input', function () {
        clearTimeout(playerSearchTimer);
        playerSearchTimer = setTimeout(function () { player.load(); }, 300);
    });
    document.getElementById('playerSearch').addEventListener('keydown', function (e) {
        if (e.key !== 'Enter') return;
        clearTimeout(playerSearchTimer);
        player.load();
    });

    playerModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-player-close]')) { player.close(); return; }

        var tab = e.target.closest('[data-player-tab]');
        if (tab) { player.showTab(tab.dataset.playerTab); return; }

        var clear = e.target.closest('[data-charm-clear]');
        if (clear) { player.clearCharm(parseInt(clear.dataset.charmClear, 10)); }
    });

    document.getElementById('pbSave').addEventListener('click', function () { player.saveBasic(); });
    document.getElementById('ppSave').addEventListener('click', function () { player.savePoints(); });
    document.getElementById('ppMax').addEventListener('click', function () { player.maxStats(); });
    document.getElementById('pmSave').addEventListener('click', function () { player.saveMoney(); });
    document.getElementById('plSave').addEventListener('click', function () { player.saveLocation(); });
    document.getElementById('ptSave').addEventListener('click', function () { player.saveTree(); });

    document.getElementById('piMax').addEventListener('click', function () { player.maxStats(); });
    document.getElementById('piCooldown').addEventListener('click', function () { player.clearCooldown(); });
    document.getElementById('piClear').addEventListener('click', function () { player.clearIntrinsic(); });
    document.getElementById('pskSave').addEventListener('click', function () { player.saveSkills(); });
    document.getElementById('pAdvSave').addEventListener('click', function () { player.saveAdvanced(); });
    document.getElementById('prToggle').addEventListener('click', function () { player.toggleRaw(); });
    document.getElementById('prColumn').addEventListener('change', function () { player.loadRaw(); });
    document.getElementById('prSave').addEventListener('click', function () { player.saveRaw(); });

    document.getElementById('petSave').addEventListener('click', function () { player.savePet(); });
    document.getElementById('petDelete').addEventListener('click', function () { player.deletePet(); });

    document.getElementById('ptnSave').addEventListener('click', function () { player.saveMainTask(); });
    document.getElementById('pstSave').addEventListener('click', function () { player.saveSideTask(false); });
    document.getElementById('ptcSave').addEventListener('click', function () { player.saveSideTask(true); });

    /* ======================== 4f. QUẢN LÝ BẢN ĐỒ ======================== */

    /** Đổ danh sách [[value, label]] vào một <select> có thêm dòng "tất cả". */
    function fillFilter(id, rows, allLabel) {
        var el = document.getElementById(id);
        if (!el) return;
        el.innerHTML = '<option value="">' + escapeHtml(allLabel) + '</option>'
            + rows.map(function (row) {
                return '<option value="' + row.value + '">' + escapeHtml(row.label) + '</option>';
            }).join('');
    }

    /** Đổ danh sách [[value, label]] vào <select>, giữ nguyên lựa chọn cũ nếu còn. */
    function fillOptions(id, rows, placeholder) {
        var el = document.getElementById(id);
        if (!el) return;
        var keep = el.value;
        el.innerHTML = (placeholder === undefined ? '' : '<option value="">' + escapeHtml(placeholder) + '</option>')
            + rows.map(function (row) {
                return '<option value="' + row.value + '">' + escapeHtml(row.label) + '</option>';
            }).join('');
        if (keep) el.value = keep;
    }

    /** Một dòng bảng con (quái / NPC / cổng) có nút xoá dùng chung. */
    function subRow(cells) {
        return '<tr>' + cells.join('') + '<td><button class="btn-mini btn-mini--danger" type="button"'
            + ' data-row-del="1">Xóa</button></td></tr>';
    }

    /**
     * Trang "Quản Lý Map": danh sách 248 bản đồ + modal sửa thông tin, danh sách
     * quái, NPC và cổng dịch chuyển của từng bản đồ.
     */
    var mapPanel = {
        active: false,
        index: null,
        detail: null,
        catalog: null,
        mapId: 0,
        activeTab: 'info',

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        load: async function () {
            var type = valueOf_('mapTypeFilter');
            var planet = valueOf_('mapPlanetFilter');
            try {
                this.index = (await PanelAPI.map.list({
                    q: valueOf_('mapSearch'),
                    type: type,
                    planet: planet
                })) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách bản đồ: ' + e.message, 'error');
                return;
            }

            var data = this.index;
            fillFilter('mapTypeFilter', (data.types || []).map(function (t) {
                return { value: t.id, label: t.name };
            }), 'Tất cả loại bản đồ');
            fillFilter('mapPlanetFilter', (data.planets || []).map(function (p) {
                return { value: p.id, label: p.name };
            }), 'Tất cả hành tinh');
            // fillFilter ghi lại <option>, phải gán lại giá trị đang lọc
            setSelect('mapTypeFilter', type);
            setSelect('mapPlanetFilter', planet);

            this.renderList();
        },

        renderList: function () {
            var data = this.index || {};
            var rows = data.maps || [];
            var zones = 0;
            var withMobs = 0;
            var withNpcs = 0;
            rows.forEach(function (row) {
                zones += row.zones;
                if (row.mobCount > 0) withMobs++;
                if (row.npcCount > 0) withNpcs++;
            });

            setText('mpTotal', formatPower(data.total || 0));
            setText('mpZones', formatPower(zones));
            setText('mpWithMobs', formatPower(withMobs));
            setText('mpWithNpcs', formatPower(withNpcs));

            document.getElementById('mapEmpty').hidden = rows.length > 0;
            document.getElementById('mapBody').innerHTML = rows.map(function (row) {
                return '<tr>'
                    + '<td class="num">' + row.id + '</td>'
                    + '<td><span class="map-name">' + escapeHtml(row.name) + '</span>'
                    + '<span class="map-sub">' + escapeHtml(row.genderName) + '</span></td>'
                    + '<td>' + escapeHtml(row.planetName) + '</td>'
                    + '<td>' + escapeHtml(row.typeName) + '</td>'
                    + '<td class="num">' + row.zones + '</td>'
                    + '<td class="num">' + row.maxPlayer + '</td>'
                    + '<td class="num">' + row.mobCount + '</td>'
                    + '<td class="num">' + row.npcCount + '</td>'
                    + '<td class="num">' + row.waypointCount + '</td>'
                    + '<td>' + escapeHtml(row.genderName) + '</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-map-edit="'
                    + row.id + '">Sửa</button></td>'
                    + '</tr>';
            }).join('');

            var hint = document.getElementById('mapHint');
            var notes = [];
            if ((data.total || 0) > rows.length) {
                notes.push('Có ' + data.total + ' bản đồ nhưng chỉ hiện ' + rows.length
                    + ' dòng đầu (giới hạn ' + data.limit + ' dòng/lần) — hãy dùng ô tìm kiếm.');
            }
            notes.push('Id bản đồ là vị trí mà client dùng để tra tên map, nên panel không cho thêm/xoá bản đồ.');
            hint.textContent = notes.join(' · ');
            hint.hidden = false;
        },

        /* -------------------------- mở / đóng modal -------------------------- */

        open: async function (id, tab) {
            try {
                var res = await PanelAPI.map.detail(id);
                this.detail = (res && res.map) || null;
                this.catalog = (res && res.catalog) || {};
            } catch (e) {
                toast('Không đọc được bản đồ #' + id + ': ' + e.message, 'error');
                return;
            }
            if (!this.detail) {
                toast('Không đọc được bản đồ #' + id, 'error');
                return;
            }

            this.mapId = id;
            this.fillCatalog();
            this.fillInfo();
            this.renderMobs();
            this.renderNpcs();
            this.renderWaypoints();

            document.getElementById('mapModalTitle').textContent = 'SỬA BẢN ĐỒ #' + id;
            document.getElementById('mapModalState').textContent = this.detail.name || '';
            document.getElementById('mapModal').hidden = false;
            this.showTab(tab || this.activeTab || 'info');
        },

        close: function () {
            document.getElementById('mapModal').hidden = true;
        },

        showTab: function (name) {
            this.activeTab = name;
            Array.prototype.forEach.call(document.querySelectorAll('#mapTabs .tab'), function (btn) {
                btn.classList.toggle('is-active', btn.dataset.mapTab === name);
            });
            Array.prototype.forEach.call(document.querySelectorAll('#mapModal [data-map-pane]'), function (pane) {
                pane.classList.toggle('is-active', pane.dataset.mapPane === name);
            });
        },

        /* ------------------------------ điền dữ liệu ------------------------- */

        fillCatalog: function () {
            var cat = this.catalog || {};
            var typeOptions = (cat.types || []).map(function (t) {
                return '<option value="' + t.id + '">' + escapeHtml(t.name) + '</option>';
            });
            document.getElementById('mfType').innerHTML = typeOptions.join('');

            document.getElementById('mfPlanet').innerHTML = (cat.planets || []).map(function (p) {
                return '<option value="' + p.id + '">' + escapeHtml(p.name) + '</option>';
            }).join('');

            document.getElementById('mfGender').innerHTML = (cat.genders || []).map(function (g) {
                return '<option value="' + g.id + '">' + escapeHtml(g.name) + '</option>';
            }).join('');

            // Select dùng lại cho mọi dòng quái / NPC / cổng
            this.mobOptions = '<option value="0">— chọn mẫu quái —</option>'
                + (cat.mobs || []).map(function (m) {
                    return '<option value="' + m.id + '">#' + m.id + ' — ' + escapeHtml(m.name)
                        + ' (' + formatPower(m.hp) + ' HP)</option>';
                }).join('');
            this.npcOptions = '<option value="0">— chọn NPC —</option>'
                + (cat.npcs || []).map(function (n) {
                    return '<option value="' + n.id + '">#' + n.id + ' — ' + escapeHtml(n.name) + '</option>';
                }).join('');
            this.mapOptions = '<option value="0">— chọn bản đồ —</option>'
                + (cat.waypointTargets || []).map(function (m) {
                    return '<option value="' + m.id + '">#' + m.id + ' — ' + escapeHtml(m.name) + '</option>';
                }).join('');
        },

        fillInfo: function () {
            var map = this.detail || {};
            setInput('mfName', map.name);
            setSelect('mfType', String(map.type));
            setSelect('mfPlanet', String(map.planetId));
            setInput('mfZones', map.zones);
            setInput('mfMaxPlayer', map.maxPlayer);
            setSelect('mfGender', String(map.genderType));
            setInput('mfBgType', map.bgType);
            setInput('mfTileId', map.tileId);
            setInput('mfBgId', map.bgId);
            setText('mfData', 'Cột data (không được game đọc, giữ nguyên): ' + (map.data || '—'));
        },

        renderMobs: function () {
            var mobs = (this.detail || {}).mobs || [];
            document.getElementById('mapMobEmpty').hidden = mobs.length > 0;
            document.getElementById('mapTabMobCount').textContent = mobs.length;
            document.getElementById('mapMobBody').innerHTML = mobs.map(function (mob) {
                return subRow([
                    '<td><select class="map-mob-temp" data-value="' + mob.tempId + '">'
                        + mapPanel.mobOptions + '</select>'
                        + '<span class="spawn-name">' + escapeHtml(mob.tempName) + '</span></td>',
                    '<td><input type="number" class="map-mob-level" value="' + mob.level + '" min="0" max="127"></td>',
                    '<td><input type="number" class="map-mob-hp" value="' + mob.hp + '" min="0"></td>',
                    '<td><input type="number" class="map-mob-x" value="' + mob.x + '"></td>',
                    '<td><input type="number" class="map-mob-y" value="' + mob.y + '"></td>'
                ]);
            }).join('');
            applyRowSelects('#mapMobBody .map-mob-temp');
        },

        renderNpcs: function () {
            var npcs = (this.detail || {}).npcs || [];
            document.getElementById('mapNpcEmpty').hidden = npcs.length > 0;
            document.getElementById('mapTabNpcCount').textContent = npcs.length;
            document.getElementById('mapNpcBody').innerHTML = npcs.map(function (npc) {
                return subRow([
                    '<td><select class="map-npc-id" data-value="' + npc.npcId + '">'
                        + mapPanel.npcOptions + '</select>'
                        + '<span class="spawn-name">' + escapeHtml(npc.npcName) + '</span></td>',
                    '<td><input type="number" class="map-npc-x" value="' + npc.x + '"></td>',
                    '<td><input type="number" class="map-npc-y" value="' + npc.y + '"></td>'
                ]);
            }).join('');
            applyRowSelects('#mapNpcBody .map-npc-id');
        },

        renderWaypoints: function () {
            var points = (this.detail || {}).waypoints || [];
            document.getElementById('mapWpEmpty').hidden = points.length > 0;
            document.getElementById('mapTabWpCount').textContent = points.length;
            document.getElementById('mapWpBody').innerHTML = points.map(function (point) {
                return subRow([
                    '<td><input type="text" class="map-wp-name" value="' + escapeHtml(point.name) + '" maxlength="32"></td>',
                    '<td><input type="number" class="map-wp-minx" value="' + point.minX + '"></td>',
                    '<td><input type="number" class="map-wp-miny" value="' + point.minY + '"></td>',
                    '<td><input type="number" class="map-wp-maxx" value="' + point.maxX + '"></td>',
                    '<td><input type="number" class="map-wp-maxy" value="' + point.maxY + '"></td>',
                    '<td><input type="checkbox" class="map-wp-enter"' + (point.isEnter ? ' checked' : '') + '></td>',
                    '<td><input type="checkbox" class="map-wp-offline"' + (point.isOffline ? ' checked' : '') + '></td>',
                    '<td><select class="map-wp-gomap" data-value="' + point.goMap + '">'
                        + mapPanel.mapOptions + '</select>'
                        + '<span class="spawn-name">' + escapeHtml(point.goMapName) + '</span></td>',
                    '<td><input type="number" class="map-wp-gox" value="' + point.goX + '"></td>',
                    '<td><input type="number" class="map-wp-goy" value="' + point.goY + '"></td>'
                ]);
            }).join('');
            applyRowSelects('#mapWpBody .map-wp-gomap');
        },

        /* -------------------------------- ghi -------------------------------- */

        post: async function (action, body) {
            try {
                return await PanelAPI.map.run(action, body);
            } catch (e) {
                toast(e.message, 'error');
                return null;
            }
        },

        refresh: async function () {
            await this.open(this.mapId, this.activeTab);
            if (this.active) await this.load();
        },

        save: async function (action, body, message) {
            var res = await this.post(action, Object.assign({ id: this.mapId }, body));
            if (!res) return;
            toast(res.message || message, 'success');
            await this.refresh();
        },

        saveInfo: function () {
            this.save('map-update', {
                name: valueOf_('mfName'),
                type: parseInt(valueOf_('mfType'), 10) || 0,
                planetId: parseInt(valueOf_('mfPlanet'), 10) || 0,
                zones: parseInt(valueOf_('mfZones'), 10) || 1,
                maxPlayer: parseInt(valueOf_('mfMaxPlayer'), 10) || 1,
                bgType: parseInt(valueOf_('mfBgType'), 10) || 0,
                tileId: parseInt(valueOf_('mfTileId'), 10) || 0,
                bgId: parseInt(valueOf_('mfBgId'), 10) || 0
            }, 'Đã lưu thông tin bản đồ');
        },

        addMob: function () {
            document.getElementById('mapMobEmpty').hidden = true;
            var body = document.getElementById('mapMobBody');
            var row = document.createElement('tbody');
            row.innerHTML = subRow([
                '<td><select class="map-mob-temp">' + this.mobOptions + '</select></td>',
                '<td><input type="number" class="map-mob-level" value="1" min="0" max="127"></td>',
                '<td><input type="number" class="map-mob-hp" value="0" min="0"></td>',
                '<td><input type="number" class="map-mob-x" value="0"></td>',
                '<td><input type="number" class="map-mob-y" value="0"></td>'
            ]);
            body.appendChild(row.firstChild);
            this.renderCounts();
        },

        addNpc: function () {
            document.getElementById('mapNpcEmpty').hidden = true;
            var body = document.getElementById('mapNpcBody');
            var row = document.createElement('tbody');
            row.innerHTML = subRow([
                '<td><select class="map-npc-id">' + this.npcOptions + '</select></td>',
                '<td><input type="number" class="map-npc-x" value="0"></td>',
                '<td><input type="number" class="map-npc-y" value="0"></td>'
            ]);
            body.appendChild(row.firstChild);
            this.renderCounts();
        },

        addWaypoint: function () {
            document.getElementById('mapWpEmpty').hidden = true;
            var body = document.getElementById('mapWpBody');
            var row = document.createElement('tbody');
            row.innerHTML = subRow([
                '<td><input type="text" class="map-wp-name" maxlength="32" placeholder="Tên cổng"></td>',
                '<td><input type="number" class="map-wp-minx" value="0"></td>',
                '<td><input type="number" class="map-wp-miny" value="0"></td>',
                '<td><input type="number" class="map-wp-maxx" value="0"></td>',
                '<td><input type="number" class="map-wp-maxy" value="0"></td>',
                '<td><input type="checkbox" class="map-wp-enter"></td>',
                '<td><input type="checkbox" class="map-wp-offline"></td>',
                '<td><select class="map-wp-gomap">' + this.mapOptions + '</select></td>',
                '<td><input type="number" class="map-wp-gox" value="0"></td>',
                '<td><input type="number" class="map-wp-goy" value="0"></td>'
            ]);
            body.appendChild(row.firstChild);
            this.renderCounts();
        },

        /** Số trên tab luôn khớp số dòng đang có trên bảng. */
        renderCounts: function () {
            document.getElementById('mapTabMobCount').textContent = countRows('#mapMobBody');
            document.getElementById('mapTabNpcCount').textContent = countRows('#mapNpcBody');
            document.getElementById('mapTabWpCount').textContent = countRows('#mapWpBody');
        },

        saveMobs: function () {
            var rows = [];
            Array.prototype.forEach.call(document.querySelectorAll('#mapMobBody tr'), function (tr) {
                rows.push({
                    tempId: parseInt(tr.querySelector('.map-mob-temp').value, 10) || 0,
                    level: parseInt(tr.querySelector('.map-mob-level').value, 10) || 0,
                    hp: parseInt(tr.querySelector('.map-mob-hp').value, 10) || 0,
                    x: parseInt(tr.querySelector('.map-mob-x').value, 10) || 0,
                    y: parseInt(tr.querySelector('.map-mob-y').value, 10) || 0
                });
            });
            this.save('spawns', { mobs: rows }, 'Đã lưu danh sách quái');
        },

        saveNpcs: function () {
            var rows = [];
            Array.prototype.forEach.call(document.querySelectorAll('#mapNpcBody tr'), function (tr) {
                rows.push({
                    npcId: parseInt(tr.querySelector('.map-npc-id').value, 10) || 0,
                    x: parseInt(tr.querySelector('.map-npc-x').value, 10) || 0,
                    y: parseInt(tr.querySelector('.map-npc-y').value, 10) || 0
                });
            });
            this.save('spawns', { npcs: rows }, 'Đã lưu danh sách NPC');
        },

        saveWaypoints: function () {
            var rows = [];
            var missing = false;
            Array.prototype.forEach.call(document.querySelectorAll('#mapWpBody tr'), function (tr) {
                var name = tr.querySelector('.map-wp-name').value.trim();
                if (!name) missing = true;
                rows.push({
                    name: name,
                    minX: parseInt(tr.querySelector('.map-wp-minx').value, 10) || 0,
                    minY: parseInt(tr.querySelector('.map-wp-miny').value, 10) || 0,
                    maxX: parseInt(tr.querySelector('.map-wp-maxx').value, 10) || 0,
                    maxY: parseInt(tr.querySelector('.map-wp-maxy').value, 10) || 0,
                    isEnter: tr.querySelector('.map-wp-enter').checked,
                    isOffline: tr.querySelector('.map-wp-offline').checked,
                    goMap: parseInt(tr.querySelector('.map-wp-gomap').value, 10) || 0,
                    goX: parseInt(tr.querySelector('.map-wp-gox').value, 10) || 0,
                    goY: parseInt(tr.querySelector('.map-wp-goy').value, 10) || 0
                });
            });
            if (missing) { toast('Có cổng chưa nhập tên', 'error'); return; }
            this.save('waypoints', { waypoints: rows }, 'Đã lưu cổng dịch chuyển');
        },

        sync: async function () {
            var res = await this.post('reload', {});
            if (res) toast(res.message, 'success');
        }
    };

    /** Chọn đúng giá trị cho các <select> vừa dựng bằng innerHTML. */
    function applyRowSelects(selector) {
        Array.prototype.forEach.call(document.querySelectorAll(selector), function (el) {
            var value = el.dataset.value || '0';
            setSelectValue(el, value, value);
        });
    }

    function countRows(selector) {
        return document.querySelectorAll(selector + ' tr').length;
    }

    /* ======================== 4g. QUẢN LÝ MẪU QUÁI ====================== */

    var mobPanel = {
        active: false,
        index: null,
        detail: null,
        catalog: null,
        // null = đang thêm mới; 0 là id hợp lệ nên không được dùng so sánh kiểu truthy
        mobId: null,

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        load: async function () {
            var type = valueOf_('mobTypeFilter');
            try {
                this.index = (await PanelAPI.mob.list({
                    q: valueOf_('mobSearch'),
                    type: type
                })) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách quái: ' + e.message, 'error');
                return;
            }

            fillFilter('mobTypeFilter', (this.index.types || []).map(function (t) {
                return { value: t.id, label: t.name };
            }), 'Tất cả kiểu quái');
            setSelect('mobTypeFilter', type);
            this.renderList();
        },

        renderList: function () {
            var data = this.index || {};
            var rows = data.mobs || [];
            var ground = 0;
            var fly = 0;
            var used = 0;
            rows.forEach(function (row) {
                if (row.type === 1) ground++;
                if (row.type === 4) fly++;
                if (row.mapCount > 0) used++;
            });

            setText('mbTotal', formatPower(data.total || 0));
            setText('mbGround', formatPower(ground));
            setText('mbFly', formatPower(fly));
            setText('mbUsed', formatPower(used));

            document.getElementById('mobEmpty').hidden = rows.length > 0;
            document.getElementById('mobBody').innerHTML = rows.map(function (row) {
                return '<tr>'
                    + '<td class="num">' + row.id + '</td>'
                    + '<td><span class="mob-name">' + escapeHtml(row.name) + '</span></td>'
                    + '<td>' + escapeHtml(row.typeName) + '</td>'
                    + '<td class="num">' + formatPower(row.hp) + '</td>'
                    + '<td class="num">' + row.rangeMove + '</td>'
                    + '<td class="num">' + row.speed + '</td>'
                    + '<td class="num">' + row.dartType + '</td>'
                    + '<td class="num">' + row.percentDame + '</td>'
                    + '<td class="num">' + row.percentTiemNang + '</td>'
                    + '<td class="num">' + row.percentGold + '</td>'
                    + '<td class="num">' + row.mapCount + '</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-mob-edit="'
                    + row.id + '">Sửa</button></td>'
                    + '</tr>';
            }).join('');

            var hint = document.getElementById('mobHint');
            hint.textContent = 'Không có chức năng xoá: client đánh số mẫu quái theo vị trí trong danh sách'
                + ' server gửi xuống, xoá một dòng ở giữa sẽ làm mọi quái phía sau hiển thị sai.';
            hint.hidden = false;
        },

        open: async function (id, isNew) {
            if (isNew) {
                this.detail = null;
                this.mobId = null;
                // Danh sách kiểu quái đã có sẵn từ lần tải bảng, không cần gọi lại API
                this.catalog = { types: (this.index && this.index.types) || [] };
                document.getElementById('mobModalTitle').textContent = 'THÊM MẪU QUÁI #'
                    + ((this.index && this.index.nextId) || 0);
                document.getElementById('mobModalState').textContent = 'Mẫu quái mới';
                this.fillCatalog();
                this.fillFields({
                    name: '', type: 1, hp: 100, rangeMove: 100, speed: 1, dartType: 0,
                    percentDame: 100, percentTiemNang: 100, percentGold: 100
                }, []);
                document.getElementById('mobModal').hidden = false;
                return;
            }

            try {
                var data = await PanelAPI.mob.detail(id);
                this.detail = (data && data.mob) || null;
                this.catalog = (data && data.catalog) || {};
            } catch (e) {
                toast('Không đọc được quái #' + id + ': ' + e.message, 'error');
                return;
            }
            if (!this.detail) {
                toast('Không đọc được quái #' + id, 'error');
                return;
            }

            this.mobId = id;
            document.getElementById('mobModalTitle').textContent = 'SỬA MẪU QUÁI #' + id;
            document.getElementById('mobModalState').textContent = this.detail.name || '';
            this.fillCatalog();
            this.fillFields(this.detail, this.detail.maps || []);
            document.getElementById('mobModal').hidden = false;
        },

        close: function () {
            document.getElementById('mobModal').hidden = true;
        },

        fillCatalog: function () {
            var types = (this.catalog && this.catalog.types) || [];
            document.getElementById('mbfType').innerHTML = types.map(function (t) {
                return '<option value="' + t.id + '">' + escapeHtml(t.name) + '</option>';
            }).join('');
        },

        fillFields: function (mob, maps) {
            setInput('mbfName', mob.name);
            setSelect('mbfType', String(mob.type));
            setInput('mbfHp', mob.hp);
            setInput('mbfRangeMove', mob.rangeMove);
            setInput('mbfSpeed', mob.speed);
            setInput('mbfDartType', mob.dartType);
            setInput('mbfPercentDame', mob.percentDame);
            setInput('mbfPercentTiemNang', mob.percentTiemNang);
            setInput('mbfPercentGold', mob.percentGold);

            setText('mbfMaps', maps.length
                ? ('Đang được đặt trên ' + maps.length + ' bản đồ: '
                    + maps.map(function (m) { return m.name; }).join(', '))
                : 'Mẫu quái này chưa được đặt trên bản đồ nào.');
        },

        save: async function () {
            var body = {
                name: valueOf_('mbfName'),
                type: parseInt(valueOf_('mbfType'), 10) || 0,
                hp: parseInt(valueOf_('mbfHp'), 10) || 0,
                rangeMove: parseInt(valueOf_('mbfRangeMove'), 10) || 0,
                speed: parseInt(valueOf_('mbfSpeed'), 10) || 0,
                dartType: parseInt(valueOf_('mbfDartType'), 10) || 0,
                percentDame: parseInt(valueOf_('mbfPercentDame'), 10) || 0,
                percentTiemNang: parseInt(valueOf_('mbfPercentTiemNang'), 10) || 0,
                percentGold: parseInt(valueOf_('mbfPercentGold'), 10) || 0
            };
            if (!body.name) { toast('Chưa nhập tên quái', 'error'); return; }

            var isNew = this.mobId === null;
            var action = isNew ? 'mob-create' : 'mob-update';
            if (!isNew) body.id = this.mobId;

            try {
                var res = await PanelAPI.mob.run(action, body);
                toast(res.message || 'Đã lưu mẫu quái', 'success');
                this.close();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /* ======================= 4h. QUẢN LÝ NỘI TẠI ======================== */

    var intrinsicPanel = {
        active: false,
        index: null,
        detail: null,
        catalog: null,
        // null = đang thêm mới; 0 là id hợp lệ nên không được dùng so sánh kiểu truthy
        intrinsicId: null,

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        load: async function () {
            var gender = valueOf_('intrinsicGenderFilter');
            try {
                this.index = (await PanelAPI.intrinsic.list({
                    q: valueOf_('intrinsicSearch'),
                    gender: gender
                })) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách nội tại: ' + e.message, 'error');
                return;
            }

            fillFilter('intrinsicGenderFilter', (this.index.genders || []).map(function (g) {
                return { value: g.id, label: g.name };
            }), 'Tất cả hệ');
            setSelect('intrinsicGenderFilter', gender);
            this.renderList();
        },

        renderList: function () {
            var data = this.index || {};
            var rows = data.intrinsics || [];
            var shared = 0;
            var own = 0;
            var carried = 0;
            rows.forEach(function (row) {
                if (row.gender > 2) shared++; else own++;
                carried += row.playerCount;
            });

            setText('itTotal', formatPower(data.total || 0));
            setText('itShared', formatPower(shared));
            setText('itOwn', formatPower(own));
            setText('itPlayers', formatPower(carried));

            document.getElementById('intrinsicEmpty').hidden = rows.length > 0;
            document.getElementById('intrinsicBody').innerHTML = rows.map(function (row) {
                var icon = row.icon > 0
                    ? '<img class="intrinsic-icon" src="/game-data/icon/x1/' + row.icon
                        + '.png" alt="" onerror="this.remove()">'
                    : '';
                return '<tr>'
                    + '<td class="num">' + row.id + '</td>'
                    + '<td>' + icon + ' <span class="muted">#' + row.icon + '</span></td>'
                    + '<td><span class="intrinsic-name">' + escapeHtml(row.description.split('\n')[0])
                        + '</span><span class="intrinsic-raw">' + escapeHtml(row.name) + '</span></td>'
                    + '<td class="param-range">' + row.paramFrom1 + ' → <b>' + row.paramTo1 + '</b></td>'
                    + '<td class="param-range">' + row.paramFrom2 + ' → <b>' + row.paramTo2 + '</b></td>'
                    + '<td>' + escapeHtml(row.genderName) + '</td>'
                    + '<td class="num">' + row.playerCount + '</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-intrinsic-edit="'
                    + row.id + '">Sửa</button></td>'
                    + '</tr>';
            }).join('');
        },

        open: async function (id, isNew) {
            if (isNew) {
                this.intrinsicId = null;
                this.detail = null;
                this.catalog = this.index || {};
                document.getElementById('intrinsicModalTitle').textContent = 'THÊM NỘI TẠI #'
                    + ((this.index && this.index.nextId) || 0);
                document.getElementById('intrinsicModalState').textContent = 'Nội tại mới';
                this.prepareCatalog();
                this.fillFields({
                    name: '', paramFrom1: 0, paramTo1: 0, paramFrom2: 0, paramTo2: 0,
                    icon: 0, gender: 3, genderName: 'Cả ba hệ'
                }, []);
                document.getElementById('intrinsicModal').hidden = false;
                return;
            }

            try {
                var res = await PanelAPI.intrinsic.detail(id);
                this.detail = (res && res.intrinsic) || null;
                this.catalog = (res && res.catalog) || {};
            } catch (e) {
                toast('Không đọc được nội tại #' + id + ': ' + e.message, 'error');
                return;
            }
            if (!this.detail) {
                toast('Không đọc được nội tại #' + id, 'error');
                return;
            }

            this.intrinsicId = id;
            document.getElementById('intrinsicModalTitle').textContent = 'SỬA NỘI TẠI #' + id;
            document.getElementById('intrinsicModalState').textContent = this.detail.genderName;
            this.prepareCatalog();
            this.fillFields(this.detail, this.detail.players || []);
            document.getElementById('intrinsicModal').hidden = false;
        },

        close: function () {
            document.getElementById('intrinsicModal').hidden = true;
        },

        prepareCatalog: function () {
            var genders = (this.catalog && this.catalog.genders) || [];
            document.getElementById('itfGender').innerHTML = genders.map(function (g) {
                return '<option value="' + g.id + '">' + escapeHtml(g.name) + '</option>';
            }).join('');
        },

        fillFields: function (intrinsic, players) {
            setInput('itfName', intrinsic.name);
            setInput('itfFrom1', intrinsic.paramFrom1);
            setInput('itfTo1', intrinsic.paramTo1);
            setInput('itfFrom2', intrinsic.paramFrom2);
            setInput('itfTo2', intrinsic.paramTo2);
            setInput('itfIcon', intrinsic.icon);
            setSelect('itfGender', String(intrinsic.gender));
            intrinsicPanel.previewIcon();
            intrinsicPanel.previewText();

            setText('itfPlayerCount', players.length + ' nhân vật');
            document.getElementById('itfPlayers').innerHTML = players.length
                ? players.map(function (row) {
                    return '<div class="player-line"><b>#' + row.id + ' ' + escapeHtml(row.name)
                        + '</b><span class="player-line__meta">tham số ' + row.param1 + ' / ' + row.param2
                        + '</span></div>';
                }).join('')
                : '<p class="muted">Chưa nhân vật nào mang nội tại này.</p>';
        },

        previewIcon: function () {
            var icon = parseInt(valueOf_('itfIcon'), 10) || 0;
            document.getElementById('itfIconThumb').innerHTML = icon > 0
                ? '<img src="/game-data/icon/x1/' + icon + '.png" alt="" onerror="this.remove()">'
                : '';
        },

        /** Dựng trước câu mô tả y như game hiển thị cho người chơi. */
        previewText: function () {
            var name = valueOf_('itfName');
            var text = String(name)
                .replace(/p0/g, valueOf_('itfFrom1') || '0')
                .replace(/p1/g, valueOf_('itfTo1') || '0')
                .replace(/p2/g, valueOf_('itfFrom2') || '0')
                .replace(/p3/g, valueOf_('itfTo2') || '0');
            setText('itfPreview', 'Hiện trong game: ' + (text || '—'));
        },

        save: async function () {
            var name = valueOf_('itfName');
            if (!name) { toast('Chưa nhập tên nội tại', 'error'); return; }
            var body = {
                name: name,
                paramFrom1: parseInt(valueOf_('itfFrom1'), 10) || 0,
                paramTo1: parseInt(valueOf_('itfTo1'), 10) || 0,
                paramFrom2: parseInt(valueOf_('itfFrom2'), 10) || 0,
                paramTo2: parseInt(valueOf_('itfTo2'), 10) || 0,
                icon: parseInt(valueOf_('itfIcon'), 10) || 0,
                gender: parseInt(valueOf_('itfGender'), 10)
            };
            var isNew = this.intrinsicId === null;
            var action = isNew ? 'intrinsic-create' : 'intrinsic-update';
            if (!isNew) body.id = this.intrinsicId;
            try {
                var res = await PanelAPI.intrinsic.run(action, body);
                toast(res.message || 'Đã lưu nội tại', 'success');
                this.close();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        remove: async function () {
            if (this.intrinsicId === null) { toast('Nội tại mới chưa lưu, không cần xoá', 'error'); return; }
            if (!window.confirm('Xoá nội tại #' + this.intrinsicId + '?\n\n'
                    + 'Nhân vật nào đang mang nội tại này sẽ không cho xoá.')) return;
            try {
                var res = await PanelAPI.intrinsic.run('intrinsic-delete', { id: this.intrinsicId });
                toast(res.message || 'Đã xoá nội tại', 'success');
                this.close();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /* ====================== 4i. QUẢN LÝ TÀI KHOẢN ======================= */

    var accountPanel = {
        active: false,
        index: null,
        detail: null,
        catalog: null,
        accountId: 0,
        activeTab: 'info',

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        load: async function () {
            var filter = valueOf_('accountFilter');
            try {
                this.index = (await PanelAPI.account.list({
                    q: valueOf_('accountSearch'),
                    filter: filter
                })) || {};
            } catch (e) {
                this.index = null;
                toast('Không đọc được danh sách tài khoản: ' + e.message, 'error');
                return;
            }

            fillFilter('accountFilter', (this.index.filters || []).slice(1).map(function (f) {
                return { value: f.id, label: f.name };
            }), 'Tất cả tài khoản');
            setSelect('accountFilter', filter);
            this.renderList();
        },

        renderList: function () {
            var data = this.index || {};
            var rows = data.accounts || [];
            var banned = 0;
            var admins = 0;
            rows.forEach(function (row) {
                if (row.ban) banned++;
                if (row.is_admin) admins++;
            });

            setText('acTotal', formatPower(data.total || 0));
            setText('acOnline', formatPower(data.online || 0));
            setText('acBanned', formatPower(banned));
            setText('acAdmin', formatPower(admins));

            document.getElementById('accountEmpty').hidden = rows.length > 0;
            document.getElementById('accountBody').innerHTML = rows.map(function (row) {
                var tags = [];
                if (row.is_admin) tags.push('<span class="role-tag role-tag--admin">GAME ADMIN</span>');
                if (row.active) tags.push('<span class="role-tag role-tag--mtv">THÀNH VIÊN</span>');
                if (row.vip > 0) tags.push('<span class="role-tag role-tag--vip">VIP ' + row.vip + '</span>');
                if (!tags.length) tags.push('<span class="role-tag">Thường</span>');

                var state = [];
                if (row.online) state.push('<span class="type-badge type-badge--live">Online</span>');
                else state.push('<span class="type-badge type-badge--off">Offline</span>');
                if (row.ban) state.push('<span class="type-badge type-badge--dead">Đang khoá</span>');

                return '<tr>'
                    + '<td class="num">' + row.id + '</td>'
                    + '<td><span class="acct-name">' + escapeHtml(row.username) + '</span>'
                    + '<span class="acct-sub">Tạo ' + escapeHtml(row.created || '—') + '</span></td>'
                    + '<td><div class="role-tags">' + tags.join('') + '</div></td>'
                    + '<td class="num">' + row.charCount + '</td>'
                    + '<td class="num">' + formatPower(row.danap) + '</td>'
                    + '<td><div class="money-cell">'
                    + '<span><b>' + formatPower(row.cash) + '</b><i>Cash</i></span>'
                    + '<span><b>' + formatPower(row.sotien) + '</b><i>Số tiền (sotien)</i></span>'
                    + '<span><b>' + formatPower(row.thoiVang) + '</b><i>thỏi vàng</i></span>'
                    + '</div></td>'
                    + '<td><div class="role-tags">' + state.join('') + '</div></td>'
                    + '<td>' + escapeHtml(row.ipAddress || '—') + '</td>'
                    + '<td>' + escapeHtml(row.lastLogin || '—') + '</td>'
                    + '<td><div class="player-acts">'
                    + '<button class="btn-mini btn-mini--dark" type="button" data-account-edit="'
                    + row.id + '">Sửa</button>'
                    + (row.online
                        ? '<button class="btn-mini btn-mini--danger" type="button" data-account-kick="'
                            + row.id + '">Kick</button>'
                        : '')
                    + '</div></td>'
                    + '</tr>';
            }).join('');
        },

        /* -------------------------- mở / đóng modal -------------------------- */

        open: async function (id, tab) {
            try {
                var res = await PanelAPI.account.detail(id);
                this.detail = (res && res.account) || null;
                this.characters = (res && res.characters) || [];
                this.catalog = (res && res.catalog) || {};
            } catch (e) {
                toast('Không đọc được tài khoản #' + id + ': ' + e.message, 'error');
                return;
            }
            if (!this.detail) {
                toast('Không đọc được tài khoản #' + id, 'error');
                return;
            }

            this.accountId = id;
            this.prepareCatalog();
            this.fillInfo();
            this.fillRights();
            this.fillCharacters();

            document.getElementById('accountModalTitle').textContent = 'TÀI KHOẢN "'
                + this.detail.username + '" — #' + id;
            document.getElementById('accountModalState').innerHTML = this.detail.online
                ? '<span class="is-live">● Đang online</span>'
                : '○ Đang offline';
            document.getElementById('accountModal').hidden = false;
            this.showTab(tab || this.activeTab || 'info');
        },

        close: function () {
            document.getElementById('accountModal').hidden = true;
        },

        showTab: function (name) {
            this.activeTab = name;
            Array.prototype.forEach.call(document.querySelectorAll('#accountTabs .tab'), function (btn) {
                btn.classList.toggle('is-active', btn.dataset.accountTab === name);
            });
            Array.prototype.forEach.call(document.querySelectorAll('#accountModal [data-account-pane]'), function (pane) {
                pane.classList.toggle('is-active', pane.dataset.accountPane === name);
            });
        },

        prepareCatalog: function () {
            var fields = (this.catalog && this.catalog.moneyFields) || [];
            document.getElementById('acfMoneyField').innerHTML = fields.map(function (f) {
                return '<option value="' + f.id + '">' + escapeHtml(f.name) + '</option>';
            }).join('');
        },

        fillInfo: function () {
            var account = this.detail || {};
            setInput('acfUsername', account.username);
            setInput('acfIp', account.ipAddress);
            setInput('acfAgeDays', account.accountAgeDays);
            setInput('acfBdPlayer', account.bdPlayer);
            setInput('acfServerLogin', account.serverLogin);
            setInput('acfMkc2', account.mkc2);
            setInput('acfEmail', account.email);
            setText('acfTimes', 'Tạo: ' + (account.created || '—')
                + ' · Đăng nhập cuối: ' + (account.lastLogin || '—'));
        },

        fillRights: function () {
            var account = this.detail || {};
            setCheck('acfAdmin', account.is_admin);
            setCheck('acfActive', account.active);
            setCheck('acfBan', account.ban);
            setInput('acfVip', account.vip);
            setInput('acfCash', account.cash);
            setInput('acfDanap', account.danap);
            setInput('acfCoin', account.coin);
            setInput('acfSotien', account.sotien);
            setInput('acfThoiVang', account.thoiVang);
            setInput('acfPassword', '');
            setInput('acfPassword2', '');
        },

        fillCharacters: function () {
            var chars = this.characters || [];
            setText('acfCharCount', chars.length + ' nhân vật');
            document.getElementById('acfCharEmpty').hidden = chars.length > 0;
            document.getElementById('acfCharBody').innerHTML = chars.map(function (row) {
                return '<tr>'
                    + '<td class="num">' + row.id + '</td>'
                    + '<td><b>' + escapeHtml(row.name) + '</b></td>'
                    + '<td>' + escapeHtml(row.genderName) + '</td>'
                    + '<td class="num">' + formatPower(row.power) + '</td>'
                    + '<td><div class="money-cell">'
                    + '<span><b>' + formatPower(row.gold) + '</b><i>vàng</i></span>'
                    + '<span><b>' + formatPower(row.gem) + '</b><i>ngọc</i></span>'
                    + '<span><b>' + formatPower(row.ruby) + '</b><i>hồng ngọc</i></span>'
                    + '</div></td>'
                    + '<td>' + (row.online
                        ? '<span class="type-badge type-badge--live">Online</span>'
                        : '<span class="type-badge type-badge--off">Offline</span>') + '</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-char-edit="'
                    + row.id + '">Sửa</button></td>'
                    + '</tr>';
            }).join('');
        },

        /* -------------------------------- ghi ------------------------------- */

        post: async function (action, body) {
            try {
                return await PanelAPI.account.run(action, body);
            } catch (e) {
                toast(e.message, 'error');
                return null;
            }
        },

        refresh: async function () {
            await this.open(this.accountId, this.activeTab);
            if (this.active) await this.load();
        },

        save: async function (action, body, message) {
            var res = await this.post(action, Object.assign({ id: this.accountId }, body));
            if (!res) return;
            toast(res.message || message, 'success');
            await this.refresh();
        },

        saveInfo: function () {
            this.save('account-update', {
                username: valueOf_('acfUsername'),
                ipAddress: valueOf_('acfIp'),
                accountAgeDays: parseInt(valueOf_('acfAgeDays'), 10) || 0,
                bdPlayer: parseFloat(valueOf_('acfBdPlayer')) || 0,
                serverLogin: parseInt(valueOf_('acfServerLogin'), 10) || 0,
                mkc2: valueOf_('acfMkc2'),
                email: valueOf_('acfEmail')
            }, 'Đã lưu thông tin tài khoản');
        },

        saveRights: function () {
            this.save('account-update', {
                is_admin: document.getElementById('acfAdmin').checked,
                active: document.getElementById('acfActive').checked,
                ban: document.getElementById('acfBan').checked,
                vip: parseInt(valueOf_('acfVip'), 10) || 0
            }, 'Đã lưu quyền và trạng thái');
        },

        saveMoney: function () {
            this.save('account-update', {
                cash: parseInt(valueOf_('acfCash'), 10) || 0,
                danap: parseInt(valueOf_('acfDanap'), 10) || 0,
                coin: parseInt(valueOf_('acfCoin'), 10) || 0,
                sotien: parseInt(valueOf_('acfSotien'), 10) || 0,
                thoiVang: parseInt(valueOf_('acfThoiVang'), 10) || 0
            }, 'Đã lưu ví tài khoản');
        },

        money: async function (add) {
            var field = valueOf_('acfMoneyField');
            var amount = parseInt(valueOf_('acfMoneyAmount'), 10) || 0;
            if (amount <= 0) { toast('Số lượng phải là số dương', 'error'); return; }
            var res = await this.post('money', {
                id: this.accountId, field: field, amount: amount, add: add
            });
            if (!res) return;
            toast(res.message, 'success');
            await this.refresh();
        },

        savePassword: async function () {
            var password = valueOf_('acfPassword');
            if (password !== valueOf_('acfPassword2')) {
                toast('Hai lần nhập mật khẩu không giống nhau', 'error');
                return;
            }
            if (password.length < 3) { toast('Mật khẩu phải có ít nhất 3 ký tự', 'error'); return; }
            var res = await this.post('password', { id: this.accountId, password: password });
            if (!res) return;
            toast(res.message, 'success');
            await this.refresh();
        },

        kick: async function (id) {
            var target = this.findAccount(id);
            var label = target ? ('tài khoản "' + target.username + '"') : ('tài khoản #' + id);
            if (!window.confirm('Ngắt kết nối ' + label + '?')) return;
            var res = await this.post('kick', { id: id });
            if (!res) return;
            toast(res.message, 'success');
            if (this.accountId === id) await this.refresh();
            await this.load();
        },

        findAccount: function (id) {
            var found = null;
            ((this.index && this.index.accounts) || []).forEach(function (row) {
                if (row.id === id) found = row;
            });
            return found;
        },

        remove: async function () {
            if (!this.accountId) return;
            var chars = (this.characters || []).length;
            if (!window.confirm('XOÁ VĨNH VIỄN tài khoản "' + this.detail.username + '" (#'
                    + this.accountId + ')?\n\nSẽ xoá luôn ' + chars + ' nhân vật cùng dữ liệu con của họ,'
                    + ' lịch sử nạp của tài khoản — không thể hoàn tác.')) return;
            var res = await this.post('account-delete', { id: this.accountId });
            if (!res) return;
            toast(res.message, 'success');
            this.accountId = 0;
            this.close();
            await this.load();
        },

        create: async function (event) {
            event.preventDefault();
            var username = valueOf_('acCreateUser');
            var password = valueOf_('acCreatePass');
            try {
                var res = await PanelAPI.account.run('account-create', {
                    username: username,
                    password: password,
                    ipAddress: valueOf_('acCreateIp')
                });
                toast(res.message || 'Đã tạo tài khoản', 'success');
                document.getElementById('accountCreateForm').reset();
                document.getElementById('accountCreateModal').hidden = true;
                await this.load();
                if (res && res.id) await this.open(res.id);
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /* ---- Sự kiện 4 trang mới ---- */

    var mapModal = document.getElementById('mapModal');
    var mobModal = document.getElementById('mobModal');
    var intrinsicModal = document.getElementById('intrinsicModal');
    var accountModal = document.getElementById('accountModal');
    var accountCreateModal = document.getElementById('accountCreateModal');

    /* Bản đồ */
    document.getElementById('view-map').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-map-edit]');
        if (edit) mapPanel.open(parseInt(edit.dataset.mapEdit, 10));
    });
    document.getElementById('mapReload').addEventListener('click', function () { mapPanel.load(); });
    document.getElementById('mapSync').addEventListener('click', function () { mapPanel.sync(); });
    document.getElementById('mapTypeFilter').addEventListener('change', function () { mapPanel.load(); });
    document.getElementById('mapPlanetFilter').addEventListener('change', function () { mapPanel.load(); });
    debounceInput('mapSearch', function () { mapPanel.load(); });

    mapModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-map-close]')) { mapPanel.close(); return; }
        var tab = e.target.closest('[data-map-tab]');
        if (tab) { mapPanel.showTab(tab.dataset.mapTab); return; }
        var del = e.target.closest('[data-row-del]');
        if (del) {
            var row = del.closest('tr');
            if (row) row.remove();
            mapPanel.renderCounts();
        }
    });
    document.getElementById('mfSave').addEventListener('click', function () { mapPanel.saveInfo(); });
    document.getElementById('mapMobAdd').addEventListener('click', function () { mapPanel.addMob(); });
    document.getElementById('mapNpcAdd').addEventListener('click', function () { mapPanel.addNpc(); });
    document.getElementById('mapWpAdd').addEventListener('click', function () { mapPanel.addWaypoint(); });
    document.getElementById('mapMobSave').addEventListener('click', function () { mapPanel.saveMobs(); });
    document.getElementById('mapNpcSave').addEventListener('click', function () { mapPanel.saveNpcs(); });
    document.getElementById('mapWpSave').addEventListener('click', function () { mapPanel.saveWaypoints(); });

    /* Mẫu quái */
    document.getElementById('view-mob').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-mob-edit]');
        if (edit) mobPanel.open(parseInt(edit.dataset.mobEdit, 10));
    });
    document.getElementById('mobReload').addEventListener('click', function () { mobPanel.load(); });
    document.getElementById('mobTypeFilter').addEventListener('change', function () { mobPanel.load(); });
    debounceInput('mobSearch', function () { mobPanel.load(); });
    document.getElementById('mobAdd').addEventListener('click', function () { mobPanel.open(0, true); });
    mobModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-mob-close]')) mobPanel.close();
    });
    document.getElementById('mbfSave').addEventListener('click', function () { mobPanel.save(); });

    /* Nội tại */
    document.getElementById('view-intrinsic').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-intrinsic-edit]');
        if (edit) intrinsicPanel.open(parseInt(edit.dataset.intrinsicEdit, 10));
    });
    document.getElementById('intrinsicReload').addEventListener('click', function () { intrinsicPanel.load(); });
    document.getElementById('intrinsicGenderFilter').addEventListener('change', function () { intrinsicPanel.load(); });
    debounceInput('intrinsicSearch', function () { intrinsicPanel.load(); });
    document.getElementById('intrinsicAdd').addEventListener('click', function () { intrinsicPanel.open(0, true); });
    intrinsicModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-intrinsic-close]')) intrinsicPanel.close();
    });
    document.getElementById('itfSave').addEventListener('click', function () { intrinsicPanel.save(); });
    document.getElementById('itfDelete').addEventListener('click', function () { intrinsicPanel.remove(); });
    document.getElementById('itfIcon').addEventListener('input', function () { intrinsicPanel.previewIcon(); });
    ['itfName', 'itfFrom1', 'itfTo1', 'itfFrom2', 'itfTo2'].forEach(function (id) {
        document.getElementById(id).addEventListener('input', function () { intrinsicPanel.previewText(); });
    });

    /* Tài khoản */
    document.getElementById('view-account').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-account-edit]');
        if (edit) { accountPanel.open(parseInt(edit.dataset.accountEdit, 10)); return; }
        var kick = e.target.closest('[data-account-kick]');
        if (kick) accountPanel.kick(parseInt(kick.dataset.accountKick, 10));
    });
    document.getElementById('accountReload').addEventListener('click', function () { accountPanel.load(); });
    document.getElementById('accountFilter').addEventListener('change', function () { accountPanel.load(); });
    debounceInput('accountSearch', function () { accountPanel.load(); });
    document.getElementById('accountAdd').addEventListener('click', function () {
        accountCreateModal.hidden = false;
        document.getElementById('acCreateUser').focus();
    });
    document.getElementById('accountCreateForm').addEventListener('submit', function (e) {
        accountPanel.create(e);
    });
    accountCreateModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-account-create-close]')) accountCreateModal.hidden = true;
    });
    accountModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-account-close]')) { accountPanel.close(); return; }
        var tab = e.target.closest('[data-account-tab]');
        if (tab) { accountPanel.showTab(tab.dataset.accountTab); return; }
        var open = e.target.closest('[data-char-edit]');
        if (open) {
            var playerId = parseInt(open.dataset.charEdit, 10);
            accountPanel.close();
            navigate('player');
            setTimeout(function () { player.open(playerId); }, 500);
        }
    });
    document.getElementById('acfSave').addEventListener('click', function () { accountPanel.saveInfo(); });
    document.getElementById('acfSaveRights').addEventListener('click', function () { accountPanel.saveRights(); });
    document.getElementById('acfSaveMoney').addEventListener('click', function () { accountPanel.saveMoney(); });
    document.getElementById('acfSavePassword').addEventListener('click', function () { accountPanel.savePassword(); });
    document.getElementById('acfMoneyAdd').addEventListener('click', function () { accountPanel.money(true); });
    document.getElementById('acfMoneySub').addEventListener('click', function () { accountPanel.money(false); });
    document.getElementById('acfKick').addEventListener('click', function () { accountPanel.kick(accountPanel.accountId); });
    document.getElementById('acfDelete').addEventListener('click', function () { accountPanel.remove(); });

    /** Ô tìm kiếm có chờ 300ms sau lần gõ cuối (dùng chung cho 4 trang mới). */
    function debounceInput(id, run) {
        var timer;
        var el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(run, 300);
        });
        el.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') return;
            clearTimeout(timer);
            run();
        });
    }

    /** Ghi giá trị vào input/select nếu người dùng không đang sửa. */
    function setInput(id, value) {
        var el = document.getElementById(id);
        if (el && document.activeElement !== el) el.value = value == null ? '' : value;
    }

    function setCheck(id, checked) {
        var el = document.getElementById(id);
        if (el && document.activeElement !== el) el.checked = !!checked;
    }

    function setSelect(id, value) {
        var el = document.getElementById(id);
        if (el) el.value = value;
    }

    /** Chọn giá trị trong <select>; giá trị lạ thì thêm một option để không bị mất dữ liệu. */
    function setSelectValue(target, value, label) {
        var el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;
        if (!el.querySelector('option[value="' + value + '"]')) {
            var option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            el.insertBefore(option, el.firstChild);
        }
        el.value = value;
    }

    /** Chỉ đồng bộ select khi người dùng chưa tự đổi giá trị. */
    function syncSelect(id, value) {
        var el = document.getElementById(id);
        if (!el || el.dataset.dirty || document.activeElement === el) return;
        setSelect(id, value);
    }

    function syncEvents(events, currentId, currentName) {
        var sel = document.getElementById('srvEventSelect');
        if (sel && !sel.dataset.filled && events.length) {
            sel.innerHTML = events.map(function (e) {
                return '<option value="' + e.id + '">' + escapeHtml(e.name) + '</option>';
            }).join('');
            sel.dataset.filled = '1';
        }
        syncSelect('srvEventSelect', String(currentId));
        setText('srvEventCurrent', currentName);
    }

    function valueOf_(id) {
        var el = document.getElementById(id);
        return el ? el.value.trim() : '';
    }

    function hourOf(time) {
        return (time || '23:59').split(':')[0];
    }

    function minuteOf(time) {
        return (time || '23:59').split(':')[1];
    }

    /**
     * Avatar boss: ảnh head nằm trong client nên server không có sẵn.
     * Thả ảnh vào web/assets/boss/<headID>.png là mọi boss có avatar ngay;
     * chưa có ảnh thì hiện vòng tròn chữ cái đầu (không gọi ảnh lỗi vì có cờ hasAvatar).
     */
    function bossAvatar(b) {
        var letter = escapeHtml(String(b.name || '?').trim().charAt(0).toUpperCase() || '?');
        var img = b.hasAvatar
            ? '<img src="assets/boss/' + b.head + '.png" alt="" loading="lazy" onerror="this.remove()">'
            : '';
        return '<span class="avatar" title="Bộ đồ: head ' + b.head + ' · body ' + b.body + ' · leg ' + b.leg + '">'
            + '<b>' + letter + '</b>' + img + '</span>';
    }

    function bossClass(status) {
        if (status === 'ACTIVE') return 'type-badge type-badge--live';
        if (status === 'DEAD') return 'type-badge type-badge--dead';
        return 'type-badge type-badge--wait';
    }

    var serverTabsEl = document.getElementById('serverTabs');

    function showServerTab(key) {
        Array.prototype.forEach.call(serverTabsEl.querySelectorAll('.tab'), function (tab) {
            tab.classList.toggle('is-active', tab.dataset.tab === key);
        });
        Array.prototype.forEach.call(document.querySelectorAll('#view-servers .tabpane'), function (pane) {
            pane.classList.toggle('is-active', pane.dataset.pane === key);
        });
        // Bảng bot tải theo yêu cầu vì server có thể có hàng nghìn bot
        if (key === 'bot' && !servers.bots) {
            servers.loadBots();
        }
    }

    serverTabsEl.addEventListener('click', function (e) {
        var btn = e.target.closest('.tab');
        if (btn) showServerTab(btn.dataset.tab);
    });

    document.getElementById('view-servers').addEventListener('click', function (e) {
        var summon = e.target.closest('[data-summon]');
        if (summon) {
            servers.task('boss-summon', { id: parseInt(summon.dataset.summon, 10) });
            return;
        }

        var del = e.target.closest('[data-bot-del]');
        if (del) {
            servers.removeBot(del.dataset.botGroup, parseInt(del.dataset.botDel, 10));
            return;
        }

        var kick = e.target.closest('[data-kick]');
        if (kick) {
            servers.task('kick', { id: parseInt(kick.dataset.kick, 10) },
                'Kick người chơi #' + kick.dataset.kick + ' khỏi máy chủ?');
            return;
        }

        var btn = e.target.closest('[data-task]');
        if (!btn) return;

        switch (btn.dataset.task) {
            case 'bot-create':
                servers.createBots();
                break;
            case 'bot-delete':
                servers.deleteBots();
                break;
            case 'bot-reload':
                servers.loadBots(true);
                break;
            case 'autosave-on':
                servers.task('autosave', { on: true });
                break;
            case 'autosave-off':
                servers.task('autosave', { on: false });
                break;
            case 'reset-boss':
                servers.task('reset-boss', undefined, 'Reset toàn bộ boss về trạng thái ban đầu?');
                break;
            case 'kick-all':
                servers.task('kick-all', undefined, 'Kick TẤT CẢ người chơi đang online khỏi máy chủ?');
                break;
            case 'reload-top':
                servers.task('reload-top', undefined, 'Nạp lại toàn bộ bảng xếp hạng? Việc này có thể tốn vài giây.');
                break;
            default:
                servers.task(btn.dataset.task);
        }
    });

    /* ---- Tab Thiết Lập Server ---- */

    var loginAccessControl = {
        active: false, busy: false, saving: false, known: null, sequence: 0,
        start: function () { this.active = true; if (!this.saving) this.load(); },
        stop: function () { this.active = false; this.sequence++; if (!this.saving) this.busy = false; },
        reflect: function (on) {
            if (this.busy || this.saving) return;
            this.known = on;
            document.getElementById('srvAdminOnly').checked = on;
            document.getElementById('srvAdminOnly').disabled = false;
            setText('srvAdminOnlyStatus', on ? 'Đang bật: chỉ admin được đăng nhập.' : 'Đang mở: tất cả tài khoản được đăng nhập.');
        },
        load: async function () {
            if (this.busy || this.saving) return;
            var request = ++this.sequence; this.busy = true;
            document.getElementById('srvAdminOnly').disabled = true;
            document.getElementById('srvAdminOnlyReload').disabled = true;
            setText('srvAdminOnlyStatus', 'Đang tải trạng thái…');
            try {
                var value = await PanelAPI.server.loginAccess();
                if (!this.active || request !== this.sequence) return;
                this.busy = false; this.reflect(value.adminOnly);
            } catch (error) {
                if (this.active && request === this.sequence) { this.known = null; setText('srvAdminOnlyStatus', error.message); }
            } finally {
                if (request === this.sequence) { this.busy = false; document.getElementById('srvAdminOnlyReload').disabled = false; }
            }
        },
        save: async function () {
            if (this.busy || this.saving || this.known === null) return;
            var control = document.getElementById('srvAdminOnly'), previous = this.known, on = control.checked;
            this.saving = true; control.disabled = true; document.getElementById('srvAdminOnlyReload').disabled = true;
            setText('srvAdminOnlyStatus', 'Đang lưu chế độ đăng nhập…');
            try {
                var value = await PanelAPI.server.setLoginAccess(on); this.known = value.adminOnly;
                toast(value.message);
            } catch (error) {
                this.known = previous; toast(error.message, 'error');
            } finally {
                this.saving = false; this.reflect(this.known); document.getElementById('srvAdminOnlyReload').disabled = false;
                if (this.active) this.load();
            }
        }
    };
    document.getElementById('srvAdminOnly').addEventListener('change', function () { loginAccessControl.save(); });
    document.getElementById('srvAdminOnlyReload').addEventListener('click', function () { loginAccessControl.load(); });

    document.getElementById('srvExpSave').addEventListener('click', function () {
        var rate = parseInt(document.getElementById('srvExpRate').value, 10);
        if (!(rate >= 1) || rate >= 32767) {
            toast('Tỷ lệ EXP phải trong khoảng 1 - 32766', 'error');
            return;
        }
        servers.task('exp-rate', { rate: rate });
    });

    document.getElementById('srvEventSelect').addEventListener('change', function () {
        this.dataset.dirty = '1';
    });

    document.getElementById('srvEventApply').addEventListener('click', function () {
        servers.task('set-event', { id: parseInt(valueOf_('srvEventSelect'), 10) });
    });

    document.getElementById('srvMaintNow').addEventListener('click', function () {
        var seconds = parseInt(valueOf_('srvMaintSeconds'), 10);
        if (!(seconds >= 5) || seconds > 86400) {
            toast('Số giây bảo trì phải trong khoảng 5 - 86400', 'error');
            return;
        }
        servers.task('maintenance-now', { seconds: seconds },
            'Bắt đầu bảo trì ngay? Máy chủ sẽ đóng sau ' + seconds + ' giây.');
    });

    document.getElementById('srvMaintSave').addEventListener('click', function () {
        var hour = parseInt(valueOf_('srvMaintHour'), 10);
        var minute = parseInt(valueOf_('srvMaintMinute'), 10);
        if (!(hour >= 0) || hour > 23 || !(minute >= 0) || minute > 59) {
            toast('Giờ bảo trì không hợp lệ (0-23 giờ, 0-59 phút)', 'error');
            return;
        }
        servers.task('auto-maintenance', {
            on: document.getElementById('srvMaintEnable').checked
        });
    });

    document.getElementById('srvCurrency').addEventListener('change', function () {
        this.dataset.dirty = '1';
    });

    document.getElementById('srvCurrencyApply').addEventListener('click', function () {
        servers.task('currency', { int: valueOf_('srvCurrency') === '1' });
    });

    /* ---- Tab Quản Lý Boss / Bot ---- */

    document.getElementById('srvPlayerSearch').addEventListener('input', function () {
        servers.playerSearch = this.value;
        if (servers.data) servers.renderPlayers(servers.data.playerList || []);
    });

    document.getElementById('srvBossSearch').addEventListener('input', function () {
        servers.bossSearch = this.value;
        if (servers.data) servers.renderBosses(servers.data.bossList || []);
    });

    ['botFilterGroup', 'botFilterType'].forEach(function (id) {
        document.getElementById(id).addEventListener('change', function () { servers.loadBots(true); });
    });

    document.getElementById('botFilterSearch').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            servers.loadBots(true);
        }
    });

    /* ============ 4j. CẤU HÌNH JAVA PANEL (stored-only) ============ */

    /**
     * Hai trang dùng chung một API: "Cài Đặt Website" sửa nhóm `site` + `download`,
     * "Cấu Hình Nạp Tiền" sửa nhóm thẻ cào / PayOS / ngân hàng.
     */
    var WEBSITE_GROUPS = ['site', 'download'];

    var serverConfig = {
        active: false,
        data: null,

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
        },

        load: async function () {
            try {
                this.data = (await PanelAPI.website.config()) || {};
            } catch (e) {
                toast('Không đọc được cấu hình website: ' + e.message, 'error');
                return;
            }
            this.render();
        },

        render: function () {
            var data = this.data || {};
            var storage = data.storage || {};

            document.getElementById('wsFields').innerHTML = this.groupsHtml(data, WEBSITE_GROUPS);
            document.getElementById('payGroups').innerHTML = this.groupsHtml(data, null);

            var where = (storage.path || storage.table || 'website.json')
                + ' · ' + (storage.rows || 0) + ' biến · Java panel';
            setText('wsFilePath', where);
            setText('payFilePath', where);

            var warn = document.getElementById('wsHint');
            warn.textContent = 'Cấu hình được lưu bởi Java panel'
                + (storage.updated ? ' · cập nhật ' + storage.updated : '')
                + '. Các khoá website/thanh toán hiện chỉ được lưu trữ; chưa có consumer thanh toán hoặc website bên ngoài. Không sửa PHP và không tự xử lý nạp tiền.';

            this.renderBackups(data.backups || []);
            Array.prototype.forEach.call(document.querySelectorAll('.form-actions__status'), function (el) {
                if (el.id === 'wsStatus' || el.id === 'payStatus') setStatus(el, 'Chưa có thay đổi');
            });
        },

        /**
         * Các ô nhập gom theo nhóm. Truyền `only` thì lấy đúng những nhóm đó,
         * truyền null thì lấy hết phần còn lại (trang "Cấu Hình Nạp Tiền").
         */
        groupsHtml: function (data, only) {
            var fields = data.fields || [];
            var config = data.config || {};
            return (data.groups || []).filter(function (g) {
                return only ? only.indexOf(g.id) >= 0 : WEBSITE_GROUPS.indexOf(g.id) < 0;
            }).map(function (group) {
                var rows = fields.filter(function (f) { return f.group === group.id; });
                if (!rows.length) return '';
                return '<div class="group">'
                    + '<h3 class="group__title">' + escapeHtml(group.label) + '</h3>'
                    + '<div class="grid grid--2">'
                    + rows.map(function (f) { return serverConfig.fieldHtml(f, config); }).join('')
                    + '</div></div>';
            }).join('');
        },

        /** Một ô cấu hình Java panel; secret được server trả về dạng che. */
        fieldHtml: function (field, config) {
            var id = 'cfg_' + field.name;
            var value = config[field.name] === undefined ? '' : config[field.name];
            var type = field.secret ? 'password' : (field.kind === 'int' ? 'number' : 'text');
            var input = '<input type="' + type + '" id="' + id + '" data-cfg="'
                + escapeHtml(field.name) + '" value="' + escapeHtml(value)
                + '" autocomplete="off" spellcheck="false">';
            if (field.secret) {
                input = '<div class="input-group">' + input
                    + '<button class="mini-btn" type="button" data-cfg-reveal="' + id
                    + '" title="Hiện / ẩn giá trị">👁</button></div>';
            }
            return '<div class="field">'
                + '<label for="' + id + '">' + escapeHtml(field.label) + '</label>'
                + input
                + (field.hint ? '<p class="field__hint">' + escapeHtml(field.hint) + '</p>' : '')
                + '</div>';
        },

        renderBackups: function (backups) {
            document.getElementById('wsBackupEmpty').hidden = backups.length > 0;
            setText('wsBackupCount', backups.length + ' bản');
            document.getElementById('wsBackupBody').innerHTML = backups.map(function (row) {
                return '<tr>'
                    + '<td>' + escapeHtml(row.time) + '</td>'
                    + '<td><span class="dns-name">' + escapeHtml(row.name) + '</span></td>'
                    + '<td class="num">' + Math.max(1, Math.round(row.size / 1024)) + ' KB</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-cfg-restore="'
                    + escapeHtml(row.name) + '">Phục Hồi</button></td>'
                    + '</tr>';
            }).join('');
        },

        /** Chỉ lấy giá trị của những ô nằm trong một khung. */
        collect: function (containerId) {
            var values = {};
            Array.prototype.forEach.call(
                document.querySelectorAll('#' + containerId + ' [data-cfg]'),
                function (input) { values[input.dataset.cfg] = input.value; });
            return values;
        },

        save: async function (containerId, statusId) {
            var values = this.collect(containerId);
            if (!Object.keys(values).length) {
                toast('Chưa có ô nào để lưu', 'error');
                return;
            }
            var status = document.getElementById(statusId);
            setStatus(status, 'Đang lưu…', 'dirty');
            try {
                var res = await PanelAPI.website.save(values);
                toast(res.message || 'Đã lưu cấu hình', 'success');
                await this.load();
                setStatus(document.getElementById(statusId), res.message || 'Đã lưu', 'saved');
            } catch (e) {
                setStatus(status, e.message, 'dirty');
                toast(e.message, 'error');
            }
        },

        restore: async function (name) {
            if (!window.confirm('Phục hồi cấu hình Java panel từ bản sao lưu:\n\n' + name + '\n\n'
                    + 'Cấu hình hiện tại sẽ được sao lưu lại trước khi ghi đè. Không sửa website/PHP bên ngoài.')) return;
            try {
                var res = await PanelAPI.website.restore(name);
                toast(res.message || 'Đã phục hồi', 'success');
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    /** Trạng thái ở thanh form-actions (is-dirty = vàng, is-saved = xanh). */
    function setStatus(el, text, state) {
        if (!el) return;
        el.textContent = text || '';
        el.className = 'form-actions__status' + (state ? ' is-' + state : '');
    }

    /* ===================== 4k. QUẢN LÝ DNS CLOUDFLARE ===================== */

    var cfRecordModal = document.getElementById('cfRecordModal');

    var cloudflarePanel = {
        active: false,
        data: null,
        recordId: null,
        search: '',
        typeFilter: '',

        start: function () {
            this.active = true;
            this.load();
        },

        stop: function () {
            this.active = false;
            this.close();
        },

        load: async function () {
            try {
                this.data = (await PanelAPI.cloudflare.info()) || {};
            } catch (e) {
                toast('Không đọc được Cloudflare: ' + e.message, 'error');
                return;
            }
            this.render();
        },

        render: function () {
            var data = this.data || {};
            var records = data.records || [];
            var proxied = 0;
            var types = {};
            records.forEach(function (row) {
                if (row.proxied) proxied++;
                types[row.type] = true;
            });

            var connected = data.configured;
            setText('cfState', data.error ? 'Lỗi kết nối' : (connected ? 'Đã kết nối' : 'Chưa kết nối'));
            setText('cfZone', data.zoneName || '—');
            setText('cfTotal', formatPower(records.length));
            setText('cfProxied', formatPower(proxied));
            setText('cfStorage', data.storage || '—');

            var status = document.getElementById('cfStatus');
            if (data.error) setStatus(status, data.error, 'dirty');
            else setStatus(status, data.message || '', connected ? 'saved' : '');

            fillFilter('cfTypeFilter', Object.keys(types).map(function (type) {
                return { value: type, label: type };
            }), 'Tất cả loại');
            setSelect('cfTypeFilter', this.typeFilter);

            var zones = data.zones || [];
            var select = document.getElementById('cfZoneSelect');
            select.innerHTML = zones.length
                ? '<option value="">— chọn tên miền —</option>' + zones.map(function (zone) {
                    return '<option value="' + escapeHtml(zone.id) + '">' + escapeHtml(zone.name)
                        + (zone.status && zone.status !== 'active' ? ' (' + escapeHtml(zone.status) + ')' : '')
                        + '</option>';
                }).join('')
                : '<option value="">' + (data.tokenSet ? 'Không đọc được zone nào' : 'Chưa có token') + '</option>';
            select.disabled = !zones.length;
            setSelect('cfZoneSelect', data.zoneId || '');

            this.renderRecords();
        },

        renderRecords: function () {
            var records = ((this.data || {}).records) || [];
            var keyword = this.search.toLowerCase();
            var filter = this.typeFilter;
            var shown = records.filter(function (row) {
                if (filter && row.type !== filter) return false;
                if (!keyword) return true;
                return (row.name || '').toLowerCase().indexOf(keyword) >= 0
                    || (row.content || '').toLowerCase().indexOf(keyword) >= 0;
            });

            document.getElementById('cfEmpty').hidden = shown.length > 0;
            document.getElementById('cfBody').innerHTML = shown.map(function (row) {
                var proxy = row.proxied
                    ? '<span class="type-badge type-badge--live">Qua Cloudflare</span>'
                    : '<span class="type-badge type-badge--off">DNS only</span>';
                return '<tr>'
                    + '<td><b>' + escapeHtml(row.type) + '</b></td>'
                    + '<td><span class="dns-name">' + escapeHtml(row.name) + '</span></td>'
                    + '<td><span class="dns-content" title="' + escapeHtml(row.content) + '">'
                    + escapeHtml(row.content) + '</span></td>'
                    + '<td class="num">' + (row.ttl === 1 ? 'Tự động' : row.ttl) + '</td>'
                    + '<td>' + proxy + '</td>'
                    + '<td class="num">' + ((row.type === 'MX' || row.type === 'SRV') ? row.priority : '—')
                    + '</td>'
                    + '<td><button class="btn-mini btn-mini--dark" type="button" data-cf-edit="'
                    + escapeHtml(row.id) + '">Sửa</button></td>'
                    + '</tr>';
            }).join('');
        },

        /* ------------------------------ modal ------------------------------ */

        open: function (id) {
            var data = this.data || {};
            var record = null;
            if (id) {
                (data.records || []).forEach(function (row) { if (row.id === id) record = row; });
                if (!record) { toast('Không tìm thấy bản ghi', 'error'); return; }
            }
            if (record && (data.types || []).indexOf(record.type) < 0) {
                toast('Bản ghi ' + record.type + ' chỉ xem trong panel; biểu mẫu đơn giản không hỗ trợ sửa kiểu này.', 'error');
                return;
            }
            this.recordId = id || null;

            fillOptions('cfType', (data.types || []).map(function (type) {
                return { value: type, label: type };
            }));
            fillOptions('cfTtl', (data.ttlOptions || []).map(function (row) {
                return { value: row.value, label: row.label };
            }));

            document.getElementById('cfModalTitle').textContent =
                record ? 'SỬA BẢN GHI DNS' : 'THÊM BẢN GHI DNS';
            document.getElementById('cfModalState').textContent =
                record ? record.type + ' · ' + record.name : (data.zoneName || '');

            setSelect('cfType', record ? record.type : 'A');
            setInput('cfName', record ? record.name : '');
            setInput('cfContent', record ? record.content : '');
            setSelect('cfTtl', String(record ? record.ttl : 1));
            setInput('cfPriority', record ? record.priority : 10);
            document.getElementById('cfProxied').checked = !!(record && record.proxied);
            document.getElementById('cfDelete').hidden = !record;

            this.syncProxyable();
            cfRecordModal.hidden = false;
        },

        close: function () {
            cfRecordModal.hidden = true;
        },

        /** Chỉ A / AAAA / CNAME mới bật được proxy cam. */
        syncProxyable: function () {
            var type = valueOf_('cfType');
            var allowed = ((this.data || {}).proxyable) || [];
            var box = document.getElementById('cfProxied');
            var ok = allowed.indexOf(type) >= 0;
            box.disabled = !ok;
            if (!ok) box.checked = false;
        },

        save: async function () {
            var body = {
                type: valueOf_('cfType'),
                name: valueOf_('cfName').trim(),
                content: valueOf_('cfContent').trim(),
                ttl: parseInt(valueOf_('cfTtl'), 10) || 1,
                priority: parseInt(valueOf_('cfPriority'), 10) || 0,
                proxied: document.getElementById('cfProxied').checked
            };
            if (!body.name) { toast('Chưa nhập tên bản ghi', 'error'); return; }
            if (!body.content) { toast('Chưa nhập giá trị bản ghi', 'error'); return; }
            if (this.recordId) body.id = this.recordId;

            try {
                var res = await PanelAPI.cloudflare.saveRecord(body);
                toast(res.message || 'Đã lưu bản ghi', 'success');
                this.close();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        remove: async function () {
            if (!this.recordId) return;
            var name = valueOf_('cfName');
            if (!window.confirm('Xoá bản ghi DNS "' + name + '"?\n\nThao tác này không hoàn tác được.')) return;
            try {
                var res = await PanelAPI.cloudflare.deleteRecord(this.recordId);
                toast(res.message || 'Đã xoá bản ghi', 'success');
                this.close();
                await this.load();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        /* --------------------------- token / zone --------------------------- */

        saveToken: async function () {
            var token = valueOf_('cfToken').trim();
            if (!token) { toast('Chưa dán API Token', 'error'); return; }
            setStatus(document.getElementById('cfStatus'), 'Đang kiểm tra token…', 'dirty');
            try {
                var res = await PanelAPI.cloudflare.saveToken(token);
                document.getElementById('cfToken').value = '';
                toast(res.message || 'Đã lưu token', 'success');
                this.data = res;
                this.render();
            } catch (e) {
                setStatus(document.getElementById('cfStatus'), e.message, 'dirty');
                toast(e.message, 'error');
            }
        },

        useZone: async function () {
            var select = document.getElementById('cfZoneSelect');
            var zoneId = select.value;
            if (!zoneId) { toast('Chưa chọn zone', 'error'); return; }
            var zoneName = select.options[select.selectedIndex].textContent;
            try {
                var res = await PanelAPI.cloudflare.useZone(zoneId, zoneName);
                toast(res.message || 'Đã chọn zone', 'success');
                this.data = res;
                this.render();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        forget: async function () {
            if (!window.confirm('Xoá API Token Cloudflare khỏi panel?')) return;
            try {
                var res = await PanelAPI.cloudflare.forget();
                toast(res.message || 'Đã xoá token', 'success');
                this.data = res;
                this.render();
            } catch (e) {
                toast(e.message, 'error');
            }
        },

        purge: async function () {
            if (!window.confirm('Xoá toàn bộ cache của zone này?')) return;
            try {
                var res = await PanelAPI.cloudflare.purge();
                toast(res.message || 'Đã xoá cache', 'success');
                this.data = res;
                this.render();
            } catch (e) {
                toast(e.message, 'error');
            }
        }
    };

    document.getElementById('view-cloudflare').addEventListener('click', function (e) {
        var edit = e.target.closest('[data-cf-edit]');
        if (edit) { cloudflarePanel.open(edit.dataset.cfEdit); return; }
    });

    document.getElementById('view-website-settings').addEventListener('click', function (e) {
        var reveal = e.target.closest('[data-cfg-reveal]');
        if (reveal) {
            var input = document.getElementById(reveal.dataset.cfgReveal);
            input.type = input.type === 'password' ? 'text' : 'password';
            return;
        }
        var restore = e.target.closest('[data-cfg-restore]');
        if (restore) serverConfig.restore(restore.dataset.cfgRestore);
    });

    document.getElementById('view-payment').addEventListener('click', function (e) {
        var reveal = e.target.closest('[data-cfg-reveal]');
        if (reveal) {
            var input = document.getElementById(reveal.dataset.cfgReveal);
            input.type = input.type === 'password' ? 'text' : 'password';
        }
    });

    document.getElementById('wsSave').addEventListener('click', function () {
        serverConfig.save('wsFields', 'wsStatus');
    });
    document.getElementById('wsReload').addEventListener('click', function () { serverConfig.load(); });
    document.getElementById('paySave').addEventListener('click', function () {
        serverConfig.save('payGroups', 'payStatus');
    });
    document.getElementById('payReload').addEventListener('click', function () { serverConfig.load(); });

    document.getElementById('cfSaveToken').addEventListener('click', function () { cloudflarePanel.saveToken(); });
    document.getElementById('cfUseZone').addEventListener('click', function () { cloudflarePanel.useZone(); });
    document.getElementById('cfForget').addEventListener('click', function () { cloudflarePanel.forget(); });
    document.getElementById('cfPurge').addEventListener('click', function () { cloudflarePanel.purge(); });
    document.getElementById('cfReload').addEventListener('click', function () { cloudflarePanel.load(); });
    document.getElementById('cfAdd').addEventListener('click', function () { cloudflarePanel.open(null); });
    document.getElementById('cfSave').addEventListener('click', function () { cloudflarePanel.save(); });
    document.getElementById('cfDelete').addEventListener('click', function () { cloudflarePanel.remove(); });
    document.getElementById('cfType').addEventListener('change', function () { cloudflarePanel.syncProxyable(); });
    debounceInput('cfSearch', function () {
        cloudflarePanel.search = valueOf_('cfSearch');
        cloudflarePanel.renderRecords();
    });
    document.getElementById('cfTypeFilter').addEventListener('change', function () {
        cloudflarePanel.typeFilter = valueOf_('cfTypeFilter');
        cloudflarePanel.renderRecords();
    });
    cfRecordModal.addEventListener('click', function (e) {
        if (e.target.closest('[data-cf-close]')) cloudflarePanel.close();
    });

    /* ============================== 5. EXTRAS ============================= */
    var themeSwitch = document.getElementById('themeSwitch');

    function applyTheme(theme) {
        document.documentElement.dataset.theme = theme;
        Array.prototype.forEach.call(themeSwitch.querySelectorAll('button'), function (b) {
            b.classList.toggle('is-active', b.dataset.themeValue === theme);
        });
        try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* ignore */ }
    }

    themeSwitch.addEventListener('click', function (e) {
        var btn = e.target.closest('button[data-theme-value]');
        if (btn) applyTheme(btn.dataset.themeValue);
    });

    var toastEl = document.getElementById('toast');
    var toastTimer;

    function toast(message, kind) {
        // Đang ở màn hình đăng nhập thì không cần thêm thông báo lỗi 401 của từng module
        if (kind === 'error' && loginScreen && !loginScreen.hidden) {
            return;
        }
        toastEl.textContent = message;
        toastEl.className = 'toast is-visible' + (kind ? ' is-' + kind : '');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { toastEl.className = 'toast'; }, 2600);
    }

    var statusEl = document.getElementById('serverStatus');

    function applyStatusPill() {
        if (PanelAPI.online) {
            var info = PanelAPI.info || {};
            var port = info.port ? String(info.port) : (location.port || '8888');
            statusEl.className = 'pill pill--online';
            statusEl.innerHTML = '<i class="dot"></i> Port ' + port + ' Online';
        } else {
            statusEl.className = 'pill pill--offline';
            statusEl.innerHTML = PanelAPI.served
                ? '<i class="dot"></i> Offline — chưa kết nối server'
                : '<i class="dot"></i> Mở từ file — chưa có server';
            statusEl.title = PanelAPI.served
                ? 'Không gọi được API. Không có thay đổi nào được lưu cục bộ; hãy kiểm tra máy chủ.'
                : 'Chỉ xem giao diện. Chạy ServerManager -web và mở panel qua HTTP(S) để thao tác.';
        }
    }

    /* ============================ 6. ĐĂNG NHẬP =========================== */
    var loginScreen = document.getElementById('loginScreen');
    var loginForm = document.getElementById('loginForm');
    var loginError = document.getElementById('loginError');
    var loginUser = document.getElementById('loginUser');
    var loginPass = document.getElementById('loginPass');
    var loginSubmit = document.getElementById('loginSubmit');
    var loginRemember = document.getElementById('loginRemember');
    var currentUser = document.getElementById('currentUser');

    var auth = {
        /** Tên tài khoản đang đăng nhập (null khi mở từ file://) */
        username: null,

        /**
         * Gọi một lần trong boot: quyết định cho vào panel hay bắt đăng nhập.
         * Trả về true nếu được phép vào panel.
         */
        start: async function () {
            if (!PanelAPI.served) {
                // Mở trực tiếp file:// — chỉ để xem giao diện, không có API nào để bảo vệ
                this.allow(null);
                return true;
            }
            try {
                var info = await PanelAPI.auth.me();
                if (info.authenticated) {
                    this.allow(info.username);
                    return true;
                }
                this.locked('');
            } catch (e) {
                this.locked(e.message);
            }
            return false;
        },

        /** Cho vào panel. */
        allow: function (username) {
            this.username = username || null;
            loginScreen.hidden = true;
            document.getElementById('appShell').hidden = false;
            resetLoginButton();

            currentUser.hidden = !username;
            if (username) {
                currentUser.textContent = username;
                currentUser.title = 'Đang đăng nhập bằng tài khoản quản trị "' + username + '"';
            }
        },

        /** Đưa về màn hình đăng nhập (chưa đăng nhập, hết phiên, hoặc vừa đăng xuất). */
        locked: function (message) {
            this.username = null;
            currentUser.hidden = true;
            document.getElementById('appShell').hidden = true;
            loginScreen.hidden = false;
            resetLoginButton();
            this.error(message || '');

            // Dừng mọi việc đang chạy nền để không gọi API liên tục khi đã mất phiên
            servers.stop();
            loginAccessControl.stop();
            revenue.stop();
            shop.stop();
            player.stop();
            mapPanel.stop();
            mobPanel.stop();
            if (window.DropPanel) window.DropPanel.stop();
            if (window.BossPanel) window.BossPanel.stop();
            if (window.BossDropPanel) window.BossDropPanel.stop();
            if (window.TopPanel) window.TopPanel.stop();
            if (window.LuckyRoundPanel) window.LuckyRoundPanel.stop();
            if (window.DatabaseResetPanel) window.DatabaseResetPanel.stop();
            if (window.DonatePackagePanel) window.DonatePackagePanel.stop();
            intrinsicPanel.stop();
            accountPanel.stop();
            serverConfig.stop();
            cloudflarePanel.stop();
            Array.prototype.forEach.call(document.querySelectorAll('.modal'), function (modal) {
                modal.hidden = true;
            });
            setTimeout(function () { loginUser.focus(); }, 30);
        },

        error: function (message) {
            loginError.hidden = !message;
            loginError.textContent = message || '';
        },

        /** Tài khoản quản trị riêng trong data/config/web_admin.properties, không phải tài khoản game. */
        submit: async function () {
            var user = loginUser.value.trim();
            var pass = loginPass.value;
            if (!user || !pass) {
                this.error('Nhập đủ tài khoản và mật khẩu.');
                return;
            }

            this.error('');
            loginSubmit.disabled = true;
            loginSubmit.textContent = 'Đang kiểm tra...';
            try {
                var res = await PanelAPI.auth.login(user, pass, loginRemember.checked);
                loginPass.value = '';
                this.allow(res.username);
                toast((res && res.message) || 'Đăng nhập thành công', 'success');
                // Vào thẳng Doanh Thu & CSDL, không giữ trang của phiên trước
                await startPanel(true);
            } catch (e) {
                loginPass.value = '';
                resetLoginButton();
                this.error(e.message || 'Đăng nhập thất bại');
            }
        },

        logout: async function () {
            try {
                var res = await PanelAPI.auth.logout();
                loginUser.value = '';
                loginPass.value = '';
                this.locked('');
                toast(res.message || 'Đã đăng xuất', 'success');
            } catch (e) {
                if (e.unauthorized) this.locked(e.message);
                else toast('Chưa xác nhận thu hồi phiên: ' + e.message, 'error');
            }
        }
    };

    function resetLoginButton() {
        loginSubmit.disabled = false;
        loginSubmit.textContent = 'Đăng Nhập';
    }

    loginForm.addEventListener('submit', function (e) {
        e.preventDefault();
        auth.submit();
    });

    document.getElementById('btnLogout').addEventListener('click', function () {
        auth.logout();
    });

    /* ============================== BOOTSTRAP ============================ */

    /** Phần sau khi đã có quyền vào panel: nạp route + cấu hình + trạng thái. */
    async function startPanel(forceDefault) {
        if (forceDefault || !location.hash) {
            navigate(DEFAULT_ROUTE, true);
        } else {
            routeChanged();
        }

        await loadSettings();

        // thử lấy trạng thái server (nếu chạy qua WebPanelServer)
        try {
            var info = await PanelAPI.status();
            if (info) { PanelAPI.online = true; }
        } catch (e) { /* Status failure remains visibly offline; no fallback. */ }
        applyStatusPill();
    }

    async function boot() {
        var savedTheme;
        try { savedTheme = localStorage.getItem(THEME_KEY); } catch (e) { savedTheme = null; }
        applyTheme(savedTheme === 'dark' ? 'dark' : 'light');

        renderSidebar();

        // Mất phiên giữa chừng (server khởi động lại, hết hạn 12 giờ) -> quay về đăng nhập
        PanelAPI.onUnauthorized = function (message) {
            if (!loginScreen.hidden) return;   // đang ở màn hình đăng nhập rồi thì thôi
            auth.locked(message || 'Phiên đăng nhập đã hết hạn, đăng nhập lại.');
        };

        if (await auth.start()) {
            if (!PanelAPI.served) {
                Array.prototype.forEach.call(document.querySelectorAll('#content input, #content select, #content textarea, #content button, .modal input, .modal select, .modal textarea, .modal button, #btnLogout'), function (el) {
                    el.disabled = true;
                });
                toast('Chế độ file chỉ xem giao diện — không tải hoặc lưu dữ liệu.', 'info');
            }
            await startPanel(false);
        }
    }

    /* ====================== 7. CHẶN MỞ CÔNG CỤ DEV ====================== */
    /**
     * Chặn các phím mở DevTools / xem nguồn và menu chuột phải.
     * Đây chỉ là lớp cản trở cho người dùng phổ thông: người biết kỹ thuật vẫn mở
     * được DevTools bằng menu trình duyệt, còn dữ liệu thật đã được chặn ở API (401).
     */
    function guardDevTools() {
        document.addEventListener('contextmenu', function (e) {
            e.preventDefault();
        }, true);

        document.addEventListener('keydown', function (e) {
            var key = e.key;
            var devKeys = ['I', 'i', 'J', 'j', 'C', 'c'];

            if (key === 'F12') {
                e.preventDefault();
                return;
            }
            if ((e.ctrlKey || e.metaKey) && e.shiftKey && devKeys.indexOf(key) >= 0) {
                e.preventDefault();
                return;
            }
            if ((e.ctrlKey || e.metaKey) && (key === 'U' || key === 'u' || key === 'S' || key === 's')) {
                e.preventDefault();
            }
        }, true);
    }

    guardDevTools();

    // expose cho các task sau (ví dụ: `Panel.toast('...')`)
    window.Panel = {
        toast: toast,
        navigate: navigate,
        auth: auth,
        state: state,
        markDirty: markDirty,
        refreshPreview: renderPreview,
        giftcodes: giftcodes,
        servers: servers,
        revenue: revenue,
        shop: shop,
        player: player,
        map: mapPanel,
        mob: mobPanel,
        intrinsic: intrinsicPanel,
        account: accountPanel,
        serverConfig: serverConfig,
        cloudflare: cloudflarePanel
    };

    document.addEventListener('DOMContentLoaded', boot);
})();
