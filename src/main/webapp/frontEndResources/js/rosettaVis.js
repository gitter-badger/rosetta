var earth;
var marker;
document.getElementById("updateLocation").onclick = updateLocationVisMarker;

function initLocationVis() {
    earth = new WE.map('location_vis_earth_div');
    WE.tileLayer('http://tileserver.maptiler.com/nasa/{z}/{x}/{y}.jpg',{
        attribution: 'NASA Blue Marble'
    }).addTo(earth);

    var lat = 40;
    var lon = -105;
    var name = "init";
    marker = WE.marker([lat,lon]);
    marker.bindPopup(name);
    marker.addTo(earth);
    earth.panTo([lat, lon]);
}

function updateLocationVisMarker(lat, lon, name) {
    marker.setLatLng([lat,lon]);
    marker.bindPopup(name);
    earth.panTo([lat, lon]);
}