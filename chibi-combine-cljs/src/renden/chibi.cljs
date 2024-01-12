(ns renden.chibi
 ;(:require
    ;[debux.cs.core :as d :refer-macros [dbg clog break]]
   ;)
(:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
    ))

; null 이면 3
(defn sp3? [doll]
  (if-let [v (? doll.spineData.version)]
    (or (.startsWith v "3") (.startsWith v " "))
    true))

(defn track0 [doll]
  (if doll
    (? doll.state.tracks.0)))

(defn set-ani!
  ([doll action]
   (set-ani! doll action true))
  ([doll action loop]
   (let [state (.-state doll)]
     (.setAnimationByName state 0 (name action) loop)
     doll
     )))

(defn pos [doll]
  (let [p (.-position doll)]
    {:x (.-x p) :y (.-y p)}))

(defn set-pos! [doll x y]
  (let [p (.-position doll)]
    (.set p x y) doll))

(def SCALE 1.0)


(defn flipX [doll]
  (let [sp3? (sp3? doll)]
    (if sp3?
      (< (? doll.skeleton.scaleX) 0)
      (? doll.skeleton.flipX))))

(defn flipX! [doll faceLeft]
  (let [sp3? (sp3? doll)
        abs-sx (Math/abs (? doll.skeleton.scaleX))]
    (if sp3?
      (! doll.skeleton.scaleX (* abs-sx (if faceLeft -1 1)))
      (! doll.skeleton.flipX (if faceLeft true false))) ))

(defn seek [doll t]
  (if doll
    (let [sp3? (sp3? doll)
          trk (track0 doll)]
      (if trk
        (aget trk (if sp3? "trackTime" "track"))
        (do
          ;(println "no tracks")
          nil)) )))
(defn seek! [doll t]
  (let [sp3? (sp3? doll)
        trk (track0 doll)]
    (if trk
      (aset trk (if sp3? "trackTime" "track") t)
      (do
        ;(println "no tracks")
        nil))
    ))