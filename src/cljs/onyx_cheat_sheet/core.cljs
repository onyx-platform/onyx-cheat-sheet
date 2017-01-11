(ns onyx-cheat-sheet.core
  (:require [om.core :as om :include-macros true]
	    [om.dom :as dom :include-macros true]
	    [om-bootstrap.panel :as p]
	    [om-bootstrap.button :as b]
	    [om-bootstrap.random :as r]
	    [om-bootstrap.nav :as n]
	    [om-bootstrap.grid :as g]
	    [om-bootstrap.input :as i]
	    [om-tools.core :refer-macros [defcomponent]]
	    [om-tools.dom :include-macros true]
	    [onyx.information-model :refer [model model-display-order]]
	    [markdown.core]
            [clojure.string]
	    [fipp.edn :refer [pprint]]
	    [secretary.core :as secretary :refer-macros [defroute]]
	    [goog.events :as events]
	    [goog.history.EventType :as EventType])
(:import goog.History))

(def model-order 
  [:job :catalog-entry :flow-conditions-entry :lifecycle-entry :lifecycle-calls
   :window-entry :state-aggregation :trigger-entry :state-event :event-map
   :peer-config :env-config])


(def model-names {:job "Job"
                  :catalog-entry "Catalogs"
                  :flow-conditions-entry "Flow Conditions"
                  :lifecycle-entry "Lifecycles"
                  :lifecycle-calls "Lifecycle Calls"
                  :window-entry "Windows"
                  :state-aggregation "State / Aggregation"
                  :trigger-entry "Triggers"
                  :state-event "State Event"
                  :event-map "Event Map"
                  :peer-config "Peer Configuration"
                  :env-config "Environment Configuration"})

(defonce app-state (atom {}))

(secretary/set-config! :prefix "#")

(defroute home-path "/" []
  (swap! app-state assoc :view nil))

(defroute calls-path "/:calls" [calls]
  (swap! app-state assoc :view (if (= "all" calls)
				 (keys model)
				 [(keyword calls)])))

(defroute calls-jump "/:calls/:namespace/:name" [calls namespace name]
  (swap! app-state assoc :view [(keyword calls)])
  ;; Super hacky way to jump to the correct entry
  (.setTimeout js/window 
               #(let [element (.getElementById js/document (str namespace "/" name))]
                  (.scrollIntoView element)) 
               200))

(defroute search-route "/search/:search" [search]
  (swap! app-state assoc :search search))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))

;;; SEARCH CODE ;;;
;;;
(defn flatten-anything [coll]
  (cond (or (sequential? coll)
            (set? coll))
        (mapcat flatten-anything coll)
        (map? coll)
        (into (mapcat flatten-anything (keys coll))
              (mapcat flatten-anything (vals coll)))
        :else
        [coll]))

(defn stringize-sub-model [sub-model]
  (->> sub-model
       (map (fn [kv]
                (vector (first kv) (clojure.string/lower-case (clojure.string/join " " (flatten-anything kv))))))
       (into {})))

(defn stringize [model]
  (reduce (fn [m k]
            (update-in m [k :model] stringize-sub-model))
          model
          (keys model)))

(def stringized (stringize model))

(defn filter-sub-model [sub-model model sub-key substring]
  (let [stringized-sub (get stringized sub-key)]
    (->> sub-model
         (filter (fn [[k v]]
                   (if (re-find (re-pattern substring)
                                (get-in stringized-sub [:model k]))
                     [k v])))
         (into {}))))

(defn filter-model [model substring]
  (if (empty? substring)
    model
    (reduce (fn [m k]
	      (update-in m [k :model] filter-sub-model model k substring))
	    model
	    (keys model))))

(def examples
  {:catalog-entry
   {:core-async-input
    {:doc "A catalog entry to read input from a core.async channel."
     :code
     {:onyx/name :my-input-task-name
      :onyx/plugin :onyx.plugin.core-async/input
      :onyx/type :input
      :onyx/medium :core.async
      :onyx/batch-size 20
      :onyx/max-peers 1
      :onyx/doc "Reads segments from a core.async channel"}
     :restrictions ["`:onyx/max-peers` must be set to `1`. Multiple peers trying to read from the same channel would yield incorrect behavior."
                    "The core.async input plugin is not fault tolerant. Only use it for development."]}

    :core-async-output
    {:doc "A catalog entry to write output to a core.async channel."
     :code
     {:onyx/name :my-output-task-name
      :onyx/plugin :onyx.plugin.core-async/output
      :onyx/type :output
      :onyx/medium :core.async
      :onyx/batch-size 20
      :onyx/max-peers 1
      :onyx/doc "Writes segments to a core.async channel"}
     :restrictions ["`:onyx/max-peers` must be set to `1`. Multiple peers trying to write to the same channel would yield incorrect behavior."
                    "You'll probably want to use a channel with a sliding or dropping buffer. If a channel put operation blocks, a virtual peer threads will also block, and progress cannot be made until the channel unblocks."]}}})


