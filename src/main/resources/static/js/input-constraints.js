(() => {
    const DEFAULT_TEXT_MAX = 2000;

    /**
     * Returns the configured max length for a text field.
     * @param {HTMLInputElement|HTMLTextAreaElement} field
     * @returns {number}
     */
    function resolveMax(field) {
        const raw = field.dataset.charMax || field.getAttribute("maxlength");
        const parsed = Number.parseInt(raw, 10);
        if (Number.isNaN(parsed) || parsed <= 0) {
            return DEFAULT_TEXT_MAX;
        }
        return parsed;
    }

    /**
     * Renders and updates the counter text for a field.
     * @param {HTMLInputElement|HTMLTextAreaElement} field
     * @param {HTMLElement} counter
     */
    function updateCounter(field, counter) {
        const max = resolveMax(field);
        const currentLength = (field.value || "").length;
        counter.textContent = `${currentLength}/${max} characters`;
    }

    /**
     * Adds counter + max-length behavior to one field.
     * @param {HTMLInputElement|HTMLTextAreaElement} field
     */
    function attachCounter(field) {
        if (field.dataset.counterBound === "true") {
            return;
        }

        // Normalize the DOM max-length so browser-native enforcement matches the rendered counter.
        const max = resolveMax(field);
        field.maxLength = max;

        let counter = field.nextElementSibling;
        if (!counter || !counter.classList.contains("char-counter")) {
            counter = document.createElement("div");
            counter.className = "char-counter";
            field.insertAdjacentElement("afterend", counter);
        }

        const sync = () => updateCounter(field, counter);
        field.addEventListener("input", sync);
        field.addEventListener("change", sync);
        sync();

        field.dataset.counterBound = "true";
    }

    /**
     * Binds counters for all marked fields in a container.
     * @param {ParentNode} [container=document]
     */
    function attachCharCounters(container = document) {
        const fields = container.querySelectorAll(".js-limited-text");
        fields.forEach(field => attachCounter(field));
    }

    document.addEventListener("DOMContentLoaded", () => {
        // Bind once on initial page load; dynamically rendered rows call attachCharCounters separately.
        attachCharCounters(document);
    });

    window.attachCharCounters = attachCharCounters;
})();
