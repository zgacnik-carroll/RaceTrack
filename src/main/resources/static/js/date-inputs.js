(() => {
    function todayIsoDate() {
        return new Date().toISOString().slice(0, 10);
    }

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
            if (!input.value) {
                input.value = today;
            }
            attachDatePickerBehavior(input);
        });
    });
})();
