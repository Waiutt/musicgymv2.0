package com.example.musicgym;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {BlogPost.class, WorkoutRecord.class, WeightRecord.class,
        StrengthRecord.class, BodyMeasurement.class, WorkoutTemplate.class,
        Playlist.class, PlaylistSong.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract BlogPostDao blogPostDao();
    public abstract WorkoutRecordDao workoutRecordDao();
    public abstract WeightRecordDao weightRecordDao();
    public abstract StrengthRecordDao strengthRecordDao();
    public abstract BodyMeasurementDao bodyMeasurementDao();
    public abstract WorkoutTemplateDao workoutTemplateDao();
    public abstract PlaylistDao playlistDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `workout_records` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT, `sportType` TEXT," +
                    "`distanceKm` REAL NOT NULL, `durationSeconds` INTEGER NOT NULL," +
                    "`calories` INTEGER NOT NULL, `pathPointsJson` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `weight_records` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT, `weightKg` REAL NOT NULL)");
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `strength_records` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT," +
                    "`durationSeconds` INTEGER NOT NULL, `exercisesJson` TEXT)");
        }
    };
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT," +
                    "`weightKg` REAL NOT NULL, `chestCm` REAL NOT NULL, `waistCm` REAL NOT NULL," +
                    "`hipCm` REAL NOT NULL, `armCm` REAL NOT NULL, `thighCm` REAL NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `workout_templates` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `exercisesJson` TEXT)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, `createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`playlistId` INTEGER NOT NULL, `trackPath` TEXT, " +
                    "`title` TEXT, `artist` TEXT, `position` INTEGER NOT NULL)");
        }
    };

    private static volatile AppDatabase INSTANCE;
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "musicgym_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
