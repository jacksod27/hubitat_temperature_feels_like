// WeatherSense App v1.0
// Includes: preferences + subscriptions + handlers + calculator + consts
/*
 Hubitat WeatherSense App (converted from HA config flow).

 Original: ha-weathersense/config_flow.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV  
 Source:   https://github.com/smkrv/ha-weathersense
*/

import groovy.transform.Field

definition(
    name: "WeatherSense",
    namespace: "weathersense",
    author: "SMKRV (Hubitat port)",
    description: "Feels-like temperature with comfort levels",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
    page(name: "sensorPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "WeatherSense Setup", install: true, uninstall: true) {
        section("WeatherSense Settings") {
            input name: "appName", type: "text", title: "Name", defaultValue: "Feels Like Temperature", submitOnChange: true
            input name: "isOutdoor", type: "bool", title: "Outdoor Location?", defaultValue: true, submitOnChange: true
        }
        
        if (isOutdoor) {
            href "sensorPage", title: "Configure Sensors →", description: ""
        } else {
            section("Indoor Sensors (Required)") {
                input name: "temperatureSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, submitOnChange: true
                input name: "humiditySensor", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: true, submitOnChange: true
            }
        }
        
        section("Advanced Options") {
            input name: "displayUnit", type: "enum", title: "Display Unit", 
                   options: [["C": "Celsius (°C)"], ["F": "Fahrenheit (°F)"]], 
                   defaultValue: "C", submitOnChange: true
        }
    }
}

def sensorPage() {
    dynamicPage(name: "sensorPage", title: "Outdoor Sensors", install: true, uninstall: true) {
        section("Required Sensors") {
            input name: "temperatureSensor", type: "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, submitOnChange: true
            input name: "humiditySensor", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: true, submitOnChange: true
        }
        
        section("Optional Weather Sensors") {
            input name: "windSpeedSensor", type: "capability.illuminanceMeasurement", title: "Wind Speed Sensor (m/s)", required: false, submitOnChange: true
            input name: "pressureSensor", type: "capability.illuminanceMeasurement", title: "Pressure Sensor (kPa)", required: false, submitOnChange: true
            input name: "solarRadiationSensor", type: "capability.illuminanceMeasurement", title: "Solar Radiation", required: false, submitOnChange: true
            input name: "windDirectionSensor", type: "capability.illuminanceMeasurement", title: "Wind Direction (degrees)", required: false, submitOnChange: true
        }
        
        section("Advanced Features") {
            input name: "enableWindDirCorrection", type: "bool", title: "Enable Wind Direction Correction?", defaultValue: false, submitOnChange: true
            input name: "enableSmoothing", type: "bool", title: "Enable Smoothing?", defaultValue: false, submitOnChange: true
            input name: "smoothingFactor", type: "decimal", title: "Smoothing Factor (0.05-0.95)", defaultValue: 0.3, range: "0.05..0.95", submitOnChange: true
        }
        
        section {
            href "mainPage", title: "← Back to Main Settings"
        }
    }
}

// Installed callback
def installed() {
    log.debug "WeatherSense installed"
    initialize()
}

// Updated callback  
def updated(settings) {
    log.debug "WeatherSense updated: ${settings}"
    unsubscribe()
    initialize()
}

// Initialization
def initialize() {
    // Subscribe to sensor changes
    subscribe(temperatureSensor, "temperature", sensorHandler)
    subscribe(humiditySensor, "humidity", sensorHandler)
    if (windSpeedSensor) subscribe(windSpeedSensor, "illuminance", sensorHandler)  // Reuse illuminance for numeric values
    if (pressureSensor) subscribe(pressureSensor, "illuminance", sensorHandler)
    if (windDirectionSensor) subscribe(windDirectionSensor, "illuminance", sensorHandler)
    
    // Create child virtual sensor for feels-like temp
    def child = addChildDevice("hubitat", "Virtual Temperature Sensor", "${app.id}-feelslike", [name: "${app.label} FeelsLike", isComponent: false])
    state.childDeviceId = child.deviceNetworkId
    
    calculateAndUpdate()
}

