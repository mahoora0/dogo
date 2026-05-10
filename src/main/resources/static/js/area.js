let map; // 전역 변수로 선언하여 다른 함수에서도 접근 가능하게 합니다.
let currentMarker = null;
// area.js 수정본
function drawMap() {
  const mapContainer = document.getElementById('map');
  if (mapContainer) {
    const mapOption = {
      center: new kakao.maps.LatLng(35.1795, 129.0756),
      level: 4
    };
    map = new kakao.maps.Map(mapContainer, mapOption);
    console.log("지도 그리기 성공!");
  }
}

// 카카오 라이브러리가 완전히 준비된 후 실행되도록 보장
if (typeof kakao !== 'undefined' && kakao.maps) {
  kakao.maps.load(drawMap);
} else {
  // 혹시 모르니 페이지 전체 로드 후 한 번 더 시도
  window.onload = function() {
    if (typeof kakao !== 'undefined') kakao.maps.load(drawMap);
  };
}
// 카카오 지도 라이브러리가 완전히 로드된 후 실행되도록 보장합니다.
kakao.maps.load(function() {
  const mapContainer = document.getElementById('map');

  if (mapContainer) {
    const mapOption = {
      center: new kakao.maps.LatLng(35.1795, 129.0756),
      level: 4
    };

    // 여기서 지도를 생성하므로 'kakao is not defined' 에러가 사라집니다.
    map = new kakao.maps.Map(mapContainer, mapOption);
    console.log("지도 로드 완료!");
  }
});

// 검색 버튼 클릭 시 실행될 함수
function updateMap() {
  const sel = document.getElementById('areaSelect');
  const opt = sel.options[sel.selectedIndex];

  // 지도가 아직 생성되지 않았거나 선택된 값이 없으면 리턴
  if (!map || !opt.value) return;

  const lat = opt.getAttribute('data-lat');
  const lng = opt.getAttribute('data-lng');
  const moveLatLon = new kakao.maps.LatLng(lat, lng);

  // 지도 중심 부드럽게 이동
  map.panTo(moveLatLon);

  // 기존 마커 제거 후 새 마커 표시
  if (currentMarker) currentMarker.setMap(null);
  currentMarker = new kakao.maps.Marker({
    position: moveLatLon,
    map: map
  });
}