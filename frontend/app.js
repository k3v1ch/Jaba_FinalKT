// ─── Config ───────────────────────────────────────────────────────────────────
const API = '/api';

// ─── State ────────────────────────────────────────────────────────────────────
let _token = localStorage.getItem('ep_token');
const _eventStore = {};

function getToken()      { return _token; }
function setToken(t)     { _token = t; t ? localStorage.setItem('ep_token', t) : localStorage.removeItem('ep_token'); }
function parseJwt(t)     { try { return JSON.parse(atob(t.split('.')[1])); } catch { return {}; } }
function getRole()       { return _token ? parseJwt(_token).role : null; }
function getName()       { return _token ? parseJwt(_token).name : null; }
function hasRole(...rs)  { return rs.includes(getRole()); }
function isMod()         { return hasRole('MODERATOR', 'ADMIN'); }
function isAdmin()       { return hasRole('ADMIN'); }

// ─── API Layer ────────────────────────────────────────────────────────────────
async function api(url, opts = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (_token) headers['Authorization'] = `Bearer ${_token}`;
    Object.assign(headers, opts.headers || {});

    const res = await fetch(API + url, { ...opts, headers });

    if (res.status === 204) return null;
    let body; try { body = await res.json(); } catch { body = {}; }

    if (res.status === 401) { setToken(null); go('/login'); throw new Error('Требуется авторизация'); }
    if (!res.ok) throw new Error(body.message || `Ошибка ${res.status}`);
    return body;
}

// ─── Router ───────────────────────────────────────────────────────────────────
const routes = {
    '/login':                 pageLogin,
    '/register':              pageRegister,
    '/events':                pageEvents,
    '/my-applications':       pageMyApps,
    '/profile':               pageProfile,
    '/admin/events':          pageAdminEvents,
    '/admin/applications':    pageAdminApplications,
    '/admin/users':           pageAdminUsers,
};

function go(path) { location.hash = path; }
function getPath() { return location.hash.slice(1) || '/events'; }

window.addEventListener('hashchange', route);
window.addEventListener('load', route);

function route() {
    const path = getPath();
    if (['/my-applications', '/profile'].includes(path) && !_token) { go('/login'); return; }
    if (path.startsWith('/admin') && !isMod()) { go('/events'); return; }
    if (path === '/admin/users' && !isAdmin()) { go('/admin/events'); return; }
    renderNav();
    (routes[path] || pageEvents)();
}

// ─── Navbar ───────────────────────────────────────────────────────────────────
function renderNav() {
    const role = getRole();
    const roleColor = role === 'ADMIN' ? 'bg-red-500' : role === 'MODERATOR' ? 'bg-yellow-500' : 'bg-indigo-400';
    document.getElementById('navbar').innerHTML = `
        <div class="bg-indigo-700 shadow-lg">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="flex items-center justify-between h-16">
                    <div class="flex items-center gap-5">
                        <a href="#/events" class="text-white font-bold text-xl flex items-center gap-2 shrink-0">
                            <i class="fa-solid fa-calendar-days"></i> EventPlatform
                        </a>
                        <a href="#/events" class="navlink">Мероприятия</a>
                        ${_token ? `<a href="#/my-applications" class="navlink">Мои заявки</a>` : ''}
                        ${isMod() ? `<a href="#/admin/events" class="navlink">Управление</a>
                                     <a href="#/admin/applications" class="navlink">Заявки</a>` : ''}
                        ${isAdmin() ? `<a href="#/admin/users" class="navlink">Пользователи</a>` : ''}
                    </div>
                    <div class="flex items-center gap-3">
                        ${_token ? `
                            <span class="text-indigo-200 text-sm hidden sm:block">${esc(getName())}</span>
                            <span class="text-xs px-2 py-0.5 rounded-full ${roleColor} text-white">${role}</span>
                            <a href="#/profile" class="text-indigo-200 hover:text-white transition" title="Профиль"><i class="fa fa-user"></i></a>
                            <button onclick="logout()" class="text-indigo-200 hover:text-white transition" title="Выйти"><i class="fa fa-right-from-bracket"></i></button>
                        ` : `
                            <a href="#/login" class="bg-white text-indigo-700 px-4 py-1.5 rounded-lg text-sm font-semibold hover:bg-indigo-50 transition">Войти</a>
                            <a href="#/register" class="bg-indigo-500 text-white px-4 py-1.5 rounded-lg text-sm font-semibold hover:bg-indigo-400 transition">Регистрация</a>
                        `}
                    </div>
                </div>
            </div>
        </div>
        <style>.navlink{color:rgb(199 210 254);font-size:.875rem;font-weight:500;transition:color .15s}.navlink:hover{color:#fff}</style>
    `;
}

