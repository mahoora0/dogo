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
      const card = document.createElement("article");
      card.className = "missing-alert-card";

      const head = document.createElement("div");
      head.className = "missing-alert-card-head";

      const type = document.createElement("span");
      type.textContent = compactText(alert.targetType, "실종 경보");
      const date = document.createElement("time");
      date.textContent = compactText(alert.occurrenceDate, "발생일 미상");
      head.append(type, date);

      const title = document.createElement("h2");
      title.textContent = alertSummary(alert);

      const detailList = document.createElement("dl");
      [
        ["이름", compactText(alert.name, "미상")],
        ["키/몸무게", `${compactText(alert.height, "-")} / ${compactText(alert.weight, "-")}`],
        ["체격", compactText(alert.bodyType, "미상")],
        ["두발", `${compactText(alert.hairColor, "미상")} · ${compactText(alert.hairStyle, "미상")}`]
      ].forEach(([label, value]) => {
        const item = document.createElement("div");
        const term = document.createElement("dt");
        const description = document.createElement("dd");
        term.textContent = label;
        description.textContent = value;
        item.append(term, description);
        detailList.appendChild(item);
      });

      const dressing = document.createElement("p");
      dressing.textContent = compactText(alert.dressing, "착의 정보가 제공되지 않았습니다.");

      const source = document.createElement("small");
      source.textContent = compactText(alert.sourceLabel, "자료 출처: 경찰청");

      card.append(head, title, detailList, dressing, source);
      grid.appendChild(card);
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
