(ns chibi-deco.core
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
               [cljs.core.async.macros :only [go]]
               )
  (:require
    [domina :as dom] [goog.string :as gstr] [goog.string.format] [clojure.string :as str]
    [cljs-time.core :as dt] [cljs-time.format :as df]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]

    ; deco-3 : spine36, skb3 를 활성화
    [renden.spine :as sp]
    ;[renden.spine36 :as sp]
    ;[renden.spine38 :as sp38]
    [renden.chibi :as c]
    ;[renden.skb3 :as skb]

    [chibi-deco.features :as features]
    ;[cljs.js] [library.core]
    [cljs.js]
    )
  )

(enable-console-print!)

(def spine_rt (or js/PIXI.spine.SpineRuntime js/PIXI.spine.core))

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

(def png-hldr (.getElementById js/document "png_holder"))

(defonce _h (obj :width inner-width :height inner-height :bs_x 30 :bs_y 30 :urls {}))

(declare change-scale-xy)
(declare regen-lists)

(set! js/enable_clear_fail_step false)
(set! js/is_gen_setup_pose false)


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

(def stage (? app.stage))

(def gr (new js/PIXI.Graphics))
(def bounds-gr (new js/PIXI.Graphics))
(def bg-gr (new js/PIXI.Graphics))
(def mask-gr (new js/PIXI.Graphics))
(def cross-gr (new js/PIXI.Graphics))
(def cross-gr2 (new js/PIXI.Graphics))
(def cntr (new js/PIXI.Container))
;(def renderer (js/PIXI.autoDetectRenderer. outer-width outer-height))
(def renderer (js/PIXI.CanvasRenderer. outer-width outer-height))

(defonce fltrL (js/PIXI.filters.ColorMatrixFilter.))
(defonce fltrR (js/PIXI.filters.ColorMatrixFilter.))
(def fltrSelected (js/PIXI.filters.GlowFilter.))
(aset fltrSelected "color" 0xFF0000)

(declare on-change-heterochromia)

(defn parse-int [a b]
  (let [v (js/parseInt a)]
    (if (js/isNaN v) b v)))
(defn parse-float [a b]
  (let [v (js/parseFloat a)]
    (if (js/isNaN v) b v)))

(defn register-ticker-listener [f key]
  (let [t (.-ticker app)]
    (when-let [yet (not (aget t key))]
      (.add t f)
      (aset t key true)
      )))

(defn round [n p]
  (let [d (Math/pow 10 p)]
    (/ (Math/round (* n d)) d)))

(defn parseIntOrNil [n]
  (let [x (js/parseInt n)]
    (if (js/isNaN x) nil x)))

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

