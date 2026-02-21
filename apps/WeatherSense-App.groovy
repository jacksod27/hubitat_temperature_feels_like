/*
 WeatherSense App v1.0 (AUTO-BUILT)
 Built: 2026-02-21 04:20:41
 Source: https://github.com/YOURUSER/ha-weathersense-hubitat
 License: CC BY-NC-SA 4.0
*/

/*
 Constants for the WeatherSense integration (Hubitat / Groovy version).

 Original: ha-weathersense (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

class WeatherSenseConst {

    // Domain / name
    static final String DOMAIN = "weathersense"
    static final String NAME   = "WeatherSense"

    // Configuration options
    static final String CONF_TEMPERATURE_SENSOR        = "temperature_sensor"
    static final String CONF_HUMIDITY_SENSOR           = "humidity_sensor"
    static final String CONF_WIND_SPEED_SENSOR         = "wind_speed_sensor"
    static final String CONF_PRESSURE_SENSOR           = "pressure_sensor"
    static final String CONF_IS_OUTDOOR                = "is_outdoor"
    static final String CONF_SOLAR_RADIATION_SENSOR    = "solar_radiation_sensor"
    static final String CONF_TIME_OF_DAY               = "time_of_day"
    static final String CONF_DISPLAY_UNIT              = "display_unit"
    static final String CONF_WIND_DIRECTION_SENSOR     = "wind_direction_sensor"
    static final String CONF_WIND_DIRECTION_CORRECTION = "wind_direction_correction"
    static final String CONF_SMOOTHING_ENABLED         = "smoothing_enabled"
    static final String CONF_SMOOTHING_FACTOR          = "smoothing_factor"

    // Default values
    static final String DEFAULT_NAME             = "Feels Like Temperature"
    static final Boolean DEFAULT_IS_OUTDOOR      = true
    static final BigDecimal DEFAULT_SMOOTHING_FACTOR = 0.3G

    // Comfort levels (short form)
    static final String COMFORT_EXTREME_COLD = "extreme_cold"
    static final String COMFORT_VERY_COLD    = "very_cold"
    static final String COMFORT_COLD         = "cold"
    static final String COMFORT_COOL         = "cool"
    static final String COMFORT_SLIGHTLY_COOL = "slightly_cool"
    static final String COMFORT_COMFORTABLE   = "comfortable"
    static final String COMFORT_SLIGHTLY_WARM = "slightly_warm"
    static final String COMFORT_WARM          = "warm"
    static final String COMFORT_HOT           = "hot"
    static final String COMFORT_VERY_HOT      = "very_hot"
    static final String COMFORT_EXTREME_HOT   = "extreme_hot"

    // Comfort level descriptions
    static final Map<String,String> COMFORT_DESCRIPTIONS = [
        (COMFORT_EXTREME_COLD): "Extreme Cold Stress",
        (COMFORT_VERY_COLD)   : "Very Strong Cold Stress",
        (COMFORT_COLD)        : "Strong Cold Stress",
        (COMFORT_COOL)        : "Moderate Cold Stress",
        (COMFORT_SLIGHTLY_COOL): "Slight Cold Stress",
        (COMFORT_COMFORTABLE) : "No Thermal Stress (Comfort)",
        (COMFORT_SLIGHTLY_WARM): "Slight Heat Stress",
        (COMFORT_WARM)        : "Moderate Heat Stress",
        (COMFORT_HOT)         : "Strong Heat Stress",
        (COMFORT_VERY_HOT)    : "Very Strong Heat Stress",
        (COMFORT_EXTREME_HOT) : "Extreme Heat Stress"
    ]

    // Comfort level detailed explanations
    static final Map<String,String> COMFORT_EXPLANATIONS = [
        (COMFORT_EXTREME_COLD): "Extreme risk: frostbite possible in less than 5 minutes",
        (COMFORT_VERY_COLD)   : "High risk: frostbite possible in 5-10 minutes",
        (COMFORT_COLD)        : "Warning: frostbite possible in 10-30 minutes",
        (COMFORT_COOL)        : "Caution: prolonged exposure may cause discomfort",
        (COMFORT_SLIGHTLY_COOL): "Slightly cool: light discomfort for sensitive individuals",
        (COMFORT_COMFORTABLE) : "Optimal thermal conditions: most people feel comfortable",
        (COMFORT_SLIGHTLY_WARM): "Slightly warm: light discomfort for sensitive individuals",
        (COMFORT_WARM)        : "Caution: fatigue possible with prolonged exposure",
        (COMFORT_HOT)         : "Extreme caution: heat exhaustion possible",
        (COMFORT_VERY_HOT)    : "Danger: heat cramps and exhaustion likely",
        (COMFORT_EXTREME_HOT) : "Extreme danger: heat stroke imminent"
    ]

    // Comfort icons
    static final Map<String,String> COMFORT_ICONS = [
        (COMFORT_EXTREME_COLD):  "mdi:snowflake-alert",
        (COMFORT_VERY_COLD)   :  "mdi:snowflake-thermometer",
        (COMFORT_COLD)        :  "mdi:thermometer-low",
        (COMFORT_COOL)        :  "mdi:thermometer-minus",
        (COMFORT_SLIGHTLY_COOL): "mdi:thermometer-minus",
        (COMFORT_COMFORTABLE) :  "mdi:hand-okay",
        (COMFORT_SLIGHTLY_WARM): "mdi:thermometer-plus",
        (COMFORT_WARM)        :  "mdi:thermometer-high",
        (COMFORT_HOT)         :  "mdi:thermometer-alert",
        (COMFORT_VERY_HOT)    :  "mdi:heat-wave",
        (COMFORT_EXTREME_HOT) :  "mdi:fire-alert"
    ]

    // Sensor attributes (Hubitat camelCase)
    static final String ATTR_COMFORT_LEVEL             = "comfortLevel"
    static final String ATTR_COMFORT_DESCRIPTION       = "comfortDescription"
    static final String ATTR_COMFORT_EXPLANATION       = "comfortExplanation"
    static final String ATTR_CALCULATION_METHOD        = "calculationMethod"
    static final String ATTR_TEMPERATURE               = "temperature"
    static final String ATTR_HUMIDITY                  = "humidity"
    static final String ATTR_WIND_SPEED                = "windSpeed"
    static final String ATTR_PRESSURE                  = "pressure"
    static final String ATTR_IS_OUTDOOR                = "isOutdoor"
    static final String ATTR_TIME_OF_DAY               = "timeOfDay"
    static final String ATTR_IS_COMFORTABLE            = "isComfortable"
    static final String ATTR_WIND_DIRECTION            = "windDirection"
    static final String ATTR_WIND_DIRECTION_CORRECTION = "windDirectionCorrection"

}


/* ───────────────────────── */

