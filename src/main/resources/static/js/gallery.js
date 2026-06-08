/*
 * 이미지 갤러리 (Swiper 기반 공통 초기화)
 * 마크업 규약:
 *   <div class="dogo-gallery" data-fancybox-group="lost-gallery"> ... </div>
 *     .gallery-main   : 메인 Swiper (.swiper)
 *     .gallery-thumbs : 썸네일 Swiper (.swiper, 선택)
 *     .gallery-prev / .gallery-next : 네비게이션 버튼
 *     .gallery-current : 현재 인덱스 표시 span
 *     .gallery-blur    : 배경 블러 엘리먼트
 *   슬라이드(a.swiper-slide)에 data-fancybox="<group>" 부여 시 라이트박스 연동
 */
(function () {
  function injectStyles() {
    if (document.getElementById('dogo-gallery-style')) return;
    var css =
      '.dogo-gallery{width:100%;min-width:0;}' +
      '.dogo-gallery .swiper{width:100%;min-width:0;}' +
      '.dogo-gallery .gallery-main{overflow:hidden;border-radius:.75rem;}' +
      '.dogo-gallery .gallery-thumbs{margin-top:0;}' +
      '.dogo-gallery .gallery-thumbs .swiper-slide{aspect-ratio:1/1;border:2px solid transparent;' +
      'border-radius:.75rem;overflow:hidden;cursor:pointer;opacity:.55;' +
      'transition:opacity .2s,border-color .2s;}' +
      '.dogo-gallery .gallery-thumbs .swiper-slide:hover{opacity:.85;}' +
      '.dogo-gallery .gallery-thumbs .swiper-slide-thumb-active{border-color:#0f172a;opacity:1;}' +
      '.dogo-gallery .gallery-thumbs .swiper-slide img{width:100%;height:100%;object-fit:cover;}' +
      '.dogo-gallery .gallery-prev.swiper-button-disabled,' +
      '.dogo-gallery .gallery-next.swiper-button-disabled{opacity:0;pointer-events:none;}';
    var style = document.createElement('style');
    style.id = 'dogo-gallery-style';
    style.textContent = css;
    document.head.appendChild(style);
  }

  function initGallery(root) {
    if (typeof window.Swiper === 'undefined') return;
    var mainEl = root.querySelector('.gallery-main');
    if (!mainEl) return;

    var thumbsEl = root.querySelector('.gallery-thumbs');
    var blur = root.querySelector('.gallery-blur');
    var current = root.querySelector('.gallery-current');

    var thumbsSwiper = null;
    if (thumbsEl) {
      thumbsSwiper = new window.Swiper(thumbsEl, {
        slidesPerView: 5,
        spaceBetween: 12,
        watchSlidesProgress: true
      });
    }

    new window.Swiper(mainEl, {
      slidesPerView: 1,
      navigation: {
        prevEl: root.querySelector('.gallery-prev'),
        nextEl: root.querySelector('.gallery-next')
      },
      thumbs: thumbsSwiper ? { swiper: thumbsSwiper } : undefined,
      on: {
        slideChange: function (sw) {
          var idx = sw.activeIndex;
          if (current) current.textContent = idx + 1;
          var slide = sw.slides[idx];
          var img = slide ? slide.querySelector('img') : null;
          if (blur && img) {
            blur.style.backgroundImage = "url('" + img.src.replace(/'/g, "\\'") + "')";
          }
        }
      }
    });

    var group = root.getAttribute('data-fancybox-group');
    if (group && window.Fancybox) {
      window.Fancybox.bind(root, '[data-fancybox="' + group + '"]', {
        Images: { zoom: true },
        Toolbar: {
          display: { left: ['infobar'], middle: [], right: ['zoom', 'close'] }
        }
      });
    }
  }

  function initAll() {
    injectStyles();
    var roots = document.querySelectorAll('.dogo-gallery');
    for (var i = 0; i < roots.length; i++) initGallery(roots[i]);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();
