package com.headsupwatchface


import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Weather model to represent the API call result in a nice Kotlin class
 */
object WeatherModel {
    data class Result(
            val lat: String, val lon: String,
            val current: CurrentWeather,
//            val minutely: MinutelyWeather,
//            val hourly: HourlyWeather,
            )
    data class CurrentWeather(
            val dt: Long, val temp: Float)
}

enum class WeatherQueryStatus{
    UNINITIALIZED,
    OK,
    INVALID_API_KEY,
    TIMEOUT_ERROR,
    OTHER_ERROR,
}

/**
 * Interface for Retrofit defining how to query weather data using the REST interface
 */
interface WeatherApiService {
    @GET("data/2.5/onecall")
    fun getData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("exclude") exclude: String,
        @Query("units") units: String,
        @Query("appid") appid: String,
    ):
            Observable<WeatherModel.Result>

    companion object {
        fun create(): WeatherApiService {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://api.openweathermap.org/")
                    .build()
            return retrofit.create(WeatherApiService::class.java)
        }
    }
}

/**
 * Class for getting weather information from the internet.
 * Using the OpenWeatherMap API and Retrofit to convert the JSON response into a nice Kotlin object
 * Inspired by https://github.com/elye/demo_wiki_search_count
 */
class Weather(
    private val context: Context,
    private val mSharedPreferences: SharedPreferences,
    private val resources: Resources,
){
    // create retrofit service when it is used for the first time
    private val weatherApiServe by lazy {
        WeatherApiService.create()
    }
    // RXJava2.0 object tracking fetching activity
    private var disposableObserver: Disposable? = null

    var weather : WeatherModel.Result? = null
    var status = WeatherQueryStatus.UNINITIALIZED
    var errorMessage = ""

    private val mLocationService = LocationService(context, resources)

    fun updateWeather(){
        println("Updating weather data")

        // Todo: get coordinated from device and api key from settings
        // ToDo: settings for units
        val apiKey:String = mSharedPreferences.getString(context.getString(R.string.preference_weather_api_key), "").toString()
        println("using api key $apiKey")

        val latitude = if (mLocationService.lastKnownLocation != null)
            mLocationService.lastKnownLocation!!.latitude
        else 0.0 // test location for emulated devices
        val longitude = if (mLocationService.lastKnownLocation != null)
            mLocationService.lastKnownLocation!!.longitude
        else 28.0 // test location for emulated devices

        disposableObserver = weatherApiServe.getData(latitude.toString(), longitude.toString(),
                "", context.getString(R.string.weather_units), apiKey)
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {result ->
                            println("Got weather result temp: ${result.current.temp}")
                            weather = result
                            status = WeatherQueryStatus.OK
                            errorMessage = ""
                        },
                        {error ->
                            errorMessage = error.message.toString()
                            println("Weather query error: $errorMessage")
                            status = when{
                                errorMessage.contains("401") -> WeatherQueryStatus.INVALID_API_KEY
                                errorMessage.contains("timeout") -> WeatherQueryStatus.TIMEOUT_ERROR
                                else -> WeatherQueryStatus.OTHER_ERROR
                            }
                            // ToDo: show error in toast somehow (needs to be done in other Thread)
                        }
                )
        // ToDo: Handle abnormal result (too many calls, no internet, ...)
    }
}


