/*
 WeatherSense Virtual Sensor v1.0
 =================================
 HA WeatherSense → Hubitat Port
 License: CC BY-NC-SA 4.0 International
 Repo: https://github.com/YOURUSER/ha-weathersense-hubitat
*/

metadata {
    definition (name: "WeatherSense Virtual Sensor", namespace: "weathersense", author: "SMKRV (Hubitat port)") {
        capability "Temperature Measurement"
        capability "Refresh"
        capability "Sensor"
        
        // Core attributes
        attribute "comfortLevel", "string"
        attribute "comfortDescription", "string"
        attribute "comfortExplanation", "string"
        attribute "calculationMethod", "string"
        attribute "isComfortable", "boolean"
        
        // Input values
        attribute "inputTemperature", "number"
        attribute "inputHumidity", "number" 
        attribute "inputWindSpeed", "number"
        attribute "inputPressure", "number"
        attribute "windDirCorrection", "number"
        
        // DASHBOARD CARD ✨
        attribute "dashboardCard", "string"
        
        // Dashboard status
        attribute "dashboardStatus", "string"
    }
    
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable description text?", defaultValue: true
        input "tempOffset", "number", title: "Temperature Offset", description: "Adjust temperature by this many degrees", range: "*..*", defaultValue: 0
    }
}

def installed() {
    log.debug "WeatherSense Virtual Sensor installed"
    sendEvent(name: "comfortLevel", value: "comfortable")
}

def updated() {
    log.debug "WeatherSense Virtual Sensor updated"
}

def refresh() {
    log.debug "Refresh requested from parent"
    parent.calculateAndUpdate()
}

def parse(String description) {
    // Not used - parent updates directly
}

// Called by parent app
def updateDashboardData(Map data) {
    log.debug "Updating dashboard data: ${data.comfortLevel}"
    
    // Temperature (primary display)
    sendEvent(name: "temperature", value: data.feelsLike)
    
    // Comfort info
    sendEvent(name: "comfortLevel", value: data.comfortLevel)
    sendEvent(name: "comfortDescription", value: data.comfortDesc)
    sendEvent(name: "comfortExplanation", value: data.comfortExpl)
    sendEvent(name: "calculationMethod", value: data.method)
    sendEvent(name: "isComfortable", value: data.isComfortable)
    
    // Input values
    sendEvent(name: "inputTemperature", value: data.inputTemp)
    sendEvent(name: "inputHumidity", value: data.inputHumidity)
    sendEvent(name: "inputWindSpeed", value: data.inputWindSpeed)
    sendEvent(name: "inputPressure", value: data.inputPressure)
    sendEvent(name: "windDirCorrection", value: data.windDirCorrection)
    
    // Dashboard summary
    sendEvent(name: "dashboardStatus", value: "${data.feelsLike.round(1)}°C (${data.comfortLevel})")
    
    // GLASSMORPHISM DASHBOARD CARD ✨
    sendEvent(name: "dashboardCard", value: buildDashboardCard(data))
}

private String buildDashboardCard(Map data) {
    def colors = getComfortColors(data.comfortLevel)
    def icon = getComfortIcon(data.comfortLevel)
    def isComfy = data.isComfortable ? "Comfy" : "Not"
    
    return """
<div class="ws-card" 
     style="--bg: ${colors.bg}; --light: ${colors.light}; --text: ${colors.text};">
  <div class="ws-header">
    <div>WeatherSense</div>
    <div class="ws-badge">${isComfy}</div>
  </div>
  <div class="ws-main">
    <div class="ws-icon">$icon</div>
    <div class="ws-temp">
      <span class="ws-value">${data.feelsLike.round(1)}</span>
      <span class="ws-unit">°C</span>
    </div>
  </div>
  <div class="ws-details">
    <div class="ws-row">
      <span>Temp:</span> <span>${data.inputTemp?.round(1)}°C</span>
    </div>
    <div class="ws-row">
      <span>RH:</span> <span>${data.inputHumidity?.round(1)}%</span>
    </div>
  </div>
  <div class="ws-status">${data.comfortDesc}</div>
</div>

<style>
.ws-card {
  --bg: #10b981; --light: #86efac; --text: #059669;
  padding: 20px; border-radius: 24px;
  background: linear-gradient(135deg, var(--bg)20 0%, var(--bg)10 100%);
  backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.2);
  font-family: system-ui, -apple-system, sans-serif;
  color: var(--text); height: 100%; display: flex; flex-direction: column;
  gap: 12px; box-sizing: border-box;
}
.ws-header { display: flex; justify-content: space-between; font-size: 14px; opacity: 0.9; }
.ws-badge { 
  padding: 4px 12px; border-radius: 20px; background: var(--light)40; 
  font-size: 12px; font-weight: 600; border: 1px solid var(--light)60;
}
.ws-main { display: flex; align-items: center; gap: 16px; }
.ws-icon { 
  font-size: 48px; width: 64px; height: 64px; 
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; background: var(--light)30;
}
.ws-temp { display: flex; align-items: baseline; gap: 4px; }
.ws-value { font-size: 42px; font-weight: 200; line-height: 1; }
.ws-unit { font-size: 20px; opacity: 0.8; font-weight: 300; }
.ws-details { display: flex; flex-direction: column; gap: 8px; font-size: 13px; opacity: 0.8; }
.ws-row { display: flex; justify-content: space-between; }
.ws-status { 
  font-size: 12px; opacity: 0.7; 
  border-top: 1px solid var(--light)20; padding-top: 8px; 
}
</style>
"""
}

private Map getComfortColors(String level) {
    def colors = [
        "extreme_cold": [bg: "#1e3a8a", light: "#3b82f6", text: "#1e40af"],
        "very_cold": [bg: "#1e40af", light: "#60a5fa", text: "#1e40af"],
        "cold": [bg: "#2563eb", light: "#93c5fd", text: "#1d4ed8"],
        "cool": [bg: "#0891b2", light: "#67e8f9", text: "#0e7490"],
        "slightly_cool": [bg: "#0d9488", light: "#5eead4", text: "#0f766e"],
        "comfortable": [bg: "#10b981", light: "#86efac", text: "#059669"],
        "slightly_warm": [bg: "#f59e0b", light: "#fcd34d", text: "#d97706"],
        "warm": [bg: "#f97316", light: "#fb923c", text: "#ea580c"],
        "hot": [bg: "#ef4444", light: "#f87171", text: "#dc2626"],
        "very_hot": [bg: "#dc2626", light: "#f87171", text: "#b91c1c"],
        "extreme_hot": [bg: "#991b1b", light: "#ef4444", text: "#7f1d1d"]
    ]
    return colors.get(level, colors.comfortable)
}

private String getComfortIcon(String level) {
    def icons = [
        "extreme_cold": "❄️",
        "very_cold": "🧊", 
        "cold": "🥶",
        "cool": "❄️",
        "slightly_cool": "🌡️",
        "comfortable": "😊",
        "slightly_warm": "🌡️",
        "warm": "🥵",
        "hot": "🔥",
        "very_hot": "🌡️",
        "extreme_hot": "☀️"
    ]
    return icons.get(level, "🌡️")
}
