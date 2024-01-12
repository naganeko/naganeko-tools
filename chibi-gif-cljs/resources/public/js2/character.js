//modified to fit priconneDB by jinsung

var spine_cysp = {
  lastFrameTime: Date.now() / 1000,
  canvas: null,
  shader: null,
  batcher: null,
  gl: null,
  mvp: new spine.webgl.Matrix4(),
  skeletonRenderer: null,
  shapes: null,
  Skeleton: {},
  animationQueue: [],
  speedFactor: 1,
  bgColor: [1,1,1,0],

  getClass: function(i) {
    return (i < 10 ? '0' : '') + i;
  },
  loadData: function(url, cb, loadType, progress) {
    var xhr = new XMLHttpRequest;
    xhr.open('GET', url, true);
    if (loadType) xhr.responseType = loadType;
    if (progress) xhr.onprogress = progress;
    xhr.onload = function () {
      if (xhr.status == 200)
        cb(true, xhr.response);
      else
        cb(false);
    }
    xhr.onerror = function () {
      cb(false);
    }
    xhr.send();
  },
  init: function() {
    spine_cysp.canvas = document.getElementById("canvas");
    var config = { alpha: false };
    spine_cysp.gl = spine_cysp.canvas.getContext("webgl", config) || spine_cysp.canvas.getContext("experimental-webgl", config);
    if (!spine_cysp.gl) {
      alert('WebGL is unavailable.');
      return;
    }
  
    // Create a simple shader, mesh, model-view-projection matrix and SkeletonRenderer.
    spine_cysp.shader = spine.webgl.Shader.newTwoColoredTextured(spine_cysp.gl);
    spine_cysp.batcher = new spine.webgl.PolygonBatcher(spine_cysp.gl);
    spine_cysp.mvp.ortho2d(0, 0, spine_cysp.canvas.width - 1, spine_cysp.canvas.height - 1);
    spine_cysp.skeletonRenderer = new spine.webgl.SkeletonRenderer(spine_cysp.gl);
    spine_cysp.shapes = new spine.webgl.ShapeRenderer(spine_cysp.gl);
  },

  loading: false,
  loadingSkeleton: null,
  generalBattleSkeletonData: null,
  currentTexture: null,
  currentClassAnimData: {
    type: 0,
    data: {}
  },
  currentCharaAnimData: {
    id: 0,
    data: {}
  },
  currentClass: '1',

  load: function(unit_id, class_id) {
    if (spine_cysp.loading) return;
    spine_cysp.loading = true;
    spine_cysp.currentClass = class_id;
    var baseUnitId = unit_id | 0;
    baseUnitId -= baseUnitId % 100 - 1;
    spine_cysp.loadingSkeleton = { id: unit_id };
  
    if (!spine_cysp.generalBattleSkeletonData)
      if (Math.floor(unit_id / 100000) !== 1)
        spine_cysp.loadData('data/img/spine/unitanimation/' + unit_id + '_CHARA_BASE.cysp', function (success, json) {
          spine_cysp.generalBattleSkeletonData = json;
          spine_cysp.loadClassAnimation();
        }, 'arraybuffer');
      else 
        spine_cysp.loadData('data/img/spine/unitanimation/000000_CHARA_BASE.cysp', function (success, json) {
          spine_cysp.generalBattleSkeletonData = json;
          spine_cysp.loadClassAnimation();
        }, 'arraybuffer');
    else spine_cysp.loadClassAnimation();
  },
  loadClassAnimation: function() {
    if (spine_cysp.currentClassAnimData.type == spine_cysp.currentClass)
      spine_cysp.loadCharaSkillAnimation();
    else
      spine_cysp.loadData('data/img/spine/unitanimation/' + spine_cysp.getClass(spine_cysp.currentClass) + '_COMMON_BATTLE.cysp', function (success, json) {
        spine_cysp.currentClassAnimData = {
          type: spine_cysp.currentClass,
          data: json
        }
        spine_cysp.loadCharaSkillAnimation();
      }, 'arraybuffer');
  },
  loadCharaSkillAnimation: function() {
    var baseUnitId = spine_cysp.loadingSkeleton.id;
    if (Math.floor(baseUnitId / 100000) !== 1) 
      baseUnitId -= baseUnitId % 100;
    else 
      baseUnitId -= baseUnitId % 100 - 1;
    
    if (spine_cysp.currentCharaAnimData.id == baseUnitId)
      spine_cysp.loadTexture();
    else
      spine_cysp.loadData('data/img/spine/unitanimation/' + baseUnitId + '_BATTLE.cysp', function (success, json) {
      spine_cysp.currentCharaAnimData = {
        id: baseUnitId,
        data: json
      }
      spine_cysp.loadTexture();
    }, 'arraybuffer');
  },
  loadTexture: function() {
    spine_cysp.loadData('data/img/spine/sdnormal/spine_' + spine_cysp.loadingSkeleton.id + '/' + spine_cysp.loadingSkeleton.id + '.atlas', function (success, atlasText) {
      spine_cysp.loadData('data/img/spine/sdnormal/spine_' + spine_cysp.loadingSkeleton.id + '/' + spine_cysp.loadingSkeleton.id + '.png', function (success, blob) {
        if (!success) return spine_cysp.loading = false;
        var img = new Image();
        img.onload = function () {
          var created = !!spine_cysp.Skeleton.skeleton;
          if (created) {
            spine_cysp.Skeleton.state.clearTracks();
            spine_cysp.Skeleton.state.clearListeners();
            spine_cysp.gl.deleteTexture(spine_cysp.currentTexture.texture)
          }
  
          var imgTexture = new spine.webgl.GLTexture(spine_cysp.gl, img);
          URL.revokeObjectURL(img.src);
          atlas = new spine.TextureAtlas(atlasText, function (path) {
            return imgTexture;
          });
          spine_cysp.currentTexture = imgTexture;
          atlasLoader = new spine.AtlasAttachmentLoader(atlas);

          //assume always no more than 128 animations
          var newBuffSize = spine_cysp.generalBattleSkeletonData.byteLength - 64 + 1;
  
          var animationCount = 0;
          if (spine_cysp.currentClassAnimData.type !== 0) { 
            var classAnimView = new DataView(spine_cysp.currentClassAnimData.data);
            var classAnimCount = classAnimView.getInt32(12, true);
            animationCount += classAnimCount;
            newBuffSize += spine_cysp.currentClassAnimData.data.byteLength - (++classAnimCount) * 32;
          }
          var unitAnimView = new DataView(spine_cysp.currentCharaAnimData.data);
          var unitAnimCount = unitAnimView.getInt32(12, true)
          animationCount += unitAnimCount;
          newBuffSize += spine_cysp.currentCharaAnimData.data.byteLength - (++unitAnimCount) * 32;

          var newBuff = new Uint8Array(newBuffSize);
          var offset = 0;
          newBuff.set(new Uint8Array(spine_cysp.generalBattleSkeletonData.slice(64)), 0);
          offset += spine_cysp.generalBattleSkeletonData.byteLength - 64;
          newBuff[offset] = animationCount;
          offset++;
          if (spine_cysp.currentClassAnimData.type !== 0) {
            newBuff.set(new Uint8Array(spine_cysp.currentClassAnimData.data.slice(classAnimCount * 32)), offset);
            offset += spine_cysp.currentClassAnimData.data.byteLength - classAnimCount * 32;
          }
          newBuff.set(new Uint8Array(spine_cysp.currentCharaAnimData.data.slice(unitAnimCount * 32)), offset);
          offset += spine_cysp.currentCharaAnimData.data.byteLength - unitAnimCount * 32;
  
          var skeletonBinary = new spine.SkeletonBinary(atlasLoader);
          var skeletonData = skeletonBinary.readSkeletonData(newBuff.buffer);
          var skeleton = new spine.Skeleton(skeletonData);
          
          skeleton.setSkinByName('default');
          var bounds = spine_cysp.calculateBounds(skeleton);
  
          animationStateData = new spine.AnimationStateData(skeleton.data);
          var animationState = new spine.AnimationState(animationStateData);

          let defaultAnimation = skeleton.data.animations[0].name;
          skeleton.data.animations.map(function(val) {
            if(val.name.indexOf('_idle') >= 0) {
              defaultAnimation = val.name
            }
          })
          //animationState.setAnimation(0, spine_cysp.getClass(spine_cysp.currentClass) + '_idle', true);
          animationState.setAnimation(0, defaultAnimation, true);
          animationState.addListener({
            /*start: function (track) {
              console.log("Animation on track " + track.trackIndex + " started");
            },
            interrupt: function (track) {
              console.log("Animation on track " + track.trackIndex + " interrupted");
            },
            end: function (track) {
              console.log("Animation on track " + track.trackIndex + " ended");
            },
            disposed: function (track) {
              console.log("Animation on track " + track.trackIndex + " disposed");
            },*/
            complete: function tick(track) {
              //console.log("Animation on track " + track.trackIndex + " completed");
              if (spine_cysp.animationQueue.length) {
                var nextAnim = spine_cysp.animationQueue.shift();
                if (nextAnim == 'stop') return;
                if (nextAnim == 'hold') return setTimeout(tick, 1e3);
                if (nextAnim.substr(0, 1) != '1') nextAnim = spine_cysp.getClass(spine_cysp.currentClassAnimData.type) + '_' + nextAnim;
                console.log(nextAnim);
                animationState.setAnimation(0, nextAnim, !spine_cysp.animationQueue.length);
              }
            },
            /*event: function (track, event) {
              console.log("Event on track " + track.trackIndex + ": " + JSON.stringify(event));
            }*/
          });
  
          spine_cysp.Skeleton = { skeleton: skeleton, state: animationState, bounds: bounds, premultipliedAlpha: true }
          spine_cysp.loading = false;
          (spine_cysp.setupAnimationUI() || spine_cysp.setupUI)();
          if (!created) {
            spine_cysp.canvas.style.width = '99%';
            requestAnimationFrame(spine_cysp.render);
            setTimeout(function () {
              spine_cysp.canvas.style.width = '';
            }, 0)
          }
        }
        img.src = URL.createObjectURL(blob);
      }, 'blob', function (e) {
        var perc = e.loaded / e.total * 40 + 60;
      });
    })
  },
  calculateBounds: function(skeleton) {
    skeleton.setToSetupPose();
    skeleton.updateWorldTransform();
    var offset = new spine.Vector2();
    var size = new spine.Vector2();
    skeleton.getBounds(offset, size, []);
    offset.y = 0
    return { offset: offset, size: size };
  },
  setupAnimationUI: function () {
    var animationList = $("#animationList");
    animationList.empty();
    var skeleton = spine_cysp.Skeleton.skeleton;
    var state = spine_cysp.Skeleton.state;
    var activeAnimation = state.tracks[0].animation.name;
    
    [
      ['기본', 'idle'],
      ['준비완료', 'standBy'],
      ['걷기', 'walk'],
      ['뛰기', 'run'],
      ['게임시작', 'run_gamestart'],
      ['착지', 'landing'],
      ['공격', 'attack'],
      ['스킵티켓모션', 'attack_skipQuest'],
      ['짧은축하', 'joy_short,hold,joy_short_return'],
      ['축하', 'joy_long,hold,joy_long_return'],
      ['데미지', 'damage'],
      ['사망', 'die,stop'],
      ['준비완료(멀티)', 'multi_standBy'],
      ['준비중(멀티)', 'multi_idle_standBy'],
      ['기본(무장해제)', 'multi_idle_noWeapon']
    ].forEach(function (i) {
      animationList.append(new Option(i[0], i[1]));
    });
    animationList.append(new Option('-----'));
    skeleton.data.animations.forEach(function (i) {
      i = i.name;
      if (!/^\d{6}_/.test(i)) return;
      var val = i;
      if (/joyResult/.test(i)) val = i + ',stop';
      animationList.append(new Option(i.replace(/\d{6}_skill(.+)/, '스킬$1').replace(/\d{6}_joyResult/, '고유승리포즈'), val));
    })
  },
  setupUI: function() {
    $("#animationList").on('change', function () {
      var skeleton = spine_cysp.Skeleton;
      var animationState = skeleton.state, forceNoLoop = false;
      spine_cysp.animationQueue = $("#animationList")[0].value.split(',');
      if (spine_cysp.animationQueue[0] == 'multi_standBy') {
        spine_cysp.animationQueue.push('multi_idle_standBy');
      } else if ([
        'multi_idle_standBy', 'multi_idle_noWeapon', 'idle', 'walk', 'run', 'run_gamestart'
      ].indexOf(spine_cysp.animationQueue[0]) == -1) {
        spine_cysp.animationQueue.push('idle');
      }
      console.log(spine_cysp.animationQueue);
      var nextAnim = spine_cysp.animationQueue.shift();
      if (nextAnim.substr(0, 1) != '1') nextAnim = spine_cysp.getClass(spine_cysp.currentClassAnimData.type) + '_' + nextAnim;
      console.log(nextAnim);
      animationState.setAnimation(0, nextAnim, !spine_cysp.animationQueue.length && !forceNoLoop);
    });
    
    $(spine_cysp.canvas).on('click', function() {
      var skeleton = spine_cysp.Skeleton;
      var animationState = skeleton.state, forceNoLoop = false;
      
      spine_cysp.animationQueue = $('#animationList option:selected').next().val().split(',');
      console.log(spine_cysp.animationQueue);
      $("#animationList").val($('#animationList option:selected').next().val());
      
      if (spine_cysp.animationQueue[0] == 'multi_standBy') {
        spine_cysp.animationQueue.push('multi_idle_standBy');
      } else if ([
        'multi_idle_standBy', 'multi_idle_noWeapon', 'idle', 'walk', 'run', 'run_gamestart'
      ].indexOf(spine_cysp.animationQueue[0]) == -1) {
        spine_cysp.animationQueue.push('idle');
      }
      console.log(spine_cysp.animationQueue);
      var nextAnim = spine_cysp.animationQueue.shift();
      if (nextAnim.substr(0, 1) != '1') nextAnim = spine_cysp.getClass(spine_cysp.currentClassAnimData.type) + '_' + nextAnim;
      console.log(nextAnim);
      animationState.setAnimation(0, nextAnim, !spine_cysp.animationQueue.length && !forceNoLoop);
      
    });
  
    spine_cysp.setupAnimationUI();
  },
  render: function() {
    var now = Date.now() / 1000;
    var delta = now - spine_cysp.lastFrameTime;
    spine_cysp.lastFrameTime = now;
    delta *= spine_cysp.speedFactor;
  
    // Update the MVP matrix to adjust for canvas size changes
    spine_cysp.resize();
  
    spine_cysp.gl.clearColor(spine_cysp.bgColor[0], spine_cysp.bgColor[1], spine_cysp.bgColor[2], spine_cysp.bgColor[3]);
    spine_cysp.gl.clear(spine_cysp.gl.COLOR_BUFFER_BIT);
  
    // Apply the animation state based on the delta time.
    var state = spine_cysp.Skeleton.state;
    var skeleton = spine_cysp.Skeleton.skeleton;
    var bounds = spine_cysp.Skeleton.bounds;
    var premultipliedAlpha = spine_cysp.Skeleton.premultipliedAlpha;
    state.update(delta);
    state.apply(skeleton);
    skeleton.updateWorldTransform();
  
    // Bind the shader and set the texture and model-view-projection matrix.
    spine_cysp.shader.bind();
    spine_cysp.shader.setUniformi(spine.webgl.Shader.SAMPLER, 0);
    spine_cysp.shader.setUniform4x4f(spine.webgl.Shader.MVP_MATRIX, spine_cysp.mvp.values);
  
    // Start the batch and tell the SkeletonRenderer to render the active skeleton.
    spine_cysp.batcher.begin(spine_cysp.shader);
  
    spine_cysp.skeletonRenderer.premultipliedAlpha = premultipliedAlpha;
    spine_cysp.skeletonRenderer.draw(spine_cysp.batcher, skeleton);
    spine_cysp.batcher.end();
  
    spine_cysp.shader.unbind();
  
    requestAnimationFrame(spine_cysp.render);
  },
  resize: function() {
    var useBig = screen.width * devicePixelRatio > 1280;
    //var w = useBig ? 1920 : 1280;
    //var h = useBig ? 1080 : 720;
    /*
    var w = canvas.clientWidth * devicePixelRatio;
    var h = canvas.clientHeight * devicePixelRatio;
    */
    var w = $('.preCanvas').width();
    var h = $('.preCanvas').height();
    
    var bounds = spine_cysp.Skeleton.bounds;
    if (spine_cysp.canvas.width != w || spine_cysp.canvas.height != h) {
      spine_cysp.canvas.width = w;
      spine_cysp.canvas.height = h;
    }
    
    // magic
    var centerX = bounds.offset.x + bounds.size.x / 2;
    var centerY = bounds.offset.y + bounds.size.y / 2;
    var scaleX = bounds.size.x / spine_cysp.canvas.width;
    var scaleY = bounds.size.y / spine_cysp.canvas.height;
    var scale = Math.max(scaleX, scaleY) * 0.7;
    
    if (scale < 1) scale = 1;
    var width = spine_cysp.canvas.width * scale;
    var height = spine_cysp.canvas.height * scale;
  
    spine_cysp.mvp.ortho2d(centerX - width / 2, centerY - height / 1.2, width, height);
    spine_cysp.gl.viewport(0, 0, spine_cysp.canvas.width, spine_cysp.canvas.height);
  }
}

