/* =========================
   HOME JS
   - Form display
   - Student selection
========================= */

let selectedUserId = null;
let selectedAthleteDisplayName = "";
let selectedAthleteEmail = "";
let selectedManagedUserRole = "";
let activeFooterAthleteMenu = null;
const currentUserRole = (window.currentUserRole || "athlete").toLowerCase();
const currentUserId = window.currentUserId || null;
const currentUserName = window.currentUserName || "";

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
    clearAthleteViewSelection();
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

function syncRunningPainDetailsVisibility() {
    const hurtingSelect = document.getElementById("runningHurtingSelect");
    const painRow = document.getElementById("runningPainDetailsWrapper");
    const painInput = document.getElementById("runningPainDetailsInput");
    if (!hurtingSelect || !painRow || !painInput) return;

    const isHurting = hurtingSelect.value === "true";
    painRow.style.display = isHurting ? "" : "none";
    painInput.required = isHurting;

    if (!isHurting) {
        painInput.value = "";
        const counter = painInput.nextElementSibling;
        if (counter?.classList?.contains("char-counter")) {
            counter.textContent = "0/100 characters";
        }
    }
}

/* =========================
   STUDENT SELECTION
========================= */

/**
 * Stores selected athlete metadata in shared state.
 * @param {HTMLElement} element selected athlete trigger/menu item
 * @returns {boolean}
 */
function setSelectedStudent(element) {
    const source = element?.closest?.(".athlete-menu") || element;
    if (!source?.dataset) return false;

    selectedUserId = source.dataset.userId || null;
    selectedAthleteDisplayName = source.dataset.displayName || "Selected athlete";
    selectedAthleteEmail = source.dataset.email || "";
    selectedManagedUserRole = source.dataset.role || "";
    updateRunningSheetHeaderLabel();
    return true;
}

/**
 * Selects an athlete and opens the requested sheet.
 * @param {HTMLElement} element selected athlete trigger/menu item
 * @param {"running"|"workout"} sheetType
 */
function viewStudentSheet(element, sheetType) {
    if (!setSelectedStudent(element)) return;

    closeAthleteMenus();
    hideMainContent();
    if (sheetType === "workout") {
        showWorkoutSheet(selectedUserId);
        return;
    }
    showRunningSheet(selectedUserId);
}

/**
 * Selects an athlete for coach view and opens that athlete's running sheet.
 * @param {HTMLElement} element selected athlete button
 */
function selectStudent(element) {
    viewStudentSheet(element, "running");
}

/**
 * Closes all open athlete menus in the footer.
 */
function closeAthleteMenus() {
    document.querySelectorAll(".athlete-menu.is-active").forEach((menu) => {
        menu.classList.remove("is-active");
    });

    const floatingMenu = document.getElementById("athleteFloatingMenu");
    if (floatingMenu) {
        floatingMenu.classList.remove("is-open");
        floatingMenu.setAttribute("aria-hidden", "true");
        floatingMenu.style.left = "";
        floatingMenu.style.top = "";
        floatingMenu.style.bottom = "";
    }

    activeFooterAthleteMenu = null;
}

/**
 * Positions the shared floating footer menu above the clicked athlete button.
 * @param {HTMLElement} button
 */
function positionFooterMenu(button) {
    const floatingMenu = document.getElementById("athleteFloatingMenu");
    const footerAthletes = document.querySelector(".footer-athletes");
    if (!floatingMenu || !footerAthletes || !button) return;

    const footerRect = footerAthletes.getBoundingClientRect();
    const buttonRect = button.getBoundingClientRect();
    const menuWidth = floatingMenu.offsetWidth;
    const left = buttonRect.left - footerRect.left + ((buttonRect.width - menuWidth) / 2);
    const maxLeft = Math.max(0, footerRect.width - menuWidth);
    const clampedLeft = Math.min(Math.max(0, left), maxLeft);
    const gap = 10;
    const bottom = footerRect.bottom - buttonRect.top + gap;

    floatingMenu.style.left = `${clampedLeft}px`;
    floatingMenu.style.top = "auto";
    floatingMenu.style.bottom = `${bottom}px`;
}

