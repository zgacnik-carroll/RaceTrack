/* =========================
   HOME JS
   - Form display
   - Student selection
========================= */

let selectedUserId = null;
let selectedAthleteDisplayName = "";
let selectedAthleteEmail = "";
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
 * @param {HTMLButtonElement} button selected athlete button
 */
function selectStudent(button) {
    if (!button?.dataset) return;

    selectedUserId = button.dataset.userId || null;
    selectedAthleteDisplayName = button.dataset.displayName || "Selected athlete";
    selectedAthleteEmail = button.dataset.email || "";
    updateRunningSheetHeaderLabel();
    hideMainContent();

    // By default, show running sheet when a student is selected
    showRunningSheet(selectedUserId);
}

/**
 * Clears selected athlete and returns coach UI to empty state.
 */
function clearStudentSelection() {
    selectedUserId = null;
    selectedAthleteDisplayName = "";
    selectedAthleteEmail = "";
    updateRunningSheetHeaderLabel();

    if (currentUserRole === "athlete") {
        showDefaultForm();
        return;
    }

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

    const text = selectedAthleteDisplayName
        ? `- ${selectedAthleteDisplayName}`
        : currentUserId ? "- My Logs" : "";
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

function storeReloadNotice(message, type = "success") {
    try {
        window.sessionStorage.setItem("racetrackReloadNotice", JSON.stringify({ message, type }));
    } catch (_error) {
        // Ignore storage failures and continue without a persisted notice.
    }
}

function showStoredNotice() {
    try {
        const raw = window.sessionStorage.getItem("racetrackReloadNotice");
        if (!raw) return;
        window.sessionStorage.removeItem("racetrackReloadNotice");
        const parsed = JSON.parse(raw);
        if (parsed?.message) {
            showSaveNotice(parsed.message, parsed.type || "success");
        }
    } catch (_error) {
        // Ignore storage/parsing failures and continue normally.
    }
}

async function readErrorMessage(response, fallbackMessage) {
    try {
        const text = await response.text();
        if (!text) {
            return fallbackMessage;
        }

        try {
            const body = JSON.parse(text);
            if (body?.message) {
                return body.message;
            }
            if (body?.error) {
                return body.error;
            }
        } catch (_error) {
            // Non-JSON response, fall through to plain text.
        }

        return text;
    } catch (_error) {
        return fallbackMessage;
    }
}

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

function setupCoachAdminActions() {
    if (currentUserRole !== "coach") return;

    const createAthleteForm = document.getElementById("createAthleteForm");
    const submitButton = document.getElementById("createAthleteSubmitButton");
    const editAthleteForm = document.getElementById("editAthleteForm");
    const editSubmitButton = document.getElementById("editAthleteSubmitButton");
    if (!createAthleteForm || !submitButton || !editAthleteForm || !editSubmitButton) return;

    createAthleteForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const formData = new FormData(createAthleteForm);
        const payload = {
            firstName: String(formData.get("firstName") || "").trim(),
            lastName: String(formData.get("lastName") || "").trim(),
            email: String(formData.get("email") || "").trim(),
            temporaryPassword: String(formData.get("temporaryPassword") || "").trim()
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            showSaveNotice("First name, last name, and email are required.", "warning");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "Creating...";

        try {
            const response = await fetch("/api/admin/athletes", {
                method: "POST",
                headers: jsonHeaders(),
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await readErrorMessage(response, `Failed to create athlete (${response.status})`));
            }

            const modalElement = document.getElementById("createAthleteModal");
            const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;
            if (modal) {
                modal.hide();
            }

            createAthleteForm.reset();
            storeReloadNotice("Athlete created in Okta and added to RaceTrack.", "success");
            window.location.reload();
        } catch (error) {
            console.error(error);
            showSaveNotice(error.message || "Could not create athlete.", "danger");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Create Athlete";
        }
    });

    editAthleteForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (!selectedUserId) {
            showSaveNotice("Select an athlete before editing.", "warning");
            return;
        }

        const formData = new FormData(editAthleteForm);
        const payload = {
            firstName: String(formData.get("firstName") || "").trim(),
            lastName: String(formData.get("lastName") || "").trim(),
            email: String(formData.get("email") || "").trim(),
            temporaryPassword: String(formData.get("temporaryPassword") || "").trim()
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            showSaveNotice("First name, last name, and email are required.", "warning");
            return;
        }

        editSubmitButton.disabled = true;
        editSubmitButton.textContent = "Saving...";

        try {
            const response = await fetch(`/api/admin/athletes/${encodeURIComponent(selectedUserId)}`, {
                method: "PUT",
                headers: jsonHeaders(),
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await readErrorMessage(response, `Failed to edit athlete (${response.status})`));
            }

            const modalElement = document.getElementById("editAthleteModal");
            const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;
            if (modal) {
                modal.hide();
            }

            editAthleteForm.reset();
            storeReloadNotice("Athlete updated in RaceTrack and Okta.", "success");
            window.location.reload();
        } catch (error) {
            console.error(error);
            showSaveNotice(error.message || "Could not update athlete.", "danger");
        } finally {
            editSubmitButton.disabled = false;
            editSubmitButton.textContent = "Save Changes";
        }
    });
}

