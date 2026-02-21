/*
 Hubitat WeatherSense App (converted from HA config flow).

 Original: ha-weathersense/config_flow.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV  
 Source:   https://github.com/smkrv/ha-weathersense
*/

// ========== DEFINITION ==========
definition(
    name: "WeatherSense", 
    namespace: "weathersense",
    description: "HA WeatherSense port for Hubitat",
    category: "Convenience"
) {
    // Capabilities handled automatically
}

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

def updated(settings) {
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
    runIn(1, "calculateAndUpdate")  // Debounce
}

// ========== CORE LOGIC ==========
def calculateAndUpdate() {
    def inputs = [
        temperature:  safeValue(temperatureSensor, "temperature"),
        humidity:     safeValue(humiditySensor, "humidity"),
        windSpeed:    safeValue(windSpeedSensor, "illuminance"),
        pressure:     safeValue(pressureSensor, "illuminance"),
        windDirection:safeValue(windDirectionSensor, "illuminance"),
        isOutdoor:    isOutdoor ?: true,
        timeOfDay:    new Date(),
        latitude:     location.latitude,
        enableWindDirectionCorrection: enableWindDirectionCorrection ?: false
    ]
    
    if (inputs.temperature == null || inputs.humidity == null) {
        log.warn "Missing required sensors"
        return
    }
    
    def result = WeatherSenseCalculator.calculateFeelsLike(
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
 
 // Smoothing
    BigDecimal displayValue = convertUnit(result.feelsLike)
    if (enableSmoothing && state.smoothedValue != null) {
        BigDecimal alpha = smoothingFactor ?: 0.3G
        displayValue = alpha * displayValue + (1 - alpha) * state.smoothedValue
    }
    state.smoothedValue = displayValue

    state.lastResult = result
    state.lastInputs = inputs

  // Update child
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

BigDecimal convertUnit(BigDecimal celsius) {
    return displayUnit == "F" ? (celsius * 9/5G + 32G) : celsius
}   

def updateChildDevice(result, displayValue) {
    def child = getChildDevice(state.childDeviceId)
    if (!child) return
    
    
    def C = WeatherSenseConst

    child.sendEvent(name: C.ATTR_COMFORT_LEVEL, value: result.comfortLevel)
    child.sendEvent(name: C.ATTR_COMFORT_DESCRIPTION, value: C.COMFORT_DESCRIPTIONS[result.comfortLevel])
    child.sendEvent(name: C.ATTR_CALCULATION_METHOD, value: result.method)
    child.sendEvent(name: C.ATTR_IS_COMFORTABLE, value: isComfortable(result.comfortLevel))
    child.sendEvent(name: C.ATTR_WIND_DIRECTION_CORRECTION, value: result.windDirectionCorrection)


    // Input values as attributes
    child.sendEvent(name: "inputTemp", value: state.lastInputs.temperature?.round(1))
    child.sendEvent(name: "inputHumidity", value: state.lastInputs.humidity?.round(1))
}

boolean isComfortable(String level) {
    def consts = WeatherSenseConst
    return level in [
     consts.COMFORT_COMFORTABLE, 
     consts.COMFORT_SLIGHTLY_WARM, 
     consts.COMFORT_SLIGHTLY_COOL
                    ]
}

// ========== DASHBOARD ==========
def getDashboardStatus() {
    return state.lastResult ? "${state.smoothedValue?.round(1)}°C (${state.lastResult.comfortLevel})" : "Waiting..."
}
