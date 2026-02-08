// Initialize Tabulator
let table = new Tabulator("#runner-log-table", {
    ajaxURL: "/api/runner-log/" + runnerId,
    layout: "fitColumns",
    height: "auto",
    movableColumns: true,
    resizableRows: true,
    columnVertAlign: "middle",
    responsiveLayout: "hide",
    columns: [
        {title:"Date", field:"date", editor:"input"},
        {title:"Miles", field:"workout", editor:"input"},

        // Yes/No dropdowns using autocomplete
        {
            title:"Hurting?",
            field:"shoes",
            editor:"autocomplete",
            editorParams:{
                showListOnEmpty:true,
                values:["Yes","No"]
            }
        },
        {
            title:"Plate proportions on?",
            field:"time",
            editor:"autocomplete",
            editorParams:{
                showListOnEmpty:true,
                values:["Yes","No"]
            }
        },
        {
            title:"Did you get that bread?",
            field:"pace",
            editor:"autocomplete",
            editorParams:{
                showListOnEmpty:true,
                values:["Yes","No"]
            }
        },

        // 1–10 dropdowns using autocomplete
        {
            title:"Stress / Anxiety",
            field:"distance",
            editor:"autocomplete",
            editorParams:{
                showListOnEmpty:true,
                values:[1,2,3,4,5,6,7,8,9,10]
            }
        },
        {
            title:"RPE",
            field:"weather",
            editor:"autocomplete",
            editorParams:{
                showListOnEmpty:true,
                values:[1,2,3,4,5,6,7,8,9,10]
            }
        },

        // Text input columns
        {title:"How Feel", field:"shoes", editor:"input"},
        {title:"Comments", field:"feeling", editor:"input"},
        {title:"Strength / Strides?", field:"sleep", editor:"input"},
        {title:"Hard Day Workout & Paces", field:"hrAvg", editor:"input"}
    ],

    // Persist edits to backend
    cellEdited: function(cell) {
        let data = cell.getRow().getData();
        if (!data.id) return; // skip if row has no ID yet
        fetch(`/api/runner-log/${runnerId}/${data.id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        }).catch(err => console.error("Failed to save cell:", err));
    }
});

// Add new row button
document.getElementById("add-row-btn").addEventListener("click", function() {
    let newRow = {
        date: "",
        workout: "",
        distance: null, // Stress / Anxiety
        time: "",       // Plate proportions
        pace: "",       // Did you get that bread
        shoes: "",      // Hurting? / How Feel
        weather: null,  // RPE
        feeling: "",    // Comments
        sleep: "",      // Strength / Strides?
        hrAvg: "",      // Hard Day Workout & Paces
        hrMax: null,
        notes: ""
    };

    // Save to backend first
    fetch(`/api/runner-log/${runnerId}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(newRow)
    })
        .then(res => res.json())
        .then(savedRow => {
            table.addRow(savedRow, true).then(row => row.edit());
        })
        .catch(err => console.error("Failed to add row:", err));
});
