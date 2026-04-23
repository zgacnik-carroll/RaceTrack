(() => {
    /**
     * Returns current date in YYYY-MM-DD format for date inputs.
     * @returns {string}
     */
    function todayIsoDate() {
        const now = new Date();
        const yyyy = String(now.getFullYear());
        const mm = String(now.getMonth() + 1).padStart(2, "0");
        const dd = String(now.getDate()).padStart(2, "0");
        return `${yyyy}-${mm}-${dd}`;
    }

    /**
     * Opens native picker on click/focus when browser supports showPicker().
     * @param {HTMLInputElement} input
     */
    function attachDatePickerBehavior(input) {
        const openPicker = () => {
            if (typeof input.showPicker === "function") {
                input.showPicker();
            }
        };

        input.addEventListener("focus", openPicker);
        input.addEventListener("click", openPicker);
    }

    document.addEventListener("DOMContentLoaded", () => {
        const inputs = document.querySelectorAll("input.js-date-input[type='date']");
        const today = todayIsoDate();

        inputs.forEach(input => {
            // Keep existing values intact; only default blank fields.
            if (!input.value) {
                input.value = today;
            }
            // Bind picker helpers after any default value has been applied.
            attachDatePickerBehavior(input);
        });
    });
})();
