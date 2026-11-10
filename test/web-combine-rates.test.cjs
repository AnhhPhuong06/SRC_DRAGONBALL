// Exercise the production form functions without starting the game or using its database.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../web/js/app.js'), 'utf8');
const functions = source.slice(source.indexOf('async function loadCombineRates()'), source.indexOf('async function loadMobDrops()'));
const rates = {
    rateNangCapDo: [80, 50, 20, 10, 5, 4, 3, 0.125],
    ratePhaLeHoa: [50, 45, 40, 35, 30, 10, 2, 1, 0.5, 0.25],
    skhVip1: 45, skhVip2: 10, skhThuong: 45,
    rateBongTai2: 50, rateBongTai3: 20, rateZeno: 15,
    rateTrungDeTu1: 80, rateTrungDeTu2: 70, rateTrungDeTu3: 50
};
const button = { disabled: false };
const container = { innerHTML: '', textContent: '' };
let inputs = [], calls = [], notices = [], invalidReports = 0;
let reply = { success: true, data: rates };
const context = vm.createContext({
    document: {
        getElementById: id => id === 'btnSaveCombineRates' ? button : container,
        querySelectorAll: () => inputs
    },
    apiRequest: async (endpoint, method = 'GET', body) => {
        calls.push({ endpoint, method, body });
        return typeof reply === 'function' ? reply() : reply;
    },
    showToast: (message, kind) => notices.push({ message, kind })
});
vm.runInContext(functions, context);

function readInputs() {
    inputs = [...container.innerHTML.matchAll(/<input\b[^>]*>/g)].map(([tag]) => {
        const attrs = Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(match => [match[1], match[2]]));
        return {
            id: attrs.id, value: attrs.value,
            dataset: { key: attrs['data-key'], ...(attrs['data-index'] === undefined ? {} : { index: attrs['data-index'] }) },
            reportValidity() {
                const value = Number(this.value);
                const valid = this.value !== '' && Number.isFinite(value) && value >= 0 && value <= 100 &&
                    (attrs.step === 'any' || Number.isInteger(value));
                if (!valid) invalidReports++;
                return valid;
            }
        };
    });
}

async function run() {
    await context.loadCombineRates();
    readInputs();
    assert.equal(inputs.length, 27, 'Every upgrade, star and scalar has an input');
    assert.equal(button.disabled, false);
    assert(!container.innerHTML.includes('undefined'));
    for (const input of inputs) assert(container.innerHTML.includes(`for="${input.id}"`), 'Input has a matching label');
    await context.saveCombineRates();
    const payload = JSON.parse(JSON.stringify(calls.at(-1).body));
    assert.deepEqual(payload, rates, 'Form serializes the complete actual API schema without losing decimals');

    const first = inputs.find(input => input.id === 'rate_rateNangCapDo_0');
    first.value = '0';
    await context.saveCombineRates();
    assert.equal(calls.at(-1).body.rateNangCapDo[0], 0, 'Zero is preserved');
    for (const invalid of ['', '-1', '101', 'NaN']) {
        first.value = invalid;
        const count = calls.length;
        await context.saveCombineRates();
        assert.equal(calls.length, count, 'Invalid input does not send a request');
    }
    assert.equal(invalidReports, 4);
    first.value = '80';
    const vip = inputs.find(input => input.dataset.key === 'skhVip1');
    vip.value = '99';
    const count = calls.length;
    await context.saveCombineRates();
    assert.equal(calls.length, count, 'Invalid SKH total does not send a request');
    assert.equal(notices.at(-1).kind, 'error');
    vip.value = '45';

    let release;
    reply = () => new Promise(resolve => { release = resolve; });
    const pending = context.saveCombineRates();
    assert.equal(button.disabled, true, 'Save button locks while waiting');
    const inFlightCount = calls.length;
    await context.saveCombineRates();
    assert.equal(calls.length, inFlightCount, 'Repeated save does not send a duplicate');
    release({ success: false, error: 'Fixture persistence failure' });
    await pending;
    assert.equal(button.disabled, false, 'Failed save permits retry');
    assert.equal(notices.at(-1).message, 'Fixture persistence failure');
    assert.equal(first.value, '80', 'Failed save preserves form values');

    reply = { success: false, error: '<fixture load failure>' };
    await context.loadCombineRates();
    assert.equal(button.disabled, true, 'Load failure disables saving');
    assert.equal(container.textContent, '<fixture load failure>', 'Load errors use plain text');
    reply = { success: true, data: { ...rates, ratePhaLeHoa: [1, 2] } };
    await context.loadCombineRates();
    assert.equal(button.disabled, true, 'Incomplete server data cannot be saved');
    reply = { success: true, data: rates };
    await context.loadCombineRates();
    assert.equal(button.disabled, false, 'Successful reload recovers the form');
    console.log('Web combine form checks passed');
}
run().catch(error => { console.error(error); process.exitCode = 1; });
