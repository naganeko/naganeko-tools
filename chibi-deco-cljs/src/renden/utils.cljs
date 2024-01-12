(ns renden.utils
(:use-macros [purnam.core :only [? ?> ! !> f.n def.n do.n obj arr def* do*n def*n f*n]]
    ))

(when-not (.getElementById js/document "png_wrk_canvas")
  (let [cvs (.createElement js/document "canvas")
        body (aget (.getElementsByTagName js/document "body") 0)]
    (.setAttribute cvs "id" "png_wrk_canvas")
    (.setAttribute cvs "style" "display:none")
    (.append body cvs)
    (set! js/window.png_wrk_canvas cvs)
    (set! js/window.png_wrk_ctx (.getContext cvs "2d"))
    ))

(defn fix-ctx [ctx w h]
  (let [
        ;w (.-width canvas)
        ;h (.-height canvas)
        img-data (.getImageData ctx 0 0 w h)
        data (.-data img-data)]
    (doseq [ir (range 0 (.-length data) 4)]
      (let [r (aget data ir)
            ig (+ 1 ir)
            ib (+ 2 ir)
            ia (+ 3 ir)
            g (aget data ig)
            b (aget data ib)
            a (aget data ia)
            op /
            ]
        (when (< a 255)
          (aset data ir (min 255 (op r (/ a 255.0))))
          (aset data ig (min 255 (op g (/ a 255.0))))
          (aset data ib (min 255 (op b (/ a 255.0))))
          )))
    (.putImageData ctx img-data 0 0)
    ;(.log js/console data "ctx w=" w "h=" h)
    ))

(defn unpma [png-data]
  (if js/window.skip_unpma
    png-data
    (let [w (.-width png-data)
          h (.-height png-data)
          ctx js/window.png_wrk_ctx
          canvas (.-canvas ctx)
          img (js/Image. w h)
          ]
      (! canvas.width w)
      (! canvas.height h)
      (.drawImage ctx png-data 0 0)
      (fix-ctx ctx w h)
      (aset img "src" (.toDataURL canvas))
      ;(.-canvas ctx)
      img)))

(defn round [n p]
  (let [d (Math/pow 10 p)]
    (/ (Math/round (* n d)) d)))

(defn parseIntOrNil [n]
  (let [x (js/parseInt n)]
    (if (js/isNaN x) nil x)))


(defn fb [b]
  (js/_fb b))


(defn round4 [n]
  (/ (Math/round (* n 10000)) 10000))

(defn approx1= [n]
  (= 1.0000 (round4 n)))
(defn approx-zero? [n]
  (= 0.0000 (round4 n)))
(defn approx= [a b]
  (= (round4 a) (round4 b)))

; 2.1 xxxxxx
(defn skewed? [b]
  (if-let [pbd (-> b .-parent .-data)]                      ;
    (let [sx (.-scaleX pbd)
          sy (.-scaleY pbd)
          t (aget b "data" "transformMode")]
      ;(println sx sy t)
      (when (= t 2)                                         ; NoRotationOrReflection
        (println (aget b "data" "name")
          (aget js/PIXI.spine.core.TransformMode t)))
      (and
        (or (not (approx1= sx)) (not (approx1= sy)))
        (or (= t 0) (= t 2))
        ))
    (do
      (println "root")
      false)))
;
; setb( aaa.w2lr( aaa.getWorldRotationX() + bbb.origionRotaton))  ; aaa와의 상대각도
; setb( aaa.w2lr( aaa.data.rotation + bbb.origionRotaton))  ; aaa와의 상대각도
; bbb.data.rotation = aaa.w2lr ( WorldDeg ) ; bbb를 world좌표계 기준으로 세팅할 때
; parent 의 공식을 이용하는게 비결.

; pbone scaleX != 1 or scaleY != 1
(defn deskew [bone]
  (let [
        pb (-> bone .-parent)
        pd (-> pb .-data)
        pbd-r (.-rotation pd)
        pbd-sx (.-scaleX pd)
        pbd-sy (.-scaleY pd)
        data (.-data bone)
        r (.-rotation data)
        sx (.-scaleX data)
        sy (.-scaleY data)
        ]
    (aset data "rotation_old" r)
    (aset data "rotation" (.worldToLocalRotation pb (+ pbd-r r)))
    (aset data "transformMode" 3)                           ;; NoScale
    (when (not (approx1= pbd-sx))
      (aset data "scaleX_old" sx)
      (aset data "scaleX" (* pbd-sx sx)))
    (when (not (approx1= pbd-sy))
      (aset data "scaleY_old" sy)
      (aset data "scaleY" (* pbd-sy sy)))
    )
  nil)


(defn deskew-bone [b]
  (if (skewed? b) (deskew b)
                  (println "not skewed")))

