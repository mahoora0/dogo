(function () {
  const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
  const originalFetch = window.fetch.bind(window);

  function metaContent(name) {
    const meta = document.querySelector(`meta[name="${name}"]`);
    return meta ? meta.getAttribute('content') : null;
  }

  function csrfHeaderName() {
    return metaContent('_csrf_header');
  }

  function csrfToken() {
    return metaContent('_csrf');
  }

  function isSameOrigin(input) {
    const url = input instanceof Request ? input.url : input;
    return new URL(url, window.location.href).origin === window.location.origin;
  }

  function csrfHeaders(existingHeaders) {
    const headers = new Headers(existingHeaders || {});
    const headerName = csrfHeaderName();
    const token = csrfToken();

    if (headerName && token && !headers.has(headerName)) {
      headers.set(headerName, token);
    }

    return headers;
  }

  window.dogoCsrf = {
    headerName: csrfHeaderName,
    token: csrfToken,
    headers: csrfHeaders
  };

  window.fetch = function (input, init) {
    const options = init ? { ...init } : {};
    const method = (options.method || (input instanceof Request ? input.method : 'GET')).toUpperCase();

    if (unsafeMethods.has(method) && isSameOrigin(input)) {
      options.headers = csrfHeaders(options.headers || (input instanceof Request ? input.headers : undefined));
    }

    return originalFetch(input, options);
  };
})();
