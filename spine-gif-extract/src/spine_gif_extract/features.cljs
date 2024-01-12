(ns spine-gif-extract.features
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
               [cljs.core.async.macros :only [go]])
  (:require
    [domina :as dom]
    [goog.string :as gstr]
    [goog.string.format]
    [clojure.string :as str]
    [cljs-time.core :as dt]
    [cljs-time.format :as df]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]
    [renden.spine :as sp])
  )


(defn atlas-4x [upscale lines]
  (let [chk? (fn [a b]
               (and
                 (not (nil? b))
                 ;(= 2 (count lst))
                 (re-find #"(size|orig|xy|offset)" a)
                 ))
        double (fn [s]
                 (->> s
                   (#(str/split % ","))
                   (map str/trim)
                   (map js/parseFloat)
                   (map #(* upscale %))
                   )
                 )
        ]
    (map
      (fn [line]
        (let [[a b] (str/split line ":")]
          (if
            (chk? a b)
            (let [d (double b)]
              (str a ": " (str/join "," d))
              )
            line)))
      lines)
    ))

(defn saveas-string [string name]
  (->
    ;(js/JSON.stringify js-obj nil 2)
    string
    (js/Array)
    (js/Blob. {:type "text/plain;charset=utf-8"})
    (js/saveAs name)
    ))

(defn do4x [filename url]
  (go
    (let [res (<! (http/get
                    url    ;"blob:http://localhost:3459/88508115-65a9-4db1-9860-377a0ec54148"
                    {}))]
      (let [body (:body res)
            lines (str/split body #"\n")
            new-name (str/replace filename ".atlas" "_4x.atlas")
            new-str (str/join "\n" (atlas-4x 4 lines))
            ]
        (saveas-string (str new-str "\n") new-name)
        lines
        ))))

(defn gen-4x [upscale-str filename url]
  (js/http_get url
    (fn [body]
      (let [
            ;body (:body res)
            new-name (str/replace filename ".atlas" (str "_" upscale-str "x.atlas"))
            lines (str/split body #"\n")
            upscale (js/parseFloat upscale-str)
            new-str (str/join "\n" (atlas-4x upscale lines))
            ]
        (saveas-string (str new-str "\n") new-name)
        ;lines
        nil
        )
      )))

(defn js-vals [o]
  (mapv #(aget o %)
    (.keys js/Object o)))

(defn remove-parent-null [o]
  (when o
    (cond
      (array? o) (mapv remove-parent-null o)
      (object? o)
      (doseq [k (js-keys o)]
        (let [v (aget o k)] (if (and (= "parent" k) (nil? v)) (js-delete o k) (remove-parent-null v))))
      :else o)))


(defn dig [o f]
  (letfn [(dig0 [k0 o]
           (when o
             (cond
               (array? o) (map-indexed dig0 o)
               (object? o)
               (doseq [k (js-keys o)]
                 (let [v (aget o k)]
                   (cond
                     ;(f? k v o)
                     (and (= "skinnedmesh" v)
                       (= "type" k))
                     (do
                       ;(println k)
                       (f k v o k0))
                     :else (dig0 k v)
                     )))
               :else o)))]
    (dig0 nil o)))

(defn lookup-atlas-region [name]
  (let [sk (js/_sk)
        regions (? sk.data._sp_atlas.regions)
        ]
    (first (filter #(= name (aget % "name")) regions))
    ;regions
    ))

(defn gen-edges [h]
  (let [ar #js []
        h2 (* (dec h) 2)]
    (doseq [i (range 0 h2 2)]
      (.push ar i)
      (.push ar (+ 2 i)))
    (.push ar 0)
    (.push ar h2)
    ar))

(defn add-whd [k v o k0]
  (let [mesh-name k0
        hull (aget o "hull")
        edges (aget o "edges")
        rgn (lookup-atlas-region mesh-name)
        w (aget rgn "width")
        h (aget rgn "height")
        ]
    (aset o "width" w)
    (aset o "height" h)
    (aset o "edges" (gen-edges hull))
    (.log js/console mesh-name "wh=" w h "h=" hull)
    ))

(defn fix-mesh [json-obj]
  ;(.log js/console json-obj)
  ;(.log js/console (aget json-obj "skeleton" "nonessential"))
   (if (false? (aget json-obj "skeleton" "nonessential"))
     (do
     (dig (aget json-obj "skins") add-whd)
     )
   (.warn js/console "nonessential flag must be false")
   ))

;(fix-skins (aget json-obj "skins"))

(defn fix-curve [mp]
  (if mp
    (let [fix
          (fn [[cx1 cy1 cx2 cy2]]
            (! mp.curve cx1)
            (! mp.c2 cy1)
            (! mp.c3 cx2)
            (! mp.c4 cy2))
          cv (aget mp "curve")]
      (cond
        (array? cv) (fix cv)
        (array? mp) (mapv fix-curve mp)
        (object? mp) (mapv fix-curve (js-vals mp))
        :else nil
        ))))

(defn farewell-to-flipx [nm ani]
  ;(println nm)
  (let [bones (? ani.bones)
        names (.keys js/Object bones)
        ]
    (doseq [n names]
      (let [b (aget bones n)
            r (aget js/_bonesRMap n)
            flipx? (? b.flipX.0.x)
            ]
        (when (aget b "flipY")
          (js-delete b "flipY"))
        (when flipx?
          (mapv
            (fn [timeline]
              (let [angle (? timeline.angle)
                    na0 (+ (-
                             (+ angle (* r 2)
                               360
                               -360
                               ))
                          ;180
                          -180
                          )
                    ;0)
                    na (cond
                         (< na0 -180) (+ na0 360)
                         (> na0 360) (- na0 360)
                         :else na0)
                    ;na na0
                    ]
                (! timeline.angle_old angle)
                (! timeline.angle na)
                ;(println nm n (? timeline.time) angle na r)
                ))
            (? b.rotate))
          (mapv
            (fn [timeline]
              ;(! timeline.x (- (? timeline.x)))
              (! timeline.y (- (? timeline.y)))
              )
            (? b.scale))
          )
        (js-delete b "flipX")
        ))
    ;(! ani.deform (aget ani "ffd"))
    ;(js-delete ani "ffd")
    )
  nil)

(defn fix-skins [skins-kv]                                  ; map
  (let [fix-ty
        (fn [kv]
          ;(println kv)
          ;(println (aget kv "type"))
          (when (= "skinnedmesh" (aget kv "type"))
            ;(aset kv "type" "linkedmesh")
            (aset kv "type" "mesh")
            ))
        fix-att-kv
        (fn [k att-kv]
          (mapv #(fix-ty (aget att-kv %))
            (.keys js/Object att-kv))
          ;(println k (.keys js/Object att-kv))
          )
        skn (.keys js/Object skins-kv)
        ]
    (mapv
      (fn [k]
        (let [skin (aget skins-kv k)]
          (mapv
            (fn [sn] (fix-att-kv sn (aget skin sn)))
            (.keys js/Object skin))
          ))
      skn)
    )
  nil)
(defn fix-skins-st [skel]
  (let [skins (aget skel "skins")
        ar #js []
        ns (.keys js/Object skins)
        ]
    (doseq [n ns]
      (.push ar
        #js {:name        n
             :attachments (aget skins n)
             })
      )
    (aset skel "skins" ar)
    (js-delete skel "skinsName"))
  nil)

(defn kill-empty-ar [obj k v]
  (if obj
    (cond
      (and (array? v) (empty? v))
      (js-delete obj k)
      (array? v)
      (mapv #(kill-empty-ar v % (aget v %)) (js-keys v))
      (object? v)
      (mapv #(kill-empty-ar v % (aget v %)) (js-keys v))
      :else nil
      )))
; 3.8은
; timeline의 키프레임이 짝수가 돼야 하는 듯? m200_s38.json
(defn remove-empty-timeline [anis]
  (doseq [ani (.values js/Object anis)]
    (kill-empty-ar true nil ani)
    ))

;; Spine 3.3 / 3.8 변환
(defn cv4spine3! [json-obj s38?]
  (set! js/_bonesRMap #js {})
  (mapv (fn [o]
          (do
            (aset js/_bonesRMap (? o.name) (? o.rotation))
            ))
    (aget json-obj "bones"))
  (let [anis (? json-obj.animations)
        names (.keys js/Object anis)
        ]
    ;(println names)
    (set! js/_names names)
    (remove-parent-null json-obj)
    (mapv
      #(farewell-to-flipx % (aget anis %))
      names)
    (when s38?
      (fix-skins (aget json-obj "skins"))                   ; for 3.6 에서 추가된 거
      (fix-skins-st json-obj)                               ; for 3.8 에서 추가된 거
      (fix-curve (aget json-obj "animations"))

      (remove-empty-timeline (aget json-obj "animations"))
      ;"flipX": false,
      ;"flipY": false,
      ;"inheritScale": true,
      ;"inheritRotation": true,
      ;
      ;(mapv (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY" "inheritScale" "inheritRotation"]))
      ;  (aget json-obj "bones"))

      ;(! json-obj.skeleton.lz "sh")
      (! json-obj.skeleton.spine "3.8")
      ;(! json-obj.skeleton.spine_gif (str "" js/spine_gif_version))
      )
    (when-not s38?
      ;"flipX": false,
      ;"flipY": false,
      ;"inheritScale": true,
      ;"inheritRotation": true,
      ;
      ;(mapv
      ;  (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY"]))
      ;  (aget json-obj "bones"))
      ;(mapv
      ;  (fn [{:keys [inheritScale inheritRotation] :as ba}]
      ;    (mapv #(js-delete ba %) ["inheritScale" "inheritRotation"])
      ;    (cond
      ;      (and inheritRotation (not inheritScale)) (aset ba "transform" "noScale")
      ;      (and (not inheritRotation) inheritScale) (aset ba "transform" "noRotationOrReflection")
      ;      (and (not inheritRotation) (not inheritScale)) (aset ba "transform" "onlyTranslation")
      ;      ;(and inheritRotation inheritScale ) (aset ba "transform" "normal")
      ;      ;:else (aset ba "transform" "onlyTranslation")) ; do nothing
      ;    nil))
      ;  (aget json-obj "bones"))
      (remove-empty-timeline (aget json-obj "animations"))
      ;(! json-obj.skeleton.lz "sh")
      (! json-obj.skeleton.spine "3.3")
      ;(! json-obj.skeleton.spine_gif (str "" js/spine_gif_version))
      )

    (defn get-kv-else [obj k d]
      (let [v (aget obj k)]
        (if (nil? v) d v)))

    (let [bones (aget json-obj "bones")]
      (mapv
        (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY"]))
        bones)
      (mapv
        #(let [ba %
               inheritScale (get-kv-else ba "inheritScale" true)
               inheritRotation (get-kv-else ba "inheritRotation" true)
               name (.-name ba)]
          ;(println name inheritScale inheritRotation ba)
          (if s38?
            (mapv (fn [k] (js-delete ba k)) ["inheritScale" "inheritRotation"]))
          (cond
            (and inheritRotation (not inheritScale)) (aset ba "transform" "noScale")
            (and (not inheritRotation) inheritScale) (aset ba "transform" "noRotationOrReflection")
            (and (not inheritRotation) (not inheritScale)) (aset ba "transform" "onlyTranslation")
            :else (aset ba "transform" "normal")
            ;:else (aset ba "transform" "onlyTranslation")) ; do nothing
            ))
        bones))

    (if s38?
      (do
        ;(! json-obj.skeleton.spine js/EXPORT_SPINE_VER)
        (! json-obj.skeleton.spine_gif (str "s38_spine_gif_" js/spine_gif_version)))
      (! json-obj.skeleton.spine_gif (str "db_spine_gif_" js/spine_gif_version))
      ))
  ;(set! js/_bonesRMap #js {})
  ;(set! js/_names names)
  nil)

(set! js/EXPORT_SPINE_VER "3.8.0-test")

;
;(defn load-kazari []
;
;  (set! js/bat0 (js/PIXI.Sprite.fromImage. "img/test/Commander_1640_1.png"))
;  (set! js/axe0 (js/PIXI.Sprite.fromImage. "img/test/Commander_1720_N.png"))
;  (set! js/tube0 (js/PIXI.Sprite.fromImage. "img/test/commander_30040_0.png"))
;
;  (set! js/kazaris #js [js/bat0 js/axe0 js/tube0])
;  ;
;  ;;var phone = new PIXI.Sprite.fromImage("img/phone.png");
;  ;(let [phone (js/PIXI.Sprite.fromImage. "img/megaphone.png")
;  ;      len (? doll.state.data.skeletonData.slots.length)
;  ;      children (? doll.children)
;  ;      gun (aget children (- len 2))
;  ;      shadow (aget children 0)
;  ;      ]
;  ;  (.addChild gun phone)
;  ;  (! gun.children.0.visible false)
;  ;  (! shadow.children.0.visible false)
;  ;  (!> phone.scale.set 0.5)
;  ;  (!> phone.anchor.set 0.6 0.7)                       ; 0.7 0.8
;  ;  (!> phone.pivot.set 0.6 0.7)
;  ;  (aset phone "rotation" (* (/ Math/PI 180) -160))    ; -130
;  ;  )
;
;  )

; hashmap 의 val에만 map 작업을 하고 싶다
; https://stackoverflow.com/questions/1676891/mapping-a-function-on-the-values-of-a-map-in-clojure
(defn map-vals [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

; chibi의 attachments 를 slot index : [att] 형태의 hashmap로 분해
(defn attachments-map [d]
  (if-let [attachments (aget d "spineData" "defaultSkin" "attachments")]
    (do
      (->> attachments
        (remove nil?)
        (js/Object.keys)
        ;(map #(str/split % #":"))
        (group-by #(-> (str/split % #":") first js/parseInt))
        (map-vals (fn [v] (map #(-> (str/split %":") second) v)))
        ))
    (do
      (js/alert "skin not found.")
      nil)))



;(when (re-find #"^Rodoggie" key)
;  ;var phone = new PIXI.Sprite.fromImage("img/phone.png");
;  (let [phone (js/PIXI.Sprite.fromImage. "img/megaphone.png")
;        len (? doll.state.data.skeletonData.slots.length)
;        children (? doll.children)
;        gun (aget children (- len 2))
;        shadow (aget children 0)
;        ]
;    (.addChild gun phone)
;    (! gun.children.0.visible false)
;    (! shadow.children.0.visible false)
;    (!> phone.scale.set 0.5)
;    (!> phone.anchor.set 0.6 0.7)                       ; 0.7 0.8
;    (!> phone.pivot.set 0.6 0.7)
;    (aset phone "rotation" (* (/ Math/PI 180) -160))    ; -130
;    ))



;(defn find-reye [doll]
;  ;  (if-let [slots (? doll.skeleton.slots)]
;  (if-let [slots (? doll.spineData.slots)]
;    (let [
;          ;          names (remove nil? (map (fn [s] (? s.currentSpriteName)) slots))
;          names (map (fn [s] (.-name s)) slots)
;          ;reye (first (filter #(re-find #"eyeR" %) names))
;          ]
;      (or
;        (first (filter #(re-find #"eyeR" %) names))
;        (first (filter #(re-find #"R-eye" %) names))
;        (first (filter #(re-find #"Reye" %) names))
;        (first (filter #(re-find #"Eye_Right" %) names))
;        )
;      )))

;(defn test3 [ev]
;  (let [sk (? _h.doll.skeleton)
;        reye-n (find-reye (? _h.doll))
;        ;sl (js/findSlot0 sk reye-n)
;        sl (.findSlot sk reye-n)
;        sps-obj (or (aget sl "meshes") (aget sl "sprites") )
;        sp (aget sps-obj (first (js-keys sps-obj)))
;        ;p (.-parent sp)
;        ;mgn js/megane
;        ]
;    sp
;    ))

; 아직 버그 있음
; 도로시 처럼 mesh->sprite->mesh 바뀌는 눈은 좌표계,hierarchy 가 바뀜.
;(defn on-click-apply-megane [ev]
;  (let [sk (? _h.doll.skeleton)
;        reye-n (find-reye (? _h.doll))
;        ;sl (js/findSlot0 sk reye-n)
;        sl (.findSlot sk reye-n)
;        sps-obj (or (aget sl "sprites") (aget sl "meshes"))
;        sp (aget sps-obj (first (js-keys sps-obj)))
;        p (.-parent sp)
;        mgn js/megane
;        ]
;    (.addChild p mgn)
;    (! mgn.rotation (* (/ Math/PI 180) (js/parseInt (.val (jQ "#adjust_gls_r" )))))
;    (! mgn.position.x (js/parseInt (.val (jQ "#adjust_gls_x" ))))
;    (! mgn.position.y (js/parseInt (.val (jQ "#adjust_gls_y" ))))
;    (! mgn.scale.x (js/parseFloat (.val (jQ "#adjust_gls_sx" ))))
;    (! mgn.scale.y (js/parseFloat (.val (jQ "#adjust_gls_sy" ))))
;    ))

