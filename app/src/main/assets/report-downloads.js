(() => {
  if (window !== window.top || !window.GastoDownloads || window.__giDownloadsInstalled) return;
  window.__giDownloadsInstalled = true;
  const blobs = new Map();
  const create = URL.createObjectURL.bind(URL);
  const revoke = URL.revokeObjectURL.bind(URL);
  URL.createObjectURL = (value) => {
    const url = create(value);
    if (value instanceof Blob) blobs.set(url, value);
    return url;
  };
  URL.revokeObjectURL = (url) => { blobs.delete(url); revoke(url); };
  let busy = false;
  let waiting;
  window.GastoDownloads.onmessage = (event) => {
    if (!waiting) return;
    try {
      const result = JSON.parse(event.data);
      if (result.id !== waiting.id) return;
      const pending = waiting;
      waiting = undefined;
      clearTimeout(pending.timer);
      result.ok ? pending.resolve() : pending.reject(new Error(result.error || 'Não foi possível exportar o relatório.'));
    } catch (_) { /* Ignore malformed responses; timeout completes the request. */ }
  };
  function send(message) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => { waiting = undefined; reject(new Error('A exportação demorou demais. Tente novamente.')); }, 30000);
      waiting = { id: message.id, resolve, reject, timer };
      window.GastoDownloads.postMessage(JSON.stringify(message));
    });
  }
  async function save(blob, name) {
    if (busy) { alert('Conclua a exportação em andamento antes de iniciar outra.'); return; }
    busy = true;
    const id = Array.from(crypto.getRandomValues(new Uint8Array(16)), b => b.toString(16).padStart(2, '0')).join('');
    try {
      if (blob.size > 25 * 1024 * 1024) throw new Error('O relatório excede o limite de 25 MB. Exporte um período menor.');
      await send({ type: 'begin', id, name, mime: blob.type, size: blob.size });
      for (let offset = 0; offset < blob.size; offset += 32768) {
        const bytes = new Uint8Array(await blob.slice(offset, offset + 32768).arrayBuffer());
        let binary = '';
        for (const byte of bytes) binary += String.fromCharCode(byte);
        await send({ type: 'chunk', id, data: btoa(binary) });
      }
      await send({ type: 'end', id });
    } catch (error) {
      window.GastoDownloads.postMessage(JSON.stringify({ type: 'cancel', id }));
      alert(error.message || 'Não foi possível exportar o relatório.');
    } finally { busy = false; }
  }
  function intercept(anchor) {
    if (!anchor.hasAttribute('download') || !anchor.href.startsWith('blob:')) return false;
    const blob = blobs.get(anchor.href);
    if (!blob) { alert('Este arquivo não está mais disponível. Gere o relatório novamente.'); return true; }
    // Capture the Blob synchronously: the existing site revokes some URLs immediately after click().
    void save(blob, anchor.download || 'relatorio');
    return true;
  }
  const click = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () { if (!intercept(this)) click.call(this); };
  // jsPDF/FileSaver dispatches a synthetic click on a detached anchor.
  const dispatch = HTMLAnchorElement.prototype.dispatchEvent;
  HTMLAnchorElement.prototype.dispatchEvent = function (event) {
    if (event.type === 'click' && intercept(this)) { event.preventDefault(); return false; }
    return dispatch.call(this, event);
  };
  document.addEventListener('click', (event) => {
    const anchor = event.target instanceof Element ? event.target.closest('a[download]') : null;
    if (anchor && intercept(anchor)) { event.preventDefault(); event.stopImmediatePropagation(); }
  }, true);
})();
