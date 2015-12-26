(ns robip-server.builder
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [me.raynes.fs :as fs]))

(def command-run-platformio "/usr/local/bin/platformio run")
(def base-project "./resources/platformio-project")

(defn- project-config []
  (let [project-dir (fs/file (fs/tmpdir) (fs/temp-name "robip-" ""))]
    {:base-dir project-dir
     :source-file (fs/file project-dir "src" "main.ino")
     :command (str "cd " (.getPath project-dir) "; " command-run-platformio)
     :bin-file (fs/file project-dir ".pioenvs" "esp12e" "firmware.bin")}))

(defn build [code]
  (let [config (project-config)]
    (fs/copy-dir (io/file base-project) (:base-dir config))
    (with-open [writer (io/writer (:source-file config))]
      (.write writer code))
    (let [command-result (sh "/bin/sh" "-c" (:command config))
          result (assoc config
                        :result command-result)]
      (if (= (:exit command-result) 0)
        (dissoc result :bin-file)
        result))))

(defn clean [{:keys [base-dir] :as build-result}]
  (when (and base-dir
             (.startsWith (.getPath base-dir) (fs/tmpdir)))
    (fs/delete-dir base-dir)))










