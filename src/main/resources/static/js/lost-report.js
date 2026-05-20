// =========================
// 전역 변수 선언
// =========================
let selectedElement = null;
let isDragging = false;
let isResizing = false;
let startX = 0, startY = 0;
let startLeft = 0, startTop = 0;
let startWidth = 0, startHeight = 0;

// =========================
// 실시간 데이터 바인딩
// =========================
function syncText() {
  const canvasTitle = document.getElementById('canvas-title');
  if (canvasTitle) canvasTitle.innerText = document.getElementById('input-title').value;

  const viewProfileBar = document.getElementById('view-profile-bar');
  if (viewProfileBar) {
    const name = document.getElementById('info-name').value.trim();
    const gender = document.getElementById('info-gender').value.trim();
    const age = document.getElementById('info-age').value.trim();
    const color = document.getElementById('info-color').value.trim();
    const breed = document.getElementById('info-breed').value.trim();
    const size = document.getElementById('info-size').value.trim();
    const neutral = document.getElementById('info-neutral').value;
    const chip = document.getElementById('info-chip').value;

    const details = [];
    if (gender) details.push(gender);
    if (age) details.push(age);
    if (color) details.push(color);
    if (breed) details.push(breed);
    if (size) details.push(size);
    if (neutral && neutral !== '선택 안함') details.push(`중성화 ${neutral}`);
    if (chip && chip !== '선택 안함') details.push(chip === '없음' ? '칩 없음' : chip);

    if (name) {
      if (details.length > 0) {
        viewProfileBar.innerText = `${name} (${details.join(', ')})`;
      } else {
        viewProfileBar.innerText = name;
      }
    } else {
      if (details.length > 0) {
        viewProfileBar.innerText = `(${details.join(', ')})`;
      } else {
        viewProfileBar.innerText = '';
      }
    }
  }

  if (document.getElementById('view-date'))
    document.getElementById('view-date').innerText = document.getElementById('input-date').value;

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
// 타이틀 스타일 테마 변경
// =========================
function changeTitleTheme() {
  const themeSelect = document.getElementById('input-title-theme');
  const canvasTitle = document.getElementById('canvas-title');
  if (themeSelect && canvasTitle) {
    const selectedTheme = themeSelect.value;
    canvasTitle.className = `static-title ${selectedTheme}`;
  }
}

// =========================
// 요소 선택
// =========================
function selectElement(event) {
  event.stopPropagation();
  hideContextMenu();

  if (selectedElement) {
    selectedElement.classList.remove('selected');
    // Remove any existing floating editor
    const oldEditor = selectedElement.querySelector('.image-floating-editor');
    if (oldEditor) oldEditor.remove();
  }

  selectedElement = event.currentTarget;
  selectedElement.classList.add('selected');

  const handle = selectedElement.querySelector('.resize-handle');
  if (handle) handle.onmousedown = startResize;

  // 실제 마우스 클릭(mousedown) 시에만 드래그/리사이즈 동작이 시작되도록 호출
  if (event && event.type === 'mousedown') {
    if (event.target.classList.contains('resize-handle')) {
      startResize(event);
    } else {
      startDrag(event);
    }
  }

  // 선택한 요소 내에 업로드된 사진이 있으면 플로팅 조절 패널 띄우기
  const img = selectedElement.querySelector('.uploaded-image');
  if (img) {
    // 혹시라도 이미 존재한다면 중복 추가 방지
    let floatingEditor = selectedElement.querySelector('.image-floating-editor');
    if (!floatingEditor) {
      const currentScale = parseFloat(img.getAttribute('data-scale')) || 1.0;
      floatingEditor = document.createElement('div');
      floatingEditor.className = 'image-floating-editor';
      floatingEditor.setAttribute('onmousedown', 'event.stopPropagation()');
      floatingEditor.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255, 255, 255, 0.1); padding-bottom: 6px; margin-bottom: 4px;">
          <span style="font-size: 11px; font-weight: 700; color: #cbd5e1; display: flex; align-items: center; gap: 4px;">🖼️ 사진 크기/위치 조절</span>
          <button onclick="closeFloatingEditor(event)" style="background: none; border: none; color: #94a3b8; font-size: 14px; font-weight: bold; cursor: pointer; padding: 2px 6px; line-height: 1;">×</button>
        </div>
        
        <div style="display: flex; flex-direction: column; gap: 6px;">
          <div style="display: flex; justify-content: space-between; align-items: center; font-size: 11px; color: #94a3b8;">
            <span>🔍 확대/축소</span>
            <span id="floating-zoom-value" style="color: #38bdf8; font-weight: bold;">${currentScale.toFixed(1)}x</span>
          </div>
          
          <div style="display: flex; gap: 6px; align-items: center;">
            <button onclick="adjustZoom(-0.1)" style="background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 6px; color: #fff; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; cursor: pointer; transition: all 0.2s;">-</button>
            <input type="range" id="floating-zoom-slider" min="1" max="4" step="0.05" value="${currentScale}" 
                   style="flex: 1; height: 6px; border-radius: 3px; background: rgba(255, 255, 255, 0.2); outline: none; accent-color: #38bdf8; cursor: pointer; margin: 0;"
                   oninput="syncFloatingZoom(this.value)">
            <button onclick="adjustZoom(0.1)" style="background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 6px; color: #fff; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; cursor: pointer; transition: all 0.2s;">+</button>
          </div>
        </div>

        <div style="display: flex; gap: 6px; margin-top: 4px;">
          <button onclick="triggerSidebarImageUpload()" style="flex: 1; background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 6px; color: #fff; font-size: 11px; padding: 6px; cursor: pointer; transition: all 0.2s; font-weight: bold;">🔄 사진교체</button>
          <button onclick="resetImagePosition()" style="flex: 1; background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 6px; color: #fff; font-size: 11px; padding: 6px; cursor: pointer; transition: all 0.2s; font-weight: bold;">🎯 중앙정렬</button>
          <button onclick="deleteFloatingImage(event)" style="flex: 1; background: rgba(239, 68, 68, 0.2); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 6px; color: #fecaca; font-size: 11px; padding: 6px; cursor: pointer; transition: all 0.2s; font-weight: bold;">🗑️ 삭제</button>
        </div>
      `;
      selectedElement.appendChild(floatingEditor);
    }
  }
}

function deselectAll(event) {
  hideContextMenu();

  if (event.target.id === 'leaflet-canvas') {
    if (selectedElement) {
      selectedElement.classList.remove('selected');
      // Remove floating editor
      const editor = selectedElement.querySelector('.image-floating-editor');
      if (editor) editor.remove();
    }
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

function closeFloatingEditor(event) {
  if (event) event.stopPropagation();
  if (selectedElement) {
    selectedElement.classList.remove('selected');
    const editor = selectedElement.querySelector('.image-floating-editor');
    if (editor) editor.remove();
    selectedElement = null;
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

  const newWidth = Math.max(30, startWidth + (e.clientX - startX));
  const newHeight = Math.max(30, startHeight + (e.clientY - startY));

  selectedElement.style.width = `${newWidth}px`;
  selectedElement.style.height = `${newHeight}px`;
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
  newElem.onmousedown = selectElement;

  newElem.innerHTML = `
    <div class="element-content reward-only-box">
      <div class="reward-line">사례금: <span id="view-reward"></span>원</div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
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
  newElem.onmousedown = selectElement;

  newElem.innerHTML = `
    <div class="element-content image-placeholder" style="font-size:11px;">
      🔲 QR코드
      <input type="file" accept="image/*" class="file-input" style="display: none;" onchange="uploadImage(event, this)">
    </div>
    <div class="image-edit-overlay">
      <div class="overlay-button-group" style="display: flex; flex-direction: column; gap: 8px; align-items: center; justify-content: center;">
        <button class="btn-image-edit-overlay" onclick="triggerOverlayUpload(event, this)">📷 이미지 등록</button>
      </div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
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
  newElem.onmousedown = selectElement;

  newElem.innerHTML = `
    <div class="element-content image-placeholder">
      📷 추가 사진
      <input type="file" accept="image/*" class="file-input" style="display: none;" onchange="uploadImage(event, this)">
    </div>
    <div class="image-edit-overlay">
      <div class="overlay-button-group" style="display: flex; flex-direction: column; gap: 8px; align-items: center; justify-content: center;">
        <button class="btn-image-edit-overlay" onclick="triggerOverlayUpload(event, this)">📷 사진 등록</button>
      </div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
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
  newElem.onmousedown = selectElement;

  newElem.innerHTML = `
    <div class="element-content" contenteditable="true" style="padding:8px; font-size:13px; outline:none; color:#333;">
      더블클릭하여 내용을 입력하세요.
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
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

// =========================
// 예시 문구 자동으로 비우기 및 복원
// =========================
function initPlaceholderBehavior() {
  const defaultValues = {
    'input-title': '강아지를 찾습니다',
    'info-name': '초코',
    'info-gender': '수컷',
    'info-breed': '푸들',
    'info-color': '갈색',
    'info-age': '3세',
    'info-size': '5kg',
    'input-date': '2026년 5월 19일 오후 2시경',
    'input-location': '서울시 마포구 망원한강공원 인근',
    'input-features': '빨간색 줄무늬 옷을 입고 있었습니다. 겁이 많으니 발견 시 억지로 잡으려 하지 마시고 바로 연락 부탁드립니다.',
    'input-reward': '500,000',
    'input-contact': '010-XXXX-XXXX'
  };

  Object.keys(defaultValues).forEach(id => {
    const element = document.getElementById(id);
    if (!element) return;

    // 브라우저 네이티브 placeholder 속성을 적용하여 비어있을 때 예시 문구가 회색으로 뜨도록 함
    element.placeholder = defaultValues[id];

    element.addEventListener('focus', function() {
      if (this.value.trim() === defaultValues[id].trim()) {
        this.value = '';
        syncText();
      }
    });

    element.addEventListener('blur', function() {
      // 강제로 예시 문구를 실제 값으로 채워 넣지 않고 진짜 빈 칸으로 둠 (전단지에서 가림)
      if (this.value.trim() === '') {
        syncText();
      }
    });
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initPlaceholderBehavior);
} else {
  initPlaceholderBehavior();
}

// 개별 요소의 X 버튼 클릭 시 삭제 로직
function deleteThisElement(event, btn) {
  if (event) event.stopPropagation();
  const element = btn.closest('.element');
  if (element) {
    if (selectedElement === element) {
      selectedElement = null;
    }
    element.remove();
  }
}

// =========================
// 기본 인적사항 복구
// =========================
function addProfileElement() {
  if (document.getElementById('view-profile-bar')) {
    alert("이미 기본 인적사항 항목이 있습니다.");
    return;
  }
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');
  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 350px; left: 30px; width: 440px; height: 45px; z-index:10;';
  newElem.onmousedown = selectElement;
  newElem.innerHTML = `
    <div class="element-content profile-red-bar">
      <span id="view-profile-bar">초코 (수컷, 3세, 갈색, 푸들)</span>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
    <div class="resize-handle"></div>
  `;
  canvas.appendChild(newElem);
  syncText();
  toggleGuideLines();
}

// =========================
// 실종일시/장소 복구
// =========================
function addDateTimeLocationElement() {
  if (document.getElementById('view-date') || document.getElementById('view-location')) {
    alert("이미 실종일시/장소 항목이 있습니다.");
    return;
  }
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');
  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 405px; left: 30px; width: 440px; height: 70px; z-index:10;';
  newElem.onmousedown = selectElement;
  newElem.innerHTML = `
    <div class="element-content info-details-box">
      <div class="info-row">🗓️ <b>실종일시:</b> <span id="view-date">2026년 5월 19일 오후 2시경</span></div>
      <div class="info-row">📍 <b>실종장소:</b> <span id="view-location">서울시 마포구 망원한강공원 인근</span></div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
    <div class="resize-handle"></div>
  `;
  canvas.appendChild(newElem);
  syncText();
  toggleGuideLines();
}

// =========================
// 특징 및 목격자 안내 복구
// =========================
function addFeaturesElement() {
  if (document.getElementById('view-features')) {
    alert("이미 특징 및 목격자 안내 항목이 있습니다.");
    return;
  }
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');
  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 485px; left: 30px; width: 440px; height: 100px; z-index:10;';
  newElem.onmousedown = selectElement;
  newElem.innerHTML = `
    <div class="element-content info-details-box">
      <div class="feature-title">💡 특징 및 목격자 안내</div>
      <div id="view-features" class="feature-text">빨간색 줄무늬 옷을 입고 있었습니다. 겁이 많으니 발견 시 억지로 잡으려 하지 마시고 바로 연락 부탁드립니다.</div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
    <div class="resize-handle"></div>
  `;
  canvas.appendChild(newElem);
  syncText();
  toggleGuideLines();
}

// =========================
// 연락처 복구
// =========================
function addContactElement() {
  if (document.getElementById('view-contact')) {
    alert("이미 연락처 항목이 있습니다.");
    return;
  }
  const canvas = document.getElementById('leaflet-canvas');
  const newElem = document.createElement('div');
  newElem.id = 'container-contact';
  newElem.className = 'element draggable resizable';
  newElem.style = 'top: 630px; left: 30px; width: 440px; height: 45px; z-index:10;';
  newElem.onmousedown = selectElement;
  newElem.innerHTML = `
    <div class="element-content contact-only-box">
      <div class="contact-line">📞 연락처: <span id="view-contact">010-XXXX-XXXX</span></div>
    </div>
    <div class="delete-btn" onclick="deleteThisElement(event, this)">×</div>
    <div class="resize-handle"></div>
  `;
  canvas.appendChild(newElem);
  syncText();
  toggleGuideLines();
}

// =========================
// 이미지 실시간 업로드 및 렌더링
// =========================
function triggerImageUpload(element) {
  const fileInput = element.querySelector('.file-input');
  if (fileInput) {
    fileInput.click();
  }
}

function uploadImage(event, input) {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const dataUrl = e.target.result;
    const parent = input.closest('.element-content');
    const element = parent.closest('.element');
    
    // Remove placeholder class and set up direct image tag covering the box
    parent.classList.remove('image-placeholder');
    parent.innerHTML = `
      <img src="${dataUrl}" class="uploaded-image" data-scale="1" data-x="0" data-y="0"
           onmousedown="startPanning(event, this)"
           onwheel="handleWheelZoom(event, this)"
           style="position: absolute; left: 50%; top: 50%; width: 100%; height: 100%; object-fit: cover; transform: translate(-50%, -50%) translate(0px, 0px) scale(1); transform-origin: center center; cursor: grab; pointer-events: auto;">
      <input type="file" accept="image/*" class="file-input" style="display: none;" onchange="uploadImage(event, this)">
    `;
    
    // Remove any legacy floating toolbar to keep clean
    const oldToolbar = element.querySelector('.image-adjust-toolbar');
    if (oldToolbar) oldToolbar.remove();
    
    parent.onclick = null;
    
    // 업로드 즉시 해당 요소를 활성 선택하여 사이드바 조절창이 열리게 함
    selectElement({
      stopPropagation: () => {},
      currentTarget: element,
      clientX: parseFloat(element.style.left) || 0,
      clientY: parseFloat(element.style.top) || 0
    });
  };
  reader.readAsDataURL(file);
}

// =========================
// 사진 휠(스크롤) 크기 조절 기능
// =========================
function handleWheelZoom(e, img) {
  e.preventDefault();
  
  let scale = parseFloat(img.getAttribute('data-scale')) || 1;
  const zoomSpeed = 0.08;
  
  if (e.deltaY < 0) {
    // 휠 올림 -> 확대 (최대 4배)
    scale = Math.min(4, scale + zoomSpeed);
  } else {
    // 휠 내림 -> 축소 (최소 1배)
    scale = Math.max(1, scale - zoomSpeed);
  }
  
  img.setAttribute('data-scale', scale);
  
  // 좌측 사이드바 슬라이더 동기화
  const sidebarSlider = document.getElementById('sidebar-zoom-slider');
  if (sidebarSlider) {
    sidebarSlider.value = scale;
  }
  const zoomValDisplay = document.getElementById('zoom-value-display');
  if (zoomValDisplay) {
    zoomValDisplay.innerText = `${scale.toFixed(1)}x`;
  }
  
  const x = img.getAttribute('data-x') || '0';
  const y = img.getAttribute('data-y') || '0';
  img.style.transform = `translate(-50%, -50%) translate(${x}px, ${y}px) scale(${scale})`;
}

// =========================
// 사진 드래그 이동(위치) 기능
// =========================
let isPanning = false;
let panStartX = 0, panStartY = 0;
let imgStartX = 0, imgStartY = 0;

function startPanning(e, img) {
  // Prevent dragging the whole element container
  e.stopPropagation();
  e.preventDefault(); // 브라우저 자체의 이미지 드래그앤드롭 기본 동작 방지 (Custom Panning 완벽 보장)
  
  // Programmatically select parent element to make it editable and show controls/handles immediately
  const element = img.closest('.element');
  if (element) {
    selectElement({
      stopPropagation: () => {},
      currentTarget: element
    });
  }
  
  isPanning = true;
  panStartX = e.clientX;
  panStartY = e.clientY;
  imgStartX = parseFloat(img.getAttribute('data-x')) || 0;
  imgStartY = parseFloat(img.getAttribute('data-y')) || 0;
  
  img.style.cursor = 'grabbing';

  function doPan(ev) {
    if (!isPanning) return;
    const dx = ev.clientX - panStartX;
    const dy = ev.clientY - panStartY;
    const newX = imgStartX + dx;
    const newY = imgStartY + dy;
    
    img.setAttribute('data-x', newX);
    img.setAttribute('data-y', newY);
    
    const scale = img.getAttribute('data-scale') || '1';
    img.style.transform = `translate(-50%, -50%) translate(${newX}px, ${newY}px) scale(${scale})`;
  }

  function stopPan() {
    isPanning = false;
    img.style.cursor = 'grab';
    document.removeEventListener('mousemove', doPan);
    document.removeEventListener('mouseup', stopPan);
  }

  document.addEventListener('mousemove', doPan);
  document.addEventListener('mouseup', stopPan);
}

// =========================
// 플로팅 이미지 조절 핸들러
// =========================
function syncFloatingZoom(value) {
  if (!selectedElement) return;
  const img = selectedElement.querySelector('.uploaded-image');
  if (img) {
    img.setAttribute('data-scale', value);
    const valDisplay = document.getElementById('floating-zoom-value');
    if (valDisplay) valDisplay.innerText = `${parseFloat(value).toFixed(1)}x`;
    const x = img.getAttribute('data-x') || '0';
    const y = img.getAttribute('data-y') || '0';
    img.style.transform = `translate(-50%, -50%) translate(${x}px, ${y}px) scale(${value})`;
  }
}

function adjustZoom(amount) {
  if (!selectedElement) return;
  const img = selectedElement.querySelector('.uploaded-image');
  if (img) {
    let scale = parseFloat(img.getAttribute('data-scale')) || 1.0;
    scale = Math.max(1, Math.min(4, scale + amount));
    
    img.setAttribute('data-scale', scale);
    
    // sync floating slider
    const slider = document.getElementById('floating-zoom-slider');
    if (slider) slider.value = scale;
    
    // sync floating display
    const valDisplay = document.getElementById('floating-zoom-value');
    if (valDisplay) valDisplay.innerText = `${scale.toFixed(1)}x`;
    
    const x = img.getAttribute('data-x') || '0';
    const y = img.getAttribute('data-y') || '0';
    img.style.transform = `translate(-50%, -50%) translate(${x}px, ${y}px) scale(${scale})`;
  }
}

function triggerSidebarImageUpload() {
  if (!selectedElement) return;
  const fileInput = selectedElement.querySelector('.file-input');
  if (fileInput) {
    fileInput.click();
  }
}

function resetImagePosition() {
  if (!selectedElement) return;
  const img = selectedElement.querySelector('.uploaded-image');
  if (img) {
    img.setAttribute('data-x', '0');
    img.setAttribute('data-y', '0');
    img.setAttribute('data-scale', '1');
    
    // 플로팅 UI 리셋
    const slider = document.getElementById('floating-zoom-slider');
    if (slider) slider.value = 1;
    const valDisplay = document.getElementById('floating-zoom-value');
    if (valDisplay) valDisplay.innerText = '1.0x';
    
    img.style.transform = `translate(-50%, -50%) translate(0px, 0px) scale(1)`;
  }
}

// =========================
// 오버레이 사진 등록/변경 버튼 클릭 핸들러
// =========================
function triggerOverlayUpload(event, btn) {
  // 이벤트 버블링을 방지하여 요소 선택 및 드래그 동작 간섭 차단
  event.stopPropagation();
  
  const element = btn.closest('.element');
  const fileInput = element.querySelector('.file-input');
  if (fileInput) {
    fileInput.click();
  }
}

// =========================
// 오버레이 사진 삭제 버튼 클릭 핸들러
// =========================
function deleteOverlayImage(event, btn) {
  // 이벤트 버블링 방지하여 드래그/선택 차단
  event.stopPropagation();
  
  const element = btn.closest('.element');
  const content = element.querySelector('.element-content');
  if (!content) return;
  
  // 위치 정보를 기반으로 어떤 사진 박스인지 파악하여 정확한 플레이스홀더 복구
  let label = '📷 사진 등록 영역';
  const leftVal = parseInt(element.style.left) || 0;
  if (leftVal === 30) {
    label = '📸 사진 1 등록 영역';
  } else if (leftVal === 260) {
    label = '📸 사진 2 등록 영역';
  } else {
    label = '📷 추가 사진';
  }
  
  // 플레이스홀더 상태로 복구
  content.classList.add('image-placeholder');
  content.innerHTML = `
    ${label}
    <input type="file" accept="image/*" class="file-input" style="display: none;" onchange="uploadImage(event, this)">
  `;
  
  // 업로드 시 설정된 임시 클릭 핸들러가 있다면 제거
  content.onclick = null;
  
  // 플로팅 이미지 편집 창 닫기
  const editor = element.querySelector('.image-floating-editor');
  if (editor) editor.remove();
}

function deleteFloatingImage(event) {
  if (event) event.stopPropagation();
  if (!selectedElement) return;
  
  const content = selectedElement.querySelector('.element-content');
  if (!content) return;
  
  // 위치 정보를 기반으로 어떤 사진 박스인지 파악하여 정확한 플레이스홀더 복구
  let label = '📷 사진 등록 영역';
  const leftVal = parseInt(selectedElement.style.left) || 0;
  if (leftVal === 30) {
    label = '📸 사진 1 등록 영역';
  } else if (leftVal === 260) {
    label = '📸 사진 2 등록 영역';
  } else {
    label = '📷 추가 사진';
  }
  
  // 플레이스홀더 상태로 복구
  content.classList.add('image-placeholder');
  content.innerHTML = `
    ${label}
    <input type="file" accept="image/*" class="file-input" style="display: none;" onchange="uploadImage(event, this)">
  `;
  
  // 업로드 시 설정된 임시 클릭 핸들러가 있다면 제거
  content.onclick = null;
  
  // 플로팅 이미지 편집 창 닫기
  const editor = selectedElement.querySelector('.image-floating-editor');
  if (editor) editor.remove();
  
  // 선택 해제
  selectedElement.classList.remove('selected');
  selectedElement = null;
}

// =========================
// 이미지/PDF 저장
// =========================
async function savePoster(format) {
  const canvasElement = document.getElementById('leaflet-canvas');
  if (!canvasElement) return;

  // 1. 캡쳐 전 준비: 선택 해제 및 가이드라인 숨기기 등
  deselectAll({ target: canvasElement });
  
  const toggleGuide = document.getElementById('toggle-guide');
  const originalToggleState = toggleGuide ? toggleGuide.checked : false;
  if (originalToggleState) {
    toggleGuide.checked = false;
    toggleGuideLines();
  }
  
  // 삭제 버튼, 크기 조절 핸들, 오버레이 숨기기
  const elementsToHide = canvasElement.querySelectorAll('.delete-btn, .resize-handle, .image-edit-overlay, .image-floating-editor');
  elementsToHide.forEach(el => el.style.display = 'none');
  
  // 편집 가능 요소 블러 처리
  if (document.activeElement && document.activeElement.blur) {
      document.activeElement.blur();
  }

  try {
    // 2. html2canvas로 캡쳐
    const canvas = await html2canvas(canvasElement, {
      scale: 2, // 고해상도
      useCORS: true, // 외부 이미지(QR 등) 허용
      backgroundColor: '#ffffff'
    });

    const imageData = canvas.toDataURL('image/png');

    if (format === 'png') {
      // 3. PNG 다운로드
      const link = document.createElement('a');
      link.download = '실종전단지.png';
      link.href = imageData;
      link.click();
    } else if (format === 'pdf') {
      // 3. PDF 다운로드 (jsPDF)
      const { jsPDF } = window.jspdf;
      
      const canvasWidth = canvas.width;
      const canvasHeight = canvas.height;
      
      // 캔버스 픽셀 크기에 딱 맞춘 PDF 생성 (흰 여백 제거)
      const pdf = new jsPDF({
        orientation: canvasWidth > canvasHeight ? 'landscape' : 'portrait',
        unit: 'px',
        format: [canvasWidth, canvasHeight]
      });

      pdf.addImage(imageData, 'PNG', 0, 0, canvasWidth, canvasHeight);
      pdf.save('실종전단지.pdf');
    }

  } catch (error) {
    console.error('전단지 저장 중 오류 발생:', error);
    alert('전단지 저장 중 오류가 발생했습니다.');
  } finally {
    // 4. 캡쳐 후 원상복구
    if (originalToggleState) {
      toggleGuide.checked = true;
      toggleGuideLines();
    }
    elementsToHide.forEach(el => el.style.display = '');
  }
}