/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version).

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

import groovy.transform.Field

class WeatherSenseCalculator {

 static final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

    static BigDecimal calculateHeatIndex(BigDecimal temperature, BigDecimal humidity) {
        // Convert Celsius to Fahrenheit for calculation
        BigDecimal tF = (temperature * 9/5) + 32
        BigDecimal rh = humidity

        BigDecimal hiF
        if (rh < 40 || tF < 80) {
            hiF = 0.5 * (tF + 61.0 + ((tF - 68.0) * 1.2) + (rh * 0.094))
            hiF = (hiF + tF) / 2
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
            if (rh < 13 && tF >= 80 && tF <= 112) {
                BigDecimal adjustment = ((13 - rh) / 4) * Math.sqrt((17 - Math.abs(tF - 95)) / 17)
                hiF -= adjustment
            } else if (rh > 85 && tF >= 80 && tF <= 87) {
                BigDecimal adjustment = ((rh - 85) / 10) * ((87 - tF) / 5)
                hiF += adjustment
            }
        }

        // Convert back to Celsius
        BigDecimal hiC = (hiF - 32) * 5/9

        // Sanity check - heat index should not be unreasonably high
        if (hiC > 70) {
            hiC = Math.min(hiC, temperature + 25)
        }

        return hiC
    }

    static BigDecimal calculateWindChill(BigDecimal temperature, BigDecimal windSpeed) {
        BigDecimal t = temperature
        // Convert m/s to km/h as the formula requires km/h
        BigDecimal vKmh = Math.max(windSpeed * 3.6, 4.828G)  // Minimum 4.828 km/h (≈1.34 m/s)

        BigDecimal wct = 13.12G
        wct += 0.6215G * t
        wct -= 11.37G * Math.pow(vKmh, 0.16G)
        wct += 0.3965G * t * Math.pow(vKmh, 0.16G)

        return wct
    }

