(ns snakejure.levels.borders
    (:use [snakejure.core :exclude (width height)]
	  [clojure.contrib.seq-utils :only (find-first)]))

(def width 30)
(def height 30)
(def noisers-num 3)

(defn- cr-walls []
  (lazy-cat (for [x (range (inc width))] (create-wall [0 x]))
	    (for [x (range (inc width))] (create-wall [height x]))
	    (for [y (range height)] (create-wall [y 0]))
	    (for [y (range height)] (create-wall [y width]))))

(defn- random-point []
  [(inc (rand-int (dec height)))
   (inc (rand-int (dec width)))])

(defn- cr-snake []
  (create-snake (list [ (quot height 2)
		     (quot width 2) ])))

(defn- cr-noisers []
  (map create-noiser (repeatedly noisers-num random-point)))

(defn-  cr-apple 
  ([{:keys [snake noisers]}]
   (cr-apple snake noisers))
  ([snake noisers]
   (->> random-point
	(repeatedly)
	(find-first #(not (overlaps-body? snake %)))
	(create-apple))))

(defn- level []
  (let [walls (cr-walls)
	snake (cr-snake)
	noisers (cr-noisers)
	apple (cr-apple snake noisers)]
    {:snake snake
     :apple apple
     :walls walls
     :noisers noisers
     :apple-generator cr-apple}))

(defn level-description []
  {:name "Borders"
   :level-creator level})