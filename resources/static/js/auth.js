(() => {
    const meta = document.querySelector('meta[name="access-token"]');
    let token = meta ? meta.content : null;
    if (meta) meta.remove();

    const sameOrigin = url => new URL(url, location.href).origin === location.origin;
    const headers = url => token && sameOrigin(url) ? {Authorization: `Bearer ${token}`} : {};

    $.ajaxPrefilter((options, original, xhr) => {
        if (token && sameOrigin(options.url)) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    });
    $(document).ajaxError((event, xhr) => {
        if (xhr.status === 401) location.assign('/view/login?expired');
    });

    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || event.defaultPrevented
                || form.method.toLowerCase() !== 'post' || !sameOrigin(form.action)) return;
        event.preventDefault();
        const button = event.submitter;
        if (button) button.disabled = true;
        try {
            const data = new FormData(form);
            let body = form.enctype === 'multipart/form-data' ? data : new URLSearchParams(data);
            const requestHeaders = headers(form.action);
            if (form.hasAttribute('data-login')) {
                body = JSON.stringify({username: data.get('username'), password: data.get('password')});
                requestHeaders['Content-Type'] = 'application/json';
            }
            const response = await fetch(form.action, {
                method: 'POST', credentials: 'same-origin', headers: requestHeaders, body
            });
            if (form.hasAttribute('data-login')) {
                if (!response.ok) throw new Error('Не удалось войти. Проверьте логин и пароль.');
                token = (await response.json()).accessToken;
                location.assign('/');
            } else if (response.status === 401) {
                location.assign('/view/login?expired');
            } else if (new URL(form.action).pathname === '/api/auth/logout' && response.ok) {
                token = null;
                location.assign('/');
            } else if (response.redirected) {
                location.assign(response.url);
            } else if ((response.headers.get('content-type') || '').includes('text/html')) {
                // Validation errors return the original server-rendered form.
                const html = await response.text();
                document.open(); document.write(html); document.close();
            } else if (!response.ok) {
                const error = await response.json();
                throw new Error(error.detail || 'Не удалось сохранить изменения.');
            } else {
                location.reload();
            }
        } catch (error) {
            let message = form.querySelector('[data-auth-error]');
            if (!message) {
                message = document.createElement('p');
                message.dataset.authError = '';
                message.className = 'alert alert-danger';
                message.setAttribute('role', 'alert');
                form.prepend(message);
            }
            message.textContent = error.message;
        } finally {
            if (button) button.disabled = false;
        }
    });
})();
