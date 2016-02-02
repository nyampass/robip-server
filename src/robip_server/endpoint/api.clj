(ns robip-server.endpoint.api
  (:require [compojure.core :refer [routes context GET POST]]
            [ring.util.response :as res]
            [ring.middleware.format :refer [wrap-restful-format]]
            [robip-server.builder :as builder]
            [robip-server.component.db :as db]))

(defn response [status opts]
  (res/response (merge {:status status} opts)))

(defn ok [& {:as opts}]
  (response :ok opts))

(defn error [msg & {:as opts}]
  (response :error (merge {:message msg} opts)))

(defn build [{{:keys [id code]} :params} db]
  (if code
    (let [{bin-file :bin-file {:keys [out err exit]} :result} (builder/build code)]
      (if bin-file
        (do (db/save-file db id bin-file)
            (ok :out out :err err :exit exit))
        (error "build failed" :out out :err err :exit exit)))
    (error "invalid request")))


(defn api-endpoint [{db :db}]
  (-> (routes
       (context "/api" []
                (POST "/:id/build" req
                      (build req db))
      (wrap-restful-format :formats [:json-kw])))
