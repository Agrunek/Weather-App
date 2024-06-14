package com.app.weather.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.weather.R
import com.app.weather.WeatherViewModel
import com.app.weather.util.Constants.ICON_FOLDER_PATH
import com.app.weather.util.WeatherApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ForecastFragment : Fragment(R.layout.fragment_forecast) {

    private val formatter = DateTimeFormatter.ofPattern("EEE hh a")

    private val model: WeatherViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scrollview: RecyclerView = view.findViewById(R.id.forecast_scrollview)

        scrollview.layoutManager = LinearLayoutManager(view.context)

        model.weather.observe(viewLifecycleOwner) {
            scrollview.adapter = ForecastAdapter(it)
        }
    }

    private inner class ForecastAdapter(private val weather: WeatherApi) :
        RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.item_forecast, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(weather.forecast[position])
        }

        override fun getItemCount(): Int = weather.forecast.size

        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val iconWebView: WebView
            private val temperatureTextView: TextView
            private val datetimeTextView: TextView

            init {
                iconWebView = view.findViewById(R.id.item_forecast_icon_web_view)
                temperatureTextView = view.findViewById(R.id.item_forecast_temperature_text_view)
                datetimeTextView = view.findViewById(R.id.item_forecast_datetime_text_view)
            }

            fun bind(item: WeatherApi.ForecastApi) {
                val timestamp = Instant.ofEpochSecond(item.dateTime).atZone(ZoneId.systemDefault())

                iconWebView.loadUrl(ICON_FOLDER_PATH + "${item.iconCode}.svg")
                temperatureTextView.text = item.temperature
                datetimeTextView.text = timestamp.toLocalDateTime().format(formatter)
            }
        }
    }
}