/* =========================
   SHEETS JS
   - Running & Workout Spreadsheet Logic
========================= */

/* =========================
   HELPERS
========================= */

function formatDate(dateString) {
    if (!dateString) return "";
    const d = new Date(dateString);
    return `${d.getMonth() + 1}-${d.getDate()}-${d.getFullYear()}`;
}

function booleanDisplay(value) {
    if (value === true) return "Yes";
    if (value === false) return "No";
    return "";
}

/* =========================
   RUNNING LOGS
========================= */

function loadRunningLogs(userId) {
    fetch(`/api/running-logs/${userId}`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById("runningSpreadsheetBody");
            tbody.innerHTML = "";

            data.forEach(log => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${formatDate(log.logDate)}</td>
                    <td>${log.mileage ?? ""}</td>
                    <td>${booleanDisplay(log.hurting)}</td>
                    <td>${log.sleepHours ?? ""}</td>
                    <td>${booleanDisplay(log.plateProportion)}</td>
                    <td>${booleanDisplay(log.gotThatBread)}</td>
                    <td>${log.feel ?? ""}</td>
                    <td>${log.rpe ?? ""}</td>
                    <td class="expandable-cell">${log.details ?? ""}</td>
                `;
                tbody.appendChild(row);
            });
        });
}

function showRunningSheet(userId = null) {
    hideMainContent();
    document.getElementById("runningSpreadsheet").style.display = "block";

    const idToLoad = userId ?? selectedUserId ?? "me";
    loadRunningLogs(idToLoad);
}

/* =========================
   WORKOUT LOGS
========================= */

function loadWorkoutLogs(userId) {
    fetch(`/api/workout-logs/${userId}`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById("workoutSpreadsheetBody");
            tbody.innerHTML = "";

            data.forEach(log => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${formatDate(log.logDate)}</td>
                    <td>${log.workoutType ?? ""}</td>
                    <td><div class="workout-expandable">${log.completionDetails ?? ""}</div></td>
                    <td><div class="workout-expandable">${log.actualPaces ?? ""}</div></td>
                    <td><div class="workout-expandable">${log.workoutDescription ?? ""}</div></td>
                `;
                tbody.appendChild(row);
            });
        });
}

function showWorkoutSheet(userId = null) {
    hideMainContent();
    document.getElementById("workoutSpreadsheet").style.display = "block";

    const idToLoad = userId ?? selectedUserId ?? "me";
    loadWorkoutLogs(idToLoad);
}
