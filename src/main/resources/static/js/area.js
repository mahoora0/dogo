let map;
let currentMarker = null;

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
  if (!map || !sel) {
    return;
  }

  const opt = sel.options[sel.selectedIndex];
  if (!opt || !opt.value) {
    return;
  }

  const lat = Number(opt.getAttribute('data-lat'));
  const lng = Number(opt.getAttribute('data-lng'));
  const level = Number(opt.getAttribute('data-level'));
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return;
  }

  const moveLatLon = new kakao.maps.LatLng(lat, lng);
  if (Number.isFinite(level)) {
    map.setLevel(level);
  }
  map.panTo(moveLatLon);

  if (currentMarker) {
    currentMarker.setMap(null);
  }

  currentMarker = new kakao.maps.Marker({
    position: moveLatLon,
    map
  });
}

document.addEventListener('DOMContentLoaded', loadMap);
