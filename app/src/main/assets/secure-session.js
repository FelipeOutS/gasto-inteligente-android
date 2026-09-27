(() => {
  if (window !== window.top || !window.GastoSecureSession) return;

  let saved = false;
  let sequence = 0;
  const pending = new Map();

  function request(type, data = {}) {
    const id = String(++sequence);

    return new Promise((resolve) => {
      const timer = setTimeout(() => {
        pending.delete(id);

        resolve({
          success: false,
          error: 'A operação expirou. Tente novamente.',
        });
      }, 55000);

      pending.set(id, {
        resolve,
        timer,
      });

      window.GastoSecureSession.postMessage(
        JSON.stringify({
          id,
          type,
          ...data,
        }),
      );
    });
  }

  window.GastoSecureSession.onmessage = ({data}) => {
    let result;

    try {
      result = JSON.parse(data);
    } catch (_) {
      return;
    }

    const entry = pending.get(result.id);

    if (!entry) return;

    pending.delete(result.id);
    clearTimeout(entry.timer);

    if (typeof result.saved === 'boolean') {
      saved = result.saved;
    }

    entry.resolve(result);

    window.dispatchEvent(
      new Event('AndroidSecureSessionChanged'),
    );
  };

  /*
   * =====================================================
   * SESSÃO SEGURA DO APLICATIVO
   * =====================================================
   */

  window.AndroidSecureSession = {
    hasSession: () => saved,

    getSavedEmail: () => null,

    saveSession: async (
      access_token,
      refresh_token,
      userId,
      email,
    ) =>
      (
        await request('save', {
          access_token,
          refresh_token,
          userId,
          email,
        })
      ).success === true,

    clearSession: () => {
      saved = false;

      return request('clear');
    },

    unlockSession: async () => {
      const detail = await request('unlock');

      window.dispatchEvent(
        new CustomEvent(
          'AndroidSecureSessionResult',
          {
            detail,
          },
        ),
      );
    },
  };

  /*
   * =====================================================
   * COFRE — BIOMETRIA NATIVA
   * =====================================================
   *
   * Mantém o mecanismo existente.
   */

  window.NativeVault = {
    version: 2,

    status: () =>
      request(
        'vaultStatus',
      ),

    enroll: (
      userId,
      master_key,
    ) =>
      request(
        'vaultSave',
        {
          userId,
          master_key,
        },
      ),

    unlock: (
      userId,
    ) =>
      request(
        'vaultUnlock',
        {
          userId,
        },
      ),

    clear: (
      userId,
    ) =>
      request(
        'vaultClear',
        {
          userId,
        },
      ),
  };

  /*
   * =====================================================
   * COFRE — PIN LOCAL SEGURO
   * =====================================================
   *
   * O PIN é processado pelo Android nativo.
   * Este bridge apenas transporta a solicitação durante
   * a operação e não persiste PIN ou Master Key.
   *
   * O PIN legado do servidor continua intacto enquanto
   * a nova migração não for concluída.
   */

  window.NativeVaultPin = {
    version: 1,

    status: (
      userId,
    ) =>
      request(
        'vaultPinStatus',
        {
          userId,
        },
      ),

    enroll: (
      userId,
      pin,
      master_key,
    ) =>
      request(
        'vaultPinEnroll',
        {
          userId,
          pin,
          master_key,
        },
      ),

    unlock: (
      userId,
      pin,
    ) =>
      request(
        'vaultPinUnlock',
        {
          userId,
          pin,
        },
      ),

    clear: (
      userId,
    ) =>
      request(
        'vaultPinClear',
        {
          userId,
        },
      ),
  };

  /*
   * Inicializa o estado da sessão.
   */

  void request('status');
})();