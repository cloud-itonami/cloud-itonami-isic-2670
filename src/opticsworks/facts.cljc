(ns opticsworks.facts
  "Optical-instrument/photographic-equipment optical-standard evidence
  catalog -- the G2-style spec-basis table the Module-Seating Governor
  checks every `:optical-standard-rules/verify` proposal against.

  Like `moldworks.facts`'s injection-molded-plastics material-spec
  catalog (the SAME honest structural observation), optical-module
  conformance does not decompose into one scheme per ISO3 country: it
  is a mix of named engineering-standards-body specifications (ISO/IEC/
  SAE) organized by PRODUCT CLASS (what the optical module becomes)
  rather than by the plant's own country. This catalog's keys reflect
  that real structure honestly rather than forcing a false per-country
  shape (UNLIKE `commsdevice.facts`'s own per-ISO3-country radio-
  type-approval table, whose subject -- a radio TRANSMITTER -- really
  is regulated per-country):

    - \"SMARTPHONE-CAMERA\" -- smartphone camera-module optics.
      ISO 12233 (Photography -- Electronic still-picture imaging --
      Resolution and spatial frequency responses) is the real,
      standard resolution/MTF test method for camera-module
      image-quality QA; IEC 60825-1 (Safety of laser products --
      Equipment classification and requirements) is applicable because
      modern smartphone camera modules commonly integrate a laser-based
      autofocus/Time-of-Flight (ToF) depth-sensing emitter -- itself a
      laser-product subassembly requiring its own laser-safety
      classification, distinct from the camera module's own optical
      resolution conformance.
    - \"AUTOMOTIVE-ADAS\" -- automotive optical/ADAS sensor modules
      (backup cameras, ADAS-grade camera/LIDAR housings). ISO 26262
      (Road vehicles -- Functional safety, all parts) is the real,
      widely-referenced functional-safety framework for qualifying
      ADAS-grade camera/LIDAR sensor-module suppliers; ISO 20653 (Road
      vehicles -- Degrees of protection (IP code)) defines the IP6K9K
      ingress-protection rating (dust-tight + high-pressure/
      high-temperature water-jet washdown resistance) commonly required
      for exterior-mounted automotive camera/optical-sensor housings.

  HONEST CONFIDENCE DISCLOSURE on SAE J3088: this catalog also lists
  SAE J3088 under AUTOMOTIVE-ADAS as a PLAUSIBLE ADAS/camera-
  calibration-adjacent SAE reference, but this session is NOT
  confident that J3088's exact current title/scope is precisely
  'ADAS camera calibration' -- SAE's J-number catalogue is large, and
  this session could not verify J3088's exact scope without live web
  access. Rather than presenting an unverified document number as a
  certain citation, it is disclosed under `:supplementary-citations-
  unconfident` with an explicit LOW-confidence flag, and is
  deliberately held OUT of `:required-evidence` below, so
  `required-evidence-satisfied?` (the governor's hard evidence gate)
  NEVER depends on an unverified citation. This is the SAME 'never
  fabricate precision' discipline `moldworks.robotics`'s cavity-
  pressure-factor confidence disclosure and `commsdevice.robotics`'s
  bonding-pressure-band disclosure use for numeric engineering
  estimates, extended here to a citation whose exact scope this
  session could not verify.

  HONEST CONFIDENCE DISCLOSURE on provenance URLs: `:provenance` below
  cites each standard's owning body and (for ISO) its technical
  committee, but deliberately does NOT include a specific numbered
  ISO/IEC catalogue-page URL or edition year, since this session could
  not verify exact current-edition catalogue numbers without live web
  access -- a fabricated-looking but unverified specific URL would be
  LESS honest than a correct, general standards-body reference.
  Implementers should confirm the current edition via the standards
  body's own catalogue before relying on this catalog for compliance.

  Coverage is reported HONESTLY: a product class not in this table has
  NO spec-basis. Seed values cite official standards-owning bodies;
  this is a starting catalog (two product classes: the smartphone and
  automotive-ADAS downstream consumers this actor's own README `Scope
  note` names), not a survey of every product class an optical-module
  manufacturer might produce (e.g. medical-imaging optics or
  industrial machine-vision optics, which have their own distinct
  regulatory frameworks this catalog does NOT cite, honestly, rather
  than fabricating a citation this session was not confident of).")

(def catalog
  {"SMARTPHONE-CAMERA"
   {:name "Smartphone camera-module optics (ISO resolution + IEC laser-safety basis)"
    :owner-authority "International Organization for Standardization (ISO, ISO/TC 42 Photography) / International Electrotechnical Commission (IEC, IEC TC 76 Optical radiation safety and laser equipment)"
    :legal-basis "ISO 12233 (Photography -- Electronic still-picture imaging -- Resolution and spatial frequency responses) -- the standard resolution/MTF test method for camera-module image-quality QA; IEC 60825-1 (Safety of laser products -- Equipment classification and requirements) -- applicable because modern smartphone camera modules commonly integrate a laser-based autofocus/Time-of-Flight (ToF) depth-sensing emitter, itself a laser-product subassembly requiring its own laser-safety classification"
    :national-spec "ISO 12233 resolution/MTF conformance for the assembled camera module + IEC 60825-1 laser-safety classification for the integrated autofocus/ToF laser emitter"
    :provenance "https://www.iso.org/ (ISO 12233, ISO/TC 42 Photography) ; https://www.iec.ch/ (IEC 60825-1, IEC TC 76) -- standards-body/committee reference only, not a specific catalogue-page URL/edition year (see ns docstring honest-precision disclosure)"
    :required-evidence ["ISO 12233 resolution/MTF test report"
                        "IEC 60825-1 laser/optical-radiation safety classification report (autofocus/ToF laser emitter)"
                        "Lens-element/coating material traceability record"]}
   "AUTOMOTIVE-ADAS"
   {:name "Automotive ADAS optical/camera sensor modules (ISO functional-safety + ingress-protection basis)"
    :owner-authority "International Organization for Standardization (ISO, ISO/TC 22 Road vehicles)"
    :legal-basis "ISO 26262 (Road vehicles -- Functional safety, all parts) -- the standard functional-safety framework commonly referenced for qualifying ADAS-grade camera/LIDAR sensor modules and their supplier development process; ISO 20653 (Road vehicles -- Degrees of protection (IP code) -- Protection of electrical equipment against foreign objects, water and access) -- defines the IP6K9K ingress-protection rating (dust-tight + high-pressure/high-temperature water-jet washdown resistance) commonly required for exterior-mounted automotive camera/optical-sensor housings"
    :national-spec "ISO 26262 functional-safety work-product/assessment conformance (sensor-module supplier scope) + ISO 20653 IP6K9K ingress-protection conformance for exterior camera/optical-sensor housings"
    :provenance "https://www.iso.org/ (ISO 26262 series, ISO/TC 22/SC 32 Electrical and electronic components and general system aspects) ; https://www.iso.org/ (ISO 20653, ISO/TC 22) -- standards-body/committee reference only, same honest-precision disclosure as SMARTPHONE-CAMERA above"
    :required-evidence ["ISO 26262 functional-safety work-product/assessment report (sensor-module supplier scope)"
                        "ISO 20653 IP6K9K ingress-protection test report (dust + high-pressure/high-temperature water-jet washdown)"]
    :supplementary-citations-unconfident
    ["SAE J3088 -- cited as a PLAUSIBLE ADAS/camera-calibration-adjacent SAE reference. This session is NOT confident this exact document number's current title/scope is precisely 'ADAS camera calibration' (SAE's J-number catalogue is large and this session could not verify J3088's exact scope without live web access). Disclosed HONESTLY as an unconfident citation and deliberately held OUT of :required-evidence above, so the governor's evidence-checklist gate never depends on an unverified citation -- see ns docstring."]}})

(defn spec-basis [scheme] (get catalog scheme))

(defn coverage
  ([] (coverage (keys catalog)))
  ([schemes]
   (let [have (filter catalog schemes)
         missing (remove catalog schemes)]
     {:requested (count schemes)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2670 R0: " (count catalog)
                 " product-class optical-standard schemes seeded "
                 "(SMARTPHONE-CAMERA: ISO 12233 + IEC 60825-1 / "
                 "AUTOMOTIVE-ADAS: ISO 26262 + ISO 20653/IP6K9K, "
                 "SAE J3088 disclosed as an unconfident supplementary "
                 "citation only). Extend `opticsworks.facts/catalog`, "
                 "never fabricate a product class's requirements or "
                 "present an unverified citation as certain.")})))

(defn required-evidence-satisfied?
  [scheme submitted]
  (when-let [{:keys [required-evidence]} (spec-basis scheme)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [scheme]
  (:required-evidence (spec-basis scheme) []))
