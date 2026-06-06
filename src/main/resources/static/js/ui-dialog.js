/*
 * 공통 다이얼로그 헬퍼 (SweetAlert2 기반)
 * - window.alert 를 SweetAlert2 모달로 대체
 * - window.confirmDialog(message, opts) : Promise<boolean> 반환
 * - <form data-confirm="메시지"> 자동 처리 (기존 onsubmit="return confirm(...)" 대체)
 */
(function () {
  var nativeAlert = window.alert.bind(window);

  var BRAND = '#0f172a'; // slate-900
  var DANGER = '#dc2626'; // red-600

  function swalReady() {
    return typeof window.Swal !== 'undefined';
  }

  // 사이트 디자인 톤에 맞춘 모달 스타일 1회 주입
  function injectStyles() {
    if (document.getElementById('dogo-swal-style')) return;
    var css =
      '.dogo-swal-popup{border-radius:1.25rem;padding:1.75rem 1.5rem 1.5rem;' +
      'border:1px solid #e2e8f0;box-shadow:0 24px 48px -16px rgba(15,23,42,.28);font-family:inherit;}' +
      '.dogo-swal-title{font-size:1.125rem;line-height:1.4;font-weight:700;color:#0f172a;padding:0;}' +
      '.dogo-swal-text{font-size:.9rem;line-height:1.55;color:#64748b;margin-top:.5rem;}' +
      '.dogo-swal-actions{gap:.5rem;margin-top:1.5rem;width:100%;}' +
      '.dogo-swal-confirm,.dogo-swal-cancel{border-radius:9999px;padding:.65rem 1.4rem;font-size:.875rem;' +
      'font-weight:700;margin:0;box-shadow:none !important;transition:background-color .15s,border-color .15s,transform .1s;}' +
      '.dogo-swal-confirm:active,.dogo-swal-cancel:active{transform:scale(.97);}' +
      '.dogo-swal-confirm:focus,.dogo-swal-cancel:focus{outline:none;box-shadow:none !important;}' +
      '.dogo-swal-cancel{background:#fff !important;color:#475569 !important;border:1px solid #e2e8f0 !important;}' +
      '.dogo-swal-cancel:hover{background:#f8fafc !important;}' +
      // 아이콘은 작고 은은하게
      '.dogo-swal-popup .swal2-icon{transform:scale(.72);margin:.25rem auto .5rem;border-width:3px;}' +
      '.dogo-swal-popup .swal2-icon.swal2-warning{border-color:#fde68a;color:#f59e0b;}' +
      '.dogo-swal-popup .swal2-icon.swal2-question{border-color:#e2e8f0;color:#94a3b8;}' +
      '.dogo-swal-popup .swal2-icon.swal2-success{border-color:#a7f3d0;color:#10b981;}' +
      '.dogo-swal-popup .swal2-icon.swal2-error{border-color:#fecaca;color:#ef4444;}' +
      // 토스트
      '.dogo-swal-toast{border-radius:.85rem;border:1px solid #e2e8f0;box-shadow:0 10px 30px -10px rgba(15,23,42,.25);}' +
      '.dogo-swal-toast .dogo-swal-title{font-size:.875rem;font-weight:600;margin-top:0;color:#334155;}';
    var style = document.createElement('style');
    style.id = 'dogo-swal-style';
    style.textContent = css;
    document.head.appendChild(style);
  }
  injectStyles();

  var BASE_CLASSES = {
    popup: 'dogo-swal-popup',
    title: 'dogo-swal-title',
    htmlContainer: 'dogo-swal-text',
    actions: 'dogo-swal-actions',
    confirmButton: 'dogo-swal-confirm',
    cancelButton: 'dogo-swal-cancel'
  };

  // alert() -> SweetAlert2 모달 (블로킹 동작에 의존하지 않는 곳에서만 사용)
  window.alert = function (message) {
    if (swalReady()) {
      window.Swal.fire({
        text: message == null ? '' : String(message),
        confirmButtonText: '확인',
        confirmButtonColor: BRAND,
        buttonsStyling: true,
        customClass: BASE_CLASSES
      });
    } else {
      nativeAlert(message);
    }
  };

  // 확인/취소 다이얼로그. Promise<boolean> 반환.
  window.confirmDialog = function (message, opts) {
    opts = opts || {};
    if (!swalReady()) {
      return Promise.resolve(window.confirm(message));
    }
    return window.Swal.fire({
      title: opts.title || '확인',
      text: message == null ? '' : String(message),
      icon: opts.icon || 'warning',
      showCancelButton: true,
      confirmButtonText: opts.confirmText || '확인',
      cancelButtonText: opts.cancelText || '취소',
      confirmButtonColor: opts.confirmColor || DANGER,
      buttonsStyling: true,
      reverseButtons: true,
      focusCancel: true,
      customClass: BASE_CLASSES
    }).then(function (result) {
      return result.isConfirmed;
    });
  };

  // 토스트 (선택적으로 사용)
  window.showToast = function (message, type) {
    if (!swalReady()) {
      nativeAlert(message);
      return;
    }
    window.Swal.fire({
      toast: true,
      position: 'top-end',
      icon: type || 'success',
      title: message == null ? '' : String(message),
      showConfirmButton: false,
      timer: 2500,
      timerProgressBar: true,
      customClass: { popup: 'dogo-swal-toast', title: 'dogo-swal-title' }
    });
  };

  // data-confirm 속성이 있는 폼 자동 처리
  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form || !form.matches || !form.matches('form[data-confirm]')) return;
    if (form.dataset.confirmed === 'true') return; // 이미 확인됨 -> 통과

    e.preventDefault();
    var message = form.getAttribute('data-confirm');
    var icon = form.getAttribute('data-confirm-icon') || 'warning';

    window.confirmDialog(message, { icon: icon }).then(function (ok) {
      if (ok) {
        form.dataset.confirmed = 'true';
        if (typeof form.requestSubmit === 'function') {
          form.requestSubmit();
        } else {
          form.submit();
        }
      }
    });
  }, true);
})();
