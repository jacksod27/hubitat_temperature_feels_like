#!/usr/bin/env groovy

println "=== WeatherSense Build Script Starting ==="

// Define the source files (in order)
def sourceFiles = [
    "WeatherSenseConst.groovy",
    "WeatherSenseCalculator.groovy",
    "WeatherSenseAppMain.groovy"
]

// Build paths
def srcDir  = new File("src")
def outDir  = new File("apps")
def outFile = new File(outDir, "WeatherSense-App.groovy")

// Ensure output directory exists
if (!outDir.exists()) {
    println "Creating output directory: ${outDir}"
    outDir.mkdirs()
}

// Read + combine source files
println "Combining source files:"
def combined = sourceFiles.collect { name ->
    def file = new File(srcDir, name)
    if (!file.exists()) {
        throw new RuntimeException("Missing source file: ${file}")
    }
    println " - ${file}"
    return file.text
}.join("\n\n")

// Write output file
println "Writing output to: ${outFile}"
outFile.text = combined

println "=== Build Complete ==="
