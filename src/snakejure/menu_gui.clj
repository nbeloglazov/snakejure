(ns snakejure.menu-gui
    (:import (javax.swing JFrame JList JLabel JOptionPane JPanel ListCellRenderer ImageIcon)
	     (java.awt.event KeyAdapter KeyEvent)
	     (java.awt Dimension Color BorderLayout)
	     (org.apache.log4j Logger FileAppender SimpleLayout Level))
    (:use [snakejure.level-loader :only (get-levels-map)]
	  [snakejure.game-gui :only (create-game-panel)]
	  [snakejure.results :only (update-results get-results-map)]))

(def #^{:private true} width 300)
(def #^{:private true} height 400)
(def *log-file* "log.txt")
(def *log-level* (Level/TRACE))

(defn- init-logging 
  "Inits logging to log it to *log-file*"
  []
  (doto (Logger/getRootLogger)
    (.addAppender (FileAppender. (SimpleLayout.) *log-file* false))
    (.setLevel *log-level*)))

(defn- get-level-vectors
  "Create vector of levels + according result"
  []
  (let [level-names (sort (keys (get-levels-map)))
	results (get-results-map)]
    (for [name level-names] [name (results name)])))

(defn- create-list-renderer []
  (let [icon (ImageIcon. "resources/check.gif")]
    (proxy  [ListCellRenderer] []
      (getListCellRendererComponent [_ [name result] _ is-sel _]
				    (doto (JPanel. (BorderLayout.))
				      (.setName name)
				      (.add (JLabel. name (when (= result :win) icon) (JLabel/LEFT)))
				      (.setBackground (if is-sel
							(Color/LIGHT_GRAY)
							(Color/WHITE))))))))

    

(defn- create-poor-jlist
  "Creates Jlist without any listeners."
  []
  (doto (JList. (to-array (get-level-vectors)))
    (.setCellRenderer (create-list-renderer))
    (.setSelectedIndex 0)))

(defn- switch-panel [frame panel]
  (doto (.getContentPane frame)
    (.removeAll)
    (.add panel)
    (.revalidate))
  (doto frame
    (.pack)
    (.setLocationRelativeTo nil))
  (.requestFocus panel))

(declare create-jlist)

(defn end-fn [frame name result]
  (case result
	:win (do
	       (JOptionPane/showMessageDialog frame (str "You win level " name))
	       (update-results name :win))
	:lose (JOptionPane/showMessageDialog frame (str "You lose level " name))
	nil)
  (switch-panel frame (create-jlist frame)))


(defn- create-key-listener [frame]
  (proxy [KeyAdapter] []
	 (keyPressed [e] 
		   (when (= (.getKeyCode e) (KeyEvent/VK_ENTER))
			  (let [name (first (.. e getSource getSelectedValue))
				levels-map (get-levels-map)
				game-panel (create-game-panel ((levels-map name)) 
							      (partial end-fn frame name))]
			    (switch-panel frame game-panel))))))

(defn- create-jlist [frame]
  (let [jlist (create-poor-jlist)
	key-listener (create-key-listener frame)]
    (doto jlist
      (.addKeyListener key-listener)
      (.setPreferredSize (Dimension. width height)))))

(defn show-menu []
  (let [frame (JFrame. "Snakejure")
	list (create-jlist frame)]
    (init-logging)
    (doto frame
      (.add list)
;      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setLocationRelativeTo nil)
      (.setVisible true))))
	 
  