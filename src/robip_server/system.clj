(ns robip-server.system
  (:require [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [robip-server.endpoint.api :refer [api-endpoint]]
            [robip-server.component.db :as db]))

(defn base-config [session-cookie-key]
  {:app {:middleware [[wrap-not-found :not-found]
                      [wrap-defaults :defaults]
                      #(wrap-session % {:cookie-attrs {:max-age 86400}
                                        :store (cookie-store session-cookie-key)})]
         :not-found  "Resource Not Found"
         :defaults   (meta-merge api-defaults {})}})

(defn new-system [config]
  (let [config (meta-merge (base-config (-> config :http :session-cookie-key))
                           config)]
    (-> (component/system-map
         :app  (handler-component (:app config))
         :http (jetty-server (:http config))
         :api (endpoint-component api-endpoint)
         :db (db/db-component (:db config)))
        (component/system-using
         {:http [:app]
          :app  [:api]
          :api [:db]}))))
