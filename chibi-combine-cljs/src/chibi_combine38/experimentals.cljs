(ns chibi-combine38.experimentals
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
    [renden.chibi :as chibi]
    )
  )



(def jQ js/jQuery)

;loadPSD("test4/elfeldt_setup_pose_tree.psd", function ( d ) {  mypsd = d; } );
(defn init-experimentals [cntr]
  (println "init-experimentals!!!")
; ; (defn load-spine-data-url3 [atlas-url png-url skel-url skin is-json]
  (chibi-combine38.core/load-spine-data-url3
    "./test2022_02/ro635mod.atlas"
    "./test2022_02/ro635mod.png"
    "./test2022_02/ro635mod_s38.json"
    "ro635mod"
    true)
  (chibi-combine38.core/load-spine-data-url3
    "./test2022_02/rrico.atlas"
    "./test2022_02/rrico.png"
    "./test2022_02/rrico_s38.json"
    "rrico"
    true)
  ;(chibi-combine38.core/load-spine-data-url3
  ;  "./test5/spine42.atlas"
  ;  "./test5/spine42.png"
  ;  "./test5/spine42.json"
  ;  "spine42"
  ;  true)

    ;(chibi-combine38.core/load-spine-data-url3
    ;  "./test5/char_002_amiya.atlas"
    ;  "./test5/char_002_amiya.png"
    ;  "./test5/char_002_amiya.skel"
    ;  "char_002_amiya"
    ;  false)
    ;(chibi-combine38.core/load-spine-data-url3
    ;  "./test6/char_113_cqbw_epoque_7.atlas"
    ;  "./test6/char_113_cqbw_epoque_7.png"
    ;  "./test6/char_113_cqbw_epoque_7.skel"
    ;  "char_113_cqbw_epoque_7"
    ;  false)

  nil)


(set! js/after_doll_loaded
  (fn [d]
    (let [skin (aget d "skin")
          x (aget d "x")
          y (aget d "y")
          ]
    ;;(.log js/console (aget d "skin"))
    (.log js/console "skin=" skin "xy=" x y)
    (cond
      (= skin "rrico") (chibi/set-pos! d (+ x 60) y)
      (= skin "ro635mod") (chibi/set-pos! d (- x 60) y)
      )

      )
    ;;(aset d "alpha" 0.2)
    nil))
