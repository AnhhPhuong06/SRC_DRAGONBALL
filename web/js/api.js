/* =========================================================================
 * api.js — same-origin JSON transport for server.web.WebAdminServer.
 * Cookie sessions stay HttpOnly; CSRF tokens stay in memory only.
 * file:// is view-only. Failed HTTP requests never save local/fake data.
 * ========================================================================= */
(function (global) {
    'use strict';

    var csrfToken = null;
    var api = {
        online: false,
        served: global.location.protocol === 'http:' || global.location.protocol === 'https:',
        info: null,
        onUnauthorized: null
    };
    var OFFLINE_MESSAGE = 'Chế độ file chỉ xem giao diện. Chạy ServerManager -web và mở panel qua HTTP(S) để sử dụng API.';

    function object(value) {
        return value !== null && typeof value === 'object' && !Array.isArray(value);
    }

    function invalidResponse() {
        return new Error('API trả dữ liệu không đúng hợp đồng JSON. Kiểm tra đường dẫn /api/panel và cấu hình máy chủ.');
    }

    function hasFields(value, fields) {
        return object(value) && Object.keys(fields).every(function (key) {
            var expected = fields[key];
            return expected === 'array' ? Array.isArray(value[key])
                : expected === 'object' ? object(value[key]) : typeof value[key] === expected;
        });
    }

    function settingsShape(value) {
        var fields = ['appName', 'fname', 'ftitle', 'domain', 'supportEmail', 'fgmail', 'fpgmail', 'zalo', 'zalo2', 'logo'];
        return object(value) && Object.keys(value).every(function (key) {
            return key === '_meta' ? object(value[key]) : fields.indexOf(key) >= 0 && typeof value[key] === 'string';
        });
    }

    function authShape(value) {
        return hasFields(value, { authenticated: 'boolean' }) && (!value.authenticated
            || (typeof value.username === 'string' && value.username.length > 0
                && typeof value.csrfToken === 'string' && value.csrfToken.length > 0));
    }

    function rememberSession(value) {
        csrfToken = value.authenticated ? value.csrfToken : null;
        return value;
    }

    function endpoint(name, params) {
        var query = new URLSearchParams();
        Object.keys(params || {}).forEach(function (key) {
            var value = params[key];
            if (value !== undefined && value !== null) query.set(key, String(value));
        });
        return '/api/panel/' + name + (query.toString() ? '?' + query.toString() : '');
    }

    /** Reject HTML 200s, redirects, malformed JSON, error bodies and wrong DTOs. */
    async function request(method, path, body, validate, quiet) {
        if (!api.served) throw new Error(OFFLINE_MESSAGE);
        var target = new URL(path, global.location.href);
        if (target.origin !== global.location.origin || !/^\/api\/panel\//.test(target.pathname)) {
            throw new Error('Panel chỉ gọi API cùng origin tại /api/panel/.');
        }
        var opts = {
            method: method,
            headers: { Accept: 'application/json' },
            cache: 'no-store',
            credentials: 'same-origin',
            mode: 'same-origin',
            redirect: 'error'
        };
        var login = target.pathname === '/api/panel/auth' && target.searchParams.get('action') === 'login';
        if (method !== 'GET' && method !== 'HEAD' && !login) {
            if (!csrfToken) throw new Error('Chưa có phiên CSRF hợp lệ. Hãy đăng nhập lại.');
            opts.headers['X-CSRF-Token'] = csrfToken;
        }
        if (body !== undefined) {
            if (body instanceof global.FormData) {
                opts.body = body; // Browser sets the multipart boundary.
            } else {
                opts.headers['Content-Type'] = 'application/json';
                opts.body = JSON.stringify(body);
            }
        }
        var res;
        try {
            res = await global.fetch(target.href, opts);
        } catch (e) {
            api.online = false;
            throw new Error('Không kết nối được API cùng origin. Không có dữ liệu nào được lưu cục bộ.');
        }
        if (res.status === 401) {
            csrfToken = null;
            var authError = new Error('Chưa đăng nhập, thông tin đăng nhập sai hoặc phiên đã hết hạn.');
            authError.unauthorized = true;
            if (!quiet && api.onUnauthorized) api.onUnauthorized(authError.message);
            throw authError;
        }
        var contentType = res.headers.get('Content-Type') || '';
        if (!/^application\/json\s*(?:;|$)/i.test(contentType)) {
            api.online = false;
            throw invalidResponse();
        }
        var data;
        try {
            data = JSON.parse(await res.text());
        } catch (e) {
            api.online = false;
            throw invalidResponse();
        }
        if ((!object(data) && !Array.isArray(data))) throw invalidResponse();
        if (!res.ok || (object(data) && (data.success === false || data.ok === false || data.error))) {
            throw new Error(object(data) && typeof (data.error || data.message) === 'string'
                ? (data.error || data.message) : 'API thất bại (HTTP ' + res.status + ').');
        }
        if (!validate(data)) throw invalidResponse();
        api.online = true;
        return data;
    }

    function get(name, params, validate) {
        return request('GET', endpoint(name, params), undefined, validate);
    }

    function mutationShape(value) {
        return object(value) && (value.success === true || value.ok === true)
            && (value.message === undefined || typeof value.message === 'string');
    }

    function run(name, action, body) {
        return request('POST', endpoint(name, { action: action }), body, mutationShape);
    }

    api.auth = {
        me: async function () {
            try {
                return rememberSession(await request('GET', endpoint('auth'), undefined, authShape, true));
            } catch (e) {
                csrfToken = null;
                if (e.unauthorized) return { authenticated: false };
                throw e;
            }
        },
        login: async function (username, password, remember) {
            var value = await request('POST', endpoint('auth', { action: 'login' }),
                { username: username, password: password, remember: !!remember }, authShape, true);
            if (!value.authenticated) throw new Error('Máy chủ chưa xác nhận đăng nhập.');
            return rememberSession(value);
        },
        logout: async function () {
            var value = await request('POST', endpoint('auth', { action: 'logout' }), undefined,
                function (data) { return authShape(data) && data.authenticated === false && data.success === true; });
            return rememberSession(value);
        }
    };

    api.loadSettings = function () {
        return get('settings', null, settingsShape);
    };
    api.saveSettings = async function (settings) {
        var value = await request('POST', endpoint('settings'), settings, function (data) {
            return mutationShape(data) && settingsShape(data.settings);
        });
        return value.settings;
    };

    api.giftcodes = {
        list: function () {
            return get('giftcode', null, function (data) {
                return Array.isArray(data) && data.every(function (row) {
                    return hasFields(row, { id: 'number', code: 'string', items: 'array', countLeft: 'number' });
                });
            });
        },
        create: function (data) { return request('POST', endpoint('giftcode'), data, mutationShape); },
        createBatch: function (data) { return request('POST', endpoint('giftcode/batch'), data, function (value) {
            return mutationShape(value) && value.count === 50 && value.countLeft === 1 && value.synced === true
                && Array.isArray(value.codes) && value.codes.length === 50
                && value.codes.every(function (code) { return typeof code === 'string' && code.length > 0; });
        }); },
        update: function (data) { return request('PUT', endpoint('giftcode', { id: data.id }), data, mutationShape); },
        remove: function (id) { return request('DELETE', endpoint('giftcode', { id: id }), undefined, mutationShape); },
        sync: function () { return request('POST', endpoint('giftcode/sync'), undefined, mutationShape); }
    };

    function catalogShape(value) {
        return hasFields(value, { id: 'number', name: 'string' });
    }
    function catalogSearch(name, q) {
        return get(name, { q: q || '' }, function (data) { return Array.isArray(data) && data.every(catalogShape); });
    }
    api.catalog = {
        item: function (id) { return get('item', { id: id }, catalogShape); },
        option: function (id) { return get('option', { id: id }, catalogShape); },
        searchItems: function (q) { return catalogSearch('item', q); },
        searchOptions: function (q) { return catalogSearch('option', q); }
    };

    api.server = {
        loginAccess: function () { return get('server', { action: 'login-access' }, function (data) {
            return hasFields(data, { adminOnly: 'boolean', persistent: 'boolean' });
        }); },
        setLoginAccess: function (on) { return request('POST', endpoint('server', { action: 'login-access' }), { on: on }, function (data) {
            return mutationShape(data) && typeof data.adminOnly === 'boolean' && data.persistent === true && data.liveApplied === true;
        }); },
        info: function () {
            return get('server', null, function (data) {
                return hasFields(data, { players: 'number', playerList: 'array', bossList: 'array', bosses: 'object' });
            });
        },
        bots: function (filters) {
            return get('server', Object.assign({ action: 'bots' }, filters || {}), function (data) {
                return hasFields(data, { total: 'number', rows: 'array' });
            });
        },
        run: function (action, body) { return run('server', action, body); }
    };
    api.boss = {
        info: function () {
            return get('boss', null, function (data) {
                return hasFields(data, { templates: 'array', runtime: 'array', maps: 'array', revision: 'number', loadError: 'string' });
            });
        },
        run: function (action, body) { return run('boss', action, body || {}); }
    };
    api.top = {
        info: function () {
            return get('top', null, function (data) {
                return hasFields(data, { tops: 'array', catalog: 'object', timezone: 'string', serverTime: 'string' });
            });
        },
        run: function (action, body) { return run('top', action, body || {}); }
    };
    api.revenue = {
        info: function () {
            return get('revenue', null, function (data) {
                return hasFields(data, { accounts: 'object', revenue: 'object', chart: 'array', topupToday: 'object', recharge: 'object' });
            });
        },
        run: function (action, body) { return run('revenue', action, body); }
    };
    api.shop = {
        list: function () {
            return get('shop', null, function (data) { return hasFields(data, { shops: 'array', npcs: 'array', typeShops: 'array', typeSells: 'array' }); });
        },
        detail: function (id) {
            return get('shop', { shop: id }, function (data) { return object(data) && hasFields(data.shop, { id: 'number', tabs: 'array' }); });
        },
        run: function (action, body) { return run('shop', action, body || {}); }
    };

    // Preserve raw UI DTOs rather than inventing success wrappers or money aliases.
    function entity(name, plural, detailFields) {
        return {
            list: function (filters) {
                return get(name, filters || {}, function (data) {
                    return object(data) && Array.isArray(data[plural]) && typeof data.total === 'number';
                });
            },
            detail: function (id) {
                return get(name, { id: id }, function (data) {
                    return hasFields(data, detailFields);
                });
            },
            run: function (action, body) { return run(name, action, body || {}); }
        };
    }
    api.player = entity('player', 'players', { player: 'object', catalog: 'object' });
    api.map = entity('map', 'maps', { map: 'object', catalog: 'object' });
    api.mob = entity('mob', 'mobs', { mob: 'object', catalog: 'object' });
    api.drop = {
        info: function () { return get('drop', null, function (data) { return hasFields(data, { rules: 'array', revision: 'number', catalog: 'object' }); }); },
        run: function (action, body) { return run('drop', action, body || {}); }
    };
    api.bossDrop = {
        info: function () { return get('boss-drop', null, function (data) { return hasFields(data, { rules: 'array', revision: 'number', catalog: 'object' }); }); },
        run: function (action, body) { return run('boss-drop', action, body || {}); }
    };
    api.combineRates = {
        info: function () { return get('combine-rates', null, function (data) { return hasFields(data, { rates: 'array' }); }); },
        run: function (action, body) { return run('combine-rates', action, body || {}); }
    };
    api.intrinsic = entity('intrinsic', 'intrinsics', { intrinsic: 'object', catalog: 'object' });
    api.account = entity('account', 'accounts', { account: 'object', characters: 'array', catalog: 'object' });

    api.website = {
        config: function () {
            return get('website', null, function (data) {
                return hasFields(data, { config: 'object', fields: 'array', groups: 'array', storage: 'object', backups: 'array' });
            });
        },
        save: function (values) { return run('website', 'save', { values: values }); },
        restore: function (backup) { return run('website', 'restore', { backup: backup || '' }); }
    };
    api.cloudflare = {
        info: function () {
            return get('cloudflare', null, function (data) {
                return hasFields(data, { configured: 'boolean', tokenSet: 'boolean', records: 'array', zones: 'array' });
            });
        },
        saveToken: function (token) { return run('cloudflare', 'token', { token: token }); },
        useZone: function (zoneId, zoneName) { return run('cloudflare', 'zone', { zoneId: zoneId, zoneName: zoneName }); },
        forget: function () { return run('cloudflare', 'forget'); },
        saveRecord: function (record) { return run('cloudflare', record.id ? 'record-update' : 'record-create', record); },
        deleteRecord: function (id) { return run('cloudflare', 'record-delete', { id: id }); },
        purge: function () { return run('cloudflare', 'purge'); }
    };
    api.status = async function () {
        try {
            var value = await get('status', null, function (data) {
                return hasFields(data, { port: 'number', playersOnline: 'number' });
            });
            api.info = value;
            return value;
        } catch (e) {
            api.online = false;
            throw e;
        }
    };
    api.uploadLogo = async function (file) {
        var form = new global.FormData();
        form.append('file', file);
        var value = await request('POST', endpoint('logo'), form, function (data) {
            return mutationShape(data) && data.url === '/api/panel/logo';
        });
        return value.url;
    };

    api.luckyRound = {
        info: function () { return get('lucky-round', null, function (data) {
            return hasFields(data, { state: 'object', revision: 'string', fields: 'array', groupFields: 'array', catalog: 'object' });
        }); },
        save: function (data) { return request('POST', endpoint('lucky-round'), data, function (value) {
            return mutationShape(value) && value.liveApplied === true && typeof value.revision === 'string';
        }); }
    };
    api.donatePackage = {
        info: function () { return get('donate-package', null, function (data) {
            return hasFields(data, { packages: 'array', catalog: 'object', notes: 'string' });
        }); },
        run: function (action, body) { return run('donate-package', action, body || {}); }
    };
    api.databaseReset = {
        info: function () { return get('database-reset', null, function (data) {
            return object(data) && typeof data.completed === 'boolean' && (data.completed || hasFields(data, {database:'string',tables:'array',blockers:'array',canReset:'boolean',token:'string',expiresAt:'number',confirmation:'string'}));
        }); },
        reset: function (body) { return request('POST', endpoint('database-reset'), body, function (data) {
            return mutationShape(data) && data.completed === true && data.restartRequired === true && object(data.deleted);
        }); }
    };
    global.PanelAPI = api;
})(window);
