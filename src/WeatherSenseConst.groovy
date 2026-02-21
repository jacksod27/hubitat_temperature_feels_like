/*
 Constants for the WeatherSense integration (Hubitat / Groovy version) - INLINE VERSION.

 Original: ha-weathersense (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   [https://github.com/smkrv/ha-weathersense](https://github.com/smkrv/ha-weathersense)
*/

// Domain / name
static final String WEATHERSENSE_DOMAIN = "weathersense"
static final String WEATHERSENSE_NAME   = "WeatherSense"

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
static final String DEFAULT_NAME                    = "Feels Like Temperature"
static final Boolean DEFAULT_IS_OUTDOOR             = true
static final BigDecimal DEFAULT_SMOOTHING_FACTOR    = 0.3G

// Comfort levels (short form)
static final String COMFORT_EXTREME_COLD   = "extreme_cold"
static final String COMFORT_VERY_COLD      = "very_cold"
static final String COMFORT_COLD           = "cold"
static final String COMFORT_COOL           = "cool"
static final String COMFORT_SLIGHTLY_COOL  = "slightly_cool"
static final String COMFORT_COMFORTABLE    = "comfortable"
static final String COMFORT_SLIGHTLY_WARM  = "slightly_warm"
static final String COMFORT_WARM           = "warm"
static final String COMFORT_HOT            = "hot"
static final String COMFORT_VERY_HOT       = "very_hot"
static final String COMFORT_EXTREME_HOT    = "extreme_hot"

// Comfort level descriptions
static final Map<String, String> COMFORT_DESCRIPTIONS = [
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
static final Map<String, String> COMFORT_EXPLANATIONS = [
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
static final Map<String, String> COMFORT_ICONS = [
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
