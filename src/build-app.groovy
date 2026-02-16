def apps = [
    [
        sources: [
            'WeatherSenseConst.groovy',
            'WeatherSenseCalculator.groovy',
            'WeatherSenseAppMain.groovy'
        ],
        output: 'apps/WeatherSense-App.groovy'
    ]
]

apps.each { app ->
    def combined = app.sources.collect { src ->
        new File("src/${src}").text
    }.join("\n\n")

    def outFile = new File(app.output)
    outFile.parentFile.mkdirs()
    outFile.text = combined

    println "Built: ${app.output}"
}