(defn fix-by-minmax [input]
  (let [minv (parseIntOrNil (.attr input "min"))
        maxv (parseIntOrNil (.attr input "max"))
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
    ;(.log js/console c-work)
    ;(println bg x y w h)
    ;(.clearRect ctx 0 0 (max width outer-width) (max height outer-height))
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


(defn calc-repack-size [rects power-of-two?]
  (let [
        ws (map (fn [r] (if (? r.rots) (+ (? r.x) (? r.height))
                                       (+ (? r.x) (? r.width)))) rects)
        hs (map (fn [r] (if (? r.rots) (+ (? r.y) (? r.height))
                                       (+ (? r.y) (? r.width)))) rects)
        pow2 (fn [n] (Math/pow 2 (Math/ceil (/ (Math/log n) (Math/log 2)))))
        w (apply max ws)
        h (apply max hs)]
    (.log js/console "txtr wh=" w h)
    (if power-of-two?
      [(pow2 w) (pow2 h)]
      [(Math/ceil w) (Math/ceil h)]
      )))

(defn repack-canvas-0 [img canvas ctx rx ry rw rh x y w h rot?]
  ;(.translate ctx 0 0)
  ;(.rotate ctx 0)
  (.setTransform ctx 1 0 0 1 0 0)
  ;(! canvas.width rw)
  ;(! canvas.height rh)
  ;(.clearRect ctx )
  ;(.drawImage ctx png-hldr x y w h 0 0 w h)
  (.drawImage ctx img rx ry rw rh x y w h)
  )
(defn repack-canvas-r [img canvas ctx rx ry rw rh x y w h rot?]
  ;(! canvas.width w)
  ;(! canvas.height h)
  (.setTransform ctx 1 0 0 1 0 0)
  (.translate ctx (+ x rw) y)
  (.rotate ctx CW90D)
  ;(.drawImage ctx png-hldr x y h w 0 0 h w)
  (.drawImage ctx img rx ry rh rw 0 0 h w)
  )


(defn repack0 [canvas ctx pack-rect rgn]
  (let [
        [name rx ry rw rh r?] (mapv #(aget rgn %) ["name" "x" "y" "width" "height" "rotate"])
        [x y rot?] (mapv #(aget pack-rect %) ["x" "y" "rot"])
        ;btxtr (aget rgn "page" "rendererObject")
        btxtr (aget rgn "page" "baseTexture") ; for Spine 3
        img (aget btxtr "source")
        _dw (aget rgn "_deco_w")
        _dh (aget rgn "_deco_h")
        ]
    (.log js/console name rx ry rw rh x y _dw _dh)
    (if r?
      (do
        ;(.log js/console r? name rx ry rw rh "repack=" x y rot?)
        (repack-canvas-r img canvas ctx rx ry rw rh x y (or _dw rw) (or _dh rh) rot?))
      (repack-canvas-0 img canvas ctx rx ry rw rh x y (or _dw rw) (or _dh rh) rot?)
      )
    ))


;(defn cvs-shot [] (. (jQ "#cvs_shot") get 0))
;(defn ctx-shot [] (. (cvs-shot) getContext "2d"))

(defn repack [atlas]
  (let [regions (aget atlas "regions")
        canvas (.get (jQ "#cvs_shot2") 0)
        ctx (.getContext canvas "2d")
        ]
    (.log js/console atlas)
    (.reset js/packer)
    ;(-> (jQ "#sshot") .empty)
    ;(! _h.shot_blobs (arr))
    ;(! _h.zip_file (str
    ;                 (? _h.doll.skin)
    ;                 "_unpacked"
    ;                 (if (aget atlas "_skip_unpma") "" "_unpma")
    ;                 ".zip"))

    (let [[w h] [1 2]
          rgns #js []
          ]
      (doseq [r regions]
        (.push rgns
          #js {
               :width  (or (? r._deco_w) (? r.width))
               :height (or (? r._deco_h) (? r.height))
               ;:width  (if (? r.rotate) (? r.height) (? r.width))
               ;:height (if (? r.rotate) (? r.width) (? r.height))
               :name   (? r.name)
               :item   r
               }))
      (set! js/window.rgns rgns)
      (.addArray js/packer rgns)
      (let [rects (aget js/packer "bins" 0 "rects")
            [w h] (calc-repack-size rects false)]
        (.log js/console "repack size" w h)
        (! canvas.width w)
        (! canvas.height h)
        (.clearRect ctx 0 0 w h)

        (mapv
          (fn [r]
            ;(.log js/console r)
            (repack0 canvas ctx r (? r.item))
            nil)
          rects)

        (set! js/window.new_atlas
          (features/gen-atlas
            (str (? _h.doll.skin) ".png")
            w h
            rects))

        ))
    )
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

;[function gen_apng () {
;apb = new APNGBuilder ()             ;
;apb.setDelay (0.025)                 ;
;function addFrame (o) {
;apb.addFrame (o) ;
;}
;$ (".gif_cut") .each (function (idx, en) {
; var src = en.getAttribute ("src") ;
; // console.log (src) ;
; addFrame (src) ;
; console.log (idx) ;
; }, this) ;
;
;// apngimg = $ ("<img></img>")       ;
;// apngblob = apb.getAPng ()         ;
;// apngurl = URL.createObjectURL (apngblob) ;
;// apngimg.attr ("src", apngurl)     ;
;// $ ("#gifhere") .empty () .append (apngimg) ;
;setTimeout (function () {
;$ ("<img></img>") .attr ("src", URL.createObjectURL (apb.getAPng ())) .appendTo ($ ("#gifhere") .empty ()) ;
;
;}, 800)     ;
;
;}]

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

(defn parse-motion-list [s]
  (->>
    s
    (#(str/split % #"\n"))
    (map #(str/split % #" +"))
    (filter (fn [[a]] (not (or (nil? a) (= "" a)))))
    (map (fn [[m c]] [m (parse-int c 1)]))
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
        total (round total 6)]
    ;(println "split method" method)
    (if (= "tnof" method)
      (let [tnof (js/parseInt (.val (jQ "#divide_by")))]
        (.val (jQ "#step_by")
          (round (* 1000.0 (/ total tnof)) 2))
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

(defn calc-bounds [doll mlist]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert "No data in motion list")
      (do
        (set-auto-update false)
        (!> doll.state.clearTracks)                               ; 기존 트랙 제거
        (doseq [[m c] lst]
          (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
        (!> doll.update 0)
        (let [td (sum-duration (? doll.state.tracks.0))]
          (.text (jQ "#total_duration") (round td 6))
          (set-split-method td)

          (let [step (/ (js/parseInt (.val (jQ "#step_by"))) 1000.0)
                total (js/parseInt (.val (jQ "#divide_by")))
                ]

            (! _h.max-mounds #js {:xmin 10000 :ymin 10000 :xmax -10000 :ymax -10000})

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


(defn preview-total [doll mlist]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert "No data in motion list")
      (do
        (set-auto-update false)
        (!> doll.state.clearTracks)                               ; 기존 트랙 제거
        (doseq [[m c] lst]
          (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
        (!> doll.update 0)
        (let [td (sum-duration (? doll.state.tracks.0))]
          (.text (jQ "#total_duration") (round td 6))
          (set-split-method td)
          (js/setTimeout #(set-auto-update true) 10)
          )
        )
      ))
  nil)


(defn gen-shots-multi [doll mlist devide-by]
  (let [lst (parse-motion-list mlist)]
    (if (empty? lst)
      (js/alert "No data in motion list")
      (gen-shots-multi0 doll lst devide-by)
      ))
  nil)

(defn gen-shots-multi0 [doll lst devide-by]
  (-> (jQ "#sshot") .empty)
  (set-auto-update false)
  (!> doll.state.clearTracks)                               ; 기존 트랙 제거
  (doseq [[m c] lst]
    (dotimes [n c] (!> doll.state.addAnimationByName 0 m false 0)))
  (!> doll.update 0)

  (let [total (sum-duration (? doll.state.tracks.0))
        ani (if (= 1 (count lst)) (ffirst lst) "mix")
        ]
    (set-split-method total)
    (js/setTimeout
      #(gen-shots-multi0000 doll total (.val (jQ "#divide_by")) ani)
      10)
    )
  )

(defn after-gen-shots-multi0000 []
  (destroy-shot-canvas)
  (! gr.visible true)
  (! cross-gr.visible true)
  (! cross-gr2.visible true)
  (! bounds-gr.visible true)
  ;(println step (+ step ))
  ;[endTime step]
  ;[full-d step]

  )

;(defn take-screen-stho-loop [skin ani idx step])


(defn gen-shots-multi0000 [doll full-d devide-by ani]

  ; 모션 리스트에서 하나씩 add ani한다.
  ;(!> doll.state.setAnimationByName 0 ani true)
  ;(!> doll.update 0)

  (! gr.visible false)
  (! cross-gr.visible false)
  (! cross-gr2.visible false)
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

    (! _h.max-mounds #js {:xmin 10000 :ymin 10000 :xmax -10000 :ymax -10000})

    (! _h.zip_file (gstr/format "%s_%s_%s%d.zip" skin ani west delay))

    (mapv
      (fn [idx t]
        (take-screen-shot cntr "#sshot" (gstr/format "%s_%s_%s%d_%03d" skin ani west delay idx))
        (.update doll step)
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

(defn draw-cross2 [x y]
  (->
    cross-gr
    .clear
    (.lineStyle 1 0xFF0000 1 0 false)

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
  ([file k]
   (let [url (!> js/window.URL.createObjectURL file)]
     ;(println "create-obj-url" url)
     (aset (? _h.urls) (or k (.-name file)) url)
     ;(aset (? _h.urls) k url)
     url))
  ([file] (create-obj-url file nil)))

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
      ;(str "<option value=''>-----</option>")
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


(defn onload-spine-data [key ldr rsc skin]
  (let [doll (sp/gen-doll (sp/gen-skel ldr rsc "Doll" skin))
        x (js/parseInt (.val pos-x))
        y (js/parseInt (.val pos-y))
        ]
    (if-let [old-doll (aget _h "doll")]
      (.removeAllListeners old-doll))

    (! _h.doll doll)

    (js/fn_cb_d doll)

    (aset doll "interactive" true)

    ;(! doll.alpha .1)

    (let [
          onDragStart (fn [ev]
                        (aset _h "_dragging" true)
                        ;(.log js/console ev)
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


    (.removeChildren cntr)
    (.addChild cntr bg-gr)
    (setup-bg cntr)

    ;
    ;(change-dousa (.val dousa))
    ;(.update doll 0)

    (let [v 1
          m (or (.val dousa) "")
          t (parse-float (.val ct-txt) 0)
          ]
      (set-auto-update false)
      (if (not= m "")
        (sp/set-ani! doll m)
        )
      (.update doll t)
      )

    ;(!> doll.scale.set 0.8)
    ;(!> doll.position.set (/ outer-width 2) (.round js/Math (* outer-height .7)))

    (.addChild cntr doll)

    (!> doll.position.set x y)

    (change-scale-xy doll)

    (on-change-heterochromia nil)

    ; WebGL 에서 살짝 느리게 그려준다.
    (js/setTimeout #(draw-cross x y) 50)


    (if-let [f js/window.after_load_cb]
      (js/setTimeout #(f doll) 100))

    nil))

(defn regen-draw-order [snames]
  (let [slist (jQ "#slot-draw-order")
        select-bn js/current_kazari
        kv (into {} (map-indexed (fn [k v] [v k]) snames))
        idx (get kv select-bn)
        ]
    (.empty slist)
    ;(.log js/console snames)
      (mapv
        (fn [i n]
          (->
            (jQ (str
                  "<option name='" n "' "
                  (if (= i idx) "selected" "")
                  ">"
                  n
                  "</option>"
                  ))
            ;(.on CLICK (fn [ev] (println n)))
            (.appendTo slist)
            ))
        (range -1 100000)
        (concat
          ;["_BOTTOM_"]
          snames
          ["_TOP_"]
          ))
    ))

(defn regen-doll-bones [names0]
  ;(.log js/console names0)
  (let [slist (jQ "#doll-bones")
        names (filter #(not (re-find #"^bone" %)) names0)
        select-bn (or js/current_kazari "root")
        b (js/_fb select-bn)
        pbn (if b (? b.data.parent.name))
        ]
    (.empty slist)
    (.log js/console names)
    (->>
      (concat [""] names [""])
      (mapv
        (fn [n]
          (->
            (jQ (str
                  "<option name='" n "' "
                  (if (= pbn n) "selected" "")
                  " >"
                  n
                  "</option>"
                 ))
            ;(.on CLICK (fn [ev] (println n)))
            (.appendTo slist)
            ))))
      ))

(defn regen-slot-list [names]
  (let [a 1]
    (.empty slot-list)
    ;(.log js/console slots)
    (->>
      (concat [] names )
      (mapv
        (fn [n]
          (->
            (jQ (str
                  "<button type=\"button\""
                  " class=\"list-group-item list-group-item-action\""
                  ">"
                  n "</button>"))
            (.on CLICK (fn [ev] (println n)))
            (.appendTo slot-list)
            )))
      )))

(defn regen-lists [doll bone]
  (let [
        slots (? doll.spineData.slots)
        bones (? doll.spineData.bones)
        snames  (mapv #(aget % "name") slots)
        bnames  (mapv #(aget % "name") bones)
        ]
      (regen-slot-list snames)
      (regen-draw-order snames)
      (regen-doll-bones bnames)

      (if bone
        (do
          (-> (jQ "#kazari_name") (.text (? bone.data.name)))
          (-> (jQ "#kazari_x") (.val (? bone.data.x)))
          (-> (jQ "#kazari_y") (.val (? bone.data.y)))
          (-> (jQ "#kazari_r") (.val (? bone.data.rotation)))
          (-> (jQ "#kazari_sc") (.val (? bone.data.scaleX))))
        (do
          (-> (jQ "#kazari_name") (.text ""))
          (-> (jQ "#kazari_x") (.val ""))
          (-> (jQ "#kazari_y") (.val ""))
          (-> (jQ "#kazari_r") (.val ""))
          (-> (jQ "#kazari_sc") (.val "")))
        )

  nil))


(defn alert [str] (js/alert str))


(defn load-spine-data-url [atlas-url skel-url png-url skin is-json]
  (let [ldr (js/PIXI.loaders.Loader.)]
    (.add ldr "Doll-atlas" atlas-url #js {:metadata #js {:type "atlas"}})
    ;(.add ldr "Doll-skel" skel-url #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
    (if is-json
      (.add ldr "Doll-json" skel-url #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr "Doll-skel" skel-url #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
      )
    (.add ldr "Doll-png" png-url #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
    (.load ldr (fn[ldr rsc]
                 (onload-spine-data skin ldr rsc skin)))
    ))

(defn load-spine-data [atlas skel pngs skin is-json]
  ;(purge-all-objs)
  ;(println "is-json" is-json )

  (.log js/console pngs)

  (let [ldr (js/PIXI.loaders.Loader.)
        atlas (create-obj-url atlas); "atlas")
        skel (create-obj-url skel); "skel")
        blob-pngs (mapv #(create-obj-url %) pngs); "png")]
        ]
    (.add ldr "Doll-atlas" atlas #js {:metadata #js {:type "atlas"}})
    (if is-json
      (.add ldr "Doll-json" skel #js {:xhrType "text" :metadata #js {:type "text" :skin skin}})
      (.add ldr "Doll-skel" skel #js {:xhrType "arraybuffer" :metadata #js {:type "skel" :skin skin}})
      )
    (if (= (count pngs) 1)
      (.add ldr "Doll-png" (first blob-pngs) #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
      (mapv
        (fn [f png]
          (.log js/console "png" (? f.name) png)
          (.add ldr (-> f (aget "name") str/trim str/lower-case)
                      png #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}}))
        pngs blob-pngs))
    ;(println "loaded=" png)
    ;(.attr (jQ "#png_holder") "src" png)

    (.load ldr
      (fn [ldr rsc]
        (.log js/console "rsc" rsc)
        (onload-spine-data skin ldr rsc skin)))
    ))

;

;(defn load-rodoggie []
;  (load-spine-data-url
;    "./spine/Rodoggie/Rodoggie.atlas"
;    "./spine/Rodoggie/Rodoggie.skel"
;    "./spine/Rodoggie/Rodoggie.png"
;    "Rodoggie"
;    ))
;
;(defn load-rodoggie-room []
;  (load-spine-data-url
;    "./spine/Rodoggie/Rodoggie.atlas"
;    "./spine/Rodoggie/Rodoggie_R.skel"
;    "./spine/Rodoggie/Rodoggie.png"
;    "Rodoggie"
;    ))


(defn load-files [files0]
  ;(println "on-change-files!!!!")
  (.log js/console files0)
  (let [files (array-seq files0)
        endsWith (fn [f postfix]
                   (let [name (? f.name)]
                     (if (.endsWith name postfix) f nil)))
        atlas0 (some #(endsWith % ".atlas") files)
        atlas2 (some #(endsWith % ".atlas.txt") files)
        ;png   (some #(endsWith % ".png") files)
        skel0  (some #(endsWith % ".skel") files)
        skel2  (some #(endsWith % ".skel.bytes") files)
        skel3  (some #(endsWith % ".skel.txt") files)
        json0  (some #(endsWith % ".json") files)
        json2  (some #(endsWith % ".json.txt") files)
        atlas (or atlas0 atlas2)
        json (or json0 json2)
        skel (or skel0 skel2 skel3)
        pngs (filter #(endsWith % ".png") files)
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

;(defn fix-2b14 []
;  (let [sk (? _h.doll.skeleton)]
;    (doseq [n ["Mbox1" "Mbox2"]]
;      ;(fn [n]
;      ;  (println "n=" n)
;        (aset (aget (.findSlot sk n) "currentMesh") "visible" false)
;        ;)
;      )))

(defn change-dousa [m]
  (println "dousa=>" m "<")
  (let [d (? _h.doll)
        anins (apply hash-set (mapv #(.-name %) (? d.spineData.animations)))
        playing? (get-auto-update)
        ]
    (when (anins m)
      ;(set-auto-update false)
      (!> d.state.clearTracks)
      (when (= "pseudo_setup_pose" m)
        (!> d.skeleton.setToSetupPose))
      (sp/set-ani! d m)
      (let [dr (round (? d.state.tracks.0.endTime) 3)]
        (-> (jQ "#duration") (.text dr))
        ;(aset d "autoUpdate" true)
        (set-auto-update true)
        (-> ct-txt (.attr "max" dr))
        (-> ct-rng (.attr "max" dr))
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
  (let [x (.val pos-x)
        y (.val pos-y)]
    (draw-cross x y)
    (!> _h.doll.position.set x y))
  nil)

(defn change-scale-xy [doll]
  (fix-by-minmax scale-xy)
  (let [sc (js/parseFloat (.val scale-xy))]
    (!> doll.scale.set sc)
    (.setItem js/localStorage "ws_scale" (.val scale-xy))
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

(defn on-change-transparent[ev]
  (let [chkd (-> (jQ "#transparent") (.prop "checked"))]
    (if chkd
      (! _h.bgcolor nil)
      (! _h.bgcolor (str "0x" (.val gif-bg))))
    )
  (setup-bg cntr)
  )

(defn on-change-shadow-off [ev]
  (let [chkd (-> (jQ "#shadow_off") (.prop "checked"))]
    ;(println chkd)
    (if-let [[shw _] (and (? _h.doll) (lk-slot-obj (? _h.doll) "shadow"))]
      (! shw.visible
        (if chkd false true)))
    ))

(defn on-change-filter-dot [ev]
  (let [v (.val fun-fltrs)
        ;chkd (-> (jQ "#filter_dot") (.prop "checked"))
        ]
    (apply-filter (? _h.doll) v)
    ;;(println chkd)
    ;(if-let [[shw _] (and (? _h.doll) (lk-slot-obj (? _h.doll) "shadow"))]
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
        (if-let [gls-ar (lk-slot-obj d name)]
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

(defn on-click-rv-megane [ev]
  (let [chkd (-> check-megane (.prop "checked"))
        idx (? _h.glasses_name)
        d (? _h.doll)
        ]
    (.prop check-megane "disabled" true)

    (if-let [m (? _h.megane.0)]
      (do
        (.prop check-megane "disabled" true)
        (.addChild stage m)
        (! m.visible true)
        (!> m.position.set (/ outer-width 2) 40)
        (.removeChild stage js/megane)
        (set! js/megane m)
        ))
    ))

(defn find-reye [doll]
;  (if-let [slots (? doll.skeleton.slots)]
  (if-let [slots (? doll.spineData.slots)]
    (let [
;          names (remove nil? (map (fn [s] (? s.currentSpriteName)) slots))
          names (map (fn [s] (.-name s)) slots)
          ;reye (first (filter #(re-find #"eyeR" %) names))
          ]
      (or
        (first (filter #(re-find #"eyeR" %) names))
        (first (filter #(re-find #"R-eye" %) names))
        (first (filter #(re-find #"Reye" %) names))
        (first (filter #(re-find #"Eye_Right" %) names))
        )
      )))

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
(defn on-click-apply-megane [ev]
  (let [sk (? _h.doll.skeleton)
        reye-n (find-reye (? _h.doll))
        ;sl (js/findSlot0 sk reye-n)
        sl (.findSlot sk reye-n)
        sps-obj (or (aget sl "sprites") (aget sl "meshes"))
        sp (aget sps-obj (first (js-keys sps-obj)))
        p (.-parent sp)
        mgn js/megane
        ]
    (.addChild p mgn)
    (! mgn.rotation (* (/ Math/PI 180) (js/parseInt (.val (jQ "#adjust_gls_r" )))))
    (! mgn.position.x (js/parseInt (.val (jQ "#adjust_gls_x" ))))
    (! mgn.position.y (js/parseInt (.val (jQ "#adjust_gls_y" ))))
    (! mgn.scale.x (js/parseFloat (.val (jQ "#adjust_gls_sx" ))))
    (! mgn.scale.y (js/parseFloat (.val (jQ "#adjust_gls_sy" ))))
    ))


(defn on-change-gif-bg [ev]
  (-> (jQ "#transparent") (.prop "checked" false))
  (! _h.bgcolor (str "0x" (.val gif-bg)))
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
          (.setItem js/localStorage "ws_w" (.val iw))
          (.setItem js/localStorage "ws_h" (.val ih))
          (.reload js/document.location)
          )) 100
      ))
  nil)

(defn add-selected-motion [m]
  ;(println "add-selected-motion:" m)
  (let [s (.val motion-list)
        lst (parse-motion-list s)
        lastone (last lst)
        [lm c] lastone
        ]
    (cond
      (nil? lm) (.val motion-list (str m))
      (= m lm)
      (.val motion-list (str (str/replace s #" *\d+$" "") " " (inc c)))
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


(defn init-core []

  ;(-> (jQ ".sagyoudai")
  ;    (.width outer-width)
  ;    (.height outer-height))

  (-> (jQ "#input_ws_w") (.val outer-width))
  (-> (jQ "#input_ws_h") (.val outer-height))

  (!> pos-x.val (/ outer-width 2))
  ;(!> pos-y.val (Math/round  (* outer-height .85)))
  (!> pos-y.val (Math/round  (* outer-height
                               (if (aget spine_rt "Bone" "yDown") .85 .15)
                               )))


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
  (.addChild stage cross-gr2)
  (.addChild stage bounds-gr)

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
        (set! js/test_PMA_glstore (not (.prop (jQ "#pma_chk") "checked")))
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

  (-> (jQ "#saveSkelJsonBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (when (and js/hold_skel_json js/skel_json)
          (save-skel-json js/skel_json (str (? _h.doll.skin) "_deco.json")))
        )))

  (-> (jQ "#export4db")
    (.off)
    (.on CLICK
      (fn [ev]
        (when (and js/hold_skel_json js/skel_json)
          (let [
                filename (str (? _h.doll.skin) ".json")
                json2 (.parse js/JSON (.stringify js/JSON js/skel_json))
                ]
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
                filename (str (? _h.doll.skin) ".json")
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
        (let
          [n (first (filter #(re-find #"\.atlas" %) (js-keys (? _h.urls))))
           u (aget (? _h.urls) n)
              ]
        (features/gen-4x n u))
        )))

  (-> (jQ "#unpackTextureBtn")
    (.off)
    (.on CLICK
      (fn [ev]
        (if js/sd_atlas
          (unpack-txtr js/sd_atlas)
          (js/alert "No SD data.."))
        )))

  (-> reserve-megane-btn
    (.off)
    (.on CLICK on-click-rv-megane)
    )
  (-> (jQ "#applyGlsBtn")
    (.off)
    (.on CLICK on-click-apply-megane)
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
        (preview-total (? _h.doll)
          (.val motion-list)
          )
        nil)))
  (-> (jQ "#calcBoundsBtn")
    (.off)
    (.on CLICK
      (fn []
        (calc-bounds (? _h.doll)
          (.val motion-list)
          )
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

  )


(init-core)

(defn kick-reload []
  (.setTimeout js/window
    #(-> (jQ "#reloadBtn")
      (.trigger CLICK)) 10)
  nil)

(declare on-select-kazari)

(defn regen-kazari-list [names]
  (let [kazari-list (jQ "#kazari-list")]
    (-> kazari-list
      (.empty)
      (.append
        (->
          (jQ (str
                "<button type='button' class='list-group-item list-group-item-action'>"
                "Clear Selection"
                "</button>"
                ))
          (.on CLICK
            (fn [ev]
              (on-select-kazari nil))))))
    (doseq [n names]
      (let [blob (lookup-obj-url n)
            ]
        (-> kazari-list
          (.append
            (->
              (jQ (str
                    "<button type='button' class='list-group-item list-group-item-action'>"
                    "<img class='kazari-thumb' height='32' src='" blob "'/>"
                    n                                       ; n2
                    "</button>"
                    ))
              (.on CLICK
                (fn [ev]
                  (on-select-kazari n)))
              ))
          )))
    ))

(defn onload-kazari-files [ldr rsc names ]
  ;(set! js/kazari_txtr_obj #js {})
  ;(set! js/kazari_txtr_names #js [])
  (let [sc0 (parse-float (.val (jQ "#kazari_master_sc")) 1)
        ]
    (println "names=" names "sc=" sc0)
    (doseq [n names]
      (let [r (aget rsc n)
            ; TODO 왜 그런지 loader 에러
            ;bt (new js/PIXI.BaseTexture r)
            ;bt (js/PIXI.BaseTexture. r)
            ;blob (lookup-obj-url n)
            blob (.-data r)
            bt2 (js/PIXI.BaseTexture.from. blob)
            n2 n
            ]
        ;(! bt2.resolution 4)
        (when-not (aget js/kazari_txtr_obj n)
          (.push js/kazari_txtr_names n2))
        (aset js/kazari_txtr_obj n2 bt2)
        (! bt2._deco_msc sc0)
        ))
    ;(new js/PIXI.BaseTexture png-data)
    (regen-kazari-list js/kazari_txtr_names)

    (set! js/current_kazari (first names))

    (println "kazari loaded.")

    (kick-reload)

    ))



(defn load-kazari-files [files0]
  ;(println "on-change-files!!!!")
  ;(println files0)
  (let [files1 (array-seq files0)
        ;endsWith (fn [f postfix]
        ;           (let [name (? f.name)]
        ;             (if (.endsWith name postfix) f nil)))
        files (filter (fn [f] (re-find #"\.png$" (.-name f))) files1)
        names (mapv #(.-name %) files)
        names2 (->> (mapv #(str/replace % #"\.png$" "") names)
                 (mapv #(str "d_" %)))
        ldr (js/PIXI.loaders.Loader.)
        ]
    ;(mapv #(println (.-name %)) files)
    ;(-> (jQ "#gifhere") (.empty))
    (mapv
      (fn [f n]
        (.add ldr
          n
          (let [blob (create-obj-url f n)
                ;n2 (str/replace n #"\.png$" "")
                ]
            ;(.log js/console "kazari file=" n blob)
            blob)
          #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}}
          ))
      files names2)
    (.load ldr (fn [ldr rsc] (onload-kazari-files ldr rsc names2)))
    nil
  ))

(-> (jQ ".file-drop-area-2")
  (.off)
  (.on "dragenter dragleave dragover"
    (fn [ev]
      (.stopPropagation ev)
      (.preventDefault ev)
      ))
  (.on "drop"
    (fn [ev]
      (.preventDefault ev)
      (let [files (? ev.originalEvent.dataTransfer.files)]
        ;(.log js/console files)
        (load-kazari-files files)
        )
      ))
  )
;(set! window.autoload_chibi 1)

(if js/window.autoload_chibi
  (let [f1
        (fn []
          (load-spine-data-url
            "./test3/22.atlas" "./test3/22.skel" "./test3/22.png" "az22" false
            )

          ;(.setTimeout js/window
          ;  #(let [
          ;        ;t (new PIXI)
          ;        t (js/PIXI.Sprite.fromImage. "test2/template0.png")
          ;         d (? _h.doll)
          ;         x (.-x d)
          ;         y (.-y d)
          ;        ]
          ;     (. cntr addChildAt t (.-length cntr))
          ;     ;(. cntr addChild t)
          ;     (! t.alpha 0.5)
          ;     (!> t.position.set x y)
          ;     (!> t.anchor.set 0.5 0.908)
          ;     (!> t.scale.set 0.65)
          ;     (set! js/t0 t)
          ;    ) 80)

          nil
          )]
    (-> (jQ "#reloadBtn")
      (.off)
      (.on CLICK
        (fn [ev]
          (f1)
          )))
    (f1))
  )

(declare after-load-cb)

(defn on-select-kazari [name]
  (println "on-select-kazari" name)
  (set! js/current_kazari name)
  (after-load-cb nil)
  )

(-> (jQ "#doll-bones")
  (.off)
  (.on "input"
    (fn [ev]
      (let [v (.val (jQ (? ev.target)))
            d0 (aget js/window.kazari_data js/current_kazari)
            d (or d0 #js {})
            ]
        (println "b=" v)
        (aset d "b_p" v)
        (aset js/window.kazari_data js/current_kazari d)
        ;(.log js/console d)
        (kick-reload)
        )
      nil)))
(-> (jQ "#slot-draw-order")
  (.off)
  (.on "input"
    (fn [ev]
      (let [v (.val (jQ (? ev.target)))
            d0 (aget js/window.kazari_data js/current_kazari)
            d (or d0 #js {})
            ]
        (println "s=" v)
        (aset d "s_o" v)
        (aset js/window.kazari_data js/current_kazari d)
        ;(.log js/console d)
        (kick-reload)
        )
      nil)))

(-> (jQ "#remove-kazari-btn")
  (.off)
  (.on CLICK
    (fn [ev]
      (if-let [k js/window.current_kazari]
        (do
          (js-delete js/window.kazari_data k)
          (js-delete js/window.kazari_txtr_obj k)
          (let [idx (.indexOf js/window.kazari_txtr_names k)]
            (when (>= idx 0)
              (.splice js/window.kazari_txtr_names idx 1)
              ))
          (set! js/window.current_kazari nil)
          (regen-kazari-list js/window.kazari_txtr_names)
          (kick-reload))
        ))
    nil))

(if false ; js/window.kazari_data
  (do
    (if-not (aget js/window.kazari_data "Commander_1620_0")
      (aset js/window.kazari_data "Commander_1620_0"
        #js {
             :s_x  -2
             :s_y  (+ 26 1 -3 -2)
             ;:s_y  (+ -5 -2 0)
             :s_r  10
             :b_r  (* 1 (+ -45 -20 -10 -10 -4 -4))
             ;:b_p "hairT"
             :b_p  "hair"
             ;:b_p "root"
             ;:b_x -10
             ;:b_y 30
             :s_o  "face"
             ;:s_o ""
             ;:b_p  "root"
             :s_sc 1.2
             ;:s_sx 1.15
             ;:s_sy 1.8

             }
        ))
    (if-not (aget js/window.kazari_data "Commander_1620_4")
      (aset js/window.kazari_data "Commander_1620_4"
        ;b_p: "face"
        ;b_r: 5
        ;b_x: 0
        ;s_o: "lex3"
        ;s_r: 0
        ;s_sc: 1.4
        ;s_x: -4
        ;s_y: 38
        ;x: 0
        #js {
             :s_x  0;-4 ;0
             :s_y  0;38;(+ 30 -2)
             ;:s_y  (+ -5 -2 0)
             :s_r  5
             :b_r  0 ;(+ -80 80)
             ;:b_p "hairT"
             ;:b_p  "hip"
             :b_p "anchor_debug"
             :b_x  3; 12
             :b_y  3 ;:b_y 30
             :s_o  "ZZZZlex3" ; hairS
             ;:s_o ""
             :s_sc 1.4
             :b_sc 1
             ;:s_sc 2.7
             ;:s_sx 1.15
             ;:s_sy 1.8
             }
        ))

    (if-not (aget js/window.kazari_data "Commander_1720_N")
      (aset js/window.kazari_data "Commander_1720_N"
        #js {
             :s_x  0
             :s_y  (+ 30 -2)
             :s_r  0
             :b_r  -80
             ;:b_p "hairT"
             :b_p  "face"
             ;:b_p "root"
             :b_x  12
             ;:b_y 30
             :s_o  "hairS"
             ;:s_o ""
             ;:b_p  "root"
             :s_sc 1.3
             ;:s_sx 1.15
             ;:s_sy 1.8

             }
        ))
    (if-not (aget js/window.kazari_data "Commander_1640_2")
      (aset js/window.kazari_data "Commander_1640_2"
        #js {
             :s_x  (+ -2 -8 -3)
             :s_y  5
             :b_r  (+ 80 10 3 4 6)
             :s_r  0
             ;:b_p "hairT"
             ;:b_p  "hip"
             :b_p  "skrit"                                  ;"hip"
             :s_o  "hairBL"
             :s_sc 1.3

             }
        ))
    (if-not (aget js/window.kazari_data "Commander_1640_0")
      (aset js/window.kazari_data "Commander_1640_0"
        #js {
             :s_x  (+ -2 -8 -3)
             :s_y  5
             :b_r  100
             :s_r  0
             ;:b_p "hairT"
             ;:b_p  "hip"
             ;:b_p  "skirt" ;"hip"
             :b_p  "skrit"                                  ;"hip"
             :s_o  "hairBL"
             :s_sc 1.3

             }
        ))
    )
  )

;(features/load-kazari)

(declare add-ev)

(defn after-load-cb [doll]
  ;(set! js/window.after_load_cb nil)

  ;(repack js/sd_atlas)

  (when js/window.sp0
    (.removeAllListeners js/sp0)
    (! js/sp0.interactive false)
    ;(! js/sp0.filters nil)
    )
  (if-let [k js/current_kazari]
    (do
      (set! js/s0 (js/_fs k))
      (set! js/sp0 (aget js/s0 "currentSprite"))
      (set! js/b0 (js/_fb k))
      (set! js/hip (js/_fb "hip"))
      (set! js/f0 (js/_fb "face"))
      (set! js/r0 (js/_fb "root"))
      (set! js/a0 (js/_fb "anchor_debug"))
      ;(! js/sp0.filters #js [fltrSelected])
      (add-ev js/sp0)
      ;(.log js/console "kazari interactiion SET!")

      ;(set! js/window.edit_mode "r")
      (regen-lists (js/_d) js/b0)
      ;(js/setTimeout #(regen-lists (js/_d)) 10)


      (let [sc (? _h.doll.scale.x)
            r-x (? _h.doll.x)
            r-y (? _h.doll.y)
            wx (* sc js/b0.parent.worldX)
            wy (* sc js/b0.parent.worldY)
            ]
        (draw-cross2
          (+ r-x wx) (+ r-y wy)
          ))


      )
    (do  ; current_kazari is null
      ;(when js/window.sp0
      ;  (.removeAllListeners js/sp0)
      ;  (! js/sp0.interactive false))
      (aset (js/_d) "interactive" true)

      (set! js/window.s0 nil)
      (set! js/window.sp0 nil)
      (set! js/window.b0 nil)
      ;(set! js/hip (js/_fb "hip"))
      ;(set! js/f0 (js/_fb "face"))
      ;(set! js/r0 (js/_fb "root"))
      ;(set! js/a0 (js/_fb "anchor_debug"))
      ;(add-ev js/sp0)

      ;(.log js/console "kazari interactiion SET!")
      ;(set! js/window.edit_mode "r")
      (regen-lists (js/_d) nil)

      (let [sc (? _h.doll.scale.x)
            r-x (? _h.doll.x)
            r-y (? _h.doll.y)
            ;wx (* sc js/b0.parent.worldX)
            ;wy (* sc js/b0.parent.worldY)
            ]
        (draw-cross (+ r-x 0) (+ r-y 0)))
      ;(js/setTimeout #(regen-lists (js/_d)) 10)
      )))

; FIXME
(set! js/window.after_load_cb after-load-cb)
;(set! js/window.after_load_cb nil)


;(.setTimeout js/window
(defn debug2 []

  ;(let [
  ;       ldr (js/PIXI.loaders.Loader.)
  ;       name "Commander_1620_4"
  ;       url "./img/test/Commander_1620_4.png"
  ;       ]
  ;   ;(set! js/current_kazari name)
  ;   (.add ldr name url #js {:loadType "png" :xhrType "blob" :metadata #js {:type "png"}})
  ;   (.load ldr
  ;     (fn [ldr rsc]
  ;       ;(set! js/current_kazari name)
  ;       (onload-kazari-files ldr rsc [name])))
  ;
  ;   ;(aset (js/_d) "interactive" false)
  ;
  ;   ; chibi_deco.core.add_ev( sp0 = _fs(current_kazari).currentSprite);
  ;
  ;   nil)

  (let [kr (jQ "#kazari_r")]
    (-> kr
    (.off)
    (.on "change"
      (fn [ev]
        (let [v (round (.val kr) 2)
              ]
          ;(.log js/console js/b0.data.name "r=" v)
          (! js/b0.data.rotation v)
          (. (js/_sk) setToSetupPose)
          (. (js/_d) update 0)
          (when-let [k (aget js/kazari_data js/current_kazari)]
            ;(! k.b_x js/b0.data.x)
            ;(! k.b_y js/b0.data.y)
            (! k.b_r v)
            (kick-reload)
            )
          )
        ))))

  (let [input (jQ "#kazari_sc")]
    (-> input
      (.off)
      (.on "change"
        (fn [ev]
          (let [v (round (.val input) 2)
                ]
            ;(.log js/console js/b0.data.name "sc=" v)
            (! js/b0.data.scaleX v)
            (! js/b0.data.scaleY v)
            (. (js/_sk) setToSetupPose)
            (. (js/_d) update 0)
            (when-let [k (aget js/kazari_data js/current_kazari)]
              ;(! k.b_x js/b0.data.x)
              ;(! k.b_y js/b0.data.y)
              (! k.b_sc v)
              (kick-reload)
              )
            )
          ))))

  (let [input (jQ "#repackBtn")]
    (-> input
      (.off)
      (.on CLICK
        (fn [ev]
          (let [v 1]
            (repack js/sd_atlas))
          ))))

  (let [input (jQ "#saveTxtrAtlasBtn")]
    (-> input
      (.off)
      (.on CLICK
        (fn [ev]
          (let [skin (? _h.doll.skin)]
            (features/saveas-string js/new_atlas
              (str skin "_deco.atlas"))
            )))))
  (let [input (jQ "#saveTxtrPngkBtn")]
    (-> input
      (.off)
      (.on CLICK
        (fn [ev]
          (let [skin (? _h.doll.skin)
                canvas (.get (jQ "#cvs_shot2") 0)
                name (str skin "_deco.png")
                ]
            (.toBlob canvas
              (fn [blob] (js/saveAs blob name)))
            )))))

  )

(.setTimeout js/window #(debug2) 500)

(defn add-ev [sp]
  (.removeAllListeners sp)
  (aset (js/_d) "interactive" false)
  (! sp.interactive true)
  (let [
        onDragStart (fn [ev]
                      (aset _h "_dragging" true)
                      ;(.log js/console ev)
                      ;(.log js/console "old xy=" js/b0.data.x js/b0.data.y js/b0.parent)
                      ;(let [p (!> ev.data.getLocalPosition (? _h.doll))
                      (let [p (!> ev.data.getLocalPosition cntr)
                            x (? _h.doll.x)
                            y (? _h.doll.y)
                            px (.-x p)
                            py (.-y p)

                            ]
                        ; px py 커서 찍은 위치 xy doll위치
                        (.log js/console px py x y)
                        (aset _h "_dx" (js/parseInt (- x px)))
                        (aset _h "_dy" (js/parseInt (- y py)))
                        (aset _h "_bx" px)
                        (aset _h "_by" py)
                        ;(aset _h "_ox" (? js/b0.data.x))
                        ;(aset _h "_oy" (? js/b0.data.y))
                        (! js/b0.data._ox js/b0.data.x) ; b0 - 선택된 bone
                        (! js/b0.data._oy js/b0.data.y)
                        )
                      )
        onDragMove (fn [ev]
                     (when (aget _h "_dragging")
                       ;(let [p (!> ev.data.getLocalPosition (? _h.doll))
                       (let [p (!> ev.data.getLocalPosition cntr)
                             x (js/parseInt (.-x p))
                             y (js/parseInt (.-y p))
                             sc (? _h.doll.scale.x)
                             ;nx (js/parseInt (+ x (? _h._bx)))
                             ;ny (js/parseInt (+ y (? _h._by)))
                             dx0 (js/parseInt (- x (? _h._bx)))
                             dy0 (js/parseInt (- y (? _h._by)))
                             dx (/ dx0 sc)
                             dy (/ dy0 sc)
                             ;dy (- dy-)
                             ;xy #js [dx, (- dy)]
                             ;mx (+ js/b0.data._ox dx)
                             pb js/b0.parent
                             xy #js [dx, dy]
                             xy2 #js [
                                      (+ dx (? pb.worldX))
                                      (+ dy (? pb.worldY))
                                      ]
                             xy2-sp3 #js {
                                      :x (+ dx (? pb.worldX))
                                      :y (+ dy (? pb.worldY))
                                      }
                             ]
                         ;(draw-cross nx ny)
                         ;(. pb localToWorld xy)
                         (. pb worldToLocal xy2-sp3)
                         ;(.log js/console "xy=" dx dy xy2 xy2-sp3)
                         ;(.log js/console "xy=" (? js/b0.data.x) (? js/b0.data.y))

                         (let [
                               ;{:strs [x y]} xy2-sp3
                               x (aget xy2-sp3 "x")
                               y (aget xy2-sp3 "y")
                               ]
                           (.log js/console "dx dy x y=" dx dy x y)
                           (! js/b0.data.x (+ x js/b0.data._ox))
                           (! js/b0.data.y (+ y js/b0.data._oy)))
                         ;(! js/b0.data.x (- (? xy.0) js/b0.data._ox))
                         ;(! js/b0.data.y (- (? xy.1) js/b0.data._oy))
                         (. (js/_sk) setToSetupPose)
                         (. (js/_d) update 0)

                         (! js/b0.data._dx dx)
                         (! js/b0.data._dy dy)

                         (let [
                               r-x (? _h.doll.x)
                               r-y (? _h.doll.y)
                               wx (* sc js/b0.parent.worldX)
                               wy (* sc js/b0.parent.worldY)
                               ]

                           ;(.log js/console sc dx dy wx wy)

                           (draw-cross2
                             (+ r-x wx)
                             (+ r-y wy)
                             )

                           nil)

                         ;(.log js/console dx dy x y nx ny)
                         ;(.log js/console dx dy)
                         ;(!> _h.doll.position.set nx ny)
                         ;(.val pos-x nx)
                         ;(.val pos-y ny)
                         ;(!> sp.position.set dx dy)
                         )))
        onDragEnd (fn [ev]
                    (aset _h "_dragging" false)
                    ;(.log js/console "new xy=" (? js/b0.data.x) (? js/b0.data.y))
                    (when-let [k (aget js/kazari_data js/current_kazari)]
                      (! k.b_x js/b0.data.x)
                      (! k.b_y js/b0.data.y)

                      (-> (jQ "#kazari_x") (.val js/b0.data.x))
                      (-> (jQ "#kazari_y") (.val js/b0.data.y))
                      (-> (jQ "#kazari_r") (.val js/b0.data.rotation))

                      (kick-reload) ; 리로드

                      )

                    nil)
        ]
    (.on sp "pointerdown" onDragStart)
    (.on sp "pointerup" onDragEnd)
    (.on sp "pointerupoutside" onDragEnd)
    (.on sp "pointermove" onDragMove)
  nil))


(defn on-js-reload []
  (println "js-reloaded")
  )
