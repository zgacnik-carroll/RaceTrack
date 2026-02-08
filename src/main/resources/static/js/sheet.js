var table = new Tabulator("#sheet-table", {

    ajaxURL: "/api/sheet",
    layout: "fitColumns",

    columns: [
        {title:"A", field:"colA", editor:"input"},
        {title:"B", field:"colB", editor:"input"},
        {title:"C", field:"colC", editor:"input"}
    ],

    cellEdited: function(cell) {

        fetch("/api/sheet/update", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                id: cell.getRow().getData().id,
                field: cell.getField(),
                value: cell.getValue()
            })
        });

    }
});
