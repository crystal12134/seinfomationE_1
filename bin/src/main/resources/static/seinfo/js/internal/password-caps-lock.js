/* 로그인 아이디/비밀번호 입력값을 변경하지 않고 Caps Lock 상태를 표시합니다. */
(() => {
    'use strict';
    if (window.seinfoPasswordCapsLockInstalled) return;
    window.seinfoPasswordCapsLockInstalled = true;

    const hints = new WeakMap();
    let nextId = 0;

    function isTarget(input) {
        return input instanceof HTMLInputElement
            && (input.type === 'password' || input.dataset.capsLock === 'login-id')
            && !input.disabled && !input.readOnly
            && input.id !== 'modalAuthNo'
            && input.inputMode !== 'numeric'
            && input.dataset.passwordHint !== 'off';
    }

    function hintFor(input) {
        let hint = hints.get(input);
        if (hint) return hint;

        let hintId;
        do { hintId = `password-caps-lock-hint-${++nextId}`; }
        while (document.getElementById(hintId));

        const box = document.createElement('div');
        box.id = hintId;
        box.className = 'password-input-hint';
        box.hidden = true;

        const status = document.createElement('span');
        status.className = 'password-input-hint__status';
        status.setAttribute('role', 'status');
        status.setAttribute('aria-live', 'polite');
        status.setAttribute('aria-atomic', 'true');
        box.appendChild(status);

        const wrapper = input.closest('.input-area') || input;
        wrapper.insertAdjacentElement('afterend', box);
        const describedBy = (input.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
        describedBy.push(hintId);
        input.setAttribute('aria-describedby', describedBy.join(' '));

        hint = { box, status };
        hints.set(input, hint);
        return hint;
    }

    function showUnknown(input) {
        if (!isTarget(input)) return;
        const hint = hintFor(input);
        hint.box.hidden = true;
        hint.box.classList.remove('is-caps-lock-on');
        hint.status.textContent = '';
    }

    function hide(input) {
        const hint = hints.get(input);
        if (hint) hint.box.hidden = true;
    }

    document.addEventListener('focusin', event => showUnknown(event.target));
    document.addEventListener('focusout', event => hide(event.target));

    function updateFromKeyboard(event) {
        const input = event.target;
        if (!isTarget(input) || typeof event.getModifierState !== 'function') return;
        const hint = hintFor(input);
        const enabled = event.getModifierState('CapsLock');
        hint.box.hidden = !enabled;
        hint.box.classList.toggle('is-caps-lock-on', enabled);
        const text = enabled ? 'Caps Lock:켜짐' : '';
        if (hint.status.textContent !== text) hint.status.textContent = text;
    }

    document.addEventListener('keydown', updateFromKeyboard, true);
    document.addEventListener('keyup', updateFromKeyboard, true);
    window.addEventListener('blur', () => hide(document.activeElement));
    window.addEventListener('focus', () => showUnknown(document.activeElement));

    // autofocus가 공통 스크립트 로드 전에 적용된 경우도 처리합니다.
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => showUnknown(document.activeElement), { once: true });
    } else {
        showUnknown(document.activeElement);
    }
})();
