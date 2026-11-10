(function (global) {
    'use strict';
    var data = null, active = false, busy = false, sequence = 0, editId = 0, editRevision = 0;
    function el(id) { return document.getElementById(id); }
    function esc(value) { return String(value == null ? '' : value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;'); }
    function notice(message, kind) { global.Panel.toast(message, kind || 'success'); }
    function lock(value) {
        busy = value;
        ['dropReload', 'dropAdd', 'dropRuleSave', 'dropOptionAdd'].forEach(function (id) {
            el(id).disabled = value || (id !== 'dropReload' && (!data || (id === 'dropAdd' && !!data.loadError)));
        });
    }
    function choice(rows, all) {
        return (all ? '<option value="-1">' + esc(all) + '</option>' : '') + rows.map(function (row) {
            return '<option value="' + row.id + '">#' + row.id + ' · ' + esc(row.name) + '</option>';
        }).join('');
    }
    function datalist(rows) {
        return rows.map(function (row) { return '<option value="' + row.id + '">' + esc(row.name) + '</option>'; }).join('');
    }
    async function load() {
        if (busy) return;
        var request = ++sequence;
        lock(true);
        try {
            var next = await global.PanelAPI.drop.info();
            if (!active || request !== sequence) return;
            data = next;
            el('dropNotes').textContent = data.notes || '';
            el('dropError').textContent = data.loadError || '';
            el('dropError').hidden = !data.loadError;
            el('dropTotal').textContent = data.rules.length + '/' + data.maxRules;
            el('dropEnabled').textContent = data.rules.filter(function (r) { return r.enabled; }).length;
            el('dfMap').innerHTML = choice(data.catalog.maps, 'Tất cả map');
            el('dfMob').innerHTML = choice(data.catalog.mobs, 'Tất cả quái thường');
            el('dropItems').innerHTML = datalist(data.catalog.items);
            el('dropOptionCatalog').innerHTML = datalist(data.catalog.options);
            renderRules();
        } catch (error) {
            if (active && request === sequence) {
                data = null;
                el('dropError').textContent = 'Không tải được cấu hình drop: ' + error.message;
                el('dropError').hidden = false;
                notice(error.message, 'error');
            }
        } finally { if (request === sequence) lock(false); }
    }
    function renderRules() {
        var needle = el('dropSearch').value.trim().toLowerCase();
        var rows = data.rules.filter(function (r) {
            return [r.id, r.name, r.itemId, r.itemName, r.mapName, r.mobName].join(' ').toLowerCase().indexOf(needle) >= 0;
        });
        el('dropBody').innerHTML = rows.map(function (r) {
            return '<tr><td>' + (r.enabled ? 'Đang bật' : 'Đang tắt') + '</td><td><b>' + esc(r.name) + '</b><br>#' + r.id
                + '</td><td>#' + r.itemId + ' · ' + esc(r.itemName) + '</td><td>' + esc(r.mapName) + '</td><td>' + esc(r.mobName)
                + '</td><td class="num">' + r.chance + '%</td><td class="num">' + r.minQuantity + (r.maxQuantity === r.minQuantity ? '' : '–' + r.maxQuantity)
                + '</td><td>' + r.options.map(function (o) {
                    var definition = data.catalog.options.find(function (entry) { return entry.id === o.id; });
                    return esc(definition ? definition.name.replace(/#/g, o.param) : '#' + o.id + ': ' + o.param);
                }).join('<br>') + '</td><td class="drop-actions">'
                + '<button class="btn-mini" type="button" data-drop-edit="' + r.id + '">Sửa</button> '
                + '<button class="btn-mini" type="button" data-drop-toggle="' + r.id + '">' + (r.enabled ? 'Tắt' : 'Bật') + '</button> '
                + '<button class="btn-mini" type="button" data-drop-delete="' + r.id + '">Xóa</button></td></tr>';
        }).join('');
        el('dropEmpty').hidden = rows.length > 0;
        el('dropEmpty').textContent = needle ? 'Không có quy tắc khớp tìm kiếm.' : 'Chưa có drop tùy chỉnh. Chọn “Thêm drop” để tạo quy tắc.';
    }
    function itemName() {
        var id = Number(el('dfItem').value);
        var item = data && data.catalog.items.find(function (entry) { return entry.id === id; });
        el('dfItemName').textContent = item ? item.name : 'Chọn ID vật phẩm có trong danh sách.';
    }
    function addOption(option) {
        if (el('dropOptions').children.length >= 32) return notice('Tối đa 32 option mỗi quy tắc.', 'error');
        var row = document.createElement('div');
        row.className = 'drop-option-row';
        row.innerHTML = '<div class="field"><label>ID OPTION</label><input type="text" aria-label="ID option" data-drop-option-id list="dropOptionCatalog" pattern="[0-9]+" required value="' + esc(option ? option.id : '') + '"></div>'
            + '<div class="field"><label>THAM SỐ</label><input aria-label="Tham số option" data-drop-option-param type="number" min="0" max="65535" step="1" required value="' + esc(option ? option.param : 0) + '"></div>'
            + '<button class="btn" type="button" data-drop-option-remove>Xóa option</button>';
        el('dropOptions').appendChild(row);
    }
    function open(rule) {
        if (busy || !data || data.loadError) return;
        rule = rule || { id: 0, name: '', mapId: -1, mobId: -1, itemId: '', chance: 1, minQuantity: 1, maxQuantity: 1, enabled: true, options: [] };
        editId = rule.id;
        editRevision = data.revision;
        el('dropEditorTitle').textContent = rule.id ? 'SỬA QUY TẮC DROP #' + rule.id : 'THÊM QUY TẮC DROP';
        [['dfName', rule.name], ['dfMap', rule.mapId], ['dfMob', rule.mobId], ['dfItem', rule.itemId], ['dfChance', rule.chance],
            ['dfMin', rule.minQuantity], ['dfMax', rule.maxQuantity], ['dfEnabled', rule.enabled ? '1' : '0']].forEach(function (pair) { el(pair[0]).value = pair[1]; });
        el('dropOptions').innerHTML = '';
        rule.options.forEach(addOption);
        itemName();
        el('dropEditor').hidden = false;
        el('dfName').focus();
    }
    function rulePayload(rule) {
        return { id: rule.id, name: rule.name, enabled: rule.enabled, mapId: rule.mapId, mobId: rule.mobId, itemId: rule.itemId,
            chance: rule.chance, minQuantity: rule.minQuantity, maxQuantity: rule.maxQuantity, options: rule.options, revision: data.revision };
    }
    async function mutate(action, body, close) {
        if (busy || !data) return;
        lock(true);
        try {
            var result = await global.PanelAPI.drop.run(action, body);
            if (!active) return;
            notice(result.message || 'Đã lưu cấu hình drop.');
            if (close) el('dropEditor').hidden = true;
            lock(false);
            await load();
        } catch (error) { notice(error.message, 'error'); }
        finally { lock(false); }
    }
    async function saveRule(event) {
        event.preventDefault();
        if (busy || !data || !el('dropRuleForm').reportValidity()) return;
        var options = Array.from(el('dropOptions').children).map(function (row) {
            return { id: Number(row.querySelector('[data-drop-option-id]').value), param: Number(row.querySelector('[data-drop-option-param]').value) };
        });
        var ids = new Set(options.map(function (option) { return option.id; }));
        if (ids.size !== options.length) return notice('Không thêm trùng ID option.', 'error');
        var minimum = Number(el('dfMin').value), maximum = Number(el('dfMax').value);
        if (maximum < minimum) return notice('Số lượng nhiều nhất phải lớn hơn hoặc bằng ít nhất.', 'error');
        await mutate('rule-save', { id: editId, revision: editRevision, name: el('dfName').value.trim(), enabled: el('dfEnabled').value === '1',
            mapId: Number(el('dfMap').value), mobId: Number(el('dfMob').value), itemId: Number(el('dfItem').value), chance: Number(el('dfChance').value),
            minQuantity: minimum, maxQuantity: maximum, options: options }, true);
    }
    el('view-drop').addEventListener('click', function (event) {
        var button = event.target.closest('button');
        if (!button) return;
        if (!data || busy) return;
        var id = Number(button.dataset.dropEdit || button.dataset.dropToggle || button.dataset.dropDelete);
        var rule = data.rules.find(function (entry) { return entry.id === id; });
        if (rule && button.hasAttribute('data-drop-edit')) open(rule);
        if (rule && button.hasAttribute('data-drop-toggle')) {
            var body = rulePayload(rule); body.enabled = !rule.enabled; mutate('rule-save', body, true);
        }
        if (rule && button.hasAttribute('data-drop-delete') && global.confirm('Xóa quy tắc “' + rule.name + '”?')) mutate('rule-delete', { id: id, revision: data.revision }, true);
        if (button.hasAttribute('data-drop-option-remove')) button.closest('.drop-option-row').remove();
    });
    el('dropAdd').addEventListener('click', function () { open(); });
    el('dropCancel').addEventListener('click', function () { if (!busy) el('dropEditor').hidden = true; });
    el('dropReload').addEventListener('click', function () { if (!busy) { el('dropEditor').hidden = true; load(); } });
    el('dropSearch').addEventListener('input', function () { if (data) renderRules(); });
    el('dropOptionAdd').addEventListener('click', function () { if (!busy) addOption(); });
    el('dfItem').addEventListener('input', itemName);
    el('dropRuleForm').addEventListener('submit', saveRule);
    global.DropPanel = {
        start: function () { active = true; document.body.classList.add('drop-view'); load(); },
        stop: function () { active = false; sequence++; busy = false; el('dropEditor').hidden = true; document.body.classList.remove('drop-view'); },
        load: load
    };
})(window);
