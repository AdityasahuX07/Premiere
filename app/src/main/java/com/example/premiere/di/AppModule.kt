package com.example.premiere.di

import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.example.premiere.data.local.PremiereDatabase
import com.example.premiere.data.local.dao.MovieDao
import com.example.premiere.data.preferences.PremierePreferences
import com.example.premiere.data.remote.OmdbApiService
import com.example.premiere.data.remote.TmdbApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService =
        retrofit.create(TmdbApiService::class.java)

    @Provides
    @Singleton
    fun provideOmdbApiService(okHttpClient: OkHttpClient, moshi: Moshi): OmdbApiService =
        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OmdbApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PremiereDatabase =
        Room.databaseBuilder(context, PremiereDatabase::class.java, PremiereDatabase.DATABASE_NAME)
            .addMigrations(
                PremiereDatabase.MIGRATION_1_2,
                PremiereDatabase.MIGRATION_2_3,
                PremiereDatabase.MIGRATION_3_4,
                PremiereDatabase.MIGRATION_4_5,
                PremiereDatabase.MIGRATION_5_6,
                PremiereDatabase.MIGRATION_6_7,
                PremiereDatabase.MIGRATION_7_8,
                PremiereDatabase.MIGRATION_8_9,
                PremiereDatabase.MIGRATION_9_10,
                PremiereDatabase.MIGRATION_10_11,
                PremiereDatabase.MIGRATION_11_12
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMovieDao(db: PremiereDatabase): MovieDao = db.movieDao()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): PremierePreferences =
        PremierePreferences(context)

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
