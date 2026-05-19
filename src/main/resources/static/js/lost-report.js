// =========================
// 실시간 데이터 바인딩
// =========================
function syncText() {
  const canvasTitle = document.getElementById('canvas-title');
  if (canvasTitle) canvasTitle.innerText = document.getElementById('input-title').value;

  if (document.getElementById('view-name'))
    document.getElementById('view-name').innerText = document.getElementById('info-name').value;

  if (document.getElementById('view-breed'))
    document.getElementById('view-breed').innerText = document.getElementById('info-breed').value;

  if (document.getElementById('view-color'))
    document.getElementById('view-color').innerText = document.getElementById('info-color').value;

  if (document.getElementById('view-age'))
    document.getElementById('view-age').innerText = document.getElementById('info-age').value;

  if (document.getElementById('view-size'))
    document.getElementById('view-size').innerText = document.getElementById('info-size').value;

  if (document.getElementById('view-neutral'))
    document.getElementById('view-neutral').innerText = document.getElementById('info-neutral').value;

  if (document.getElementById('view-chip'))
    document.getElementById('view-chip').innerText = document.getElementById('info-chip').value;

  if (document.getElementById('view-location'))
    document.getElementById('view-location').innerText = document.getElementById('input-location').value;

  if (document.getElementById('view-features'))
    document.getElementById('view-features').innerText = document.getElementById('input-features').value;

  const viewReward = document.getElementById('view-reward');
  if (viewReward)
    viewReward.innerText = document.getElementById('input-reward').value;

  const viewContact = document.getElementById('view-contact');
  if (viewContact)
    viewContact.innerText = document.getElementById('input-contact').value;
}

// =========================
// 요소 선택
// =========================
function selectElement(event) {
  event.stopPropagation();
  hideContextMenu();

  if (selectedElement) selectedElement.classList.remove('selected');

  selectedElement = event.currentTarget;
  selectedElement.classList.add('selected');

  const handle = selectedElement.querySelector('.resize-handle');
  selectedElement.onmousedown = startDrag;

  if (handle) handle.onmousedown = startResize;
}

function deselectAll(event) {
  hideContextMenu();

  if (event.target.id === 'leaflet-canvas') {
    if (selectedElement) selectedElement.classList.remove('selected');
    selectedElement = null;
  }
}

// =========================
// 삭제
// =========================
function deleteSelectedElement() {
  if (selectedElement) {
    selectedElement.remove();
    selectedElement = null;
    hideContextMenu();
  } else {
    alert("지우고 싶은 요소를 먼저 선택해 주세요!");
  }
}

document.addEventListener('keydown', function (event) {
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (
        document.activeElement.tagName !== 'INPUT' &&
        document.activeElement.tagName !== 'TEXTAREA' &&
        !document.activeElement.isContentEditable
    ) {
      deleteSelectedElement();
    }
  }
});

// =========================
// 우클릭 메뉴
// =========================
document.addEventListener('contextmenu', function (e) {
  const element = e.target.closest('.element');

  if (element) {
    e.preventDefault();

    selectElement({
      stopPropagation: () => {
      },
      currentTarget: element
    });

    const menu = document.getElementById('custom-context-menu');
    menu.style.display = 'block';
    menu.style.left = `${e.pageX}px`;
    menu.style.top = `${e.pageY}px`;
  } else {
    hideContextMenu();
  }
});

document.addEventListener('click', function () {
  hideContextMenu();
});

function hideContextMenu() {
  const menu = document.getElementById('custom-context-menu');
  if (menu) menu.style.display = 'none';
}

// =========================
// 드래그 / 리사이즈
// =========================
function startDrag(e) {
  if (isResizing) return;

  isDragging = true;
  startX = e.clientX;
  startY = e.clientY;
  startLeft = parseInt(selectedElement.style.left) || 0;
  startTop = parseInt(selectedElement.style.top) || 0;

  document.onmousemove = doDrag;
  document.onmouseup = stopActions;
}

function doDrag(e) {
  if (!isDragging) return;

  selectedElement.style.left = `${startLeft + (e.clientX - startX)}px`;
  selectedElement.style.top = `${startTop + (e.clientY - startY)}px`;
}

function startResize(e) {
  e.stopPropagation();

  isResizing = true;
  startX = e.clientX;
  startY = e.clientY;
  startWidth = selectedElement.offsetWidth;
  startHeight = selectedElement.offsetHeight;

  document.onmousemove = doResize;
  document.onmouseup = stopActions;
}

function doResize(e) {
  if (!isResizing) return;

  selectedElement.style.width = `${startWidth + (e.clientX - startX)}px`;
  selectedElement.style.height = `${startHeight + (e.clientY - startY)}px`;
}

function stopActions() {
  isDragging = false;
  isResizing = false;
  document.onmousemove = null;
  document.onmouseup = null;
}

// =========================
// 요소 추가
// =========================
function addRewardElement() {
  if (document.getElementById('view-reward')) {
    alert("이미 사례금 항목이 있습니다.");
    return;
  }

  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');

  newElem.id = 'container-reward';
  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 250px; left: 80px; width: 300px; height: 50px; z-index:10;';
  newElem.onclick = selectElement;

  newElem.innerHTML = `
    <div class="element-content reward-only-box">
      <div class="reward-line">💰 사례금: <span id="view-reward"></span>원</div>
    </div>
    <div class="resize-handle"></div>
  `;

  canvas.appendChild(newElem);
  syncText();
  toggleGuideLines();
}

function addQrElement() {
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');

  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 220px; left: 350px; width: 90px; height: 90px; z-index:10;';
  newElem.onclick = selectElement;

  newElem.innerHTML = `
    <div class="element-content image-placeholder" style="font-size:11px;">🔲 QR코드</div>
    <div class="resize-handle"></div>
  `;

  canvas.appendChild(newElem);
  toggleGuideLines();
}

function addImageElement() {
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');

  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 150px; left: 150px; width: 140px; height: 140px; z-index:10;';
  newElem.onclick = selectElement;

  newElem.innerHTML = `
    <div class="element-content image-placeholder">📷 추가 사진</div>
    <div class="resize-handle"></div>
  `;

  canvas.appendChild(newElem);
  toggleGuideLines();
}

function addTextElement() {
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');

  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 200px; left: 120px; width: 200px; height: 60px; z-index:10;';
  newElem.onclick = selectElement;

  newElem.innerHTML = `
    <div class="element-content" contenteditable="true" style="padding:8px; font-size:13px; outline:none; color:#333;">
      더블클릭하여 내용을 입력하세요.
    </div>
    <div class="resize-handle"></div>
  `;

  canvas.appendChild(newElem);
  toggleGuideLines();
}

// =========================
// 가이드라인 표시
// =========================
function toggleGuideLines() {
  const isChecked = document.getElementById('toggle-guide').checked;
  const elements = document.querySelectorAll('.element');

  elements.forEach(el => {
    el.style.borderColor = isChecked ? "#cbd5e1" : "transparent";
  });
}