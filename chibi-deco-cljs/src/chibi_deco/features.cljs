(ns chibi-deco.features
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
               [cljs.core.async.macros :only [go]])
  (:require
    [domina :as dom]
    [goog.string :as gstr]
    [goog.string.format]
    [clojure.string :as str]
    [cljs-time.core :as dt] [cljs-time.format :as df]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]
    [renden.spine :as sp])
  )


(defn atlas-4x [lines]
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
                   (map #(* 4 %))
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
                    url ;"blob:http://localhost:3459/88508115-65a9-4db1-9860-377a0ec54148"
                    {}))]
      (let [body (:body res)
            lines (str/split body #"\n")
            new-name (str/replace filename ".atlas" "_4x.atlas")
            new-str (str/join "\n" (atlas-4x lines))
            ]
        (saveas-string (str new-str "\n") new-name)
        lines
        ))))

(defn gen-4x [filename url]
  (js/http_get url
    (fn [body]
      (let [
            ;body (:body res)
            lines (str/split body #"\n")
            new-name (str/replace filename ".atlas" "_4x.atlas")
            new-str (str/join "\n" (atlas-4x lines))
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
      (doseq [k  (js-keys o)]
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
        (object? mp) (mapv fix-curve (js-vals mp))
        :else nil
        ))))

(defn farewell-to-flipx [nm ani]
  ;(println nm)
  (let [bones (? ani.bones)
        names (.keys js/Object bones)
        ]
    (doseq [n names ]
      (let [b (aget bones n)
            r (aget js/_bonesRMap n)
            flipx? (? b.flipX.0.x)
            ]
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
                (println nm n (? timeline.time) angle na r)
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
        (fn [kv ]
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
      (fix-skins (aget json-obj "skins"))                   ; for 3.6
      (fix-skins-st json-obj)                               ; for 3.8
      (fix-curve (aget json-obj "animations"))

      ;"flipX": false,
      ;"flipY": false,
      ;"inheritScale": true,
      ;"inheritRotation": true,
      ;
      (mapv (fn [ba] (mapv #(js-delete ba %) ["flipX" "flipY" "inheritScale" "inheritRotation"]))
        (aget json-obj "bones"))

      ;(! json-obj.skeleton.lz "sh")
      ;(! json-obj.skeleton.spine "3.8")
      ;(! json-obj.skeleton.spine_gif (str "" js/spine_gif_version))
      )
    (if s38?
      (do
        (! json-obj.skeleton.spine js/EXPORT_SPINE_VER)
        (! json-obj.skeleton.spine_gif (str "s38_spine_gif_" js/spine_gif_version)))
      (! json-obj.skeleton.spine_gif (str "db_spine_gif_" js/spine_gif_version))
      )
    )
  ;(set! js/_bonesRMap #js {})
  ;(set! js/_names names)
  nil)

(set! js/EXPORT_SPINE_VER "3.8.0-test")


(defn load-kazari []

  (set! js/bat0 (js/PIXI.Sprite.fromImage. "img/test/Commander_1640_1.png"))
  (set! js/axe0 (js/PIXI.Sprite.fromImage. "img/test/Commander_1720_N.png"))
  (set! js/tube0 (js/PIXI.Sprite.fromImage. "img/test/commander_30040_0.png"))

  (set! js/kazaris #js [js/bat0 js/axe0 js/tube0])
    ;
    ;;var phone = new PIXI.Sprite.fromImage("img/phone.png");
    ;(let [phone (js/PIXI.Sprite.fromImage. "img/megaphone.png")
    ;      len (? doll.state.data.skeletonData.slots.length)
    ;      children (? doll.children)
    ;      gun (aget children (- len 2))
    ;      shadow (aget children 0)
    ;      ]
    ;  (.addChild gun phone)
    ;  (! gun.children.0.visible false)
    ;  (! shadow.children.0.visible false)
    ;  (!> phone.scale.set 0.5)
    ;  (!> phone.anchor.set 0.6 0.7)                       ; 0.7 0.8
    ;  (!> phone.pivot.set 0.6 0.7)
    ;  (aset phone "rotation" (* (/ Math/PI 180) -160))    ; -130
    ;  )

  )



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


(defn gen-atlas [name w h rects]
  (let [tab "  "
        tuple (fn [n a b] (str n ": " a ", " b))
        single (fn [n a] (str n ": " a))
        ttuple (fn [n a b] (str tab (tuple n a b)))
        tsingle (fn [n a] (str tab (single n a)))
        rects2 (.sort rects
                 (fn [a b]
                   (let [a (str/lower-case (? a.name))
                         b (str/lower-case (? b.name))]
                     (compare a b))))
        ]
    (clojure.string/join "\n"
      (concat [""
               name
               (tuple "size" w h)
               (single "format" "RGBA8888")
               (tuple "filter" "Linear" "Linear")
               (single "repeat" "none")
               ]
        (mapcat (fn [r]
                  [(? r.name)
                   (tsingle "rotate" (true? (? r.rot)))
                   (ttuple "xy" (? r.x) (? r.y))
                   (ttuple "size" (? r.width) (? r.height))
                   (ttuple "orig" (? r.width) (? r.height))
                   (ttuple "offset" 0 0)
                   (tsingle "index" -1)
                   ]) rects2)
        [""])
      )))

;    _d().spineData.animations[6].timelines.filter ( t=> t.drawOrders )[0].drawOrders[0].map( n => { if (n>=0) { return n+":"+_ss()[n] }  } )
;
;    DrawingOrder 타임라인 보정
;    모든 slot의 idx를 구한다
;    신규 parts의 idx를 구한다
;    타임라인
;    현재 sidx
;    offset > 0 일 경우
;    sidx <= a <= sidx + offset 에 신규 파츠 인덱스가 몇개 걸리는지 계산 => d
;    new-offset = offset + d
;    offset < 0 일 경우
;    new-offset = offset - d
;    * */


(defn fix-draw-order [deco-names slots animations]
  (let [slot-map (into {} (map (fn [s i] [(aget s "name") i]) slots (range)))
        k-indices (map #(get slot-map %) deco-names)
        ]
    (doseq [ani (.values js/Object animations)]
      (when-let [draw-order (aget ani "drawOrder")]
        ;(.log js/console draw-order)
        (mapv
          (fn [tl]
            (mapv
              (fn [o]
                (let [slot (? o.slot)
                      offset (? o.offset)
                      sidx (get slot-map slot)
                      nidx (+ sidx offset)
                      a (Math/min sidx nidx)
                      b (Math/max sidx nidx)
                      c (count (filter #(and (<= a %) (<= % b)) k-indices))
                      ]
                  (when (> c 0)
                    ;(if (not= offset 0) (! o.offset2 offset))
                    (cond
                      (> offset 0) (! o.offset (+ offset c))
                      (< offset 0) (! o.offset (- offset c))
                      :else nil ; TODO 이 경우는 아직 명쾌한 답은 못 찾았음
                    ))
                  )) (or (? tl.offsets) [])
              )) draw-order)))
    ))

(set! js/window.fix_draw_order fix-draw-order)
