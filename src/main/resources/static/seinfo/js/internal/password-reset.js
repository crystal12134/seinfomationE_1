(() => {
    'use strict';
    let token = new URLSearchParams(location.hash.slice(1)).get('token') || '';
    history.replaceState(null, '', location.pathname);
    const form = document.getElementById('resetForm');
    const button = document.getElementById('saveButton');
    const result = document.getElementById('result');
    if (!/^[A-Za-z0-9_-]{43}$/.test(token)) {
        result.textContent = '메일의 재설정 링크로 접속해주세요. 링크가 만료됐다면 비밀번호 찾기를 다시 진행해주세요.';
        button.disabled = true;
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const password = document.getElementById('newPassword').value;
        if (password !== document.getElementById('confirmPassword').value) {
            result.textContent = '입력한 비밀번호가 서로 다릅니다.'; return;
        }
        const cookie = document.cookie.split(';').map(v => v.trim()).find(v => v.startsWith('XSRF-TOKEN='));
        if (!cookie) { result.textContent = '요청 인증값이 없습니다. 메일의 링크를 다시 열어주세요.'; return; }
        button.disabled = true;
        try {
            const response = await fetch('/login/passwordReset/complete', {
                method: 'POST', credentials: 'same-origin',
                headers: {'Content-Type':'application/json','X-XSRF-TOKEN':decodeURIComponent(cookie.slice(11))},
                body: JSON.stringify({token:token,newPassword:password})
            });
            const data = await response.json();
            result.textContent = data.message || '처리하지 못했습니다. 잠시 후 다시 시도해주세요.';
            if (response.ok) { token = ''; form.reset(); return; }
        } catch (error) { result.textContent = '서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.'; }
        button.disabled = false;
    });
})();
