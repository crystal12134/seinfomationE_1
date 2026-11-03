// Authentication uses an HttpOnly cookie. Only CSRF and non-secret display data are read here.
window.seinfoSecurity = {
    csrf: function () {
        const item = document.cookie.split(';').map(v => v.trim()).find(v => v.startsWith('XSRF-TOKEN='));
        return item ? decodeURIComponent(item.substring(11)) : '';
    },
    sameOrigin: function (url) { return new URL(url || location.href, location.href).origin === location.origin; }
};
$.ajaxPrefilter(function (options, original, xhr) {
    if (seinfoSecurity.sameOrigin(options.url) && !/^(GET|HEAD|OPTIONS|TRACE)$/i.test(options.type || 'GET'))
        xhr.setRequestHeader('X-XSRF-TOKEN', seinfoSecurity.csrf());
});
if (window.fetch) {
    const originalFetch = window.fetch.bind(window);
    window.fetch = function (input, init) {
        const request = new Request(input, init);
        if (seinfoSecurity.sameOrigin(request.url) && !/^(GET|HEAD|OPTIONS|TRACE)$/i.test(request.method)) {
            const headers = new Headers(request.headers);
            headers.set('X-XSRF-TOKEN', seinfoSecurity.csrf());
            return originalFetch(new Request(request, {headers: headers}));
        }
        return originalFetch(request);
    };
}
document.addEventListener('submit', function (event) {
    const form = event.target;
    if (form.tagName === 'FORM' && seinfoSecurity.sameOrigin(form.action) && (form.method || '').toLowerCase() === 'post') {
        let input = form.querySelector('input[name="_csrf"]');
        if (!input) { input = document.createElement('input'); input.type = 'hidden'; input.name = '_csrf'; form.appendChild(input); }
        input.value = seinfoSecurity.csrf();
    }
}, true);
pf.escapeHtml = function (value) {
    return String(value == null ? '' : value).replace(/[&<>"'`]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;'}[c]));
};
pf.safeId = function (value) { const id=String(value == null ? '' : value); return /^[A-Za-z0-9_-]{1,120}$/.test(id)?id:''; };
