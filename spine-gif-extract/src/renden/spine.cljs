(ns renden.spine
  (:require
    [renden.utils :as u]
    [clojure.string :as str]
  )
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
    ))

(defonce blank-skeleton #js {:bones [] :slots []  :ikConstraints []})

(defn lk-slot-obj [doll name]
  ;(println "name:" name)
  (if-let [slot (.findSlot (aget doll "skeleton") name)]
    (do
      ;(println slot)
      (let [sprites  (aget slot "sprites")
            meshes (aget slot "meshes")
            cs (aget slot "currentSprite")
            cm (aget slot "currentMesh")
            ]
        (cond
          (and sprites (aget sprites name)) #js [(aget sprites name) "sprite"]
          (and meshes (aget meshes name)) #js [(aget meshes name) "meshes"]
          cs #js [cs "sprite"]
          cm #js [cm "meshes"]
          :else nil
          )))
    (println name " NOT FOUND")
    ))

(def t-bone-v
  (clj->js
    {:rotate [{:time 0, :angle 0, :curve "stepped"} {:time 1, :angle 0}],
     :translate
     [{:time 0, :x 0, :y 0, :curve "stepped"}
      {:time 1, :x 0, :y 0}
      ],
     :scale
     [{:time 0, :x 1, :y 1, :curve "stepped"}
      {:time 1, :x 1, :y 1}
      ]})
  )
           ;:slots {},
           ;:ik {},
           ;:ffd {},
           ;:drawOrder [{:offsets [], :time 0}]))
(def t-setup-pose
         (obj
           :bones
           {:root
            {:rotate [{:time 0, :angle 0, :curve "stepped"} {:time 1, :angle 0}],
             :translate
                     [{:time 0, :x 0, :y 0, :curve "stepped"} {:time 1, :x 0, :y 0}],
             :scale
                     [{:time 0, :x 1, :y 1, :curve "stepped"} {:time 1, :x 1, :y 1}]}},
           :slots {},
           :ik {},
           :ffd {},
           :drawOrder [{:offsets [], :time 0}]))

(defn set-ani!
  ([doll action]
   (set-ani! doll action true))
  ([doll action loop]
   (let [state (.-state doll)]
     (.setAnimationByName state 0 (name action) loop)
     doll
     )))

(defn pos [doll]
  (let [p (.-position doll)]
    {:x (.-x p) :y (.-y p)}))

(defn set-pos! [doll x y]
  (let [p (.-position doll)]
    (.set p x y) doll))

(def SCALE 1.0)

