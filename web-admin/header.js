var icons = {
  dashboard: "fa fa-tachometer-alt",
  sensors: "fa fa-rss",
  inventory: "fa fa-tags",
  tag_stats: "fa fa-tags",
  upstream: "fa fa-cloud-upload-alt",
  downstream: "fa fa-download",
  scheduler: "fa fa-calendar",
  configuration: "fa fa-cogs"
};

var pages = {
  dashboard: { label: "Dashboard", ref: "dashboard.html", icon: icons.dashboard},
  sensors: { label: "Sensors", ref: "sensors-main.html", icon: icons.sensors},
  inventory: { label: "Inventory", ref: "inventory-main.html", icon: icons.inventory},
  tag_stats: { label: "Tag Statistics", ref: "tag-stats.html", icon: icons.tag_stats},
  scheduler: { label: "Scheduler", ref: "scheduler.html", icon: icons.scheduler},
};


// height of 100px for the size of the logo
// width of 160px for the drop down items default width
document.write('<div class="w3-bar">');
document.write('  <div class="w3-bar-item">');
document.write('    <div class="w3-bar-block w3-black w3-dropdown-hover">');
document.write('      <button class="w3-button"><i class="fa fa-bars w3-xlarge"></i></button>');
document.write('      <div class="w3-dropdown-content">');

Object.keys(pages).forEach(key => {
    let page = pages[key];
    document.write('    ' +
        '<a class="w3-bar-item w3-button" ' +
        'href="' + page["ref"] + '">' +
        page["label"] +
        '</a>');
});


document.write('      </div>');
document.write('    </div>');
document.write('  </div>');
document.write('  <div class="w3-bar-item w3-xlarge">');
document.write('    ' + pages[currentPage].label + ' <i class=" ' +pages[currentPage].icon +'"></i> ');
document.write('  </div>');
document.write('</div>');

