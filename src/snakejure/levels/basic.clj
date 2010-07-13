(ns snakejure.levels.basic
  (:use (clojure.contrib seq-utils)
	[snakejure.core :exclude (width height)]))

(def width 30)
(def height 30)
(def noisers-num 2)
(def walls-num 10)

(defn- random-point
  "Creates random point with x in [0,width), y in [0, height).
  If walls is given, then created point won't overlap them."
  ([]
     [(rand-int height) (rand-int width)])
  ([walls]
     (find-first #(not (overlaps-walls? walls %)) (repeatedly random-point))))



(defn- create-walls
  "Creates sequence of n walls in random points.
  Walls may overlap each other"
  [n]
  (map create-wall (repeatedly n random-point)))


(defn create-apple-local
  "Creates new apple in random point.
  Created apple won't overlap snake's body and walls."
  ([{:keys [snake walls]}]
   (create-apple-local snake walls))
  ([snake walls]
  (create-apple (find-first #(not (overlaps-body? snake %)) (repeatedly #(random-point walls))))))


(defn- create-noisers
  "Creates sequence with n noisers in random locations.
  Noisers may overlap each other."
  [n walls]
  (map create-noiser (repeatedly n #(random-point walls))))


(defn create-level []
  (let [walls (create-walls walls-num)
	snake (create-snake (list (random-point walls)))
	noisers (create-noisers noisers-num  walls)
	apple (create-apple-local snake walls)]
    {:walls walls
     :noisers noisers
     :snake snake
     :apple apple
     :apple-generator create-apple-local}))

(defn level-description []
  {:name "Basic"
   :level-creator create-level})