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
