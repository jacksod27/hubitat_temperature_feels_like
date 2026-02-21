/*
 WeatherSense App v1.0 (AUTO-BUILT)
 Built: 2026-02-21 06:05:18
 Source: https://github.com/YOURUSER/ha-weathersense-hubitat
 License: CC BY-NC-SA 4.0
*/

/*
 Constants for the WeatherSense integration (Hubitat / Groovy version) - INLINE VERSION.

 Original: ha-weathersense (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   [https://github.com/smkrv/ha-weathersense](https://github.com/smkrv/ha-weathersense)
*/

// Domain / name
final String WEATHERSENSE_DOMAIN = "weathersense"
final String WEATHERSENSE_NAME   = "WeatherSense"

// Configuration options
final String CONF_TEMPERATURE_SENSOR        = "temperature_sensor"
final String CONF_HUMIDITY_SENSOR           = "humidity_sensor"
final String CONF_WIND_SPEED_SENSOR         = "wind_speed_sensor"
final String CONF_PRESSURE_SENSOR           = "pressure_sensor"
final String CONF_IS_OUTDOOR                = "is_outdoor"
final String CONF_SOLAR_RADIATION_SENSOR    = "solar_radiation_sensor"
final String CONF_TIME_OF_DAY               = "time_of_day"
final String CONF_DISPLAY_UNIT              = "display_unit"
final String CONF_WIND_DIRECTION_SENSOR     = "wind_direction_sensor"
final String CONF_WIND_DIRECTION_CORRECTION = "wind_direction_correction"
final String CONF_SMOOTHING_ENABLED         = "smoothing_enabled"
final String CONF_SMOOTHING_FACTOR          = "smoothing_factor"

// Default values
final String DEFAULT_NAME                    = "Feels Like Temperature"
final Boolean DEFAULT_IS_OUTDOOR             = true
final BigDecimal DEFAULT_SMOOTHING_FACTOR    = 0.3G

// Comfort levels (short form)
final String COMFORT_EXTREME_COLD   = "extreme_cold"
final String COMFORT_VERY_COLD      = "very_cold"
final String COMFORT_COLD           = "cold"
final String COMFORT_COOL           = "cool"
final String COMFORT_SLIGHTLY_COOL  = "slightly_cool"
final String COMFORT_COMFORTABLE    = "comfortable"
final String COMFORT_SLIGHTLY_WARM  = "slightly_warm"
final String COMFORT_WARM           = "warm"
final String COMFORT_HOT            = "hot"
final String COMFORT_VERY_HOT       = "very_hot"
final String COMFORT_EXTREME_HOT    = "extreme_hot"

// Comfort level descriptions
final Map<String, String> COMFORT_DESCRIPTIONS = [
    extreme_cold:    "Extreme Cold Stress",
    very_cold:       "Very Strong Cold Stress",
    cold:            "Strong Cold Stress",
    cool:            "Moderate Cold Stress",
    slightly_cool:   "Slight Cold Stress",
    comfortable:     "No Thermal Stress (Comfort)",
    slightly_warm:   "Slight Heat Stress",
    warm:            "Moderate Heat Stress",
    hot:             "Strong Heat Stress",
    very_hot:        "Very Strong Heat Stress",
    extreme_hot:     "Extreme Heat Stress"
]

// Comfort level detailed explanations
final Map<String, String> COMFORT_EXPLANATIONS = [
    extreme_cold:    "Extreme risk: frostbite possible in less than 5 minutes",
    very_cold:       "High risk: frostbite possible in 5-10 minutes",
    cold:            "Warning: frostbite possible in 10-30 minutes",
    cool:            "Caution: prolonged exposure may cause discomfort",
    slightly_cool:   "Slightly cool: light discomfort for sensitive individuals",
    comfortable:     "Optimal thermal conditions: most people feel comfortable",
    slightly_warm:   "Slightly warm: light discomfort for sensitive individuals",
    warm:            "Caution: fatigue possible with prolonged exposure",
    hot:             "Extreme caution: heat exhaustion possible",
    very_hot:        "Danger: heat cramps and exhaustion likely",
    extreme_hot:     "Extreme danger: heat stroke imminent"
]