var spine_full = {
  getClass: function(i){
    return (i < 10 ? '0' : '') + i;
  },
  loadData: function(url, cb, loadType, progress) {
    var xhr = new XMLHttpRequest;
    xhr.open('GET', url, true);
    if (loadType) xhr.responseType = loadType;
    if (progress) xhr.onprogress = progress;
    xhr.onload = function () {
      if (xhr.status == 200)
        cb(true, xhr.response);
      else
        cb(false);
    }
    xhr.onerror = function () {
      cb(false);
    }
    xhr.send();
  },
  init: function() {
    spine_full.lastFrameTime= Date.now() / 1000;
    spine_full.canvas= null;
    spine_full.shader= null;
    spine_full.batcher= null;
    spine_full.gl=null;
    spine_full.mvp= new spine.webgl.Matrix4();
    spine_full.skeletonRenderer= null;
    spine_full.shapes= null;
    spine_full.Skeleton= {};
    spine_full.animationQueue=[];
    spine_full.speedFactor= 1;
    spine_full.bgColor= [1,1,1,0];
    
    
    spine_full.canvas = document.getElementById("fullcanvas");
    var config = { alpha: false };
    spine_full.gl = spine_full.canvas.getContext("webgl", config) || spine_full.canvas.getContext("experimental-webgl", config);
    if (!spine_full.gl) {
      alert('WebGL is unavailable.');
      return;
    }
  
    // Create a simple shader, mesh, model-view-projection matrix and SkeletonRenderer.
    spine_full.shader = spine.webgl.Shader.newTwoColoredTextured(spine_full.gl);
    spine_full.batcher = new spine.webgl.PolygonBatcher(spine_full.gl);
    spine_full.mvp.ortho2d(0, 0, spine_full.canvas.width - 1, spine_full.canvas.height - 1);
    spine_full.skeletonRenderer = new spine.webgl.SkeletonRenderer(spine_full.gl);
    spine_full.shapes = new spine.webgl.ShapeRenderer(spine_full.gl);
  },

  loading: false,
  rawSkeletonData: null,
  currentTexture: null,

  load: function(unit_id, isstory) {
    var directory = 'spine/full/spine_';
    
    if(typeof isstory !== 'undefined') {
      unit_id += isstory-1;
      directory = 'storydata/spine/full/spine_'
    }
    else {
      unit_id += 10;
    }
    if (spine_full.loading) return;
    spine_full.loading = true;
    
    
    
    spine_full.loadData('data/img/' + directory + unit_id + '/' + unit_id + '.skel', function (success, json) {
      rawSkeletonData = json;
      spine_full.loadTexture(unit_id, directory);
    }, 'arraybuffer');
  },
  loadTexture: function(unit_id, directory) {
    spine_full.loadData('data/img/' + directory + unit_id + '/' + unit_id + '.atlas', function (success, atlasText) {
      spine_full.loadData('data/img/' + directory + unit_id + '/' + unit_id + '.png', function (success, blob) {
        if (!success) return spine_full.loading = false;
        var img = new Image();
        img.onload = function () {
          var created = !!spine_full.Skeleton.skeleton;
          if (created) {
            spine_full.Skeleton.state.clearTracks();
            spine_full.Skeleton.state.clearListeners();
            spine_full.gl.deleteTexture(currentTexture.texture)
          }
  
          var imgTexture = new spine.webgl.GLTexture(spine_full.gl, img);
          URL.revokeObjectURL(img.src);
          atlas = new spine.TextureAtlas(atlasText, function (path) {
            return imgTexture;
          });
          currentTexture = imgTexture;
          atlasLoader = new spine.AtlasAttachmentLoader(atlas);
  
          var skeletonBinary = new spine.SkeletonBinary(atlasLoader);
          var skeletonData = skeletonBinary.readSkeletonData(rawSkeletonData);
          var skeleton = new spine.Skeleton(skeletonData);
          
          skeleton.setSkinByName('normal');
          var bounds = spine_full.calculateBounds(skeleton);
  
          animationStateData = new spine.AnimationStateData(skeleton.data);
          var animationState = new spine.AnimationState(animationStateData);
          animationState.setAnimation(0, 'eye_idle', true);
          animationState.addListener({
            /*start: function (track) {
              console.log("Animation on track " + track.trackIndex + " started");
            },
            interrupt: function (track) {
              console.log("Animation on track " + track.trackIndex + " interrupted");
            },
            end: function (track) {
              console.log("Animation on track " + track.trackIndex + " ended");
            },
            disposed: function (track) {
              console.log("Animation on track " + track.trackIndex + " disposed");
            },*/
            complete: function tick(track) {
              //console.log("Animation on track " + track.trackIndex + " completed");
              if (spine_full.animationQueue.length) {
                var nextAnim = spine_full.animationQueue.shift();
                if (nextAnim == 'stop') return;
                if (nextAnim == 'hold') return setTimeout(tick, 1e3);
                //if (nextAnim.substr(0, 1) != '1') nextAnim = getClass(currentClassAnimData.type) + '_' + nextAnim;
                animationState.setAnimation(0, nextAnim, !spine_full.animationQueue.length);
              }
            },
            /*event: function (track, event) {
              console.log("Event on track " + track.trackIndex + ": " + JSON.stringify(event));
            }*/
          });
  
          spine_full.Skeleton = { skeleton: skeleton, state: animationState, bounds: bounds, premultipliedAlpha: true }
          spine_full.loading = false;
          (spine_full.setupAnimationUI() || spine_full.setupUI)();
          if (!created) {
            spine_full.canvas.style.width = '99%';
            requestAnimationFrame(spine_full.render);
            setTimeout(function () {
              spine_full.canvas.style.width = '';
            }, 0)
          }
        }
        img.src = URL.createObjectURL(blob);
      }, 'blob', function (e) {
        var perc = e.loaded / e.total * 40 + 60;
      });
    })
  },  
  calculateBounds: function(skeleton) {
    skeleton.setToSetupPose();
    skeleton.updateWorldTransform();
    var offset = new spine.Vector2();
    var size = new spine.Vector2();
    skeleton.getBounds(offset, size, []);
    offset.y = 0
    return { offset: offset, size: size };
  },  
  setupAnimationUI: function () {
    var skinList = $("#fullanimation");
    skinList.empty();
    var skeleton = spine_full.Skeleton.skeleton;
    var state = spine_full.Skeleton.state;
    //var skinlist = skeleton.data.skins.map(function(val) { return val.name })
    [
      ['기본표정', 'normal'],
      ['화남', 'anger'],
      ['즐거움', 'joy'],
      ['슬픔', 'sad'],
      ['부끄러움', 'shy'],
      ['놀람', 'surprised']
    ].forEach(function (i) {
      skinList.append(new Option(i[0], i[1]));
    });
  },
  
  setupUI: function() {
    $("#fullanimation").on('change', function () {
      var skeleton = spine_full.Skeleton.skeleton;
      skeleton.setSkinByName($("#fullanimation").val());
    });
  
    spine_full.setupAnimationUI();
  },  
  render: function() {
    var now = Date.now() / 1000;
    var delta = now - spine_full.lastFrameTime;
    spine_full.lastFrameTime = now;
    delta *= spine_full.speedFactor;
  
    // Update the MVP matrix to adjust for canvas size changes
    spine_full.resize();
  
    spine_full.gl.clearColor(spine_full.bgColor[0], spine_full.bgColor[1], spine_full.bgColor[2], spine_full.bgColor[3]);
    spine_full.gl.clear(spine_full.gl.COLOR_BUFFER_BIT);
  
    // Apply the animation state based on the delta time.
    var state = spine_full.Skeleton.state;
    var skeleton = spine_full.Skeleton.skeleton;
    var bounds = spine_full.Skeleton.bounds;
    var premultipliedAlpha = spine_full.Skeleton.premultipliedAlpha;
    state.update(delta);
    state.apply(skeleton);
    skeleton.updateWorldTransform();
  
    // Bind the shader and set the texture and model-view-projection matrix.
    spine_full.shader.bind();
    spine_full.shader.setUniformi(spine.webgl.Shader.SAMPLER, 0);
    spine_full.shader.setUniform4x4f(spine.webgl.Shader.MVP_MATRIX, spine_full.mvp.values);
  
    // Start the batch and tell the SkeletonRenderer to render the active skeleton.
    spine_full.batcher.begin(spine_full.shader);
  
    spine_full.skeletonRenderer.premultipliedAlpha = premultipliedAlpha;
    spine_full.skeletonRenderer.draw(spine_full.batcher, skeleton);
    spine_full.batcher.end();
  
    spine_full.shader.unbind();
  
    requestAnimationFrame(spine_full.render);
  },
  resize: function() {
    var useBig = screen.width * devicePixelRatio > 1280;
    //var w = useBig ? 1920 : 1280;
    //var h = useBig ? 1080 : 720;
    /*
    var w = spine_full.canvas.clientWidth * devicePixelRatio;
    var h = spine_full.canvas.clientHeight * devicePixelRatio;
    */
    
    
    var w = $('.prefullcanvas').width();
    var h = $('.prefullcanvas').height();
    
    var bounds = spine_full.Skeleton.bounds;
    if (spine_full.canvas.width != w || spine_full.canvas.height != h) {
      spine_full.canvas.width = w * devicePixelRatio;
      spine_full.canvas.height = h * devicePixelRatio;
    }
    
    // magic
    var centerX = bounds.offset.x + bounds.size.x / 2;
    var centerY = bounds.offset.y + bounds.size.y / 2;
    var scaleX = bounds.size.x / spine_full.canvas.width;
    var scaleY = bounds.size.y / spine_full.canvas.height;
    var scale = Math.max(scaleX, scaleY) * 1;
    
    //if (scale < 1) scale = 1;
    var width = spine_full.canvas.width * scale;
    var height = spine_full.canvas.height * scale;
  
    spine_full.mvp.ortho2d(centerX - width / 2, centerY - height / 2, width, height);
    spine_full.gl.viewport(0, 0, spine_full.canvas.width, spine_full.canvas.height);

    $(spine_full.canvas).css("width", w)
    $(spine_full.canvas).css("height", h)
  }
}