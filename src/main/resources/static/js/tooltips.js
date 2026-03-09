// Enable Bootstrap tooltips after the DOM is ready.
document.addEventListener('DOMContentLoaded', function () {
    // Bootstrap expects explicit initialization for each tooltip trigger.
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    const tooltipList = [...tooltipTriggerList].map(el => new bootstrap.Tooltip(el));
});
