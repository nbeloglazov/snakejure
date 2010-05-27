(ns snakejure.ui
    (:use (clojure.contrib import-static)
	  (snakejure core))
    (:import (java.awt Dimension)
	     (java.awt.event ActionListener KeyListener)
	     (javax.swing JPanel Timer JFrame JOptionPane)))

(import-static java.awt.event.KeyEvent VK_SPACE)

(def dir-keys (set (keys dirs)))

(defn draw-point 
  "Draws point on given graphics g."
  [g [y x]]
  (.fillRect g (* size x) (* size y) size size))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :snake [g {:keys [body color]}]
  (.setColor g color)
  (doseq [part body] (draw-point g part)))

(defmethod paint :apple [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :wall [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location))

(defn switch-timer [timer]
  "Stop timer, if it's running.
  Start otherwise."
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

      
(defn game-panel 
  "Creates proxy...
  See source :)"
  [frame snake apple noisers walls]
  (proxy [JPanel ActionListener] []
	 (paintComponent [g]
			 (proxy-super paintComponent g)
			 (paint g @apple)
			 (paint g @snake)
			 (doseq [noiser @noisers] (paint g noiser @snake))
			 (doseq [wall @walls] (paint g wall)))
	 (actionPerformed [e]
			  (update-position snake apple walls)
			  (when (lose? @snake @walls) 
			    (JOptionPane/showMessageDialog frame "You lose...")
			    (reset snake apple noisers walls))
			  (when (win? @snake @noisers)
			    (JOptionPane/showMessageDialog frame "You win!")
			    (reset snake apple noisers walls))
			  (.repaint this))
	 (getPreferredSize []
			   (Dimension. (* (inc (dec width)) size) 
				       (* (inc (dec height)) size)))))

(defn create-key-listener [snake timer]
  (proxy [KeyListener] []
	 (keyPressed [e]
		     (if (= (.getKeyCode e) VK_SPACE)
		       (switch-timer timer))
		     (if (dir-keys (.getKeyCode e))
		       (update-direction snake (.getKeyCode e))))
	 (keyReleased [e])
	 (keyTyped [e])))

(defn game []
  "Starts game. It will create frame with walls, snake, apple and noisers in it."
  (let [walls (ref (create-walls walls-num))
	snake (ref (create-snake @walls))
	apple (ref (create-apple @snake @walls))
	noisers (ref (create-noisers noisers-num @walls))
	frame (JFrame. "Snake")
	panel (game-panel frame snake apple noisers walls)
	timer (Timer. speed panel)
 	key-listener (create-key-listener snake timer)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))
    (doto frame
      (.add panel)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))
    (.start timer)))