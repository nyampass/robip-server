(ns robip-server.endpoint.api
  (:require [compojure.core :refer [routes context GET POST]]
            [ring.util.response :as res]
            [ring.middleware.format :refer [wrap-restful-format]]))

(defn handle-build [{{:keys [code]} :params}]
  (res/response {:status :ok :code (str code)}))

(defn api-endpoint [config]
  (-> (routes
       (context "/api" []
                (GET "/build" req
                     (handle-build req))))
      (wrap-restful-format :formats [:transit-json])))
