(ns chibi-combine38.features
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


;;(defn atlas-4x [upscale lines]
;;  (let [chk? (fn [a b]
;;               (and
;;                 (not (nil? b))
;;                 ;(= 2 (count lst))
;;                 (re-find #"(size|orig|xy|offset)" a)
;;                 ))
;;        double (fn [s]
;;                 (->> s
;;                   (#(str/split % ","))
;;                   (map str/trim)
;;                   (map js/parseFloat)
;;                   (map #(* upscale %))
;;                   )
;;                 )
;;        ]
;;    (map
;;      (fn [line]
;;        (let [[a b] (str/split line ":")]
;;          (if
;;            (chk? a b)
;;            (let [d (double b)]
;;              (str a ": " (str/join "," d))
;;              )
;;            line)))
;;      lines)
;;    ))

;;(defn saveas-string [string name]
;;  (->
;;    ;(js/JSON.stringify js-obj nil 2)
;;    string
;;    (js/Array)
;;    (js/Blob. {:type "text/plain;charset=utf-8"})
;;    (js/saveAs name)
;;    ))

;;(defn do4x [filename url]
;;  (go
;;    (let [res (<! (http/get
;;                    url                                     ;"blob:http://localhost:3459/88508115-65a9-4db1-9860-377a0ec54148"
;;                    {}))]
;;      (let [body (:body res)
;;            lines (str/split body #"\n")
;;            new-name (str/replace filename "_tex.json" "_4x_tex.json")
;;            new-str (str/join "\n" (atlas-4x 4 lines))
;;            ]
;;        (saveas-string (str new-str "\n") new-name)
;;        lines
;;        ))))
;;
;;(defn gen-4x [upscale-str filename url]
;;  (js/http_get url
;;    (fn [body]
;;      (let [
;;            ;body (:body res)
;;            new-name (str/replace filename ".atlas" (str "_" upscale-str "x.atlas"))
;;            lines (str/split body #"\n")
;;            upscale (js/parseFloat upscale-str)
;;            new-str (str/join "\n" (atlas-4x upscale lines))
;;            ]
;;        (saveas-string (str new-str "\n") new-name)
;;        ;lines
;;        nil
;;        )
;;      )))

;;(defn js-vals [o]
;;  (mapv #(aget o %)
;;    (.keys js/Object o)))
;;
;;(defn remove-parent-null [o]
;;  (when o
;;    (cond
;;      (array? o) (mapv remove-parent-null o)
;;      (object? o)
;;      (doseq [k (js-keys o)]
;;        (let [v (aget o k)] (if (and (= "parent" k) (nil? v)) (js-delete o k) (remove-parent-null v))))
;;      :else o)))
;;
;;(defn fix-curve [mp]
;;  (if mp
;;    (let [fix
;;          (fn [[cx1 cy1 cx2 cy2]]
;;            (! mp.curve cx1)
;;            (! mp.c2 cy1)
;;            (! mp.c3 cx2)
;;            (! mp.c4 cy2))
;;          cv (aget mp "curve")]
;;      (cond
;;        (array? cv) (fix cv)
;;        (array? mp) (mapv fix-curve mp)
;;        (= cv "linear")
;;        (do (js-delete mp "curve"))
;;        (object? mp) (mapv fix-curve (js-vals mp))
;;        :else nil
;;        ))))
;;
;;
;;(defn fix-skins [skins-kv]                                  ; map
;;  (let [fix-ty
;;        (fn [kv]
;;          ;(println kv)
;;          ;(println (aget kv "type"))
;;          (when (= "skinnedmesh" (aget kv "type"))
;;            ;(aset kv "type" "linkedmesh")
;;            (aset kv "type" "mesh")
;;            ))
;;        fix-att-kv
;;        (fn [k att-kv]
;;          (mapv #(fix-ty (aget att-kv %))
;;            (.keys js/Object att-kv))
;;          ;(println k (.keys js/Object att-kv))
;;          )
;;        skn (.keys js/Object skins-kv)
;;        ]
;;    (mapv
;;      (fn [k]
;;        (let [skin (aget skins-kv k)]
;;          (mapv
;;            (fn [sn] (fix-att-kv sn (aget skin sn)))
;;            (.keys js/Object skin))
;;          ))
;;      skn)
;;    )
;;  nil)
;;(defn fix-skins-st [skel]
;;  (let [skins (aget skel "skins")
;;        ar #js []
;;        ns (.keys js/Object skins)
;;        ]
;;    (doseq [n ns]
;;      (.push ar
;;        #js {:name        n
;;             :attachments (aget skins n)
;;             })
;;      )
;;    (aset skel "skins" ar)
;;    (js-delete skel "skinsName"))
;;  nil)
;;
;;(defn remove-dark-tint [skel]
;;  (let [slots (aget skel "slots")]
;;    (mapv
;;      (fn [sl] (js-delete sl "dark"))
;;      slots))
;;  nil)

;(defn cv4spine38 [json-obj s33? s38?]
;;(defn cv4spine38 [json-obj s33? no-dark-tint?]
;;  ;(set! js/_bonesRMap #js {})
;;  ;(mapv (fn [o]
;;  ;        (do
;;  ;          (aset js/_bonesRMap (? o.name) (? o.rotation))
;;  ;          ))
;;  ;  (aget json-obj "bones"))
;;  (let [anis (? json-obj.animations)
;;        names (.keys js/Object anis)
;;        s38? true
;;        ]
;;    ;(println names)
;;    (set! js/_names names)
;;    (remove-parent-null json-obj)
;;    (if no-dark-tint? (remove-dark-tint json-obj))
;;    ;(mapv
;;    ;  #(farewell-to-flipx % (aget anis %))
;;    ;  names)
;;    (when s33?
;;      (fix-skins (aget json-obj "skins"))                   ; for 3.6 에서 추가된 거
;;      )
;;    ;(when s38?
;;    (do
;;      (fix-skins-st json-obj)                               ; for 3.8 에서 추가된 거
;;      (fix-curve (aget json-obj "animations"))
;;
;;      ;"flipX": false,
;;      ;"flipY": false,
;;      ;"inheritScale": true,
;;      ;"inheritRotation": true,
;;      ;
;;      ;(mapv (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY" "inheritScale" "inheritRotation"]))
;;      ;  (aget json-obj "bones"))
;;
;;      ;(! json-obj.skeleton.lz "sh")
;;      (! json-obj.skeleton.spine "3.8")
;;      ;(! json-obj.skeleton.chibi_gif (str "" js/chibi_gif_version))
;;      )
;;
;;    (when false
;;      (let [bones (aget json-obj "bones")]
;;        (mapv
;;          (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY"]))
;;          bones)
;;        (mapv
;;          #(let [ba %
;;                 inheritScale (.-inheritScale ba)
;;                 inheritRotation (.-inheritRotation ba)
;;                 name (.-name ba)]
;;             ;(println name inheritScale inheritRotation ba)
;;             (if s38?
;;               (mapv (fn [k] (js-delete ba k)) ["inheritScale" "inheritRotation"]))
;;             (cond
;;               (and inheritRotation (not inheritScale)) (aset ba "transform" "noScale")
;;               (and (not inheritRotation) inheritScale) (aset ba "transform" "noRotationOrReflection")
;;               (and (not inheritRotation) (not inheritScale)) (aset ba "transform" "onlyTranslation")
;;               :else (aset ba "transform" "normal")
;;               ;:else (aset ba "transform" "onlyTranslation")) ; do nothing
;;               ))
;;          bones))
;;      )
;;
;;    (if s38?
;;      (do
;;        ;(! json-obj.skeleton.spine js/EXPORT_SPINE_VER)
;;        (! json-obj.skeleton.chibi_gif (str "s38_chibi_gif_" js/chibi_gif_version)))
;;      (! json-obj.skeleton.chibi_gif (str "db_chibi_gif_" js/chibi_gif_version))
;;      ))
;;  ;(set! js/_bonesRMap #js {})
;;  ;(set! js/_names names)
;;  nil)

;;(set! js/EXPORT_SPINE_VER "3.8.0-test")

; hashmap 의 val에만 map 작업을 하고 싶다
; https://stackoverflow.com/questions/1676891/mapping-a-function-on-the-values-of-a-map-in-clojure
;;(defn map-vals [f m]
;;  (into (empty m) (for [[k v] m] [k (f v)])))

; chibi의 attachments 를 slot index : [att] 형태의 hashmap로 분해
;;(defn attachments-map [d]
;;  (if-let [attachments (aget d "spineData" "defaultSkin" "attachments")]
;;    (do
;;      (mapv
;;        #(->> %1
;;           js/Object.keys
;;           (mapv identity)
;;           )
;;        (remove nil? attachments))
;;      ;(mapv
;;      ;
;;      ;  )
;;      )
;;
;;    (do
;;      (js/alert "skin not found.")
;;      nil)))





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


(defn atlas-rename [prefix atlas-text]
  (let [
        lines (str/split atlas-text #"\r?\n")
        [blk body] (partition-by #(zero? (count %)) lines)
        head (take 5 body)
        regions (drop 5 body)
        ]
    (concat
      blk
      head
      (->> regions
        (map (fn [line]
               (let [a 1]
                 (cond
                   (str/includes? line ":") line
                   ;;:else (str chibi-name "_" line)
                   :else (str prefix line)
                   )
                 )))
        ))
    ))

(defn combine-atlas-files [atlas-map name-list prefix-list]
  (let [
        ks name-list ;; (js/Object.keys atlas-map)
        prefix-map (into {} (map (fn [k v] [k v]) name-list prefix-list))
        rename (fn [k]
                 (let [body (aget atlas-map k)
                       prefix (get prefix-map k)
                       ]
                   (atlas-rename prefix body)))
        ]
    (->> ks
      (mapcat rename)
      ;;concat
      )))

(defn saveas-string [string name]
  (->
    ;(js/JSON.stringify js-obj nil 2)
    string
    (js/Array)
    (js/Blob. {:type "text/plain;charset=utf-8"})
    (js/saveAs name)
    ))

(defn combine-atlas-save [atlas-map name-list prefix-list filename]
  (->>
    ;;atlas-map
    (combine-atlas-files atlas-map name-list prefix-list)
    (str/join "\n")
    (#(saveas-string % filename))
    ))

(defn combine-skel-a-b [sk sk2]
  "skin skin2 를 결합한다"
  (let [
        bs (aget sk "bones")
        bs2 (aget sk2 "bones")
        sl (aget sk "slots")
        sl2 (aget sk2 "slots")
        ev (aget sk "events")
        ev2 (aget sk2 "events")
        evN (aget sk "eventsName")
        evN2 (aget sk2 "eventsName")
        new-bones (.concat bs bs2)
        new-slots (.concat sl sl2)
        new-eventsName (.concat evN evN2)
        animations (aget sk "animations")
        animations2 (aget sk2 "animations")
        ;;default-skin (aget sk "skins" "default")
        ;;default-skin2 (aget sk2 "skins" "default")
        default-skin (aget sk "skins" 0 "attachments")
        default-skin2 (aget sk2 "skins" 0 "attachments")
        ]
    (let [
          ]
      (aset sk "slots" new-slots)
      (aset sk "bones" new-bones)
      (aset sk "eventsName" new-eventsName)

      (doseq [name2 (js/Object.keys default-skin2)]
        (aset default-skin name2 (aget default-skin2 name2))
        )
      (doseq [name2 (js/Object.keys animations2)]
        (aset animations name2 (aget animations2 name2))
        )
      (doseq [name2 (js/Object.keys ev2)]
        (aset ev name2 (aget ev2 name2))
        )
      sk)))

(declare fn_fix_obj_names)

(defn combine-skels [name-list prefix-list skel-map transform-map]
  "skeleton 결합하기"
  (let [
        ;;ks (js/Object.keys skel-map)
        ks name-list
        skels (map #(aget skel-map %) ks)
        ts (map #(aget transform-map %) ks)
        ]
    (loop [[k & k-rest] ks
           [prefix & prefix-rest] prefix-list
           [sk & sk-rest] skels
           [t & t-rest] ts
           idx 0
           shift 0
           new-root? true
           result nil
           ;;scale (! d.scale.get)
           ]
      (if k
        (do
          ;;(js/window.fn_fix_obj_names sk k (+ shift (if new-root? 1 0)) new-root? t)
          (fn_fix_obj_names sk k prefix
            (+ shift (if new-root? 1 0)) new-root? t)
          (if result
            (combine-skel-a-b result sk))
          (let [bones (aget sk "bones")
                cnt (count bones)
                ;;combined-sk (if result )
                ;;new-root? (if new-root?)
                ]
            ;;(js/console.log "skin=" k shift new-root? "bones cnt=" cnt )
            (recur k-rest prefix-rest sk-rest t-rest (inc idx) (+ cnt shift) false (or result sk))))
        ;; 더이상 doll이 없으면 리턴
        result))
    ))


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

(defn fix-skins [skins-kv] ; map
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
        #js {:name n
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
      (fix-skins (aget json-obj "skins")) ; for 3.6 에서 추가된 거
      (fix-skins-st json-obj) ; for 3.8 에서 추가된 거
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
  json-obj)

(set! js/EXPORT_SPINE_VER "3.8.0-test")


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
        (map-vals (fn [v] (map #(-> (str/split % ":") second) v)))
        ))
    (do
      (js/alert "skin not found.")
      nil)))

;;(defn- nn0 [c k] (str c "_" k))
(defn- nn0 [c k] (str c k))

(defn nn [c k]
  (if k (nn0 c k)))

(defn r! [cn o k]
  (aset o k (nn cn (aget o k))))

(defn rename_skins38 [skins prefix shiftNum]
  (.log js/console "skins=" skins)
  (let [newSkins #js []]
    (doseq [skin skins]
      (let [
            skinName (aget skin "name")
            attachments (aget skin "attachments")
            ;;skin (aget skins skinName)
            newAttachments #js {}]
        ;;(js/console.log "skinName=" skinName)
        ;;(js/console.log "attachments=" attachments)
        (doseq [kk (js/Object.keys attachments)]
          ;;(js/console.log "kk=" kk)
          (let [o (aget attachments kk)
                newKk (nn prefix kk)
                newO #js {}]
            ;;(js/console.log "o=" o)
            (doseq [kk2 (js/Object.keys o)]
              ;;(js/console.log "kk2=" kk2)
              (let [oo (aget o kk2)
                    ;;_ (js/console.log "oo=" oo)
                    newKk2 (nn prefix kk2)
                    newOo (-> oo js/JSON.stringify js/JSON.parse)
                    ;;_ (js/console.log "newOo=" newOo)
                    name (aget newOo "name")
                    path (aget newOo "path")
                    ty (aget newOo "type")
                    vertices (aget newOo "vertices")
                    uvs (aget newOo "uvs")
                    cnt-vertices (count vertices)
                    cnt-uvs (count uvs)
                    ]
                (aset newOo "name" (nn prefix name))
                (aset newOo "path" (nn prefix path))
                ;;(js/console.log "newOo=" newOo)
                ;; vertices
                ;;(when (= ty "mesh")
                ;;  (.log js/console "mesh=" name cnt-vertices cnt-uvs))
                (when (and cnt-vertices
                        ;;(= ty "skinnedmesh")
                        (= ty "mesh")
                        (> cnt-vertices cnt-uvs) ;; weighted mesh
                        )
                  (let [length cnt-vertices]
                    (loop [offset 0]
                      (when (< offset length)
                        (let [n (aget vertices offset)]
                          (doseq [i (range 0 n)]
                            (let [offset2 (+ offset (* i 4) 1)
                                  bone-idx (aget vertices offset2)]
                              (aset vertices offset2 (+ bone-idx shiftNum))
                              ))
                          (recur (+ offset (* n 4) 1)))
                        )
                      )))
                ;; vertices
                (aset newO newKk2 newOo)
                ))
            (aset newAttachments newKk newO)
            ))

        ;;(aset skins skinName newAttachments)
        (.push newSkins #js {:name skinName :attachments newAttachments})
        ))
    ;;skins
    (.log js/console "newSkins=" newSkins)
    newSkins
    ))

(defn r-ani-slots [cn ani]
  (when-let [slots (aget ani "slots")]
    (let [new-ani-slots #js {}]
      (doseq [ks (js/Object.keys slots)]
        (let [slot (aget slots ks)
              newSlot #js {}
              attrs (or (aget slot "attachment") #js [])
              ]
          (doseq [idx (js/Object.keys attrs)]
            (let [tl (aget attrs idx)]
              (aset tl "name" (nn cn (aget tl "name")))
              ))
          (aset newSlot "attachment" attrs)
          (aset new-ani-slots (nn cn ks) newSlot)
          ))
      (aset ani "slots" new-ani-slots)
      )))


(defn r-ani-bones [cn ani]
  (when-let [bones (aget ani "bones")]
    (let [newAniBones #js {}]
      (doseq [kb (js/Object.keys bones)]
        (aset newAniBones (nn cn kb) (aget bones kb)))
      (aset ani "bones" newAniBones)
      )))

(defn r-ani-events [cn ani]
  (when-let [events (aget ani "events")]
    (let [a 1]
      (doseq [ev events]
        (aset ev "name" (nn cn (aget ev "name"))))
      ;;(aset ani "bones" new-events)
      )))


(defn r-ani-ffds [cn ani]
  (when-let [ffds (aget ani "ffd")]
    (let [newFfds #js {}]
      (doseq [ffdname (js/Object.keys ffds)]
        (let [ffd (aget ffds ffdname)
              newFfd #js {}
              ;;attrs (or (aget ffd "attachment") #js [])
              ]
          (doseq [k (js/Object.keys ffd)]
            (let [ffdo (aget ffd k)
                  newFfdo #js {}
                  ]
              (doseq [k2 (js/Object.keys ffdo)]
                (let [ar (aget ffdo k2)]
                  (aset newFfdo (nn cn k2) ar)
                  ))
              (aset newFfd (nn cn k) newFfdo)
              ))
          (aset newFfds ffdname newFfd)
          ;;(aset newFfd "attachment" attrs)
          ;;(aset newFfds (nn cn ks) newFfd)
          ))
      (aset ani "ffd" newFfds)
      )))


(defn r-ani-drawOrder [cn ani]
  (when-let [drawOrder (aget ani "drawOrder")]
    (let [a 1]
      (doseq [dwo drawOrder]
        (let [offsets (aget dwo "offsets")]
          (doseq [offset offsets]
            (let [a 1]
              (aset offset "slot" (nn cn (aget offset "slot")))
              ;;(r! cn offset "slot")
              ))
          ))
      )))


(defn r-slots [cn skel slots]
  ;;(.log js/console "slots=" slots)
  (doseq [k (js/Object.keys slots)]
    (let [o (aget slots k)]
      (aset o "name" (nn cn (aget o "name")))
      (aset o "bone" (nn cn (aget o "bone")))
      (aset o "attachment" (nn cn (aget o "attachment")))
      )))

(defn r-bones [cn skel bones newRoot x y sc-x sc-y]
  (let [newBones #js []]
    (if newRoot (.push newBones
                  #js {:name "root" :parent nil :color "9b9b9bff"}))
    (doseq [o bones]
      (aset o "name" (nn cn (aget o "name")))
      ;;(r! cn o "name")
      (if (aget o "parent")
        ;;(r! cn o "parent")
        (aset o "parent" (nn cn (aget o "parent")))
        (let [x0 (or (aget o "x") 0)
              y0 (or (aget o "y") 0)
              sx (or (aget o "scaleX") 1)
              sy (or (aget o "scaleY") 1)
              ]
          (aset o "x" (+ x0 x))
          (aset o "y" (+ y0 y))
          (aset o "scaleX" (* sx sc-x))
          (aset o "scaleY" (* sy sc-y))
          (aset o "parent" "root")
          ))
      (.push newBones o)
      )
    (aset skel "bones" newBones)
    newBones))

(defn r-events [cn skel]
  (if-let [events (aget skel "events")]
    (let [newEvents #js {}]
      (doseq [k (js/Object.keys events)]
        (let [v (aget events k)]
          (aset newEvents (nn cn k) v)
          ))
      (aset skel "events" newEvents)
      )))

(defn r-eventsName [cn skel]
  (if-let [eventsName (aget skel "eventsName")]
    (let [newEventsName #js []]
      (doseq [v eventsName]
        (.push newEventsName (nn cn v)))
      (aset skel "eventsName" newEventsName)
      )))

(defn r-ani [cn skel]
  (if-let [animations (aget skel "animations")]
    (let [newAnis #js {}]
      (doseq [name (js/Object.keys animations)]
        (let [ani (aget animations name)
              ;;aniSlots (aget ani "slots")
              ]
          (r-ani-slots cn ani)
          (r-ani-bones cn ani)
          (r-ani-events cn ani)
          (r-ani-ffds cn ani)
          (r-ani-drawOrder cn ani)
          (aset newAnis (nn cn name) ani)
          ))
      (aset skel "animations" newAnis)
      )))

(defn fn_fix_obj_names [skel chibi_name prefix shiftNum newRoot t]
  ;;(js/console.log chibi_name, prefix "shiftNum=", shiftNum, "newRoot=", newRoot, t)
  (let [x (or (aget t "x") 0)
        y (or (aget t "y") 0)
        ;;scale (or (aget t "scale") 1.0)
        sc-x (or (aget t "sc_x") 1.0)
        sc-y (or (aget t "sc_y") 1.0)
        skins (aget skel "skins")
        new-skins (rename_skins38 skins prefix shiftNum)
        ]
    (r-slots prefix skel (aget skel "slots"))
    (r-bones prefix skel (aget skel "bones") newRoot x y sc-x sc-y)
    (r-events prefix skel)
    (r-eventsName prefix skel)
    (r-ani prefix skel)
    (aset skel "skins" new-skins)
    ))



(defn combine-skels-save [skels-map
                          name-list prefix-list
                          transform-map filename]
  ;;(js/console.log "ts=" transform-map)
  (->
    ;;skels-map
    (combine-skels name-list prefix-list skels-map transform-map)
    ;;(#(do (set! js/window.combined_json %) %))
    ;;(cv4spine3! true)
    (#(do (set! js/window.combined_json %) %))
    (js/JSON.stringify nil 1)
    (saveas-string filename)
    ))