(defn codify [text]
  (om.dom/div #js {:dangerouslySetInnerHTML #js {:__html (markdown.core/md->html text)}} nil))

(defn allowed-types [section k]
  (let [types (get-in model [section :model k :type])
        types (if-not (coll? types) [types] types)]
    [(dom/strong #js {:className "inline-header"} "allowed types")
     (for [t types]
       (r/badge {:class "onyx-badge"} (pr-str t)))]))

(defn restrictions [usage section k]
  (when-let [restrictions (get-in usage [section :model k :restrictions])]
    (r/alert {:bs-style "warning"}
             [(dom/h5 {} "Restrictions")
              (for [restriction restrictions]
                (codify restriction))])))

(defn choices [section k]
  (when-let [choices (get-in model [section :model k :choices])]
    (let [choices (if-not (coll? choices) [choices] choices)]
      [(dom/strong #js {:className "inline-header"} "choices")
       (for [c choices]
         (r/badge {:class "onyx-badge"} (pr-str c)))])))

(defn added [section k]
  (when-let [added (get-in model [section :model k :added])]
    [(dom/strong #js {:className "inline-header"} "added")
     (r/badge {:class "onyx-badge"} added)]))

(defn unit [section k]
  (when-let [unit (get-in model [section :model k :unit])]
    [(dom/strong #js {:className "inline-header"} "unit")
     (r/badge {:class "onyx-badge"} (pr-str unit))]))

(defn default-value [section k]
  (when-let [v (get-in model [section :model k :default])]
    [(dom/strong #js {:className "inline-header"} "default")
     (r/badge {:class "onyx-badge"} (pr-str v))]))

(defn requirements [section k]
  (cond (= (get-in model [section :model k :optional?]) false)
        (r/badge {:class "required-badge onyx-badge"} "required")
        (get-in model [section :model k :required-when])
        (r/badge {:class "required-badge onyx-badge"} "sometimes required")))

(defn required-when [section k]
  (when-let [conditions (get-in model [section :model k :required-when])]
    (r/alert {:bs-style "warning"}
             [(dom/h5 {} "Required when")
              (for [c conditions]
                (codify c))])))

(defn optionally-allowed-when [section k]
  (when-let [conditions (get-in model [section :model k :optionally-allowed-when])]
    (r/alert {:bs-style "success"}
             [(dom/h5 {} "Optionally allowed when")
              (for [c conditions]
                (codify c))])))

(defn deprecated [section k]
  (when-let [deprecated-version (get-in model [section :model k :deprecated-version])]
    (r/alert {:bs-style "danger"}
             [(dom/h5 {} "Deprecated")
              (dom/p {} 
                     (dom/b {} "Version: ")
                     deprecated-version)
              (dom/p {} 
                     (codify (get-in model [section :model k :deprecation-doc])))])))

(defn keyword-sanitize-? [k]
  (if (namespace k)
    (keyword (clojure.string/replace (namespace k) #"\?" "-QMARK")
             (clojure.string/replace (name k) #"\?" "-QMARK")) 
    (keyword (clojure.string/replace (name k) #"\?" "-QMARK"))))

(defcomponent display-feature [{k :key section :section}]
  (render [_]
          (let [deprecated? (boolean (get-in model [section :model k :deprecated-version]))] 
            (p/panel
              {:key (name k)
               :id (str (keyword-sanitize-? k))}
              (dom/pre #js {:className "key-header"}
                       (dom/a #js {:href (str "#" (name section) "/" (keyword-sanitize-? k))}
                              (dom/i #js {:className "glyphicon glyphicon-link"}))
                       (str k)
                       (if deprecated? 
                         (r/badge {:class "deprecated-badge onyx-badge"} "deprecated"))
                       (requirements section k))
              (deprecated section k)
              (r/well {:class "entry-doc"} (codify (get-in model [section :model k :doc])))
              (when-let [doc-url (get-in model [section :model k :doc-url])] 
                (dom/a #js {:href doc-url :target "_blank"} (str "Documentation")))
              (dom/p {})
              (when-let [cheat-sheet-url (get-in model [section :model k :cheat-sheet-url])] 
                (dom/a #js {:href cheat-sheet-url} (str "Parameters")))


              (dom/p {})
              (restrictions model section k)
              (dom/p {})
              (required-when section k)
              (dom/p {})
              (optionally-allowed-when section k)

              (allowed-types section k)
              (dom/p {})

              (unit section k)
            (dom/p {})
            (default-value section k)
            (dom/p {})
            (choices section k)
            (dom/p {})
            (added section k)
            (dom/p {})))))

(defcomponent feature-view [{:keys [model sections]} owner]
  (render [_]
          (dom/div {}
                   (for [section sections] 
                     (into [(if-not (empty? (get-in model [section :model])) 
                              (p/panel 
                                {:id "summary" :bs-style "primary" :class "summary-doc" :header (dom/h3 nil (str (model-names section) " Summary"))}
                                (get-in model [section :summary])
                                (dom/p {})
                                (when-let [doc-url (get-in model [section :doc-url])] 
                                  (dom/a #js {:href doc-url} "Documentation"))))] 
                           (for [k (model-display-order section)]
                             (dom/div (if (get-in model [section :model k]) #js {} #js {:style #js {:display "none"}})
                                      (om/build display-feature {:section section :key k}))))))))

(defcomponent feature-options [{:keys [model section]} owner]
  (render [_]
          (r/well
            {}
            [(dom/h4 #js {:className "key-set"} "Keys")
             (for [k (model-display-order section)]
               [(dom/code #js {:className "code-example"} 
                          (dom/a #js {:href (str "#" (name section) "/" (keyword-sanitize-? k))} (str k)))
                (dom/p {})])])))

(defn pretty-edn [input]
  (clojure.string/replace (with-out-str (pprint input {:width 10})) #"\}\n\s\{" "}\n\n {"))

(defn catalog-examples-view []
  (for [k (sort (keys (get examples :catalog-entry)))]
    (p/panel
     {:id (str k)}
     (dom/pre
      {}
      (str k))
     (dom/p {})
     (r/well {:class "entry-doc"} (codify (get-in examples [:catalog-entry k :doc])))
     (dom/p {})
     (dom/code #js {:className "code-example"} (pretty-edn (get-in examples [:catalog-entry k :code])))
     (dom/p {})
     (restrictions examples :catalog-entry k))))

(defn catalog-entry-examples-options []
  (feature-options examples :catalog-entry))

(defn handle-change
  "Grab the input element via the `input` reference."
  [owner data]
  (let [node (om/get-node owner "input")]
    (when node
      (om/update! data :view (keys model))
      (om/update! data :search (.-value node)))))

(defcomponent search-input [data owner]
  (render [_]
          (i/input
            {:feedback? true
             :type "text"
             :value (:search data)
             :label "Search"
             :placeholder ""
             ;:group-classname "group-class"
             ;:wrapper-classname "wrapper-class"
             ;:label-classname "label-class"
             :on-change #(handle-change owner data)})))

(defn build-nav-item [model k name-str]
  (if-not (empty? (get-in model [k :model]))
    (n/nav-item {:key (name k) 
                 :href (str "#/" (name k)) 
                 :on-click #(swap! app-state assoc :view [k] :search "")} 
                name-str)))

(defn main []
  (om/root
   (fn [app owner]
     (reify
       om/IRender
       (render [_]
         (let [search (:search app)
               view (:view app)
               filtered-model (filter-model model search)]
           (g/grid
            {}
            (g/row {:id "search-bar"}
                   (g/col {:xs 18 :md 3})
                   (g/col {:xs 18 :md 6}
                          (om/build search-input app)))

            (g/row
             {:id "cheat-sheet-block"}
             (g/col
              {:xs 18 :md 3}
              (r/well
               {}
               (n/nav
                {:collapsible? true :stacked? true :bs-style "pills"}
                (dom/h4 #js {:className "section-set"} "Sections")
                (cons (n/nav-item {:key "all" :href "#/all" :on-click #(swap! app-state assoc :view (keys model) :search "")} "All")
                      (keep (fn [k]
                              (build-nav-item filtered-model k (get model-names k))) 
                            model-order)))))

             (g/col
              {:xs 18 :md 6}
              (if view 
                (om/build feature-view {:model filtered-model :sections view})
                (dom/div
                 #js {:id "logo-container"}
                 (dom/img #js {:src "https://raw.githubusercontent.com/onyx-platform/onyx/3bf02e6fafe41315e0302f0f525992eb76eca04e/resources/logo/high-res.png" :className "full-logo"})
                 (dom/h3 #js {:className "feature-choose"} "<< Choose a feature"))))

             (if (or (= 1 (count view))
                     (:search app))
               (g/col
                {:xs 18 :md 3}
                (om/build feature-options {:model model :section (first view)})))))))))
   app-state
   {:target (. js/document (getElementById "app"))}))