(defn gen-doll [skel]
  ;(let [d (js/PIXI.spine.Spine. skel)]
  (let [d (new js/TDoll skel)]
    (if-let [skin (? skel.skin)] (! d.skin skin))
    (!> d.scale.set SCALE)
    d))

(defn add-data
  ([ldr key folder prefix json?]
    ;(println "add-data" key)
   (letfn [(add [postfix ext typ]
             (.add ldr
                   (str key "-" postfix)
                   (str "./character/" folder "/" prefix "." ext)
                   #js {:xhrType typ}))]
     (if json?
       (add "json" "json" "text")
       (add "skel" "skel" "arraybuffer"))
     (add "atlas" "atlas" "text")
     (add "png" "png" "png")
     ))
  ([ldr key] (add-data ldr key key key false))
  ([ldr key flr] (add-data ldr key flr key false))
  ([ldr key flr prefix] (add-data ldr key flr prefix false))
  )

(defn append-setup-pose! [js-json]
  (set! js/pseudo_setup_pose_added false)
  (when (nil? (? js-json.animations.pseudo_setup_pose))
    ;(! js-json.animations.pseudo_setup_pose t-setup-pose))
    (let [bones (obj)
          bone-names0 (map #(aget % "name") (aget js-json "bones"))
          bone-names (sort (filter #(re-find (js/RegExp "(body|arm|leg|head|eye)" "i") %) bone-names0))
          ;bone-names (sort bone-names0)
          ]
      (set! js/pseudo_setup_pose_added true)
      (doseq [n bone-names]
        (aset bones n t-bone-v))
      (->>
        #js {:bones bones
             :slots #js {},
             :ik #js {}, :ffd #js {},
             :drawOrder #js [ #js {:offsets #js [], :time 0}]
             }
        ;(#(aset % "bones" bones))
        js/JSON.stringify
        js/JSON.parse
        (#(! js-json.animations.pseudo_setup_pose %))
        )))
  js-json
  )

(defn apply-filter! [sk-jsn]
  (let [
        bs (aget sk-jsn "bones")
        sl (aget sk-jsn "slots")
        ani (aget sk-jsn "animations")
        skn (aget sk-jsn "skins")
        ]

    ;function fn_bones_filter(slots, skel) {}
    ;function fn_slots_filter(bones, skel) {}
    ;function fn_animations_filter(animations, skel) {}
    (js/fn_skins_filter skn sk-jsn)
    (js/fn_slots_filter sl sk-jsn bs)
    (js/fn_bones_filter bs sk-jsn)
    (js/fn_animations_filter ani sk-jsn bs)
    (js/fn_skeleton_filter sk-jsn bs sl ani skn))
  )

(defn gen-skel [ldr rsc key skin]
  (letfn [(load-skel [k]
            (let [skel_bin (js/SkeletonBinary.)
                  ar (js/Uint8Array. (.-data (aget rsc (str k "-skel"))))]
              (aset skel_bin "data" ar)
              (.initJson skel_bin)
              (.-json skel_bin)
              ))
          ]
    (let [
          rad (.-data (aget rsc (str key "-atlas")))
          ;png-data (.-data (aget rsc (str key "-png")))
          png-data (if-let [png-rsc (aget rsc (str key "-png"))] (.-data png-rsc))
          pma-ka  (fn [d] (if js/window.skip_unpma d (u/unpma d)))
          skelkey (str key "-skel")
          skel-v (aget rsc skelkey)
          ;skin (? skel-v.metadata.skin)
          skd0 (if skel-v
                (let [json (load-skel key)] ; binary
                  ;(when js/hold_skel_json (set! js/skel_json json))
                  json)
                (.parse js/JSON (.-data (aget rsc (str key "-json"))))) ; json
          skd (apply-filter! (append-setup-pose! skd0))
          ;skd skd0
          atlas (js/PIXI.spine.SpineRuntime.Atlas.
                  rad
                  ; iterateParser가 사용하는 loaderFunction(line, function (texture)
                  ; 인자 2개
                  (fn [line cb]
                    (do
                      ;(.log js/console "line=>" line "<")
                      ;(.log js/console cb)
                      (if png-data
                        (cb (new js/PIXI.BaseTexture (pma-ka png-data)))
                        (let [k (-> line str/trim str/lower-case)
                              png-data (.-data (aget rsc k))]
                          (if png-data
                            (cb (new js/PIXI.BaseTexture (pma-ka png-data)))
                            (do
                              (js/alert (str "file: " line " not found or cannot be loaded"))
                              )))
                        )))
                  (fn [s]
                    ;(.log js/console "atlas parsing complete:" s)
                    ;(set! js/sd_atlas s)
                    (js/fn_cb_atlas_after_load s)
                    ))
          p (js/PIXI.spine.SpineRuntime.AtlasAttachmentParser. atlas)
          jp (js/PIXI.spine.SpineRuntime.SkeletonJsonParser. p)
          data (. jp readSkeletonData skd key)
          ]
      (aset atlas "_skip_unpma" (true? js/window.skip_unpma))
      (aset data "_sp_atlas" atlas)
      (set! js/sd_atlas atlas)
      (when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
      (! data.skin skin)
      (if-not js/window.skels (set! js/window.skels #js []))
      (.push js/window.skels data)
      ;(aset data )
      data)))

;

(if-not js/fn_skeleton_filter
  (set! js/fn_skeleton_filter (fn [sk] sk)))

;(when-not (.getElementById js/document "png_wrk_canvas")
;  (let [cvs (.createElement js/document "canvas")
;        body (aget (.getElementsByTagName js/document "body") 0)]
;    (.setAttribute cvs "id" "png_wrk_canvas")
;    (.setAttribute cvs "style" "display:none")
;    (.append body cvs)
;    (set! js/window.png_wrk_canvas cvs)
;    (set! js/window.png_wrk_ctx (.getContext cvs "2d"))
;    ))
; fn_skeleton_filter = function(s){d=s.animations.die;if(!d||!d){return s};var sl=d.slots;Object.keys(sl).filter(n=>/eye/i.test(n)).forEach(n=> delete sl[n]);return s;}

