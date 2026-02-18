/* =========================
   HOME JS
   - Form display
   - Student selection
========================= */

let selectedUserId = null;

/* =========================
   FORM DISPLAY
========================= */

function showForm(type) {
    hideMainContent();

    if (type === "running") {
        document.getElementById("runningForm").style.display = "block";
    }

    if (type === "workout") {
        document.getElementById("workoutForm").style.display = "block";
    }
}

/* =========================
   STUDENT SELECTION
========================= */

function selectStudent(userId) {
    selectedUserId = userId;
    hideMainContent();

    // By default, show running sheet when a student is selected
    showRunningSheet(userId);
}

function clearStudentSelection() {
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
