/* =========================
   HOME JS
   - Form display
   - Student selection
========================= */

let selectedUserId = null;
const currentUserRole = (window.currentUserRole || "athlete").toLowerCase();
const currentUserId = window.currentUserId || null;

/* =========================
   FORM DISPLAY
========================= */

function showForm(type) {
    if (currentUserRole === "coach") return;
    hideMainContent();

    if (type === "running") {
        document.getElementById("runningForm").style.display = "block";
    }

    if (type === "workout") {
        document.getElementById("workoutForm").style.display = "block";
    }
}

function showAllForms() {
    if (currentUserRole === "coach") return;
    hideMainContent();

    const runningForm = document.getElementById("runningForm");
    const workoutForm = document.getElementById("workoutForm");
    if (runningForm) runningForm.style.display = "block";
    if (workoutForm) workoutForm.style.display = "block";
}

/* =========================
   STUDENT SELECTION
========================= */

function selectStudent(userId) {
    if (currentUserRole !== "coach") return;
    selectedUserId = userId;
    hideMainContent();

    // By default, show running sheet when a student is selected
    showRunningSheet(userId);
}

function clearStudentSelection() {
    if (currentUserRole !== "coach") return;
    selectedUserId = null;
    hideMainContent();
    document.getElementById("emptyState").style.display = "block";
}

/* =========================
   MAIN CONTENT TOGGLING
========================= */

function hideMainContent() {
    // Hide forms
    const runningForm = document.getElementById("runningForm");
    const workoutForm = document.getElementById("workoutForm");
    if (runningForm) runningForm.style.display = "none";
    if (workoutForm) workoutForm.style.display = "none";

    // Hide sheets
    const runningSheet = document.getElementById("runningSpreadsheet");
    const workoutSheet = document.getElementById("workoutSpreadsheet");
    if (runningSheet) runningSheet.style.display = "none";
    if (workoutSheet) workoutSheet.style.display = "none";

    // Hide empty state
    const emptyState = document.getElementById("emptyState");
    if (emptyState) emptyState.style.display = "none";
}

function showSaveNotice(message, type = "success") {
    const notice = document.getElementById("saveNotice");
    if (!notice) return;

    notice.className = `alert alert-${type} mb-3`;
    notice.textContent = message;

    window.clearTimeout(window.__saveNoticeTimeout);
    window.__saveNoticeTimeout = window.setTimeout(() => {
        notice.className = "alert d-none mb-3";
        notice.textContent = "";
    }, 3500);
}

window.showSaveNotice = showSaveNotice;

function showSubmissionNoticeFromUrl() {
    const params = new URLSearchParams(window.location.search);
    let shown = false;

    if (params.has("runningSuccess")) {
        showSaveNotice("Running log saved.", "success");
        shown = true;
    }

    if (params.has("workoutSuccess")) {
        showSaveNotice("Workout log saved.", "success");
        shown = true;
    }

    if (shown) {
        const cleanUrl = window.location.pathname;
        window.history.replaceState({}, "", cleanUrl);
    }
}

function setupAthleteSearch() {
    if (currentUserRole !== "coach") return;

    const search = document.getElementById("athleteSearch");
    const athleteButtons = Array.from(document.querySelectorAll(".athlete-list button"));
    if (!search || athleteButtons.length === 0) return;

    search.addEventListener("input", (event) => {
        const query = event.target.value.trim().toLowerCase();
        athleteButtons.forEach((button) => {
            const name = (button.dataset.name || "").toLowerCase();
            const email = (button.dataset.email || "").toLowerCase();
            const visible = !query || name.includes(query) || email.includes(query);
            button.style.display = visible ? "" : "none";
        });
    });
}

document.addEventListener("DOMContentLoaded", () => {
    setupAthleteSearch();
    showSubmissionNoticeFromUrl();

    if (currentUserRole === "athlete") {
        showAllForms();
    }
});
