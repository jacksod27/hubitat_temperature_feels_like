// Create ENHANCED Virtual Device (separate DTH):

definition(
    name: "WeatherSense Virtual Sensor",
    namespace: "weathersense",
    author: "SMKRV",
    description: "Rich feels-like sensor"
) {
    capability "Temperature Measurement"
    capability "Refresh"
    attribute "comfortLevel", "string"
    attribute "comfortDescription", "string" 
    attribute "comfortExplanation", "string"
    attribute "calculationMethod", "string"
    attribute "isComfortable", "boolean"
    attribute "inputTemperature", "number"
    attribute "inputHumidity", "number"
    attribute "inputWindSpeed", "number"
    attribute "inputPressure", "number"
    attribute "windDirCorrection", "number"
    attribute "dashboardCard", "string"  // ← NEW
    command "refresh"
}

def refresh() {
    parent?.calculateAndUpdate()
}
