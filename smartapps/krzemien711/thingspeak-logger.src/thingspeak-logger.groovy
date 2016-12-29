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
    /*
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    */
    iconUrl: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX2Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg",
    iconX3Url: "https://pbs.twimg.com/profile_images/805850886417174528/EhFtRCsF.jpg")


preferences {
    section("Log devices...") {
        input "temps", "capability.temperatureMeasurement", title: "Temperature Sensors", required: false, multiple: true
        // input "energy", "capability.energyMeter", title: "Energy", required: false, multiple: true
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

    updateChannelInfo()
    log.debug "State: ${state.fieldMap}"
}

def handleTempEvent(evt) {
	log.debug "Temperature Event Name: $evt.displayName"
    log.debug "Temperature Event Attribute: $evt.name"
    logField(evt,"field1") { it.toString() }

}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    log.debug "Retrieving map info for ${fieldMap}"
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "http://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
            state.channelInfo = response.data?.channel
            log.debug "state.channelInfo = $state.channelInfo"
        }
    }

    state.fieldMap = getFieldMap(state.channelInfo)
    log.debug "state.fieldMap = $state.fieldMap"
}

private logField(evt, field, Closure c) {
    //def deviceName = evt.displayName.trim() + ':' + field
    //def fieldNum = state.fieldMap[deviceName]
    //def fieldNum = "field1"
    def deviceName = evt.displayName.trim()
    def fieldNum = state.fieldMap[deviceName]

    if (!fieldNum) {
        log.debug "Device '${deviceName}' has no field"
        log.debug "fieldNum = '${fieldNum}'"
        return
    }
    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
}