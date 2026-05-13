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

function loadSubRegions() {
  const sel = document.getElementById('areaSelect');
  const subSel = document.getElementById('subAreaSelect');
  const neighborSel = document.getElementById('neighborhoodSelect');
  
  if (!sel || !subSel) return;
  
  const region = sel.value;
  if (!region) {
    subSel.disabled = true;
    neighborSel.disabled = true;
    subSel.innerHTML = '<option value="">시/군/구</option>';
    neighborSel.innerHTML = '<option value="">읍/면/동</option>';
    return;
  }
  
  fetch(`/api/police/sub-regions?region=${encodeURIComponent(region)}`)
    .then(res => {
      if (!res.ok) throw new Error('Network response was not ok');
      return res.json();
    })
    .then(data => {
      subSel.innerHTML = '<option value="">시/군/구</option>';
      data.forEach(sub => {
        subSel.innerHTML += `<option value="${sub}">${sub}</option>`;
      });
      subSel.disabled = false;
      neighborSel.disabled = true;
      neighborSel.innerHTML = '<option value="">읍/면/동</option>';
      updateMap();
    })
    .catch(err => {
      console.error('Failed to load sub-regions:', err);
      subSel.disabled = false; // Enable anyway to prevent stuck UI
    });
}

function loadNeighborhoods() {
  const sel = document.getElementById('areaSelect');
  const subSel = document.getElementById('subAreaSelect');
  const neighborSel = document.getElementById('neighborhoodSelect');
  
  const region = sel.value;
  const subRegion = subSel.value;
  
  if (!subRegion) {
    neighborSel.disabled = true;
    neighborSel.innerHTML = '<option value="">읍/면/동</option>';
    updateMap();
    return;
  }
  
  fetch(`/api/police/neighborhoods?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion)}`)
    .then(res => {
      if (!res.ok) throw new Error('Network response was not ok');
      return res.json();
    })
    .then(data => {
      neighborSel.innerHTML = '<option value="">읍/면/동</option>';
      if (data.length > 0) {
        data.forEach(n => {
          neighborSel.innerHTML += `<option value="${n}">${n}</option>`;
        });
        neighborSel.disabled = false;
      } else {
        neighborSel.disabled = true;
      }
      updateMap();
    })
    .catch(err => {
      console.error('Failed to load neighborhoods:', err);
      neighborSel.disabled = false;
    });
}

function updateMap() {
  const sel = document.getElementById('areaSelect');
  const subSel = document.getElementById('subAreaSelect');
  const neighborSel = document.getElementById('neighborhoodSelect');
  const keywordInput = document.getElementById('centerSearch');

  if (!map || !sel) return;

  const region = sel.value;
  const subRegion = subSel.value;
  const neighborhood = neighborSel.value;
  const keyword = keywordInput ? keywordInput.value.trim() : '';

  // 기존 마커 및 리스트 초기화
  if (markers) {
    markers.forEach(marker => marker.setMap(null));
  }
  markers = [];
  const listContainer = document.getElementById('centerList');
  if (listContainer) {
    listContainer.innerHTML = '<div class="p-4 text-center text-gray-500">검색 중...</div>';
  }

  if (!region) return;

  // 경찰관서 데이터 불러오기
  fetch(`/api/police?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion)}&neighborhood=${encodeURIComponent(neighborhood)}`)
      .then(response => response.json())
      .then(data => {
        renderCenterList(data, keyword);
        
        if (data.length > 0) {
            const bounds = new kakao.maps.LatLngBounds();
            data.forEach(item => {
                const marker = new kakao.maps.Marker({
                    position: new kakao.maps.LatLng(item.latitude, item.longitude),
                    map: map
                });
                markers.push(marker);
                bounds.extend(marker.getPosition());
            });
            map.setBounds(bounds);
        } else {
            // 결과가 없으면 해당 광역 지역의 중심좌표로 이동
            fetch(`/api/areas/coords?name=${encodeURIComponent(region)}`)
                .then(res => res.json())
                .then(coord => {
                    if (coord) {
                        map.setCenter(new kakao.maps.LatLng(coord.latitude, coord.longitude));
                        map.setLevel(coord.defaultLevel || 8);
                    }
                });
        }
      })
      .catch(err => {
        console.error('Search failed:', err);
        if (listContainer) {
          listContainer.innerHTML = '<div class="p-4 text-center text-red-500">데이터를 불러오지 못했습니다.</div>';
        }
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
        <h4>${station.polstnNm} ${station.se}</h4>
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
          <b>${station.polstnNm} ${station.se}</b><br>
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