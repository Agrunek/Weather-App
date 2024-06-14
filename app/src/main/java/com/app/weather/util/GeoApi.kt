package com.app.weather.util

import com.fasterxml.jackson.databind.JsonNode

class GeoApi(private val rootGeo: JsonNode) {

    val cities = Array(rootGeo.count()) { i -> City(i) }

    inner class City(index: Int) {

        private val node = rootGeo.get(index)

        val name
            get() = node.get("name").asText()
        val lat
            get() = node.get("lat").asDouble()
        val lon
            get() = node.get("lon").asDouble()
        val country
            get() = node.get("country").asText()
        val state
            get() = node.get("state")?.asText()
    }
}