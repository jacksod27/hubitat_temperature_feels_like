/*
 WeatherSense App v1.0 (AUTO-BUILT)
 Built: 2026-02-28 08:57:24
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
 Source:   https://github.com/smkrv/ha-weathersense
*/

// FIX #8: Promote to @Field so it is accessible from all methods in the app/driver
@Field final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

final Map COMFORT_LEVELS = [
    "extreme_cold":  "extreme_cold",
    "very_cold":     "very_cold",
    "cold":          "cold",
    "cool":          "cool",
    "slightly_cool": "slightly_cool",
    "comfortable":   "comfortable",
    "slightly_warm": "slightly_warm",
    "warm":          "warm",
    "hot":           "hot",
    "very_hot":      "very_hot",
    "extreme_hot":   "extreme_hot"
]

def calculateHeatIndex(BigDecimal temperature, BigDecimal humidity) {
    BigDecimal tF = (temperature * 9G / 5G) + 32G
    BigDecimal rh = humidity ?: 50G

    BigDecimal hiF
    if (rh < 40G || tF < 80G) {
        hiF = 0.5G * (tF + 61.0G + ((tF - 68.0G) * 1.2G) + (rh * 0.094G))
        hiF = (hiF + tF) / 2G
    } else {
        // Full Rothfusz regression (Fahrenheit)
        hiF = -42.379G
        hiF += 2.04901523G  * tF
        hiF += 10.14333127G * rh
        hiF -= 0.22475541G  * tF * rh
        hiF -= 0.00683783G  * tF * tF
        hiF -= 0.05481717G  * rh * rh
        hiF += 0.00122874G  * tF * tF * rh
        hiF += 0.00085282G  * tF * rh * rh
        hiF -= 0.00000199G  * tF * tF * rh * rh

        // FIX #5 + #6: Use .toDouble() for Math methods; use .abs() on BigDecimal object
        if (rh < 13G && tF >= 80G && tF <= 112G) {
            BigDecimal adjustment = ((13G - rh) / 4G) *
                new BigDecimal(Math.sqrt(((17G - (tF - 95G).abs()) / 17G).toDouble()))
            hiF -= adjustment
        } else if (rh > 85G && tF >= 80G && tF <= 87G) {
            BigDecimal adjustment = ((rh - 85G) / 10G) * ((87G - tF) / 5G)
            hiF += adjustment
        }
    }

    BigDecimal hiC = (hiF - 32G) * 5G / 9G

    // FIX #7: Replace Math.min() (no BigDecimal overload) with ternary
    if (hiC > 70G) {
        BigDecimal cap = temperature + 25G
        hiC = hiC < cap ? hiC : cap
    }

    return hiC
}

def calculateWindChill(BigDecimal temperature, BigDecimal windSpeed) {
    BigDecimal t = temperature ?: 0G
    BigDecimal vKmh = ((windSpeed ?: 0G) * 3.6G).max(4.828G)  // min 4.828 km/h

    // FIX #1: Math.pow() requires double arguments; cast both and result back to BigDecimal
    BigDecimal vPow = new BigDecimal(Math.pow(vKmh.toDouble(), 0.16))

    BigDecimal wct = 13.12G
    wct += 0.6215G  * t
    wct -= 11.37G   * vPow
    wct += 0.3965G  * t * vPow

    return wct
}

def calculateSteadmanApparentTemp(BigDecimal temperature, BigDecimal humidity, BigDecimal windSpeed) {
    BigDecimal t = temperature ?: 20G
    BigDecimal v = windSpeed ?: 0G

    // FIX #2: Math.exp() requires double; cast argument and wrap result back in BigDecimal
    BigDecimal expArg = (17.27G * t) / (237.7G + t)
    BigDecimal e = ((humidity ?: 50G) / 100G) * 6.105G * new BigDecimal(Math.exp(expArg.toDouble()))

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

    if (hour >= 6 && hour <= 18) {
        BigDecimal hoursFromSunrise = (hour - 6) as BigDecimal

        // FIX #3: Math.sin() requires double; wrap result back in BigDecimal
        BigDecimal solarIntensity = new BigDecimal(
            Math.sin((Math.PI * hoursFromSunrise.toDouble()) / 12.0)
        )

        BigDecimal maxSolarCorrection = feelsLike >= 25G ? 2.5G :
                                        feelsLike >= 15G ? 2.0G :
                                        feelsLike >= 5G  ? 1.5G : 1.0G

        BigDecimal cloudFactor = 1G - (cloudiness / 100G)
        solarFactor = maxSolarCorrection * solarIntensity * cloudFactor

    } else if (hour >= 22 || hour <= 4) {
        solarFactor = -0.5G
    }

    return feelsLike + solarFactor
}

