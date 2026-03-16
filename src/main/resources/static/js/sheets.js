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

/**
 * Builds JSON request headers and injects CSRF values when present.
 * @returns {Record<string, string>}
 */
function jsonHeaders() {
    const headers = { "Content-Type": "application/json" };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

/**
 * Formats a backend date into MM-DD-YY display format.
 * @param {string|null|undefined} dateString
 * @returns {string}
 */
function formatDate(dateString) {
    if (!dateString) return "";
    const d = new Date(dateString);
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const yy = String(d.getFullYear()).slice(-2);
    return `${mm}-${dd}-${yy}`;
}

/**
 * Converts backend date values into YYYY-MM-DD for `<input type="date">`.
 * @param {string|null|undefined} dateString
 * @returns {string}
 */
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

/**
 * Binds focus/click handlers to open the native date picker when supported.
 * @param {string} inputId
 */
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

/**
 * Displays boolean values in read-only cells.
 * @param {boolean|null|undefined} value
 * @returns {string}
 */
function booleanDisplay(value) {
    if (value === true) return "Yes";
    if (value === false) return "No";
    return "";
}

/**
 * Renders a yes/no select for editable boolean cells.
 * @param {boolean|null|undefined} value
 * @returns {string}
 */
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

/**
 * Renders the running "feel" select.
 * @param {string|null|undefined} value
 * @returns {string}
 */
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

/**
 * Clamps numeric values into [0,1].
 * @param {number} value
 * @returns {number}
 */
function clamp01(value) {
    return Math.max(0, Math.min(1, value));
}

/**
 * Produces a green-to-red background tone from normalized values.
 * @param {number} score01
 * @returns {string}
 */
function gradientColor01(score01) {
    const safe = clamp01(score01);
    const hue = 120 * safe;
    return `hsl(${hue}, 70%, 86%)`;
}

/**
 * Computes sleep-cell color.
 * @param {number|null|undefined} sleepHours
 * @returns {string}
 */
function sleepColor(sleepHours) {
    if (sleepHours === null || sleepHours === undefined) return "";
    const n = Number(sleepHours);
    if (Number.isNaN(n)) return "";
    return gradientColor01(n / 10);
}

/**
 * Computes stress-cell color (inverted so lower stress is greener).
 * @param {number|null|undefined} stressLevel
 * @returns {string}
 */
function stressColor(stressLevel) {
    if (stressLevel === null || stressLevel === undefined) return "";
    const n = Number(stressLevel);
    if (Number.isNaN(n)) return "";
    const normalized = 1 - ((n - 1) / 9);
    return gradientColor01(normalized);
}

/**
 * Maps yes/no values to cell background colors.
 * @param {boolean|null|undefined} value
 * @returns {string}
 */
function yesNoColor(value) {
    if (value === true) return "hsl(120, 55%, 86%)";
    if (value === false) return "hsl(0, 65%, 87%)";
    return "";
}

/**
 * Maps "feel" values to cell background colors.
 * @param {string|null|undefined} value
 * @returns {string}
 */
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

/**
 * Applies a background color to a row cell if it exists.
 * @param {string} cellId
 * @param {string} color
 */
function setWellnessCellColor(cellId, color) {
    const cell = document.getElementById(cellId);
    if (!cell) return;
    cell.style.backgroundColor = color || "";
}

/**
 * Generates inline style snippets for read-only table rows.
 * @param {string} color
 * @returns {string}
 */
function styleAttrFromColor(color) {
    if (!color) return "";
    return ` style="background-color:${color}"`;
}

/**
 * Recomputes wellness colors for a specific running-log row.
 * @param {number} logId
 */
function updateRunningRowColors(logId) {
    const sleepValue = parseIntOrNull(document.getElementById(`running-sleep-${logId}`)?.value);
    const stressValue = parseIntOrNull(document.getElementById(`running-stress-${logId}`)?.value);
    const hurtingValue = parseBoolean(document.getElementById(`running-hurting-${logId}`)?.value);
    const plateValue = parseBoolean(document.getElementById(`running-plate-${logId}`)?.value);
    const breadValue = parseBoolean(document.getElementById(`running-bread-${logId}`)?.value);
    const feelValue = document.getElementById(`running-feel-${logId}`)?.value ?? "";


    setWellnessCellColor(`running-sleep-cell-${logId}`, sleepColor(sleepValue));
    setWellnessCellColor(`running-stress-cell-${logId}`, stressColor(stressValue));
    setWellnessCellColor(`running-plate-cell-${logId}`, yesNoColor(plateValue));
    setWellnessCellColor(`running-bread-cell-${logId}`, yesNoColor(breadValue));
    setWellnessCellColor(`running-feel-cell-${logId}`, feelColor(feelValue));
}

/**
 * Binds row inputs so color state updates as athlete edits values.
 * @param {number} logId
 */
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

/**
 * Resolves active target user from explicit value, selected coach user, or self.
 * @param {string|null|undefined} inputUserId
 * @returns {string}
 */
function getTargetUserId(inputUserId) {
    return inputUserId ?? selectedUserId ?? "me";
}

/**
 * Normalizes a date to midnight local time for range comparisons.
 * @param {string|Date} date
 * @returns {Date}
 */
function startOfDay(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
}

/**
 * Applies currently selected date filter to a log list.
 * @param {Array<any>} logs
 * @param {"running"|"workout"} type
 * @returns {Array<any>}
 */
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

/**
 * Sets active date filter and refreshes corresponding sheet.
 * @param {"running"|"workout"} type
 * @param {"default"|"today"|"week"|"month"} range
 */
function setDateFilter(type, range) {
    dateFilters[type] = range;
    updateFilterButtons(type);
    if (type === "running") {
        loadRunningLogs();
    } else {
        loadWorkoutLogs();
    }
}

/**
 * Updates active/inactive state of date filter buttons.
 * @param {"running"|"workout"} type
 */
function updateFilterButtons(type) {
    const ranges = ["default", "today", "week", "month"];
    ranges.forEach(range => {
        const button = document.getElementById(`${type}-filter-${range}`);
        if (!button) return;
        button.classList.toggle("active", dateFilters[type] === range);
    });
}

/**
 * Athletes can only edit their own rows.
 * @param {string} targetUserId
 * @returns {boolean}
 */
function canAthleteEdit(targetUserId) {
    if (role !== "athlete") return false;
    return targetUserId === "me" || targetUserId === userId;
}

/**
 * Renders coach-comment cell as textarea+button for coaches, plain text otherwise.
 * @param {number} logId
 * @param {string|null|undefined} comment
 * @param {"running"|"workout"} logType
 * @returns {string}
 */
function coachCommentCell(logId, comment, logType) {
    if (role !== "coach") {
        return `<td><div class="expandable-cell">${escapeHtml(comment ?? "")}</div></td>`;
    }

    return `
      <td>
        <div class="coach-comment-editor">
          <textarea class="sheet-input sheet-textarea coach-comment-input" id="${logType}-coach-comment-${logId}">${escapeHtml(comment ?? "")}</textarea>
          <div class="coach-comment-actions">
            <button type="button" class="btn btn-sm btn-outline-primary coach-comment-save-btn" id="${logType}-coach-save-${logId}" onclick="saveCoachComment('${logType}', ${logId})">Save</button>
            <span class="comment-save-indicator" id="${logType}-coach-indicator-${logId}" aria-live="polite"></span>
          </div>
        </div>
      </td>
    `;
}

/* =========================
   RUNNING LOGS
========================= */

/**
 * Loads running logs for the selected/active user and renders table rows.
 * @param {string|null} [requestedUserId]
 */
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
                        <td>
                            <div class="d-flex flex-column gap-2">
                                <button class="btn btn-sm btn-primary" onclick="saveRunningLog(${log.id})">Save</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteRunningLog(${log.id})">Delete</button>
                            </div>
                        </td>
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
                        <td><div class="expandable-cell">${escapeHtml(log.details ?? "")}</div></td>
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

/**
 * Shows running sheet panel and loads data.
 * @param {string|null} [userIdParam]
 */
function showRunningSheet(userIdParam = null) {
    hideMainContent();
    document.getElementById("runningSpreadsheet").style.display = "block";
    if (window.updateRunningSheetHeaderLabel) {
        window.updateRunningSheetHeaderLabel();
    }
    loadRunningLogs(userIdParam);
}

/**
 * Sends athlete edits for one running row to the API.
 * @param {number} logId
 */
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

/**
 * Loads workout logs for the selected/active user and renders table rows.
 * @param {string|null} [requestedUserId]
 */
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
                        <td>
                            <div class="d-flex flex-column gap-2">
                                <button class="btn btn-sm btn-primary" onclick="saveWorkoutLog(${log.id})">Save</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteWorkoutLog(${log.id})">Delete</button>
                            </div>
                        </td>
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

/**
 * Shows workout sheet panel and loads data.
 * @param {string|null} [userIdParam]
 */
function showWorkoutSheet(userIdParam = null) {
    hideMainContent();
    document.getElementById("workoutSpreadsheet").style.display = "block";
    loadWorkoutLogs(userIdParam);
}

/**
 * Sends athlete edits for one workout row to the API.
 * @param {number} logId
 */
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

/**
 * Deletes one running row after explicit browser confirmation.
 * @param {number} logId
 */
function deleteRunningLog(logId) {
    const confirmed = window.confirm("Permanently delete this running row?");
    if (!confirmed) {
        return;
    }

    fetch(`/api/running-logs/${logId}`, {
        method: "DELETE",
        headers: jsonHeaders()
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to delete running row (${res.status})`);
            loadRunningLogs("me");
            if (window.showSaveNotice) window.showSaveNotice("Running row deleted.", "success");
        })
        .catch(error => {
            console.error(error);
            if (window.showSaveNotice) window.showSaveNotice("Could not delete running row.", "danger");
        });
}

/**
 * Deletes one workout row after explicit browser confirmation.
 * @param {number} logId
 */
function deleteWorkoutLog(logId) {
    const confirmed = window.confirm("Permanently delete this workout row?");
    if (!confirmed) {
        return;
    }

    fetch(`/api/workout-logs/${logId}`, {
        method: "DELETE",
        headers: jsonHeaders()
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to delete workout row (${res.status})`);
            loadWorkoutLogs("me");
            if (window.showSaveNotice) window.showSaveNotice("Workout row deleted.", "success");
        })
        .catch(error => {
            console.error(error);
            if (window.showSaveNotice) window.showSaveNotice("Could not delete workout row.", "danger");
        });
}

