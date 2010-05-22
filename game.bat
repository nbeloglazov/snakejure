# /bin/sh
java -cp "lib/*":"src/snakejure":. clojure.main -e "(do (load \"ui\") (snakejure.ui/game))"