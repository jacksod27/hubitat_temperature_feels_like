#!/usr/bin/env groovy

println "=== WeatherSense Build v1.0 ==="

def TIMESTAMP = new Date().format("yyyy-MM-dd HH:mm:ss")

// Source files (build order matters!)
def sourceFiles = [
    "WeatherSenseConst.groovy",
    "WeatherSenseCalculator.groovy", 
    "WeatherSenseAppMain.groovy"
]

def srcDir = new File("src")
def appOutDir = new File("apps")
def driverOutDir = new File("drivers")

// Ensure directories
[appOutDir, driverOutDir].each { dir ->
    if (!dir.exists()) {
        println "📁 Creating: $dir"
        dir.mkdirs()
    }
}

// === BUILD APP ===
def appOutFile = new File(appOutDir, "WeatherSense-App.groovy")
println "\n📱 Building APP:"
def appContent = sourceFiles.collect { name ->
    def file = new File(srcDir, name)
    if (!file.exists()) error("❌ Missing: ${srcDir}/${name}")
    println "  📄 $name (${file.length()} bytes)"
    return file.text
}.join("\n\n/* ───────────────────────── */\n\n")

// Add header
def header = """/*
 WeatherSense App v1.0 (AUTO-BUILT)
 Built: ${TIMESTAMP}
 Source: https://github.com/YOURUSER/ha-weathersense-hubitat
 License: CC BY-NC-SA 4.0
*/\n\n"""
appOutFile.text = header + appContent
println "✅ APP: ${appOutFile} (${appContent.length()} chars)"


// === COPY DRIVER (already single-file) ===
def driverSrc = new File("drivers-src/WeatherSense-Virtual.groovy")
def driverOut = new File(driverOutDir, "WeatherSense-Virtual.groovy")
if (driverSrc.exists()) {
    driverOut.text = driverSrc.text
    println "✅ DRIVER: ${driverOut} (${driverOut.length()} chars)"
} else {
    println "⚠️  Driver source missing - copy manually"
}

// === VALIDATION ===
def appLines = appOutFile.readLines().size()
println """
=== BUILD SUMMARY ===
📱 App:          ${appOutFile} (${appLines} lines)
🚗 Driver:       ${driverOut}
⏰ Built:        ${TIMESTAMP}
🚀 Ready for HPM!

Next: git add apps/ drivers/ && git commit -m "Build $TIMESTAMP"
"""

def error(String msg) {
    println "❌ BUILD FAILED: $msg"
    System.exit(1)
}
