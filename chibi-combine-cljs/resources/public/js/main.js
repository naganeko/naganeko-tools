var ws_w = localStorage.getItem("ws_w_cc") || 800;
var ws_h = localStorage.getItem("ws_h_cc") || 500;
// var ws_scale = localStorage.getItem("ws_scale") || 1.0;
var ws_scale = 1.0;
ws_w = parseInt(ws_w);
ws_h = parseInt(ws_h);
ws_scale = parseFloat(ws_scale);

var single_motion_only = "true" == localStorage.getItem("single_motion_only") || false;

function set_single_motion_only(b) {
  single_motion_only = !!b;
  localStorage.setItem("single_motion_only", single_motion_only);
  $("#dousa").css("background", single_motion_only ? "orange" : "");
  return single_motion_only;
}

let spinner_opts = {
  lines: 15, // The number of lines to draw
  length: 20, // The length of each line
  width: 6, // The line thickness
  radius: 30, // The radius of the inner circle
  scale: 0.75, // Scales overall size of the spinner
  corners: 0.7, // Corner roundness (0..1)
  color: '#000000', // CSS color or array of colors
  fadeColor: 'transparent', // CSS color or array of colors
  speed: 1, // Rounds per second
  rotate: 0, // The rotation offset
  animation: 'spinner-line-fade-quick', // The CSS animation name for the lines
  direction: 1, // 1: clockwise, -1: counterclockwise
  zIndex: 2e9, // The z-index (defaults to 2000000000)
  className: 'spinner', // The CSS class to assign to the spinner
  top: '0%', // Top position relative to parent
  left: '0%', // Left position relative to parent
  shadow: '0 0 1px transparent', // Box-shadow for the lines
  position: 'relative' // Element positioning
};

var spinner;

function add_spinner() {
  var target = document.getElementById('gifhere');
  spinner = new Spinner(spinner_opts).spin(target);
}

function remove_spinner() {
  spinner.stop();
}

window.URL = window.URL || window.webkitURL;

function _d() {
  return window.chibi_ns.core._h.doll;
}

function _s() {
  return _d().state;
}

function _sk() {
  return _d().skeleton;
}

function _fb(n) {
  return _sk().findBone(n);
}

function _fbi(n) {
  return _sk().findBoneIndex(n);
}

function _fs(n) {
  return _sk().findSlot(n);
}

function _fsi(n) {
  return _sk().findSlotIndex(n);
}

function _bs() {
  return _d().spineData.bones.map(a => a.name)
}

function _ss() {
  return _d().spineData.slots.map(a => a.name)
}

function _fa(k) {
  let d = _d().spineData;
  return "number" === $.type(k) ? d.animations[k] : d.findAnimation(k);
}

function _lkbntl(idx, n) {
  var bidx = _sk().findBoneIndex(n);
  return _fa(idx).timelines.filter(t => t.boneIndex == bidx);
}

function _lksltl(idx, n) {
  var sidx = _sk().findSlotIndex(n);
  return _fa(idx).timelines.filter(t => t.slotIndex == sidx);
}

function _dcp(o) {
  return JSON.parse(JSON.stringify(o));
}

function _sltxtr(d, sn) {
  var s = d.skeleton.findSlot(sn);
  return s.attachment.region.texture;
}

var megane = null;

var hold_skel_json = true;
var clearfail;

function http_get(url, callback) {
  $.get(url, function (res) {
    callback(res);
  });
}

var last_files = null;



function findSlot0(sk, slotName) {
  var slots = sk.slots;
  for (var i = 0, n = slots.length; i < n; i++) {
    var sl = slots[i];
    var sps = sl.sprites;
    if (!sps) {
      continue;
    }
    var sp = sps[slotName];
    if (sp) {
      return sl;
    }
  }
  return null;
}

