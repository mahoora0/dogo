let map;
let markers = [];

function drawMap() {
  const mapContainer = document.getElementById('map');
  if (!mapContainer || typeof kakao === 'undefined' || !kakao.maps) {
    return;
  }

  const mapOption = {
    center: new kakao.maps.LatLng(35.1795, 129.0756),
    level: 4
  };

  map = new kakao.maps.Map(mapContainer, mapOption);
}

function loadMap() {
  if (typeof kakao === 'undefined' || !kakao.maps) {
    console.error('Kakao Maps SDK is not loaded.');
    return;
  }

  kakao.maps.load(drawMap);
}

function updateMap() {
  const sel = document.getElementById('areaSelect');
  const keyword = document.getElementById('centerSearch').value.trim();

  if (!map || !sel) return;

  const opt = sel.options[sel.selectedIndex];
  if (!opt || !opt.value) return;

  const region = opt.text;
  const lat = Number(opt.getAttribute('data-lat'));
  const lng = Number(opt.getAttribute('data-lng'));
  const level = Number(opt.getAttribute('data-level'));

  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

  const moveLatLon = new kakao.maps.LatLng(lat, lng);

  if (Number.isFinite(level)) {
    map.setLevel(level);
  }

  map.panTo(moveLatLon);

  // 기존 마커 삭제
  markers.forEach(marker => marker.setMap(null));
  markers = [];

  // 경찰관서 데이터 불러오기
  fetch(`/api/police?region=${region}`)
      .then(response => response.json())
      .then(data => {
        renderCenterList(data, keyword);
        renderMarkers(data, keyword);
      });
}

function renderCenterList(data, keyword) {
  const centerList = document.getElementById('centerList');
  centerList.innerHTML = '';

  const filtered = data.filter(station =>
      keyword === '' || station.polstnNm.includes(keyword)
  );

  if (filtered.length === 0) {
    centerList.innerHTML = '<p class="empty-msg">검색 결과가 없습니다.</p>';
    return;
  }

  filtered.forEach(station => {
    centerList.innerHTML += `
      <div class="center-card">
        <h4>${station.polstnNm}</h4>
        <p>${station.addr}</p>
        <p>${station.telno}</p>
      </div>
    `;
  });
}

function renderMarkers(data, keyword) {
  const filtered = data.filter(station =>
      keyword === '' || station.polstnNm.includes(keyword)
  );

  filtered.forEach(station => {
    const marker = new kakao.maps.Marker({
      map: map,
      position: new kakao.maps.LatLng(station.latitude, station.longitude)
    });

    const infowindow = new kakao.maps.InfoWindow({
      content: `
        <div style="padding:8px; font-size:13px;">
          <b>${station.polstnNm}</b><br>
          ${station.telno}
        </div>
      `
    });

    kakao.maps.event.addListener(marker, 'click', function () {
      infowindow.open(map, marker);
    });

    markers.push(marker);
  });
}

document.addEventListener('DOMContentLoaded', loadMap);