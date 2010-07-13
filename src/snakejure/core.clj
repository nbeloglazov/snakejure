(ns snakejure.core
  (:use (clojure.contrib seq-utils
			 import-static))
  (:import (java.awt Color)))

(def width 40)
(def height 40)
(def indent 4)

(def dirs {:left  [0 -1]
	   :right [0 1]
	   :up [-1 0]
	   :down [1 0]} )
(def opposite {:left :right
               :right :left
	       :up :down
	       :down :up})

(defmacro let-map 
  "There are no neccesaty in this macro, 
  because I found out how to do it using let.
  But it's my first macro and I'll keep it yet."
  [[mp & ks] & body]
  `(let ~(vec (apply concat
		     (for [key ks] 
		          [key `(~(keyword key) ~mp)])))
     ~@body))

(defn create-wall
  "Creates new wall in given point."
  [p]
  {:location p
   :type :wall
   :color (Color/BLACK)})

(defn create-noiser
  "Creates new noiser in given point."
  [p]
  {:location p
   :type :noiser
   :color-noise (Color/BLUE)
   :color-silence (Color/MAGENTA)})

(defn create-snake
  "Creates new snake with given body. And direction to right."
  [body]
  {:body body
   :type :snake
   :dir :right
   :color (Color/CYAN)})

(defn create-apple
  "Creates new apple in given point."
  [p]
  {:location p
   :type :apple
   :color (Color/RED)})



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

(defn move-snake
  "Moves snake in current direction. If grow is given, 
  snake will grow, length will increase by 1 unit."
  [{:keys [body dir] :as snake} & grow]
  (assoc snake :body  (cons (add-points (first body) (dirs dir))
			    (if grow
			      body
			      (butlast body)))))

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

(defn inside? 
  "Checks if point is inside allowed bounds,
  it depends on width, height and indent vars."
  [[y x]]
  (and (>= x indent)
       (< x (- width indent))
       (>= y indent)
       (< y (- height indent))))

(defn normalize-level 
  "Normalizes :d in level, so snake head will be inside screen."
  [{[head & rst] :body dir :dir}  d]
  (if (inside? (add-points head d))
    d
    (add-points d ((dir opposite) dirs))))

(defn add-d 
  "Adds d to new level in such way, that snake will in the center."
  [{ {[[y x]] :body} :snake :as level}]
  (assoc level :d [ (- (quot height 2) y)
		    (- (quot width 2) x)] ))

(defn update-snake-direction 
  "Takes level ref, and set new snake with updated direction in it."
  [level dir]
  (dosync (alter level 
		 assoc :snake (turn-snake (:snake @level) dir))))


(defn update-snake-position
  "Takes level ref and moves snake in it,
  altering ref."
  [level]
  (let-map [@level snake apple walls apple-generator d]
	   (dosync 
	    (if (eats? snake apple)
	      (let [new-snake (move-snake snake :grow)]
		(alter level assoc :snake new-snake 
                                   :apple (apple-generator level)
				   :d (normalize-level new-snake d)))
	      (let [new-snake (move-snake snake)]
		(alter level assoc :snake new-snake
		                   :d (normalize-level new-snake d)))))))