function splitSelectedAthleteName() {
    const trimmed = (selectedAthleteDisplayName || "").trim();
    if (!trimmed) {
        return { firstName: "", lastName: "" };
    }

    const parts = trimmed.split(/\s+/);
    if (parts.length === 1) {
        return { firstName: parts[0], lastName: "" };
    }

    return {
        firstName: parts[0],
        lastName: parts.slice(1).join(" ")
    };
}

function openEditAthleteModal() {
    if (currentUserRole !== "coach") return;
    if (!selectedUserId) {
        showSaveNotice("Select an athlete before editing.", "warning");
        return;
    }

    const name = splitSelectedAthleteName();
    const firstNameInput = document.getElementById("editAthleteFirstName");
    const lastNameInput = document.getElementById("editAthleteLastName");
    const emailInput = document.getElementById("editAthleteEmail");
    const passwordInput = document.getElementById("editAthleteTemporaryPassword");

    if (firstNameInput) firstNameInput.value = name.firstName;
    if (lastNameInput) lastNameInput.value = name.lastName;
    if (emailInput) emailInput.value = selectedAthleteEmail;
    if (passwordInput) passwordInput.value = "";

    const modalElement = document.getElementById("editAthleteModal");
    if (!modalElement) return;
    bootstrap.Modal.getOrCreateInstance(modalElement).show();
}

async function clearData() {
    if (currentUserRole !== "coach") return;

    const confirmed = window.confirm(
        "Clear all running logs and workout logs for every user? User accounts will be preserved."
    );
    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch("/api/admin/data", {
            method: "DELETE",
            headers: jsonHeaders()
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response, `Failed to clear data (${response.status})`));
        }

        storeReloadNotice("All log data cleared. User accounts were preserved.", "success");
        window.location.reload();
    } catch (error) {
        console.error(error);
        showSaveNotice(error.message || "Could not clear log data.", "danger");
    }
}

async function deleteSelectedAthlete() {
    if (currentUserRole !== "coach") return;

    if (!selectedUserId) {
        showSaveNotice("Select an athlete before deleting.", "warning");
        return;
    }

    const athleteName = selectedAthleteDisplayName || "this athlete";
    const confirmed = window.confirm(
        `Delete ${athleteName} from RaceTrack and Okta? This will also remove that athlete's running and workout logs.`
    );
    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`/api/admin/athletes/${encodeURIComponent(selectedUserId)}`, {
            method: "DELETE",
            headers: jsonHeaders()
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response, `Failed to delete athlete (${response.status})`));
        }

        storeReloadNotice("Athlete deleted from RaceTrack and Okta.", "success");
        window.location.reload();
    } catch (error) {
        console.error(error);
        showSaveNotice(error.message || "Could not delete the selected athlete.", "danger");
    }
}

window.clearData = clearData;
window.deleteSelectedAthlete = deleteSelectedAthlete;
window.openEditAthleteModal = openEditAthleteModal;

/**
 * Enables coach athlete filtering in the footer list.
 */
function setupAthleteSearch() {
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
    setupCoachAdminActions();
    showSubmissionNoticeFromUrl();
    showStoredNotice();

    // Athletes land on the running log form by default.
    // The workout form is shown only when explicitly requested via the header button.
    if (currentUserRole === "athlete") {
        showDefaultForm();
    }

    updateRunningSheetHeaderLabel();
});

window.updateRunningSheetHeaderLabel = updateRunningSheetHeaderLabel;
