/*
 Weather calculation functions for Hubitat WeatherSense (Groovy version) - INLINE VERSION.

 Original: ha-weathersense/weather_calculator.py (Home Assistant)
 License:  CC BY-NC-SA 4.0 International
 Author:   SMKRV
 Source:   [https://github.com/smkrv/ha-weathersense](https://github.com/smkrv/ha-weathersense)
*/

final BigDecimal STANDARD_PRESSURE = 101.3  // kPa

def calculateHeatIndex(BigDecimal temperature, BigDecimal humidity) {
    BigDecimal tF = (temperature * 9/5) + 32
    BigDecimal rh = humidity ?: 50

    BigDecimal hiF
    if (rh < 40 || tF < 80) {
        hiF = 0.5 * (tF + 61.0 + ((tF - 68.0) * 1.2) + (rh * 0.094))
        hiF = (hiF + tF) / 2
    } else {
        hiF = -42.379
        hiF += 2.04901523 * tF
        hiF += 10.14333127 * rh
        hiF -= 0.22475541 * tF * rh
        hiF -= 0.00683783 * tF * tF
        hiF -= 0.05481717 * rh * rh
        hiF += 0.00122874 * tF * tF * rh
        hiF += 0.00085282 * tF * rh * rh
        hiF -= 0.00000199 * tF * tF * rh * rh

        if (rh < 13 && tF >= 80 && tF <= 112) {
            BigDecimal adjustment = ((13 - rh) / 4) * Math.sqrt((17 - Math.abs(tF - 95)) / 17)
            hiF -= adjustment
        } else if (rh > 85 && tF >=
