(ns renden.skb3
(:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]))

(def SCALE 1.0)
;

;  if (nonessential) {
;     // no fps in 3.4
;     if (minorVersion >= 5) {
;       skeleton.fps = this.readFloat();
;     }
;     skeleton.images = this.readString();
;     if (skeleton.images.length == 0)
;       skeleton.images = null;
;   }
(defn check_skel [mnr-v skb skel nonessential]
  (println "nonessential" nonessential)
  (when nonessential
    (if (>= mnr-v 5)
      (aset skel "fps" (.readFloat skb)))
    (let [images (.readString skb)]
      ;(println "images" images)
      (aset skel "images"
        (if (zero? (count images))
          nil images))) ))

(defn read-bones [mnr-v skb bones nonessential]
  (let [sc (.-scale skb)]
    (doseq [i (range 0 (.-length bones))]
      (let [bnd (js/Object.)
            n (.readString skb)]
        (aset bnd "name" n)
        (aset bnd "parent" nil)
        (when (> i 0)
          (->> (. skb readInt true)
            (#(aset bnd "parent" (aget bones % "name")))))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.rotation v)))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.x (* v sc))))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.y (* v sc))))
        ;(! bnd.x (* (.readFloat skb) sc))
        ;(! bnd.y (* (.readFloat skb) sc))
        ;(! bnd.scaleX (.readFloat skb))
        ;(! bnd.scaleY (.readFloat skb))
        ;(! bnd.shearX (.readFloat skb))
        ;(! bnd.shearY (.readFloat skb))
        ;(! bnd.length (* (.readFloat skb) sc))
        (let [v (.readFloat skb)]
          (if (not= v 1) (! bnd.scaleX v)))
        (let [v (.readFloat skb)]
          (if (not= v 1) (! bnd.scaleY v)))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.shearX v)))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.shearY v)))
        (let [v (.readFloat skb)]
          (if (not= v 0) (! bnd.length (* v sc))))
        (if (<= mnr-v 4)
          (do
            (! bnd.inheritRotation (.readBoolean skb))
            (! bnd.inheritScale (.readBoolean skb)))
          (! bnd.transform
            (aget js/TransformMode (.readInt skb true))))
        (if nonessential
          (! bnd.color (.readColor skb)))
        (aset bones i bnd)
        ;(.log js/console i n bnd)
        ))))

(defn read-slots [mnr-v skb bones slots nonessential]
  (let [sc (.-scale skb)]
    (doseq [i (range 0 (.-length slots))]
      (let [slot-data (js/Object.)
            name (.readString skb)
            bone-data (aget bones (.readInt skb true))
            ;n (.readString skb)
            ]
        (aset slot-data "name" name)
        (aset slot-data "bone" (.-name bone-data))
        (let [v (.readColor skb)]
          (if (not= v "ffffffff")
            (aset slot-data "color" v)))
        (if (>= mnr-v 6)
          (let [dark-color (.readColor skb)]
            ; 원래 dark는 RGB만 사용. -1이면 미사용.
            (if (= "ffffffff" dark-color)
              nil
              (aset slot-data "dark" dark-color))
            ))
        (aset slot-data "attachment" (.readString skb))
        (aset slot-data "blend"
          (aget js/BlendMode (.readInt skb true)))
        (aset slots i slot-data)
        ;(.log js/console (? slot-data.name) "dark" (? slot-data.dark))
        ;(println "slot name" name)
        ))))
; if (nonessential) {
;   skeleton.images = this.readString();
;   if (skeleton.images.length == 0) skeleton.images = null;
; }

;    if (minorVersion >= 5) {
;     ikConstraints.order = this.readInt(true)
;   }
(defn check-ik [mnr-v skb ikConstraints]
  (if (>= mnr-v 5)
    (aset ikConstraints "order" (.readInt skb true))))

; if (minorVersion >= 5) {
;   transformConst.order = this.readInt(true);
; }
(defn check-trs [mnr-v skb transformConst]
  (if (>= mnr-v 5)
    (aset transformConst "order" (.readInt skb true))))

;      if (minorVersion >= 5) {
;        pathConst.order = this.readInt(true);
;      }
(defn check-path [mnr-v skb obj]
  (if (>= mnr-v 5)
    (aset obj "order" (.readInt skb true))))


