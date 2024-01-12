(ns chibi-gif.features
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
                    url                                     ;"blob:http://localhost:3459/88508115-65a9-4db1-9860-377a0ec54148"
                    {}))]
      (let [body (:body res)
            lines (str/split body #"\n")
            new-name (str/replace filename "_tex.json" "_4x_tex.json")
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
        (= cv "linear")
        (do (js-delete mp "curve"))
        (object? mp) (mapv fix-curve (js-vals mp))
        :else nil
        ))))


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

(defn remove-dark-tint [skel]
  (let [slots (aget skel "slots")]
    (mapv
      (fn [sl] (js-delete sl "dark"))
      slots))
  nil)

;(defn cv4spine38 [json-obj s33? s38?]
(defn cv4spine38 [json-obj s33? no-dark-tint?]
  ;(set! js/_bonesRMap #js {})
  ;(mapv (fn [o]
  ;        (do
  ;          (aset js/_bonesRMap (? o.name) (? o.rotation))
  ;          ))
  ;  (aget json-obj "bones"))
  (let [anis (? json-obj.animations)
        names (.keys js/Object anis)
        s38? true
        ]
    ;(println names)
    (set! js/_names names)
    (remove-parent-null json-obj)
    (if no-dark-tint? (remove-dark-tint json-obj))
    ;(mapv
    ;  #(farewell-to-flipx % (aget anis %))
    ;  names)
    (when s33?
      (fix-skins (aget json-obj "skins"))                   ; for 3.6 에서 추가된 거
      )
    ;(when s38?
    (do
      (fix-skins-st json-obj)                               ; for 3.8 에서 추가된 거
      (fix-curve (aget json-obj "animations"))

      ;"flipX": false,
      ;"flipY": false,
      ;"inheritScale": true,
      ;"inheritRotation": true,
      ;
      ;(mapv (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY" "inheritScale" "inheritRotation"]))
      ;  (aget json-obj "bones"))

      ;(! json-obj.skeleton.lz "sh")
      (! json-obj.skeleton.spine "3.8")
      ;(! json-obj.skeleton.chibi_gif (str "" js/chibi_gif_version))
      )

    (when false
      (let [bones (aget json-obj "bones")]
        (mapv
          (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY"]))
          bones)
        (mapv
          #(let [ba %
                 inheritScale (.-inheritScale ba)
                 inheritRotation (.-inheritRotation ba)
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
      )

    (if s38?
      (do
        ;(! json-obj.skeleton.spine js/EXPORT_SPINE_VER)
        (! json-obj.skeleton.chibi_gif (str "s38_chibi_gif_" js/chibi_gif_version)))
      (! json-obj.skeleton.chibi_gif (str "db_chibi_gif_" js/chibi_gif_version))
      ))
  ;(set! js/_bonesRMap #js {})
  ;(set! js/_names names)
  nil)

(set! js/EXPORT_SPINE_VER "3.8.0-test")

; hashmap 의 val에만 map 작업을 하고 싶다
; https://stackoverflow.com/questions/1676891/mapping-a-function-on-the-values-of-a-map-in-clojure
(defn map-vals [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

; chibi의 attachments 를 slot index : [att] 형태의 hashmap로 분해
(defn attachments-map [d]
  (if-let [attachments (aget d "spineData" "defaultSkin" "attachments")]
    (do
      (mapv
        #(->> %1
           js/Object.keys
           (mapv identity)
           )
        (remove nil? attachments))
      ;(mapv
      ;
      ;  )
      )

    (do
      (js/alert "skin not found.")
      nil)))