// Main sensor event handler
def sensorHandler(evt) {
    log.debug "${evt.name} changed to ${evt.value}"
    runIn(2, calculateAndUpdate)  // Debounce
}

// Core calculation + state update
def calculateAndUpdate() {
    def temp = getSensorValue(temperatureSensor, "temperature")
    def humidity = getSensorValue(humiditySensor, "humidity")
    
    if (!temp || !humidity) {
        log.warn "Missing required sensors"
        return
    }
    
    def config = [
        temperature: temp,
        humidity: humidity,
        windSpeed: getSensorValue(windSpeedSensor, "illuminance"),
        pressure: getSensorValue(pressureSensor, "illuminance"),
        windDirection: getSensorValue(windDirectionSensor, "illuminance"),
        isOutdoor: isOutdoor ?: true,
        timeOfDay: new Date(),
        latitude: location.latitude,  // Hubitat location
        enableWindDirectionCorrection: enableWindDirCorrection ?: false
    ]
    
    def result = WeatherSenseCalculator.calculateFeelsLike(**config)
    
    // Update child device
    def childDni = state.childDeviceId
    def child = getChildDevice(childDni)
    if (child) {
        child.sendEvent(name: "temperature", value: result.feelsLike)
        child.sendEvent(name: "comfortLevel", value: result.comfortLevel)
        child.sendEvent(name: "calculationMethod", value: result.method)
    }
    
    // Update app state
    state.lastFeelsLike = result.feelsLike
    state.lastComfort = result.comfortLevel
    state.lastCalcTime = new Date()
    
    log.info "Feels Like: ${result.feelsLike}°C (${result.comfortLevel}) via ${result.method}"
}

// Helper to safely get sensor values
private BigDecimal getSensorValue(device, attribute) {
    if (!device) return null
    def val = device.currentValue(attribute)
    return val ? val.toBigDecimal() : null
}

// Dashboard tile support
attribute "feelsLikeTemp", "number"
attribute "comfortLevel", "string"

def getFeelsLikeTemp() { state.lastFeelsLike }
def getComfortLevel() { state.lastComfort }
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

    // Sensor attributes
    static final String ATTR_COMFORT_LEVEL              = "comfort_level"
    static final String ATTR_COMFORT_DESCRIPTION        = "comfort_description"
    static final String ATTR_COMFORT_EXPLANATION        = "comfort_explanation"
    static final String ATTR_CALCULATION_METHOD         = "calculation_method"
    static final String ATTR_TEMPERATURE                = "temperature"
    static final String ATTR_HUMIDITY                   = "humidity"
    static final String ATTR_WIND_SPEED                 = "wind_speed"
    static final String ATTR_PRESSURE                   = "pressure"
    static final String ATTR_IS_OUTDOOR                 = "is_outdoor"
    static final String ATTR_TIME_OF_DAY                = "time_of_day"
    static final String ATTR_IS_COMFORTABLE             = "is_comfortable"
    static final String ATTR_WIND_DIRECTION             = "wind_direction"
    static final String ATTR_WIND_DIRECTION_CORRECTION  = "wind_direction_correction_applied"
}
/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version).

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   https://github.com/smkrv/ha-weathersense
*/

import groovy.transform.Field

@Field static final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

class WeatherSenseCalculator {

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
            BigDecimal solarIntensity = Math.sin(Math.toRadians(Math.PI * hoursFromSunrise / 12))

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
        if (!pressure || pressure <= 0) {
            return feelsLike
        }

        if (pressure < 80 || pressure > 110) {
            return feelsLike
        }

        BigDecimal correction = 0.1G * (STANDARD_PRESSURE - pressure)
        return feelsLike + correction
    }

    static Map applyWindDirectionCorrection(BigDecimal feelsLike, BigDecimal windDirection, BigDecimal latitude = null, BigDecimal maxCorrection = 1.0G) {
        if (!windDirection) {
            return [feelsLike: feelsLike, correction: 0G]
        }

        // Normalize wind direction to 0-360 range
        windDirection = windDirection % 360

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
        BigDecimal windDirCorrection = 0G

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
                windDirCorrection = result.correction
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
            windDirCorrection: windDirCorrection
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