/**
 * Opens the shared floating footer menu for one athlete button.
 * @param {HTMLElement} button
 */
function openAthleteMenu(button) {
    const menu = button?.closest?.(".athlete-menu");
    const floatingMenu = document.getElementById("athleteFloatingMenu");
    if (!menu || !floatingMenu) return;

    if (activeFooterAthleteMenu === menu && floatingMenu.classList.contains("is-open")) {
        closeAthleteMenus();
        return;
    }

    setSelectedStudent(menu);
    closeAthleteMenus();
    activeFooterAthleteMenu = menu;
    menu.classList.add("is-active");
    floatingMenu.classList.add("is-open");
    floatingMenu.setAttribute("aria-hidden", "false");
    positionFooterMenu(button);
}

/**
 * Opens the selected athlete's sheet from the shared footer menu.
 * @param {"running"|"workout"} sheetType
 */
function viewSelectedFooterSheet(sheetType) {
    if (!activeFooterAthleteMenu) return;
    viewStudentSheet(activeFooterAthleteMenu, sheetType);
}

/**
 * Clears selected athlete and returns coach UI to empty state.
 */
function clearStudentSelection() {
    clearAthleteViewSelection();
    updateRunningSheetHeaderLabel();

    if (currentUserRole === "athlete") {
        showDefaultForm();
        return;
    }

    hideMainContent();
    document.getElementById("emptyState").style.display = "block";
}

/**
 * Clears athlete-view selection context so self-navigation does not reuse another athlete.
 */
function clearAthleteViewSelection() {
    selectedUserId = null;
    selectedAthleteDisplayName = "";
    selectedAthleteEmail = "";
    selectedManagedUserRole = "";
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
        : currentUserName ? `- ${currentUserName}` : currentUserId ? "- My Logs" : "";
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

    const createUserForm = document.getElementById("createUserForm");
    const submitButton = document.getElementById("createUserSubmitButton");
    const editUserForm = document.getElementById("editUserForm");
    const editSubmitButton = document.getElementById("editUserSubmitButton");
    if (!createUserForm || !submitButton || !editUserForm || !editSubmitButton) return;

    createUserForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const formData = new FormData(createUserForm);
        const payload = {
            firstName: String(formData.get("firstName") || "").trim(),
            lastName: String(formData.get("lastName") || "").trim(),
            email: String(formData.get("email") || "").trim(),
            role: String(formData.get("role") || "").trim().toLowerCase(),
            temporaryPassword: String(formData.get("temporaryPassword") || "").trim()
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            showSaveNotice("First name, last name, and email are required.", "warning");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "Creating...";

        try {
            const response = await fetch("/api/admin/users", {
                method: "POST",
                headers: jsonHeaders(),
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await readErrorMessage(response, `Failed to create user (${response.status})`));
            }

            const modalElement = document.getElementById("createUserModal");
            const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;
            if (modal) {
                modal.hide();
            }

            createUserForm.reset();
            storeReloadNotice("User created in Okta and added to RaceTrack.", "success");
            window.location.reload();
        } catch (error) {
            console.error(error);
            showSaveNotice(error.message || "Could not create user.", "danger");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Create User";
        }
    });

    editUserForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (!selectedUserId) {
            showSaveNotice("Select a user before editing.", "warning");
            return;
        }

        const formData = new FormData(editUserForm);
        const payload = {
            firstName: String(formData.get("firstName") || "").trim(),
            lastName: String(formData.get("lastName") || "").trim(),
            email: String(formData.get("email") || "").trim(),
            role: String(formData.get("role") || "").trim().toLowerCase(),
            temporaryPassword: String(formData.get("temporaryPassword") || "").trim()
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            showSaveNotice("First name, last name, and email are required.", "warning");
            return;
        }

        editSubmitButton.disabled = true;
        editSubmitButton.textContent = "Saving...";

        try {
            const response = await fetch(`/api/admin/users/${encodeURIComponent(selectedUserId)}`, {
                method: "PUT",
                headers: jsonHeaders(),
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await readErrorMessage(response, `Failed to edit user (${response.status})`));
            }

            const modalElement = document.getElementById("editUserModal");
            const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;
            if (modal) {
                modal.hide();
            }

            editUserForm.reset();
            storeReloadNotice("User updated in RaceTrack and Okta.", "success");
            window.location.reload();
        } catch (error) {
            console.error(error);
            showSaveNotice(error.message || "Could not update user.", "danger");
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

function openEditUserModal() {
    if (currentUserRole !== "coach") return;
    if (!selectedUserId) {
        showSaveNotice("Select a user before editing.", "warning");
        return;
    }

    const name = splitSelectedAthleteName();
    const firstNameInput = document.getElementById("editUserFirstName");
    const lastNameInput = document.getElementById("editUserLastName");
    const emailInput = document.getElementById("editUserEmail");
    const roleInput = document.getElementById("editUserRole");
    const passwordInput = document.getElementById("editUserTemporaryPassword");

    if (firstNameInput) firstNameInput.value = name.firstName;
    if (lastNameInput) lastNameInput.value = name.lastName;
    if (emailInput) emailInput.value = selectedAthleteEmail;
    if (roleInput) roleInput.value = selectedManagedUserRole || "athlete";
    if (passwordInput) passwordInput.value = "";

    const modalElement = document.getElementById("editUserModal");
    if (!modalElement) return;
    bootstrap.Modal.getOrCreateInstance(modalElement).show();
}

function openEditUserModalFor(element) {
    if (!setSelectedStudent(element)) return;
    openEditUserModal();
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

async function deleteSelectedUser() {
    if (currentUserRole !== "coach") return;

    if (!selectedUserId) {
        showSaveNotice("Select a user before deleting.", "warning");
        return;
    }

    const userName = selectedAthleteDisplayName || "this user";
    const confirmed = window.confirm(
        `Delete ${userName} from RaceTrack and Okta? This will also remove that user's running and workout logs.`
    );
    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`/api/admin/users/${encodeURIComponent(selectedUserId)}`, {
            method: "DELETE",
            headers: jsonHeaders()
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response, `Failed to delete user (${response.status})`));
        }

        storeReloadNotice("User deleted from RaceTrack and Okta.", "success");
        window.location.reload();
    } catch (error) {
        console.error(error);
        showSaveNotice(error.message || "Could not delete the selected user.", "danger");
    }
}

