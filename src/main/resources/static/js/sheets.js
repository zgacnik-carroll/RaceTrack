/* =========================
   SHEETS JS
   - Running & Workout Spreadsheet Logic
========================= */

const role = (window.currentUserRole || "athlete").toLowerCase();
const userId = window.currentUserId || "";
const dateFilters = {
    running: "default",
    workout: "default"
};

function jsonHeaders() {
    const headers = { "Content-Type": "application/json" };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

function formatDate(dateString) {
    if (!dateString) return "";
    const d = new Date(dateString);
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const yy = String(d.getFullYear()).slice(-2);
    return `${mm}-${dd}-${yy}`;
}

function formatDateForInput(dateString) {
    if (!dateString) return "";
    if (typeof dateString === "string" && dateString.includes("T")) {
        return dateString.split("T")[0];
    }
    if (typeof dateString === "string" && dateString.length === 10) {
        return dateString;
    }

    const d = new Date(dateString);
    if (Number.isNaN(d.getTime())) return "";
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const yyyy = String(d.getFullYear());
    return `${yyyy}-${mm}-${dd}`;
}

function bindDateInputPicker(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const openPicker = () => {
        if (typeof input.showPicker === "function") {
            input.showPicker();
        }
    };
    input.addEventListener("focus", openPicker);
    input.addEventListener("click", openPicker);
}

function booleanDisplay(value) {
    if (value === true) return "Yes";
    if (value === false) return "No";
    return "";
}

function booleanSelect(value) {
    const normalized = value === true ? "true" : value === false ? "false" : "";
    return `
      <select class="sheet-input" data-field="bool">
        <option value="" ${normalized === "" ? "selected" : ""}></option>
        <option value="true" ${normalized === "true" ? "selected" : ""}>Yes</option>
        <option value="false" ${normalized === "false" ? "selected" : ""}>No</option>
      </select>
    `;
}

function feelSelect(value) {
    const normalized = String(value ?? "");
    const options = ["Good", "Okay", "Tired", "Sore", "Bad"];
    return `
      <select class="sheet-input" id="running-feel-select">
        <option value="" ${normalized === "" ? "selected" : ""}></option>
        ${options.map(option => `<option value="${option}" ${normalized === option ? "selected" : ""}>${option}</option>`).join("")}
      </select>
    `;
}

function clamp01(value) {
    return Math.max(0, Math.min(1, value));
}

function gradientColor01(score01) {
    const safe = clamp01(score01);
    const hue = 120 * safe;
    return `hsl(${hue}, 70%, 86%)`;
}

function sleepColor(sleepHours) {
    if (sleepHours === null || sleepHours === undefined) return "";
    const n = Number(sleepHours);
    if (Number.isNaN(n)) return "";
    return gradientColor01(n / 10);
}

function stressColor(stressLevel) {
    if (stressLevel === null || stressLevel === undefined) return "";
    const n = Number(stressLevel);
    if (Number.isNaN(n)) return "";
    const normalized = 1 - ((n - 1) / 9);
    return gradientColor01(normalized);
}

function yesNoColor(value) {
    if (value === true) return "hsl(120, 55%, 86%)";
    if (value === false) return "hsl(0, 65%, 87%)";
    return "";
}

function feelColor(value) {
    if (!value) return "";
    const map = {
        Good: "hsl(120, 55%, 86%)",
        Okay: "hsl(85, 65%, 86%)",
        Tired: "hsl(40, 80%, 85%)",
        Sore: "hsl(18, 80%, 85%)",
        Bad: "hsl(0, 65%, 87%)"
    };
    return map[value] ?? "";
}

function setWellnessCellColor(cellId, color) {
    const cell = document.getElementById(cellId);
    if (!cell) return;
    cell.style.backgroundColor = color || "";
}

function styleAttrFromColor(color) {
    if (!color) return "";
    return ` style="background-color:${color}"`;
}

function updateRunningRowColors(logId) {
    const sleepValue = parseIntOrNull(document.getElementById(`running-sleep-${logId}`)?.value);
    const stressValue = parseIntOrNull(document.getElementById(`running-stress-${logId}`)?.value);
    const hurtingValue = parseBoolean(document.getElementById(`running-hurting-${logId}`)?.value);
    const plateValue = parseBoolean(document.getElementById(`running-plate-${logId}`)?.value);
    const breadValue = parseBoolean(document.getElementById(`running-bread-${logId}`)?.value);
    const feelValue = document.getElementById(`running-feel-${logId}`)?.value ?? "";

    setWellnessCellColor(`running-hurting-cell-${logId}`, yesNoColor(hurtingValue));
    setWellnessCellColor(`running-sleep-cell-${logId}`, sleepColor(sleepValue));
    setWellnessCellColor(`running-stress-cell-${logId}`, stressColor(stressValue));
    setWellnessCellColor(`running-plate-cell-${logId}`, yesNoColor(plateValue));
    setWellnessCellColor(`running-bread-cell-${logId}`, yesNoColor(breadValue));
    setWellnessCellColor(`running-feel-cell-${logId}`, feelColor(feelValue));
}

function bindRunningRowColorHandlers(logId) {
    const hurtingSelect = document.getElementById(`running-hurting-${logId}`);
    const sleepInput = document.getElementById(`running-sleep-${logId}`);
    const stressInput = document.getElementById(`running-stress-${logId}`);
    const plateSelect = document.getElementById(`running-plate-${logId}`);
    const breadSelect = document.getElementById(`running-bread-${logId}`);
    const feelSelect = document.getElementById(`running-feel-${logId}`);

    if (hurtingSelect) hurtingSelect.addEventListener("change", () => updateRunningRowColors(logId));
    if (sleepInput) sleepInput.addEventListener("input", () => updateRunningRowColors(logId));
    if (stressInput) stressInput.addEventListener("input", () => updateRunningRowColors(logId));
    if (plateSelect) plateSelect.addEventListener("change", () => updateRunningRowColors(logId));
    if (breadSelect) breadSelect.addEventListener("change", () => updateRunningRowColors(logId));
    if (feelSelect) feelSelect.addEventListener("change", () => updateRunningRowColors(logId));
}

function getTargetUserId(inputUserId) {
    return inputUserId ?? selectedUserId ?? "me";
}

function startOfDay(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
}

function applyDateFilter(logs, type) {
    const range = dateFilters[type] ?? "default";
    const now = new Date();
    const todayStart = startOfDay(now);

    let filtered = logs;
    if (range === "today") {
        filtered = logs.filter(log => startOfDay(log.logDate).getTime() === todayStart.getTime());
    } else if (range === "week") {
        const weekStart = new Date(todayStart);
        weekStart.setDate(weekStart.getDate() - 6);
        filtered = logs.filter(log => new Date(log.logDate) >= weekStart);
    } else if (range === "month") {
        const monthStart = new Date(todayStart);
        monthStart.setDate(monthStart.getDate() - 29);
        filtered = logs.filter(log => new Date(log.logDate) >= monthStart);
    }

    if (range === "default") {
        return filtered.slice(0, 60);
    }

    return filtered;
}

function setDateFilter(type, range) {
    dateFilters[type] = range;
    updateFilterButtons(type);
    if (type === "running") {
        loadRunningLogs();
    } else {
        loadWorkoutLogs();
    }
}

function updateFilterButtons(type) {
    const ranges = ["default", "today", "week", "month"];
    ranges.forEach(range => {
        const button = document.getElementById(`${type}-filter-${range}`);
        if (!button) return;
        button.classList.toggle("active", dateFilters[type] === range);
    });
}

function canAthleteEdit(targetUserId) {
    if (role !== "athlete") return false;
    return targetUserId === "me" || targetUserId === userId;
}

function coachCommentCell(logId, comment, logType) {
    if (role !== "coach") {
        return `<td><div class="expandable-cell">${escapeHtml(comment ?? "")}</div></td>`;
    }

    return `
      <td>
        <textarea class="sheet-input sheet-textarea" id="${logType}-coach-comment-${logId}">${escapeHtml(comment ?? "")}</textarea>
        <button class="btn btn-sm btn-outline-primary mt-2" onclick="saveCoachComment('${logType}', ${logId})">Save Comment</button>
      </td>
    `;
}

/* =========================
   RUNNING LOGS
========================= */

function loadRunningLogs(requestedUserId) {
    const targetUserId = getTargetUserId(requestedUserId);
    fetch(`/api/running-logs/${targetUserId}`)
        .then(res => {
            if (!res.ok) throw new Error(`Failed to load running logs (${res.status})`);
            return res.json();
        })
        .then(data => {
            const tbody = document.getElementById("runningSpreadsheetBody");
            tbody.innerHTML = "";
            updateFilterButtons("running");

            const editable = canAthleteEdit(targetUserId);
            const filteredData = applyDateFilter(data, "running");
            filteredData.forEach(log => {
                const row = document.createElement("tr");

                if (editable) {
                    row.innerHTML = `
                        <td class="log-date-cell"><input class="sheet-input" type="date" value="${formatDateForInput(log.logDate)}" id="running-date-${log.id}"></td>
                        <td><input class="sheet-input" type="number" step="0.01" value="${log.mileage ?? ""}" id="running-mileage-${log.id}"></td>
                        <td id="running-hurting-cell-${log.id}" class="wellness-cell">${booleanSelect(log.hurting).replace('data-field="bool"', `id="running-hurting-${log.id}"`)}</td>
                        <td id="running-sleep-cell-${log.id}" class="wellness-cell"><input class="sheet-input" type="number" value="${log.sleepHours ?? ""}" id="running-sleep-${log.id}"></td>
                        <td id="running-stress-cell-${log.id}" class="wellness-cell"><input class="sheet-input" type="number" min="1" max="10" value="${log.stressLevel ?? ""}" id="running-stress-${log.id}"></td>
                        <td id="running-plate-cell-${log.id}" class="wellness-cell">${booleanSelect(log.plateProportion).replace('data-field="bool"', `id="running-plate-${log.id}"`)}</td>
                        <td id="running-bread-cell-${log.id}" class="wellness-cell">${booleanSelect(log.gotThatBread).replace('data-field="bool"', `id="running-bread-${log.id}"`)}</td>
                        <td id="running-feel-cell-${log.id}" class="wellness-cell">${feelSelect(log.feel).replace('id="running-feel-select"', `id="running-feel-${log.id}"`)}</td>
                        <td><input class="sheet-input" type="number" value="${log.rpe ?? ""}" id="running-rpe-${log.id}"></td>
                        <td><textarea class="sheet-input sheet-textarea" id="running-details-${log.id}">${escapeHtml(log.details ?? "")}</textarea></td>
                        <td><div class="expandable-cell">${escapeHtml(log.coachComment ?? "")}</div></td>
                        <td><button class="btn btn-sm btn-primary" onclick="saveRunningLog(${log.id})">Save</button></td>
                    `;
                } else {
                    row.innerHTML = `
                        <td class="log-date-cell">${formatDate(log.logDate)}</td>
                        <td>${escapeHtml(log.mileage ?? "")}</td>
                        <td class="wellness-cell"${styleAttrFromColor(yesNoColor(log.hurting))}>${booleanDisplay(log.hurting)}</td>
                        <td class="wellness-cell"${styleAttrFromColor(sleepColor(log.sleepHours))}>${escapeHtml(log.sleepHours ?? "")}</td>
                        <td class="wellness-cell"${styleAttrFromColor(stressColor(log.stressLevel))}>${escapeHtml(log.stressLevel ?? "")}</td>
                        <td class="wellness-cell"${styleAttrFromColor(yesNoColor(log.plateProportion))}>${booleanDisplay(log.plateProportion)}</td>
                        <td class="wellness-cell"${styleAttrFromColor(yesNoColor(log.gotThatBread))}>${booleanDisplay(log.gotThatBread)}</td>
                        <td class="wellness-cell"${styleAttrFromColor(feelColor(log.feel))}>${escapeHtml(log.feel ?? "")}</td>
                        <td>${escapeHtml(log.rpe ?? "")}</td>
                        <td class="expandable-cell">${escapeHtml(log.details ?? "")}</td>
                        ${coachCommentCell(log.id, log.coachComment, "running")}
                    `;
                }

                tbody.appendChild(row);

                if (editable) {
                    bindRunningRowColorHandlers(log.id);
                    updateRunningRowColors(log.id);
                    bindDateInputPicker(`running-date-${log.id}`);
                }
            });
        })
        .catch(error => {
            console.error(error);
        });
}

function showRunningSheet(userIdParam = null) {
    hideMainContent();
    document.getElementById("runningSpreadsheet").style.display = "block";
    loadRunningLogs(userIdParam);
}

function saveRunningLog(logId) {
    const payload = {
        mileage: parseNumber(document.getElementById(`running-mileage-${logId}`).value),
        hurting: parseBoolean(document.getElementById(`running-hurting-${logId}`).value),
        sleepHours: parseIntOrNull(document.getElementById(`running-sleep-${logId}`).value),
        stressLevel: parseIntOrNull(document.getElementById(`running-stress-${logId}`).value),
        plateProportion: parseBoolean(document.getElementById(`running-plate-${logId}`).value),
        gotThatBread: parseBoolean(document.getElementById(`running-bread-${logId}`).value),
        feel: document.getElementById(`running-feel-${logId}`).value,
        rpe: parseIntOrNull(document.getElementById(`running-rpe-${logId}`).value),
        details: document.getElementById(`running-details-${logId}`).value,
        logDate: document.getElementById(`running-date-${logId}`).value || null
    };

    fetch(`/api/running-logs/${logId}`, {
        method: "PUT",
        headers: jsonHeaders(),
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to save running row (${res.status})`);
            loadRunningLogs("me");
            if (window.showSaveNotice) window.showSaveNotice("Running row saved.", "success");
        })
        .catch(error => {
            console.error(error);
            if (window.showSaveNotice) window.showSaveNotice("Could not save running row.", "danger");
        });
}

/* =========================
   WORKOUT LOGS
========================= */

function loadWorkoutLogs(requestedUserId) {
    const targetUserId = getTargetUserId(requestedUserId);
    fetch(`/api/workout-logs/${targetUserId}`)
        .then(res => {
            if (!res.ok) throw new Error(`Failed to load workout logs (${res.status})`);
            return res.json();
        })
        .then(data => {
            const tbody = document.getElementById("workoutSpreadsheetBody");
            tbody.innerHTML = "";
            updateFilterButtons("workout");

            const editable = canAthleteEdit(targetUserId);
            const filteredData = applyDateFilter(data, "workout");
            filteredData.forEach(log => {
                const row = document.createElement("tr");

                if (editable) {
                    row.innerHTML = `
                        <td class="log-date-cell"><input class="sheet-input" type="date" value="${formatDateForInput(log.logDate)}" id="workout-date-${log.id}"></td>
                        <td><input class="sheet-input" type="text" value="${escapeHtml(log.workoutType ?? "")}" id="workout-type-${log.id}"></td>
                        <td><textarea class="sheet-input sheet-textarea" id="workout-completion-${log.id}">${escapeHtml(log.completionDetails ?? "")}</textarea></td>
                        <td><textarea class="sheet-input sheet-textarea" id="workout-paces-${log.id}">${escapeHtml(log.actualPaces ?? "")}</textarea></td>
                        <td><textarea class="sheet-input sheet-textarea" id="workout-description-${log.id}">${escapeHtml(log.workoutDescription ?? "")}</textarea></td>
                        <td><div class="expandable-cell">${escapeHtml(log.coachComment ?? "")}</div></td>
                        <td><button class="btn btn-sm btn-primary" onclick="saveWorkoutLog(${log.id})">Save</button></td>
                    `;
                } else {
                    row.innerHTML = `
                        <td class="log-date-cell">${formatDate(log.logDate)}</td>
                        <td>${escapeHtml(log.workoutType ?? "")}</td>
                        <td><div class="workout-expandable">${escapeHtml(log.completionDetails ?? "")}</div></td>
                        <td><div class="workout-expandable">${escapeHtml(log.actualPaces ?? "")}</div></td>
                        <td><div class="workout-expandable">${escapeHtml(log.workoutDescription ?? "")}</div></td>
                        ${coachCommentCell(log.id, log.coachComment, "workout")}
                    `;
                }

                tbody.appendChild(row);

                if (editable) {
                    bindDateInputPicker(`workout-date-${log.id}`);
                }
            });
        })
        .catch(error => {
            console.error(error);
        });
}

function showWorkoutSheet(userIdParam = null) {
    hideMainContent();
    document.getElementById("workoutSpreadsheet").style.display = "block";
    loadWorkoutLogs(userIdParam);
}

function saveWorkoutLog(logId) {
    const payload = {
        workoutType: document.getElementById(`workout-type-${logId}`).value,
        completionDetails: document.getElementById(`workout-completion-${logId}`).value,
        actualPaces: document.getElementById(`workout-paces-${logId}`).value,
        workoutDescription: document.getElementById(`workout-description-${logId}`).value,
        logDate: document.getElementById(`workout-date-${logId}`).value || null
    };

    fetch(`/api/workout-logs/${logId}`, {
        method: "PUT",
        headers: jsonHeaders(),
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to save workout row (${res.status})`);
            loadWorkoutLogs("me");
            if (window.showSaveNotice) window.showSaveNotice("Workout row saved.", "success");
        })
        .catch(error => {
            console.error(error);
            if (window.showSaveNotice) window.showSaveNotice("Could not save workout row.", "danger");
        });
}

/* =========================
   COACH COMMENTS
========================= */

function saveCoachComment(logType, logId) {
    const fieldId = `${logType}-coach-comment-${logId}`;
    const value = document.getElementById(fieldId).value;

    fetch(`/api/${logType}-logs/${logId}/coach-comment`, {
        method: "PUT",
        headers: jsonHeaders(),
        body: JSON.stringify({ coachComment: value })
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to save coach comment (${res.status})`);
            if (logType === "running") {
                loadRunningLogs();
            } else {
                loadWorkoutLogs();
            }
            if (window.showSaveNotice) window.showSaveNotice("Coach comment saved.", "success");
        })
        .catch(error => {
            console.error(error);
            if (window.showSaveNotice) window.showSaveNotice("Could not save coach comment.", "danger");
        });
}

function parseBoolean(value) {
    if (value === "true") return true;
    if (value === "false") return false;
    return null;
}

function parseNumber(value) {
    if (value === null || value === undefined || value === "") return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
}

function parseIntOrNull(value) {
    if (value === null || value === undefined || value === "") return null;
    const parsed = parseInt(value, 10);
    return Number.isNaN(parsed) ? null : parsed;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

window.setDateFilter = setDateFilter;
