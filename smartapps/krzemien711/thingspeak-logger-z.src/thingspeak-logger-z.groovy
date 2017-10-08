/**
 * My Thingspeak Logger
 *  Copyright 2017 Kevin J. Rzemien
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
    name: "Thingspeak Logger Z",
    namespace: "krzemien711",
    author: "Kevin J. Rzemien",
    description: "Thingspeak Logger Z",
    category: "My Apps",
    iconUrl: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX2Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX3Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg")

preferences {
    section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required:false, multiple: true
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminances", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
        input "thermostats", "capability.thermostat", title: "Select thermostat", required: false, multiple: true
    }
    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel ID"
    }

    section ("ThinkSpeak write key...") {
        input "channelKey", "password", title: "Channel Key"
    }
    
    section ("Debug mode?") {
        input "debugOn", "number", range: "0..1", title: "Debug Mode"
    }    
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(temperatures, "temperature", handleTemperatureEvent)
    subscribe(humidities, "humidities", handlehumidityEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(switches, "switch", handleSwitchEvent)
    subscribe(thermostats, "temperature", handleThermostatTemperature)
    subscribe(thermostats, "humidity", handleThermostatHumidity)
    

    updateChannelInfo()
    if (debugOn) {log.debug "State: ${state.fieldMap}"}
}


def handleTemperatureEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    //log.debug "Device ID is $evt.deviceId"
    
    logField("temperature", evt) { it.toString() }
}
def handlehumidityEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    
    logField("humidity", evt) { it.toString() }
}
def handleContactEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    logField("contact", evt) { it == "open" ? "1" : "0" }
}
def handleAccelerationEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    logField("acceleration", evt) { it == "active" ? "1" : "0" }
}
def handleMotionEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    logField("motion", evt) { it == "active" ? "1" : "0" }
}
def handleSwitchEvent(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    logField("switch", evt) { it == "on" ? "1" : "0" }
}
def handleThermostatTemperature(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    if (debugOn) {log.debug "TStat Temperature event: $evt.value"}
    logField("thermostat.temperature", evt) { it.toString() }
}
def handleThermostatHumidity(evt) {
    if (debugOn) {log.debug "EVENT is $evt.displayName"}
    if (debugOn) {log.debug "EVENT VALUE is $evt.value"}
    if (debugOn) {log.debug "TStat Humidity event: $evt.value"}
    logField("thermostat.humidity", evt) { it.toString() }
}



private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    return fieldMap
}


private updateChannelInfo() {
    if (debugOn) {log.debug "Retrieving channel info for ${channelId}"}
    def url = "https://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            if (debugOn) {log.debug "ThingSpeak data retrieval failed, status = ${response.status}"}
        } else {
        	if (debugOn) {log.debug "ThingSpeak data retrieval successful, status = ${response.status}"}
            state.channelInfo = response.data?.channel
            if (debugOn) {log.debug "ThingSpeak data = ${state.channelInfo}"}
        }
        //log.debug response.data
    }
    state.fieldMap = getFieldMap(state.channelInfo)
    if (debugOn) {log.debug "State fieldmap = ${state.fieldMap}"}
}



private logField(capability, evt, Closure c) {
    if (debugOn) {log.debug "Got Log Request: $capability $evt.displayName $evt.value"}
    def deviceName = evt.displayName.trim() + "." + capability
    def fieldNum = state.fieldMap[deviceName]
    if (!fieldNum) {
        if (debugOn) {log.debug "Device '${deviceName}' has no field"}
        return
    }
    def value = c(evt.value)
    if (debugOn) {log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"}
    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}&tstream=true"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            if (debugOn) {log.debug "ThingSpeak logging failed, status = ${response.status}"}
        }
    }
}
