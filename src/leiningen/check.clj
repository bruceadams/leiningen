(ns leiningen.check
  "Check syntax and warn on reflection."
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [bultitude.core :as b]
            [clojure.java.io :as io]))

(defn check
  "Check syntax and warn on reflection."
  ([project]
     (let [source-files (map io/file (:source-paths project))
           nses (b/namespaces-on-classpath :classpath source-files)
           ns-src-map (zipmap '(1 2 3) '(<file> 5 6))
           action `(let [failures# (atom 0)
                         something# ~ns-src-map]
;                     (println something#)
                     (doseq [ns# '~nses]
                       ;; load will add the .clj, so can't use ns/path-for.
                       (let [ns-file# (-> (str ns#)
                                          (.replace \- \_)
                                          (.replace \. \/))]
                         (println "Compiling namespace" ns#)
                         (try
                           (binding [*warn-on-reflection* true]
                             (load ns-file#))
                           (catch java.io.FileNotFoundException fnfe#
                             (swap! failures# inc)
                             (println (str "File " ; (something# ns#)
                                           " for namespace " ns# " not found!")))
                           (catch ExceptionInInitializerError e#
                             (swap! failures# inc)
                             (.printStackTrace e#)))))
                     (System/exit @failures#))]
       (try (eval/eval-in-project project action)
            (catch clojure.lang.ExceptionInfo e
              (main/abort "Failed."))))))