/* =========================
   COACH COMMENTS
========================= */

/**
 * Persists coach comment for the specified row.
 * @param {"running"|"workout"} logType
 * @param {number} logId
 */
function saveCoachComment(logType, logId) {
    const fieldId = `${logType}-coach-comment-${logId}`;
    const saveButtonId = `${logType}-coach-save-${logId}`;
    const indicatorId = `${logType}-coach-indicator-${logId}`;
    const value = document.getElementById(fieldId).value;
    const saveButton = document.getElementById(saveButtonId);
    const indicator = document.getElementById(indicatorId);

    if (saveButton) {
        saveButton.disabled = true;
        saveButton.textContent = "Saving...";
        saveButton.classList.remove("btn-outline-success", "btn-outline-danger");
        saveButton.classList.add("btn-outline-primary");
    }
    if (indicator) {
        indicator.textContent = "";
    }

    fetch(`/api/${logType}-logs/${logId}/coach-comment`, {
        method: "PUT",
        headers: jsonHeaders(),
        body: JSON.stringify({ coachComment: value })
    })
        .then(res => {
            if (!res.ok) throw new Error(`Failed to save coach comment (${res.status})`);
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.textContent = "Saved";
                saveButton.classList.remove("btn-outline-primary", "btn-outline-danger");
                saveButton.classList.add("btn-outline-success");
            }
            if (indicator) {
                indicator.textContent = "Saved";
            }
            window.setTimeout(() => {
                if (saveButton) {
                    saveButton.textContent = "Save";
                    saveButton.classList.remove("btn-outline-success", "btn-outline-danger");
                    saveButton.classList.add("btn-outline-primary");
                }
                if (indicator) {
                    indicator.textContent = "";
                }
            }, 2000);
            if (window.showSaveNotice) window.showSaveNotice("Coach comment saved.", "success");
        })
        .catch(error => {
            console.error(error);
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.textContent = "Retry";
                saveButton.classList.remove("btn-outline-primary", "btn-outline-success");
                saveButton.classList.add("btn-outline-danger");
            }
            if (indicator) {
                indicator.textContent = "Save failed";
            }
            if (window.showSaveNotice) window.showSaveNotice("Could not save coach comment.", "danger");
        });
}

/**
 * Parses tri-state boolean input values from select fields.
 * @param {string} value
 * @returns {boolean|null}
 */
function parseBoolean(value) {
    if (value === "true") return true;
    if (value === "false") return false;
    return null;
}

/**
 * Parses numeric text into Number or null when blank/invalid.
 * @param {string} value
 * @returns {number|null}
 */
function parseNumber(value) {
    if (value === null || value === undefined || value === "") return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
}

/**
 * Parses integer text into Number or null when blank/invalid.
 * @param {string} value
 * @returns {number|null}
 */
function parseIntOrNull(value) {
    if (value === null || value === undefined || value === "") return null;
    const parsed = parseInt(value, 10);
    return Number.isNaN(parsed) ? null : parsed;
}

/**
 * Escapes text before rendering into HTML templates.
 * @param {any} value
 * @returns {string}
 */
function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

window.setDateFilter = setDateFilter;
