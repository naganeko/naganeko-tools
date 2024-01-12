(ns spine-gif-extract.core
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
               [cljs.core.async.macros :only [go]]
               )
  (:require
    [domina :as dom] [goog.string :as gstr] [goog.string.format] [clojure.string :as str]
    [cljs-time.core :as dt] [cljs-time.format :as df]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]
    [renden.spine :as sp]
    [renden.spine-debug :as sp-debug]
    [renden.utils :as u]
    [spine-gif-extract.features :as features]
    [spine-gif-extract.experimentals :as experimentals]
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

(declare change-scale-xy init-pos-xy)
(declare regen-lists)

(set! js/_doll nil)
(defn _d [] js/_doll)

(set! js/enable_clear_fail_step false)
(set! js/is_gen_setup_pose false) ; unused

(defonce app (js/PIXI.Application.
               outer-width outer-height
               (obj :antialias true
                 :transparent true
                 ;:transparent "notMultiplied"
                 ;:backgroundColor bg-color
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

(defonce fltrL (js/PIXI.filters.ColorMatrixFilter.))
(defonce fltrR (js/PIXI.filters.ColorMatrixFilter.))

(declare on-change-heterochromia)

(defn parse-int [a b]
  (let [v (js/parseInt a)]
    (if (js/isNaN v) b v)))

(defn register-ticker-listener [f key]
  (let [t (.-ticker app)]
    (when-let [yet (not (aget t key))]
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
    (.attr i1 "title" name)
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

(defn gen-region-canvas-0 [img canvas ctx x y w h]
  (! canvas.width w)
  (! canvas.height h)
  ;(.clearRect ctx )
  ;(.drawImage ctx png-hldr x y w h 0 0 w h)
  (.drawImage ctx img x y w h 0 0 w h)
  )
(defn gen-region-canvas-r [img canvas ctx x y w h]
  (! canvas.width w)
  (! canvas.height h)
  (.translate ctx w 0)
  (.rotate ctx CW90D)
  ;(.drawImage ctx png-hldr x y h w 0 0 h w)
  (.drawImage ctx img x y h w 0 0 h w)
  )

(defn gen-region-canvas [canvas ctx rgn]
  (let [
        [x y w h r?] (mapv #(aget rgn %) ["x" "y" "width" "height" "rotate"])
        img (? rgn.page.rendererObject.source)
        ]
    (if r?
      (gen-region-canvas-r img canvas ctx x y w h)
      (gen-region-canvas-0 img canvas ctx x y w h))
    ))

; js/sd_atlas
(defn unpack-txtr [atlas]
  (let [regions (aget atlas "regions")
        canvas (cvs-shot)
        ctx (ctx-shot)
        ]
    (-> (jQ "#sshot") .empty)
    (! _h.shot_blobs (arr))
    (.log js/console atlas)
    (! _h.zip_file (str
                     (? _h.doll.skin)
                     "_unpacked"
                     (if (aget atlas "_skip_unpma") "" "_unpma")
                     ".zip"))

    (mapv
      (fn [rgn]
        (println (.-name rgn))
        (gen-region-canvas canvas ctx rgn)
        ;(if-not js/window.skip_unpma
        ;  (u/fix-ctx ctx (.-width canvas) (.-height canvas)))
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
(def scale-xy (jQ "#scale_xy"))
(def pos-x (jQ "#pos_x"))
(def pos-y (jQ "#pos_y"))
(def saving-progress (jQ "#saving_progress"))
(def ct-rng (jQ "#currentTime_r"))
(def ct-txt (jQ "#currentTime_t"))
(def motion-list (jQ "#motion_list"))
(def gif-bg (jQ "#gif_bg"))
(def fun-fltrs (jQ "#fun_fltrs"))

(def eye-r (jQ "#eye_r"))
(def eye-l (jQ "#eye_l"))
(def val-eye-l (jQ "#val_eye_l"))
(def val-eye-r (jQ "#val_eye_r"))
(def eye-r-sat (jQ "#eye_r-sat"))
(def eye-l-sat (jQ "#eye_l-sat"))
(def val-eye-l-sat (jQ "#val_eye_l-sat"))
(def val-eye-r-sat (jQ "#val_eye_r-sat"))

(def check-megane (jQ "#megane_off"))
;(def reserve-megane-btn (jQ "#reserveGlassesBtn"))
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
                            (or js/spine_gif_version "x"))
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
    (let [td (+ d (? t.endTime))
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
  (doseq [d (.-dolls tgroup)]
    (aset d "filters" js/TDGroup.unselectedFilters))
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
        (doseq [d (.-dolls tgroup)]
          (aset d "filters" js/TDGroup.unselectedFilters))
        (! doll.mlst mlist)
        (gen-shots-multi0 doll lst devide-by)
        )
      )) nil)

(defn gen-shots-multi1 [doll lst devide-by]
  ;(-> (jQ "#sshot") .empty)
  ;(set-auto-update false)
  (!> doll.state.clearTracks)
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
  (let [
        lst (parse-motion-list (? doll.mlst))
        ]
    (! _h.max-duration 0)
    (mapv
      #(gen-shots-multi1 % (parse-motion-list (.-mlst %)) devide-by)
      (.-dolls tgroup))
    (let [total (? _h.max-duration)
          ani (if (= 1 (count lst)) (first lst) "mix")
          ]
      (set-split-method total)
      (js/setTimeout
        #(gen-shots-multi0000 doll total (.val (jQ "#divide_by")) ani)
        50)
      ))

  ;(!> doll.state.clearTracks)                               ; 기존 트랙 제거
  ;(doseq [[m c] lst]
  ;  (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
  ;(!> doll.update 0)

  ;(let [total (sum-duration (? doll.state.tracks.0))
  ;      ani (if (= 1 (count lst)) (ffirst lst) "mix")
  ;      ]
  ;  (set-split-method total)
  ;  (js/setTimeout
  ;    #(gen-shots-multi0000 doll total (.val (jQ "#divide_by")) ani)
  ;    10)
  ;  )
  )

(defn after-gen-shots []
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

        west (if (? _h.doll.skeleton.flipX) "w_" "")

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

  (after-gen-shots)
  ;(destroy-shot-canvas)
  ;(! gr.visible true)
  ;(! cross-gr.visible true)
  ;(! bounds-gr.visible true)
  ;;(println step (+ step ))
  ;;[endTime step]
  ;;[full-d step]
  )

(defn gen-psd3 [doll shot-bg psd-children d-bs-x d-bs-y d-bs-w d-bs-h]
  (let [bones-cntr (sp-debug/draw-bones doll)
        layers #js []
        bones-layer #js {
                         :blendMode "normal"
                         :opacity   1 :transparencyProtected false
                         :clipping  false
                         :top       0
                         :left      0
                         ;:bottom    d-bs-h :right d-bs-w
                         :bottom    0 :right 0
                         :children #js []
                         :hidden    false
                         :opened true
                         :name      "bones"
                         ;:sectionDivider #js {:type 1 :key "norm"}
                         }
        ]

    (! bones-cntr.visible true)
    (doseq [child (.-children bones-cntr)]
      (aset child "visible" false))

    ;(let [a 1]
    ;  (! shot-bg.width 1)
    ;  (! shot-bg.height 1)
    ;  (let [canvas (gen-canvas)
    ;        ctx (.getContext canvas "2d")]
    ;    (aset bones-layer "imageData" (.getImageData ctx 0 0 1 1))))

    ; saveAs( new Blob([agPsd.writePsd(mypsd2)] , { type: 'application/octet-stream' } ) , "aaa.psd")

    ;; 도트??
    (when 1
      (doseq [child (.-children bones-cntr)]
        (aset child "visible" true)
        (.render app)
        (let [
              bs (.getBounds child)
              [x0 y0 w0 h0] (mapv #(aget bs (name %)) '(x y width height))
              x (Math/floor x0)
              y (Math/floor y0)
              w (Math/ceil w0)
              h (Math/ceil h0)
              top (- y d-bs-y)
              left (- x d-bs-x)
              layer #js {
                         :blendMode "normal"
                         :opacity   1 :transparencyProtected false
                         :clipping  false
                         :top       top
                         :left      left
                         :bottom    (+ top h) :right (+ left w)
                         :hidden    false
                         }
              name (str "BONE: " (? child._bone_name) "_" (? child._bone.data.parent.name))
              ]
          (aset layer "name" name)
          (when (and (> w 0) (> h 0))
            (! shot-bg.x x)
            (! shot-bg.y y)
            (! shot-bg.width w)
            (! shot-bg.height h)
            (let [canvas (gen-canvas)
                  ctx (.getContext canvas "2d")]
              (aset layer "imageData" (.getImageData ctx 0 0 w h)))
            (.push layers layer)
            ;(.push psd-children layer)
            )
          )
        (aset child "visible" false))
      )

      (aset bones-layer "children" layers)
      (.push psd-children bones-layer)

    psd-children))


(defn gen-psd2 [doll nr hs d-bs-x d-bs-y d-bs-w d-bs-h]
  (let [
        shot-bg (or (? _h.bg)
                  (let [newbg (js/PIXI.Sprite. js/PIXI.Texture.EMPTY)] (! _h.bg newbg) newbg))
        doll-bs (.getBounds doll)
        slot-idx-att-lst (features/attachments-map doll)
        sk (.-skeleton doll)
        slots (? doll.skeleton.slots)
        skin (or
               (aget doll "spineData" "defaultSkin")
               (aget doll "spineData" "skins" 0))
        get-att (.bind (? skin.getAttachment) skin)
        slot-containers (.-slotContainers doll)
        psd-children #js []
        ;[d-bs-x0 d-bs-y0] (mapv #(aget doll-bs (name %)) '(x y))
        ;d-bs-x (Math/floor d-bs-x0)
        ;d-bs-y (Math/floor d-bs-y0)
        get-internal-name (fn [obj] (? obj.rendererObject.name))
        ]
    (doseq [idx (keys slot-idx-att-lst)]
      ;(.log js/console idx)
      (if-let [slot (aget slots idx)]
        (let [cnt (aget slot-containers idx)]
          (doseq [att-nm (get slot-idx-att-lst idx [])]
            (if-let [att (get-att idx att-nm)]
              (let [typ (.-type att)
                    renobj-name (get-internal-name att)]
                ;(.log js/console idx att-nm renobj-name typ)
                (cond
                  (= typ 0) ; js/PIXI.spine.SpineRuntime.AttachmentType.region
                  (do
                    (! slot.sprites (or (? slot.sprites) #js {}))
                    (if-let [sp (aget (? slot.sprites) renobj-name)]
                      (aset sp "_rndn_name" renobj-name)
                      (let [sp (.createSprite doll slot att)]
                        (! sp.visible false)
                        (aset sp "_rndn_name" renobj-name)
                        (.addChild cnt sp))))
                  (or (= typ 2) (= typ 3))    ; mesh skinnedmesh
                  (do
                    (! slot.meshes (or (? slot.meshes) #js {}))
                    (if-let [m (aget (? slot.meshes) renobj-name)]
                      (aset m "_rndn_name" renobj-name)          ; do nothing
                      (let [m (.createMesh doll slot att)]
                        (! m.visible false)
                        (aset m "_rndn_name" renobj-name)
                        (.addChild cnt m))))
                  )))))))

    (doseq [sc slot-containers]
      (! sc._rndn_v_holder (? sc.visible))
      (! sc.visible true) ; 다 켠다.
      (doseq [child (.-children sc)]
        (! child._rndn_v_holder (? child.visible))
        (! child.visible false)
        ))
    (mapv
      (fn [idx slt sc]
        (doseq [child (.-children sc)]
          (aset child "visible" true)
          (.render app)
          (let [
                bs (.getBounds child)
                [x0 y0 w0 h0] (mapv #(aget bs (name %)) '(x y width height))
                x (Math/floor x0)
                y (Math/floor y0)
                w (Math/ceil w0)
                h (Math/ceil h0)
                top (- y d-bs-y)
                left (- x d-bs-x)
                layer #js {
                           :blendMode "normal"
                           :opacity   1 :transparencyProtected false
                           :hidden    true :clipping false
                           :top       top
                           :left      left
                           :bottom    (+ top h) :right (+ left w)
                           }
                c-nm (aget child "_rndn_name")
                name (str (? slt.data.name) "_" c-nm "_" idx)
                ;name (gstr/format "%s_%03d" c-nm (inc idx))
                ]

            (when (or (nil? c-nm) (empty? c-nm))
              (.log js/console "slot" slt)
              (.log js/console "child" child)
              )
            ;(setup-bg2 cntr x y w h)
            ;(.log js/console c-nm)
            (aset layer "name" name)
            (when (and (> w 0) (> h 0))
              (aset layer "hidden" (if
                                     (and (aget sc "_rndn_v_holder")
                                       (aget child "_rndn_v_holder"))
                                     false true))
              (! shot-bg.x x)
              (! shot-bg.y y)
              (! shot-bg.width w)
              (! shot-bg.height h)
              (let [canvas (gen-canvas)
                    ctx (.getContext canvas "2d")]
                (aset layer "imageData" (.getImageData ctx 0 0 w h)))
              (.push psd-children layer)
              )
            nil)
          (aset child "visible" false)
          ))
      (range) slots slot-containers)

    (if js/window.chibi_psd_include_bones
      (gen-psd3 doll shot-bg psd-children d-bs-x d-bs-y d-bs-w d-bs-h)
      )

    ; container visible 복원
    (doseq [sc slot-containers]
      (! sc.visible (? sc._rndn_v_holder))
      (doseq [child (.-children sc)]
        (! child.visible (? child._rndn_v_holder))
        ))
    psd-children))

; psd 생성
(defn gen-psd [doll nr hs]

  (.log js/console "nr hs" nr hs)

  (-> (jQ "#sshot") .empty)
  (set-auto-update false)
  (aset doll "filters" js/TDGroup.unselectedFilters)
  (let [
        shot-bg (or (? _h.bg)
                  (let [newbg (js/PIXI.Sprite. js/PIXI.Texture.EMPTY)] (! _h.bg newbg) newbg))
        ; 사이즈를 Spine바운더리로 정했는데... 왜지? 2021-04-29
        ;doll-bs (.getBounds doll)
        doll-bs (.getBounds shot-bg)
        [d-bs-x0 d-bs-y0 d-bs-w0 d-bs-h0] (mapv #(aget doll-bs (name %)) '(x y width height))
        d-bs-x (Math/floor d-bs-x0)
        d-bs-y (Math/floor d-bs-y0)
        d-bs-w (Math/ceil d-bs-w0)
        d-bs-h (Math/ceil d-bs-h0)]

    (-> (jQ "#bs_margin") (.val 0))

    (setup-bg2 cntr d-bs-x d-bs-y d-bs-w d-bs-h)

    (! gr.visible false)
    (! cross-gr.visible false)
    (! bounds-gr.visible false)

    (.render app)
    (let [
          bg-width  d-bs-w ; (? _h.bg.width)
          bg-height d-bs-h ; (? _h.bg.height)
          skin (or (aget doll "skin") "sp")
          ;doll-cntr-children (? doll.children)
          ;draw-order (? doll.skeleton.drawOrder)
          ;slots-data (? doll.spineData.slots)
          ;visible-ar (mapv #(aget %1 "visible") doll-cntr-children)
          motion (or (? doll._motion) "none")
          filename (gstr/format "%s_%s_%s.psd" skin motion
                     (df/unparse dt-fmt (dt/time-now)))
          psd #js {:width     bg-width :height bg-height
                   :channels 3 :bitPerChaneel 8
                   :colorMode 3 :imageData nil
                   ;:children #js []
                   }
          ;children #js []
          ]

      (.log js/console "bg=" (? _h.bg))

      (let [ctx (.getContext (gen-canvas) "2d")]
        (aset psd "imageData" (.getImageData ctx 0 0 bg-width bg-height)))

      (aset psd "children" (gen-psd2 doll nr hs d-bs-x d-bs-y d-bs-w d-bs-h))

      (set! js/window.mypsd psd)

      (js/setTimeout
        (fn []
          (-> (js/agPsd.writePsd psd #js {})
            (js/Array)
            (js/Blob. #js {:type 'application/octet-stream'})
            (js/saveAs filename)
            )) 10)
      ;saveAs(new Blob([writePsd(mypsd, {generateThumbnail:true})], {type:'application/octet-stream'}), "m200_maid.psd")

      ;(mapv (fn [c flag] (! c.visible flag)) doll-cntr-children visible-ar)
      )

    )

  (after-gen-shots)

  nil)

(defn save-psd [psd filename]
  (js/setTimeout
    (fn []
      (-> (js/agPsd.writePsd psd #js {})
        (js/Array)
        (js/Blob. #js {:type 'application/octet-stream'})
        (js/saveAs filename)
        )) 10)
  )


(defn draw-cross [x y]
  ;(println x y)
  (->
    cross-gr
    .clear
    (.lineStyle 1 0xFFFF00 1 0 false)

    (.drawRect 0 y outer-width 1)
    (.drawRect x 0 1 outer-width)
    ;(.moveTo 0 y)
    ;(.lineTo outer-width y)
    ;(.moveTo x 0)
    ;(.lineTo x outer-height)
    ))


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

    (-> gr
      .clear
      (.lineStyle 1.0 0xff0000 1 0)
      (.drawRect
        0 0
        width height)
      )
    ))

(defn revoke-obj-url [k url]
  (do
    (!> js/window.URL.revokeObjectURL url)
    (js-delete (? _h.urls) k)
    ))

(defn create-obj-url
  ([f k]
   (let [url (!> js/window.URL.createObjectURL f)]
     ;(println "create-obj-url" url)
     (aset (? _h.urls) (or k (.-name f)) url)
     ;(aset (? _h.urls) k url)
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


(defn find-right-eyes [doll]
  (if-let [slots (? doll.spineData.slots)]
    (let [names (map (fn [s] (.-name s)) slots)]
      (concat
        (filter #(re-find #"eyeR" %) names)
        (filter #(re-find #"Reye" %) names)
        (filter #(re-find #"R-eye" %) names)
        (filter #(re-find #"Eye_Right" %) names)
        )
      )))

(defn find-left-eyes [doll]
  (if-let [slots (? doll.spineData.slots)]
    (let [
          names (map (fn [s] (.-name s)) slots)
          ]
      (concat
       (filter #(re-find #"eyeL" %) names)
       (filter #(re-find #"Leye" %) names)
       (filter #(re-find #"L-eye" %) names)
       (filter #(re-find #"Eye_Left" %) names)
        )
      )))


(defn apply-eye-filter [sk n fltr]
  ;(println n)
  (let [sl (.findSlot sk n)
        sp (? sl.currentSprite.parent)
        m (? sl.currentMesh.parent)
        t (or sp m)]
    (when t
      (if (nil? fltr)
        (aset t "filters" nil)
        (aset t "filters" #js [fltr])
        ))))

(defn set-right-eye-filter [doll fltr]
  ;(println (? doll.name))
  (let [sk (? doll.skeleton)]
    (->>
      doll
      find-right-eyes
      (mapv #(apply-eye-filter sk % fltr))
      ))
  )

(defn set-left-eye-filter [doll fltr]
  ;(println (? doll.name))
  (let [sk (? doll.skeleton)]
    (->>
      doll
      find-left-eyes
      (mapv #(apply-eye-filter sk % fltr))
      ))
  )

(defn onselect-doll [old-doll new-doll]
  (doseq [d (.-dolls tgroup)] (aset d "filters" js/TDGroup.unselectedFilters))
  (if old-doll
    (do
      (! old-doll.mlst (or (.val motion-list) ""))
      (! old-doll.mlooplast (-> "#loop_last" jQ (.prop "checked")))
    ))
  (if new-doll
    (let [
          x (.-x new-doll)
          y (.-y new-doll)
          btn (jQ "#flipxBtn")
          ]
      (aset new-doll "filters" js/TDGroup.selectedFilters)
      (.val pos-x x)
      (.val pos-y y)
      (.val scale-xy (? new-doll.scale.x))
      (.val motion-list (or (? new-doll.mlst) ""))
      (-> "#loop_last" jQ
        (.prop "checked" (? new-doll.mlooplast)))

      (.html dousa (extract-motions new-doll))
      (if (? new-doll.skeleton.flipX)
        (.addClass btn "red-bg")
        (.removeClass btn "red-bg")
        )

      (draw-cross x y)
      (-> (jQ "#doll-skin") (.text (? new-doll.skin)))
      (! _h.doll new-doll)
      )
    (let [a 1]
      (init-pos-xy)
      (-> (jQ "#doll-skin") (.text "N/A"))
      (! _h.doll nil)
      (.removeClass (jQ "#flipxBtn") "red-bg")
      )
    )
  nil)

(declare onload-spine-data0)

(defn onload-spine-data [key ldr rsc skin]
  (let [atlas-text (aget rsc (str "Doll" "-atlas") "data")
        ar (js/PIXI.spine.SpineRuntime.AtlasReader. atlas-text)
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
    (if (= c 1)
      (go-next)
      (cond
        (not= (->> files (into #{}) count) (count files))
        (js/alert (str ".atlas problem: some .png names are duplicated.\n"
                    (str/join "\n" files)
          "\n check your .atlas file")) ;
        (loop [k (first files) files (rest files)]
          (if k
            ;(if (nil? (aget rsc k))
            (if (nil? (aget rsc (-> k str/trim str/lower-case)))
              (do
                (js/alert (str ".png problem: '" k "' - file not found"))
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
  (let [doll (sp/gen-doll (sp/gen-skel ldr rsc "Doll" skin))
        x (js/parseInt (.val pos-x))
        y (js/parseInt (.val pos-y))
        ]
    ;(if-let [old-doll (aget _h "doll")]
    ;  ;(.removeAllListeners old-doll)
    ;  (aset new-doll "filters" js/TDGroup.unselectedFilters))

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
                               nx (+ x (? _h._dx))
                               ny (+ y (? _h._dy))
                               ]
                           (draw-cross nx ny)
                           (!> _h.doll.position.set nx ny)
                           (.val pos-x nx)
                           (.val pos-y ny)
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

    (change-scale-xy doll)

    (let [shadow (sp/lk-slot-obj doll "shadow")
          s (jQ "#shadow_off")]
      ;(println name)
      ;(println (if (= "shadow" name) false true))
      (.prop s "checked" false)
      (.prop s "disabled" (if shadow false true))
      )

    ;(let [s (jQ "#filter_dot")]
    ;  (.prop s "checked" false)
    ;  ;(.prop s "disabled" (if shadow false true))
    ;  )


    (->>
      (map-indexed  (fn [i sl] [i (? sl.name )]) (? _h.doll.skeleton.data.slots))
      (filter (fn [[i n]] (re-find #"(?i)\s*(gla|gal)ss(es)?\s*" n)))
      ;(filter (fn [[i n]] (re-find #"(?i)glass(es)?" n)))
      first
      second
      (! _h.glasses_name)
      )

    (on-change-heterochromia nil)

    ; WebGL 에서 살짝 느리게 그려준다.
    (js/setTimeout #(draw-cross x y) 50)
    ;(js/setTimeout #(regen-lists doll) 80)

    ; 작업영역 밖인지 체크
    (js/setTimeout #(check-bs doll) 100)

    (if js/window.after_doll_loaded
      (js/window.after_doll_loaded doll))

    nil))




(defn load-spine-data-url
  ([atlas skel png skin is-json]
  (let [ldr (js/PIXI.loaders.Loader.)]
    (.add ldr "Doll-atlas" atlas #js {:metadata #js {:type "atlas"}})
    (if is-json
      (.add ldr "Doll-json" skel #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr "Doll-skel" skel #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}}))
    (.add ldr "Doll-png" png #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (.load ldr (fn [ldr rsc]
                 (onload-spine-data skin ldr rsc skin)))
    ))
  ([atlas skel png skin] (load-spine-data-url atlas skel png skin false))
  )

(defn load-spine-data [atlas skel pngs skin is-json]
  (purge-all-objs)

  (.log js/console pngs)

  (let [ldr (js/PIXI.loaders.Loader.)
        atlas (create-obj-url atlas)
        skel (create-obj-url skel)
        blob-pngs (mapv #(create-obj-url %) pngs)
    ;png (create-obj-url png "png")
        ]
    (.add ldr "Doll-atlas" atlas #js {:metadata #js {:type "atlas"}})
    (if is-json
      (.add ldr "Doll-json" skel #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr "Doll-skel" skel #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
      )
    ;(.add ldr "Doll-png" png #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (if (= (count pngs) 1)
      (.add ldr "Doll-png" (first blob-pngs) #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
      (mapv
        (fn [f png]
          ;(.log js/console "png" (? f.name) png)
          (.add ldr (-> f (aget "name") str/trim str/lower-case)
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
        ;png   (some #(endsWith % ".png") files)
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
    ;(mapv #(println (.-name %)) files)
    ;(println json)
    (cond
      (< (count files) 3)
      (alert "you have to select at least 3 files ( .png, .atlas, .skel(or json) )")
      (nil? atlas) (alert ".atlas file not found")
      (empty? pngs) (alert ".png files not found")
      (and (nil? skel) (nil? json)) (alert ".skel (or json) file not found")
      (and pngs (or skel json) atlas)
      (do
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
    ;(aset d "autoUpdate" false)
    (set-auto-update false)
    (! d.state.tracks.0.time v)
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
        (when (= "pseudo_setup_pose" m)
          (!> d.skeleton.setToSetupPose))
        (sp/set-ani! d m)
        (aset d "_motion" m)
        (let [dr (u/round (? d.state.tracks.0.endTime) 3)]
          (-> (jQ "#duration") (.text dr))
          ;(aset d "autoUpdate" true)
          (set-auto-update true)
          (-> ct-txt (.attr "max" dr))
          (-> ct-rng (.attr "max" dr))
          )
        (when js/window.single_motion_only
          ; reset list
          ; add motion
          (.val motion-list m)
          )
        )
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
  (let [x (js/parseInt (.val pos-x))
        y (js/parseInt (.val pos-y))]
    (draw-cross x y)
    (!> _h.doll.position.set x y))
  nil)

(defn change-scale-xy [doll]
  (fix-by-minmax scale-xy)
  (let [sc (js/parseFloat (.val scale-xy))]
    (.setItem js/localStorage "ws_scale" (.val scale-xy))
    (!> doll.scale.set sc)
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

(defn on-click-gengif-btn [ev]
  (let [
        delay (parse-int (.val (jQ "#delay")) 25)
        rpt (parse-int (.val (jQ "#repeat")) 0)
        frame-delay (parse-int (.val (jQ "#step_by")) 25)
        ]
    (if (or
          (= delay frame-delay)
          (js/confirm
            (str "Warning!!\nframe delay of GIF(or APNG):" delay
              " is not equal to frame delay of PNGs:" frame-delay "."
              "\nClick OK to continue.")
          ))
      (if (= "png" (.val (jQ "#aniPicType")))
        (gen-png delay rpt)
        (gen-gif delay rpt))
      )
    ) nil)

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

(defn on-click-gen-psd-btn [ev]
  (let [doll (? _h.doll)]
    (cond
      (nil? doll) (alert "select a chibi")
      :else
      (do
        (-> (jQ "#chibiPsdModalCenter") (.modal "hide"))
        (js/setTimeout
          #(let [nr (.val (jQ "#chibi-psd_naming-rule"))
                 hs (.val (jQ "#chibi-psd_hidden-slot"))
                 ]
             (if (= "true" (.val (jQ "#chibi-psd_include_bones")))
               (set! js/window.chibi_psd_include_bones true)
               (set! js/window.chibi_psd_include_bones false))
             (gen-psd doll nr hs)) 10))
      )
    nil))

(defn on-click-gen-atlas4x-btn [ev]
  (let [doll (? _h.doll)]
      (do
        (-> (jQ "#atlas4xModalCenter") (.modal "hide"))
        (js/setTimeout
          (fn [ev]
            (let
              [n (first (filter #(re-find #"\.atlas" %) (js-keys (? _h.urls))))
               u (aget (? _h.urls) n)
               v0 (.val (jQ "#atlas4x_upscale"))
               v1 (.val (jQ "#atlas4x_upscale_2"))
               upscale-str (if (not= "" v0) v0 v1)
               ]
              (features/gen-4x upscale-str n u)))
          10))
    nil))

(defn on-change-transparent[ev]
  (let [chkd (-> (jQ "#transparent") (.prop "checked"))]
    (if chkd
      (! _h.bgcolor nil)
      (! _h.bgcolor (str "0x" (str/replace (.val gif-bg) #"^#" ""))))
    )
  (setup-bg cntr)
  )

(defn on-change-shadow-off [ev]
  (let [chkd (-> (jQ "#shadow_off") (.prop "checked"))]
    ;(println chkd)
    (if-let [[shw _] (and (? _h.doll) (sp/lk-slot-obj (? _h.doll) "shadow"))]
      (! shw.visible
        (if chkd false true)))
    ))

(defn on-change-filter-dot [ev]
  (let [v (.val fun-fltrs)
        ;chkd (-> (jQ "#filter_dot") (.prop "checked"))
        ]
    (apply-filter (? _h.doll) v)
    ;;(println chkd)
    ;(if-let [[shw _] (and (? _h.doll) (sp/lk-slot-obj (? _h.doll) "shadow"))]
    ;  (! shw.visible
    ;    (if chkd false true)))
    ))

(defn on-change-megane-off [ev]
  (let [chkd (-> check-megane (.prop "checked"))
        name (? _h.glasses_name)
        d (? _h.doll)
        ]
    (when d
      (when name
        (if-let [gls-ar (sp/lk-slot-obj d name)]
          (! gls-ar.0.visible (if chkd false true)))
        ;(let [children (? _h.doll.children)
        ;      gls (aget children idx) ]
        ;  (! gls.children.0.visible (if chkd false true)))
        ))
    ))

(defn save-skel-json [js-obj name]
  (-> (js/JSON.stringify js-obj nil 1)
    (js/Array)
    (js/Blob. {:type "text/plain;charset=utf-8"})
    (js/saveAs name)
    ))

;(defn on-click-rv-megane [ev]
;  (let [chkd (-> check-megane (.prop "checked"))
;        idx (? _h.glasses_name)
;        d (? _h.doll)
;        ]
;    (.prop check-megane "disabled" true)
;
;    (if-let [m (? _h.megane.0)]
;      (do
;        (.prop check-megane "disabled" true)
;        (.addChild stage m)
;        (! m.visible true)
;        (!> m.position.set (/ outer-width 2) 40)
;        (.removeChild stage js/megane)
;        (set! js/megane m)
;        ))
;    ))


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
        images (.folder zip (str (? _h.doll.skin) "_images"))
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
          (.setItem js/localStorage "ws_w" (.val iw))
          (.setItem js/localStorage "ws_h" (.val ih))
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
      ;(= m lm) ; 숫자 넣기는 폐지
      ;(.val motion-list (str (str/replace s #" *\d+$" "") " " (inc c)))
      :else
      (.val motion-list (str s "\n" m))
      )
    )

  nil)


(defn on-change-heterochromia [ev]
  ;(println (? _h.doll))
  ;(if-let [d (? _h.doll)]
  (if (.prop (jQ "#enable_heterochromia") "checked")
    (do
      (.fadeIn (jQ "#heterochromia_ui"))
      (if-let [d (? _h.doll)]
        (do
          (set-left-eye-filter d fltrL)
          (set-right-eye-filter d fltrR)
          ))
      )
    (do
      (.fadeOut (jQ "#heterochromia_ui"))
      ;(.reset fltrL)
      ;(.reset fltrR)
      (if-let [d (? _h.doll)]
        (do
          (set-left-eye-filter d nil)
          (set-right-eye-filter d nil)
          ))
      )
    ))

(defn change-hue [hue-r hue-l sat-r sat-l]
  (.reset fltrR)
  (.reset fltrL)
  (.saturate fltrR sat-r)
  (.saturate fltrL sat-l)
  (.hue fltrR hue-r true)
  (.hue fltrL hue-l true)
  )

(defn on-input-hue [ev]
  (let [
         hue-r (js/parseFloat (.val eye-r))
         hue-l (js/parseFloat (.val eye-l))
         sat-r (js/parseFloat (.val eye-r-sat))
         sat-l (js/parseFloat (.val eye-l-sat))
         ]
    (change-hue hue-r hue-l sat-r sat-l)
    (.val val-eye-r hue-r)
    (.val val-eye-l hue-l)
    (.val val-eye-r-sat sat-r)
    (.val val-eye-l-sat sat-l)
    ))
(defn on-input-hue2 [ev]
  (let [
         hue-r (js/parseFloat (.val val-eye-r))
         hue-l (js/parseFloat (.val val-eye-l))
         sat-r (js/parseFloat (.val val-eye-r-sat))
         sat-l (js/parseFloat (.val val-eye-l-sat))
         ]
    (change-hue hue-r hue-l sat-r sat-l)
    (.val eye-r hue-r)
    (.val eye-l hue-l)
    (.val eye-r-sat sat-r)
    (.val eye-l-sat sat-l)
    ))

(defn init-pos-xy []
  (!> pos-x.val (/ outer-width 2))
  (!> pos-y.val (Math/round  (* outer-height .75)))
  )


(defn load-atlas-files [files0]
  (let [files (array-seq files0)
        v (-> (jQ "#atlas_resizer") .val)
        scale-str (if (= "" v) "4" v)
        resize (fn [file]
                 (let [url (!> js/window.URL.createObjectURL file)
                       name (.-name file)
                       ]
                   (features/gen-4x scale-str name url)
                   (!> js/window.URL.revokeObjectURL url)
                   ))
        ]
    (doseq [file files]
      (js/console.log (.-name file) file)
      (resize file)
      )))


(defn on-change-atlas-files [ev]
  ;(.log js/console ev);
  (load-atlas-files (? ev.target.files)))


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
  (-> (jQ "#transparent")
    (.off)
    (.on CHANGE on-change-transparent)
    )
  (-> (jQ "#clearFailStepChk")
    (.off)
    (.on CHANGE
      (fn [ev]
        (set! js/enable_clear_fail_step
          (.prop (jQ "#clearFailStepChk") "checked"))
          (js/alert "You need to open a Spine file set again to change this option.")
          )))

  (-> (jQ "#pma_chk")
    (.off)
    (.on CHANGE
      (fn [ev]
        ;(set! js/test_PMA_glstore (not (.prop (jQ "#pma_chk") "checked")))
        (set! js/window.skip_unpma (not (.prop (jQ "#pma_chk") "checked")))
        ;(js/alert "You need to open a Spine file set again to change this option.")
          )))

  (-> (jQ "#genSetupPoseChk")
    (.off)
    (.on CHANGE
      (fn [ev]
        (set! js/is_gen_setup_pose
          (.prop (jQ "#genSetupPoseChk") "checked"))
          (js/alert "You need to open a Spine file set again to change this option.")
          )))

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


  (-> fun-fltrs
    (.off)
    (.on CHANGE on-change-filter-dot)
    )
  (-> (jQ "#shadow_off")
    (.off)
    (.on CHANGE on-change-shadow-off)
    )
  (-> check-megane
    (.off)
    (.on CHANGE on-change-megane-off)
    )

  (-> (jQ "#bringFwrdBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if-let [d (? _h.doll)]
          (let [idx (.getChildIndex tgroup d)
                len-1 (- (? tgroup.dolls.length) 1)
                ]
            ;(println "idx=" idx)
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
            ;(println "idx=" idx)
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


  (-> (jQ "#saveSkelJsonBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (when (and js/hold_skel_json js/skel_json)
          (save-skel-json js/skel_json (str (? _h.doll.skin) "_s21.json")))
        )))

  (-> (jQ "#export4db")
    (.off)
    (.on CLICK
      (fn [ev]
        (when (and js/hold_skel_json js/skel_json)
          (let [json2 (.parse js/JSON (.stringify js/JSON js/skel_json))
                lst (remove nil? (mapcat u/bones-skewed-kamo (? _h.doll.spineData.animations)))
                ]
            (set! js/window.json_ex json2)
            (set! js/window.json_ex_chklst lst)
            (if (empty? lst)
              (-> (jQ "#exportDB_asis_confirmed") (.trigger CLICK))
              (do
                (-> (jQ "#exportDBModalCenter") (.modal))
                )))
        ))))

  (-> (jQ "#exportDB_asis_confirmed")
    (.off)
    (.on CLICK
      (fn [ev]
        (-> (jQ "#exportDBModalCenter") (.modal "hide"))
        (when (and js/hold_skel_json js/skel_json)
          (let [
                filename (str (? _h.doll.skin) "_db.json")
                json2 js/window.json_ex
                ]
            (features/fix-mesh json2)
            (features/cv4spine3! json2 false)
          (save-skel-json json2 filename))
        )
        )))
  (-> (jQ "#exportDB_n_confirmed")
    (.off)
    (.on CLICK
      (fn [ev]
        (-> (jQ "#exportDBModalCenter") (.modal "hide"))
        (when (and js/hold_skel_json js/skel_json)
          (let [
                filename (str (? _h.doll.skin) "_n_db.json")
                json2 js/window.json_ex]
            (u/deskew-for-db js/window.json_ex_chklst json2)
            (features/fix-mesh json2)
            (features/cv4spine3! json2 false)
          (save-skel-json json2 filename))
        )
        )))

  (-> (jQ "#export4s38")
    (.off)
    (.on CLICK
      (fn [ev]
        (when (and js/hold_skel_json js/skel_json)
          (let [
                filename (str (? _h.doll.skin) "_s38.json")
                json2 (.parse js/JSON (.stringify js/JSON js/skel_json))
                ]
            (features/cv4spine3! json2 true)
          (save-skel-json json2 filename))
        )
        )))

  (-> (jQ "#saveAtlas4xBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (-> (jQ "#atlas4xModalCenter") (.modal)))
      ))
  (-> (jQ "#execAtlas4xBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (on-click-gen-atlas4x-btn ev))
      ))

  (-> (jQ "#unpackTextureBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if-let [atlas (? _h.doll.spineData._sp_atlas)]
          (unpack-txtr atlas)
          (js/alert "No SD data.."))
        )))
  (-> (jQ "#saveUnPMATextureBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (let [atlas (? _h.doll.spineData._sp_atlas)]
          (cond
            (not atlas) (js/alert "No SD data..")
            (aget atlas "_skip_unpma") (js/alert "you don't need to save the texture because nothing changed.")
            :else (save-unpma-txtr atlas)))
        )))

  (-> (jQ "#generatePsdBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (let [doll (? _h.doll)]
          (cond
            (nil? doll) (alert "select a chibi")
            :else
            (-> (jQ "#chibiPsdModalCenter") (.modal)))
          ))))

  (-> (jQ "#chibiPsdExecBtn")
    (.off)
    (.on CLICK on-click-gen-psd-btn))

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

  (.val scale-xy js/ws_scale)
  (-> scale-xy
    (.off)
    (.on CHANGE (fn []
                    (change-scale-xy (? _h.doll))
                    )))
  (-> gif-bg
    (.off)
    (.on CHANGE
      on-change-gif-bg))


  (-> (jQ "#splitBtn")
    (.off)
    (.on CLICK on-click-split-btn))

  (-> (jQ "#genGIFbtn")
    (.off)
    (.on CLICK on-click-gengif-btn))

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

  (-> (jQ "#flipxBtn")
    (.off)
    (.on CLICK (fn []
                   (let [skel (? _h.doll.skeleton)
                         new-flipX (not (? skel.flipX))
                         btn (jQ "#flipxBtn")
                         ]
                     (if new-flipX
                       (.addClass btn "red-bg")
                       (.removeClass btn "red-bg")
                       )
                     (! skel.flipX new-flipX)
                     (!> _h.doll.update 0)
                     ))))

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

  (-> (jQ "#saveAllBtn")
    (.off)
    (.on CLICK on-click-save-allframes))

  (-> (jQ "#saveAsZipBtn")
    (.off)
    (.on CLICK (fn []
                 (save-zip)
                 )))

  (-> (jQ "#saveGifBtn")
    (.off)
    (.on CLICK on-click-save-gif))

  ;(-> (jQ "#rodoggie")
  ;    (.off)
  ;    (.on CLICK
  ;      #(js/setTimeout load-rodoggie-room 10)))


  (-> (jQ "#regenWSBtn")
    (.off)
    (.on CLICK on-click-reset-ws))

  (-> (jQ "#addMotionBtn")
    (.off)
    (.on CLICK
      (fn []
        (let [v (.val dousa)]
          (if (= "" v)
            nil
            (add-selected-motion v)))))
    )
  (-> (jQ "#resetListBtn")
    (.off)
    (.on CLICK
      (fn []
        (.val motion-list "")
        ))
    )
  (-> (jQ "#centerBoundsBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (on-click-bsxy-center ev)
        ))
    )

  (-> (jQ "#previewBtn")
    (.off)
    (.on CLICK
      (fn []
        (js/alert "sorry, temporary disabled :P")
        ;(preview-total (? _h.doll)
        ;  (.val motion-list)
        ;  )
        nil)))
  (-> (jQ "#calcBoundsBtn")
    (.off)
    (.on CLICK
      (fn []
        (calc-bounds (? _h.doll)
          (.val motion-list)
          )
        nil)))

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


  (-> (jQ ".range-heterochromia")
      (.off)
      (.on "input" on-input-hue))
  (-> (jQ ".number-heterochromia")
      (.off)
      (.on "input" on-input-hue2))

  (-> (jQ "#enable_heterochromia")
      (.off)
      (.on CHANGE on-change-heterochromia))

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


  ;;(-> (jQ "#atlas_files")
  ;;  (.off)
  ;;  (.on CHANGE #(load-atlas-files (? ev.target.files)))
  ;;  )
  (let [dd-area (jQ "#resizeAtlasArea")]
    (-> dd-area
     (.off)
     (.on "dragover"
       (fn [ev]
         (.stopPropagation ev)
         (.preventDefault ev)
         ))
     (.on "dragleave"
       (fn [ev]
         (.removeClass dd-area "dragover")
         (.stopPropagation ev)
         (.preventDefault ev)
         ))
     (.on "dragenter"
       (fn [ev]
         (.addClass dd-area "dragover")
         (.stopPropagation ev)
         (.preventDefault ev)
         ))
     (.on "drop"
       (fn [ev]
         (.preventDefault ev)
         (.removeClass dd-area "dragover")
         (load-atlas-files
           (? ev.originalEvent.dataTransfer.files))
         ))
     ))



  (register-ticker-listener
    (fn [delta]
      (when js/PIXI.spine.Spine.globalAutoUpdate
        (if-let [trk (? _h.doll.state.tracks.0)]
          (let [t (? trk.time)
                l (? trk.endTime)
                v (mod t l)]
            (when (not (js/isNaN v))
              (update-ct-ui v)
              ;(draw-doll-bounds (? _h.doll))
              )
            ))))
    "_range")



  (set! js/cntr cntr)
  (set! js/tgroup tgroup)


  )


;(init-core)
; $(function () {
;})
(jQ #(init-core))

(experimentals/init-experimentals cntr)

(defn kick-reload []
  (-> (jQ "#reloadBtn")
    (.trigger CLICK))
  nil)



(declare on-select-kazari)






(defn on-js-reload []
  (println "js-reloaded")
  )
