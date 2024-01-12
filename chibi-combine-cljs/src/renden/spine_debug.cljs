(ns renden.spine-debug
  (:require
    [renden.utils :as u]
    [clojure.string :as str]
    )
  (:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
))

(def psd-k "_chibi_psd_debug")

(defn init [doll]
  (let [children (.-children doll)
        last-child (aget children (dec (count children)))
        dg (if (aget last-child psd-k)
             (do
               (.removeChildren last-child)
               last-child)
             (let [c (js/PIXI.Container.)]
               (aset c psd-k 42)
               (.addChild doll c)
               c))
        _ts #js {}
        ghs #js {}
        sp doll
        sk (.-skeleton doll)
        ;dg (new js/PIXI.Container)
        lst #js ["bones"]
        cnt (js/PIXI.Container.)
        ]
    (aset _ts "ghs" ghs)
    (aset _ts "lst" lst)
    (aset ghs "bones" cnt)
    (.addChild dg cnt)

    (! doll._chibi_psd _ts)

    ))

(defn draw-bones [doll]
  (init doll)
  (let [bones-cntr (? doll._chibi_psd.ghs.bones)
        sk-x (? doll.skeleton.x)
        sk-y (? doll.skeleton.y)
        color 0xFF3333
        c-color 0x000000
        c-alpha 1
        bones (filter
                (fn [b] (and (not= "root" (? b.data.name))
                          (.-parent b)))
                (? doll.skeleton.bones))
        scale (or (? doll.scale.x) (? doll.scale.y) 1)
        lw (/ 1 (Math/abs scale))

        ref-ration (/ 1.5 scale)
        rad (/ Math/PI 180)
        ]

    (set! js/window.chibi_psd_bones_cntr bones-cntr)

    ;(.log js/console "ref-ration" ref-ration)
    ;(.log js/console "bones c=" (count bones))

    (mapv
      #(.destroy % #js {:children true :texture true :baseTexture true})
      (.-children bones-cntr))
    (.removeChildren bones-cntr)

    (mapv
      (fn [idx bone]
        (let [blen (? bone.data.length)
              bwx (? bone.worldX)
              bwy (? bone.worldY)
              sx (+ sk-x bwx)
              sy (+ sk-y bwy)
              ex (+ sk-x (* blen (.-m00 bone)) bwx)
              ey (+ sk-y (* blen (.-m10 bone)) bwy)
              w (Math/abs (- sx ex))
              h (Math/abs (- sy ey))
              a2 (Math/pow w 2)
              b h
              b2 (Math/pow h 2)
              c (Math/sqrt (+ a2 b2))
              c2 (Math/pow c 2)
              bX (Math/acos (/ (+ c2 b2 (- a2)) (* 2 b c)))
              bb (if (js/isNaN bX) 0 bX)
              ;bb (/ (.-rotation bone) rad)
              ]

          ;(.log js/console
          ;  idx
          ;  ;"b=" bone
          ;  ;(? bone.data)
          ;  (? bone.data.name)
          ;  blen w h sx sy ex ey)

          (if (= 0 c)
            nil
            (let [gp (js/PIXI.Graphics.)]
              (.addChild bones-cntr gp)

              (! gp._bone bone) ; debug
              (! gp._bone_name (? bone.data.name))

              (.beginFill gp color c-alpha)
              (.drawPolygon gp
                0 0
                (- 0 (* ref-ration 2)) (- c (* ref-ration 6))
                0 (- c ref-ration)
                (+ 0 (* ref-ration 2)) (- c (* ref-ration 6))
                )
              (.endFill gp)
              (! gp.x sx)
              (! gp.y sy)
              (! gp.pivot.y c)

              ; TODO rotation
              (->>
                (cond
                  (and (< sx ex) (< sy ey)) (+ (- bb) (* 180 rad))
                  (and (> sx ex) (< sy ey)) (+ (+ bb) (* 180 rad))
                  (and (> sx ex) (> sy ey)) (- bb)
                  (and (< sx ex) (> sy ey)) bb
                  (and (< sx ex) (= sy ey)) (* 90 rad)
                  (and (> sx ex) (= sy ey)) (* -90 rad)
                  (and (= sx ex) (< sy ey)) (* 180 rad)
                  (and (= sx ex) (> sy ey)) 0
                  :else 0)
                (aset gp "rotation"))
              ;(! gp.rotation (* (+ bone.worldRotation 90) rad))
              ;(.log js/console (? bone.data.name) (? gp.rotation))

              (.lineStyle gp (+ lw (/ ref-ration 2.4)) color c-alpha)
              (.beginFill gp c-color 0.6)
              (.drawCircle gp 0 c (* ref-ration 1.5))
              (.endFill gp)
              ))
          ))
      (range) bones)

    bones-cntr))
