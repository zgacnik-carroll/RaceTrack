(() => {
    /**
     * Returns current date in YYYY-MM-DD format for date inputs.
     * @returns {string}
     */
    function todayIsoDate() {
        return new Date().toISOString().slice(0, 10);
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
            attachDatePickerBehavior(input);
        });
    });
})();
