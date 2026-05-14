let map;
let markers = [];
let currentInfowindow = null;

function drawMap() {
  const mapContainer = document.getElementById('map');
  if (!mapContainer || typeof kakao === 'undefined' || !kakao.maps) {
    return;
  }

  const mapOption = {
    center: new kakao.maps.LatLng(36.5, 127.5),
    level: 13
  };

  map = new kakao.maps.Map(mapContainer, mapOption);
}

function loadMap() {
  if (typeof kakao === 'undefined' || !kakao.maps) {
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
  
  // Clear and disable immediately to provide visual feedback
  subSel.innerHTML = '<option value="">시/군/구</option>';
  subSel.disabled = true;
  neighborSel.innerHTML = '<option value="">읍/면/동</option>';
  neighborSel.disabled = true;

  if (!region) {
    updateMap();
    return;
  }

  fetch(`/api/police/sub-regions?region=${encodeURIComponent(region)}`)
      .then(res => {
        if (!res.ok) throw new Error('API request failed');
        return res.json();
      })
      .then(data => {
        const list = Array.isArray(data) ? data : (data.value || []);
        subSel.innerHTML = '<option value="">시/군/구</option>';
        if (list.length > 0) {
          list.forEach(sub => {
            subSel.innerHTML += `<option value="${sub}">${sub}</option>`;
          });
          subSel.disabled = false;
        } else {
          subSel.disabled = true;
        }
        updateMap();
      })
      .catch(err => {
        console.error('Error loading sub-regions:', err);
        subSel.disabled = true;
        updateMap();
      });
}

function loadNeighborhoods() {
  const sel = document.getElementById('areaSelect');
  const subSel = document.getElementById('subAreaSelect');
  const neighborSel = document.getElementById('neighborhoodSelect');
  if (!subSel || !neighborSel) return;

  const region = sel.value;
  const subRegion = subSel.value;
  
  // Clear and disable immediately
  neighborSel.innerHTML = '<option value="">읍/면/동</option>';
  neighborSel.disabled = true;

  if (!subRegion) {
    updateMap();
    return;
  }

  fetch(`/api/police/neighborhoods?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion)}`)
      .then(res => {
        if (!res.ok) throw new Error('API request failed');
        return res.json();
      })
      .then(data => {
        const list = Array.isArray(data) ? data : (data.value || []);
        neighborSel.innerHTML = '<option value="">읍/면/동</option>';
        if (list.length > 0) {
          list.forEach(n => {
            neighborSel.innerHTML += `<option value="${n}">${n}</option>`;
          });
          neighborSel.disabled = false;
        } else {
          neighborSel.disabled = true;
        }
        updateMap();
      })
      .catch(err => {
        console.error('Error loading neighborhoods:', err);
        neighborSel.disabled = true;
        updateMap();
      });
}

const fetchJson = (url) => fetch(url).then(res => {
  if (!res.ok) return [];
  const contentType = res.headers.get("content-type");
  if (!contentType || !contentType.includes("application/json")) return [];
  return res.json();
}).catch(() => []);

function getSelectedMapLevel(subRegion, neighborhood) {
  if (neighborhood) return 4;
  if (subRegion) return 6;
  return 8;
}

function focusSelectedArea(region, subRegion, neighborhood, searchId) {
  if (!region) return;

  fetch(`/api/areas/coords?name=${encodeURIComponent(region)}`)
      .then(res => res.ok ? res.json() : null)
      .then(coord => {
        if (searchId !== currentSearchId || !coord) return;
        map.setCenter(new kakao.maps.LatLng(coord.latitude, coord.longitude));
        map.setLevel(subRegion || neighborhood ? getSelectedMapLevel(subRegion, neighborhood) : (coord.defaultLevel || 8));
      })
      .catch(() => {});
}

function pointFromItem(item) {
  const lat = parseFloat(item.latitude);
  const lng = parseFloat(item.longitude);
  if (!lat || !lng || lat === 0 || lng === 0) return null;
  return { lat, lng };
}

function selectedPoliceBounds(policeData, subRegion, neighborhood) {
  if (!subRegion && !neighborhood) return null;
  if (!Array.isArray(policeData) || policeData.length === 0) return null;

  const points = policeData.map(pointFromItem).filter(Boolean);
  if (points.length === 0) return null;

  const lats = points.map(p => p.lat);
  const lngs = points.map(p => p.lng);
  const padding = neighborhood ? 0.015 : 0.05;

  return {
    minLat: Math.min(...lats) - padding,
    maxLat: Math.max(...lats) + padding,
    minLng: Math.min(...lngs) - padding,
    maxLng: Math.max(...lngs) + padding
  };
}

function isInsideBounds(item, bounds) {
  const point = pointFromItem(item);
  if (!point || !bounds) return false;
  return point.lat >= bounds.minLat
      && point.lat <= bounds.maxLat
      && point.lng >= bounds.minLng
      && point.lng <= bounds.maxLng;
}

function textMatchesSelection(item, subRegion, neighborhood) {
  const target = [
    item.subRegion,
    item.locationDetails,
    item.detailLocation,
    item.stationName
  ].filter(Boolean).join(' ');

  if (neighborhood && target.includes(neighborhood)) return true;
  if (subRegion && target.includes(subRegion)) return true;

  const shortSubRegion = subRegion && subRegion.length >= 2 ? subRegion.substring(0, 2) : subRegion;
  const shortNeighborhood = neighborhood && neighborhood.length >= 2 ? neighborhood.substring(0, 2) : neighborhood;

  return (!!shortNeighborhood && target.includes(shortNeighborhood))
      || (!!shortSubRegion && target.includes(shortSubRegion));
}

function filterRailBySelection(data, policeData, subRegion, neighborhood) {
  if (!subRegion && !neighborhood) return data;
  if (!Array.isArray(data)) return [];

  const bounds = selectedPoliceBounds(policeData, subRegion, neighborhood);
  return data.filter(item => textMatchesSelection(item, subRegion, neighborhood) || isInsideBounds(item, bounds));
}

let currentSearchId = 0;

function updateMap(keyword = '') {
  const sel = document.getElementById('areaSelect');
  const subSel = document.getElementById('subAreaSelect');
  const neighborSel = document.getElementById('neighborhoodSelect');
  const listContainer = document.getElementById('centerList');

  if (!map || !sel) return;

  const region = sel.value;
  const subRegion = subSel.value;
  const neighborhood = neighborSel.value;
  const searchId = ++currentSearchId;

  markers.forEach(m => m.setMap(null));
  markers = [];
  if (currentInfowindow) currentInfowindow.close();

  if (!region) {
    if (listContainer) {
      listContainer.innerHTML = '<div class="p-4 text-center text-gray-500">지역을 선택하시면 상세 목록이 표시됩니다.</div>';
    }
    return;
  }

  if (listContainer) {
    listContainer.innerHTML = '<div class="p-4 text-center text-gray-500"><div class="spinner-border spinner-border-sm me-2"></div>검색 중...</div>';
  }

  const policeUrl = `/api/police?region=${encodeURIComponent(region)}&subRegion=${encodeURIComponent(subRegion || '')}&neighborhood=${encodeURIComponent(neighborhood || '')}`;
  const korailUrl = `/api/korail/lost-found?region=${encodeURIComponent(region)}`;
  const subwayUrl = `/api/subway/list?region=${encodeURIComponent(region)}`;


  Promise.all([fetchJson(policeUrl), fetchJson(korailUrl), fetchJson(subwayUrl)])
      .then(([pDataRaw, kDataRaw, sDataRaw]) => {
        if (searchId !== currentSearchId) return;

        const pData = Array.isArray(pDataRaw) ? pDataRaw : (pDataRaw.value || []);
        const kData = filterRailBySelection(
            Array.isArray(kDataRaw) ? kDataRaw : (kDataRaw.value || []),
            pData,
            subRegion,
            neighborhood
        );
        const sData = filterRailBySelection(
            Array.isArray(sDataRaw) ? sDataRaw : (sDataRaw.value || []),
            pData,
            subRegion,
            neighborhood
        );

        const allData = [
          ...pData.map(d => ({ ...d, type: 'POLICE', name: `${d.polstnNm} ${d.se}`, address: d.addr, tel: d.telno })),
          ...kData.map(d => ({ ...d, type: 'KORAIL', name: `${d.stationName}역 유실물센터 (${d.lineName})`, address: d.locationDetails, tel: d.telNo })),
          ...sData.map(d => ({ ...d, type: 'SUBWAY', name: `${d.stationName}역 유실물센터`, address: d.detailLocation, tel: d.telNo }))
        ];

        renderCenterList(allData, keyword);
        renderMarkersOnMap(pData, kData, sData, { region, subRegion, neighborhood, searchId });
      })
      .catch(err => {
        console.error('Update map error:', err);
        if (searchId === currentSearchId && listContainer) {
          listContainer.innerHTML = '<div class="p-4 text-center text-red-500">데이터를 불러오는 중 오류가 발생했습니다.</div>';
        }
      });
}

function renderMarkersOnMap(policeData, korailData, subwayData, selection) {
  const bounds = new kakao.maps.LatLngBounds();
  let hasMarkers = false;
  let markerCount = 0;
  let firstPosition = null;

  const addMarkers = (data, type) => {
    if (!Array.isArray(data)) return;
    data.forEach(item => {
      // Use latitude/longitude if they exist and are not 0
      const lat = parseFloat(item.latitude);
      const lng = parseFloat(item.longitude);
      
      if (!lat || !lng || lat === 0 || lng === 0) return;
      
      const pos = new kakao.maps.LatLng(lat, lng);
      
      let markerImage = null;
      if (type === 'KORAIL') {
          markerImage = new kakao.maps.MarkerImage('https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png', new kakao.maps.Size(24, 35));
      } else if (type === 'SUBWAY') {
          markerImage = new kakao.maps.MarkerImage('https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png', new kakao.maps.Size(24, 35));
      }

      const marker = new kakao.maps.Marker({
        position: pos,
        map: map,
        image: markerImage
      });

      const name = type === 'POLICE' ? `${item.polstnNm || ''} ${item.se || ''}` : `${item.stationName || ''}역 유실물센터`;
      const addr = type === 'POLICE' ? (item.addr || item.address) : (type === 'KORAIL' ? item.locationDetails : item.detailLocation);
      const tel = item.telNo || item.telno || item.tel;

      const infowindow = new kakao.maps.InfoWindow({
        content: `<div style="padding:10px; min-width:200px;"><div style="font-weight:bold;">${name}</div><div style="font-size:12px; color:#666;">${addr || ''}</div><div style="font-size:12px; color:#007bff;">${tel || ''}</div></div>`
      });

      kakao.maps.event.addListener(marker, 'click', () => {
        if (currentInfowindow) currentInfowindow.close();
        infowindow.open(map, marker);
        currentInfowindow = infowindow;
      });

      markers.push(marker);
      bounds.extend(pos);
      firstPosition = firstPosition || pos;
      markerCount++;
      hasMarkers = true;
    });
  };

  addMarkers(policeData, 'POLICE');
  addMarkers(korailData, 'KORAIL');
  addMarkers(subwayData, 'SUBWAY');

  if (!hasMarkers) {
    focusSelectedArea(selection.region, selection.subRegion, selection.neighborhood, selection.searchId);
    return;
  }

  if (markerCount === 1) {
    map.setCenter(firstPosition);
    map.setLevel(getSelectedMapLevel(selection.subRegion, selection.neighborhood));
  } else {
    map.setBounds(bounds, 48, 48, 48, 48);
  }
}

function renderCenterList(data, keyword) {
  const centerList = document.getElementById('centerList');
  if (!centerList) return;
  centerList.innerHTML = '';

  const filtered = data.filter(item => keyword === '' || item.name.includes(keyword));

  if (filtered.length === 0) {
    centerList.innerHTML = '<p class="empty-msg">검색 결과가 없습니다.</p>';
    return;
  }

  filtered.forEach(item => {
    const typeClass = item.type === 'POLICE' ? 'bg-blue-100 text-blue-800' : item.type === 'KORAIL' ? 'bg-green-100 text-green-800' : 'bg-purple-100 text-purple-800';
    const typeLabel = item.type === 'POLICE' ? '경찰' : (item.type === 'KORAIL' ? '코레일' : '지하철');

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

document.addEventListener('DOMContentLoaded', loadMap);
