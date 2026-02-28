/*
 Hubitat WeatherSense App (converted from HA config flow).

 Original: ha-weathersense/config_flow.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

// ========== INLINE CONSTANTS ==========
final Map WEATHERSENSE_CONST = [
    COMFORT_EXTREME_COLD: "extreme_cold",
    COMFORT_VERY_COLD: "very_cold",
    COMFORT_COLD: "cold",
    COMFORT_COOL: "cool",
    COMFORT_SLIGHTLY_COOL: "slightly_cool",
    COMFORT_COMFORTABLE: "comfortable",
    COMFORT_SLIGHTLY_WARM: "slightly_warm",
    COMFORT_WARM: "warm",
    COMFORT_HOT: "hot",
    COMFORT_VERY_HOT: "very_hot",
    COMFORT_EXTREME_HOT: "extreme_hot",

    COMFORT_DESCRIPTIONS: [
        "extreme_cold":  "Extreme Cold Stress",
        "very_cold":     "Very Strong Cold Stress",
        "cold":          "Strong Cold Stress",
        "cool":          "Moderate Cold Stress",
        "slightly_cool": "Slight Cold Stress",
        "comfortable":   "No Thermal Stress (Comfort)",
        "slightly_warm": "Slight Heat Stress",
        "warm":          "Moderate Heat Stress",
        "hot":           "Strong Heat Stress",
        "very_hot":      "Very Strong Heat Stress",
        "extreme_hot":   "Extreme Heat Stress"
    ],

    ATTR_COMFORT_LEVEL:             "comfortLevel",
    ATTR_COMFORT_DESCRIPTION:       "comfortDescription",
    ATTR_CALCULATION_METHOD:        "calculationMethod",
    ATTR_IS_COMFORTABLE:            "isComfortable",
    ATTR_WIND_DIRECTION_CORRECTION: "windDirectionCorrection"
]

// ========== DEFINITION ==========
definition(
    name:        "WeatherSense",
    namespace:   "weathersense",
    author:      "SMKRV",
    description: "HA WeatherSense port for Hubitat",
    category:    "Convenience",
    iconUrl:     "",
    iconX2Url:   ""
)

// ========== PREFERENCES ==========
preferences {
    page(name: "mainPage")
    page(name: "sensorPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "WeatherSense Setup", install: true, uninstall: true) {
        section("Basic Settings") {
            input "appName",    "text", title: "App Name", defaultValue: "Feels Like"
            input "isOutdoor",  "bool", title: "Outdoor?", defaultValue: true
        }
        // FIXED: href must be inside a section block
        section("Pages") {
            href "sensorPage", title: "Configure Sensors ->"
        }
        section("Display") {
            input "displayUnit", "enum", title: "Temperature Unit",
                  options: [["C": "Celsius (degC)"], ["F": "Fahrenheit (degF)"]], defaultValue: "C"
        }
    }
}

def sensorPage() {
    dynamicPage(name: "sensorPage", title: "Sensors", install: true) {
        section("Required") {
            input "temperatureSensor", "capability.temperatureMeasurement",      title: "Temperature", required: true
            input "humiditySensor",    "capability.relativeHumidityMeasurement", title: "Humidity",    required: true
        }
        section("Optional Weather") {
            // FIXED: use capability.sensor for generic numeric sensors
            input "windSpeedSensor",     "capability.sensor", title: "Wind Speed (m/s)"
            input "pressureSensor",      "capability.sensor", title: "Pressure (kPa)"
            input "windDirectionSensor", "capability.sensor", title: "Wind Direction (degrees)"
        }
        section("Advanced") {
            input "enableWindDirectionCorrection", "bool",    title: "Wind Direction Correction", defaultValue: false
            input "enableSmoothing",               "bool",    title: "Smoothing",                 defaultValue: false
            input "smoothingFactor",               "decimal", title: "Smoothing Factor",          range: "0.05..0.95", defaultValue: 0.3
        }
    }
}

// ========== LIFECYCLE ==========
def installed() {
    state.version = "1.0.0"
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def uninstalled() {
    unsubscribe()
    if (state.childDeviceId) {
        deleteChildDevice(state.childDeviceId)
    }
}

// ========== INITIALIZATION ==========
def initialize() {
    subscribe(temperatureSensor, "temperature", sensorHandler)
    subscribe(humiditySensor,    "humidity",    sensorHandler)
    if (windSpeedSensor)     subscribe(windSpeedSensor,     "illuminance", sensorHandler)
    if (pressureSensor)      subscribe(pressureSensor,      "illuminance", sensorHandler)
    if (windDirectionSensor) subscribe(windDirectionSensor, "illuminance", sensorHandler)

    def childDni = "${app.id}-weathersense"
    try {
        addChildDevice("weathersense", "WeatherSense Virtual", childDni, [name: "${app.label} Sensor"])
        state.childDeviceId = childDni
    } catch (e) {
        log.warn "Child device already exists: ${e.message}"
    }

    calculateAndUpdate()
}

// ========== EVENT HANDLER ==========
def sensorHandler(evt) {
    // FIXED: replaced Unicode arrow -> with plain ASCII ->
    log.debug "${evt.device.displayName} ${evt.name} -> ${evt.value}"
    runIn(1, "calculateAndUpdate")
}

// ========== CORE LOGIC ==========
def calculateAndUpdate() {
    def inputs = [
        temperature:                    safeValue(temperatureSensor, "temperature"),
        humidity:                       safeValue(humiditySensor,    "humidity"),
        windSpeed:                      safeValue(windSpeedSensor,   "illuminance"),
        pressure:                       safeValue(pressureSensor,    "illuminance"),
        windDirection:                  safeValue(windDirectionSensor, "illuminance"),
        isOutdoor:                      isOutdoor ?: true,
        timeOfDay:                      new Date(),
        // FIXED: cast latitude to Double - state cannot serialise BigDecimal
        latitude:                       location.latitude as Double,
        enableWindDirectionCorrection:  enableWindDirectionCorrection ?: false
    ]

    if (inputs.temperature == null || inputs.humidity == null) {
        log.warn "WeatherSense: Missing required sensor values"
        return
    }

    def result = calculateFeelsLike(
        inputs.temperature,
        inputs.humidity,
        inputs.windSpeed,
        inputs.pressure,
        inputs.isOutdoor,
        inputs.timeOfDay,
        0,  // FIXED: removed 'G' suffix; plain 0 is safe
        inputs.windDirection,
        inputs.latitude != null ? new BigDecimal(inputs.latitude.toString()) : null,
        inputs.enableWindDirectionCorrection
    )

    if (result?.feelsLike == null) {
        log.warn "WeatherSense: Feels-like calculation returned null"
        return
    }

    if (result.outOfRange) {
        log.warn "WeatherSense: Feels-like (${result.feelsLike}C) is unusually far from actual (${inputs.temperature}C)"
    }

    BigDecimal displayValue = convertUnit(result.feelsLike)

    // FIXED: safely convert state.smoothedValue (stored as Double) back to BigDecimal
    if (enableSmoothing && state.smoothedValue != null) {
        BigDecimal alpha = smoothingFactor ? new BigDecimal(smoothingFactor.toString()) : new BigDecimal("0.3")
        BigDecimal prevSmoothed = new BigDecimal(state.smoothedValue.toString())
        displayValue = alpha * displayValue + (1 - alpha) * prevSmoothed
    }

    // FIXED: store as Double for state serialisation
    state.smoothedValue = displayValue.doubleValue()
    state.lastResult    = result
    // FIXED: store only serialisable primitives in state
    state.lastInputTemp    = inputs.temperature?.doubleValue()
    state.lastInputHumidity = inputs.humidity?.doubleValue()

    updateChildDevice(result, displayValue)
}

def safeValue(device, String attr) {
    def v = device?.currentValue(attr)
    if (v == null) return null
    try {
        return new BigDecimal(v.toString())
    } catch (e) {
        log.warn "WeatherSense: Invalid numeric value for ${attr}: ${v}"
        return null
    }
}

def convertUnit(BigDecimal celsius) {
    if (displayUnit == "F") {
        return (celsius * 9 / 5) + 32
    }
    return celsius
}

def updateChildDevice(result, BigDecimal displayValue) {
    def child = getChildDevice(state.childDeviceId)
    if (!child) {
        log.warn "WeatherSense: Child device not found"
        return
    }

    def C = WEATHERSENSE_CONST

    child.sendEvent(name: C.ATTR_COMFORT_LEVEL,             value: result.comfortLevel)
    child.sendEvent(name: C.ATTR_COMFORT_DESCRIPTION,       value: C.COMFORT_DESCRIPTIONS[result.comfortLevel])
    child.sendEvent(name: C.ATTR_CALCULATION_METHOD,        value: result.method)
    child.sendEvent(name: C.ATTR_IS_COMFORTABLE,            value: isComfortable(result.comfortLevel))
    child.sendEvent(name: C.ATTR_WIND_DIRECTION_CORRECTION, value: result.windDirectionCorrection)

    // FIXED: use stored state primitives instead of inputs map (not in scope here)
    child.sendEvent(name: "inputTemp",     value: state.lastInputTemp?.round(1))
    child.sendEvent(name: "inputHumidity", value: state.lastInputHumidity?.round(1))
    child.sendEvent(name: "feelsLike",     value: displayValue.setScale(1, BigDecimal.ROUND_HALF_UP))
}

def isComfortable(String level) {
    return level in ["comfortable", "slightly_warm", "slightly_cool"]
}

// ========== DASHBOARD ==========
def getDashboardStatus() {
    if (!state.lastResult) return "Waiting for data..."
    def unit = displayUnit == "F" ? "degF" : "degC"
    // FIXED: use state.smoothedValue (Double) safely
    def rounded = state.smoothedValue != null ? Math.round(state.smoothedValue * 10) / 10.0 : "?"
    return "${rounded} ${unit} (${state.lastResult.comfortLevel})"
}
