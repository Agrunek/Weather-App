package com.app.weather.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.weather.R
import com.app.weather.WeatherViewModel
import com.app.weather.dataStore
import com.app.weather.util.Constants
import com.app.weather.util.Constants.API_KEY
import com.app.weather.util.Constants.FAVOURITE_DATA_STORE_KEY
import com.app.weather.util.Constants.IMPERIAL_DATA_STORE_KEY
import com.app.weather.util.GeoApi
import com.app.weather.util.WeatherApi
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

private const val prefix = "https://api.openweathermap.org/geo/1.0"

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val favouriteCities: MutableList<GeoApi.City> by lazy { retrieveFavouriteCitiesSynchronously() }

    private val model: WeatherViewModel by activityViewModels()

    private lateinit var scrollview: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val measurementSwitch: SwitchMaterial = view.findViewById(R.id.settings_measurement_switch)
        val dialogOpenButton: FloatingActionButton = view.findViewById(R.id.settings_dialog_button)

        measurementSwitch.isChecked = model.initialImperial
        measurementSwitch.setOnCheckedChangeListener { _, value ->
            storeImperial(value)
            model.updateMeasurement(value)
        }

        dialogOpenButton.setOnClickListener {
            val editText = EditText(requireContext())
            MaterialAlertDialogBuilder(requireContext()).setTitle("Enter a city:").setView(editText)
                .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }
                .setPositiveButton("SEARCH") { _, _ -> searchForCities(editText.text.toString()) }
                .show()
        }

        scrollview = view.findViewById(R.id.settings_scrollview)
        scrollview.layoutManager = LinearLayoutManager(view.context)
        scrollview.adapter = SettingsAdapter(favouriteCities)
    }

    private fun storeImperial(value: Boolean) = lifecycleScope.launch {
        requireContext().dataStore.edit {
            it[booleanPreferencesKey(IMPERIAL_DATA_STORE_KEY)] = value
        }
    }

    private fun retrieveWeatherFromStorage(city: GeoApi.City) = lifecycleScope.launch {
        context?.let { ctx ->
            val weatherApi = try {
                val rawWeather =
                    ctx.openFileInput("WEATHER:${city.name}").bufferedReader().useLines {
                        it.fold("") { sum, str -> sum.plus(str) }
                    }

                val rawForecast =
                    ctx.openFileInput("FORECAST:${city.name}").bufferedReader().useLines {
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

        storeCurrent(city)
    }

    private fun searchForCities(name: String) = lifecycleScope.launch {
        val data = fetchApiData(name)
        data?.let { processGeoApiData(it) }
    }

    private fun addFavouriteCity(city: GeoApi.City) {
        favouriteCities.add(city)
        scrollview.adapter = SettingsAdapter(favouriteCities)
        storeFavouriteCities(favouriteCities)
    }

    private fun removeFavouriteCity(city: GeoApi.City) {
        favouriteCities.remove(city)
        scrollview.adapter = SettingsAdapter(favouriteCities)
        storeFavouriteCities(favouriteCities)
    }

    private fun processGeoApiData(data: GeoApi) {
        if (data.cities.isEmpty()) {
            Toast.makeText(requireContext(), "No cities found!", Toast.LENGTH_SHORT).show()
            return
        }

        val mappedCities = data.cities.map {
            "${it.name} ( ${it.country.plus(if (it.state != null) ", ${it.state}" else "")} )"
        }.toTypedArray()

        lateinit var addButton: Button
        var checkedItem = -1

        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle("Found cities: ")
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }
            .setPositiveButton("ADD") { _, _ -> addFavouriteCity(data.cities[checkedItem]) }
            .setSingleChoiceItems(mappedCities, checkedItem) { _, which ->
                checkedItem = which
                addButton.isEnabled = true
            }.create()

        dialog.show()
        addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        addButton.isEnabled = false
    }

    private suspend fun fetchApiData(name: String): GeoApi? = withContext(Dispatchers.IO) {
        val geoApi = URL("$prefix/direct?q=$name&limit=5&appid=$API_KEY")
        val mapper = ObjectMapper()

        try {
            GeoApi(mapper.readTree(geoApi))
        } catch (e: IOException) {
            null
        }
    }

    private fun storeCurrent(city: GeoApi.City) = lifecycleScope.launch {
        val writer = ObjectMapper().writer().withDefaultPrettyPrinter()
        val json = writer.writeValueAsString(listOf(city))
        requireContext().dataStore.edit {
            it[stringPreferencesKey(Constants.CURRENT_DATA_STORE_KEY)] = json
        }
    }

    private fun retrieveFavouriteCitiesSynchronously(): MutableList<GeoApi.City> = runBlocking {
        val key = stringPreferencesKey(FAVOURITE_DATA_STORE_KEY)
        val raw = requireContext().dataStore.data.first()[key]
        val geoApi = GeoApi(ObjectMapper().readTree(raw ?: "[]"))
        val translated: MutableList<GeoApi.City> = mutableListOf()
        geoApi.cities.forEach { translated.add(it) }
        translated
    }

    private fun storeFavouriteCities(favourites: MutableList<GeoApi.City>) = lifecycleScope.launch {
        val writer = ObjectMapper().writer().withDefaultPrettyPrinter()
        val json = writer.writeValueAsString(favourites)
        requireContext().dataStore.edit {
            it[stringPreferencesKey(FAVOURITE_DATA_STORE_KEY)] = json
        }
    }

    private inner class SettingsAdapter(private val favouriteCities: MutableList<GeoApi.City>) :
        RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_settings, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(favouriteCities[position])
        }

        override fun getItemCount(): Int = favouriteCities.size

        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val cardView: MaterialCardView
            private val nameTextView: TextView
            private val deleteButton: ImageButton

            init {
                cardView = view.findViewById(R.id.item_settings)
                nameTextView = view.findViewById(R.id.item_settings_name_text_view)
                deleteButton = view.findViewById(R.id.item_settings_delete_button)
            }

            fun bind(item: GeoApi.City) {
                cardView.setOnClickListener {
                    retrieveWeatherFromStorage(item)
                }

                nameTextView.text = item.name

                deleteButton.setOnClickListener {
                    removeFavouriteCity(item)
                }
            }
        }
    }
}