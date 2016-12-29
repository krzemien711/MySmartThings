/**
 *  MATLAB API
 *
 *  Copyright 2017 Kevin J Rzemien
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "MATLAB API",
    namespace: "krzemien711",
    author: "Kevin J. Rzemien",
    description: "MATLAB API for Smartthings",
    category: "",
    iconUrl: "http://www.robinbye.com/blog/wp-content/uploads/2010/04/Matlab_Logo.png",
    iconX2Url: "http://www.robinbye.com/blog/wp-content/uploads/2010/04/Matlab_Logo.png",
    iconX3Url: "http://www.robinbye.com/blog/wp-content/uploads/2010/04/Matlab_Logo.png",
    //iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    //iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "MATLAB API ", displayLink: "http://localhost:4567"])


preferences {
  section ("Allow external service to control these things...") {
    input "switches", "capability.switch", title: "Which switches do you want to control?", multiple: true, required: true
    input "tempSensor", "capability.temperatureMeasurement", title: "Which Temperature sensors do you want to control?", multiple: true, required: true
    input "garageDoor", "capability.garageDoorControl", title: "Which Garage Doors do you want to control?", multiple: false, required: true
  }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/tempSensors") {
    action: [
      GET: "listTemps"
    ]
  }
  path("/garageDoor/:command") {
    action: [
      PUT: "updategarageDoor"
    ]
  }
}

def initialize() {
    
}


def getTemps(evt) {

    def currentState = tempSensor.currentState("temperature")
    log.debug "temperature value as a string: ${currentState.value}"
    log.debug "time this temperature record was created: ${currentState.date}"

    // shortcut notation - temperature measurement capability supports
    // a "temperature" attribute. We then append "State" to it.
    def anotherCurrentState = tempSensor.temperatureState
    log.debug "temperature value as an integer: ${anotherCurrentState.integerValue}"
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {

    def resp = []
    resp << [name: switches.displayName, value: switches.currentValue("switch")]
    log.debug "listSwitches called, settings: ${settings}"
    return resp
}

def listTemps() {

    def resp = []
    def currentState = tempSensor.currentState("temperature")
    //resp << [name: tempSensor.displayName, value: tempSensor.currentValue("temperature"), timestamp: ]
    resp << [name: tempSensor.displayName, value: currentState.value, timestamp: currentState.date]
    return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command
    def value = []
    def tmp = []
    
    log.debug "Switches command = '$command'"

    if (command?.contains(':')) {
        tmp = command.split(":")
        command = tmp[0].trim()
        value = tmp[1].trim()
    }
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "level":
            switches.setLevel(value.toInteger())
            break
        case "hue":
            switches.setHue(value.toInteger())
            break
        case "sat":
            switches.setSaturation(value.toInteger())
            break
        case "temp":
            switches.setColorTemperature(value.toInteger())
            break
        case "on":
            switches.on()
            break
        case "off":
            switches.off()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }

}

void updategarageDoor() {
    def command = params.command
    
    log.debug "Garage Door commanded $command"
    
    switch(command) {
        case "open":
            garageDoor.open()
            break
        case "close":
            garageDoor.close()
            break
        default:
            httpError(400, "$command is not a valid command for the Garage Door")
    }
}
    
def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe()
    //switches.each { switches ->
    	// log each child
    //	log.debug "switches: $switches"
    //}
}

def updated() {
	log.trace "Updated with settings: ${settings}"
	unsubscribe()
	subscribe()
}

def subscribe() {
	// log.debug 
	subscribe(tempSensor, "temperature", getTemps)
    log.trace "Subscribed"
}