// ─── Page: Login ──────────────────────────────────────────────────────────────
function pageLogin() {
    render(`
        <div class="max-w-md mx-auto mt-12 fade-in">
            <div class="bg-white rounded-2xl shadow-xl p-8">
                <div class="text-center mb-6">
                    <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-100 rounded-full mb-3">
                        <i class="fa fa-lock text-indigo-600 text-xl"></i>
                    </div>
                    <h1 class="text-2xl font-bold text-gray-800">Вход в систему</h1>
                </div>
                <form id="login-form" class="space-y-5">
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-envelope text-indigo-400 mr-1"></i> Email
                        </label>
                        <input id="l-email" type="email" required placeholder="your@email.com"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-lock text-indigo-400 mr-1"></i> Пароль
                        </label>
                        <input id="l-pass" type="password" required placeholder="••••••••"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all">
                    </div>
                    <div id="totp-wrap" class="hidden">
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-shield text-indigo-400 mr-1"></i> Код 2FA (Google Authenticator)
                        </label>
                        <input id="l-totp" type="text" placeholder="000000" maxlength="6"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all tracking-widest text-center text-lg">
                    </div>
                    <button type="submit"
                        class="w-full bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold py-3 rounded-xl transition-all shadow-md hover:shadow-lg">
                        Войти
                    </button>
                </form>
                <p class="text-center text-sm text-gray-500 mt-5">
                    Нет аккаунта? <a href="#/register" class="text-indigo-600 hover:underline font-medium">Зарегистрироваться</a>
                </p>
            </div>
        </div>
        <style>.inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.625rem 1rem;font-size:.875rem;outline:none;transition:box-shadow .15s,border-color .15s}.inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}.btn-primary{background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;transition:background .15s;cursor:pointer}.btn-primary:hover{background:#4338ca}.btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;transition:background .15s;cursor:pointer}.btn-secondary:hover{background:#e5e7eb}</style>
    `);
    document.getElementById('login-form').onsubmit = async e => {
        e.preventDefault();
        const btn = e.submitter; btn.disabled = true; btn.textContent = 'Входим...';
        try {
            const data = await api('/auth/login', { method: 'POST', body: JSON.stringify({
                email: v('l-email'), password: v('l-pass'),
                totpCode: v('l-totp') || undefined
            })});
            if (data.twoFactorRequired) {
                document.getElementById('totp-wrap').classList.remove('hidden');
                document.getElementById('l-totp').focus();
                toast('Введите код из Google Authenticator', 'info');
                btn.disabled = false; btn.textContent = 'Войти'; return;
            }
            setToken(data.accessToken);
            toast('Добро пожаловать!', 'success');
            go('/events');
        } catch (err) { toast(err.message, 'error'); btn.disabled = false; btn.textContent = 'Войти'; }
    };
}

// ─── Page: Register ───────────────────────────────────────────────────────────
function pageRegister() {
    render(`
        <div class="max-w-md mx-auto mt-12 fade-in">
            <div class="bg-white rounded-2xl shadow-xl p-8">
                <div class="text-center mb-6">
                    <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-100 rounded-full mb-3">
                        <i class="fa fa-user-plus text-indigo-600 text-xl"></i>
                    </div>
                    <h1 class="text-2xl font-bold text-gray-800">Регистрация</h1>
                </div>
                <form id="reg-form" class="space-y-5">
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-user text-indigo-400 mr-1"></i> Имя
                        </label>
                        <input id="r-name" type="text" required placeholder="Иван Иванов"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-envelope text-indigo-400 mr-1"></i> Email
                        </label>
                        <input id="r-email" type="email" required placeholder="your@email.com"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all">
                    </div>
                    <div>
                        <label class="block text-sm font-semibold text-gray-700 mb-1.5">
                            <i class="fa fa-lock text-indigo-400 mr-1"></i> Пароль
                        </label>
                        <input id="r-pass" type="password" required placeholder="Минимум 8 символов" minlength="8"
                            class="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-gray-800 placeholder-gray-400 bg-gray-50 focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 outline-none transition-all">
                    </div>
                    <button type="submit"
                        class="w-full bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold py-3 rounded-xl transition-all shadow-md hover:shadow-lg">
                        Зарегистрироваться
                    </button>
                </form>
                <p class="text-center text-sm text-gray-500 mt-5">
                    Уже есть аккаунт? <a href="#/login" class="text-indigo-600 hover:underline font-medium">Войти</a>
                </p>
            </div>
        </div>
    `);
    document.getElementById('reg-form').onsubmit = async e => {
        e.preventDefault();
        const btn = e.submitter; btn.disabled = true; btn.textContent = 'Регистрируем...';
        try {
            await api('/auth/register', { method: 'POST', body: JSON.stringify({
                name: v('r-name'), email: v('r-email'), password: v('r-pass')
            })});
            render(`
                <div class="max-w-md mx-auto mt-12 fade-in">
                    <div class="bg-white rounded-2xl shadow-xl p-8 text-center">
                        <div class="text-6xl mb-4">📧</div>
                        <h2 class="text-2xl font-bold text-gray-800 mb-2">Подтвердите email</h2>
                        <p class="text-gray-500 mb-5">На ваш адрес отправлено письмо с подтверждением.</p>
                        <div class="bg-amber-50 border border-amber-200 rounded-xl p-4 text-left text-sm text-amber-800 mb-5">
                            <p class="font-semibold mb-2">💡 DEV режим — получить токен верификации:</p>
                            <code class="block bg-amber-100 rounded-lg px-3 py-2 font-mono text-xs break-all">docker logs auth-service 2>&1 | findstr "verify-email"</code>
                            <p class="mt-2 text-xs">Скопируй ссылку из логов и открой её в браузере.</p>
                        </div>
                        <a href="#/login" class="btn-primary inline-block px-6 py-2">Перейти ко входу</a>
                    </div>
                </div>
            `);
        } catch (err) { toast(err.message, 'error'); btn.disabled = false; btn.textContent = 'Зарегистрироваться'; }
    };
}

