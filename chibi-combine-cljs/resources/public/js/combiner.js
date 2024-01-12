
function rename_skins(skins, chibi_name, shiftNum) {


  return chibi_combine.features.rename_skins(skins, chibi_name, shiftNum);

  if (!shiftNum) shiftNum = 1;
  // console.log(skins);
  for (var skinName in skins) {
    // var newK = chibi_name + "_" + skinName;
    var skin = skins[skinName];
    var newSkin = {};
    // console.log(skin);
    for (var kk in skin) {
      var o = skin[kk];
      var newKk = chibi_name + "_" + kk;
      var newO = {};
      for (var kk2 in o) {
        var oo = o[kk2];
        var newKk2 = chibi_name + "_" + kk2;
        var newOo = JSON.parse(JSON.stringify(oo));
        newOo.name = chibi_name + "_" + newOo.name;
        newOo.path = chibi_name + "_" + newOo.path;
        var ty = newOo.type;
        var vertices = newOo.vertices;
        if (vertices && "skinnedmesh" == ty) {
          var offset = 0;
          var length = vertices.length;
          while (offset < length) {
            var n = vertices[offset];
            if (n === 0) {
              console.log("???", n, offset);
              break;
            }
            for (var i = 0; i < n; i++) {
              var offset2 = offset + i * 4 + 1;
              vertices[offset2] += shiftNum;
              // console.log("bidx=", offset2, vertices[offset2]);
            }
            offset += n * 4 + 1;
          }
        }
        newO[newKk2] = newOo;
      }
      newSkin[newKk] = newO;
    }

    skins[skinName] = newSkin;
    // newSkins[newK] = newSkin;
  }
  // console.log(skins);
  return skins;
}


function fn_fix_obj_names(skel, chibi_name, shiftNum, newRoot, t) {
  console.log(chibi_name, "shiftNum", shiftNum, "newRoot", newRoot, t);
  console.log(skel);

  var x = 0, y = 0, scale = 1.0;
  if (t) {
    x = t.x || 0;
    y = t.y || 0;
    scale = t.scale || 1.0;
  }

  let bones = skel.bones;
  let slots = skel.slots;
  let animations = skel.animations;
  let skins = skel.skins;
  let events = skel.events;
  let eventsName = skel.eventsName;

  rename_skins(skins, chibi_name, shiftNum);

  if (slots) {
    for (var k in slots) {
      var o = slots[k];
      o.name = chibi_name + "_" + o.name;
      o.bone = chibi_name + "_" + o.bone;
      o.attachment = chibi_name + "_" + o.attachment;
    }
  }

  var newBones = [];
  if (newRoot) {
    newBones.push({name: "root", parent: null, color: "9b9b9bff"});
  }
  for (var i = 0; i < bones.length; i++) {
    var o = bones[i];
    o.name = chibi_name + "_" + o.name;
    if (o.parent) {
      o.parent = chibi_name + "_" + o.parent;
    } else {
      // old root bone
      o.x = (o.x || 0) + x;
      o.y = (o.y || 0) + y;
      var sX = (o.scaleX || 1) * scale;
      var sY = (o.scaleY || 1) * scale;
      o.scaleX = sX;
      o.scaleY = sY;
      o.parent = "root";
    }
    // console.log(i, o);
    newBones.push(o);
  }

  if (events) {
    var newEvents = {};
    for (var k in events) {
      var v = events[k];
      newEvents[chibi_name + "_" + k] = v;
    }
    skel.events = newEvents;
  }
  if (eventsName) {
    var newEventsName = [];
    for (var i = 0; i < eventsName.length; i++) {
      var v = eventsName[i];
      newEventsName.push(chibi_name + "_" + v);
    }
    skel.eventsName = newEventsName;
  }

  var newAnis = {};
  for (var name in animations) {
    // console.log("ani" , name);
    var ani = animations[name];

    var aniSlots = ani.slots;
    if (aniSlots) {
      var newAniSlots = {};
      for (var ks in aniSlots) {
        var slot = aniSlots[ks];
        var newSlot = {};
        var attrs = slot.attachment;
        if (!attrs) continue;
        for (var idx in attrs) {
          var tl = attrs[idx];
          tl.name = chibi_name + "_" + tl.name;
        }
        newSlot.attachment = attrs;
        newAniSlots[chibi_name + "_" + ks] = newSlot;
      }
      ani.slots = newAniSlots;

    }

    var aniBones = ani.bones;
    if (aniBones) {
      var newAniBones = {};
      for (var kb in aniBones) {
        var bone = aniBones[kb];
        newAniBones[chibi_name + "_" + kb] = bone;
      }
      ani.bones = newAniBones;
    }

    var aniEvents = ani.events;
    if (aniEvents) {
      for (var i = 0; i < aniEvents.length; i++) {
        var tl = aniEvents[i];
        tl.name = chibi_name + "_" + tl.name;
      }
    }

    var ffds = ani.ffd;
    if (ffds) {
      var newFfds = {};
      for (var ffdname in ffds) {
        // default
        var ffd = ffds[ffdname];
        var newFfd = {};
        for (var k in ffd) {
          // baodaia
          var ffdo = ffd[k];
          var newFfdo = {};
          for (var k2 in ffdo) {
            var ar = ffdo[k2];
            newFfdo[chibi_name + "_" + k2] = ar;
          }
          newFfd[chibi_name + "_" + k] = newFfdo;
        }
        newFfds[ffdname] = newFfd;
      }
      ani.ffd = newFfds;
    }

    var drawOrder = ani.drawOrder;
    if (drawOrder)
      for (var i = 0; i < drawOrder.length; i++) {
        var dwo = drawOrder[i];
        // console.log("drawOrder r=", i, dwo);
        dwo.offsets.forEach(offset => {
          offset.slot = chibi_name + "_" + offset.slot;
        });
      }

    newAnis[chibi_name + "_" + name] = ani;
  }

  skel.bones = newBones;
  skel.animations = newAnis;

  return skel;
}

