let modalMap = null;
let modalMarker = null;

/**
 * 지도 위에 특정 위치를 표시합니다.
 */
function showPlaceOnMap(lat, lng) {
  const mapContainer = document.getElementById("modalMap");
  kakao.maps.load(function () {
    const position = new kakao.maps.LatLng(lat, lng);

    if (!modalMap) {
      modalMap = new kakao.maps.Map(mapContainer, {
        center: position,
        level: 3
      });
    } else {
      modalMap.setCenter(position);
    }

    if (modalMarker) {
      modalMarker.setMap(null);
    }

    modalMarker = new kakao.maps.Marker({ position });
    modalMarker.setMap(modalMap);

    setTimeout(() => {
      modalMap.relayout();
      modalMap.setCenter(position);
    }, 300);
  });
}

/**
 * 장소 선택 목록을 렌더링합니다. (동일한 이름의 장소가 여러 개일 때)
 */
function renderPlaceSelectList(documents) {
  const panel = document.getElementById("placeSelectPanel");
  const list = document.getElementById("placeSelectList");
  if (!panel || !list) return;

  list.innerHTML = "";

  documents.forEach(function (place, index) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className =
      "w-full flex items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-slate-50 focus:outline-none focus:bg-slate-100" +
      (index === 0 ? " bg-blue-50" : "");

    const addressText = place.road_address_name || place.address_name || "";
    const categoryText = place.category_name ? place.category_name.split(" > ").pop() : "";

    btn.innerHTML =
      '<div class="flex-1 min-w-0">' +
        '<p class="font-semibold text-slate-800 truncate">' + escHtml(place.place_name) + "</p>" +
        (addressText
          ? '<p class="text-xs text-slate-500 mt-0.5 truncate">' + escHtml(addressText) + "</p>"
          : "") +
        (categoryText
          ? '<span class="inline-block mt-1 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">' + escHtml(categoryText) + "</span>"
          : "") +
      "</div>" +
      '<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mt-1 shrink-0 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M17.657 16.657L13.414 20.9a2 2 0 01-2.828 0l-4.243-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>';

    btn.addEventListener("click", function () {
      // 선택된 항목 하이라이트
      list.querySelectorAll("button").forEach(function (b) {
        b.classList.remove("bg-blue-50");
      });
      btn.classList.add("bg-blue-50");

      showPlaceOnMap(parseFloat(place.y), parseFloat(place.x));
    });

    list.appendChild(btn);
  });

  panel.classList.remove("hidden");

  // 첫 번째 장소를 기본으로 지도에 표시
  showPlaceOnMap(parseFloat(documents[0].y), parseFloat(documents[0].x));
}

function escHtml(str) {
  if (!str) return "";
  const d = document.createElement("div");
  d.textContent = str;
  return d.innerHTML;
}

window.openPlaceModal = async function (placeName) {
  const modal = document.getElementById("placeModal");
  const title = document.getElementById("modalTitle");
  const panel = document.getElementById("placeSelectPanel");
  const mapContainer = document.getElementById("modalMap");

  if (!modal || !title || !mapContainer) {
    console.error("모달 요소를 찾을 수 없습니다.");
    return;
  }

  // 모달 초기화
  modal.classList.remove("hidden");
  title.innerText = placeName + " 위치";
  if (panel) panel.classList.add("hidden");

  try {
    const response = await fetch(
      `/api/place/search?query=${encodeURIComponent(placeName)}`
    );

    if (!response.ok) {
      throw new Error("장소 검색 실패");
    }

    const data = await response.json();

    if (!data.documents || data.documents.length === 0) {
      alert("위치를 찾을 수 없습니다.");
      modal.classList.add("hidden");
      return;
    }

    const place = data.documents[0];
    showPlaceOnMap(parseFloat(place.y), parseFloat(place.x));
  } catch (error) {
    console.error(error);
    alert("지도 불러오기 실패");
  }
};

window.closePlaceModal = function () {
  const modal = document.getElementById("placeModal");
  const panel = document.getElementById("placeSelectPanel");
  modal.classList.add("hidden");
  if (panel) panel.classList.add("hidden");
};