; 부모 타임란인에 스케일이 있으면
; transform 을 noScale로 변경
;
; key프레임마다 track time을 이동하면서 {
;  ScaleTimeline 의 스케일을 보정, newScale = parent.scaleX * 현재 scale설정
;  통상 1로 바뀜
; - 만약 보모와 키프레임이 다르다면?
; - 그냥 1로 다 바꾸자
;  RotationTimeline :
;  keyframe.rotation =
;  parentBone.worldToLocalRotation ( parentBone.rotation + keyframe.rotation )
;  parentBone.rotation 계산된 부모bone의 현재 각도
;  bone의 rotation 은 상관 없음
; }
;


;
;
;
;(def spine_rt js/PIXI.spine.SpineRuntime)
(def spine_rt (or js/PIXI.spine.SpineRuntime js/PIXI.spine.core))

;(aset spine_rt "ColorTimeline" "ENTRIES" 5)
;(aset spine_rt "DrawOrderTimeline" "ENTRIES" 1)
;(aset spine_rt "EventTimeline" "ENTRIES" 1)
;(aset spine_rt "FfdTimeline" "ENTRIES" 1)
;(aset spine_rt "FlipXTimeline" "ENTRIES" 2)
;(aset spine_rt "FlipYTimeline" "ENTRIES" 2)
;(aset spine_rt "IkConstraintTimeline" "ENTRIES" 3)
;(aset spine_rt "TranslateTimeline" "ENTRIES" 3)
;(aset spine_rt "RotateTimeline" "ENTRIES" 2)
;(aset spine_rt "ScaleTimeline" "ENTRIES" 3)


  ; js typed array을 일반 배열로 바꿈.
(defn tyar->ar [typed-array]
  (.reduce typed-array
    (fn [a b] (.push a b) a) (array)))

(defn scale-timelines [timelines]
  (->> timelines
    (filter (fn [tl] (instance? (.-ScaleTimeline spine_rt) tl)))
    ))
(defn rotation-timelines [timelines]
  (->> timelines
    (filter (fn [tl] (instance? (.-RotateTimeline spine_rt) tl)))
    ))

(defonce entries-map {
                      "ColorTimeline"        5
                      "DrawOrderTimeline"    1
                      "EventTimeline"        1
                      "FfdTimeline"          1
                      "FlipXTimeline"        2
                      "FlipYTimeline"        2
                      "IkConstraintTimeline" 3
                      "TranslateTimeline"    3
                      "RotateTimeline"       2
                      "ScaleTimeline"        3})
(defonce clzz (keys entries-map))

