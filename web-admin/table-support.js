function bindSortTableCall(tableId, i) {
    return function () {
        sortTable(tableId, i);
    };
}

function initTableSort(tableId) {
    var table = document.getElementById(tableId);
    var cells = table.rows[0].cells;
    for (var i = 0; i < cells.length; i++) {

        cells[i].classList.add("w3-button")
        cells[i].innerHTML += "&nbsp;<span id=\"sort_indicator_col_" + i + "\">&nbsp;</span>";
        cells[i].addEventListener("click", bindSortTableCall(tableId, i));
    }
}

var prevSortCol = 0;

function sortTable(tableId, sortCol) {
    var table = document.getElementById(tableId);
    var i;
    var shouldSwitch;
    var count = 0;

    // Set the sorting direction to ASCending:
    var order = "ASC";

    // loop until no switching has been done
    var switching = true;
    while (switching) {

        switching = false;

        var rows = table.rows;

        // skip the header row
        for (i = 1; i < (rows.length - 1); i++) {
            shouldSwitch = false;

            var x = rows[i].getElementsByTagName("TD")[sortCol];
            var y = rows[i + 1].getElementsByTagName("TD")[sortCol];

            if (order == "ASC") {
                if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {
                    shouldSwitch = true;
                    break;
                }
            } else if (order == "DES") {
                if (x.innerHTML.toLowerCase() < y.innerHTML.toLowerCase()) {
                    shouldSwitch = true;
                    break;
                }
            }
        }

        if (shouldSwitch) {
            /* If a switch has been marked, make the switch
            and mark that a switch has been done: */
            rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
            switching = true;
            // Each time a switch is done, increase this count by 1:
            count++;
        } else {
            /* If no switching has been done AND the direction is "ASC",
            set the direction to "DES" and run the while loop again. */
            if (count == 0 && order == "ASC") {
                order = "DES";
                switching = true;
            }
        }
    }

    var e = document.getElementById("sort_indicator_col_" + prevSortCol);
    e.innerHTML = "&nbsp;";

    e = document.getElementById("sort_indicator_col_" + sortCol)

    if (order === "ASC") {
        e.innerHTML = '<i id="col_0_sort_up" class="fa fa-sort-up"></i>'
    } else {
        e.innerHTML = '<i id="col_0_sort_up" class="fa fa-sort-down"></i>'
    }
    prevSortCol = sortCol;

}
