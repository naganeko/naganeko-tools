(ns chibi-gif.experimentals
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



(def jQ js/jQuery)

;loadPSD("test4/elfeldt_setup_pose_tree.psd", function ( d ) {  mypsd = d; } );
(defn init-experimentals [cntr]
; ; (defn load-spine-data-url3 [atlas-url png-url skel-url skin is-json]
;  (chibi-gif.core/load-spine-data-url3
;    "./test5/char_002_amiya.atlas"
;    "./test5/char_002_amiya.png"
;    "./test5/char_002_amiya.skel"
;    "char_002_amiya"
;    false)
  ;(chibi-gif.core/load-spine-data-url3
  ;  "./test5/kyaru_106011.atlas"
  ;  "./test5/kyaru_106011.png"
  ;  "./test5/kyaru_106011_c2g.json"
  ;  "kyaru_106011"
  ;  true)
  ;(chibi-gif.core/load-spine-data-url3
  ;  "./test5/spine42.atlas"
  ;  "./test5/spine42.png"
  ;  "./test5/spine42.json"
  ;  "spine42"
  ;  true)

    ;(chibi-gif.core/load-spine-data-url3
    ;  "./test5/char_002_amiya.atlas"
    ;  "./test5/char_002_amiya.png"
    ;  "./test5/char_002_amiya.skel"
    ;  "char_002_amiya"
    ;  false)
    ;(chibi-gif.core/load-spine-data-url3
    ;  "./test6/char_113_cqbw_epoque_7.atlas"
    ;  "./test6/char_113_cqbw_epoque_7.png"
    ;  "./test6/char_113_cqbw_epoque_7.skel"
    ;  "char_113_cqbw_epoque_7"
    ;  false)


  nil)


(set! js/after_doll_loaded
  (fn [d]
    ;(aset d "alpha" 0.2)
    nil))