// ─── Page: Events ─────────────────────────────────────────────────────────────
const TYPE_META = {
    CONFERENCE:   { label: 'Конференция',  icon: 'fa-microphone', color: 'indigo' },
    MASTERCLASS:  { label: 'Мастер-класс', icon: 'fa-chalkboard-teacher', color: 'green' },
    OLYMPIAD:     { label: 'Олимпиада',    icon: 'fa-trophy', color: 'yellow' },
    CONTEST:      { label: 'Конкурс',      icon: 'fa-award', color: 'orange' },
    MEETUP:       { label: 'Митап',        icon: 'fa-people-group', color: 'teal' },
    CONSULTATION: { label: 'Консультация', icon: 'fa-comments', color: 'blue' },
    OTHER:        { label: 'Другое',       icon: 'fa-star', color: 'gray' },
};

const STATUS_PILL = {
    OPEN:      '<span class="pill bg-green-100 text-green-700">Открыто</span>',
    CLOSED:    '<span class="pill bg-gray-100 text-gray-500">Закрыто</span>',
    CANCELLED: '<span class="pill bg-red-100 text-red-600">Отменено</span>',
};
const APP_PILL = {
    PENDING:  '<span class="pill bg-yellow-100 text-yellow-700">На рассмотрении</span>',
    APPROVED: '<span class="pill bg-green-100 text-green-700">Одобрена</span>',
    REJECTED: '<span class="pill bg-red-100 text-red-600">Отклонена</span>',
};

async function pageEvents() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const [events, myApps] = await Promise.all([
            api('/events'),
            _token ? api('/applications/my').catch(() => []) : Promise.resolve([])
        ]);
        const applied = new Set(myApps.map(a => a.eventId));

        if (!events.length) {
            render(`<div class="text-center py-24 text-gray-400 fade-in">
                <i class="fa-solid fa-calendar-xmark text-6xl mb-4 block opacity-30"></i>
                <p class="text-xl font-medium">Нет доступных мероприятий</p>
                ${isMod() ? `<a href="#/admin/events" class="mt-4 inline-block text-indigo-600 hover:underline">Создать первое мероприятие</a>` : ''}
            </div>`);
            return;
        }

        render(`
            <div class="fade-in">
                <div class="flex items-center justify-between mb-8">
                    <h1 class="text-3xl font-bold text-gray-800">Мероприятия</h1>
                    ${isMod() ? `<a href="#/admin/events" class="btn-primary px-4 py-2 flex items-center gap-2 text-sm"><i class="fa fa-plus"></i> Создать</a>` : ''}
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    ${events.map(e => eventCard(e, applied.has(e.id))).join('')}
                </div>
            </div>
            <style>
                .pill{display:inline-flex;align-items:center;padding:.125rem .625rem;border-radius:9999px;font-size:.75rem;font-weight:500}
                .btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;transition:background .15s;text-decoration:none}
                .btn-primary:hover{background:#4338ca}
            </style>
        `);
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

function eventCard(e, alreadyApplied) {
    const tm = TYPE_META[e.type] || TYPE_META.OTHER;
    const date = fmtDate(e.eventDate);
    const spotsLeft = e.maxParticipants - e.approvedCount;
    const isFull = spotsLeft <= 0;

    let action;
    if (!_token)
        action = `<a href="#/login" class="text-sm text-indigo-600 hover:underline">Войдите чтобы подать заявку →</a>`;
    else if (alreadyApplied)
        action = `<span class="text-sm text-green-600 font-medium"><i class="fa fa-check mr-1"></i>Заявка подана</span>`;
    else if (e.status !== 'OPEN' || isFull)
        action = `<span class="text-sm text-gray-400">Мест нет</span>`;
    else
        action = `<button onclick="openApplyModal(${e.id})" class="btn-sm-primary">Подать заявку</button>`;

    return `
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3 hover:shadow-md transition-shadow">
            <div class="flex items-start justify-between gap-2">
                <h3 class="font-semibold text-gray-800 text-base leading-snug">${esc(e.title)}</h3>
                ${STATUS_PILL[e.status] || ''}
            </div>
            ${e.description ? `<p class="text-gray-500 text-sm line-clamp-2">${esc(e.description)}</p>` : ''}
            <div class="text-sm text-gray-500 space-y-1">
                <div><i class="fa fa-calendar-alt w-4 mr-1 text-indigo-400"></i>${date}</div>
                <div><i class="fa fa-users w-4 mr-1 text-indigo-400"></i>${e.approvedCount} / ${e.maxParticipants} участников
                    ${!isFull ? `<span class="text-green-600 ml-1">(ещё ${spotsLeft})</span>` : `<span class="text-red-500 ml-1">(заполнено)</span>`}
                </div>
                <div><i class="fa ${tm.icon} w-4 mr-1 text-indigo-400"></i>${tm.label}</div>
            </div>
            <div class="mt-auto pt-1">${action}</div>
        </div>
        <style>
            .btn-sm-primary{background:#4f46e5;color:#fff;padding:.375rem .875rem;border-radius:.5rem;font-size:.875rem;font-weight:600;cursor:pointer;transition:background .15s;border:none}
            .btn-sm-primary:hover{background:#4338ca}
        </style>
    `;
}