// Comfort icons
final Map<String, String> COMFORT_ICONS = [
    extreme_cold:       "mdi:snowflake-alert",
    very_cold:          "mdi:snowflake-thermometer",
    cold:               "mdi:thermometer-low",
    cool:               "mdi:thermometer-minus",
    slightly_cool:      "mdi:thermometer-minus",
    comfortable:        "mdi:hand-okay",
    slightly_warm:      "mdi:thermometer-plus",
    warm:               "mdi:thermometer-high",
    hot:                "mdi:thermometer-alert",
    very_hot:           "mdi:heat-wave",
    extreme_hot:        "mdi:fire-alert"
]

// Sensor attributes (Hubitat camelCase)
final String ATTR_COMFORT_LEVEL             = "comfortLevel"
final String ATTR_COMFORT_DESCRIPTION       = "comfortDescription"
final String ATTR_COMFORT_EXPLANATION       = "comfortExplanation"
final String ATTR_CALCULATION_METHOD        = "calculationMethod"
final String ATTR_TEMPERATURE               = "temperature"
final String ATTR_HUMIDITY                  = "humidity"
final String ATTR_WIND_SPEED                = "windSpeed"
final String ATTR_PRESSURE                  = "pressure"
final String ATTR_IS_OUTDOOR                = "isOutdoor"
final String ATTR_TIME_OF_DAY               = "timeOfDay"
final String ATTR_IS_COMFORTABLE            = "isComfortable"
final String ATTR_WIND_DIRECTION            = "windDirection"
final String ATTR_WIND_DIRECTION_CORRECTION = "windDirectionCorrection"


/* ───────────────────────── */

