(ns main
  (:require [hiccup2.core :as h]
            [reitit.ring :as rr]
            [org.httpkit.server :as hk-server]
            [ring.util.response :as ruresp]
            [jsonista.core :as json]
            [starfederation.datastar.clojure.api
             :as d*
             :refer [merge-fragment! merge-signals!]]
            [starfederation.datastar.clojure.adapter.http-kit
             :as http-kit
             :refer [->sse-response on-open]]))

(defn home
  [current-time]
  (h/html
   [:title "DATASTAR with Clojure"]
   [:meta
    {:name "viewport", :content "width=device-width, initial-scale=1.0"}]
   [:script
    {:type "module",
     :src
     "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-beta.9/bundles/datastar.js"}]
   [:link
    {:href "https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css", :rel "stylesheet"}]
   [:body {:data-signals (str "{currentTime: " "\"bkjb\"" "}"), :class "min-h-screen bg-gray-900 text-white font-mono flex items-center justify-center p-6 bg-[radial-gradient(ellipse_at_center,_#1a1a2e_0%,_#0d0d1a_70%)]"}
    [:div
     {:class "container max-w-3xl mx-auto grid gap-8"}
     [:div
      {:class "time relative bg-gray-800/80 border border-neon-cyan/50 rounded-lg p-8 shadow-[0_0_15px_rgba(0,255,255,0.3)] hover:shadow-[0_0_25px_rgba(0,255,255,0.5)] transition-all duration-300",
       :data-on-load "@get('/updates')"}
      [:div {:class "absolute inset-0 bg-[linear-gradient(45deg,_#00ffff22,_#ff00ff22)] opacity-20 rounded-lg pointer-events-none"}]
      [:span {:class "text-neon-cyan text-xl tracking-wider"} "Fragment Sync: "]
      [:span {:id "currentTime",
              :class "text-neon-cyan font-bold text-2xl glow"} (str current-time)]]
     [:div
      {:class "time relative bg-gray-800/80 border border-neon-pink/50 rounded-lg p-8 shadow-[0_0_15px_rgba(255,0,255,0.3)] hover:shadow-[0_0_25px_rgba(255,0,255,0.5)] transition-all duration-300"}
      [:div {:class "absolute inset-0 bg-[linear-gradient(45deg,_#ff00ff22,_#00ffff22)] opacity-20 rounded-lg pointer-events-none"}]
      [:span {:class "text-neon-pink text-xl tracking-wider"} "Signal Sync: "]
      [:span {:data-text "$currentTime",
              :class "text-neon-purple font-bold text-2xl glow"} (str current-time)]]]]
   [:style "
     .text-neon-cyan { color: #00ffff; text-shadow: 0 0 5px #00ffff, 0 0 10px #00ffff; }
     .text-neon-green { color: #00ff00; text-shadow: 0 0 5px #00ff00, 0 0 10px #00ff00; }
     .text-neon-pink { color: #ff00ff; text-shadow: 0 0 5px #ff00ff, 0 0 10px #ff00ff; }
     .text-neon-purple { color: #ff00cc; text-shadow: 0 0 5px #ff00cc, 0 0 10px #ff00cc; }
     .border-neon-cyan\\/50 { border-color: rgba(0, 255, 255, 0.5); }
     .border-neon-pink\\/50 { border-color: rgba(255, 0, 255, 0.5); }
     .glow { animation: glow 1.5s ease-in-out infinite alternate; }
     @keyframes glow { from { text-shadow: 0 0 5px currentColor, 0 0 10px currentColor; } to { text-shadow: 0 0 10px currentColor, 0 0 20px currentColor; } }
   "]))

(defn frag
  [current-time]
  (h/html
   [:span {:id "currentTime"
           :class "text-neon-cyan font-bold text-2xl glow"} (str current-time)]))

;; ==== Handlers ====
(defn render-home
  [req]
  (-> (java.time.LocalDateTime/now)
      (home)
      (str)
      (ruresp/response)
      (ruresp/content-type "text/html")))

(defn render-updates
  [req]
  (->sse-response
   req
   {on-open
    (fn [sse]
     (d*/with-open-sse sse
       (while true
         (do
           (let [current-time (java.time.LocalDateTime/now)]
             (d*/merge-fragment! sse (str (frag current-time)))
             (d*/merge-signals! sse (json/write-value-as-string {"currentTime" current-time})))
           (Thread/sleep 1)))))}))

(def routes
  [["/" {:get #'render-home}]
   ["/updates" {:get #'render-updates}]])

(def app
  (->
   routes
   (rr/router)
   (rr/ring-handler)))

(def my-server
  (hk-server/run-server #'app {:port 8080}))

(comment
  (my-server))
