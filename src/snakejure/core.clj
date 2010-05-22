(ns snakejure.core
    (:use (clojure.contrib seq-utils
			   import-static))
    (:import (java.awt Color Dimension)
	     (java.awt.event ActionListener KeyListener)
	     (javax.swing JPanel Timer JFrame JOptionPane)))

(import-static java.awt.event.KeyEvent VK_LEFT VK_RIGHT VK_DOWN VK_UP VK_SPACE)

(def width 30)
(def height 30)
(def size 20)
(def speed 100)
(def noisers-num 2)
(def dirs { VK_LEFT [0 -1]
	    VK_RIGHT [0 1]
	    VK_UP [-1 0]
	    VK_DOWN [1 0]} )


(defn overlaps-body? [{body :body} point]
  (includes? body point))

(defn add-points [& args]
  (vec (apply map + args)))

(defn random-point []
  [(rand-int height) (rand-int width)])

(defn create-apple 
  ([] 
   {:location (random-point)
   :type :apple
   :color (Color/RED)})
  ([snake]
   (find-first #(not (overlaps-body? snake (:location %))) (repeatedly create-apple))))

(defn create-noiser []
  {:location (random-point)
   :type :noiser
   :color-noise (Color/BLUE)
   :color-silence (Color/MAGENTA)})

(defn create-noisers [n]
  (take n (repeatedly create-noiser)))

(defn create-snake [] 
  {:body (list (random-point))
   :type :snake
   :dir VK_RIGHT
   :color (Color/CYAN)})

(defn normalize-point [[y x]]
  [(rem (+ y height) height)
   (rem (+ x width) width)])

(defn move-snake [{:keys [body dir] :as snake} & grow]
  (assoc snake :body  (cons (normalize-point (add-points (first body) (dirs dir)))
			    (if grow body (butlast body)))))

(defn eats? [{[head] :body} {apple :location}]
  (= head apple))

(defn turn-snake [{:keys [dir] :as snake} new-dir]
  (if (= [0 0] (add-points (dirs dir) (dirs new-dir)))
    snake
    (assoc snake :dir new-dir)))

(defn lose? [{[head & body] :body}]
  (includes? body head))

(defn win? [{body :body} noisers]
  (every? true? (map #(includes? body (:location %)) noisers)))

(defn reset [snake apple noisers]
  (dosync 
   (ref-set snake (create-snake))
   (ref-set apple (create-apple @snake))
   (ref-set noisers (create-noisers noisers-num))))

(defn update-direction [snake dir]
  (dosync (alter snake turn-snake dir)))

(defn update-position [snake apple]
  (dosync 
   (if (eats? @snake @apple)
     (do (alter snake move-snake :grow)
	 (ref-set apple (create-apple snake))))
     (alter snake move-snake)))

(defn draw-point [g [y x]]
  (.fillRect g (* size x) (* size y) size size))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :snake [g {:keys [body color]}]
  (.setColor g color)
  (doseq [part body] (draw-point g part)))

(defmethod paint :apple [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location))

(defn switch-timer [timer]
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

      
(defn game-panel [frame snake apple noisers]
  (proxy [JPanel KeyListener ActionListener] []
     (paintComponent [g]
       (proxy-super paintComponent g)
       (paint g @apple)
       (paint g @snake)
       (doseq [noiser @noisers] (paint g noiser @snake)))
     (actionPerformed [e]
       (update-position snake apple)
       (when (lose? @snake) 
	 (JOptionPane/showMessageDialog frame "You lose...")
	 (reset snake apple noisers))
       (when (win? @snake @noisers)
	 (reset snake apple noisers))
       (.repaint this))
     (getPreferredSize []
       (Dimension. (* (inc (dec width)) size) 
		   (* (inc (dec height)) size)))))

(defn create-key-listener [snake timer]
  (proxy [KeyListener] []
    (keyPressed [e]
       (if (= (.getKeyCode e) VK_SPACE)
	  (switch-timer timer)
	  (update-direction snake (.getKeyCode e))))
     (keyReleased [e])
     (keyTyped [e])))

(defn game []
  (let [snake (ref (create-snake))
	apple (ref (create-apple snake))
	noisers (ref (create-noisers noisers-num))
	frame (JFrame. "Snake")
	panel (game-panel frame snake apple noisers)
	timer (Timer. speed panel)
	key-listener (create-key-listener snake timer)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))
    (doto frame
      (.add panel)
      (.pack)
      (.setVisible true))
    (.start timer)
    [snake apple noisers timer]))