(defn tl-entries [tl]
  (if-let [clz (first
                 (filter #(instance? (aget spine_rt %) tl) clzz))]
    (do (get entries-map clz))
    nil))

; 타임라인의 프레임을 키프레임 크기단위로 나눈다
(defn get-pretty-frames [timeline]
  (if timeline
    (let [step
          (tl-entries timeline)]
      (->> timeline
        .-frames
        tyar->ar
        (partition step)
        ))))

(defn filter-by-bonedata [bone-data timelines]
  (if bone-data
    (let [idx (js/_fbi (.-name bone-data))]
      (filter #(= idx (.-boneIndex %)) timelines))
    []))

(defn scale-changes? [timeline]
  (if (instance? (.-ScaleTimeline spine_rt) timeline)
    (let [frames (get-pretty-frames timeline)
          ]
      (some (fn [[t sx sy]] (not (and (approx1= sx) (approx1= sy))))
        frames))
    (do (println "not a ScaleTimeline")
        false))
  )

; 애니메이션에 비틀림이 발생할 가능성
; scale keyframe 타임=0 의 sx혹은sy가 1이 아님
; bone 의 자식 bone들은 찾음
; 자식 본의 스케일 타임라인이 있고
; 동일 키프레임time의 스케일이 1이 아님

; 선두부터 스케일이 변한 타임라인인가??
;(defn scale-tl-weird? [tl]
;  (let [frames (.-frames tl)]
;    ;(println frames (>= (.-length frames) 2) (aget frames 1))
;    (and (>= (.-length frames) 2)
;      (approx-zero? (aget frames 0))
;      (not (approx1= (aget frames 1)))
;      )))

;(defn parent-bone-tls [bone-data tls]
;  (if-let [pd (and bone-data (.-parent bone-data))]
;    (filter-by-bonedata pd tls)))

;(defn bones-skewed-kamo [ani]
;  (let [sc-tls (->> ani .-timelines scale-timelines)
;        tls2 (filter scale-tl-weird? sc-tls)
;        bones (aget (js/_sk) "bones")
;        ]
;    ;(println "tls2" tls2)
;    ;(mapv #(println (.-boneIndex %)) tls2)
;    ;(set! js/window.tls2 tls2)
;    (mapv
;        #(let [
;               ;frames (.-frames %)
;               bidx (.-boneIndex %)
;               b (aget bones bidx)
;               bd (.-data b)
;               pd (.-parent bd)
;               ;pname (if pd (.-name pd))
;               ptl (first (filter-by-bonedata pd tls2))
;               ]
;           ;(println bidx (.-name bd) pname ptl)
;           (when ptl
;             {:a (.-name ani) :b (.-name bd) :bidx bidx
;              :frames (get-pretty-frames %)}
;             ))
;      tls2)
;    ))

;(defn all-kf-times [ani]
;  (->> ani
;    (.-timelines)
;    ;(#(filter-by-bonedata (.-data bone) %))
;    (mapcat #(map first (get-pretty-frames %)))
;    set
;    sort))

; animation을 선택
; bones 의 root의 자식부터 한 bone씩 검사
;
;
;
;

(defn deskew-timeline [bone ani]

  nil)

; scale타임라인의 scale을 조정한다
; 보정 대상임은 이미 판정 끝났다고 가정한다.
; (eyeL 등은 scaleX를 0.8로 바꾸는 경우가 많음. 이런 것은 보정 대상으로 하면 안됨.)
;(defn normalize-scale-timelines [bone ani time]
;  (let [pb (.-parent bone)
;        bonedata (.-data bone)
;        timelines (.-timelines ani)
;        sc-tls (->> timelines scale-timelines (filter-by-bonedata bonedata))
;        ]
;    (println "tls=" sc-tls)
;    (mapv
;      (fn [tl]
;        (let [frames (.-frames tl)]
;          (mapv
;            (fn [i]
;              (let [t (aget frames i)
;                    scx (aget frames (+ i 1))
;                    scy (aget frames (+ i 2))]
;                ;(println "t=" t "i=" i)
;                (if (and (.-old_vals tl) (aget tl "old_vals" t))
;                  (do nil)
;                  (when (approx= time t)
;                    (println "normalizing sc t=" t scx scy)
;                    (aset frames (+ i 1) 1)
;                    (aset frames (+ i 2) 1)
;                    (if-not (.-old_vals tl) (aset tl "old_vals" #js {}))
;                    (aset tl "old_vals" t #js [scx scy])
;                    ))))
;            (range 0 (.-length frames) 3))))
;      (filter #(nil? (.-old_vals %)) sc-tls))
;    nil))

;(defn normalize-rotation-timelines [bone ani time]
;  (let [pb (.-parent bone)
;        w2lr (.bind (.-worldToLocalRotation pb) pb)
;        bonedata (.-data bone)
;        bone-r (.-rotation bonedata)
;        timelines (.-timelines ani)
;        ro-tls (->> timelines rotation-timelines (filter-by-bonedata bonedata))
;        ]
;    ;(println "tls=" ro-tls)
;    (mapv
;      (fn [tl]
;        (let [frames (.-frames tl)]
;          (mapv
;            (fn [i]
;              (let [t (aget frames i)
;                    ro (aget frames (+ i 1))]
;                (println "i=" i "target time=" time "t=" t "ro=" ro)
;                (if (and (.-old_vals tl) (aget tl "old_vals" t))
;                  (do nil)
;                  (when (approx= time t)
;                    (let [target-lr (+ (.getWorldRotationX pb) ro bone-r)
;                          new-kf-r (- (w2lr target-lr) bone-r)]
;                      (println "normalizing t=" t "ro=" ro "new=" new-kf-r)
;                      (aset frames (+ i 1) new-kf-r)
;                      (if-not (.-old_vals tl) (aset tl "old_vals" #js {}))
;                      (aset tl "old_vals" t #js [ro])
;                      ))))
;              )
;            (range 0 (.-length frames) (tl-entries tl))
;            )) )
;      ;(map #((if-not (.-old_vals %) (aset % "old_vals" #js {})) %) ro-tls)
;      ro-tls)
;    nil))


;(defn normalize-timelines [bone ani time]
;  (let [a 1
;        ;pb (.-parent bone)
;        ;bonedata (.-data bone)
;        ;timelines (.-timelines ani)
;        ]
;    (normalize-scale-timelines bone ani time)
;    (normalize-rotation-timelines bone ani time)
;    nil))

; (defn fix [t] (js/seek t) (normalize-timelines (js/_fb "bbb") (js/_fa "r1") t))

;(defn normalize-bone-ani [bone-name ani-name]
;  (let [ani (js/_fa ani-name)
;        bone (js/_fb bone-name)
;        kfts (all-kf-times ani)]
;    (! bone.data.transformMode 3)
;    (doseq [t kfts]
;      (js/seek t)
;      (normalize-timelines bone ani t)) ))

;({:a "attack",
;  :b "armR2",
;  :bidx 15,
;  :frames ((0 2 1) (0.10000000149011612 2 1) (0.8333333134651184 2 1))})

; {time: 0, x: 2, y: 1, curve: "stepped"}


;(defn deskew-tl [{:keys [a b bidx frames]} ani]
;  (println "reset sc-tl" a b bidx)
;  ;json_ex.animations.attack.bones.armR1
;  (aset ani "bones" b "scale" #js [#js {:time 0 :x 1 :y 1 :curve "stepped"}]))
;
;(defn deskew-for-db [lst json-obj]
;  (let [bones (.-bones json-obj)
;        animations (.-animations json-obj)]
;  (doseq [{:keys [a b bidx frames] :as m} lst]
;    (->> bones
;      (filter (fn [bone] (= b (.-name bone))))
;      first
;      (#(aset % "inheritScale" false)))
;    (deskew-tl m (aget animations a)))
;  nil))
