(function (global) {
    'use strict';
    var data = null, active = false, busy = false, sequence = 0, tab = 'templates', editKey = '', editRevision = 0;
    var outfitFields = ['bfHead', 'bfBody', 'bfLeg', 'bfBag', 'bfAura', 'bfEffect'];
    var appear = { DEFAULT_APPEAR: 'Tự xuất hiện', APPEAR_WITH_ANOTHER: 'Cùng boss khác', ANOTHER_LEVEL: 'Giai đoạn tiếp theo', CALL_BY_ANOTHER: 'Boss khác gọi' };
    var status = { REST: 'Đang nghỉ', RESPAWN: 'Đang hồi sinh', JUST_RESPAWN: 'Vừa xuất hiện', ACTIVE: 'Đang hoạt động', AFK: 'Đang chờ', DIE: 'Đã chết', CHAT_S: 'Đối thoại đầu', CHAT_M: 'Đối thoại', CHAT_E: 'Đối thoại cuối', LEAVE_MAP: 'Rời map' };
    function el(id) { return document.getElementById(id); }
    function esc(value) { return String(value == null ? '' : value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;'); }
    function number(value) { return Number(value).toLocaleString('vi-VN'); }
    function notice(message, kind) { global.Panel.toast(message, kind || 'success'); }
    function lock(value) {
        busy = value;
        el('bossReload').disabled = value;
        el('bossSave').disabled = value || !data || !!data.loadError;
        el('bossReset').disabled = value || !data || !!data.loadError || !data.templates.some(function (row) { return row.key === editKey && row.customized; });
        el('bossMapAdd').disabled = value || !data || !el('bfMapChoice').options.length;
        el('bossCancel').disabled = value;
        el('bossFilter').disabled = tab === 'runtime';
        document.querySelectorAll('[data-boss-edit], [data-boss-summon]').forEach(function (button) {
            button.disabled = value || !data || (button.hasAttribute('data-boss-edit') && !!data.loadError);
        });
    }
    function mapName(id) {
        var map = data.maps.find(function (entry) { return entry.id === id; });
        return '#' + id + (map ? ' · ' + map.name : '');
    }
    function rows() {
        var needle = el('bossSearch').value.trim().toLowerCase(), filter = el('bossFilter').value;
        return (tab === 'templates' ? data.templates : data.runtime).filter(function (row) {
            if (tab === 'templates') {
                return (!filter || row.customized === (filter === 'custom')) && [row.key, row.config.name, row.config.mapJoin.map(mapName).join(' ')].join(' ').toLowerCase().includes(needle);
            }
            return [row.id, row.name, row.status, row.mapName, row.mapId, row.templateKeys.join(' ')].join(' ').toLowerCase().includes(needle);
        });
    }
    function render() {
        if (!data) return;
        var list = rows();
        el('bossTemplatesTab').hidden = tab !== 'templates'; el('bossRuntimeTab').hidden = tab !== 'runtime';
        el('bossFilter').disabled = tab === 'runtime';
        document.querySelectorAll('[data-boss-tab]').forEach(function (button) { button.classList.toggle('btn--primary', button.dataset.bossTab === tab); });
        el('bossShown').textContent = list.length + ' / ' + (tab === 'templates' ? data.templates.length : data.runtime.length);
        el('bossEmpty').hidden = list.length > 0;
        if (!list.length) el('bossEmpty').textContent = tab === 'runtime' && !data.runtime.length ? 'Chưa có boss trong server. Danh sách này xuất hiện khi game server đã tải boss.' : 'Không có boss phù hợp.';
        if (tab === 'templates') {
            el('bossTemplatesBody').innerHTML = list.map(function (row) {
                var c = row.config;
                var allMaps = c.mapJoin.map(mapName).join(', ');
                var shownMaps = c.mapJoin.slice(0, 3).map(mapName).join(', ') + (c.mapJoin.length > 3 ? ' (+' + (c.mapJoin.length - 3) + ' map)' : '');
                return '<tr><td>' + esc(row.key) + '</td><td><b>' + esc(c.name) + '</b></td><td class="num">' + c.hp.map(number).join('<br>')
                    + '</td><td class="num">' + number(c.dame) + '</td><td class="boss-map-cell" title="' + esc(allMaps) + '">' + esc(shownMaps || 'Theo boss chính / phó bản')
                    + '</td><td class="num">' + number(c.secondsRest) + '</td><td>' + esc(appear[row.appearType] || row.appearType)
                    + '</td><td>' + (row.customized ? 'Đã chỉnh' : 'Gốc') + '</td><td><button class="btn-mini" type="button" data-boss-edit="' + esc(row.key) + '">Sửa</button></td></tr>';
            }).join('');
        } else {
            el('bossRuntimeBody').innerHTML = list.map(function (row) {
                return '<tr><td>' + esc(row.id) + '</td><td><b>' + esc(row.name || row.templateKeys[0] || 'Boss chưa xuất hiện') + '</b></td><td>' + esc(status[row.status] || row.status)
                    + '</td><td>' + (row.mapId >= 0 ? esc(row.mapName) + ' (#' + row.mapId + ') · Khu ' + row.zoneId : 'Chưa vào map')
                    + '</td><td class="num">' + number(row.hp) + ' / ' + number(row.hpMax) + '</td><td class="num">' + number(row.dame)
                    + '</td><td>' + row.templateKeys.map(function (key) { return '<button class="btn-mini" type="button" data-boss-edit="' + esc(key) + '">' + esc(key) + '</button>'; }).join(' ')
                    + '</td><td>' + (row.canSummon ? '<button class="btn-mini" type="button" data-boss-summon="' + row.id + '">Triệu hồi</button>' : '—') + '</td></tr>';
            }).join('');
        }
        lock(busy);
    }
    async function load() {
        if (busy) return;
        var request = ++sequence; lock(true);
        try {
            var next = await global.PanelAPI.boss.info();
            if (!active || request !== sequence) return;
            data = next;
            el('bossNotes').textContent = data.notes;
            el('bossError').textContent = data.loadError; el('bossError').hidden = !data.loadError;
            el('bossMapWarning').textContent = data.mapWarning; el('bossMapWarning').hidden = !data.mapWarning;
            el('bossTemplateCount').textContent = data.templates.length;
            el('bossCustomCount').textContent = data.templates.filter(function (row) { return row.customized; }).length;
            el('bossRuntimeCount').textContent = data.runtime.length + ' / ' + data.runtime.filter(function (row) { return row.status === 'REST'; }).length;
            el('bfMapChoice').innerHTML = data.maps.filter(function (map) { return map.id !== 5; }).map(function (map) { return '<option value="' + map.id + '">' + esc(mapName(map.id)) + '</option>'; }).join('');
            render();
        } catch (error) {
            if (active && request === sequence) {
                data = null;
                el('bossError').textContent = 'Không tải được danh sách boss: ' + error.message; el('bossError').hidden = false;
                el('bossTemplatesBody').innerHTML = ''; el('bossRuntimeBody').innerHTML = '';
                el('bossEmpty').hidden = false; el('bossEmpty').textContent = 'Nhấn Tải lại để thử lại.';
                notice(error.message, 'error');
            }
        } finally { if (request === sequence) lock(false); }
    }
    function open(key) {
        if (busy || !data || data.loadError) return;
        var row = data.templates.find(function (entry) { return entry.key === key; });
        if (!row) return;
        editKey = key; editRevision = data.revision;
        var c = row.config;
        el('bossEditorTitle').textContent = 'SỬA MẪU BOSS · ' + key;
        el('bossAppearHint').textContent = 'Kiểu xuất hiện: ' + (appear[row.appearType] || row.appearType) + '. Cơ chế gọi boss được giữ theo game.';
        [['bfName', c.name], ['bfGender', c.gender], ['bfHp', c.hp.join(', ')], ['bfDame', c.dame], ['bfRest', c.secondsRest], ['bfMaps', c.mapJoin.join(', ')]].forEach(function (pair) { el(pair[0]).value = pair[1]; });
        outfitFields.forEach(function (id, index) { el(id).value = c.outfit[index] == null ? -1 : c.outfit[index]; });
        el('bossEditor').hidden = false; lock(false); el('bfName').focus();
    }
    function integers(raw, label, empty, maximum) {
        if (!raw.trim() && empty) return [];
        var parts = raw.split(',').map(function (value) { return value.trim(); });
        if (parts.length > maximum || parts.some(function (value) { return !/^-?\d+$/.test(value) || !Number.isSafeInteger(Number(value)); })) throw new Error(label + ' phải là danh sách số nguyên cách nhau bằng dấu phẩy.');
        return parts.map(Number);
    }
    async function mutate(action, body) {
        if (busy || !data) return;
        var request = ++sequence; lock(true);
        try {
            var response = await global.PanelAPI.boss.run(action, body);
            if (!active || request !== sequence) return;
            notice(response.message); el('bossEditor').hidden = true;
            lock(false); await load();
        } catch (error) { if (active && request === sequence) notice(error.message, 'error'); }
        finally { if (request === sequence) lock(false); }
    }
    el('bossForm').addEventListener('submit', function (event) {
        event.preventDefault(); if (busy || !data || !el('bossForm').reportValidity()) return;
        try {
            var hp = integers(el('bfHp').value, 'HP', false, 64), maps = integers(el('bfMaps').value, 'Map', true, 256);
            if (hp.some(function (value) { return value < 1 || value > data.maxStat; })) throw new Error('HP phải là số dương, tối đa ' + data.maxStat + '.');
            if (new Set(maps).size !== maps.length || maps.some(function (value) { return value < -1 || value > 32767; })) throw new Error('Danh sách map bị trùng hoặc ID ngoài giới hạn.');
            mutate('save', { key: editKey, revision: editRevision, name: el('bfName').value.trim(), gender: Number(el('bfGender').value),
                dame: Number(el('bfDame').value), hp: hp, mapJoin: maps, secondsRest: Number(el('bfRest').value),
                outfit: outfitFields.map(function (id) { return Number(el(id).value); }) });
        } catch (error) { notice(error.message, 'error'); }
    });
    el('view-boss').addEventListener('click', function (event) {
        var button = event.target.closest('button'); if (!button || busy) return;
        if (button.dataset.bossTab) { tab = button.dataset.bossTab; if (data) render(); return; }
        if (button.dataset.bossEdit) open(button.dataset.bossEdit);
        if (button.hasAttribute('data-boss-summon') && data && global.confirm('Triệu hồi boss đang nghỉ ID ' + button.dataset.bossSummon + '?')) mutate('summon', { id: Number(button.dataset.bossSummon) });
    });
    el('bossReset').addEventListener('click', function () { if (!busy && data && global.confirm('Khôi phục mẫu ' + editKey + ' về cấu hình gốc?')) mutate('reset', { key: editKey, revision: editRevision }); });
    el('bossCancel').addEventListener('click', function () { if (!busy) el('bossEditor').hidden = true; });
    el('bossReload').addEventListener('click', function () { if (!busy) { el('bossEditor').hidden = true; load(); } });
    el('bossSearch').addEventListener('input', function () { if (data) render(); });
    el('bossFilter').addEventListener('change', function () { if (data) render(); });
    el('bossMapAdd').addEventListener('click', function () {
        if (busy || !data || !el('bfMapChoice').options.length) return;
        try {
            var maps = integers(el('bfMaps').value, 'Map', true, 256), id = Number(el('bfMapChoice').value);
            if (!maps.includes(id)) maps.push(id); el('bfMaps').value = maps.join(', ');
        } catch (error) { notice(error.message, 'error'); }
    });
    global.BossPanel = {
        start: function () { active = true; document.body.classList.add('boss-view'); load(); },
        stop: function () { active = false; sequence++; busy = false; el('bossEditor').hidden = true; document.body.classList.remove('boss-view'); }
    };
})(window);