    static BigDecimal calculateSteadmanApparentTemp(BigDecimal temperature, BigDecimal humidity, BigDecimal windSpeed) {
        BigDecimal t = temperature
        BigDecimal v = windSpeed

        // Calculate vapor pressure (e) in kPa
        BigDecimal e = (humidity / 100) * 6.105G * Math.exp((17.27G * t) / (237.7G + t))

        BigDecimal at = t + 0.33G * e - 0.70G * v - 4.00G
        return at
    }

    static BigDecimal applySolarCorrection(BigDecimal feelsLike, Date timeOfDay = null, BigDecimal cloudiness = 0G) {
        if (!timeOfDay) {
            timeOfDay = new Date()
        }

        Calendar cal = Calendar.instance
        cal.time = timeOfDay
        int hour = cal.get(Calendar.HOUR_OF_DAY)

        BigDecimal solarFactor = 0G

        // Daytime solar radiation effect (sunrise to sunset approximately)
        if (hour >= 6 && hour <= 18) {
            BigDecimal hoursFromSunrise = hour - 6
            BigDecimal solarIntensity = Math.sin((Math.PI * hoursFromSunrise) / 12)

            BigDecimal maxSolarCorrection
            if (feelsLike >= 25) {
                maxSolarCorrection = 2.5G
            } else if (feelsLike >= 15) {
                maxSolarCorrection = 2.0G
            } else if (feelsLike >= 5) {
                maxSolarCorrection = 1.5G
            } else {
                maxSolarCorrection = 1.0G
            }

            BigDecimal cloudFactor = 1 - (cloudiness / 100)
            solarFactor = maxSolarCorrection * solarIntensity * cloudFactor
        }
        // Nighttime cooling effect (minor)
        else if (hour >= 22 || hour <= 4) {
            solarFactor = -0.5G
        }

        return feelsLike + solarFactor
    }

    static BigDecimal applyPressureCorrection(BigDecimal feelsLike, BigDecimal pressure = null) {
        if (pressure == null || pressure <= 0) {
            return feelsLike
        }

        if (pressure < 80 || pressure > 110) {
            return feelsLike
        }

        BigDecimal correction = 0.1G * (STANDARD_PRESSURE - pressure)
        return feelsLike + correction
    }

    static Map applyWindDirectionCorrection(BigDecimal feelsLike, BigDecimal windDirection, BigDecimal latitude = null, BigDecimal maxCorrection = 1.0G) {
        if (windDirection == null) {
            return [feelsLike: feelsLike, correction: 0G]
        }

        // Normalize wind direction to 0-360 range
        windDirection = ((windDirection % 360) + 360) % 360

        // Calculate north-south component using cosine
        BigDecimal northFactor = Math.cos(Math.toRadians(windDirection))

        // Determine hemisphere from latitude (default to Northern if not specified)
        boolean isNorthernHemisphere = !latitude || latitude >= 0

        BigDecimal correction
        if (isNorthernHemisphere) {
            correction = -northFactor * maxCorrection
        } else {
            correction = northFactor * maxCorrection
        }

        return [feelsLike: feelsLike + correction, correction: correction]
    }

    static Map calculateFeelsLike(
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
            if (temperature >= 27 && humidity >= 40) {
                feelsLike = calculateHeatIndex(temperature, humidity)
                method = "Heat Index"
            } else if (temperature <= 10 && windSpeed > 1.34G) {
                feelsLike = calculateWindChill(temperature, windSpeed)
                method = "Wind Chill"
            } else {
                feelsLike = calculateSteadmanApparentTemp(temperature, humidity, windSpeed)
                method = "Steadman Apparent Temperature"
            }

            // Apply solar and pressure corrections
            BigDecimal originalFeelsLike = feelsLike
            feelsLike = applySolarCorrection(feelsLike, timeOfDay, cloudiness)
            if (pressure) {
                feelsLike = applyPressureCorrection(feelsLike, pressure)
            }

            // Apply experimental wind direction correction if enabled
            if (enableWindDirectionCorrection && windDirection) {
                def result = applyWindDirectionCorrection(feelsLike, windDirection, latitude)
                feelsLike = result.feelsLike
                windDirectionCorrection = result.correction
            }

            // Sanity check for unreasonable values
            if (Math.abs(feelsLike - originalFeelsLike) > 0.1G) {
                log.debug "Applied corrections: original=${originalFeelsLike}°C, after corrections=${feelsLike}°C"
            }

            if (feelsLike > temperature + 25 || feelsLike < temperature - 25) {
                log.warn "Calculated feels-like temperature (${feelsLike}°C) is far from actual temperature (${temperature}°C)"
            }

            // Determine comfort level for outdoor
            comfortLevel = determineOutdoorComfort(feelsLike, method)
        } else {
            // Indoor calculation - simplified approach
            feelsLike = calculateIndoorFeelsLike(temperature, humidity)
            method = "Indoor Comfort Model"
            comfortLevel = determineIndoorComfort(feelsLike, humidity)
        }