def applyPressureCorrection(BigDecimal feelsLike, BigDecimal pressure = null) {
    if (pressure == null || pressure <= 0G) return feelsLike
    if (pressure < 80G || pressure > 110G) return feelsLike

    // FIX #8: STANDARD_PRESSURE now accessible as @Field
    BigDecimal correction = 0.1G * (STANDARD_PRESSURE - pressure)
    return feelsLike + correction
}

def applyWindDirectionCorrection(BigDecimal feelsLike, BigDecimal windDirection, BigDecimal latitude = null, BigDecimal maxCorrection = 1.0G) {
    if (windDirection == null) {
        return [feelsLike: feelsLike, correction: 0G]
    }

    windDirection = ((windDirection % 360G) + 360G) % 360G

    // FIX #3: Math.cos() + Math.toRadians() require double
    BigDecimal northFactor = new BigDecimal(Math.cos(Math.toRadians(windDirection.toDouble())))

    boolean isNorthernHemisphere = !latitude || latitude >= 0G

    BigDecimal correction = isNorthernHemisphere ?
        -northFactor * maxCorrection : northFactor * maxCorrection

    return [feelsLike: feelsLike + correction, correction: correction]
}

def calculateFeelsLike(
    BigDecimal temperature,
    BigDecimal humidity,
    BigDecimal windSpeed               = 0G,
    BigDecimal pressure                = null,
    boolean isOutdoor                  = true,
    Date timeOfDay                     = null,
    BigDecimal cloudiness              = 0G,
    BigDecimal windDirection           = null,
    BigDecimal latitude                = null,
    boolean enableWindDirectionCorrection = false
) {
    String method = ""
    BigDecimal windDirectionCorrection = 0G
    BigDecimal feelsLike
    String comfortLevel

    if (isOutdoor) {
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

        feelsLike = applySolarCorrection(feelsLike, timeOfDay, cloudiness)
        if (pressure != null && pressure > 0G) {
            feelsLike = applyPressureCorrection(feelsLike, pressure)
        }

        if (enableWindDirectionCorrection && windDirection != null) {
            def result = applyWindDirectionCorrection(feelsLike, windDirection, latitude)
            feelsLike = result.feelsLike
            windDirectionCorrection = result.correction
        }

        comfortLevel = determineOutdoorComfort(feelsLike, method)
    } else {
        feelsLike = calculateIndoorFeelsLike(temperature, humidity)
        method = "Indoor Comfort Model"
        comfortLevel = determineIndoorComfort(feelsLike, humidity)
    }

    boolean outOfRange = (feelsLike - temperature).abs() > 25G  // FIX #6: .abs() on BigDecimal

    return [
        feelsLike:               feelsLike,
        method:                  method,
        comfortLevel:            comfortLevel,
        windDirectionCorrection: windDirectionCorrection,
        outOfRange:              outOfRange
    ]
}

def calculateIndoorFeelsLike(BigDecimal temperature, BigDecimal humidity) {
    BigDecimal rh = humidity ?: 50G
    BigDecimal humidityFactor = 0G
    if (rh < 30G) {
        humidityFactor = (rh - 30G) * 0.05G  // FIX: was using raw humidity which could be null
    } else if (rh > 60G) {
        humidityFactor = (rh - 60G) * 0.05G
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
            if (feelsLike <= 0G)   return "slightly_cool"
            return "comfortable"

        default:  // Steadman or other
            if (feelsLike > 46G)  return "extreme_hot"
            if (feelsLike > 38G)  return "very_hot"
            if (feelsLike > 32G)  return "hot"
            if (feelsLike > 29G)  return "warm"
            if (feelsLike > 26G)  return "slightly_warm"
            if (feelsLike > 9G)   return "comfortable"
            if (feelsLike > 0G)   return "slightly_cool"
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
        BigDecimal rh = humidity ?: 50G
        if (rh < 30G)  return "slightly_cool"
        if (rh > 70G)  return "slightly_warm"
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
