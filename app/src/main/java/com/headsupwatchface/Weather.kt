package com.headsupwatchface


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

/**
 * Interface for Retrofit defining how to query weather data using the REST interface
 */
interface WeatherApiService {
    @GET("data/2.5/onecall")
    fun getData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("exclude") exclude: String,
        @Query("appid") appid: String
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
class Weather(){
    // create retrofit service when it is used for the first time
    private val weatherApiServe by lazy {
        WeatherApiService.create()
    }
    // RXJava2.0 object tracking fetching activity
    private var disposableObserver: Disposable? = null

    fun updateWeather(){
        println("Updating weather data")

        // Todo: get coordinated from device and api key from settings
        disposableObserver = weatherApiServe.getData("", "", "", "")
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {result -> println("Got weather result temp: ${result.current.temp}")},
                        {error -> println("Weather query error: ${error.message}")}
                )
        // ToDo: Handle abnormal result (invalid api key, too many calls, no internet, ...)
    }
}


