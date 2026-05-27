(function () {
  function initImageUploader() {
    var input = document.getElementById('imageInput');
    if (!input) return;

    var preview = document.getElementById('imagePreview');
    var list = document.getElementById('imageList');
    var count = document.getElementById('imageCount');
    var clearBtn = document.getElementById('imageClear');
    var dropZone = document.getElementById('imageDropZone');
    if (!preview || !list || !count || !clearBtn || !dropZone) return;

    var promptEl = dropZone.querySelector('p');
    var emptyText = promptEl ? promptEl.textContent : '';
    var filledText = (promptEl && promptEl.getAttribute('data-filled-text')) || '클릭하여 이미지를 추가하세요';

    var selectedFiles = [];

    function fileKey(file) {
      return file.name + '|' + file.size + '|' + file.lastModified;
    }

    function syncInput() {
      try {
        var dt = new DataTransfer();
        selectedFiles.forEach(function (f) { dt.items.add(f); });
        input.files = dt.files;
      } catch (e) {
        // Older browsers may not support DataTransfer assignment; selectedFiles still drives UI.
      }
    }

    function buildThumbnail(file, index) {
      var wrap = document.createElement('div');
      wrap.className = 'relative group';

      var img = document.createElement('img');
      img.className = 'h-20 w-full rounded-lg border border-slate-200 object-cover';
      img.alt = file.name;
      var reader = new FileReader();
      reader.onload = function (e) { img.src = e.target.result; };
      reader.readAsDataURL(file);

      var removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'absolute -top-2 -right-2 inline-flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-white text-xs font-bold shadow hover:bg-red-600 focus:outline-none';
      removeBtn.setAttribute('aria-label', '이미지 삭제');
      removeBtn.textContent = '×';
      removeBtn.addEventListener('click', function () {
        selectedFiles.splice(index, 1);
        syncInput();
        render();
      });

      var nameEl = document.createElement('p');
      nameEl.className = 'mt-1 truncate text-xs text-slate-400';
      nameEl.textContent = file.name;

      wrap.appendChild(img);
      wrap.appendChild(removeBtn);
      wrap.appendChild(nameEl);
      return wrap;
    }

    function render() {
      list.innerHTML = '';
      if (selectedFiles.length === 0) {
        preview.classList.add('hidden');
        if (promptEl) promptEl.textContent = emptyText;
        return;
      }
      preview.classList.remove('hidden');
      count.textContent = selectedFiles.length + '장 선택됨';
      if (promptEl) promptEl.textContent = filledText;
      selectedFiles.forEach(function (file, idx) {
        list.appendChild(buildThumbnail(file, idx));
      });
    }

    input.addEventListener('change', function () {
      var picked = Array.from(input.files || []);
      var existing = {};
      selectedFiles.forEach(function (f) { existing[fileKey(f)] = true; });
      picked.forEach(function (f) {
        var key = fileKey(f);
        if (!existing[key]) {
          selectedFiles.push(f);
          existing[key] = true;
        }
      });
      syncInput();
      render();
    });

    clearBtn.addEventListener('click', function () {
      selectedFiles = [];
      syncInput();
      render();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initImageUploader);
  } else {
    initImageUploader();
  }
})();
