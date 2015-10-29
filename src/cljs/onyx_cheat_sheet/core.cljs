(ns onyx-cheat-sheet.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-bootstrap.panel :as p]
            [om-bootstrap.button :as b]
            [om-bootstrap.random :as r]
            [om-bootstrap.nav :as n]
            [om-bootstrap.grid :as g]
            [onyx.information-model :refer [model]]
            [markdown.core]
            [fipp.edn :refer [pprint]]))

(enable-console-print!)

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

(defonce app-state (atom {}))

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

(defn feature-view [section]
  (into [(p/panel 
           {:id "summary" :bs-style "primary" :class "summary-doc" :header (dom/h3 nil "Summary")}
           (get-in model [section :summary]))] 
        (for [k (keys (get-in model [section :model]))]
          (p/panel
            {:id (str k)}
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
            (dom/p {})))))

(defn feature-options [usage section]
  (r/well
   {}
   [(dom/h4 #js {:className "key-set"} "Keys")
    (for [k (keys (get-in usage [section :model]))]
      [(dom/code #js {:className "code-example"} (dom/a #js {:href (str "#" k)} (str k)))
       (dom/p {})])]))

(defn catalog-entry-view []
  (feature-view :catalog-entry))

(defn catalog-entry-options []
  (feature-options model :catalog-entry))

(defn flow-conditions-entry-view []
  (feature-view :flow-conditions-entry))

(defn flow-condition-entry-options []
  (feature-options model :flow-conditions-entry))

(defn lifecycle-entry-view []
  (feature-view :lifecycle-entry))

(defn lifecycle-entry-options []
  (feature-options model :lifecycle-entry))

(defn lifecycle-calls-view []
  (feature-view :lifecycle-calls))

(defn lifecycle-calls-entry-options []
  (feature-options model :lifecycle-calls))

(defn window-entry-view []
  (feature-view :window-entry))

(defn window-entry-options []
  (feature-options model :window-entry))

(defn state-aggregation-entry-view []
  (feature-view :state-aggregation))

(defn state-aggregation-entry-options []
  (feature-options model :state-aggregation))

(defn trigger-entry-view []
  (feature-view :trigger-entry))

(defn trigger-entry-options []
  (feature-options model :trigger-entry))

(defn peer-config-view []
  (feature-view :peer-config))

(defn peer-config-options []
  (feature-options model :peer-config))

(defn env-config-view []
  (feature-view :env-config))

(defn env-config-options []
  (feature-options model :env-config))

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
             {:class "cheat-sheet-header"}
             (g/col
              {:xs 18 :md 12}
              (dom/h2 #js {:id "page-title" :onClick (fn [e] (swap! app-state dissoc :view))} "Onyx Cheat Sheet")))
            (g/row
             {}
             (g/col
              {:xs 18 :md 3}
              (r/well
               {}
               (n/nav
                {:collapsible? true :stacked? true :bs-style "pills"}
                (dom/h4 #js {:className "section-set"} "Sections")
                (n/nav-item {:key 1 :href "#" :on-click #(swap! app-state assoc :view :catalog)} "Catalogs")
                (n/nav-item {:key 2 :href "#" :on-click #(swap! app-state assoc :view :flow-conditions)} "Flow Conditions")
                (n/nav-item {:key 3 :href "#" :on-click #(swap! app-state assoc :view :lifecycles)} "Lifecycles")
                (n/nav-item {:key 4 :href "#" :on-click #(swap! app-state assoc :view :lifecycle-calls)} "Lifecycle Calls")
                (n/nav-item {:key 5 :href "#" :on-click #(swap! app-state assoc :view :windows)} "Windows")
                (n/nav-item {:key 6 :href "#" :on-click #(swap! app-state assoc :view :state-aggregation)} "State / Aggregation")
                (n/nav-item {:key 7 :href "#" :on-click #(swap! app-state assoc :view :triggers)} "Triggers")
                (n/nav-item {:key 8 :href "#" :on-click #(swap! app-state assoc :view :peer-config)} "Peer Configuration")
                (n/nav-item {:key 8 :href "#" :on-click #(swap! app-state assoc :view :env-config)} "Environment Configuration"))))
             (g/col
              {:xs 18 :md 6}
              (cond (= view :catalog)
                    (catalog-entry-view)
                    (= view :flow-conditions)
                    (flow-conditions-entry-view)
                    (= view :lifecycles)
                    (lifecycle-entry-view)
                    (= view :lifecycle-calls)
                    (lifecycle-calls-view)
                    (= view :windows)
                    (window-entry-view)
                    (= view :state-aggregation)
                    (state-aggregation-entry-view)
                    (= view :triggers)
                    (trigger-entry-view)
                    (= view :peer-config)
                    (peer-config-view)
                    (= view :env-config)
                    (env-config-view)
                    :else
                    (dom/div
                     #js {:id "logo-container"}
                     (dom/img #js {:src "https://raw.githubusercontent.com/onyx-platform/onyx/3bf02e6fafe41315e0302f0f525992eb76eca04e/resources/logo/high-res.png" :className "full-logo"})
                     (dom/h3 #js {:className "feature-choose"} "<< Choose a feature"))))
             (g/col
              {:xs 18 :md 3}
              (cond (= view :catalog)
                    (catalog-entry-options)
                    (= view :flow-conditions)
                    (flow-condition-entry-options)
                    (= view :lifecycles)
                    (lifecycle-entry-options)
                    (= view :lifecycle-calls)
                    (lifecycle-calls-entry-options)
                    (= view :windows)
                    (window-entry-options)
                    (= view :state-aggregation)
                    (state-aggregation-entry-options)
                    (= view :triggers)
                    (trigger-entry-options)
                    (= view :peer-config)
                    (peer-config-options)
                    (= view :env-config)
                    (env-config-options)))))))))
   app-state
   {:target (. js/document (getElementById "app"))}))
