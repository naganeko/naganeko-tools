(ns chibi-combine38.core
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
               [cljs.core.async.macros :only [go]]
               )
  (:require
    [domina :as dom] [goog.string :as gstr] [goog.string.format] [clojure.string :as str]
    [cljs-time.core :as dt] [cljs-time.format :as df]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]
    [renden.spine36 :as sp]
    [renden.spine38 :as sp38]
    [renden.utils :as u]
    [renden.chibi :as c]
    [renden.skb3 :as skb]
    [chibi-combine38.features :as features]
    [renden.chibi-debug :as chb-debug]

    [chibi-combine38.experimentals :as experimentals]

    ;[cljs.js] [library.core]
    [cljs.js]
    )
  )

(enable-console-print!)

(def jQ js/jQuery)
(def bg-color 0x1099bb)
;(def bg-color 0x00ff00)

(defonce CW90D (* 0.5 Math/PI))

(defonce CHANGE "change")
(defonce CLICK "click")

(def outer-width (or js/ws_w 400))
(def outer-height (or js/ws_h 400))
(def inner-width (- outer-width 60))
(def inner-height (- outer-height 60))

(def dt-fmt (df/formatter "yyyyMMdd-HHmmss"))

(defn cvs-work [] (. (jQ "#content canvas") get 0))
(defn cvs-shot [] (. (jQ "#cvs_shot") get 0))
(defn ctx-shot [] (. (cvs-shot) getContext "2d"))
(defn alert [str] (js/alert str))

(def png-hldr (.getElementById js/document "png_holder"))

(defonce _h (obj :width inner-width :height inner-height :bs_x 30 :bs_y 30 :urls {}))

(declare change-scale-x init-pos-xy)
(declare regen-lists)

(set! js/_doll nil)
(defn _d [] js/_doll)

(set! js/enable_clear_fail_step false)
(set! js/is_gen_setup_pose false) ; unused


(.log js/console "wh=" outer-width outer-height)

(defonce app (js/PIXI.Application.
               ;;outer-width outer-height
               (obj
                 :width outer-width
                 :height outer-height
                 :antialias true
                 :transparent true
                 ;:transparent "notMultiplied"
                 ;;:backgroundColor bg-color
                 ;:backgroundColor 0xFFFFFF
                 ;:forceCanvas true
                 :preserveDrawingBuffer true
                 :premultipliedAlpha true
                 )))

(aset app "stage" (js/PIXI.display.Stage.))
(def stage (? app.stage))
(set! js/app app)


(def gr (new js/PIXI.Graphics))
(def bounds-gr (new js/PIXI.Graphics))
(def bg-gr (new js/PIXI.Graphics))
(def mask-gr (new js/PIXI.Graphics))
(def cross-gr (new js/PIXI.Graphics))
(def cntr (new js/PIXI.Container))

(def tgroup (new js/TDGroup))


;(def renderer (js/PIXI.autoDetectRenderer. outer-width outer-height))
;(def renderer (js/PIXI.CanvasRenderer. outer-width outer-height))

;;(declare on-change-heterochromia)

(defn zero-pos []
  [ (/ outer-width 2)
   (Math/round  (* outer-height .85))]
  )
(defn cvs->bone-xy [x y]
  (let [[cx cy] (zero-pos)]
    [(- x cx)
     (- cy y)
     ]))
(defn bone->cvs-xy [x y]
  (let [[cx cy] (zero-pos)]
    [(+ x cx)
     (+ cy (- y))
     ]))

(defn parse-int [a b]
  (let [v (js/parseInt a)]
    (if (js/isNaN v) b v)))

(defn register-ticker-listener [f key]
  (let [t (.-ticker app)]
    (when-let [yet (not (aget t key))]
      ;(println "registering ticker listener: " key)
      (.add t f)
      (aset t key true)
      )))

(defn fix-by-minmax [input]
  (let [minv (u/parseIntOrNil (.attr input "min"))
        maxv (u/parseIntOrNil (.attr input "max"))
        v (.val input)]
    (when (and minv maxv v)
      (.val input (max (min v maxv) minv))
      )))

(defn draw-bounds [g bs]
  (let [x (aget bs "x")
        y (aget bs "y")
        w (aget bs "width")
        h (aget bs "height")]

    (.drawRect g x y w h)))


