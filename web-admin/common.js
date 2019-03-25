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
