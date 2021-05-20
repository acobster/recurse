;; A simple grow-only counter CRDT, AKA G-Counter.
(ns gcounter
  (:require
    [clojure.core.async :refer [chan <! put! mult tap untap go-loop]]))

;; Define a simple top-level API:
;; `value`
;; `increment`


;; PLUMBING
;; This isn't really part of our CRDT implementation or API, think of it as
;; basically the network topology.

;; First, set up a global channel to broadcast over.
(def broadcast-channel (chan))
;; Now set up a global endpoint where all nodes can listen for broadcasts.
(def endpoint (mult broadcast-channel))

(def subscriptions (atom #{}))

(defn broadcast!
  "Broadcast the message x. Can be literally any value."
  [x]
  (prn "Broadcasting:" x)
  (put! broadcast-channel x))

(defn subscribe!
  "Subscribe to the global endpoint, calling (f message) for each message
  received. Returns a channel tapped into the endpoint channel."
  [f]
  (let [subscription (chan)]
    (tap endpoint subscription)
    (swap! subscriptions conj subscription)
    (go-loop []
             (let [message (<! subscription)]
               ;; simulate network latency
               (Thread/sleep (rand-int 3000))
               (f message))
             (recur))
    subscription))

(defn unsub-all! []
  (doall (for [subscription @subscriptions]
           (untap endpoint subscription)))
  (reset! subscriptions #{}))


(comment
  (subscribe! (fn [message] (prn "Node 1:" message)))
  (subscribe! (fn [message] (prn "Node 2:" message)))

  (broadcast! [3, 2, 1])
  (broadcast! [1, 1, 2])

  (deref subscriptions)
  (unsub-all!)

  )


;; join is a merge using max(): merge([3, 2, 0], [2, 2, 1]) => [3, 2, 1]
;; value is just sum(): value([3, 2, 1]) => 6

(defn join [a b]
  (map max a b))

(defn sum [v]
  (reduce + v))


(defn node [idx]
  (atom {:idx idx
         :state [0, 0, 0]}))

(def node0 (node 0))
(def node1 (node 1))
(def node2 (node 2))

(subscribe! (fn [new-state]
              (swap! node0 update :state (fn [old-state]
                                           (prn "join at node 0" (join old-state new-state))
                                           (join old-state new-state)))))
(subscribe! (fn [new-state]
              (swap! node1 update :state (fn [old-state]
                                           (prn "join at node 1" (join old-state new-state))
                                           (join old-state new-state)))))
(subscribe! (fn [new-state]
              (swap! node2 update :state (fn [old-state]
                                           (prn "join at node 2" (join old-state new-state))
                                           (join old-state new-state)))))


(def nodes [node0 node1 node2])


(defn increment [{:keys [idx state]}]
  (let [state (update state idx inc)]
    (prn "new state:" state)
    (swap! (get nodes idx) assoc :state state)
    (broadcast! state)))

(defn value [{:keys [state]}]
  (sum state))

;; const x = {a: "A"}

;; const a = x.a
;; OR
;; const { a } = x



(comment
  (value [3, 2, 1])
  (= [3, 2, 1] (join [3, 2, 0] [2, 2, 1]))

  (swap! node0 update :state (fn [old-state]
                               (prn "join at node 0" (join old-state [0 0 0]))
                               (join old-state [0 0 0])))
  (broadcast! [2 0 0])

  (map deref nodes)

  (doall (for [_ (range 3)]
    (do
      (Thread/sleep (rand-int 3000))
      (increment (deref (get nodes (rand-int 3))))
      (prn "Value at Node 0:" (value @node0))
      (prn "Value at Node 1:" (value @node1))
      (prn "Value at Node 2:" (value @node2))
      :done)))

  )
