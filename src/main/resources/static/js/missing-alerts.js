(function () {
  const grid = document.getElementById("missingAlertGrid");
  const status = document.getElementById("missingAlertStatus");
  const dateInput = document.getElementById("missingAlertDate");
  const sizeInput = document.getElementById("missingAlertSize");
  const refreshButton = document.getElementById("missingAlertRefresh");
  const summary = document.getElementById("homeMissingAlertSummary");
  const pagination = document.getElementById("missingAlertPagination");
  let currentPage = 1;

  if (!grid && !summary) {
    return;
  }

  function formatDateForApi(dateValue) {
    return dateValue ? dateValue.replaceAll("-", "") : "";
  }

  function compactText(value, fallback) {
    return value && value.trim() ? value.trim() : fallback;
  }

  function alertSummary(alert) {
    const address = compactText(alert.occurrenceAddress, "장소 미상");
    const age = compactText(alert.ageNow || alert.age, "나이 미상");
    const sex = compactText(alert.sex, "성별 미상");
    return `${address} / ${age} / ${sex}`;
  }

  function setStatus(message, tone) {
    if (!status) {
      return;
    }
    if (!message) {
      status.style.display = "none";
      return;
    }
    status.style.display = "block";
    status.textContent = message;
    status.dataset.tone = tone || "neutral";
  }

  function renderCards(alerts) {
    if (!grid) {
      return;
    }
    grid.innerHTML = "";

    alerts.forEach((alert) => {
      const row = document.createElement("article");
      row.className = "missing-alert-row";

      // 1. Left side: Target type and Occurrence date
      const leftCol = document.createElement("div");
      leftCol.className = "missing-alert-col-left";

      const type = document.createElement("span");
      type.className = "missing-alert-badge";
      const targetType = compactText(alert.targetType, "실종 경보");
      type.textContent = targetType;

      if (targetType.includes("아동")) {
        type.classList.add("badge-child");
      } else if (targetType.includes("장애")) {
        type.classList.add("badge-disabled");
      } else if (targetType.includes("치매")) {
        type.classList.add("badge-dementia");
      } else {
        type.classList.add("badge-default");
      }

      const date = document.createElement("time");
      date.className = "missing-alert-date";
      date.textContent = compactText(alert.occurrenceDate, "발생일 미상");

      leftCol.append(type, date);

      // 2. Middle side: Profile details (Name, gender, age, location)
      const midCol = document.createElement("div");
      midCol.className = "missing-alert-col-mid";

      const profileInfo = document.createElement("div");
      profileInfo.className = "missing-alert-profile";

      const nameSpan = document.createElement("strong");
      nameSpan.className = "missing-alert-name";
      nameSpan.textContent = compactText(alert.name, "미상");

      const ageSexSpan = document.createElement("span");
      ageSexSpan.className = "missing-alert-age-sex";
      const ageNowText = alert.ageNow ? ` / 현재 ${alert.ageNow}세` : "";
      const ageText = alert.age ? `당시 ${alert.age}세${ageNowText}` : "나이 미상";
      const sexText = compactText(alert.sex, "성별 미상");
      ageSexSpan.textContent = `(${ageText}, ${sexText})`;

      profileInfo.append(nameSpan, ageSexSpan);

      const locationDiv = document.createElement("div");
      locationDiv.className = "missing-alert-location";

      const locIcon = document.createElement("span");
      locIcon.className = "location-icon";
      locIcon.textContent = "📍";

      const locText = document.createElement("span");
      locText.textContent = `발생장소: ${compactText(alert.occurrenceAddress, "장소 미상")}`;

      locationDiv.append(locIcon, locText);
      midCol.append(profileInfo, locationDiv);

      // 3. Right side: Physical features and clothing
      const rightCol = document.createElement("div");
      rightCol.className = "missing-alert-col-right";

      const specsDiv = document.createElement("div");
      specsDiv.className = "missing-alert-specs";

      const specLabel = document.createElement("span");
      specLabel.className = "spec-label";
      specLabel.textContent = "신체특징";

      const specValue = document.createElement("span");
      specValue.className = "spec-value";

      const heightWeight = `${compactText(alert.height, "-")}cm / ${compactText(alert.weight, "-")}kg`;
      const bodyType = compactText(alert.bodyType, "체격 미상");
      const hair = `${compactText(alert.hairColor, "미상")} 두발`;
      specValue.textContent = `${heightWeight}, ${bodyType}, ${hair}`;
      specsDiv.append(specLabel, specValue);

      const clothingDiv = document.createElement("div");
      clothingDiv.className = "missing-alert-clothing";

      const clothLabel = document.createElement("span");
      clothLabel.className = "cloth-label";
      clothLabel.textContent = "착의사항";

      const clothValue = document.createElement("span");
      clothValue.className = "cloth-value";
      clothValue.textContent = compactText(alert.dressing, "착의 정보가 제공되지 않았습니다.");
      clothingDiv.append(clothLabel, clothValue);

      rightCol.append(specsDiv, clothingDiv);

      // 4. Source Column
      const sourceCol = document.createElement("div");
      sourceCol.className = "missing-alert-col-source";

      const source = document.createElement("small");
      source.className = "missing-alert-source";
      source.textContent = compactText(alert.sourceLabel, "자료 출처: 경찰청");

      sourceCol.append(source);

      // Assemble
      row.append(leftCol, midCol, rightCol, sourceCol);
      grid.appendChild(row);
    });
  }

  function renderHomeSummary(alerts) {
    if (!summary || alerts.length === 0) {
      return;
    }
    summary.textContent = `${alertSummary(alerts[0])} 실종 경보가 접수되었습니다.`;
  }

  // 페이징 컨트롤 동적 렌더링 함수
  function renderPagination(totalCount, rowSize) {
    if (!pagination) return;
    pagination.innerHTML = "";

    const totalPages = Math.ceil(totalCount / rowSize);
    if (totalPages <= 1) {
      return;
    }

    // 다른 리스트 페이지들과 통일성 있는 Tailwind 클래스 추가
    pagination.className = "mt-12 flex items-center justify-center gap-3 text-sm";

    // 1. [이전] 버튼 생성
    const prevBtn = document.createElement("button");
    prevBtn.className = "flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 shadow-sm transition-all hover:bg-slate-100 hover:text-slate-900 hover:border-slate-300 active:scale-95";
    if (currentPage === 1) {
      prevBtn.classList.add("pointer-events-none", "opacity-40");
    }
    prevBtn.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"/>
      </svg>
    `;
    prevBtn.addEventListener("click", () => {
      if (currentPage > 1) {
        currentPage--;
        loadAlerts();
      }
    });
    pagination.appendChild(prevBtn);

    // 2. [현재페이지 / 총페이지] 캡슐 생성
    const midDisplay = document.createElement("div");
    midDisplay.className = "flex items-center gap-1 rounded-full bg-white px-4 py-2 shadow-sm border border-slate-100 font-semibold";
    midDisplay.innerHTML = `
      <span class="text-slate-900">${currentPage}</span>
      <span class="text-slate-300 mx-1">/</span>
      <span class="text-slate-500">${totalPages}</span>
    `;
    pagination.appendChild(midDisplay);

    // 3. [다음] 버튼 생성
    const nextBtn = document.createElement("button");
    nextBtn.className = "flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 shadow-sm transition-all hover:bg-slate-100 hover:text-slate-900 hover:border-slate-300 active:scale-95";
    if (currentPage === totalPages) {
      nextBtn.classList.add("pointer-events-none", "opacity-40");
    }
    nextBtn.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7"/>
      </svg>
    `;
    nextBtn.addEventListener("click", () => {
      if (currentPage < totalPages) {
        currentPage++;
        loadAlerts();
      }
    });
    pagination.appendChild(nextBtn);
  }

  async function loadAlerts() {
    const params = new URLSearchParams();
    const rowSize = sizeInput ? parseInt(sizeInput.value) : 10;
    params.set("rowSize", rowSize.toString());
    params.set("page", currentPage.toString());

    const dateValue = dateInput ? formatDateForApi(dateInput.value) : "";
    if (dateValue) {
      params.set("occrde", dateValue);
    }

    setStatus("실종 경보를 불러오는 중입니다.");

    try {
      const response = await fetch(`/api/missing-alerts?${params.toString()}`);
      if (!response.ok) {
        throw new Error("missing-alert-request-failed");
      }

      const pageData = await response.json();
      const alerts = Array.isArray(pageData.alerts) ? pageData.alerts : [];
      renderHomeSummary(alerts);

      if (alerts.length === 0) {
        renderCards([]);
        if (pagination) pagination.innerHTML = "";
        setStatus("조회된 실종 경보가 없습니다.", "empty");
        return;
      }

      renderCards(alerts);
      
      // 페이징 컨트롤 렌더링
      const totalCount = pageData.totalCount || alerts.length;
      renderPagination(totalCount, rowSize);
      
      const startNum = (currentPage - 1) * rowSize + 1;
      const endNum = Math.min(currentPage * rowSize, totalCount);
      setStatus("", "success");
    } catch (error) {
      renderCards([]);
      if (pagination) pagination.innerHTML = "";
      setStatus("실종 경보 API 정보를 불러오지 못했습니다. 인증 정보 또는 네트워크 상태를 확인해주세요.", "error");
    }
  }

  if (refreshButton) {
    refreshButton.addEventListener("click", () => {
      currentPage = 1;
      loadAlerts();
    });
  }

  if (sizeInput) {
    sizeInput.addEventListener("change", () => {
      currentPage = 1;
      loadAlerts();
    });
  }

  if (dateInput) {
    dateInput.addEventListener("change", () => {
      currentPage = 1;
      loadAlerts();
    });
  }

  loadAlerts();
})();
