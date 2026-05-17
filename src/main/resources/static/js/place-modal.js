let modalMap = null;
let modalMarker = null;

window.openPlaceModal = async function(placeName) {
  const modal = document.getElementById("placeModal");
  const title = document.getElementById("modalTitle");
  const mapContainer = document.getElementById("modalMap");

  if (!modal || !title || !mapContainer) {
    console.error("모달 요소를 찾을 수 없습니다.");
    return;
  }

  modal.classList.remove("hidden");
  title.innerText = placeName + " 위치";

  try {
    const response = await fetch(`/api/place/search?query=${encodeURIComponent(placeName)}`);

    if (!response.ok) {
      throw new Error("장소 검색 실패");
    }

    const data = await response.json();

    if (!data.documents || data.documents.length === 0) {
      alert("위치를 찾을 수 없습니다.");
      return;
    }

    const place = data.documents[0];
    const lat = parseFloat(place.y);
    const lng = parseFloat(place.x);

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

      modalMarker = new kakao.maps.Marker({
        position: position
      });

      modalMarker.setMap(modalMap);

      setTimeout(() => {
        modalMap.relayout();
        modalMap.setCenter(position);
      }, 300);
    });

  } catch (error) {
    console.error(error);
    alert("지도 불러오기 실패");
  }
};

window.closePlaceModal = function() {
  const modal = document.getElementById("placeModal");
  modal.classList.add("hidden");
};