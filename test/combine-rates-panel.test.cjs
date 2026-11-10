const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const nodes = new Map();
function node(id) {
    if (!nodes.has(id)) nodes.set(id, {
        disabled: false, hidden: true, value: '', textContent: '', listeners: {},
        addEventListener(type, handler) { this.listeners[type] = handler; },
        reportValidity() { return true; },
        set innerHTML(html) {
            this.html = html;
            for (const match of html.matchAll(/id="(crate_[^"]+)"[^>]+value="([^"]+)"/g)) node(match[1]).value = match[2];
        }
    });
    return nodes.get(id);
}
let data = { rates: [{ key: 'nangCapDo0', label: 'Upgrade +1', group: 'Upgrade', value: 80, min: 0, max: 100, integer: false }] };
let loads = 0, saves = 0, notices = [], saveReply;
let loadReply = async () => structuredClone(data);
const window = {
    Panel: { toast(message, kind) { notices.push({ message, kind }); } },
    PanelAPI: { combineRates: {
        info: async () => { loads++; return loadReply(); },
        run: async (action, body) => {
            saves++;
            assert.equal(action, 'rates-save');
            if (saveReply) return saveReply(body);
            data.rates[0].value = body.values.nangCapDo0;
            return { success: true, message: 'Saved' };
        }
    } }
};
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../web/js/combine-rates.js'), 'utf8'), {
    window, document: { getElementById: node }
}, { filename: 'combine-rates.js' });
const flush = () => new Promise(resolve => setImmediate(resolve));
const submit = () => node('crateForm').listeners.submit({ preventDefault() {} });

(async () => {
    window.CombineRatesPanel.start(); await flush();
    assert.equal(node('crateTotal').textContent, 1);
    node('crate_nangCapDo0').value = '2.5';
    await submit();
    assert.equal(saves, 1);
    assert.equal(loads, 2, 'Successful save reloads server values');
    assert.equal(node('crate_nangCapDo0').value, '2.5');
    assert.equal(node('crateSave').disabled, false, 'Save releases the lock without an undefined request error');

    node('crate_nangCapDo0').value = '3';
    saveReply = async () => { throw new Error('Fixture save failure'); };
    await submit();
    assert.equal(node('crateSave').disabled, false, 'Failure permits retry');
    assert.equal(notices.at(-1).message, 'Fixture save failure');
    assert.equal(node('crate_nangCapDo0').value, '3', 'Failure keeps entered values');

    let release;
    saveReply = () => new Promise(resolve => { release = resolve; });
    const pending = submit();
    assert.equal(node('crateSave').disabled, true);
    const count = saves; await submit();
    assert.equal(saves, count, 'Duplicate saves are suppressed');
    window.CombineRatesPanel.stop();
    let releaseLoad;
    loadReply = () => new Promise(resolve => { releaseLoad = resolve; });
    window.CombineRatesPanel.start();
    const noticesBefore = notices.length;
    release({ success: true, message: 'Stale save' }); await pending;
    assert.equal(node('crateReload').disabled, true, 'Stale save cannot unlock a newer load');
    assert.equal(notices.length, noticesBefore, 'Stale save does not show a success toast');
    releaseLoad(structuredClone(data)); await flush();
    assert.equal(node('crateSave').disabled, false);
    console.log('PASS: production combine-rates panel load/save/retry and navigation-race checks');
})().catch(error => { console.error(error); process.exitCode = 1; });
