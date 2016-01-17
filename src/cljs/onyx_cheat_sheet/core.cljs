(ns onyx-cheat-sheet.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-bootstrap.panel :as p]
            [om-bootstrap.button :as b]
            [om-bootstrap.random :as r]
            [om-bootstrap.nav :as n]
            [om-bootstrap.grid :as g]
            [onyx.information-model :refer [model model-display-order]]
            [markdown.core]
            [fipp.edn :refer [pprint]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(defonce app-state (atom {}))

(secretary/set-config! :prefix "#")

(defroute home-path "/" []
  (swap! app-state assoc :view nil))

(defroute calls-path "/:calls" [calls]
  (swap! app-state assoc :view (keyword calls)))

(defroute calls-jump "/:calls/:namespace/:name" [calls namespace name]
  (swap! app-state assoc :view (keyword calls))
  ;; Super hacky way to jump to the correct entry
  (.setTimeout js/window 
               #(let [element (.getElementById js/document (str namespace "/" name))]
                  (.scrollIntoView element)) 
               200))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))

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
  (om.dom/div #js  {:dangerouslySetInnerHTML #js {:__html (markdown.core/md->html text)}} nil))

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
                (dom/li {} (codify restriction)))])))

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
                (dom/li {} (codify c)))])))

(defn optionally-allowed-when [section k]
  (when-let [conditions (get-in model [section :model k :optionally-allowed-when])]
    (r/alert {:bs-style "success"}
             [(dom/h5 {} "Optionally allowed when")
              (for [c conditions]
                (dom/li {} (codify c)))])))

(defn keyword-sanitize-? [k]
  (keyword (clojure.string/replace (namespace k) #"\?" "-QMARK")
           (clojure.string/replace (name k) #"\?" "-QMARK")))

(defn feature-view [section]
  (into [(p/panel 
           {:id "summary" :bs-style "primary" :class "summary-doc" :header (dom/h3 nil "Summary")}
           (get-in model [section :summary]))] 
        (for [k (model-display-order section)]
          (p/panel
            {:id (str (keyword-sanitize-? k))}
            (dom/pre #js {:className "key-header"}
                     (str k)
                     (requirements section k))
            (r/well {:class "entry-doc"} (codify (get-in model [section :model k :doc])))
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

(defn feature-options [usage section]
  (r/well
   {}
   [(dom/h4 #js {:className "key-set"} "Keys")
    (for [k (model-display-order section)]
      [(dom/code #js {:className "code-example"} (dom/a #js {:href (str "#" (name section) "/" (keyword-sanitize-? k))} (str k)))
       (dom/p {})])]))

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

(defn main []
  (om/root
   (fn [app owner]
     (reify
       om/IRender
       (render [_]
         (let [view (:view @app-state)]
           (g/grid
            {}
            (g/row
             {:id "cheat-sheet-block"}
             (g/col
              {:xs 18 :md 3}
              (r/well
               {}
               (n/nav
                {:collapsible? true :stacked? true :bs-style "pills"}
                (dom/h4 #js {:className "section-set"} "Sections")
                (n/nav-item {:key 1 :href "#/catalog-entry" :on-click #(swap! app-state assoc :view :catalog-entry)} "Catalogs")
                (n/nav-item {:key 2 :href "#/flow-conditions-entry" :on-click #(swap! app-state assoc :view :flow-conditions-entry)} "Flow Conditions")
                (n/nav-item {:key 3 :href "#/lifecycle-entry" :on-click #(swap! app-state assoc :view :lifecycle-entry)} "Lifecycles")
                (n/nav-item {:key 4 :href "#/lifecycle-calls" :on-click #(swap! app-state assoc :view :lifecycle-calls)} "Lifecycle Calls")
                (n/nav-item {:key 5 :href "#/window-entry" :on-click #(swap! app-state assoc :view :window-entry)} "Windows")
                (n/nav-item {:key 6 :href "#/state-aggregation" :on-click #(swap! app-state assoc :view :state-aggregation)} "State / Aggregation")
                (n/nav-item {:key 7 :href "#/trigger-entry" :on-click #(swap! app-state assoc :view :trigger-entry)} "Triggers")
                (n/nav-item {:key 8 :href "#/peer-config" :on-click #(swap! app-state assoc :view :peer-config)} "Peer Configuration")
                (n/nav-item {:key 9 :href "#/env-config" :on-click #(swap! app-state assoc :view :env-config)} "Environment Configuration"))))
             (g/col
              {:xs 18 :md 6}
              (if view 
                (feature-view view)
                (dom/div
                  #js {:id "logo-container"}
                  (dom/img #js {:src "https://raw.githubusercontent.com/onyx-platform/onyx/3bf02e6fafe41315e0302f0f525992eb76eca04e/resources/logo/high-res.png" :className "full-logo"})
                  (dom/h3 #js {:className "feature-choose"} "<< Choose a feature"))))
             (if view 
               (g/col
                 {:xs 18 :md 3}
                 (feature-options model view)))))))))
   app-state
   {:target (. js/document (getElementById "app"))}))
