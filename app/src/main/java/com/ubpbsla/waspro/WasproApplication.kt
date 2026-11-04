package com.ubpbsla.waspro

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.ubpbsla.waspro.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@HiltAndroidApp
class WasproApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun api(): MobileApi = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build()) }.build())
        .build().create(MobileApi::class.java)

    @Provides @Singleton
    fun local(@ApplicationContext context: Context): LocalStore =
        Room.databaseBuilder(context, LocalStore::class.java, "waspro.db").build()
}
