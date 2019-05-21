function zeroPad(num, len) {
    var s = "0000000000" + num;
    return s.substr(s.length - len);
}

function epochToGMT(timestamp) {
    var d = new Date(timestamp);
    return d.getUTCFullYear() + "-" +
        zeroPad(d.getUTCMonth(), 2) + "-" +
        zeroPad(d.getUTCDate(), 2) + "&nbsp;&nbsp;" +
        zeroPad(d.getUTCHours(), 2) + "." +
        zeroPad(d.getUTCMinutes(), 2) + "." +
        zeroPad(d.getUTCSeconds(), 2) + "." +
        zeroPad(d.getUTCMilliseconds(), 3) + " Z";
}

function mapSeverityToColor(severity) {
    switch (severity) {
        case "info":
            return "w3-green";
        case "warning":
            return "w3-yellow";
        case "urgent":
            return "w3-orange";
        case "critical":
            return "w3-red";
    }
}

var icons = {
    dashboard: "fa fa-tachometer-alt",
    sensors: "fa fa-rss",
    inventory: "fa fa-tags",
    tag_stats: "fa fa-tags",
    upstream: "fa fa-cloud-upload-alt",
    downstream: "fa fa-download",
    scheduler: "far fa-calendar-alt",
    configuration: "fa fa-cogs",
    connected: "fa fa-link",
    disconnected: "fa fa-unlink",
    reading: "fa fa-rss",
    stopped: "fas fa-square",
    old_calendar: "fa fa-calendar",
};
