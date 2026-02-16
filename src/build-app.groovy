// Concat: Const.groovy + Calculator.groovy + AppMain.groovy → WeatherSense-App.groovy
def files = ['WeatherSenseConst.groovy', 'WeatherSenseCalculator.groovy', 'WeatherSenseAppMain.groovy']
def combined = files.collect { readFile(it) }.join('\n\n')
writeFile('apps/WeatherSense-App.groovy', combined)
