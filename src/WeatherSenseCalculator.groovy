/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version) - INLINE VERSION.

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   [https://github.com/smkrv/ha-weathersense](https://github.com/smkrv/ha-weathersense)
*/

static final BigDecimal STANDARD_PRESSURE = 101.3G  // kPa

static final Map COMFORT_LEVELS = [
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