        return [
            feelsLike: feelsLike,
            method: method,
            comfortLevel: comfortLevel,
            windDirectionCorrection: windDirectionCorrection
        ]
    }

    static BigDecimal calculateIndoorFeelsLike(BigDecimal temperature, BigDecimal humidity) {
        BigDecimal humidityFactor = 0G
        if (humidity < 30) {
            humidityFactor = (humidity - 30) * 0.05G
        } else if (humidity > 60) {
            humidityFactor = (humidity - 60) * 0.05G
        }

        return temperature + humidityFactor
    }

    static String determineOutdoorComfort(BigDecimal feelsLike, String method) {
        // Reference WeatherSenseConst comfort levels
        def consts = WeatherSenseConst

        // Sanity check - if feels_like is unreasonable, use more conservative estimate
        if (feelsLike > 60) {
            log.warn "Calculated feels-like temperature is unreasonably high: ${feelsLike}°C. Capping at 50°C"
            feelsLike = Math.min(feelsLike, 50G)
        }

        switch (method) {
            case "Heat Index":
                if (feelsLike >= 54) return consts.COMFORT_EXTREME_HOT
                if (feelsLike >= 41) return consts.COMFORT_VERY_HOT
                if (feelsLike >= 32) return consts.COMFORT_HOT
                if (feelsLike >= 27) return consts.COMFORT_WARM
                return consts.COMFORT_COMFORTABLE

            case "Wind Chill":
                if (feelsLike <= -48) return consts.COMFORT_EXTREME_COLD
                if (feelsLike <= -40) return consts.COMFORT_VERY_COLD
                if (feelsLike <= -27) return consts.COMFORT_COLD
                if (feelsLike <= -13) return consts.COMFORT_COOL
                if (feelsLike <= 0) return consts.COMFORT_SLIGHTLY_COOL
                return consts.COMFORT_COMFORTABLE

            default:  // Steadman or other
                if (feelsLike > 46) return consts.COMFORT_EXTREME_HOT
                if (feelsLike > 38) return consts.COMFORT_VERY_HOT
                if (feelsLike > 32) return consts.COMFORT_HOT
                if (feelsLike > 29) return consts.COMFORT_WARM
                if (feelsLike > 26) return consts.COMFORT_SLIGHTLY_WARM
                if (feelsLike > 9) return consts.COMFORT_COMFORTABLE
                if (feelsLike > 0) return consts.COMFORT_SLIGHTLY_COOL
                if (feelsLike > -13) return consts.COMFORT_COOL
                if (feelsLike > -27) return consts.COMFORT_COLD
                if (feelsLike > -40) return consts.COMFORT_VERY_COLD
                return consts.COMFORT_EXTREME_COLD
        }
    }

    static String determineIndoorComfort(BigDecimal temperature, BigDecimal humidity) {
        def consts = WeatherSenseConst

        if (temperature < 16) return consts.COMFORT_COLD
        if (temperature < 18) return consts.COMFORT_COOL
        if (temperature < 20) return consts.COMFORT_SLIGHTLY_COOL

        if (temperature <= 24) {
            if (humidity < 30) return consts.COMFORT_SLIGHTLY_COOL
            if (humidity > 70) return consts.COMFORT_SLIGHTLY_WARM
            return consts.COMFORT_COMFORTABLE
        }
        if (temperature <= 26) return consts.COMFORT_SLIGHTLY_WARM
        if (temperature <= 28) return consts.COMFORT_WARM
        return consts.COMFORT_HOT
    }
}


/* ───────────────────────── */

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
            input "enableWindDirCorrection", "bool", title: "Wind Direction Correction", defaultValue: false
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
        enableWindDirectionCorrection: enableWindDirCorrection ?: false
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