/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version) - INLINE VERSION.

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   [https://github.com/smkrv/ha-weathersense](https://github.com/smkrv/ha-weathersense)
*/

final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

final Map COMFORT_LEVELS = [
    "extreme_cold": "extreme_cold",
    "very_cold": "very_cold", 
    "cold": "cold",
    "cool": "cool",
    "slightly_cool": "slightly_cool",
    "comfortable": "comfortable",
    "slightly_warm": "slightly_warm",
    "warm": "warm",
    "hot": "hot",
    "very_hot": "very_hot",
    "extreme_hot": "extreme_hot"
]

def calculateHeatIndex(BigDecimal temperature, BigDecimal humidity) {
    // Convert Celsius to Fahrenheit for calculation
    BigDecimal tF = (temperature * 9G/5G) + 32G
    BigDecimal rh = humidity ?: 50G

    BigDecimal hiF
    if (rh < 40G || tF < 80G) {
        hiF = 0.5G * (tF + 61.0G + ((tF - 68.0G) * 1.2G) + (rh * 0.094G))
        hiF = (hiF + tF) / 2G
    } else {
        // Full Rothfusz regression formula (works in Fahrenheit)
        hiF = -42.379G
        hiF += 2.04901523G * tF
        hiF += 10.14333127G * rh
        hiF -= 0.22475541G * tF * rh
        hiF -= 0.00683783G * tF * tF
        hiF -= 0.05481717G * rh * rh
        hiF += 0.00122874G * tF * tF * rh
        hiF += 0.00085282G * tF * rh * rh
        hiF -= 0.00000199G * tF * tF * rh * rh

        // Adjustments for extreme conditions (Fahrenheit values)
        if (rh < 13G && tF >= 80G && tF <= 112G) {
            BigDecimal adjustment = ((13G - rh) / 4G) * Math.sqrt((17G - Math.abs(tF - 95G)) / 17G)
            hiF -= adjustment
        } else if (rh > 85G && tF >= 80G && tF <= 87G) {
            BigDecimal adjustment = ((rh - 85G) / 10G) * ((87G - tF) / 5G)
            hiF += adjustment
        }
    }

    // Convert back to Celsius
    BigDecimal hiC = (hiF - 32G) * 5G/9G

    // Sanity check - heat index should not be unreasonably high
    if (hiC > 70G) {
        hiC = Math.min(hiC, temperature + 25G)
    }

    return hiC
}

def calculateWindChill(BigDecimal temperature, BigDecimal windSpeed) {
    BigDecimal t = temperature ?: 0G
    // Convert m/s to km/h as the formula requires km/h
    BigDecimal vKmh = Math.max((windSpeed ?: 0G) * 3.6G, 4.828G)  // Minimum 4.828 km/h (≈1.34 m/s)

    BigDecimal wct = 13.12G
    wct += 0.6215G * t
    wct -= 11.37G * Math.pow(vKmh, 0.16G)
    wct += 0.3965G * t * Math.pow(vKmh, 0.16G)

    return wct
}

def calculateSteadmanApparentTemp(BigDecimal temperature, BigDecimal humidity, BigDecimal windSpeed) {
    BigDecimal t = temperature ?: 20G
    BigDecimal v = windSpeed ?: 0G

    // Calculate vapor pressure (e) in kPa
    BigDecimal e = ((humidity ?: 50G) / 100G) * 6.105G * Math.exp((17.27G * t) / (237.7G + t))

    BigDecimal at = t + 0.33G * e - 0.70G * v - 4.00G
    return at
}

def applySolarCorrection(BigDecimal feelsLike, Date timeOfDay = null, BigDecimal cloudiness = 0G) {
    if (!timeOfDay) {
        timeOfDay = new Date()
    }

    Calendar cal = Calendar.instance
    cal.time = timeOfDay
    int hour = cal.get(Calendar.HOUR_OF_DAY)

    BigDecimal solarFactor = 0G

    // Daytime solar radiation effect (sunrise to sunset approximately)
    if (hour >= 6 && hour <= 18) {
        BigDecimal hoursFromSunrise = (hour - 6G) as BigDecimal
        BigDecimal solarIntensity = Math.sin((Math.PI * hoursFromSunrise) / 12G)

        BigDecimal maxSolarCorrection = feelsLike >= 25G ? 2.5G :
                                       feelsLike >= 15G ? 2.0G :
                                       feelsLike >= 5G ? 1.5G : 1.0G

        BigDecimal cloudFactor = 1G - (cloudiness / 100G)
        solarFactor = maxSolarCorrection * solarIntensity * cloudFactor
    }
    // Nighttime cooling effect (minor)
    else if (hour >= 22 || hour <= 4) {
        solarFactor = -0.5G
    }

    return feelsLike + solarFactor
}

def applyPressureCorrection(BigDecimal feelsLike, BigDecimal pressure = null) {
    if (pressure == null || pressure <= 0G) {
        return feelsLike
    }

    if (pressure < 80G || pressure > 110G) {
        return feelsLike
    }

    BigDecimal correction = 0.1G * (STANDARD_PRESSURE - pressure)
    return feelsLike + correction
}

def applyWindDirectionCorrection(BigDecimal feelsLike, BigDecimal windDirection, BigDecimal latitude = null, BigDecimal maxCorrection = 1.0G) {
    if (windDirection == null) {
        return [feelsLike: feelsLike, correction: 0G]
    }

    // Normalize wind direction to 0-360 range
    windDirection = ((windDirection % 360G) + 360G) % 360G

    // Calculate north-south component using cosine
    BigDecimal northFactor = Math.cos(Math.toRadians(windDirection as double))

    // Determine hemisphere from latitude (default to Northern if not specified)
    boolean isNorthernHemisphere = !latitude || latitude >= 0G

    BigDecimal correction = isNorthernHemisphere ? 
        -northFactor * maxCorrection : northFactor * maxCorrection

    return [feelsLike: feelsLike + correction, correction: correction]
}

def calculateFeelsLike(
    BigDecimal temperature,
    BigDecimal humidity,
    BigDecimal windSpeed = 0G,
    BigDecimal pressure = null,
    boolean isOutdoor = true,
    Date timeOfDay = null,
    BigDecimal cloudiness = 0G,
    BigDecimal windDirection = null,
    BigDecimal latitude = null,
    boolean enableWindDirectionCorrection = false
) {
    String method = ""
    BigDecimal windDirectionCorrection = 0G

    BigDecimal feelsLike
    String comfortLevel

    if (isOutdoor) {
        // Outdoor calculation
        if (temperature >= 27G && humidity >= 40G) {
            feelsLike = calculateHeatIndex(temperature, humidity)
            method = "Heat Index"
        } else if (temperature <= 10G && (windSpeed ?: 0G) > 1.34G) {
            feelsLike = calculateWindChill(temperature, windSpeed)
            method = "Wind Chill"
        } else {
            feelsLike = calculateSteadmanApparentTemp(temperature, humidity, windSpeed)
            method = "Steadman Apparent Temperature"
        }

        // Apply solar and pressure corrections
        BigDecimal originalFeelsLike = feelsLike
        feelsLike = applySolarCorrection(feelsLike, timeOfDay, cloudiness)
        if (pressure != null && pressure > 0G) {
            feelsLike = applyPressureCorrection(feelsLike, pressure)
        }

        // Apply experimental wind direction correction if enabled
        if (enableWindDirectionCorrection && windDirection != null) {
            def result = applyWindDirectionCorrection(feelsLike, windDirection, latitude)
            feelsLike = result.feelsLike
            windDirectionCorrection = result.correction
        }

        // Determine comfort level for outdoor
        comfortLevel = determineOutdoorComfort(feelsLike, method)
    } else {
        // Indoor calculation - simplified approach
        feelsLike = calculateIndoorFeelsLike(temperature, humidity)
        method = "Indoor Comfort Model"
        comfortLevel = determineIndoorComfort(feelsLike, humidity)
    }

    // flag to identify it feelslike value is too far from actual value
    boolean outOfRange = Math.abs(feelsLike - temperature) > 25G
   
    return [
        feelsLike: feelsLike,
        method: method,
        comfortLevel: comfortLevel,
        windDirectionCorrection: windDirectionCorrection,
        outOfRange: outOfRange
    ]
}

def calculateIndoorFeelsLike(BigDecimal temperature, BigDecimal humidity) {
    BigDecimal humidityFactor = 0G
    if ((humidity ?: 50G) < 30G) {
        humidityFactor = (humidity - 30G) * 0.05G
    } else if (humidity > 60G) {
        humidityFactor = (humidity - 60G) * 0.05G
    }

    return temperature + humidityFactor
}

def determineOutdoorComfort(BigDecimal feelsLike, String method) {
    switch (method) {
        case "Heat Index":
            if (feelsLike >= 54G) return "extreme_hot"
            if (feelsLike >= 41G) return "very_hot"
            if (feelsLike >= 32G) return "hot"
            if (feelsLike >= 27G) return "warm"
            return "comfortable"

        case "Wind Chill":
            if (feelsLike <= -48G) return "extreme_cold"
            if (feelsLike <= -40G) return "very_cold"
            if (feelsLike <= -27G) return "cold"
            if (feelsLike <= -13G) return "cool"
            if (feelsLike <= 0G) return "slightly_cool"
            return "comfortable"

        default:  // Steadman or other
            if (feelsLike > 46G) return "extreme_hot"
            if (feelsLike > 38G) return "very_hot"
            if (feelsLike > 32G) return "hot"
            if (feelsLike > 29G) return "warm"
            if (feelsLike > 26G) return "slightly_warm"
            if (feelsLike > 9G) return "comfortable"
            if (feelsLike > 0G) return "slightly_cool"
            if (feelsLike > -13G) return "cool"
            if (feelsLike > -27G) return "cold"
            if (feelsLike > -40G) return "very_cold"
            return "extreme_cold"
    }
}

def determineIndoorComfort(BigDecimal temperature, BigDecimal humidity) {
    if (temperature < 16G) return "cold"
    if (temperature < 18G) return "cool"
    if (temperature < 20G) return "slightly_cool"

    if (temperature <= 24G) {
        if ((humidity ?: 50G) < 30G) return "slightly_cool"
        if (humidity > 70G) return "slightly_warm"
        return "comfortable"
    }
    if (temperature <= 26G) return "slightly_warm"
    if (temperature <= 28G) return "warm"
    return "hot"
}


/* ───────────────────────── */

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
