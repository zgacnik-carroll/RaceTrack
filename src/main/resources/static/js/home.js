function showForm(type) {
    const emptyState = document.getElementById("emptyState");
    const runningForm = document.getElementById("runningForm");
    const workoutForm = document.getElementById("workoutForm");

    // Hide everything first
    emptyState.style.display = "none";
    runningForm.style.display = "none";
    workoutForm.style.display = "none";

    // Show selected content
    if (type === "running") {
        runningForm.style.display = "block";
    }

    if (type === "workout") {
        workoutForm.style.display = "block";
    }
}
