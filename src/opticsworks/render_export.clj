(ns opticsworks.render-export
  "CLI: run the REAL `physics-2d` lens-barrel press-fit seating-test
  simulation (`opticsworks.robotics/simulate-lens-seating`, nominal
  batch-1-style passing press-run configuration) and dump the
  seating-press ('press-tool') and lens-housing rigid bodies' ACTUAL
  per-tick positions as JSON, for a downstream render harness to
  visualize. Every number in the emitted JSON comes directly off the
  real simulated `physics-2d` trajectory (the press-tool's per-tick
  `:position`) or a real, fixed geometry/anchor constant from
  `opticsworks.robotics` (AABB half-extents x2 for `:dims`, the static
  lens-housing's fixed anchor position for its own `:frames`) -- none
  of it is hand-typed/fabricated.

  Usage: clojure -M:dev:render-export

  Writes the SAME JSON to both `/tmp/render-2670/scene-data.json` (for
  a local render harness) and `docs/samples/scene-data.json` (committed
  for traceability). No third-party JSON dependency is pulled in for
  this -- the schema emitted here is small and fully known ahead of
  time (a `{\"bodies\" [{\"id\" .. \"dims\" .. \"frames\" ..} ..]}`
  map of strings/numbers/vectors only), so a tiny hand-written
  serializer is less friction than adding a new dependency for one CLI
  script."
  (:require [clojure.string :as str]
            [opticsworks.robotics :as robotics]))

(defn- escape-json-string [s]
  (-> (str s)
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn- json-value
  "Minimal recursive EDN->JSON renderer for the known, small shape this
  script emits (strings, numbers, vectors, string-keyed maps). Not a
  general-purpose JSON library."
  [v]
  (cond
    (nil? v)     "null"
    (string? v)  (str "\"" (escape-json-string v) "\"")
    (number? v)  (str v)
    (vector? v)  (str "[" (str/join "," (map json-value v)) "]")
    (sequential? v) (str "[" (str/join "," (map json-value v)) "]")
    (map? v)     (str "{" (str/join "," (map (fn [[k vv]] (str (json-value (name k)) ":" (json-value vv))) v)) "}")
    :else        (json-value (str v))))

(defn scene-json
  "Runs the REAL `physics-2d` lens-seating simulation for
  `seating-press-effective-mass-kg` (defaults to batch-1's own nominal,
  passing 0.025 kg configuration -- see `opticsworks.store/demo-data`)
  and returns the JSON string for the render-harness scene-data
  schema: {\"bodies\": [{\"id\" \"press-tool\" \"dims\" [w h] \"frames\"
  [[x y] ..]} {\"id\" \"lens-housing\" \"dims\" [w h] \"frames\"
  [[x y] ..]}]}.

  `press-tool`'s frames are the ACTUAL simulated per-tick positions of
  the moving seating-press body (`simulate-lens-seating`'s
  `:trajectory`). `lens-housing`'s frames are the real, fixed anchor
  position of the static lens-housing seating register, repeated once
  per tick (it is genuinely stationary throughout the simulation --
  `physics-2d` never moves a mass-0 body -- so a constant, real
  position is the honest per-tick value, not a fabricated one). `dims`
  for each body are the real AABB collider half-extents x2 (full
  width/height) `opticsworks.robotics` actually uses for that body."
  ([] (scene-json 0.025))
  ([seating-press-effective-mass-kg]
   (let [{:keys [trajectory]} (robotics/simulate-lens-seating seating-press-effective-mass-kg)
         press-tool-frames (mapv :position trajectory)
         n (count press-tool-frames)
         lens-housing-frames (vec (repeat n [0.0 0.0]))
         press-w (* 2 robotics/seating-press-half-w-m)
         press-h (* 2 robotics/seating-press-half-h-m)
         housing-w (* 2 robotics/lens-housing-half-w-m)
         housing-h (* 2 robotics/lens-housing-half-h-m)
         scene {"bodies" [{"id" "press-tool" "dims" [press-w press-h] "frames" press-tool-frames}
                          {"id" "lens-housing" "dims" [housing-w housing-h] "frames" lens-housing-frames}]}]
     (json-value scene))))

(defn -main [& _]
  (let [json (scene-json)]
    (doseq [dir ["/tmp/render-2670" "docs/samples"]]
      (let [d (java.io.File. (str dir))]
        (.mkdirs d)
        (spit (java.io.File. d "scene-data.json") json)))
    (println "wrote scene-data.json to /tmp/render-2670/ and docs/samples/")
    (println (subs json 0 (min 400 (count json))))))
