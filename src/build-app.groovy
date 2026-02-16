#!/usr/bin/env groovy
// Concat src/ → single deployable files

def files = [
    'WeatherSenseConst.groovy',
    'WeatherSenseCalculator.groovy', 
    'WeatherSenseAppMain.groovy'  // Main app logic
]

def appContent = files.collect { new File("src/$it").text }.join('\n\n\n/* ============ */\n\n')

new File('apps/WeatherSense-App.groovy').text = """/*
 AUTO-GENERATED - do not edit directly
 Built: ${new Date()}
*/\n\n$appContent"""

println "✅ Built apps/WeatherSense-App.groovy (${appContent.size()} chars)"
