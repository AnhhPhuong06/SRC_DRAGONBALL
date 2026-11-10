(function (global) {
    'use strict';
    var data = null, active = false, busy = false, sequence = 0;
    function el(id) { return document.getElementById(id); }
    function esc(v) { return String(v == null ? '' : v).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
    function notice(message, kind) { global.Panel.toast(message, kind || 'success'); }
    function lock(value) {
        busy = value;
        el('crateReload').disabled = value;
        el('crateSave').disabled = value || !data;
    }
    function render() {
        var groups = {};
        data.rates.forEach(function (rate) { (groups[rate.group] || (groups[rate.group] = [])).push(rate); });
        el('crateTotal').textContent = data.rates.length;
        el('crateGroupCount').textContent = Object.keys(groups).length;
        el('crateGroupsWrap').innerHTML = Object.keys(groups).map(function (group, index) {
            return '<details class="card drop-rate-group"' + (index === 0 ? ' open' : '') + '><summary>' + esc(group) + '</summary><div class="drop-rate-grid">'
                + groups[group].map(function (rate) {
                    return '<div class="field"><label for="crate_' + rate.key + '">' + esc(rate.label) + '</label>'
                        + '<input type="number" id="crate_' + rate.key + '" data-crate-rate="' + rate.key + '" min="' + rate.min
                        + '" max="' + rate.max + '" step="' + (rate.integer ? 1 : 0.01) + '" value="' + rate.value + '" required>'
                        + '<span class="field__hint">Tỉ lệ % (0 = tắt)</span></div>';
                }).join('') + '</div></details>';
        }).join('');
    }
    async function load() {
        if (busy) return;
        var request = ++sequence; lock(true);
        try {
            var next = await global.PanelAPI.combineRates.info();
            if (!active || request !== sequence) return;
            data = next;
            el('crateNotes').textContent = data.notes || '';
            el('crateError').textContent = data.loadError || '';
            el('crateError').hidden = !data.loadError;
            render();
        } catch (error) {
            if (active && request === sequence) {
                data = null;
                el('crateError').textContent = 'Không tải được tỉ lệ đập đồ: ' + error.message;
                el('crateError').hidden = false;
                notice(error.message, 'error');
            }
        } finally { if (request === sequence) lock(false); }
    }
    async function save(event) {
        event.preventDefault();
        if (busy || !data || !el('crateForm').reportValidity()) return;
        var values = {}, changedCount = 0;
        data.rates.forEach(function (rate) {
            var value = Number(el('crate_' + rate.key).value);
            if (rate.value !== value) { values[rate.key] = value; changedCount++; }
        });
        if (!changedCount) return notice('Chưa có tỉ lệ nào thay đổi.');
        var request = sequence;
        lock(true);
        try {
            var result = await global.PanelAPI.combineRates.run('rates-save', { values: values });
            if (!active || request !== sequence) return;
            notice(result.message || 'Đã lưu tỉ lệ đập đồ.');
            lock(false);
            await load();
        } catch (error) { if (active && request === sequence) notice(error.message, 'error'); }
        finally { if (request === sequence) lock(false); }
    }
    el('crateForm').addEventListener('submit', save);
    el('crateReload').addEventListener('click', function () { if (!busy) load(); });
    global.CombineRatesPanel = {
        start: function () { active = true; load(); },
        stop: function () { active = false; sequence++; busy = false; },
        load: load
    };
})(window);
