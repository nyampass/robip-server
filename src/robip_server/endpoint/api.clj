(ns robip-server.endpoint.api
  (:require [compojure.core :refer [routes context GET POST]]
            [compojure.route :as route]
            [ring.util.response :as res]
            [ring.middleware.format :refer [wrap-restful-format]]
            [robip-server.builder :as builder]
            [robip-server.component.db :as db])
  (:import java.io.File))

(defn response [status opts]
  (res/response (merge {:status status} opts)))

(defn ok [& {:as opts}]
  (response :ok opts))

(defn error [msg & {:as opts}]
  (response :error (merge {:message msg} opts)))

(defn build [{{:keys [id code wifi]} :params} db]
  (if code
    (let [prev-build (or (:build (db/peek-latest db id)) 0)
          result (builder/build code {:robip-id id :build (inc prev-build) :wifi wifi})
          {bin-file :bin-file {:keys [out err exit]} :result} result]
      (if bin-file
        (let [build (db/save-file db id bin-file)]
          (ok :build build :out out :err err :exit exit))
        (error "build failed" :out out :err err :exit exit)))
    (error "invalid request")))

(defn fetch-latest [{{:keys [id since]} :params} db]
  (if-let [{:keys [build path]} (db/fetch-latest db id)]
    (if (or (not since) (< (Integer. since) build))
      (res/file-response path)
      {:status 404 :headers {} :body ""})
    (error "invalid id")))

(defn api-endpoint [{db :db}]
  (-> (routes
       (GET "/" []
            (-> "index.html"
                (res/file-response {:root "resources/public"
                                    :allow-symlinks? true})
                (res/content-type "text/html")))
       (context "/api" []
                (POST "/:id/build" req
                      (build req db))
                (GET "/:id/latest" req
                     (fetch-latest req db)))
       (route/resources "/"))
      (wrap-restful-format :formats [:json-kw])))
