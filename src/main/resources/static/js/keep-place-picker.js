/**
 * keep-place-picker.js
 * 보관 장소 입력 필드에서 장소를 검색했을 때
 * 동일한 이름의 장소가 여러 개이면 선택 UI를 보여줍니다.
 */
(function () {
  'use strict';

  var DEBOUNCE_MS = 500;       // 입력 후 검색 대기 시간
  var MIN_QUERY_LEN = 2;       // 최소 검색 글자 수

  var searchTimer = null;
  var currentDropdown = null;
  var previewMap = null;
  var previewMarker = null;

  /* ────────────────────────────────────────────────
     초기화: id="keepPlace" 인풋이 있는 경우에만 동작
  ──────────────────────────────────────────────── */
  function initKeepPlacePicker() {
    var input = document.getElementById('keepPlace');
    if (!input) return;

    // 드롭다운 컨테이너 생성
    var wrapper = document.createElement('div');
    wrapper.style.position = 'relative';
    input.parentNode.insertBefore(wrapper, input);
    wrapper.appendChild(input);

    var dropdown = document.createElement('div');
    dropdown.id = 'keepPlaceDropdown';
    dropdown.className = 'keep-place-dropdown';
    dropdown.style.cssText = [
      'position:absolute', 'left:0', 'right:0', 'top:calc(100% + 4px)',
      'z-index:200', 'background:#fff',
      'border:1px solid #e2e8f0', 'border-radius:12px',
      'box-shadow:0 8px 24px rgba(0,0,0,0.10)',
      'overflow:hidden', 'display:none'
    ].join(';');
    wrapper.appendChild(dropdown);
    currentDropdown = dropdown;

    // 지도 미리보기 패널 (드롭다운 아래에 붙음)
    var mapPreview = document.createElement('div');
    mapPreview.id = 'keepPlaceMapPreview';
    mapPreview.style.cssText = [
      'width:100%', 'height:220px', 'border-radius:0 0 12px 12px',
      'border-top:1px solid #e2e8f0', 'display:none'
    ].join(';');
    dropdown.appendChild(mapPreview);

    // 입력 이벤트
    input.addEventListener('input', function () {
      clearTimeout(searchTimer);
      var query = input.value.trim();
      if (query.length < MIN_QUERY_LEN) {
        hideDropdown();
        return;
      }
      searchTimer = setTimeout(function () {
        searchPlace(query, input, dropdown, mapPreview);
      }, DEBOUNCE_MS);
    });

    // 바깥 클릭 시 드롭다운 닫기
    document.addEventListener('click', function (e) {
      if (!wrapper.contains(e.target)) {
        hideDropdown();
      }
    });

    // 인풋 재클릭/포커스 시 드롭다운 다시 노출
    function showDropdownIfNeeded() {
      if (dropdown.style.display === 'block') return;
      var query = input.value.trim();
      if (query.length >= MIN_QUERY_LEN) {
        searchPlace(query, input, dropdown, mapPreview);
      }
    }
    input.addEventListener('click', showDropdownIfNeeded);
    input.addEventListener('focus', showDropdownIfNeeded);

    // Escape 키로 닫기
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') hideDropdown();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initKeepPlacePicker);
  } else {
    initKeepPlacePicker();
  }

  /* ────────────────────────────────────────────────
     장소 검색 (카카오 장소 검색 API 프록시)
  ──────────────────────────────────────────────── */
  function searchPlace(query, input, dropdown, mapPreview) {
    fetch('/api/place/search?query=' + encodeURIComponent(query))
      .then(function (res) {
        if (!res.ok) throw new Error('검색 실패');
        return res.json();
      })
      .then(function (data) {
        var docs = (data.documents || []);
        if (docs.length === 0) {
          hideDropdown();
          return;
        }
        if (docs.length === 1) {
          // 결과가 1개면 드롭다운 없이 바로 확정
          input.value = docs[0].place_name;
          hideDropdown();
          return;
        }
        // 결과가 여러 개 → 선택 목록 표시
        renderDropdown(docs, input, dropdown, mapPreview);
      })
      .catch(function () {
        hideDropdown();
      });
  }

  /* ────────────────────────────────────────────────
     드롭다운 렌더링
  ──────────────────────────────────────────────── */
  function renderDropdown(docs, input, dropdown, mapPreview) {
    // 기존 목록 지우기 (지도 제외)
    while (dropdown.firstChild && dropdown.firstChild !== mapPreview) {
      dropdown.removeChild(dropdown.firstChild);
    }

    var header = document.createElement('div');
    header.style.cssText = 'padding:8px 14px 6px;font-size:11px;font-weight:600;color:#94a3b8;letter-spacing:.04em;border-bottom:1px solid #f1f5f9';
    header.textContent = '동일 이름 장소 ' + docs.length + '건 — 정확한 장소를 선택하세요';
    dropdown.insertBefore(header, mapPreview);

    var list = document.createElement('div');
    list.style.cssText = 'max-height:220px;overflow-y:auto';
    dropdown.insertBefore(list, mapPreview);

    docs.forEach(function (place, idx) {
      var item = document.createElement('button');
      item.type = 'button';
      item.style.cssText = [
        'width:100%', 'display:flex', 'align-items:flex-start', 'gap:10px',
        'padding:10px 14px', 'text-align:left', 'border:none', 'background:none',
        'cursor:pointer', 'transition:background .15s',
        idx === 0 ? 'background:#eff6ff' : ''
      ].join(';');

      var addr = place.road_address_name || place.address_name || '';
      var cat  = place.category_name ? place.category_name.split(' > ').pop() : '';

      item.innerHTML =
        '<svg xmlns="http://www.w3.org/2000/svg" style="width:16px;height:16px;color:#3b82f6;flex-shrink:0;margin-top:2px" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>' +
        '<div style="flex:1;min-width:0">' +
          '<div style="font-weight:600;font-size:13px;color:#1e293b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escHtml(place.place_name) + '</div>' +
          (addr ? '<div style="font-size:11px;color:#94a3b8;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + escHtml(addr) + '</div>' : '') +
          (cat  ? '<span style="display:inline-block;margin-top:3px;font-size:10px;font-weight:500;color:#64748b;background:#f1f5f9;border-radius:999px;padding:1px 8px">' + escHtml(cat) + '</span>' : '') +
        '</div>';

      // 호버 시 지도 미리보기
      item.addEventListener('mouseenter', function () {
        showMapPreview(parseFloat(place.y), parseFloat(place.x), mapPreview);
      });

      // 클릭 시 선택 확정
      item.addEventListener('click', function () {
        input.value = place.place_name;
        // 선택된 항목 하이라이트
        list.querySelectorAll('button').forEach(function (b) {
          b.style.background = '';
        });
        item.style.background = '#eff6ff';
        hideDropdown();
      });

      list.appendChild(item);
    });

    // 지도 미리보기 표시
    mapPreview.style.display = 'block';
    dropdown.style.display = 'block';

    // 첫 번째 장소를 기본으로 미리보기
    showMapPreview(parseFloat(docs[0].y), parseFloat(docs[0].x), mapPreview);
  }

  /* ────────────────────────────────────────────────
     드롭다운 내 지도 미리보기
  ──────────────────────────────────────────────── */
  function showMapPreview(lat, lng, mapPreview) {
    if (typeof kakao === 'undefined' || !kakao.maps) return;
    kakao.maps.load(function () {
      var position = new kakao.maps.LatLng(lat, lng);
      if (!previewMap) {
        previewMap = new kakao.maps.Map(mapPreview, {
          center: position,
          level: 3
        });
      } else {
        previewMap.setCenter(position);
      }
      if (previewMarker) previewMarker.setMap(null);
      previewMarker = new kakao.maps.Marker({ position: position });
      previewMarker.setMap(previewMap);
      setTimeout(function () {
        previewMap.relayout();
        previewMap.setCenter(position);
      }, 100);
    });
  }

  /* ────────────────────────────────────────────────
     드롭다운 숨기기
  ──────────────────────────────────────────────── */
  function hideDropdown() {
    if (currentDropdown) {
      currentDropdown.style.display = 'none';
    }
    previewMap = null;
    previewMarker = null;
  }

  function escHtml(str) {
    if (!str) return '';
    var d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
  }
})();
