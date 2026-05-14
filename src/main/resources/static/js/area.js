let map;
let markers = [];
let currentInfowindow = null;

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
  if (currentInfowindow) {
    currentInfowindow.close();
    currentInfowindow = null;
  }
  const listContainer = document.getElementById('centerList');
  if (listContainer) {
    listContainer.innerHTML = '<div class="p-4 text-center text-gray-500">검색 중...</div>';
  }

  if (!region) return;

  // 경찰관서와 코레일 유실물 센터 데이터 함께 불러오기
  const policeUrl = `/api/police?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion)}&neighborhood=${encodeURIComponent(neighborhood)}`;
  const korailUrl = `/api/korail/lost-found?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion)}&neighborhood=${encodeURIComponent(neighborhood)}`;

  Promise.all([
    fetch(policeUrl).then(res => res.json()),
    fetch(korailUrl).then(res => res.json())
  ])
  .then(([policeData, korailData]) => {
    // 코레일 데이터 필터링 (선택된 지역에 해당하는 것만 표시하고 싶을 수 있으나, 일단 모두 표시하거나 역명으로 필터링 필요)
    // 여기서는 일단 모든 코레일 센터를 데이터셋에 포함시킵니다.
    const allData = [
      ...policeData.map(d => ({ ...d, type: 'POLICE', name: `${d.polstnNm} ${d.se}`, address: d.addr, tel: d.telno })),
      ...korailData.map(d => ({ ...d, type: 'KORAIL', name: `${d.stationName}역 유실물센터 (${d.lineName})`, address: d.locationDetails, tel: d.telNo }))
    ];

    renderCenterList(allData, keyword);
    
    if (allData.length > 0) {
        const bounds = new kakao.maps.LatLngBounds();
        allData.forEach(item => {
            const marker = new kakao.maps.Marker({
                position: new kakao.maps.LatLng(item.latitude, item.longitude),
                map: map
            });
            markers.push(marker);
            bounds.extend(marker.getPosition());
            
            // 인포윈도우 추가
            const infowindow = new kakao.maps.InfoWindow({
                content: `<div style="padding:10px; min-width:200px;">
                            <div style="font-weight:bold; margin-bottom:5px;">${item.name}</div>
                            <div style="font-size:12px; color:#666;">${item.address || ''}</div>
                            <div style="font-size:12px; color:#007bff;">${item.tel || ''}</div>
                          </div>`
            });
            
            kakao.maps.event.addListener(marker, 'click', () => {
                if (currentInfowindow) {
                    currentInfowindow.close();
                }
                infowindow.open(map, marker);
                currentInfowindow = infowindow;
            });
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

  const filtered = data.filter(item =>
      keyword === '' || item.name.includes(keyword)
  );

  if (filtered.length === 0) {
    centerList.innerHTML = '<p class="empty-msg">검색 결과가 없습니다.</p>';
    return;
  }

  filtered.forEach(item => {
    const typeLabel = item.type === 'POLICE' ? '경찰' : '코레일';
    const typeClass = item.type === 'POLICE' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800';
    
    centerList.innerHTML += `
      <div class="center-card p-3 border-bottom hover-bg-light cursor-pointer">
        <div class="d-flex justify-content-between align-items-start mb-1">
          <h5 class="mb-0 fs-6 fw-bold">${item.name}</h5>
          <span class="badge ${typeClass}" style="font-size: 10px;">${typeLabel}</span>
        </div>
        <p class="mb-1 text-muted small">${item.address || '주소 정보 없음'}</p>
        <p class="mb-0 text-primary small">${item.tel || '전화번호 없음'}</p>
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
      if (currentInfowindow) {
        currentInfowindow.close();
      }
      infowindow.open(map, marker);
      currentInfowindow = infowindow;
    });

    markers.push(marker);
  });
}

document.addEventListener('DOMContentLoaded', loadMap);