package com.app.weather

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.app.weather.fragment.ForecastFragment
import com.app.weather.fragment.SettingsFragment
import com.app.weather.fragment.WeatherFragment
import com.app.weather.util.Constants.CURRENT_DATA_STORE_KEY
import com.app.weather.util.Constants.IMPERIAL_DATA_STORE_KEY
import com.app.weather.util.GeoApi
import com.app.weather.util.WeatherApi
import com.app.weather.util.ZoomOutPageTransformer
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {

    private val model: WeatherViewModel by viewModels {
        WeatherViewModel.Factory(retrieveImperialSynchronously())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        retrieveCurrentSynchronously()?.let { retrieveWeatherFromStorage(it) }

        model.weather.observe(this) { api ->
            openFileOutput("WEATHER:${api.city}", Context.MODE_PRIVATE).use {
                it.write(api.rootWeather.toString().toByteArray())
            }

            openFileOutput("FORECAST:${api.city}", Context.MODE_PRIVATE).use {
                it.write(api.rootForecast.toString().toByteArray())
            }

            Toast.makeText(this, "Weather updated!", Toast.LENGTH_SHORT).show()
        }

        if (resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            val carousel: ViewPager2 = findViewById(R.id.main_carousel)
            val fragments = listOf(WeatherFragment(), ForecastFragment(), SettingsFragment())

            carousel.setPageTransformer(ZoomOutPageTransformer())
            carousel.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = fragments.size
                override fun createFragment(position: Int): Fragment = fragments[position]
            }

            val carouselDots: TabLayout = findViewById(R.id.main_carousel_dots)
            TabLayoutMediator(carouselDots, carousel) { _, _ -> }.attach()
        }
    }

    private fun retrieveImperialSynchronously(): Boolean = runBlocking {
        dataStore.data.first()[booleanPreferencesKey(IMPERIAL_DATA_STORE_KEY)] ?: false
    }

    private fun retrieveCurrentSynchronously(): GeoApi.City? = runBlocking {
        val key = stringPreferencesKey(CURRENT_DATA_STORE_KEY)
        val raw = dataStore.data.first()[key] ?: return@runBlocking null
        GeoApi(ObjectMapper().readTree(raw)).cities[0]
    }

    private fun retrieveWeatherFromStorage(city: GeoApi.City) = lifecycleScope.launch {
        val weatherApi = try {
            val rawWeather = openFileInput("WEATHER:${city.name}").bufferedReader().useLines {
                it.fold("") { sum, str -> sum.plus(str) }
            }

            val rawForecast = openFileInput("FORECAST:${city.name}").bufferedReader().useLines {
                it.fold("") { sum, str -> sum.plus(str) }
            }

            val mapper = ObjectMapper()

            WeatherApi(
                mapper.readTree(rawWeather),
                mapper.readTree(rawForecast),
                model.weather.value?.imperial ?: model.initialImperial
            )
        } catch (e: FileNotFoundException) {
            null
        }

        model.updateCity(city, weatherApi)
    }
}