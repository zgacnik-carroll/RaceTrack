/* =========================
   SHEETS JS
   - Running & Workout Spreadsheet Logic
========================= */

const role = (window.currentUserRole || "athlete").toLowerCase();
const userId = window.currentUserId || "";
const TEXT_MAX = 2000;
const dateFilters = {
    running: "week",
    workout: "week"
};
const customDateFilterBindings = {
    running: false,
    workout: false
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
 * Renders the running "feel" textarea.
 * @param {string|null|undefined} value
 * @returns {string}
 */
function feelTextarea(value, id) {
    return `
      <textarea class="sheet-input sheet-textarea js-limited-text" maxlength="100" data-char-max="100" id="${id}">${escapeHtml(value ?? "")}</textarea>
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
 * Maps legacy "feel" values to cell background colors.
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
 * Shows/hides the running pain-details editor based on hurting selection.
 * @param {number} logId
 */
function syncRunningPainDetailsEditor(logId) {
    const hurtingValue = parseBoolean(document.getElementById(`running-hurting-${logId}`)?.value);
    const wrapper = document.getElementById(`running-pain-details-wrapper-${logId}`);
    const field = document.getElementById(`running-pain-details-${logId}`);
    if (!wrapper || !field) return;

    const show = hurtingValue === true;
    wrapper.style.display = show ? "" : "none";
    field.required = show;
    if (!show) {
        field.value = "";
        const counter = field.nextElementSibling;
        if (counter?.classList?.contains("char-counter")) {
            counter.textContent = "0/100 characters";
        }
    }
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
    const feelInput = document.getElementById(`running-feel-${logId}`);

    if (hurtingSelect) hurtingSelect.addEventListener("change", () => {
        updateRunningRowColors(logId);
        syncRunningPainDetailsEditor(logId);
    });
    if (sleepInput) sleepInput.addEventListener("input", () => updateRunningRowColors(logId));
    if (stressInput) stressInput.addEventListener("input", () => updateRunningRowColors(logId));
    if (plateSelect) plateSelect.addEventListener("change", () => updateRunningRowColors(logId));
    if (breadSelect) breadSelect.addEventListener("change", () => updateRunningRowColors(logId));
    if (feelInput) feelInput.addEventListener("input", () => updateRunningRowColors(logId));
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
 * Converts a date-like value to YYYY-MM-DD using the stored calendar date.
 * @param {string|Date|null|undefined} date
 * @returns {string}
 */
function dateKey(date) {
    return formatDateForInput(date);
}

/**
 * Returns a YYYY-MM-DD string shifted by a day offset.
 * @param {Date} date
 * @param {number} offsetDays
 * @returns {string}
 */
function shiftedDateKey(date, offsetDays = 0) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + offsetDays);
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const yyyy = String(d.getFullYear());
    return `${yyyy}-${mm}-${dd}`;
}

/**
 * Applies currently selected date filter to a log list.
 * @param {Array<any>} logs
 * @param {"running"|"workout"} type
 * @returns {Array<any>}
 */
function applyDateFilter(logs, type) {
    const range = dateFilters[type] ?? "week";
    const now = new Date();
    const todayKey = shiftedDateKey(now, 0);

    let filtered = logs;
    if (range === "today") {
        filtered = logs.filter(log => dateKey(log.logDate) === todayKey);
    } else if (range === "week") {
        const weekStartKey = shiftedDateKey(now, -6);
        filtered = logs.filter(log => {
            const logKey = dateKey(log.logDate);
            return logKey >= weekStartKey && logKey <= todayKey;
        });
    } else if (range === "month") {
        const monthStartKey = shiftedDateKey(now, -29);
        filtered = logs.filter(log => {
            const logKey = dateKey(log.logDate);
            return logKey >= monthStartKey && logKey <= todayKey;
        });
    } else if (range === "custom") {
        const customRange = getCustomDateRange(type);
        if (customRange.start && customRange.end) {
            const start = customRange.start <= customRange.end ? customRange.start : customRange.end;
            const end = customRange.start <= customRange.end ? customRange.end : customRange.start;
            filtered = logs.filter(log => {
                const logKey = dateKey(log.logDate);
                return logKey >= start && logKey <= end;
            });
        }
    }

    return filtered;
}

/**
 * Sets active date filter and refreshes corresponding sheet.
 * @param {"running"|"workout"} type
 * @param {"today"|"week"|"month"|"custom"} range
 */
function setDateFilter(type, range) {
    dateFilters[type] = range;
    syncCustomFilterVisibility(type);
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
    const ranges = ["today", "week", "month", "custom"];
    ranges.forEach(range => {
        const button = document.getElementById(`${type}-filter-${range}`);
        if (!button) return;
        button.classList.toggle("active", dateFilters[type] === range);
    });
}

/**
 * Reads the custom start/end date inputs for a sheet filter.
 * @param {"running"|"workout"} type
 * @returns {{start: string|null, end: string|null}}
 */
function getCustomDateRange(type) {
    const startValue = document.getElementById(`${type}-custom-start`)?.value ?? "";
    const endValue = document.getElementById(`${type}-custom-end`)?.value ?? "";
    return {
        start: startValue || null,
        end: endValue || null
    };
}

/**
 * Shows/hides the custom date input row for the active filter.
 * @param {"running"|"workout"} type
 */
function syncCustomFilterVisibility(type) {
    const customFilter = document.getElementById(`${type}-custom-filter-inputs`);
    if (!customFilter) return;
    customFilter.classList.toggle("d-none", dateFilters[type] !== "custom");
}

/**
 * Binds custom filter date inputs once so coaches can filter by inclusive range.
 * @param {"running"|"workout"} type
 */
function bindCustomDateFilterInputs(type) {
    if (customDateFilterBindings[type]) return;

    const startInput = document.getElementById(`${type}-custom-start`);
    const endInput = document.getElementById(`${type}-custom-end`);
    if (!startInput || !endInput) return;

    const refresh = () => {
        if (dateFilters[type] !== "custom") return;
        if (type === "running") {
            loadRunningLogs();
        } else {
            loadWorkoutLogs();
        }
    };

    startInput.addEventListener("change", refresh);
    endInput.addEventListener("change", refresh);
    bindDateInputPicker(`${type}-custom-start`);
    bindDateInputPicker(`${type}-custom-end`);
    customDateFilterBindings[type] = true;
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
 * Keeps athlete-only actions header and column sizing in sync with editability.
 * @param {"running"|"workout"} logType
 * @param {boolean} editable
 */
function syncSheetLayout(logType, editable) {
    const table = document.querySelector(`.${logType}-table`);
    const actionsHeader = document.getElementById(`${logType}-actions-header`);

    if (table) {
        table.classList.toggle("athlete-view", editable);
    }

    if (actionsHeader) {
        actionsHeader.style.display = editable ? "" : "none";
    }
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
          <textarea class="sheet-input sheet-textarea coach-comment-input js-limited-text" maxlength="${TEXT_MAX}" data-char-max="${TEXT_MAX}" id="${logType}-coach-comment-${logId}">${escapeHtml(comment ?? "")}</textarea>
          <div class="coach-comment-actions">
            <button type="button" class="btn btn-sm btn-primary coach-comment-save-btn d-none" id="${logType}-coach-save-${logId}" onclick="saveCoachComment('${logType}', ${logId})">Save</button>
            <span class="comment-save-indicator" id="${logType}-coach-indicator-${logId}" aria-live="polite"></span>
          </div>
        </div>
      </td>
    `;
}

/**
 * Renders workout-type select for editable workout rows.
 * @param {string|null|undefined} value
 * @param {string} elementId
 * @returns {string}
 */
function workoutTypeSelect(value, elementId) {
    const normalized = String(value ?? "");
    const options = ["Strength", "Strides", "Workout"];
    return `
      <select class="sheet-input" id="${elementId}">
        <option value=""></option>
        ${options.map(option => `<option value="${option}" ${normalized === option ? "selected" : ""}>${option}</option>`).join("")}
      </select>
    `;
}

/**
 * Binds row inputs so save button reflects unsaved edits.
 * @param {"running"|"workout"} logType
 * @param {number} logId
 * @param {string[]} fieldIds
 */
function bindRowDirtyState(logType, logId, fieldIds) {
    const saveButton = document.getElementById(`${logType}-save-${logId}`);
    if (!saveButton) return;

    saveButton.classList.add("d-none");
    saveButton.disabled = false;
    saveButton.textContent = "Save Changes";
    saveButton.classList.remove("btn-outline-success", "btn-warning");
    saveButton.classList.add("btn-primary");

    const markDirty = () => {
        if (!saveButton.classList.contains("d-none")) return;
        saveButton.textContent = "Save Changes";
        saveButton.classList.remove("btn-outline-success");
        saveButton.classList.add("btn-primary");
        saveButton.classList.remove("d-none");
    };

    fieldIds.forEach(fieldId => {
        const input = document.getElementById(fieldId);
        if (!input) return;
        input.addEventListener("input", markDirty);
        input.addEventListener("change", markDirty);
    });
}

/**
 * Shows coach comment save button only after comment text changes.
 * @param {"running"|"workout"} logType
 * @param {number} logId
 */
function bindCoachCommentDirtyState(logType, logId) {
    const field = document.getElementById(`${logType}-coach-comment-${logId}`);
    const saveButton = document.getElementById(`${logType}-coach-save-${logId}`);
    const indicator = document.getElementById(`${logType}-coach-indicator-${logId}`);
    if (!field || !saveButton) return;

    const initial = field.value;
    const sync = () => {
        const changed = field.value !== initial;
        saveButton.classList.toggle("d-none", !changed);
        if (indicator && !changed) {
            indicator.textContent = "";
        }
    };

    field.addEventListener("input", sync);
    field.addEventListener("change", sync);
    sync();
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
            syncSheetLayout("running", editable);
            const filteredData = applyDateFilter(data, "running");
            filteredData.forEach(log => {
                const row = document.createElement("tr");

                if (editable) {
                    row.innerHTML = `
                        <td class="log-date-cell"><input class="sheet-input" type="date" value="${formatDateForInput(log.logDate)}" id="running-date-${log.id}"></td>
                        <td>
                            <div class="d-flex flex-column gap-2">
                                <button class="btn btn-sm btn-primary d-none" id="running-save-${log.id}" onclick="saveRunningLog(${log.id})">Save Changes</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteRunningLog(${log.id})">Delete</button>
                            </div>
                        </td>
                        <td><input class="sheet-input" type="number" step="0.01" min="0" value="${log.mileage ?? ""}" id="running-mileage-${log.id}"></td>
                        <td id="running-hurting-cell-${log.id}" class="wellness-cell">
                            ${booleanSelect(log.hurting).replace('data-field="bool"', `id="running-hurting-${log.id}"`)}
                            <div id="running-pain-details-wrapper-${log.id}" class="mt-2" style="display:${log.hurting === true ? "block" : "none"};">
                                <textarea class="sheet-input sheet-textarea js-limited-text" maxlength="100" data-char-max="100" id="running-pain-details-${log.id}">${escapeHtml(log.painDetails ?? "")}</textarea>
                            </div>
                        </td>
                        <td id="running-sleep-cell-${log.id}" class="wellness-cell"><input class="sheet-input" type="number" min="0" max="24" value="${log.sleepHours ?? ""}" id="running-sleep-${log.id}"></td>
                        <td id="running-stress-cell-${log.id}" class="wellness-cell"><input class="sheet-input" type="number" min="1" max="10" value="${log.stressLevel ?? ""}" id="running-stress-${log.id}"></td>
                        <td id="running-plate-cell-${log.id}" class="wellness-cell">${booleanSelect(log.plateProportion).replace('data-field="bool"', `id="running-plate-${log.id}"`)}</td>
                        <td id="running-bread-cell-${log.id}" class="wellness-cell">${booleanSelect(log.gotThatBread).replace('data-field="bool"', `id="running-bread-${log.id}"`)}</td>
                        <td id="running-feel-cell-${log.id}" class="running-feel-cell">${feelTextarea(log.feel, `running-feel-${log.id}`)}</td>
                        <td class="running-rpe-column"><input class="sheet-input" type="number" min="1" max="10" value="${log.rpe ?? ""}" id="running-rpe-${log.id}"></td>
                        <td><textarea class="sheet-input sheet-textarea js-limited-text" maxlength="${TEXT_MAX}" data-char-max="${TEXT_MAX}" id="running-details-${log.id}">${escapeHtml(log.details ?? "")}</textarea></td>
                        <td><div class="expandable-cell">${escapeHtml(log.coachComment ?? "")}</div></td>
                    `;
                } else {
                    row.innerHTML = `
                        <td class="log-date-cell">${formatDate(log.logDate)}</td>
                        <td>${escapeHtml(log.mileage ?? "")}</td>
                        <td class="wellness-cell"><div>${booleanDisplay(log.hurting)}</div><div class="expandable-cell mt-2">${escapeHtml(log.painDetails ?? "")}</div></td>
                        <td class="wellness-cell"${styleAttrFromColor(sleepColor(log.sleepHours))}>${escapeHtml(log.sleepHours ?? "")}</td>
                        <td class="wellness-cell"${styleAttrFromColor(stressColor(log.stressLevel))}>${escapeHtml(log.stressLevel ?? "")}</td>
                        <td class="wellness-cell"${styleAttrFromColor(yesNoColor(log.plateProportion))}>${booleanDisplay(log.plateProportion)}</td>
                        <td class="wellness-cell"${styleAttrFromColor(yesNoColor(log.gotThatBread))}>${booleanDisplay(log.gotThatBread)}</td>
                        <td class="running-feel-cell"><div class="expandable-cell">${escapeHtml(log.feel ?? "")}</div></td>
                        <td class="running-rpe-column">${escapeHtml(log.rpe ?? "")}</td>
                        <td><div class="expandable-cell">${escapeHtml(log.details ?? "")}</div></td>
                        ${coachCommentCell(log.id, log.coachComment, "running")}
                    `;
                }

                tbody.appendChild(row);
                if (window.attachCharCounters) {
                    window.attachCharCounters(row);
                }

                if (editable) {
                    bindRowDirtyState("running", log.id, [
                        `running-date-${log.id}`,
                        `running-mileage-${log.id}`,
                        `running-hurting-${log.id}`,
                        `running-pain-details-${log.id}`,
                        `running-sleep-${log.id}`,
                        `running-stress-${log.id}`,
                        `running-plate-${log.id}`,
                        `running-bread-${log.id}`,
                        `running-feel-${log.id}`,
                        `running-rpe-${log.id}`,
                        `running-details-${log.id}`
                    ]);
                    bindRunningRowColorHandlers(log.id);
                    updateRunningRowColors(log.id);
                    syncRunningPainDetailsEditor(log.id);
                    bindDateInputPicker(`running-date-${log.id}`);
                } else if (role === "coach") {
                    bindCoachCommentDirtyState("running", log.id);
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
    if (role === "athlete" && (!userIdParam || userIdParam === "me")) {
        if (window.clearAthleteViewSelection) {
            window.clearAthleteViewSelection();
        }
    }
    dateFilters.running = "week";
    hideMainContent();
    document.getElementById("runningSpreadsheet").style.display = "block";
    bindCustomDateFilterInputs("running");
    syncCustomFilterVisibility("running");
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
    const saveButton = document.getElementById(`running-save-${logId}`);
    const payload = {
        mileage: parseNumber(document.getElementById(`running-mileage-${logId}`).value),
        hurting: parseBoolean(document.getElementById(`running-hurting-${logId}`).value),
        painDetails: document.getElementById(`running-pain-details-${logId}`)?.value ?? "",
        sleepHours: parseIntOrNull(document.getElementById(`running-sleep-${logId}`).value),
        stressLevel: parseIntOrNull(document.getElementById(`running-stress-${logId}`).value),
        plateProportion: parseBoolean(document.getElementById(`running-plate-${logId}`).value),
        gotThatBread: parseBoolean(document.getElementById(`running-bread-${logId}`).value),
        feel: document.getElementById(`running-feel-${logId}`).value,
        rpe: parseIntOrNull(document.getElementById(`running-rpe-${logId}`).value),
        details: document.getElementById(`running-details-${logId}`).value,
        logDate: document.getElementById(`running-date-${logId}`).value || null
    };
    const validationError = validateRunningPayload(payload);
    if (validationError) {
        if (window.showSaveNotice) window.showSaveNotice(validationError, "warning");
        return;
    }
    if (saveButton) {
        saveButton.classList.add("d-none");
    }

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
            if (saveButton) {
                saveButton.classList.remove("d-none");
            }
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
            syncSheetLayout("workout", editable);
            const filteredData = applyDateFilter(data, "workout");
            filteredData.forEach(log => {
                const row = document.createElement("tr");

                if (editable) {
                    row.innerHTML = `
                        <td class="log-date-cell"><input class="sheet-input" type="date" value="${formatDateForInput(log.logDate)}" id="workout-date-${log.id}"></td>
                        <td>
                            <div class="d-flex flex-column gap-2">
                                <button class="btn btn-sm btn-primary d-none" id="workout-save-${log.id}" onclick="saveWorkoutLog(${log.id})">Save Changes</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteWorkoutLog(${log.id})">Delete</button>
                            </div>
                        </td>
                        <td>${workoutTypeSelect(log.workoutType, `workout-type-${log.id}`)}</td>
                        <td><textarea class="sheet-input sheet-textarea js-limited-text" maxlength="${TEXT_MAX}" data-char-max="${TEXT_MAX}" id="workout-completion-${log.id}">${escapeHtml(log.completionDetails ?? "")}</textarea></td>
                        <td><textarea class="sheet-input sheet-textarea js-limited-text" maxlength="${TEXT_MAX}" data-char-max="${TEXT_MAX}" id="workout-paces-${log.id}">${escapeHtml(log.actualPaces ?? "")}</textarea></td>
                        <td><textarea class="sheet-input sheet-textarea js-limited-text" maxlength="${TEXT_MAX}" data-char-max="${TEXT_MAX}" id="workout-description-${log.id}">${escapeHtml(log.workoutDescription ?? "")}</textarea></td>
                        <td><div class="expandable-cell">${escapeHtml(log.coachComment ?? "")}</div></td>
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
                if (window.attachCharCounters) {
                    window.attachCharCounters(row);
                }

                if (editable) {
                    bindRowDirtyState("workout", log.id, [
                        `workout-date-${log.id}`,
                        `workout-type-${log.id}`,
                        `workout-completion-${log.id}`,
                        `workout-paces-${log.id}`,
                        `workout-description-${log.id}`
                    ]);
                    bindDateInputPicker(`workout-date-${log.id}`);
                } else if (role === "coach") {
                    bindCoachCommentDirtyState("workout", log.id);
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
    if (role === "athlete" && (!userIdParam || userIdParam === "me")) {
        if (window.clearAthleteViewSelection) {
            window.clearAthleteViewSelection();
        }
    }
    hideMainContent();
    document.getElementById("workoutSpreadsheet").style.display = "block";
    bindCustomDateFilterInputs("workout");
    syncCustomFilterVisibility("workout");
    if (window.updateRunningSheetHeaderLabel) {
        window.updateRunningSheetHeaderLabel();
    }
    loadWorkoutLogs(userIdParam);
}

/**
 * Sends athlete edits for one workout row to the API.
 * @param {number} logId
 */
function saveWorkoutLog(logId) {
    const saveButton = document.getElementById(`workout-save-${logId}`);
    const payload = {
        workoutType: document.getElementById(`workout-type-${logId}`).value,
        completionDetails: document.getElementById(`workout-completion-${logId}`).value,
        actualPaces: document.getElementById(`workout-paces-${logId}`).value,
        workoutDescription: document.getElementById(`workout-description-${logId}`).value,
        logDate: document.getElementById(`workout-date-${logId}`).value || null
    };
    const validationError = validateWorkoutPayload(payload);
    if (validationError) {
        if (window.showSaveNotice) window.showSaveNotice(validationError, "warning");
        return;
    }
    if (saveButton) {
        saveButton.classList.add("d-none");
    }

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
            if (saveButton) {
                saveButton.classList.remove("d-none");
            }
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
    if (value && value.length > TEXT_MAX) {
        if (window.showSaveNotice) window.showSaveNotice(`Comments are limited to ${TEXT_MAX} characters.`, "warning");
        return;
    }
    const saveButton = document.getElementById(saveButtonId);
    const indicator = document.getElementById(indicatorId);

    if (saveButton) {
        saveButton.disabled = true;
        saveButton.textContent = "Save";
        saveButton.classList.add("btn-primary");
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
                saveButton.textContent = "Save";
                saveButton.classList.add("d-none");
            }
            if (indicator) {
                indicator.textContent = "";
            }
            if (window.showSaveNotice) window.showSaveNotice("Coach comment saved.", "success");
        })
        .catch(error => {
            console.error(error);
            if (saveButton) {
                saveButton.disabled = false;
                saveButton.textContent = "Save";
                saveButton.classList.remove("d-none");
            }
            if (indicator) {
                indicator.textContent = "";
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
 * Returns error text when running payload is invalid; null when valid.
 * @param {Object} payload
 * @returns {string|null}
 */
function validateRunningPayload(payload) {
    if (payload.mileage === null || payload.mileage < 0) {
        return "Mileage must be 0 or greater.";
    }
    if (payload.sleepHours !== null && (payload.sleepHours < 0 || payload.sleepHours > 24)) {
        return "Sleep must be between 0 and 24 hours.";
    }
    if (payload.hurting === true && (!payload.painDetails || !payload.painDetails.trim())) {
        return "Hurting details are required when hurting is Yes.";
    }
    if (payload.painDetails && payload.painDetails.length > 100) {
        return "Hurting details are limited to 100 characters.";
    }
    if (payload.stressLevel !== null && (payload.stressLevel < 1 || payload.stressLevel > 10)) {
        return "Stress must be between 1 and 10.";
    }
    if (payload.rpe !== null && (payload.rpe < 1 || payload.rpe > 10)) {
        return "RPE must be between 1 and 10.";
    }
    if (!payload.feel || !payload.feel.trim()) {
        return "Feel is required.";
    }
    if (payload.feel.length > 100) {
        return "Feel is limited to 100 characters.";
    }
    if (!payload.details || !payload.details.trim()) {
        return "Details are required.";
    }
    if (payload.details.length > TEXT_MAX) {
        return `Details are limited to ${TEXT_MAX} characters.`;
    }
    return null;
}

/**
 * Returns error text when workout payload is invalid; null when valid.
 * @param {Object} payload
 * @returns {string|null}
 */
function validateWorkoutPayload(payload) {
    const validWorkoutTypes = ["Strength", "Strides", "Workout"];
    if (!validWorkoutTypes.includes(payload.workoutType)) {
        return "Workout type must be Strength, Strides, or Workout.";
    }
    if (payload.completionDetails && payload.completionDetails.length > TEXT_MAX) {
        return `Completion details are limited to ${TEXT_MAX} characters.`;
    }
    if (payload.actualPaces && payload.actualPaces.length > TEXT_MAX) {
        return `Actual paces are limited to ${TEXT_MAX} characters.`;
    }
    if (payload.workoutDescription && payload.workoutDescription.length > TEXT_MAX) {
        return `Workout description is limited to ${TEXT_MAX} characters.`;
    }
    return null;
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
