(ns snakejure.work
  (:use [snakejure.core :only (start-server start-client)]
        [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)]))

#_ (
; Вспомогательная функция для отображения новых сообщений.
(defn show-new [messages]
  (doseq [message @messages]
    (println message))
  (reset! messages []))


; Запустим сервер
(def server (start-server))



; Создадим и подключим первого клиента.
(def ch1 @(start-client))

; Создадим атом, в который первый
; клиент будет складывать сообщения.
(def mes1 (atom []))

; Добавим обработчик,
; который все сообщение будет складывать в mes1.
(receive-all ch1 #(swap! mes1 conj %))

; Пошлём на сервер сообщение.
(enqueue ch1 "Hello from 1st client!")

; Посмотрим, пришло первому клиенту его же сообщение.
(show-new mes1)



; Создадим второго клиента.
(def ch2 @(start-client))

; Создадим сообщения полученные вторым клиентом.
(def mes2 (atom []))

; Добавим обработчик на получение сообщений.
(receive-all ch2 #(swap! mes2 conj %))

; Пошлём сообщение
(enqueue ch2 "2st client is here!")

; Посмотрим, получил ли 2 клиент своё сообщение.
(show-new mes2)

; Посмотрим сообщения 1 клиента.
(show-new mes1)

; Остановим сервер
(server)




; Попробуем подключится к нелокальному серверу.
(def ch @(object-client
          {:host "taste-o-code.com" :port 12345}))

; Создадим атом собщений.
(def mes (atom []))

(receive-all ch #(swap! mes conj %))

; Пошлём сообщение!
(enqueue ch "Is anybody here?")

; Мы получили своё сообщение?
(show-new mes)

)

