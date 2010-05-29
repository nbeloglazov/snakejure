(ns snakejure.core
    (:use (clojure.contrib seq-utils
			   import-static))
    (:import (java.awt Color)))

(import-static java.awt.event.KeyEvent VK_LEFT VK_RIGHT VK_DOWN VK_UP)

(def width 30)
(def height 30)
(def size 20)
(def speed 100)
(def noisers-num 4)
(def walls-num 20)
(def dirs { VK_LEFT [0 -1]
	    VK_RIGHT [0 1]
	    VK_UP [-1 0]
	    VK_DOWN [1 0]} )


(defn overlaps-body? 
  "Check, if snake's body overlaps some given point."
  [{body :body} point]
  (includes? body point))

(defn add-points
  "Add two or more points by coordinates."
  [& args]
  (vec (apply map + args)))

(defn overlaps-walls? 
  "Check, if some given point overlaps walls."
  [walls point]
  (not (every? false? (map #(= point (:location %)) walls))))

(defn random-point
  "Creates random point with x in [0,width), y in [0, height).
  If walls is given, then created point won't overlap them."
  ([]
   [(rand-int height) (rand-int width)])
  ([walls]
   (find-first #(not (overlaps-walls? walls %)) (repeatedly random-point))))

(defn create-apple 
  "Creates new apple in random point. 
  Created apple won't overlap snake's body and walls."
  [snake walls] 
   {:location (find-first #(not (overlaps-body? snake %)) (repeatedly #(random-point walls)))
    :type :apple
    :color (Color/RED)})

(defn create-wall
  "Creates new wall in random point."
  []
  {:location (random-point)
   :type :wall
   :color (Color/BLACK)})

(defn create-walls [n]
  "Creates sequence of n walls in random points.
  Walls may overlap each other"
  (take n (repeatedly create-wall)))


(defn create-noiser [walls]
  "Creates new noiser in random location.
  It won't overlap walls."
  {:location (random-point walls)
   :type :noiser
   :color-noise (Color/BLUE)
   :color-silence (Color/MAGENTA)})

(defn create-noisers
  "Creates sequence with n noisers in random locations. 
  Noisers may overlap each other."
  [n walls]
  (take n (repeatedly #(create-noiser walls))))

(defn create-snake
  "Creates new snake of length 1 in random location. And direction to right.
  Snake's head won't overlap walls."
  [walls] 
  {:body (list (random-point walls))
   :type :snake
   :dir VK_RIGHT
   :color (Color/CYAN)})

(defn normalize-point
  "Normalize points, so it'll lay inside field."
  [[y x]]
  [(rem (+ y height) height)
   (rem (+ x width) width)])

(defn move-snake
  "Moves snake in current direction. If grow is given, 
  snake will grow, length will increase by 1 unit."
  [{:keys [body dir] :as snake} & grow]
  (assoc snake :body  (cons (normalize-point (add-points (first body) (dirs dir)))
			    (if grow body (butlast body)))))

(defn eats?
  "Checks if snake can ear appple."
  [{[head] :body} {apple :location}]
  (= head apple))

(defn turn-snake
  "Turns snake in given direction."
  [{:keys [dir] :as snake} new-dir]
  (if (= [0 0] (add-points (dirs dir) (dirs new-dir)))
    snake
    (assoc snake :dir new-dir)))

(defn lose?
  "Checks if user lose. It's possible if snake bite itself or hit wall."
  [{[head & body] :body} walls]
  (or (includes? body head)
      (overlaps-walls? walls head)))

(defn win? 
  "Check if use win. User wins, when snake lay on every noser simultaneously."
  [{body :body} noisers]
  (every? true? (map #(includes? body (:location %)) noisers)))

(defn reset 
  "Takes refs of snake, apple, noisers and resets game. 
  It will set to refs new generated snake, apple and ref. 
  Snake will be 1 unit length."
  [snake apple noisers walls]
  (dosync 
   (ref-set walls (create-walls walls-num))
   (ref-set snake (create-snake @walls))
   (ref-set apple (create-apple @snake @walls))
   (ref-set noisers (create-noisers noisers-num @walls))))

(defn update-direction 
  "Takes refs of snake, and set new snake with updated direction to it."
  [snake dir]
  (dosync (alter snake turn-snake dir)))

(defn update-position
  "Takes refs of snake and apple, and moves snakes.
  If snake eats apple, length increases."
  [snake apple walls]
  (dosync 
   (if (eats? @snake @apple)
     (do (alter snake move-snake :grow)
	 (ref-set apple (create-apple @snake @walls)))
     (alter snake move-snake))))