function openApplyModal(eventId) {
    modal(`
        <h2 class="text-xl font-bold text-gray-800 mb-4">Подать заявку</h2>
        <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Комментарий <span class="text-gray-400">(необязательно)</span></label>
            <textarea id="apply-comment" rows="3" placeholder="Расскажите почему хотите участвовать..."
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 outline-none resize-none"></textarea>
        </div>
        <div class="flex gap-3">
            <button onclick="submitApply(${eventId})" class="btn-primary flex-1 py-2 justify-center">Подать</button>
            <button onclick="closeModal()" class="btn-secondary flex-1 py-2">Отмена</button>
        </div>
        <style>.btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;transition:background .15s}.btn-primary:hover{background:#4338ca}.btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;cursor:pointer;border:none;transition:background .15s}.btn-secondary:hover{background:#e5e7eb}</style>
    `);
}

async function submitApply(eventId) {
    try {
        await api('/applications', { method: 'POST', body: JSON.stringify({
            eventId, comment: v('apply-comment') || undefined
        })});
        closeModal();
        toast('Заявка успешно подана!', 'success');
        pageEvents();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Page: My Applications ────────────────────────────────────────────────────
async function pageMyApps() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const apps = await api('/applications/my');
        render(`
            <div class="fade-in">
                <h1 class="text-3xl font-bold text-gray-800 mb-8">Мои заявки</h1>
                ${!apps.length
                    ? `<div class="text-center py-20 text-gray-400">
                        <i class="fa-solid fa-inbox text-6xl mb-4 block opacity-30"></i>
                        <p class="text-xl font-medium">Заявок пока нет</p>
                        <a href="#/events" class="mt-3 inline-block text-indigo-600 hover:underline">Посмотреть мероприятия</a>
                       </div>`
                    : `<div class="space-y-3">
                        ${apps.map(a => `
                            <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition-shadow">
                                <div class="flex-1 min-w-0">
                                    <h3 class="font-semibold text-gray-800 truncate">${esc(a.eventTitle)}</h3>
                                    ${a.comment ? `<p class="text-sm text-gray-500 mt-0.5 truncate">${esc(a.comment)}</p>` : ''}
                                    ${a.reviewComment ? `<p class="text-sm text-indigo-500 mt-0.5 italic">"${esc(a.reviewComment)}"</p>` : ''}
                                </div>
                                <div class="flex flex-col items-end gap-2 shrink-0">
                                    ${APP_PILL[a.status] || ''}
                                    <span class="text-xs text-gray-400">${fmtDateShort(a.appliedAt)}</span>
                                    ${a.status === 'PENDING'
                                        ? `<button onclick="cancelApp(${a.id})" class="text-red-400 text-xs hover:text-red-600 hover:underline">Отозвать</button>`
                                        : ''}
                                </div>
                            </div>
                        `).join('')}
                       </div>`
                }
            </div>
            <style>.pill{display:inline-flex;align-items:center;padding:.125rem .625rem;border-radius:9999px;font-size:.75rem;font-weight:500}</style>
        `);
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

async function cancelApp(id) {
    if (!confirm('Отозвать заявку?')) return;
    try {
        await api(`/applications/${id}`, { method: 'DELETE' });
        toast('Заявка отозвана', 'info');
        pageMyApps();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Page: Profile ────────────────────────────────────────────────────────────
async function pageProfile() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const p = await api('/users/me');
        const initial = p.name.charAt(0).toUpperCase();
        const roleColor = p.role === 'ADMIN' ? 'bg-red-100 text-red-700' : p.role === 'MODERATOR' ? 'bg-yellow-100 text-yellow-700' : 'bg-indigo-100 text-indigo-700';
        render(`
            <div class="max-w-xl mx-auto fade-in">
                <h1 class="text-3xl font-bold text-gray-800 mb-6">Профиль</h1>
                <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                    <div class="flex items-center gap-4 mb-6 pb-6 border-b border-gray-100">
                        <div class="w-16 h-16 rounded-full bg-indigo-600 flex items-center justify-center text-2xl font-bold text-white shrink-0">
                            ${initial}
                        </div>
                        <div>
                            <h2 class="text-xl font-semibold text-gray-800">${esc(p.name)}</h2>
                            <p class="text-gray-500 text-sm">${esc(p.email)}</p>
                            <span class="inline-flex mt-1 text-xs px-2.5 py-0.5 rounded-full font-medium ${roleColor}">${p.role}</span>
                        </div>
                    </div>
                    <form id="profile-form" class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">Имя</label>
                            <input id="pf-name" value="${esc(p.name)}" type="text" required class="inp">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">О себе</label>
                            <textarea id="pf-bio" rows="3" class="inp resize-none" placeholder="Расскажите о себе...">${esc(p.bio || '')}</textarea>
                        </div>
                        <button type="submit" class="btn-primary w-full py-2.5 justify-center">Сохранить изменения</button>
                    </form>
                </div>
                <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mt-4">
                    <h3 class="font-semibold text-gray-800 mb-3">Двухфакторная аутентификация (2FA)</h3>
                    <p class="text-sm text-gray-500 mb-4">Защитите аккаунт с помощью Google Authenticator.</p>
                    ${p.twoFactorEnabled
                        ? `<button onclick="disable2FA()" class="text-red-500 border border-red-200 rounded-lg px-4 py-2 text-sm hover:bg-red-50 transition">Отключить 2FA</button>`
                        : `<button onclick="setup2FA()" class="btn-primary px-4 py-2 text-sm">Настроить 2FA</button>`
                    }
                </div>
            </div>
            <style>
                .inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.625rem 1rem;font-size:.875rem;outline:none;transition:box-shadow .15s,border-color .15s}
                .inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}
                .btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;transition:background .15s;border:none}
                .btn-primary:hover{background:#4338ca}
            </style>
        `);
        document.getElementById('profile-form').onsubmit = async e => {
            e.preventDefault();
            try {
                await api('/users/me', { method: 'PUT', body: JSON.stringify({ name: v('pf-name'), bio: v('pf-bio') }) });
                toast('Профиль обновлён!', 'success');
            } catch (err) { toast(err.message, 'error'); }
        };
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

async function setup2FA() {
    try {
        const data = await api('/auth/2fa/setup', { method: 'POST' });
        modal(`
            <h2 class="text-xl font-bold mb-3">Настройка 2FA</h2>
            <p class="text-sm text-gray-500 mb-3">Отсканируйте QR-код в Google Authenticator или введите секрет вручную:</p>
            <div class="bg-gray-50 rounded-lg p-3 font-mono text-sm break-all mb-3">${esc(data.secret)}</div>
            <img src="${esc(data.qrCodeUrl)}" class="mx-auto mb-4 rounded-lg" width="180" onerror="this.style.display='none'">
            <p class="text-sm text-gray-500 mb-2">Введите код подтверждения из приложения:</p>
            <input id="totp-confirm" type="text" placeholder="000000" maxlength="6" class="inp mb-4">
            <div class="flex gap-3">
                <button onclick="confirm2FA()" class="btn-primary flex-1 py-2 justify-center">Подтвердить</button>
                <button onclick="closeModal()" class="btn-secondary flex-1 py-2">Отмена</button>
            </div>
            <style>.inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.625rem 1rem;font-size:.875rem;outline:none}.inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}.btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;border:none}.btn-primary:hover{background:#4338ca}.btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;cursor:pointer;border:none}</style>
        `);
    } catch (err) { toast(err.message, 'error'); }
}

async function confirm2FA() {
    try {
        await api('/auth/2fa/confirm', { method: 'POST', body: JSON.stringify({ code: v('totp-confirm') }) });
        closeModal(); toast('2FA включена!', 'success'); pageProfile();
    } catch (err) { toast(err.message, 'error'); }
}

async function disable2FA() {
    if (!confirm('Отключить 2FA?')) return;
    try {
        await api('/auth/2fa/disable', { method: 'POST' });
        toast('2FA отключена', 'info'); pageProfile();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Admin: Events ────────────────────────────────────────────────────────────
async function pageAdminEvents() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const events = await api('/events/all');
        events.forEach(e => { _eventStore[e.id] = e; });
        render(`
            <div class="fade-in">
                <div class="flex items-center justify-between mb-6">
                    <h1 class="text-3xl font-bold text-gray-800">Управление мероприятиями</h1>
                    <button onclick="openCreateEvent()" class="btn-primary px-4 py-2 gap-2 text-sm"><i class="fa fa-plus"></i> Создать</button>
                </div>
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
                    <table class="w-full text-sm min-w-[600px]">
                        <thead class="bg-gray-50 text-gray-500 font-medium text-left">
                            <tr>
                                <th class="px-5 py-3">Название</th>
                                <th class="px-5 py-3">Дата</th>
                                <th class="px-5 py-3">Тип</th>
                                <th class="px-5 py-3">Статус</th>
                                <th class="px-5 py-3">Участники</th>
                                <th class="px-5 py-3"></th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100">
                            ${!events.length ? `<tr><td colspan="6" class="text-center py-8 text-gray-400">Мероприятий пока нет</td></tr>` :
                            events.map(e => `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-5 py-3 font-medium text-gray-800">${esc(e.title)}</td>
                                    <td class="px-5 py-3 text-gray-500">${fmtDateShort(e.eventDate)}</td>
                                    <td class="px-5 py-3 text-gray-500">${TYPE_META[e.type]?.label || e.type}</td>
                                    <td class="px-5 py-3">${STATUS_PILL[e.status] || e.status}</td>
                                    <td class="px-5 py-3 text-gray-500">${e.approvedCount}/${e.maxParticipants}</td>
                                    <td class="px-5 py-3">
                                        <div class="flex gap-3 justify-end">
                                            <button onclick="openEditEvent(${e.id})" class="text-indigo-500 hover:text-indigo-700 transition" title="Редактировать"><i class="fa fa-edit"></i></button>
                                            ${isAdmin() ? `<button onclick="deleteEvent(${e.id})" class="text-red-400 hover:text-red-600 transition" title="Удалить"><i class="fa fa-trash"></i></button>` : ''}
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
            <style>
                .btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;transition:background .15s;border:none;gap:.5rem}
                .btn-primary:hover{background:#4338ca}
                .pill{display:inline-flex;align-items:center;padding:.125rem .625rem;border-radius:9999px;font-size:.75rem;font-weight:500}
            </style>
        `);
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

function openCreateEvent() { modal(eventFormModal()); }
function openEditEvent(id)  { modal(eventFormModal(_eventStore[id])); }

function eventFormModal(e = {}) {
    const isEdit = !!e.id;
    const dt = e.eventDate ? e.eventDate.slice(0, 16) : '';
    return `
        <h2 class="text-xl font-bold text-gray-800 mb-4">${isEdit ? 'Редактировать' : 'Создать мероприятие'}</h2>
        <div class="space-y-3">
            <input id="ef-title" value="${esc(e.title || '')}" placeholder="Название *" required
                class="inp">
            <textarea id="ef-desc" rows="2" placeholder="Описание" class="inp resize-none">${esc(e.description || '')}</textarea>
            <input id="ef-date" type="datetime-local" value="${dt}" required class="inp">
            <div class="grid grid-cols-2 gap-3">
                <input id="ef-max" type="number" min="1" value="${e.maxParticipants || ''}" placeholder="Макс. участников *" class="inp">
                <select id="ef-type" class="inp">
                    ${Object.entries(TYPE_META).map(([k,v]) => `<option value="${k}" ${e.type===k?'selected':''}>${v.label}</option>`).join('')}
                </select>
            </div>
            ${isEdit ? `<select id="ef-status" class="inp">
                <option value="OPEN" ${e.status==='OPEN'?'selected':''}>Открыто</option>
                <option value="CLOSED" ${e.status==='CLOSED'?'selected':''}>Закрыто</option>
                <option value="CANCELLED" ${e.status==='CANCELLED'?'selected':''}>Отменено</option>
            </select>` : ''}
        </div>
        <div class="flex gap-3 mt-5">
            <button onclick="${isEdit ? `submitEditEvent(${e.id})` : 'submitCreateEvent()'}"
                class="btn-primary flex-1 py-2 justify-center">${isEdit ? 'Сохранить' : 'Создать'}</button>
            <button onclick="closeModal()" class="btn-secondary flex-1 py-2">Отмена</button>
        </div>
        <style>
            .inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.5rem .75rem;font-size:.875rem;outline:none;transition:box-shadow .15s,border-color .15s}
            .inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}
            .btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;transition:background .15s;border:none}
            .btn-primary:hover{background:#4338ca}
            .btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;cursor:pointer;border:none;transition:background .15s}
            .btn-secondary:hover{background:#e5e7eb}
        </style>
    `;
}

function getFormData() {
    return {
        title:           v('ef-title'),
        description:     v('ef-desc') || undefined,
        eventDate:       v('ef-date') + ':00',
        maxParticipants: parseInt(v('ef-max')),
        type:            v('ef-type'),
        status:          document.getElementById('ef-status')?.value,
    };
}

async function submitCreateEvent() {
    try {
        await api('/events', { method: 'POST', body: JSON.stringify(getFormData()) });
        closeModal(); toast('Мероприятие создано!', 'success'); pageAdminEvents();
    } catch (err) { toast(err.message, 'error'); }
}

async function submitEditEvent(id) {
    try {
        await api(`/events/${id}`, { method: 'PUT', body: JSON.stringify(getFormData()) });
        closeModal(); toast('Сохранено!', 'success'); pageAdminEvents();
    } catch (err) { toast(err.message, 'error'); }
}

async function deleteEvent(id) {
    if (!confirm('Удалить мероприятие? Это действие необратимо.')) return;
    try {
        await api(`/events/${id}`, { method: 'DELETE' });
        toast('Мероприятие удалено', 'info'); pageAdminEvents();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Admin: Applications ──────────────────────────────────────────────────────
async function pageAdminApplications() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const events = await api('/events/all');
        const sections = await Promise.all(events.map(async e => {
            const apps = await api(`/applications/event/${e.id}`).catch(() => []);
            return { event: e, apps: apps.filter(a => a.status === 'PENDING') };
        }));
        const hasPending = sections.some(s => s.apps.length > 0);

        render(`
            <div class="fade-in">
                <h1 class="text-3xl font-bold text-gray-800 mb-6">Рассмотрение заявок</h1>
                ${!hasPending
                    ? `<div class="text-center py-20 text-gray-400">
                        <i class="fa-solid fa-check-circle text-6xl mb-4 block opacity-30"></i>
                        <p class="text-xl font-medium">Нет заявок на рассмотрении</p>
                       </div>`
                    : sections.filter(s => s.apps.length).map(({ event, apps }) => `
                        <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-4">
                            <div class="px-5 py-3 bg-indigo-50 border-b border-indigo-100 flex items-center justify-between">
                                <h2 class="font-semibold text-gray-800">${esc(event.title)}</h2>
                                <span class="text-sm text-indigo-600">${apps.length} заявок</span>
                            </div>
                            <table class="w-full text-sm">
                                <tbody class="divide-y divide-gray-50">
                                    ${apps.map(a => `
                                        <tr class="hover:bg-gray-50">
                                            <td class="px-5 py-3">
                                                <p class="font-medium text-gray-800">${esc(a.userName)}</p>
                                                <p class="text-gray-400 text-xs">${esc(a.userEmail)}</p>
                                            </td>
                                            <td class="px-5 py-3 text-gray-500 max-w-xs truncate">${esc(a.comment || '—')}</td>
                                            <td class="px-5 py-3 text-gray-400 text-xs whitespace-nowrap">${fmtDateShort(a.appliedAt)}</td>
                                            <td class="px-5 py-3">
                                                <div class="flex gap-2 justify-end">
                                                    <button onclick="reviewApp(${a.id},'APPROVED')" class="bg-green-100 text-green-700 px-3 py-1 rounded-lg text-xs font-medium hover:bg-green-200 transition">Одобрить</button>
                                                    <button onclick="reviewApp(${a.id},'REJECTED')" class="bg-red-100 text-red-600 px-3 py-1 rounded-lg text-xs font-medium hover:bg-red-200 transition">Отклонить</button>
                                                </div>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `).join('')
                }
            </div>
        `);
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

function reviewApp(id, status) {
    const isApprove = status === 'APPROVED';
    modal(`
        <h2 class="text-xl font-bold mb-3">${isApprove ? '✅ Одобрить заявку' : '❌ Отклонить заявку'}</h2>
        <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Комментарий <span class="text-gray-400">(необязательно)</span></label>
            <textarea id="review-comment" rows="2" class="inp resize-none" placeholder="Комментарий для участника..."></textarea>
        </div>
        <div class="flex gap-3">
            <button onclick="submitReview(${id},'${status}')"
                class="flex-1 py-2 rounded-lg font-semibold text-white cursor-pointer border-none ${isApprove ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'} transition">
                ${isApprove ? 'Одобрить' : 'Отклонить'}
            </button>
            <button onclick="closeModal()" class="btn-secondary flex-1 py-2">Отмена</button>
        </div>
        <style>.inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.5rem .75rem;font-size:.875rem;outline:none}.inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}.btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;cursor:pointer;border:none}</style>
    `);
}

async function submitReview(id, status) {
    try {
        await api(`/applications/${id}/review`, { method: 'PUT', body: JSON.stringify({
            status, reviewComment: v('review-comment') || undefined
        })});
        closeModal();
        toast(status === 'APPROVED' ? 'Заявка одобрена!' : 'Заявка отклонена', status === 'APPROVED' ? 'success' : 'info');
        pageAdminApplications();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Admin: Users ─────────────────────────────────────────────────────────────
async function pageAdminUsers() {
    render('<div class="text-center py-20 text-gray-400"><i class="fa fa-spinner fa-spin text-4xl"></i></div>');
    try {
        const users = await api('/admin/users');
        render(`
            <div class="fade-in">
                <div class="flex items-center justify-between mb-6">
                    <h1 class="text-3xl font-bold text-gray-800">Пользователи</h1>
                    <span class="text-gray-500 text-sm">${users.length} пользователей</span>
                </div>
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
                    <table class="w-full text-sm min-w-[500px]">
                        <thead class="bg-gray-50 text-gray-500 font-medium text-left">
                            <tr>
                                <th class="px-5 py-3">Пользователь</th>
                                <th class="px-5 py-3">Email</th>
                                <th class="px-5 py-3">Роль</th>
                                <th class="px-5 py-3">Зарегистрирован</th>
                                <th class="px-5 py-3"></th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100">
                            ${users.map(u => {
                                const rc = u.role === 'ADMIN' ? 'bg-red-100 text-red-700' : u.role === 'MODERATOR' ? 'bg-yellow-100 text-yellow-700' : 'bg-indigo-100 text-indigo-700';
                                return `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-5 py-3">
                                        <div class="flex items-center gap-3">
                                            <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white text-sm font-bold shrink-0">
                                                ${u.name.charAt(0).toUpperCase()}
                                            </div>
                                            <span class="font-medium text-gray-800">${esc(u.name)}</span>
                                        </div>
                                    </td>
                                    <td class="px-5 py-3 text-gray-500">${esc(u.email)}</td>
                                    <td class="px-5 py-3"><span class="pill ${rc}">${u.role}</span></td>
                                    <td class="px-5 py-3 text-gray-400">${fmtDateShort(u.createdAt)}</td>
                                    <td class="px-5 py-3 text-right">
                                        <button onclick="openChangeRole(${u.id},'${u.role}')" class="text-indigo-500 text-xs hover:text-indigo-700 hover:underline">Сменить роль</button>
                                    </td>
                                </tr>`;
                            }).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
            <style>.pill{display:inline-flex;align-items:center;padding:.125rem .625rem;border-radius:9999px;font-size:.75rem;font-weight:500}</style>
        `);
    } catch (err) { render(`<div class="text-red-500 py-10 text-center">${err.message}</div>`); }
}

function openChangeRole(userId, current) {
    modal(`
        <h2 class="text-xl font-bold mb-4">Сменить роль</h2>
        <div class="space-y-3 mb-5">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Новая роль</label>
                <select id="new-role" class="inp">
                    <option value="USER"      ${current==='USER'?'selected':''}>USER — обычный пользователь</option>
                    <option value="MODERATOR" ${current==='MODERATOR'?'selected':''}>MODERATOR — может управлять мероприятиями</option>
                    <option value="ADMIN"     ${current==='ADMIN'?'selected':''}>ADMIN — полный доступ</option>
                </select>
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">
                    Security Code <span class="text-gray-400 font-normal">(защитный код из конфига)</span>
                </label>
                <input id="sec-code" type="password" placeholder="admin-secure-2025" class="inp">
            </div>
        </div>
        <div class="flex gap-3">
            <button onclick="submitChangeRole(${userId})" class="btn-primary flex-1 py-2 justify-center">Применить</button>
            <button onclick="closeModal()" class="btn-secondary flex-1 py-2">Отмена</button>
        </div>
        <style>.inp{display:block;width:100%;border:1px solid #d1d5db;border-radius:.5rem;padding:.5rem .75rem;font-size:.875rem;outline:none;transition:box-shadow .15s,border-color .15s}.inp:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.15)}.btn-primary{display:inline-flex;align-items:center;background:#4f46e5;color:#fff;border-radius:.5rem;font-weight:600;cursor:pointer;border:none;transition:background .15s}.btn-primary:hover{background:#4338ca}.btn-secondary{background:#f3f4f6;color:#374151;border-radius:.5rem;font-weight:600;cursor:pointer;border:none}</style>
    `);
}

async function submitChangeRole(userId) {
    try {
        await api(`/admin/users/${userId}/role`, { method: 'PUT', body: JSON.stringify({
            role: v('new-role'), securityCode: v('sec-code')
        })});
        closeModal(); toast('Роль изменена!', 'success'); pageAdminUsers();
    } catch (err) { toast(err.message, 'error'); }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function render(html) { document.getElementById('app').innerHTML = html; }
function modal(html)  { document.getElementById('modal-body').innerHTML = html; document.getElementById('modal-overlay').classList.remove('hidden'); }
function closeModal() { document.getElementById('modal-overlay').classList.add('hidden'); }
function closeModalOutside(e) { if (e.target === e.currentTarget) closeModal(); }
function v(id)        { return (document.getElementById(id)?.value || '').trim(); }
function esc(s)       { return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
function logout()     { setToken(null); go('/login'); }

function fmtDate(d) {
    return new Date(d).toLocaleString('ru-RU', { day:'numeric', month:'long', year:'numeric', hour:'2-digit', minute:'2-digit' });
}
function fmtDateShort(d) {
    return new Date(d).toLocaleDateString('ru-RU', { day:'numeric', month:'short', year:'numeric' });
}

function toast(msg, type = 'info') {
    const colors = { success: 'bg-green-500', error: 'bg-red-500', info: 'bg-blue-500' };
    const icons  = { success: 'fa-check-circle', error: 'fa-circle-exclamation', info: 'fa-circle-info' };
    const el = document.createElement('div');
    el.className = `pointer-events-auto ${colors[type]||colors.info} text-white px-4 py-3 rounded-xl shadow-lg text-sm flex items-center gap-2 max-w-sm fade-in`;
    el.innerHTML = `<i class="fa ${icons[type]||icons.info}"></i><span>${esc(msg)}</span>`;
    document.getElementById('toasts').appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .3s'; setTimeout(() => el.remove(), 300); }, 3500);
}
