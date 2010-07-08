(ns snakejure.levels.copy-basic
  (:use (clojure.contrib seq-utils))
  (:import (java.io File)
	   (java.awt Color)))

(def width 30)
(def height 30)
(def noisers-num 2)
(def walls-num 10)

(defn- overlaps-body?
  "Check, if snake's body overlaps some given point."
  [{body :body} point]
  (includes? body point))

(defn- overlaps-walls?
  "Check, if some given point overlaps walls."
  [walls point]
  (not (every? false? (map #(= point (:location %)) walls))))


(defn- random-point
  "Creates random point with x in [0,width), y in [0, height).
  If walls is given, then created point won't overlap them."
  ([]
     [(rand-int height) (rand-int width)])
  ([walls]
     (find-first #(not (overlaps-walls? walls %)) (repeatedly random-point))))


(defn- create-wall
  "Creates new wall in random point."
  []
  {:location (random-point)
   :type :wall
   :color (Color/BLACK)})

(defn- create-walls
  "Creates sequence of n walls in random points.
  Walls may overlap each other"
  [n]
  (take n (repeatedly create-wall)))


(defn- create-apple
  "Creates new apple in random point.
  Created apple won't overlap snake's body and walls."
  [snake walls]
  {:location (find-first #(not (overlaps-body? snake %)) (repeatedly #(random-point walls)))
   :type :apple
   :color (Color/RED)})

(defn- create-noiser
  "Creates new noiser in random location.
  It won't overlap walls."
  [walls]
  {:location (random-point walls)
   :type :noiser
   :color-noise (Color/BLUE)
   :color-silence (Color/MAGENTA)})

(defn- create-noisers
  "Creates sequence with n noisers in random locations.
  Noisers may overlap each other."
  [n walls]
  (take n (repeatedly #(create-noiser walls))))

(defn- create-snake
  "Creates new snake of length 1 in random location. And direction to right.
  Snake's head won't overlap walls."
  [walls]
  {:body (list (random-point walls))
   :type :snake
   :dir :right
   :color (Color/CYAN)})


(defn create-level []
  (let [walls (create-walls walls-num)
	snake (create-snake walls)
	noisers (create-noisers noisers-num  walls)
	apple (create-apple snake walls)]
    {:walls walls
     :noisers noisers
     :snake snake
     :apple apple
     :apple-generator create-apple}))

(defn level-description []
  {:name "Copy Basic"
   :level-creator create-level})