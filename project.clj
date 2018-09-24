(defproject onyx-cheat-sheet "0.13.4.0"
  :description "Cheat sheet for Onyx"
  :url "https://github.com/onyx-platform/onyx-cheat-sheet"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :test-paths ["test/clj"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.456"]
                 [ring "1.5.1"]
                 [ring/ring-defaults "0.2.3"]
                 [secretary "1.2.3"]
                 [compojure "1.5.2"]
                 [enlive "1.1.6"]
                 ^{:voom {:repo "git@github.com:onyx-platform/onyx.git" :branch "master"}}
                 [org.onyxplatform/onyx "0.13.4"]
                 [prismatic/om-tools "0.4.0"]
                 [markdown-clj "0.9.94"]
                 [org.omcljs/om "0.9.0"]
                 [racehub/om-bootstrap "0.6.1"]
                 [fipp "0.6.9"]
                 [environ "1.1.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "onyx-cheat-sheet.jar"
  
  :jvm-opts ["-Xmx4g" "-XX:-OmitStackTraceInFastThrow"]

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:main onyx-cheat-sheet.main
                                        :output-to     "resources/public/js/app.js"
                                        :asset-path    "js/out"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    true
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:source-paths ["env/dev/clj"]
                   :test-paths ["test/clj"]

                   :dependencies [[figwheel "0.5.9"]
                                  [figwheel-sidecar "0.5.9"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [weasel "0.7.0"]]

                   :repl-options {:init-ns onyx-cheat-sheet.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :plugins [[lein-figwheel "0.5.6"]
                             [lein-pprint "1.1.1"]
                             [lein-set-version "0.4.1"]
                             [lein-update-dependency "0.1.2"]]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler onyx-cheat-sheet.server/http-handler}

                   :env {:is-dev true}

                   :cljsbuild {:test-commands { "test" ["phantomjs" "env/test/js/unit-test.js" "env/test/unit-test.html"] }
                               :builds {:app {:source-paths ["env/dev/cljs"]}
                                        :test {:source-paths ["src/cljs" "test/cljs"]
                                               :compiler {:output-to     "resources/public/js/app_test.js"
                                                          :output-dir    "resources/public/js/test"
                                                          :source-map    true
                                                          :pretty-print  false}}}}}

             :uberjar {:source-paths ["env/prod/clj"]
                       :hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :main onyx-cheat-sheet.server
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :whitespace
                                              :main onyx-cheat-sheet.main
                                              :source-map "resources/public/js/app.js.map"
                                              :pretty-print false}}}}}})
