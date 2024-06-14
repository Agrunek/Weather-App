package com.app.weather.fragment

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.weather.R
import com.app.weather.WeatherViewModel
import com.app.weather.util.Constants.ICON_FOLDER_PATH

class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private val model: WeatherViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cityTextView: TextView = view.findViewById(R.id.weather_city_text_view)
        val temperatureTextView: TextView = view.findViewById(R.id.weather_temperature_text_view)
        val iconWebView: WebView = view.findViewById(R.id.weather_icon_web_view)
        val descriptionTextView: TextView = view.findViewById(R.id.weather_description_text_view)
        val windSpeedIconWebView: WebView = view.findViewById(R.id.weather_wind_speed_icon_web_view)
        val windSpeedTextView: TextView = view.findViewById(R.id.weather_wind_speed_text_view)
        val pressureIconWebView: WebView = view.findViewById(R.id.weather_pressure_icon_web_view)
        val pressureTextView: TextView = view.findViewById(R.id.weather_pressure_text_view)
        val humidityIconWebView: WebView = view.findViewById(R.id.weather_humidity_icon_web_view)
        val humidityTextView: TextView = view.findViewById(R.id.weather_humidity_text_view)
        val feelsLikeIconWebView: WebView = view.findViewById(R.id.weather_feels_like_icon_web_view)
        val feelsLikeTextView: TextView = view.findViewById(R.id.weather_feels_like_text_view)

        iconWebView.loadUrl(ICON_FOLDER_PATH + "not-available.svg")
        windSpeedIconWebView.loadUrl(ICON_FOLDER_PATH + "windsock.svg")
        pressureIconWebView.loadUrl(ICON_FOLDER_PATH + "barometer.svg")
        humidityIconWebView.loadUrl(ICON_FOLDER_PATH + "raindrops.svg")
        feelsLikeIconWebView.loadUrl(ICON_FOLDER_PATH + "thermometer.svg")

        model.weather.observe(viewLifecycleOwner) {
            cityTextView.text = it.city
            temperatureTextView.text = it.temperature
            iconWebView.loadUrl(ICON_FOLDER_PATH + "${it.iconCode}.svg")
            descriptionTextView.text = it.description.replaceFirstChar(Char::titlecase)
            windSpeedTextView.text = it.windSpeed
            pressureTextView.text = it.pressure
            humidityTextView.text = it.humidity
            feelsLikeTextView.text = it.temperaturePerception
        }
    }
}