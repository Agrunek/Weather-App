package com.app.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.weather.util.Constants.API_KEY
import com.app.weather.util.Constants.UPDATE_TASK_DELAY_MS
import com.app.weather.util.GeoApi
import com.app.weather.util.WeatherApi
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

private const val prefix = "https://api.openweathermap.org/data/2.5"

class WeatherViewModel(val initialImperial: Boolean) : ViewModel() {

    private val weatherLiveData: MutableLiveData<WeatherApi> by lazy { MutableLiveData() }

    val weather: LiveData<WeatherApi> get() = weatherLiveData

    init {
        initializeDataUpdateTask()
    }

    fun updateMeasurement(value: Boolean) {
        weatherLiveData.value?.let { weatherLiveData.value = it.apply { imperial = value } }
    }

    fun updateCity(city: GeoApi.City, weather: WeatherApi?) = viewModelScope.launch {
        weather?.let { weatherLiveData.postValue(it) }
        val currentWeather = fetchApiData(city)
        currentWeather?.let { weatherLiveData.postValue(it) }
    }

    private fun initializeDataUpdateTask() = viewModelScope.launch {
        while (isActive) {
            val currentWeather = fetchApiData(null)
            currentWeather?.let { weatherLiveData.postValue(it) }
            delay(UPDATE_TASK_DELAY_MS)
        }
    }

    private suspend fun fetchApiData(c: GeoApi.City?): WeatherApi? = withContext(Dispatchers.IO) {
        val lat = c?.lat ?: weather.value?.latitude ?: return@withContext null
        val lon = c?.lon ?: weather.value?.longitude ?: return@withContext null

        val weatherApi = URL("$prefix/weather?lat=$lat&lon=$lon&units=metric&appid=$API_KEY")
        val forecastApi = URL("$prefix/forecast?lat=$lat&lon=$lon&units=metric&appid=$API_KEY")
        val mapper = ObjectMapper()

        try {
            WeatherApi(
                mapper.readTree(weatherApi),
                mapper.readTree(forecastApi),
                weather.value?.imperial ?: initialImperial
            )
        } catch (e: IOException) {
            null
        }
    }

    class Factory(private val imperial: Boolean) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Boolean::class.java).newInstance(imperial)
        }
    }
}