(defn gen-shot-canvas [w h]
  ;;(-> (jQ (gstr/format "<canvas style='display:none;' id='cvs_shot' width='%d' height='%d'><canvas>" w h))

  ;(-> (jQ (gstr/format "<canvas style='' id='cvs_shot' width='%d' height='%d'><canvas>" w h))
  ;  (.appendTo (jQ "#work2"))
  ;  (.get 0)
  ;  )
  (let [c-shot (cvs-shot)]
    (do
      (aset c-shot "width" (? _h.bg.width))
      (aset c-shot "height" (? _h.bg.height))
      )
    c-shot)
  )


(defn destroy-shot-canvas []
  ;(-> (jQ "#cvs_shot") .remove)
  nil)




(defn gen-canvas []
  (let [c-shot (cvs-shot)
        c-work (cvs-work)
        ctx (ctx-shot)
        bg (? _h.bg )
        ;[x y w h] (mapv #(aget gr (name %)) '(x y width height))
        [x y w h] (mapv #(aget bg (name %)) '(x y width height)) ; bg의 x,y,width,height를 한번에 구한다
        ;w (dec w0)
        ;h (dec h0)
        ]
    (! c-shot.width w)
    (! c-shot.height h)
    (.clearRect ctx 0 0 (max w outer-width) (max h outer-height))
    (.drawImage ctx c-work x y w h 0 0 w h)

    c-shot))

(defn take-screen-shot [sprite jq-tag name]
  (let [
        d3 (jQ jq-tag)
        block (jQ "<div class='frame1'></div>")
        i1 (jQ "<img class='gif_cut'></img>")
        ;canvas (!> renderer.extract.canvas sprite)
        canvas (gen-canvas)
        ]
    (-> block
      (.append i1)
      (.appendTo d3))

    (.toBlob
      canvas
      (fn [blob] (let [sht {:name (str name ".png") :blob blob}]
                   ;(println (:name sht))
                   (!> _h.shot_blobs.push sht))))

    (.prop i1 (str "gif_" name))
    (.attr i1 "src" (.toDataURL canvas "image/png"))
    (when name
      (.append block (str "<br/>" name))
      ))
  nil)

(defn take-screen-shot2 [canvas jq-tag name]
  (let [
        d3 (jQ jq-tag)
        block (jQ "<div class='frame1'></div>")
        i1 (jQ "<img class='gif_cut'></img>")
        ]
    (-> block
      (.append i1)
      (.appendTo d3))
    (.toBlob
      canvas
      (fn [blob] (let [sht {:name (str name ".png") :blob blob}]
                   ;(println (:name sht))
                   (!> _h.shot_blobs.push sht))))

    (.prop i1 (str "gif_" name))
    (.attr i1 "src" (.toDataURL canvas "image/png"))
    (when name
      (.append block (str "<br/>" name))
      ))
  nil)

(defn gen-region-canvas-0 [canvas ctx x y w h]
  (! canvas.width w)
  (! canvas.height h)
  ;(.clearRect ctx )
  (.drawImage ctx png-hldr x y w h 0 0 w h)
  )
(defn gen-region-canvas-r [canvas ctx x y w h]
  (! canvas.width w)
  (! canvas.height h)
  (.translate ctx w 0)
  (.rotate ctx CW90D)
  (.drawImage ctx png-hldr x y h w 0 0 h w)
  )

(defn gen-region-canvas [canvas ctx rgn]
  (let [
        [x y w h r?] (mapv #(aget rgn %) ["x" "y" "width" "height" "rotate"])
        ]
    (if r?
      (gen-region-canvas-r canvas ctx x y w h)
      (gen-region-canvas-0 canvas ctx x y w h))
    ))

; js/sd_atlas
(defn unpack-txtr [atlas]
  (let [regions (aget atlas "regions")
        canvas (cvs-shot)
        ctx (ctx-shot)
        ]
    (-> (jQ "#sshot") .empty)
    (! _h.shot_blobs (arr))

    (! _h.zip_file (str (? _h.doll.skin) "_unpacked.zip"))

    (mapv
      (fn [rgn]
        (println (.-name rgn))
        (gen-region-canvas canvas ctx rgn)
        (take-screen-shot2 canvas "#sshot" (.-name rgn))
        )
      regions)
    )
  nil)


(defn save-unpma-txtr [atlas]
  (let [pages (aget atlas "pages")
        canvas (cvs-shot)
        ctx (ctx-shot)
        ]
    (mapv
      (fn [p]
        (let [
              [w h name] (mapv #(aget p %) ["width" "height" "name"])
              img (aget p "rendererObject" "source")
              ]
          (.log js/console p name w h)
          (! canvas.width w)
          (! canvas.height h)
          (.drawImage ctx img 0 0)
          (.toBlob
            canvas
            (fn [blob]
              (js/saveAs blob (str/replace name ".png" "_unpma.png"))
              ) )))
      pages))


  nil)

;(declare gen-shots)
(declare change-dousa)

(def dousa (jQ "#dousa"))
(def waku-w (jQ "#waku_w"))
(def waku-h (jQ "#waku_h"))
(def waku-x (jQ "#waku_x"))
(def waku-y (jQ "#waku_y"))
(def scale-x (jQ "#scale_x"))
(def scale-y (jQ "#scale_y"))
(def pos-x (jQ "#pos_x"))
(def pos-y (jQ "#pos_y"))
(def saving-progress (jQ "#saving_progress"))
(def ct-rng (jQ "#currentTime_r"))
(def ct-txt (jQ "#currentTime_t"))
(def motion-list (jQ "#motion_list"))
(def gif-bg (jQ "#gif_bg"))
(def fun-fltrs (jQ "#fun_fltrs"))
(def obj-name-prefix (jQ "#obj-name-prefix"))

(def eye-r (jQ "#eye_r"))
(def eye-l (jQ "#eye_l"))
(def val-eye-l (jQ "#val_eye_l"))
(def val-eye-r (jQ "#val_eye_r"))
(def eye-r-sat (jQ "#eye_r-sat"))
(def eye-l-sat (jQ "#eye_l-sat"))
(def val-eye-l-sat (jQ "#val_eye_l-sat"))
(def val-eye-r-sat (jQ "#val_eye_r-sat"))

(def check-megane (jQ "#megane_off"))
(def reserve-megane-btn (jQ "#reserveGlassesBtn"))
(def slot-list (jQ "#slot-list"))
(def kazari-list (jQ "#kazari-list"))

(defn get-auto-update []
  (? js/PIXI.spine.Spine.globalAutoUpdate))

(defn set-auto-update [flag]
  (! js/PIXI.spine.Spine.globalAutoUpdate flag)
  )

;(def aa1 "FREE")
;(def aa4 "\xe6\x99\x82\xe4")
;(def aa5 "\xbb\xa3\xe9\x9d")
;(aef aa6 "\xa9\xe5\x91\xbd")
;(def aa2 "\x89\xe5\xbe\xa9\xe9")
;(def aa3 "\xa6\x99\xe6\xb8\xaf\x20")

(defn gen-gif [delay rpt0]
  (-> (jQ "#gifhere")
    (.empty))
  (js/add_spinner)
  ;(println delay rpt0)
  (let [
        ;rpt0 (parse-int rpt0 0)
        rpt (cond (= 1 rpt0) -1 (= 0 rpt0) 0 :else (dec rpt0))
        ;background (get options :background "#ffffff")
        background "#ffffff"
        skin (or (? _h.skin) "spine")
        filename (? (gstr/format "%s_%s_%d_%d_%s.gif" skin _h.motion _h.devide_by delay
                      (df/unparse dt-fmt (dt/time-now))))
        opt #js {:workerScript "js/gif.worker.js"
                 :workers 4 :quality 1
                 :repeat rpt
                 :transparent nil ;(:transparent options)
                 :background background
                 :comment (str filename
                            ;aa aa2 aa3 aa4 aa5 aa6
                            " naganeko "
                            (or js/chibi_gif_version "x"))
                 }
        gif (js/GIF. opt)
        frames (jQ ".gif_cut")
        len (.-length frames)
        ]
    ;(println opt)
    (-> frames
      (.each (fn [idx en]
               (.addFrame gif en #js {:delay delay :copy true})
               )))
    (.on gif "finished"
      (fn [blob]
        (let [b (.createObjectURL js/URL blob)
              img (jQ "<img></img>")
              ;skin (or (? _h.skin) "sp")
              ;filename (? (gstr/format "%s_%s_%d_%d_%s.gif" skin _h.motion _h.devide_by delay
              ;              (df/unparse dt-fmt (dt/time-now))))
              ]
          (! _h.anipic_blob blob) ; gif
          (! _h.anipic_name filename)
          (.attr img "src" b)
          (js/remove_spinner)
          (-> (jQ "#gifhere")
            (.empty)
            (.append img))
          ;(js/saveAs blob filename)
          )))

    (.render gif)
    ))


(defn gen-png [delay rpt]
  (-> (jQ "#gifhere")
    (.empty))
  (js/add_spinner)
  (let [
        ;rpt (get options :repeat 0)
        ;background (get options :background "#ffffff")
        skin (or (? _h.skin) "spine")
        filename (? (gstr/format "%s_%s_%d_%d_%s.png" skin _h.motion _h.devide_by delay
                      (df/unparse dt-fmt (dt/time-now))))
        gif (js/APNGBuilder.)
        frames (jQ ".gif_cut")
        len (.-length frames)
        ]
    ;(println opt)
    ;(println delay rpt)
    (.setDelay gif (/ (+ 0 delay) 1000.0))
    (.setNumPlays gif rpt)
    (-> frames
      (.each (fn [idx en]
               (.addFrame gif (.getAttribute en "src"))
               )))
    (js/setTimeout
      (fn []
        (let [blob (.getAPng gif)
              b (.createObjectURL js/URL blob)
              img (jQ "<img></img>")
              ]
          (! _h.anipic_blob blob) ; gif
          (! _h.anipic_name filename)
          (.attr img "src" b)
          (js/remove_spinner)
          (-> (jQ "#gifhere")
            (.empty)
            (.append img))
          ;(js/saveAs blob filename)
          ))
      (+ 500 (* len 20)))
    ;(.render gif)
    nil))


(defn draw-doll-bounds [doll]
  (let [bs (!> doll.getBounds)
        x (aget bs "x")
        y (aget bs "y")
        w (aget bs "width")
        h (aget bs "height")]
    (-> bounds-gr .clear (.lineStyle 1.0 0x0000FF 1 0) (.drawRect x y w h))
    ))

(defn update-bounds [doll]
  (let [bs (!> doll.getBounds)
        x (aget bs "x")
        y (aget bs "y")
        w (aget bs "width")
        h (aget bs "height")]
    (! _h.max-mounds.xmin (min (? _h.max-mounds.xmin) x))
    (! _h.max-mounds.ymin (min (? _h.max-mounds.ymin) y))
    (! _h.max-mounds.xmax (max (? _h.max-mounds.xmax) (+ x w)))
    (! _h.max-mounds.ymax (max (? _h.max-mounds.ymax) (+ y h)))

    ;(-> bounds-gr .clear (.lineStyle 1.0 0x0000FF 1 0) (.drawRect x y w h))
    ;    )
    ;(println x y w h)
    ))

(defn parse-motion-list0 [s]
  (println "s=" s)
  (->>
    s
    (#(str/split % #" *\n *"))
    ;(map #(str/split % #" +"))
    (filter (fn [[a]] (not (or (nil? a) (= "" a)))))
    ;(map (fn [[m c]] [m (parse-int c 1)]))
    ))
(defn parse-motion-list [s]
  (->>
    (parse-motion-list0 s)
    ;(mapcat (fn [[m c]] (repeat c m))) ; 추가
    ))

(declare gen-shots-multi0)
(declare gen-shots-multi0000)

(defn sum-duration [track]
  (loop [d 0.0 t track]
    (let [td (+ d (? t.animation.duration))
          nxt (? t.next)]
      (if (nil? nxt)
        td
        (recur td nxt)
        ))))

(defn set-split-method [total]
  (let [method (.val (jQ "#split_by"))
        total (u/round total 6)]
    ;(println "split method" method)
    (if (= "tnof" method)
      (let [tnof (js/parseInt (.val (jQ "#divide_by")))]
        (.val (jQ "#step_by")
          (u/round (* 1000.0 (/ total tnof)) 2))
        )
      (let [dpf (js/parseInt (.val (jQ "#step_by")))
            v (+ 0 (Math/ceil (/ (* 1000.0 total) dpf)))
            ]
        ;(println v total dpf)
        (.val (jQ "#divide_by")
          v)
        )
      ))
  nil)

;(defn preview-run [doll c delta]
;  (!> doll.update 0)
;  (update-bounds doll)
;  (when (> c 0)
;    ))

(declare setup-bg2)

(defn calc-bounds0 [doll mlist]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert (str "no motions : " (? doll.skin)))
      (do
        (set-auto-update false)
        (!> doll.state.clearTracks)                         ; 기존 트랙 제거
        (doall
          (map-indexed
            #(!> doll.state.addAnimationByName 0 %2 (= (inc %1) (count lst)) 0)
            lst))
        ;(doseq [[m c] lst]
        ;  (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
        (!> doll.update 0)
        (let [td (sum-duration (? doll.state.tracks.0))]
          (! _h.max-duration (max (? _h.max-duration) td))
          (.text (jQ "#total_duration") (u/round td 6))
          (set-split-method td)

          (let [step (/ (js/parseInt (.val (jQ "#step_by"))) 1000.0)
                total (js/parseInt (.val (jQ "#divide_by")))
                ]


            (mapv
              (fn [idx]
                ;(take-screen-shot cntr "#sshot" (gstr/format "%s_%s_%03d" skin ani idx))
                (update-bounds doll)
                (.update doll step)
                (.render app)
                nil)
              (range 0 total)
              )

            (let [bs (? _h.max-mounds)
                  xmin (Math/floor (? bs.xmin))
                  xmax (Math/floor (? bs.xmax))
                  ymin (Math/ceil (? bs.ymin))
                  ymax (Math/ceil (? bs.ymax))
                  width (- xmax xmin)
                  height (- ymax ymin)
                  ]

              (-> bounds-gr .clear
                (.lineStyle 1.0 0x0000FF 1 0)
                (.drawRect xmin ymin
                  width
                  height
                  ))
              (setup-bg2 cntr xmin ymin width height)
              )

            nil)
          )
        )
      ))
  nil)

(defn calc-bounds [doll mlist]
  (! _h.max-mounds #js {:xmin 10000 :ymin 10000 :xmax -10000 :ymax -10000})
  (! _h.max-duration 0)
  (! doll.mlst mlist)
  (let [a 1]
    (mapv (fn [d] (calc-bounds0 d (? d.mlst))) (.-dolls tgroup))
    (.text (jQ "#total_duration") (u/round (? _h.max-duration) 6))
    nil))

(defn preview-total [doll mlist]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert "No data in motion list")
      (do
        (! doll.mlst mlist)
        (set-auto-update false)
        (!> doll.state.clearTracks)                               ; 기존 트랙 제거
        (doall
          (map-indexed
            #(!> doll.state.addAnimationByName 0 %2 (= (inc %1) (count lst)) 0)
            lst))

        ;(doseq [[m c] lst]
        ;  (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))

        (!> doll.update 0)
        (let [td (sum-duration (? doll.state.tracks.0))]
          (.text (jQ "#total_duration") (u/round td 6))
          (set-split-method td)
          (js/setTimeout #(set-auto-update true) 10)
          )
        )
      ))
  nil)


(defn gen-shots-multi [doll mlist devide-by]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert (str "No motions: " (? doll.skin)))
      (do
        (! doll.mlst mlist)
        (gen-shots-multi0 doll lst devide-by)
        )
      ))
  nil)

(defn gen-shots-multi1 [doll lst devide-by]
  ;(-> (jQ "#sshot") .empty)
  ;(set-auto-update false)
  (!> doll.state.clearTracks)
  (println (.-skin doll) lst)
  (doall
    (map-indexed
      (fn [idx n]
        (let [
              ;loop? (= (inc idx) (count lst))
              loop? (if (? doll.mlooplast) (= (inc idx) (count lst)) false)
              ]
          (!> doll.state.addAnimationByName 0 n loop? 0)
          ))
      lst))
  ;
  ;(doseq [[m c] lst]
  ;  (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
  (!> doll.update 0)
  (let [total (sum-duration (? doll.state.tracks.0))
        ;ani (if (= 1 (count lst)) (ffirst lst) "mix")
        ]
    (! _h.max-duration (max (? _h.max-duration) total))
    (println (? doll.skin) total)
    )
  )

(defn gen-shots-multi0 [doll lst devide-by]
  (-> (jQ "#sshot") .empty)
  (set-auto-update false)
  (let [lst (parse-motion-list (? doll.mlst))]
    (! _h.max-duration 0)
    (mapv
      #(gen-shots-multi1 % (parse-motion-list (.-mlst %)) devide-by)
      (.-dolls tgroup))
    (let [total (? _h.max-duration)
          ani (if (= 1 (count lst)) (ffirst lst) "mix") ; 현재 선택된 인형 기준으로 이름이 정해짐
          ]
      (println "total" total)
      (set-split-method total)
      (js/setTimeout
        #(gen-shots-multi0000 doll total (.val (jQ "#divide_by")) ani)
        50)
      ))
  )

(defn after-gen-shots-multi0000 []
  (destroy-shot-canvas)
  (! gr.visible true)
  (! cross-gr.visible true)
  (! bounds-gr.visible true)
  ;(println step (+ step ))
  ;[endTime step]
  ;[full-d step]

  )

(defn gen-shots-multi0000 [doll full-d devide-by ani]

  ; 모션 리스트에서 하나씩 add ani한다.
  ;(!> doll.state.setAnimationByName 0 ani true)
  ;(!> doll.update 0)

  (! gr.visible false)
  (! cross-gr.visible false)
  (! bounds-gr.visible false)

  (.render app)

  (gen-shot-canvas (? _h.bg.width ) (? _h.bg.height))

  (let [
        ;endTime (? doll.state.tracks.0.endTime)
        ;full-d (sum-duration (? doll.state.tracks.0))
        d devide-by
        ;step (/ endTime d)
        step (/ full-d d)
        skin (or (? doll.skin) "sp")
        ;ani "f"

        west (if (c/flipX (? _h.doll)) "w_" "")

        delay (.val (jQ "#step_by"))

        ]
    ;(println "full duration:" full-d " delay:" step)
    (! _h.shot_blobs (arr))
    (! _h.skin skin)
    (! _h.motion ani)

    (! _h.devide_by devide-by)

    ; ????
    (! _h.max-mounds #js {:xmin 10000 :ymin 10000 :xmax -10000 :ymax -10000})

    (! _h.zip_file (gstr/format "%s_%s_%s%d.zip" skin ani west delay))

    (mapv
      (fn [idx t]
        (take-screen-shot cntr "#sshot" (gstr/format "%s_%s_%s%d_%03d" skin ani west delay idx))
        (.update tgroup step)
        (.render app)
        nil)
      (range 0 d)
      (range 0 full-d step)
      ;(range 0 endTime step)
      )

    )

  (after-gen-shots-multi0000)
  ;(destroy-shot-canvas)
  ;(! gr.visible true)
  ;(! cross-gr.visible true)
  ;(! bounds-gr.visible true)
  ;;(println step (+ step ))
  ;;[endTime step]
  ;;[full-d step]
  )

;;
;;(defn draw-cross [x y]
;;  ;(println x y)
;;  (->
;;    cross-gr
;;    .clear
;;    (.lineStyle 1 0xFFFF00 1 0 false)
;;
;;    (.drawRect 0 y outer-width 1)
;;    (.drawRect x 0 1 outer-width)
;;    ;(.moveTo 0 y)
;;    ;(.lineTo outer-width y)
;;    ;(.moveTo x 0)
;;    ;(.lineTo x outer-height)
;;    ))

(defn draw-cross [x y]
  ;;(println x y)
  (let [[cx cy] (zero-pos)
        ;;d 10
        ;;ax (- cx d)
        ;;bx (+ cx d)
        ;;ay (- cy d)
        ;;by (+ cy d)
        ]
    (->
      cross-gr
      .clear
      (.lineStyle 1 0xFFFF00 1 0 false)

      (.drawRect 0 y outer-width 1)
      (.drawRect x 0 1 outer-height)
      ;(.moveTo 0 y)
      ;(.lineTo outer-width y)
      ;(.moveTo x 0)
      ;(.lineTo x outer-height)
      (.lineStyle 1 0xFF0000 1 0 false)
      (.drawRect 0 cy outer-width 1)
      (.drawRect cx 0 1 outer-height)
      ;;(.beginFill 0xFF0000)
      ;;(.moveTo ax ay)
      ;;(.lineTo bx by)
      ;;(.moveTo ax by)
      ;;(.lineTo bx ay)
      ;;(.endFill)
      )))


(defn setup-bg2 [cnt x0 y0 w h]
  (let [bg
        (or (? _h.bg)
          (let [newbg (js/PIXI.Sprite. js/PIXI.Texture.EMPTY)] (! _h.bg newbg) newbg))
        ;width (? _h.width)
        ;height (? _h.height)
        ;x (* (- outer-width width) .5)
        ;y (* (- outer-height height) .5)
        offset (-> (jQ "#bs_margin") .val (parse-int 10))
        width  (+ w (* 2 offset))
        height (+ h (* 2 offset))
        x (- x0 offset)
        y (- y0 offset)
        ]
    (! bg.width width)
    (! bg.height height)

    ;(! bg.anchor.set 0.5)
    (!> bg.position.set x y)
    (!> bg-gr.position.set x y)
    (!> gr.position.set x y)
    (.addChild cnt bg)

    (! _h.width width)
    (! _h.height height)
    (! _h.bs_x x)
    (! _h.bs_y y)
    (.val waku-x x)
    (.val waku-y y)
    (.val waku-w width)
    (.val waku-h height)

    (->
      mask-gr
      .clear
      (.beginFill 0xffffff)
      (.drawRect x y width height)
      )

    (let [bgcolor (? _h.bgcolor)]
      (-> bg-gr .clear)
      (when bgcolor
        (let [c (js/parseInt _h.bgcolor)]
          ;(println c)
          (-> bg-gr
            (.beginFill c)
            (.drawRect 0 0 width height))
          )))
    (-> gr
      .clear
      (.lineStyle 1.0 0xff0000 1 0)
      (.drawRect
        0 0
        width height)
      )
    ))

(defn setup-bg [cnt]
  (let [bg
        (or (? _h.bg)
          (let [newbg (js/PIXI.Sprite. js/PIXI.Texture.EMPTY)] (! _h.bg newbg) newbg))
        width (? _h.width)
        height (? _h.height)
        x (? _h.bs_x)
        y (? _h.bs_y)
        ;x (* (- outer-width width) .5)
        ;y (* (- outer-height height) .5)
        ]
    (! bg.width width)
    (! bg.height height)
    ;(! bg.anchor.set 0.5)
    (!> bg.position.set x y)
    (!> bg-gr.position.set x y)
    (!> gr.position.set x y)
    (.addChild cnt bg)
    ;(aset cnt "mask" bg)

    ;(!> mask-gr.position.set x y)
    ;(aset cnt "mask" mask-gr)

    (-> bounds-gr .clear)

    (->
      mask-gr
      .clear
      (.beginFill 0xffffff)
      (.drawRect x y width height)
      )
    ;(.addChild cnt mask-gr)

    (let [bgcolor (? _h.bgcolor)]
      (-> bg-gr .clear)
      (when bgcolor
        (let [c (js/parseInt _h.bgcolor)]
          ;(println c)
          (-> bg-gr
            (.beginFill c)
            (.drawRect 0 0 width height))
          )))

    ;;(-> gr
    ;;  .clear
    ;;  (.lineStyle 1.0 0xff0000 1 0)
    ;;  (.drawRect
    ;;    0 0
    ;;    width height)
    ;;  )

    ))

(defn revoke-obj-url [k url]
  (do
    (!> js/window.URL.revokeObjectURL url)
    (js-delete (? _h.urls) k)
    ))

(defn create-obj-url
  ([f k]
   (let [url (!> js/window.URL.createObjectURL f)]
     (aset (? _h.urls) (or k (.-name f)) url)
     url))
  ([f] (create-obj-url f nil)))

(defn lookup-obj-url [k]
  (aget (? _h.urls) k))

(defn purge-all-objs []
  (let [urls (? _h.urls)]
  (mapv
    #(revoke-obj-url % (aget urls %))
    (js-keys urls))))

(defn extract-motions [doll]
  (let [animations (array-seq (? doll.spineData.animations))]
    (->>
      animations
      ;(filter #(> (.-duration %) 0.0))
      (map #(.-name %))
      (map #(gstr/format "<option value='%s'> %s </option>" % %))
      (apply str)
      (str "<option value=''>-----</option>")
      )
    ))

(defn onselect-doll [old-doll new-doll]
  ;(aset old-doll "filters" js/TDGroup.unselectedFilters)
  (if old-doll
    (do
      (! old-doll.mlst (or (.val motion-list) ""))
      ;;(! old-doll._obj_name_prefix (.val obj-name-prefix))
      (! old-doll.mlooplast (-> "#loop_last" jQ (.prop "checked")))
    ))
  ;(aset new-doll "filters" js/TDGroup.selectedFilters)
  (if new-doll
    (let [
          x0 (.-x new-doll)
          y0 (.-y new-doll)
          btn (jQ "#flipxBtn")
          [x y] (cvs->bone-xy x0 y0)
          ]
      (.val pos-x x)
      (.val pos-y y)
      (.val scale-x (? new-doll.scale.x))
      (.val scale-y (? new-doll.scale.y))
      (.val obj-name-prefix (? new-doll._obj_name_prefix))
      (.val motion-list (or (? new-doll.mlst) ""))
      (-> "#loop_last" jQ
        (.prop "checked" (? new-doll.mlooplast)))

      (.html dousa (extract-motions new-doll))
      (if (c/flipX (? new-doll))
        (.addClass btn "red-bg")
        (.removeClass btn "red-bg")
        )

      (draw-cross x0 y0)
      (-> (jQ "#doll-skin") (.text (? new-doll.skin)))
      (! _h.doll new-doll)
      )
    (let [a 1]
      ;(init-pos-xy)
      (-> (jQ "#doll-skin") (.text "N/A"))
      (.val obj-name-prefix "")
      (! _h.doll nil)
      (.removeClass (jQ "#flipxBtn") "red-bg")
      )
    )
  nil)

(defn gen-doll [ldr rsc name skin]
  ;;(if (= "3.8" js/window.sp_runtime_version)
  ;;     (sp38/gen-doll (sp38/gen-skel ldr rsc name skin))
  ;;     (sp/gen-doll (sp/gen-skel ldr rsc name skin)) )
  (let [d (sp38/gen-doll (sp38/gen-skel ldr rsc name skin))]
    (aset d "_obj_name_prefix" (str name "_"))
    (.log js/console "gen-doll=" (? d._obj_name_prefix) name)
    d))


(declare onload-spine-data0)

(defn onload-spine-data [key ldr rsc skin]
  (let [atlas-text (aget rsc (str key "-atlas") "data")
        ar (js/PIXI.spine.core.TextureAtlasReader. atlas-text)
        files (loop [page nil files [] line (.readLine ar)]
                (if line
                  (let [line (str/trim line)]
                    (cond
                      (= line "") (recur nil files (.readLine ar))
                      (nil? page) (recur 1 (conj files line) (.readLine ar))
                      :else (recur page files (.readLine ar))
                      ))
                 files))
        c (count files)
        go-next #(onload-spine-data0 key ldr rsc skin)
        ]
    (.log js/console files)
    (.log js/console "rsc" rsc)
    (if (= c 1)
      (go-next)
      (cond
        (not= (->> files (into #{}) count) (count files))
        (js/alert (str ".atlas problem: some .png names are duplicated.\n"
                    (str/join "\n" files)
                    "\n check your .atlas file")) ;
        (loop [k (first files) files (rest files)]
          (.log js/console "f=" k)
          (if k
            (if (nil? (aget rsc (-> k str/trim str/lower-case)))
              (do
                (js/alert (str ".png problem: " k " - file not found"))
                true)
              (recur (first files) (rest files)))))
        nil
        :else (go-next)))
    ))

(defn check-bs [d ]
  (let [bs (.getBounds d)
        wp-bs #js {:x 0 :y 0 :width outer-width :height outer-height}
        ]
    (when (u/out-of-area? bs wp-bs)
      (js/alert
        "Chibi might be placed outside the workspace.\n\nIt is either too big, or the root node is too far from the origin (yellow cross).\n\nDecrease scale (less than 0.2) or adjust the chibi's root node position.\n\nOr change the animation. Or... report the issue to me. "
                  )
      )))

(defn onload-spine-data0 [key ldr rsc skin]
  (let [doll (gen-doll ldr rsc key skin)
        ;;x0 (parse-int (.val pos-x) 0)
        ;;y0 (js/parseInt (.val pos-y))
        [x0 y0] [0 0]
        [x y] (bone->cvs-xy x0 y0)
        _ (println "xy=" x y)
        ]
    ;(if-let [old-doll (aget _h "doll")]
    ;  (.removeAllListeners old-doll))

    ;(aset doll "parentLayer" js/pxlyr)
    (aset doll "parentGroup" js/pxgrp)

    (! _h.doll doll)
    (js/fn_cb_d doll)

    (let [skn (? doll.spineData.skins.0.name)]
      (when (and skn (not= skin "default"))
        (!> doll.skeleton.setSkinByName skn)))

    (aset doll "interactive" true)
    (let [
          onDragStart (fn [ev]
                        (aset _h "_dragging" true)
                        ;(.log js/console (aget ev "target" "skin"))
                        ;(.log js/console ev)
                        ;(! _h.doll (? ev.target))
                        (onselect-doll (? _h.doll) (? ev.target))
                        (let [p (!> ev.data.getLocalPosition (? _h.doll.parent))
                              x (? _h.doll.x)
                              y (? _h.doll.y)
                              px (.-x p)
                              py (.-y p)
                              ]
                          (aset _h "_dx" (js/parseInt (- x px)))
                          (aset _h "_dy" (js/parseInt (- y py)))
                          )
                        )
          onDragMove (fn [ev]
                       (when (aget _h "_dragging")
                         (let [p (!> ev.data.getLocalPosition (? _h.doll.parent))
                               x (js/parseInt (.-x p))
                               y (js/parseInt (.-y p))
                               dx (? _h._dx)
                               dy (? _h._dy)
                               nx (+ x dx)
                               ny (+ y dy)
                               [cvs-nx cvs-ny] (cvs->bone-xy nx ny)
                               ]
                           ;;(js/console.log dx dy x y nx ny cvs-nx cvs-ny)
                           (draw-cross nx ny)
                           (!> _h.doll.position.set nx ny)
                           (.val pos-x cvs-nx)
                           (.val pos-y cvs-ny)
                           )))
          onDragEnd (fn [ev] (aset _h "_dragging" false))
          ]
      (.on doll "pointerdown" onDragStart)
      (.on doll "pointerup" onDragEnd)
      (.on doll "pointerupoutside" onDragEnd)
      (.on doll "pointermove" onDragMove)

      )

    (let [d (.val dousa)]
      (let [html (extract-motions doll)]
        (.html dousa html))
      (.val dousa d)
      )

    (.val waku-w (? _h.width))
    (.val waku-h (? _h.height))
    (.val waku-x (? _h.bs_x))
    (.val waku-y (? _h.bs_y))


    ;(.removeChildren cntr)
    (.addChild cntr bg-gr)
    (setup-bg cntr)

;
    (change-dousa (.val dousa))

    ;(!> doll.scale.set 0.8)
    ;(!> doll.position.set (/ outer-width 2) (.round js/Math (* outer-height .7)))

    (.addChild cntr tgroup)
    (.addChild tgroup doll)

    ;(.addChild stage doll)
    ;(.addChild cntr doll)

    (!> doll.position.set x y)

    (.val pos-x x0)
    (.val pos-y y0)

    ;;(change-scale-x doll)


    ; WebGL 에서 살짝 느리게 그려준다.
    ;;(js/setTimeout #(draw-cross x y) 50)

    (js/setTimeout #(onselect-doll nil doll) 50)
    ; 작업영역 밖인지 체크
    (js/setTimeout #(check-bs doll) 100)




    (if js/window.after_doll_loaded
      (js/window.after_doll_loaded doll))

    nil))




(defn load-spine-data-url [atlas-url skel-url png-url skin]
  (let [ldr (js/PIXI.Loader.)]
    (.add ldr (str skin "-atlas") atlas-url #js {:metadata #js {:type "atlas"}})
    (.add ldr (str skin "-skel") skel-url #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
    (.add ldr (str skin "-png") png-url #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (.load ldr (fn [ldr rsc]
                 (onload-spine-data skin ldr rsc skin)))
    ))

; spine 3.x 새로 만듬
; png skel 순서 변경
(defn load-spine-data-url3 [atlas-url png-url skel-url skin is-json]
  (let [ldr (js/PIXI.Loader.)]
    (.add ldr (str skin "-atlas") atlas-url #js {:metadata #js {:type "atlas"}})
    ;(.add ldr "Doll-skel" skel-url #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
    (if is-json
      (.add ldr (str skin "-json") skel-url #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr (str skin "-skel") skel-url #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}}) )
    (.add ldr (str skin "-png") png-url #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (.load ldr (fn[ldr rsc]
                 (onload-spine-data skin ldr rsc skin)))
    ))

(defn load-spine-data [atlas skel pngs skin is-json]
;  (.log js/console "load-spine-data" atlas skel pngs skin is-json)
  (purge-all-objs)
  ;(println "is-json" is-json )
  (let [ldr (js/PIXI.Loader.)
        atlas (create-obj-url atlas)
        skel (create-obj-url skel)
        blob-pngs (mapv #(create-obj-url %) pngs)
        ]
    (.add ldr (str skin "-atlas") atlas #js {:metadata #js {:type "atlas"}})
    (if is-json
      (.add ldr (str skin "-json") skel #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr (str skin "-skel") skel #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
      )
    ;(.add ldr "Doll-png" png #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (if (= (count pngs) 1)
      (.add ldr (str skin "-png") (first blob-pngs) #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
      (mapv
        (fn [f png]
          ;(.log js/console "png" (? f.name) png)
          (.add ldr (-> f (aget "name") str/trim str/lower-case)
          ;(.add ldr (-> f (aget "name"))
            png #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}}))
        pngs blob-pngs))
    ;(println "loaded=" png)
    ;(.attr (jQ "#png_holder") "src" png)

    (.load ldr (fn [ldr rsc]
                 (onload-spine-data skin ldr rsc skin)))
    ))

(defn load-files [files0]
  (.log js/console files0)
  (let [files (array-seq files0)
        endsWith (fn [f postfix]
                   (let [name (? f.name)]
                     (if (.endsWith name postfix) f nil)))
        atlas0 (some #(endsWith % ".atlas") files)
        atlas2 (some #(endsWith % ".atlas.txt") files)
        pngs   (filter #(endsWith % ".png") files)
        skel0  (some #(endsWith % ".skel") files)
        skel2  (some #(endsWith % ".skel.bytes") files)
        skel3  (some #(endsWith % ".skel.txt") files)
        json0  (some #(endsWith % ".json") files)
        json2  (some #(endsWith % ".json.txt") files)
        atlas (or atlas0 atlas2)
        json (or json0 json2)
        skel (or skel0 skel2 skel3)
        ]
    (mapv #(println (.-name %)) files)
    ;(println json)
    (cond
      (< (count files) 3)
      (alert "you have to select at least 3 files ( .png, .atlas, .skel(or json) )")
      (nil? atlas) (alert ".atlas file not found")
      (empty? pngs) (alert ".png files not found")
      (and (nil? skel) (nil? json)) (alert ".skel (or json) file not found")
      (and pngs (or skel json) atlas)
      (do
        (.log js/console "json?" (if json true false) "skel?" (if skel true false))
        (set! js/last_files files0)
        (load-spine-data
          atlas (or skel json) pngs
          (-> (or (aget (or skel json) "name") (? pngs.0.name)) (.replace #"^.+[/\\]" "") (.replace #"\..+$" ""))
          (if json true false)))
      :else (alert "files not ready.."))
    ))


(defn on-change-files [ev]
  ;(.log js/console ev);
  (load-files (? ev.target.files)))

(defn update-ct-ui [v]
  (.val ct-rng v)
  (.val ct-txt v)
  nil)

(defn set-currentTime [v]
  ;(println v)
  (let [d (? _h.doll)]
    (set-auto-update false)
    (c/seek! d v)
    (.update d 0)
    )
  nil)

(defn on-input-currentTime [ev]
  (let [t (? ev.target)
        $t (jQ t)
        v (js/parseFloat (.val $t))]
    (set-currentTime v)
    (update-ct-ui v)
    (draw-doll-bounds (? _h.doll))
    nil))

(defn change-dousa [m]
  ;(println "dousa=>" m "<")
  (let [d (? _h.doll)
        anins (apply hash-set (mapv #(.-name %) (? d.spineData.animations)))
        playing? (get-auto-update)
        ]
    (if (anins m)
    (do
      ;(set-auto-update false)
      (!> d.state.clearTracks)
      (when (or js/window.reset_pose_on_change_ani
              (= "pseudo_setup_pose" m))
        (!> d.skeleton.setToSetupPose))
      (sp/set-ani! d m)
      (! d._motion m)
      (let [dr (u/round (? d.state.tracks.0.animation.duration) 3)]
        (-> (jQ "#duration") (.text dr))
        (println "dr" dr)
        ;(aset d "autoUpdate" true)
        (set-auto-update true)
        (-> ct-txt (.attr "max" dr))
        (-> ct-rng (.attr "max" dr))
        ))
    (do
      (set-auto-update false)
      (.update d 0)
      ))))


;(apply-filter-dot (? _h.doll) (if chkd false true))

(defn apply-filter [d fltr-nm]
  (cond
    (= fltr-nm "dot")
    (aset d "filters" (arr (js/PIXI.filters.DotFilter.)))
    (= fltr-nm "crt")
    (aset d "filters" (arr (js/PIXI.filters.CRTFilter.)))
    :else
    (aset d "filters" nil)
    ))

;(defn apply-filter-dot [d flg]
;  (when d
;    (if flg
;      (aset d "filters" (arr (js/PIXI.filters.DotFilter.)))
;      (aset d "filters" nil)
;      )))

(defn on-change-dousa [ev]
  (let [t (? ev.target)
        $t (jQ t)
        m (.val $t)]
    (change-dousa m)
    nil))

(defn on-change-pos-xy [ev]
  (let [x (parse-int (.val pos-x) 0)
        y (parse-int (.val pos-y) 0)
        [x2 y2] (bone->cvs-xy x y)
        ]
    (js/console.log  x y x2 y2)
    (draw-cross x2 y2)
    (!> _h.doll.position.set x2 y2))
  nil)

(defn change-scale-x [doll]
  (fix-by-minmax scale-x)
  (let [sc (js/parseFloat (.val scale-x))]
    ;;(.setItem js/localStorage "ws_scale" (.val scale-x))
    (! doll.scale.x sc)
    )
  nil)

(defn change-scale-y [doll]
  (fix-by-minmax scale-y)
  (let [sc (js/parseFloat (.val scale-y))]
    ;;(.setItem js/localStorage "ws_scale" (.val scale-x))
    (! doll.scale.y sc)
    )
  nil)


(defn on-click-bsxy-center [ev]
  (let [
        width (? _h.width)
        height (? _h.height)
        x (* (- outer-width width) .5)
        y (* (- outer-height height) .5)
        ]
    (do
      (! _h.bs_x x)
      (! _h.bs_y y)
      (.val waku-x x)
      (.val waku-y y)
      (setup-bg cntr)
      )))


(defn on-change-waku-x [ev]
  (let [x (.val waku-x)]
    (do
      (! _h.bs_x x)
      (setup-bg cntr)
      )))
(defn on-change-waku-y [ev]
  (let [y (.val waku-y)]
    (do
      (! _h.bs_y y)
      (setup-bg cntr)
      )))

(defn on-change-waku-w [ev]
  (let [w (min outer-width (.val waku-w))]
    (do
      (.val waku-w w)
      (! _h.width w)
      (setup-bg cntr)
      )))

(defn on-change-waku-h [ev]
  (let [h (min outer-height (.val waku-h))]
    (do
      (.val waku-h h)
      (! _h.height h)
      (setup-bg cntr)
      )))

;;(defn on-click-gengif-btn [ev]
;;  (let [
;;        delay (parse-int (.val (jQ "#delay")) 25)
;;        rpt (parse-int (.val (jQ "#repeat")) 0)
;;        frame-delay (parse-int (.val (jQ "#step_by")) 25)
;;        ]
;;    (if (or
;;          (= delay frame-delay)
;;          (js/confirm
;;            (str "Warning!!\nframe delay of GIF(or APNG):" delay
;;              " is not equal to frame delay of PNGs:" frame-delay "."
;;              "\nClick OK to continue.")
;;          ))
;;      (if (= "png" (.val (jQ "#aniPicType")))
;;        (gen-png delay rpt)
;;        (gen-gif delay rpt))
;;      )
;;    ) nil)

; 프레임별 이미지 생성
(defn on-click-split-btn [ev]
  (let [
        doll (? _h.doll)
        ani (.val dousa)
        mlist (.val motion-list)
        divide-by (.val (jQ "#divide_by"))
        ]
    (cond
      (nil? doll) (alert "no doll data")
      ;:else (gen-shots doll ani divide-by)
      :else (gen-shots-multi doll mlist divide-by)
      )
    )
  nil)


;;(defn on-click-gen-atlas4x-btn [ev]
;;  (let [doll (? _h.doll)]
;;    (do
;;      (-> (jQ "#atlas4xModalCenter") (.modal "hide"))
;;      (js/setTimeout
;;        (fn [ev]
;;          (let
;;            [n (first (filter #(re-find #"\.atlas" %) (js-keys (? _h.urls))))
;;             u (aget (? _h.urls) n)
;;             v0 (.val (jQ "#atlas4x_upscale"))
;;             v1 (.val (jQ "#atlas4x_upscale_2"))
;;             upscale-str (if (not= "" v0) v0 v1)
;;             ]
;;            (features/gen-4x upscale-str n u)))
;;        10))
;;    nil))

;;(defn on-change-transparent[ev]
;;  (let [chkd (-> (jQ "#transparent") (.prop "checked"))]
;;    (if chkd
;;      (! _h.bgcolor nil)
;;      (! _h.bgcolor (str "0x" (str/replace (.val gif-bg) #"^#" ""))))
;;    )
;;  (setup-bg cntr)
;;  )

;;(defn save-skel-json [js-obj name]
;;  (-> (js/JSON.stringify js-obj nil 2)
;;    (js/Array)
;;    (js/Blob. {:type "text/plain;charset=utf-8"})
;;    (js/saveAs name)
;;    ))



(defn on-change-gif-bg [ev]
  (-> (jQ "#transparent") (.prop "checked" false))
  (! _h.bgcolor (str "0x" (str/replace (.val gif-bg) #"^#" "")))
  (setup-bg cntr)
  )

(defn save-frame [n lst]
  ;(println "")
  (-> saving-progress (.val n))
  (if-let [{:keys [blob name]} (first lst)]
    (do
      ;(println name "fn" (.-blobSaveAs js/window))
      (println "saving.." name)
      (js/saveAs blob name)
      (.setTimeout js/window (fn [] (save-frame (inc n) (rest lst))) 120)
      )
    ;(alert "saved.")
    )
  nil)

(defn on-click-save-allframes [ev]
  (let [lst (->> (? _h.shot_blobs) array-seq (sort-by :name))]
    (-> saving-progress
      (.prop "max" (count lst))
      (.val 0))
    (.setTimeout js/window (fn [] (save-frame 1 lst)) 10)
    ;(mapv (fn [{:keys [blob name]}] (js/saveAs blob name)) lst))
    ))

(defn save-zip []
  (let [lst (->> (? _h.shot_blobs) array-seq)
        zip (js/JSZip.)
        images (.folder zip "images")
        ]
    (mapv
      (fn [{:keys [blob name]}]
        (do
          ;(println "txtr name" name)
          (.file images name blob #js {:base64 false})
          ))
      lst)

    (->
      (.generateAsync zip #js {:type "blob"})
      (.then
        (fn [cnt]
          (js/saveAs cnt
            (or (? _h.zip_file) "images.zip"))
               ))
      )
    ))

(defn on-click-save-gif [ev]
  (println (? _h.anipic_name))
  (let [blob (? _h.anipic_blob)
        name (? _h.anipic_name)]
    (js/saveAs blob name)
    )
  nil)



(defn on-click-reset-ws []
  (let [iw (jQ "#input_ws_w")
        ih (jQ "#input_ws_h")]
    (fix-by-minmax iw)
    (fix-by-minmax ih)
    (js/setTimeout
      (fn []
        (when (js/confirm "Workspace will be reset!\nAre you sure?")
          (.setItem js/localStorage "ws_w_cc" (.val iw))
          (.setItem js/localStorage "ws_h_cc" (.val ih))
          (.reload js/document.location)
          )) 100
      ))
  nil)

(defn add-selected-motion [m]
  ;(println "add-selected-motion:" m)
  (let [s (.val motion-list)
        lst (parse-motion-list0 s)
        lastone (last lst)
        [lm c] lastone
        ]
    (cond
      (nil? lm) (.val motion-list (str m))
      ;;(= m lm)
      ;;(.val motion-list (str (str/replace s #" *\d+$" "") " " (inc c)))
      :else
      (.val motion-list (str s "\n" m))
      )
    )

  nil)


;;(defn init-pos-xy []
;;  (!> pos-x.val (/ outer-width 2))
;;  (!> pos-y.val (Math/round  (* outer-height .9)))
;;  )

(defn init-pos-xy []
  (let [
        ;;[x y] (zero-pos)
        [x y] [0 0]
        ]
    (!> pos-x.val x)
    (!> pos-y.val y))
  )

(defn combine-atlases []
  (let [
        dolls (.-dolls tgroup)
        name-list (mapv #(aget % "skin") dolls)
        prefix-list (mapv #(aget % "_obj_name_prefix") dolls)
        ;;ks (js/Object.keys js/atlas_text_map)
        ]
    (if (pos? (count dolls))
      (features/combine-atlas-save
        (->> js/atlas_text_map
          js/JSON.stringify
          js/JSON.parse)
        name-list
        prefix-list
        "combined.atlas")
      ))
  nil)

(defn combine-skels []
  (let [
        dolls (.-dolls tgroup)
        name-list (mapv #(aget % "skin") dolls)
        prefix-list (mapv #(aget % "_obj_name_prefix") dolls)
        ks (js/Object.keys js/skel_map)
        transforms #js {}
        f0 (fn [d]
             (let [x0 (aget d "position" "x")
                   y0 (aget d "position" "y")
                   [x y] (cvs->bone-xy x0 y0)
                   ]
               #js {
                    :x x
                    :y y
                    :scale (or (aget d "scale" "x") 1)
                    :sc_x  (or (aget d "scale" "x") 1)
                    :sc_y  (or (aget d "scale" "y") 1)
                    }))
        ]

    (when (pos? (count dolls))
      (doseq [k ks]
        (->> (f0 (aget js/dolls_map k)) (aset transforms k)))
      ;;(.log js/console "transforms=" transforms)
      (features/combine-skels-save
        (->> js/skel_map
          js/JSON.stringify
          js/JSON.parse)
        name-list
        prefix-list
        transforms
        "combined_s38.json"))
    ;;#js [name-list prefix-list]
    nil
    ))

(declare init-debug)

(defn init-core []

  ;(-> (jQ ".sagyoudai")
  ;    (.width outer-width)
  ;    (.height outer-height))

  (-> (jQ "#input_ws_w") (.val outer-width))
  (-> (jQ "#input_ws_h") (.val outer-height))

  (init-pos-xy)

  (let [cnt (dom/by-id "content")]
    (dom/destroy-children! cnt)
    (dom/append! cnt (.-view app))
    )
  (.removeChildren stage)

  (.clear gr)
  (.clear bg-gr)
  (.addChild cntr bg-gr)
  (.addChild stage cntr)
  (.addChild stage gr)
  (.addChild stage cross-gr)
  (.addChild stage bounds-gr)

  (set! js/pxgrp (new js/PIXI.display.Group 10 true))
  (set! js/bottom_grp (new js/PIXI.display.Group -1 false))
  (set! js/top_grp (new js/PIXI.display.Group 1000 false))
  (! app.stage.group.enableSort true)
  ;(set! js/pxlyr (js/PIXI.display.Layer.))
  (set! js/pxlyr (js/PIXI.display.Layer. js/pxgrp))
  (.addChild stage js/pxlyr)
  (.addChild stage (js/PIXI.display.Layer. js/bottom_grp))
  (.addChild stage (js/PIXI.display.Layer. js/top_grp))

  (aset tgroup "parentGroup" js/pxgrp)
  (aset cross-gr "parentGroup" js/pxgrp)
  ;(aset cross-gr "zOrder" -1000)


  (.val waku-w (? _h.width))
  (.val waku-h (? _h.height))
  (.val waku-x (? _h.bs_x))
  (.val waku-y (? _h.bs_y))

  (setup-bg cntr)

  (-> (jQ "#fileElem")
    (.off)
    (.on CHANGE on-change-files)
    )
  ;;(-> (jQ "#transparent")
  ;;  (.off)
  ;;  (.on CHANGE on-change-transparent)
  ;;  )
  ;;(-> (jQ "#clearFailStepChk")
  ;;  (.off)
  ;;  (.on CHANGE
  ;;    (fn [ev]
  ;;      (set! js/enable_clear_fail_step
  ;;        (.prop (jQ "#clearFailStepChk") "checked"))
  ;;        (js/alert "You need to open a Spine file set again to change this option.")
  ;;        )))

  ;;(-> (jQ "#pma_chk")
  ;;  (.off)
  ;;  (.on CHANGE
  ;;    (fn [ev]
  ;;      (set! js/window.skip_unpma (not (.prop (jQ "#pma_chk") "checked")))
  ;;        ;(js/alert "You need to open a Spine file set again to change this option.")
  ;;        )))

  ;;(-> (jQ "#genSetupPoseChk")
  ;;  (.off)
  ;;  (.on CHANGE
  ;;    (fn [ev]
  ;;      (set! js/is_gen_setup_pose
  ;;        (.prop (jQ "#genSetupPoseChk") "checked"))
  ;;        (js/alert "You need to open a Spine file set again to change this option.")
  ;;        )))

  (let [chk (jQ "#loop_last")]
    (-> chk
      (.off)
      (.on CHANGE
        (fn [ev]
          (if-let [d (? _h.doll)]
            (! d.mlooplast (.prop chk "checked")))
          ))
      ))
    ;(.prop "checked" (or (? old-doll.mlooplast) false)))

  (-> (jQ "#bringFwrdBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if-let [d (? _h.doll)]
          (let [idx (.getChildIndex tgroup d)
                len-1 (- (? tgroup.dolls.length) 1)
                ]
            (println "idx=" idx)
            (cond
              (< idx len-1) (do
                              (.swapChildren tgroup d (.getChildAt tgroup (+ idx 1)))
                              )
              :else nil
              )
            nil))
        )))
  (-> (jQ "#sendBckbtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if-let [d (? _h.doll)]
          (let [idx (.getChildIndex tgroup d)
                ;len-1 (- (? tgroup.dolls.length) 1)
                ]
            (println "idx=" idx)
            (cond
              (> idx 0) (do
                          (.swapChildren tgroup d (.getChildAt tgroup (- idx 1)))
                          )
              :else nil
              )
            nil))
        )))
  (-> (jQ "#alignYBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if-let [y (? _h.doll.y)]
          (doseq [d (.-dolls tgroup)]
            (! d.position.y y)
            ))
        )))

  (-> obj-name-prefix
    (.off)
    (.on "input"
      (fn [ev]
        (when-let [d (? _h.doll)]
          (! d._obj_name_prefix (str/trim (.val obj-name-prefix)))
          (js/console.log "prefix=" (? d._obj_name_prefix))
          )))
    )

  (-> ct-rng
    (.off)
    (.on "input" on-input-currentTime))
  ;(-> ct-txt
  ;    (.off)
  ;    (.on "input" on-input-currentTime))

  (-> dousa
    (.off)
    (.on "input" on-change-dousa)
    )
  (-> waku-x
    (.off)
    (.on CHANGE on-change-waku-x))
  (-> waku-y
    (.off)
    (.on CHANGE on-change-waku-y))
  (-> waku-w
    (.off)
    (.on CHANGE on-change-waku-w))
  (-> waku-h
    (.off)
    (.on CHANGE on-change-waku-h))

  (-> pos-x
    (.off)
    (.on CHANGE on-change-pos-xy))
  (-> pos-y
    (.off)
    (.on CHANGE on-change-pos-xy))

  ;;(.val scale-x js/ws_scale)

  (-> scale-x
    (.off)
    (.on CHANGE (fn []
                    (change-scale-x (? _h.doll))
                    )))
  (-> scale-y
    (.off)
    (.on CHANGE (fn []
                    (change-scale-y (? _h.doll))
                    )))
  ;;(-> gif-bg
  ;;  (.off)
  ;;  (.on CHANGE
  ;;    on-change-gif-bg))


  ;;(-> (jQ "#splitBtn")
  ;;  (.off)
  ;;  (.on CLICK on-click-split-btn))

  ;;(-> (jQ "#genGIFbtn")
  ;;  (.off)
  ;;  (.on CLICK on-click-gengif-btn))

  (-> (jQ "#playBtn")
    (.off)
    (.on CLICK
      (fn []
        ;(set! (.-autoUpdate (.-doll _h)) true)
        (set-auto-update true)
        )))

  (-> (jQ "#stopBtn")
    (.off)
    (.on CLICK
      (fn []
        ;(set! (.-autoUpdate (.-doll _h)) false)
        (set-auto-update false)
        )))

  ;;(-> (jQ "#flipxBtn")
  ;;  (.off)
  ;;  (.on CLICK (fn []
  ;;                 (let [d (? _h.doll)
  ;;                       flipped? (c/flipX d)
  ;;                       new-flipX (not flipped?)
  ;;                       btn (jQ "#flipxBtn")
  ;;                       ]
  ;;                   (if new-flipX
  ;;                     (.addClass btn "red-bg")
  ;;                     (.removeClass btn "red-bg")
  ;;                     )
  ;;                   (c/flipX! d new-flipX)
  ;;                   (.update d 0)
  ;;                   ))))

  (-> (jQ "#resetPoseBtn")
    (.off)
    (.on CLICK (fn []
                   (let [d (? _h.doll)]
                     (set-auto-update false)
                     (!> d.state.clearTracks)
                     (!> d.skeleton.setToSetupPose)
                     (!> d.update 0)
                     (.val dousa "")
                     ))))

  (-> (jQ "#fileSelect")
    (.off)
    (.on CLICK
      (fn [ev]
        (.trigger
          (jQ "#fileElem") CLICK)
        (.preventDefault ev)
        )))

  ;;(-> (jQ "#saveAllBtn")
  ;;  (.off)
  ;;  (.on CLICK on-click-save-allframes))

  ;;(-> (jQ "#saveAsZipBtn")
  ;;  (.off)
  ;;  (.on CLICK (fn []
  ;;               (save-zip)
  ;;               )))

  ;;(-> (jQ "#saveGifBtn")
  ;;  (.off)
  ;;  (.on CLICK on-click-save-gif))

  ;(-> (jQ "#rodoggie")
  ;    (.off)
  ;    (.on CLICK
  ;      #(js/setTimeout load-rodoggie-room 10)))


  (-> (jQ "#regenWSBtn")
    (.off)
    (.on CLICK on-click-reset-ws))

  ;;(-> (jQ "#addMotionBtn")
  ;;  (.off)
  ;;  (.on CLICK
  ;;    (fn []
  ;;      (let [v (.val dousa)]
  ;;        (if (= "" v)
  ;;          nil
  ;;          (add-selected-motion v)))))
  ;;  )
  ;;(-> (jQ "#resetListBtn")
  ;;  (.off)
  ;;  (.on CLICK
  ;;    (fn []
  ;;      (.val motion-list "")
  ;;      ))
  ;;  )
  ;;(-> (jQ "#centerBoundsBtn")
  ;;  (.off)
  ;;  (.on CLICK
  ;;    (fn [ev]
  ;;      (on-click-bsxy-center ev)
  ;;      ))
  ;;  )

  ;;(-> (jQ "#previewBtn")
  ;;  (.off)
  ;;  (.on CLICK
  ;;    (fn []
  ;;      (js/alert "sorry, temporary disabled :P")
  ;;      ;(preview-total (? _h.doll)
  ;;      ;  (.val motion-list)
  ;;      ;  )
  ;;      nil)))
  ;;(-> (jQ "#calcBoundsBtn")
  ;;  (.off)
  ;;  (.on CLICK
  ;;    (fn []
  ;;      (calc-bounds (? _h.doll)
  ;;        (.val motion-list)
  ;;        )
  ;;      nil)))

  (-> (jQ "#removeDollBtn")
    (.off)
    (.on CLICK
      (fn []
        (if-let [d (? _h.doll)]
          (do
            (.removeChild tgroup d)
            (onselect-doll d (first (.-dolls tgroup)))
            ))
        nil)))

  (-> (jQ "#reloadBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if js/last_files
          (load-files js/last_files)
          (alert "no previous data")
          ))))



  (-> (jQ "#combineAtlasesBtn")
    (.off)
    (.on CLICK
      #(js/window.setTimeout combine-atlases 10)
      ))

  (-> (jQ "#combineSKelsBtn")
    (.off)
    (.on CLICK
      #(js/window.setTimeout combine-skels 10)
      ))


  ;;(-> (jQ ".range-heterochromia")
  ;;    (.off)
  ;;    (.on "input" on-input-hue))
  ;;(-> (jQ ".number-heterochromia")
  ;;    (.off)
  ;;    (.on "input" on-input-hue2))
  ;;
  ;;(-> (jQ "#enable_heterochromia")
  ;;    (.off)
  ;;    (.on CHANGE on-change-heterochromia))

  (-> (jQ ".file-drop-area")
    (.off)
    (.on "dragenter dragleave dragover"
      (fn [ev]
        (.stopPropagation ev)
        (.preventDefault ev)
        ))
    (.on "drop"
      (fn [ev]
        (.preventDefault ev)
        (load-files
          (? ev.originalEvent.dataTransfer.files))
        ))
    )



  (register-ticker-listener
    (fn [delta]
      (when js/PIXI.spine.Spine.globalAutoUpdate
        (if-let [trk (c/track0 (? _h.doll))]
          (let [t (? trk.time)
                l (? trk.animation.duration)
                v (mod t l)]
            ;(.log js/console t l v)
            (when (not (js/isNaN v))
              (update-ct-ui v)
              ;(draw-doll-bounds (? _h.doll))
              )
            ))))
    "_range")



  (set! js/cntr cntr)
  (set! js/tgroup tgroup)


  (let [[x y] (zero-pos)]
    (draw-cross x y)
    )


  )


(jQ #(init-core))

(set! js/window.chibi_ns js/window.chibi_combine38)

;;(jQ #(experimentals/init-experimentals cntr))

(defn kick-reload []
  (-> (jQ "#reloadBtn")
    (.trigger CLICK))
  nil)



(defn on-js-reload []
  (println "js-reloaded")
  )
