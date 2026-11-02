package com.example.foxtrack.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.foxtrack.domain.GameStatus
import com.example.foxtrack.domain.Validation

class FoxDatabase(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val statusList = GameStatus.entries.joinToString(", ") { "'${it.dbValue}'" }
        val timestamp = "(strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                username_key TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                avatar TEXT,
                created_at TEXT NOT NULL DEFAULT $timestamp
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE user_games (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                thegamesdb_id INTEGER NOT NULL,
                title_key TEXT NOT NULL,
                title TEXT NOT NULL,
                cover_url TEXT,
                description TEXT,
                release_date TEXT,
                genres TEXT NOT NULL DEFAULT '[]',
                platforms TEXT NOT NULL DEFAULT '[]',
                selected_platforms TEXT NOT NULL DEFAULT '[]',
                status TEXT NOT NULL DEFAULT '${GameStatus.WISH.dbValue}' CHECK (status IN ($statusList)),
                rating INTEGER CHECK (rating IS NULL OR rating BETWEEN 1 AND 10),
                review TEXT CHECK (review IS NULL OR length(review) <= ${Validation.REVIEW_MAX_LENGTH}),
                added_at TEXT NOT NULL DEFAULT $timestamp,
                UNIQUE (user_id, thegamesdb_id),
                UNIQUE (user_id, title_key)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_user_games_user_added ON user_games (user_id, added_at DESC)")
        db.execSQL(
            """
            CREATE TABLE dictionary_cache (
                name TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                fetched_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        private const val NAME = "foxtrack.db"
        private const val VERSION = 1
    }
}
