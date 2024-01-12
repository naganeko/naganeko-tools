(ns renden.spine36
 (:require
   [renden.utils :as u]
   )
(:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
    ))

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
            (let [skel_bin (js/SkeletonBinary3.)
                  ar (js/Uint8Array. (.-data (aget rsc (str k "-skel"))))]
              (aset skel_bin "scale" (or js/window.skeleton_scale 1.0))
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
                      ;(println "line=" line)
                      (cb (new js/PIXI.BaseTexture png-data2))
                      ;(cb (new js/PIXI.BaseTexture.fromImage line))
                      ))
                  (fn [s]
                    (.log js/console "atlas parsing complete:" s)
                    (set! js/sd_atlas s)
                    (js/fn_cb_atlas_after_load s)
                    ))
          spineAtlasLoader (js/PIXI.spine.core.AtlasAttachmentLoader. atlas)

          skelkey (str key "-skel")
          skel-v (aget rsc skelkey)
          data (if skel-v                                   ; skel
                 (let [
                       parser (js/PIXI.spine.core.SkeletonJson. spineAtlasLoader)
                       skd0 (load-skel key)
                       skd (apply-filter! (append-setup-pose! skd0))
                       ]
                   ;(aset parser "scale" (or js/window.skeleton_scale 1.0))
                   (when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
                   (.readSkeletonData parser skd
                     ))
                 (let [
                       parser (js/PIXI.spine.core.SkeletonJson. spineAtlasLoader)
                       skd0 (.parse js/JSON (.-data (aget rsc (str key "-json"))))
                       skd (apply-filter! (append-setup-pose! skd0))
                       ]
                   ;(aset parser "scale" (or js/window.skeleton_scale 1.0))
                   (when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
                   (.readSkeletonData parser skd
                     ))
                 )
          ]
      (set! js/sd_atlas atlas)
      (.log js/console "sd_atlas" atlas)
      ;(when js/hold_skel_json (set! js/skel_json skd)) ; json 파일입력도 저장은 한다
      (! data.skin skin)
      data)))

;

(if-not js/fn_skeleton_filter
  (set! js/fn_skeleton_filter (fn [sk] sk)))


; fn_skeleton_filter = function(s){d=s.animations.die;if(!d||!d){return s};var sl=d.slots;Object.keys(sl).filter(n=>/eye/i.test(n)).forEach(n=> delete sl[n]);return s;}


(defn sort-edges [edges]
  (->>
    (partition 2 edges)
    (map sort)
    (map #(apply vector %))
    (sort compare)
    ;flatten
    ;(map #(* 2 %))
    ))


(defn gen-edges [triangles]
  (->>
    triangles
    ;(map #(* % 2))
    (partition 3)
    (mapcat (fn [[a b c]] [[a b] [b c] [c a]]))
    (map sort)
    (map #(apply vector %))
    (frequencies)
    (filter (fn [[k v]] (= v 1)))
    (map first)
    (sort compare)
    flatten
    (map #(* 2 %))
    ))

(defn check1 [o]
  (if o
    (cond (object? o)
          (mapv check1 (.values js/Object o))
          (array? o)
          (do
            (if (<= 38 (.-length o) (.log js/console o))
              (mapv check1 o)))
          :else nil)))

(defn fix-curve33 [mp]
  (if mp
    (cond (object? mp)
          (doseq [k (js-keys mp)]
            (if (= k "curve")
              (if (= "linear" (aget mp k))
                (js-delete mp "curve"))
              (fix-curve33 (aget mp k))))
          (array? mp) (mapv fix-curve33 mp)
    :else nil)))

(defn fix-num-vals [obj k v]
  (cond (nil? v) nil
        (or (object? v) (array? v))
        (mapv #(fix-num-vals v % (aget v %)) (js-keys v))
        (and (number? v) (not (js/isNaN v)))
        (aset obj k (/ (Math/round (* v 100000)) 100000))
        :else nil))

(defn del-default-kv [obj v k]
  (if (= v (aget obj k))
    (js-delete obj k)))
(defn del-kv [obj k]
    (js-delete obj k))

(defn optimize-bones [bones]
  (mapv
    (fn [b]
      (del-default-kv b 1 "scaleX")
      (del-default-kv b 1 "scaleY")
      (del-default-kv b 0 "shearX")
      (del-default-kv b 0 "shearY")
      ) bones))

(defn optimize-slots [slots]
  (mapv
    (fn [r]
      (del-kv r "dark")
      ) slots))

(defn optimize-skins [skins]
  (doseq [skin-vs (.values js/Object skins)] ; e.g. default
    (doseq [skin-v (.values js/Object skin-vs)] ; e.g. v of hair_B2 skin
      (doseq [a-nm (js-keys skin-v)]
        (let [att (aget skin-v a-nm)]                          ; e.g. attachments of hair_B2
          (do
            (when (= "mesh" (.-type att))
              (if-let [region (aget js/attachments_map a-nm "region")]
                (do
                  (aset att "width" (.-width region))
                  (aset att "height" (.-height region))
                  )))
            (del-default-kv att 1 "x")
            (del-default-kv att 1 "y")
            (del-default-kv att 1 "scaleX")
            (del-default-kv att 1 "scaleY")
            (del-default-kv att "ffffffff" "color")
            (let [triangles (aget att "triangles")
                  edges (aget att "edges")]
              (if (and triangles (nil? edges))
                (aset att "edges" (clj->js (gen-edges triangles))))
              )
            ))))))

(defn cv4sp33! [json-obj]
  (let [anis (? json-obj.animations)
        events (? json-obj.events)
        slots (aget json-obj "slots")
        skins (aget json-obj "skins")
        bones (aget json-obj "bones")
        ]

    (if events
      (doseq [ev (.values js/Object events)]
        (js-delete ev "audio")
        (js-delete ev "volume")
        (js-delete ev "balance")))

    ; TODO
    ; skin slot의 어태치먼트가
    ; point , clipping 이면 nil 처리 3.3에 없음
    ; twoColor 애니메이션 삭제

    (fix-curve33 anis)

    (optimize-bones bones)
    (optimize-slots slots)
    (optimize-skins skins)

    (if anis
      (doseq [ani-v (.values js/Object anis)]
        (js-delete ani-v "twoColor")))

    (mapv
      #(let [ba %
             t (clojure.string/lower-case (or (.-transform ba) "normal"))
             name (.-name ba)]
         (cond
           (= t "onlytranslation")
           (mapv (fn [k] (aset ba k false)) ["inheritScale" "inheritRotation"])
           (= t "norotationorreflection")
           (aset ba "inheritRotation" false)
           (or (= t "noscale") (= t "noscaleorreflection"))
           (aset ba "inheritScale" false)
           ; normal
           :else nil)
         ;(js-delete ba "transform")
          )
      bones)
    ;            TransformMode[TransformMode["Normal"] = 0] = "Normal";
    ;            TransformMode[TransformMode["OnlyTranslation"] = 1] = "OnlyTranslation";
    ;            TransformMode[TransformMode["NoRotationOrReflection"] = 2] = "NoRotationOrReflection";
    ;            TransformMode[TransformMode["NoScale"] = 3] = "NoScale";
    ;            TransformMode[TransformMode["NoScaleOrReflection"] = 4] = "NoScaleOrReflection";

    ;(! json-obj.skeleton.spine "3.3")
    (! json-obj.skeleton.spine "3.5")
    (! json-obj.skeleton.chibi_gif (str "chibi_gif_" js/chibi_gif_version))

    ;(fix-num-vals nil nil json-obj)

    nil))