async function deleteUserFor(element) {
    if (!setSelectedStudent(element)) return;
    await deleteSelectedUser();
}

window.clearData = clearData;
window.deleteSelectedUser = deleteSelectedUser;
window.deleteUserFor = deleteUserFor;
window.openAthleteMenu = openAthleteMenu;
window.openEditUserModal = openEditUserModal;
window.openEditUserModalFor = openEditUserModalFor;
window.viewSelectedFooterSheet = viewSelectedFooterSheet;
window.viewStudentSheet = viewStudentSheet;

/**
 * Enables coach athlete filtering in the footer list.
 */
function setupAthleteSearch() {
    const search = document.getElementById("athleteSearch");
    const athleteMenus = Array.from(document.querySelectorAll(".athlete-list .athlete-menu"));
    if (!search || athleteMenus.length === 0) return;

    search.addEventListener("input", (event) => {
        const query = event.target.value.trim().toLowerCase();
        athleteMenus.forEach((menu) => {
            const name = (menu.dataset.name || "").toLowerCase();
            const email = (menu.dataset.email || "").toLowerCase();
            const visible = !query || name.includes(query) || email.includes(query);
            menu.style.display = visible ? "" : "none";
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

    const hurtingSelect = document.getElementById("runningHurtingSelect");
    if (hurtingSelect) {
        hurtingSelect.addEventListener("change", syncRunningPainDetailsVisibility);
        syncRunningPainDetailsVisibility();
    }

    updateRunningSheetHeaderLabel();
});

document.addEventListener("click", (event) => {
    if (event.target.closest(".athlete-menu") || event.target.closest("#athleteFloatingMenu")) return;
    closeAthleteMenus();
});

window.clearAthleteViewSelection = clearAthleteViewSelection;
window.updateRunningSheetHeaderLabel = updateRunningSheetHeaderLabel;
