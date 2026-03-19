/* =========================
   HOME JS
   - Form display
   - Student selection
========================= */

let selectedUserId = null;
let selectedAthleteDisplayName = "";
const currentUserRole = (window.currentUserRole || "athlete").toLowerCase();
const currentUserId = window.currentUserId || null;

/* =========================
   FORM DISPLAY
========================= */

/**
 * Shows one specific athlete form and hides all other main content.
 * Coaches do not use form mode.
 * @param {"running"|"workout"} type form type to display
 */
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

/**
 * Shows only the running log form on initial page load.
 * The workout form remains hidden until explicitly requested via the header button.
 * Coaches do not use form mode.
 */
function showDefaultForm() {
    if (currentUserRole === "coach") return;
    showForm("running");
}

/* =========================
   STUDENT SELECTION
========================= */

/**
 * Selects an athlete for coach view and opens that athlete's running sheet.
 * @param {string} userId selected athlete id
 */
function selectStudent(userId, displayName) {
    if (currentUserRole !== "coach") return;
    selectedUserId = userId;
    selectedAthleteDisplayName = displayName || "Selected athlete";
    updateRunningSheetHeaderLabel();
    hideMainContent();

    // By default, show running sheet when a student is selected
    showRunningSheet(userId);
}

/**
 * Clears selected athlete and returns coach UI to empty state.
 */
function clearStudentSelection() {
    if (currentUserRole !== "coach") return;
    selectedUserId = null;
    selectedAthleteDisplayName = "";
    updateRunningSheetHeaderLabel();
    hideMainContent();
    document.getElementById("emptyState").style.display = "block";
}

/**
 * Updates running-sheet subheader text with selected athlete context.
 */
function updateRunningSheetHeaderLabel() {
    const runningLabel = document.getElementById("runningSheetAthleteLabel");
    const workoutLabel = document.getElementById("workoutSheetAthleteLabel");
    const labels = [runningLabel, workoutLabel].filter(Boolean);
    if (labels.length === 0) return;

    if (currentUserRole === "coach") {
        const text = selectedAthleteDisplayName
            ? `- ${selectedAthleteDisplayName}`
            : "- No athlete selected";
        labels.forEach((label) => {
            label.textContent = text;
        });
        return;
    }

    const text = currentUserId ? "- My Logs" : "";
    labels.forEach((label) => {
        label.textContent = text;
    });
}

/* =========================
   MAIN CONTENT TOGGLING
========================= */

/**
 * Hides all form/sheet/empty-state sections before showing one target view.
 */
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

/**
 * Displays a temporary alert banner for save/delete outcomes.
 * @param {string} message message to render
 * @param {"success"|"danger"|"warning"} [type="success"] bootstrap alert variant
 */
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

/**
 * Reads URL success params after post-redirect-get and shows corresponding banner.
 */
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

/**
 * Enables coach athlete filtering in the footer list.
 */
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
    // Bind coach-only search and render post-submit notices.
    setupAthleteSearch();
    showSubmissionNoticeFromUrl();

    // Athletes land on the running log form by default.
    // The workout form is shown only when explicitly requested via the header button.
    if (currentUserRole === "athlete") {
        showDefaultForm();
    }

    updateRunningSheetHeaderLabel();
});

window.updateRunningSheetHeaderLabel = updateRunningSheetHeaderLabel;