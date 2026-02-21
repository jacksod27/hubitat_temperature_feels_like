/*
 Hubitat WeatherSense App (converted from HA config flow).

 Original: ha-weathersense/config_flow.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV  
 Source:   https://github.com/smkrv/ha-weathersense
*/

// ========== INLINE CONSTANTS (WeatherSenseConst replacement) ==========
static final Map WEATHERSENSE_CONST = [
    // Comfort levels
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

    // Comfort descriptions
    COMFORT_DESCRIPTIONS: [
        "extreme_cold": "Extreme Cold Stress",
        "very_cold": "Very Strong Cold Stress",
        "cold": "Strong Cold Stress",
        "cool": "Moderate Cold Stress",
        "slightly_cool": "Slight Cold Stress",
        "comfortable": "No Thermal Stress (Comfort)",
        "slightly_warm": "Slight Heat Stress",
        "warm": "Moderate Heat Stress",
        "hot": "Strong Heat Stress",
        "very_hot": "Very Strong Heat Stress",
        "extreme_hot": "Extreme Heat Stress"
    ],

    // Attribute names
    ATTR_COMFORT_LEVEL: "comfortLevel",
    ATTR_COMFORT_DESCRIPTION: "comfortDescription",
    ATTR_CALCULATION_METHOD: "calculationMethod",
    ATTR_IS_COMFORTABLE: "isComfortable",
    ATTR_WIND_DIRECTION_CORRECTION: "windDirectionCorrection"
]

// ========== DEFINITION ==========
definition(
    name: "WeatherSense", 
    namespace: "weathersense",
    description: "HA WeatherSense port for Hubitat",
    category: "Convenience"
)

// ========== PREFERENCES ==========
preferences {
    page(name: "mainPage")
    page(name: "sensorPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "WeatherSense Setup", install: true, uninstall: true) {
        section("Basic Settings") {
            input "appName", "text", title: "App Name", defaultValue: "Feels Like"
            input "isOutdoor", "bool", title: "Outdoor?", defaultValue: true
        }
        href "sensorPage", title: "Configure Sensors →"
        section("Display") {
            input "displayUnit", "enum", title: "Temperature Unit", 
                  options: [["C": "°C"], ["F": "°F"]], defaultValue: "C"
        }
    }
}

def sensorPage() {
    dynamicPage(name: "sensorPage", title: "Sensors", install: true) {
        section("Required") {
            input "temperatureSensor", "capability.temperatureMeasurement", title: "Temperature", required: true
            input "humiditySensor", "capability.relativeHumidityMeasurement", title: "Humidity", required: true
        }
        section("Optional Weather") {
            input "windSpeedSensor", "capability.illuminanceMeasurement", title: "Wind Speed (m/s)"
            input "pressureSensor", "capability.illuminanceMeasurement", title: "Pressure (kPa)"
            input "windDirectionSensor", "capability.illuminanceMeasurement", title: "Wind Direction (°)"
        }
        section("Advanced") {
            input "enableWindDirectionCorrection", "bool", title: "Wind Direction Correction", defaultValue: false
            input "enableSmoothing", "bool", title: "Smoothing", defaultValue: false
            input "smoothingFactor", "decimal", title: "Smoothing Factor", range: "0.05..0.95", defaultValue: 0.3
        }
    }
}

// ========== LIFECYCLE ==========
def installed() {
    state.version = "1.0.0"
    initialize()
}

def updated() {  // Fixed: no 'settings' param
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
    // Subscribe to changes
    subscribe(temperatureSensor, "temperature", sensorHandler)
    subscribe(humiditySensor, "humidity", sensorHandler)
    if (windSpeedSensor) subscribe(windSpeedSensor, "illuminance", sensorHandler)
    if (pressureSensor) subscribe(pressureSensor, "illuminance", sensorHandler)
    if (windDirectionSensor) subscribe(windDirectionSensor, "illuminance", sensorHandler)
    
    // Create child sensor
    def childDni = "${app.id}-weathersense"
    try {
        def child = addChildDevice("weathersense", "WeatherSense Virtual", childDni, [name: "${app.label} Sensor"])
        state.childDeviceId = childDni
    } catch (e) {
        log.warn "Child device exists: $e"
    }
    
    calculateAndUpdate()  // Initial run
}

