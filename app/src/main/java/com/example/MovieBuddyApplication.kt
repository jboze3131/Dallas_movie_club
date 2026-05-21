package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.MovieBuddyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieBuddyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MovieBuddyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Singletons
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "movie_buddy_database"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = MovieBuddyRepository(database.movieBuddyDao())

        // Asynchronously populate sample data if database is empty on start
        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDatabaseIfEmpty()
        }
    }
}
