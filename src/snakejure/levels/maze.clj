(ns snakejure.levels.maze
  (:require [snakejure.core]))

(def #^{:private true} *height* 20)
(def #^{:private true} *width* 20)

(defn- neib [room]
  (map #(snakejure.core/add-points room %)
       (vals snakejure.core/dirs)))

(defn- finish? [[y x]]
  (and (= y 0)
       (= x (dec *width*))))

(defn- valid? [maze [y x :as room]] 
  (and (>= y 0) 
       (>= x 0) 
       (< y *height*) 
       (< x *width*)
       (nil? (maze room))
       (->> (neib room)
	    (filter maze)
	    (count)
	    (= 1))))

(defn- rand-set-el [set]
  (rand-nth (seq set)))

(defn- try-add-room [maze cands room]
  (if (or (valid? maze room)
	  (finish? room))
    [(conj maze room)
     (apply conj cands (neib room))]
    [maze cands]))

(defn- create-maze 
  ([]
     (create-maze #{[(dec *height*) 0]} (set (neib [(dec *height*) 0]))))
  ([maze cands] 
     (if (empty? cands)
       maze
       (let [room (rand-set-el cands)
	     [new-maze new-cands] (try-add-room maze cands room)] 
	 (recur new-maze (disj new-cands room))))))

(defn- get-walls [maze]
	(->> (for [y (range -1 (inc *height*))
		   x (range -1 (inc *width*))]
		   [y x])
	     (remove maze) 
	     (map snakejure.core/create-wall)))

(defn- apple-generator [_]
	(snakejure.core/create-apple [0
				      (dec *width*)]))

(defn- create-level []
	{:snake (snakejure.core/create-snake [[(dec *height*) 0]])
	:apple (apple-generator nil)
	:walls (get-walls (create-maze))
	:noisers [(snakejure.core/create-noiser [0
					        (dec *width*)])]
	 :apple-generator apple-generator
	 :speed 300})

(defn level-description []
	{:name "Maze"
	:level-creator create-level})