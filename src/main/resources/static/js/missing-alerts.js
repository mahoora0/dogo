(function () {
  const grid = document.getElementById("missingAlertGrid");
  const status = document.getElementById("missingAlertStatus");
  const dateInput = document.getElementById("missingAlertDate");
  const sizeInput = document.getElementById("missingAlertSize");
  const refreshButton = document.getElementById("missingAlertRefresh");
  const summary = document.getElementById("homeMissingAlertSummary");

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

  async function loadAlerts() {
    const params = new URLSearchParams();
    params.set("rowSize", sizeInput ? sizeInput.value : "1");

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

      const page = await response.json();
      const alerts = Array.isArray(page.alerts) ? page.alerts : [];
      renderHomeSummary(alerts);

      if (alerts.length === 0) {
        renderCards([]);
        setStatus("조회된 실종 경보가 없습니다.", "empty");
        return;
      }

      renderCards(alerts);
      setStatus(`총 ${page.totalCount || alerts.length}건 중 ${alerts.length}건을 표시합니다.`, "success");
    } catch (error) {
      renderCards([]);
      setStatus("실종 경보 API 정보를 불러오지 못했습니다. 인증 정보 또는 네트워크 상태를 확인해주세요.", "error");
    }
  }

  if (refreshButton) {
    refreshButton.addEventListener("click", loadAlerts);
  }

  loadAlerts();
})();
