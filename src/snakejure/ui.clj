(ns snakejure.ui
    (:use (clojure.contrib import-static))
    (:import (java.awt Dimension)
	     (java.awt.event ActionListener KeyListener)
	     (javax.swing JPanel Timer JFrame JOptionPane)))

(import-static java.awt.event.KeyEvent VK_SPACE)


(load "/core")
(refer 'snakejure.core)

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

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location))

(defn switch-timer [timer]
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

      
(defn game-panel 
  "Creates proxy...
  See source :)"
  [frame snake apple noisers]
  (proxy [JPanel ActionListener] []
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
  "Starts game. It will create frame with snake, apple and noisers in it."
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