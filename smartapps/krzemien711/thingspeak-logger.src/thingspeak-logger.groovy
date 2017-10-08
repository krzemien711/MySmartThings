/**
 *  Thingspeak Logger
 *
 *  Copyright 2015 Kevin J. Rzemien
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
 *
 */
definition(
    name: "Thingspeak Logger",
    namespace: "krzemien711",
    author: "Kevin J. Rzemien",
    description: "Thingspeak Logger",
    category: "My Apps",
    iconUrl: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX2Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX3Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg")


preferences {
    section("Log devices...") {
        input "temps", "capability.temperatureMeasurement", title: "Temperature Sensors", required: false, multiple: true
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true        
    }

    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel id"
    }

    section ("ThinkSpeak write key...") {
        input "channelKey", "text", title: "Channel key"
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
    subscribe(temps, "temperature", handleTempEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    //log.debug "Done with subscribe"

    updateChannelInfo()
    log.debug "State: ${state.fieldMap}"
}

def handleTempEvent(evt) {
	log.debug "Temperature Event Name: $evt.displayName"
    log.debug "Temperature Event Attribute: $evt.name"
    logField(evt) { it.toString() }

}

def handleContactEvent(evt) {
    logField(evt) { it == "open" ? "1" : "0" }
}

def handleAccelerationEvent(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    //channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim().toLowerCase()+1] = it.key }
    //log.debug "Key = ${it.key}"
    log.debug "Retrieving map info for ${fieldMap}"
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "https://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
        	log.debug "ThingSpeak data retrieval successful, status = ${response.status}"
            state.channelInfo = response.data?.channel
            log.debug "ThingSpeak data = ${state.channelInfo}"
        }
    }

    state.fieldMap = getFieldMap(state.channelInfo)
    log.debug "State fieldmap = ${state.fieldMap}"
}

private logField(evt, Closure c) {
    //def deviceName = evt.displayName.trim()
    def deviceName = evt.displayName.trim().toLowerCase()
    log.debug "Device Name = '${deviceName}'"
    def fieldNum = state.fieldMap[deviceName]

    if (!fieldNum) {
        log.debug "Device '${deviceName}' has no field"
        //log.debug "fieldNum = '${fieldNum}'"
        return
    }
    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}&tstream=true"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
}