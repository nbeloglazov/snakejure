(ns snakejure.core
    (:use (clojure.contrib seq-utils
			   import-static))
    (:import (java.awt Color)))

(import-static java.awt.event.KeyEvent VK_LEFT VK_RIGHT VK_DOWN VK_UP)

(def width 30)
(def height 30)
(def size 20)
(def speed 100)
(def noisers-num 2)
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

(defn random-point
  "Creates random point with x in [0,width), y in [0, height)."
  []
  [(rand-int height) (rand-int width)])

(defn create-apple 
  "Creates new apple in random location. 
  If snake is given, created apple will not overlap snake's body."
  ([] 
   {:location (random-point)
    :type :apple
    :color (Color/RED)})
  ([snake]
   (find-first #(not (overlaps-body? snake (:location %))) (repeatedly create-apple))))

(defn create-noiser []
  "Creates new noiser in random location."
  {:location (random-point)
   :type :noiser
   :color-noise (Color/BLUE)
   :color-silence (Color/MAGENTA)})

(defn create-noisers [n]
  "Creates sequence with n noisers in random locations. 
  Noisers may overlap each other."
  (take n (repeatedly create-noiser)))

(defn create-snake [] 
  "Creates new snake of length 1 in random location. And direction to right."
  {:body (list (random-point))
   :type :snake
   :dir VK_RIGHT
   :color (Color/CYAN)})

(defn normalize-point [[y x]]
  "Normalize points, so it'll lay inside field."
  [(rem (+ y height) height)
   (rem (+ x width) width)])

(defn move-snake [{:keys [body dir] :as snake} & grow]
  "Moves snake in current direction. If grow is given, 
  snake will grow, length will increase by 1 unit."
  (assoc snake :body  (cons (normalize-point (add-points (first body) (dirs dir)))
			    (if grow body (butlast body)))))

(defn eats? [{[head] :body} {apple :location}]
  "Checks if snake can ear appple."
  (= head apple))

(defn turn-snake [{:keys [dir] :as snake} new-dir]
  "Turns snake in given direction."
  (if (= [0 0] (add-points (dirs dir) (dirs new-dir)))
    snake
    (assoc snake :dir new-dir)))

(defn lose? [{[head & body] :body}]
  "Checks if user lose. Now it's only possible if snake bite itself."
  (includes? body head))

(defn win? [{body :body} noisers]
  "Check if use win. User wins, when snake lay on every noser simultaneously."
  (every? true? (map #(includes? body (:location %)) noisers)))

(defn reset [snake apple noisers]
  "Takes refs of snake, apple, noisers and resets game. 
  It will set to refs new generated snake, apple and ref. 
  Snake will be 1 unit length."
  (dosync 
   (ref-set snake (create-snake))
   (ref-set apple (create-apple @snake))
   (ref-set noisers (create-noisers noisers-num))))

(defn update-direction [snake dir]
  "Takes refs of snake, and set new snake with updated direction to it."
  (dosync (alter snake turn-snake dir)))

(defn update-position [snake apple]
  "Takes refs of snake and apple, and moves snakes.
  If snake eats apple, length increases."
  (dosync 
   (if (eats? @snake @apple)
     (do (alter snake move-snake :grow)
	 (ref-set apple (create-apple snake))))
     (alter snake move-snake)))