// ========== EVENT HANDLER ==========
def sensorHandler(evt) {
    log.debug "${evt.device.displayName} ${evt.name} → ${evt.value}"
    runIn(1, calculateAndUpdate)  // Fixed: method reference
}

// ========== CORE LOGIC ==========
def calculateAndUpdate() {
    def inputs = [
        temperature: safeValue(temperatureSensor, "temperature"),
        humidity: safeValue(humiditySensor, "humidity"),
        windSpeed: safeValue(windSpeedSensor, "illuminance"),
        pressure: safeValue(pressureSensor, "illuminance"),
        windDirection: safeValue(windDirectionSensor, "illuminance"),
        isOutdoor: isOutdoor ?: true,
        timeOfDay: new Date(),
        latitude: location.latitude,
        enableWindDirectionCorrection: enableWindDirectionCorrection ?: false
    ]
    
    if (inputs.temperature == null || inputs.humidity == null) {
        log.warn "Missing required sensors"
        return
    }
    
    // FIXED: Call inline method directly (no WeatherSenseCalculator class)
    def result = calculateFeelsLike(
        inputs.temperature,
        inputs.humidity,
        inputs.windSpeed,
        inputs.pressure,
        inputs.isOutdoor,
        inputs.timeOfDay,
        0G, // cloudiness
        inputs.windDirection,
        inputs.latitude,
        inputs.enableWindDirectionCorrection
    )

    if (result?.feelsLike == null) {
        log.warn "Feels-like calculation returned null"
        return
    }

    if (result.outOfRange) {
        log.warn "Feels-like value (${result.feelsLike}°C) is unusually far from actual temperature (${inputs.temperature}°C)"
    }
 
    // Smoothing - FIXED: null-safe alpha
    BigDecimal displayValue = convertUnit(result.feelsLike)
    if (enableSmoothing && state.smoothedValue != null) {
        BigDecimal alpha = smoothingFactor ? new BigDecimal(smoothingFactor.toString()) : 0.3G
        displayValue = alpha * displayValue + (1G - alpha) * state.smoothedValue
    }
    state.smoothedValue = displayValue

    state.lastResult = result
    state.lastInputs = inputs

    updateChildDevice(result, displayValue)
}

def safeValue(device, attr) {
    def v = device?.currentValue(attr)
    if (v == null) return null
    try {
        return new BigDecimal(v.toString())
    } catch (e) {
        log.warn "Invalid numeric value for ${attr}: ${v}"
        return null
    }
}

def convertUnit(BigDecimal celsius) {
    if (displayUnit == "F") {
        return (celsius * 9G / 5G) + 32G
    } else {
        return celsius
    }
} 

def updateChildDevice(result, displayValue) {
    def child = getChildDevice(state.childDeviceId)
    if (!child) return
    
    def C = WEATHERSENSE_CONST  // FIXED: inline consts

    child.sendEvent(name: C.ATTR_COMFORT_LEVEL, value: result.comfortLevel)
    child.sendEvent(name: C.ATTR_COMFORT_DESCRIPTION, value: C.COMFORT_DESCRIPTIONS[result.comfortLevel])
    child.sendEvent(name: C.ATTR_CALCULATION_METHOD, value: result.method)
    child.sendEvent(name: C.ATTR_IS_COMFORTABLE, value: isComfortable(result.comfortLevel))
    child.sendEvent(name: C.ATTR_WIND_DIRECTION_CORRECTION, value: result.windDirectionCorrection)

    // Input values as attributes
    child.sendEvent(name: "inputTemp", value: state.lastInputs.temperature?.round(1))
    child.sendEvent(name: "inputHumidity", value: state.lastInputs.humidity?.round(1))
}

def isComfortable(String level) {  // FIXED: inline check
    return level in [
        "comfortable", 
        "slightly_warm", 
        "slightly_cool"
    ]
}

// ========== DASHBOARD ==========
def getDashboardStatus() {
    if (!state.lastResult) return "Waiting..."
    def unit = displayUnit == "F" ? "°F" : "°C"
    return "${state.smoothedValue?.round(1)}${unit} (${state.lastResult.comfortLevel})"
}
