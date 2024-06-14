package com.app.weather.util

import com.fasterxml.jackson.databind.JsonNode
import kotlin.math.roundToInt

class WeatherApi(val rootWeather: JsonNode, val rootForecast: JsonNode, var imperial: Boolean) {

    val city
        get() = rootWeather.get("name").asText()
    val latitude
        get() = rootWeather.get("coord").get("lat").asDouble()
    val longitude
        get() = rootWeather.get("coord").get("lon").asDouble()
    val description
        get() = rootWeather.get("weather").get(0).get("description").asText()
    val iconCode
        get() = rootWeather.get("weather").get(0).get("icon").asText()
    val temperature
        get() = translateTemperature(rootWeather.get("main").get("temp").asDouble())
    val temperaturePerception
        get() = translateTemperature(rootWeather.get("main").get("feels_like").asDouble())
    val pressure
        get() = rootWeather.get("main").get("pressure").asInt().toString().plus(" hPa")
    val humidity
        get() = rootWeather.get("main").get("humidity").asInt().toString().plus(" %")
    val windSpeed
        get() = rootWeather.get("wind").get("speed").asDouble().toString().plus(" m/s")

    val forecast = Array(rootForecast.get("cnt").asInt()) { i -> ForecastApi(i) }

    private fun translateTemperature(temperature: Double) = if (imperial) {
        temperature.times(1.8).plus(32).roundToInt().toString().plus(" °F")
    } else {
        temperature.roundToInt().toString().plus(" °C")
    }

    inner class ForecastApi(index: Int) {

        private val node = rootForecast.get("list").get(index)

        val dateTime
            get() = node.get("dt").asLong()
        val iconCode
            get() = node.get("weather").get(0).get("icon").asText()
        val temperature
            get() = translateTemperature(node.get("main").get("temp").asDouble())
    }
}