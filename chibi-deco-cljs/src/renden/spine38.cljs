(ns renden.spine38
  (:require
    [renden.utils :as u]
    )
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
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
  (let [d (js/PIXI.spine.Spine. skel)]
    ;(let [d (new js/TDoll skel)]
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
          png-data (.-data (aget rsc (str key "-png")))
          png-data2 (u/unpma png-data)
          atlas (js/PIXI.spine.core.TextureAtlas.
                  rad
                  (fn [line cb]
                    (do
                      (println "line=" line)
                      (cb (new js/PIXI.BaseTexture png-data2))
                      ;(cb (new js/PIXI.BaseTexture.fromImage line))
                      )))
          spineAtlasLoader (js/PIXI.spine.core.AtlasAttachmentLoader. atlas)

          skelkey (str key "-skel")
          skel-v (aget rsc skelkey)
          data (if skel-v
                 (let [parser (js/PIXI.spine.core.SkeletonJson. spineAtlasLoader)
                       skd (js/Uint8Array.
                              (.-data (aget rsc (str key "-skel"))))
                       ]
                   (aset parser "scale" (or js/window.skeleton_scale 1.0))
                   (.readSkeletonData parser skd))
                 ;(->
                 ;  (js/PIXI.spine.core.SkeletonBinary. spineAtlasLoader)
                 ;  (.readSkeletonData
                 ;    (js/Uint8Array.
                 ;      (.-data (aget rsc (str key "-skel"))))
                 ;    )
                 ;  )
                 (let [
                       parser (js/PIXI.spine.core.SkeletonJson. spineAtlasLoader)
                       skd0 (.parse js/JSON (.-data (aget rsc (str key "-json"))))
                       skd (apply-filter! (append-setup-pose! skd0))
                       ]
                   (aset parser "scale" (or js/window.skeleton_scale 1.0))
                   (when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
                   (.readSkeletonData parser skd
                     )) )
          ]
      (set! js/sd_atlas atlas)
      ;(when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
      (! data.skin skin)
      data)))

;

(if-not js/fn_skeleton_filter
  (set! js/fn_skeleton_filter (fn [sk] sk)))


; fn_skeleton_filter = function(s){d=s.animations.die;if(!d||!d){return s};var sl=d.slots;Object.keys(sl).filter(n=>/eye/i.test(n)).forEach(n=> delete sl[n]);return s;}

