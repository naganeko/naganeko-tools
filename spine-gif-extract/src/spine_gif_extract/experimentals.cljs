(ns spine-gif-extract.experimentals
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
    [renden.spine :as sp]
    [renden.spine-debug :as sp-debug]
    )
  )
(def jQ js/jQuery)

;loadPSD("test4/elfeldt_setup_pose_tree.psd", function ( d ) {  mypsd = d; } );
(defn init-experimentals [cntr]

  (spine-gif-extract.core/load-spine-data-url
    "./test2022_12/skeleton.atlas"
    ;;"./test2022_12/skeleton_s21.json"
    "./test2022_12/skeleton.skel"
    "./test2022_12/skeleton.png"
    "star1" false
    )


  ;;(spine-gif-extract.core/load-spine-data-url
  ;;  "./test2022_3/c1067.atlas"
  ;;  "./test2022_3/c1067.json"
  ;;  "./test2022_3/c1067.png"
  ;;  "c1067" true
  ;;  )
  ;(js/loadPSD "test4/elfeldt_setup_pose.psd"
  ;  (fn [d]
  ;    (set! js/window.mypsd d)
  ;    (-> (jQ "#gifhere")
  ;      (.empty)
  ;      (.append (? d.canvas))
  ;      (.append "<br/>"))
  ;    (.log js/console (? d.children))
  ;    (mapv
  ;      (fn [lyr]
  ;        (.log js/console "name" (? lyr.name))
  ;        (-> (jQ "#gifhere")
  ;          (.append (? lyr.canvas))
  ;          (.append (? lyr.name))
  ;          ))
  ;      (? d.children))
  ;    ))
  ;(js/loadPSD "test4/char_002_amiya_Default_20201102-234407.psd"
  ;  (fn [d]
  ;    (set! js/window.mypsd d)))

  ; 테스트용
  ;(js/loadPSD "test4/test3.psd"
  ;  (fn [d]
  ;    (set! js/window.mypsd d)))

  ;(set! js/after_doll_loaded
  ;  (fn [d]
  ;    (set! js/fltr2 (new js/PIXI.filters.AdjustmentFilter))
  ;    (aset js/fltr2 "saturation" 0)
  ;    (aset d "filters" #js [js/fltr2])
  ;    nil))

  ;(spine-gif-extract.core/load-spine-data-url
  ;  "./test4/dorothy.atlas",
  ;  "./test4/dorothy.skel",
  ;  "./test4/dorothy.png",
  ;  "dorothy"
  ;  false)


  ;(spine-gif-extract.core/load-spine-data-url
  ;  "./meidoka/m200_maid_4x.atlas"
  ;  "./meidoka/rm200_maid.json"
  ;  "./meidoka/m200_maid_4x.png"
  ;  "rmaid_m200"
  ;  true)

  ; atlas skel png skin
  ;(when false
  ;  (spine-gif-extract.core/load-spine-data-url
  ;    "./test2021_2/JoanofArc.sprite.idle.atlas"
  ;    "./test2021_2/JoanofArc.sprite.idle.skel"
  ;    ;"./test2021_2/JoanofArc_s21.json"
  ;    "./test2021_2/JoanofArc.sprite.idle.png"
  ;    "JoanofArc"
  ;    false)
  ;  (set! js/after_doll_loaded
  ;    (fn [d]
  ;      ;(aset d "drawDebug" true)
  ;      ;(sp-debug/init d)
  ;      nil))
  ;  )

  ; 테스트용 sd
  ;(when true
  ;  (spine-gif-extract.core/load-spine-data-url
  ;    "./test4/sv98mod/sv98mod.atlas"
  ;    "./test4/sv98mod/sv98mod.skel"
  ;    "./test4/sv98mod/sv98mod.png"
  ;    "sv98mod")
  ;  (set! js/after_doll_loaded
  ;    (fn [d]
  ;      ;(aset d "drawDebug" true)
  ;      ;(sp-debug/init d)
  ;      nil))
  ;  )

  ;(set! js/window.writePsd js/agPsd.writePsd)
  ;(set! js/window.readPsd js/agPsd.readPsd)

  nil)
;
;(defn init-experimentals-old [cntr]
;
;  ;(println core)
;
;  ;(println core/bg-color)
;
;  ;  ;var phone = new PIXI.Sprite.fromImage("img/phone.png");
;  (let [eyes (js/PIXI.Sprite.fromImage. "img/commander_9920_n.png")
;        ;len (? doll.state.data.skeletonData.slots.length)
;        ;children (? doll.children)
;        ;gun cntr
;        ]
;    (.addChild cntr eyes)
;    ;(! gun.children.0.visible false)
;    ;(! shadow.children.0.visible false)
;    (!> eyes.scale.set 0.4)
;    (!> eyes.anchor.set 0.5)
;    ;(!> eyes.pivot.set 0.6 0.7)
;    ;(aset eyes "rotation" (* (/ Math/PI 180) -160))        ; -130
;    (set! js/window.crying_eyes eyes)
;    (aset eyes "parentGroup" js/top_grp)
;    )
;
;    ;(spine-gif-extract.core/load-spine-data-url
;    ;  "./test2/gsh18mod_4x.atlas"
;    ;  "./test2/rgsh18mod.skel"
;    ;  "./test2/gsh18mod_4x.png"
;    ;  "gsh18mod" false
;    ;  )
;    ;(spine-gif-extract.core/load-spine-data-url
;    ;  "./test3/dp12.atlas.txt"
;    ;  "./test3/dp12_s21.json"
;    ;  ;"./test3/dp12.skel.bytes"
;    ;  "./test3/dp12.png"
;    ;  "dp12"
;    ;  true)
;    (spine-gif-extract.core/load-spine-data-url
;      "./test3/4-fn57.atlas"
;      "./test3/4-fn57.skel"
;      ;"./test3/dp12.skel.bytes"
;      "./test3/4-fn57.png"
;      "4-fn57"
;      false)
;
;  nil)
;


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
