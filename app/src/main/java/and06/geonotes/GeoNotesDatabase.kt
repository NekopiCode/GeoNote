package and06.geonotes

import android.content.Context
import androidx.room.*


@Entity(tableName = "projekte")
data class Projekt(
    @PrimaryKey
    val id: Long,
    var beschreibung: String?
)

@Entity(
    tableName = "locations",
    primaryKeys = ["latitude", "longitude"]
)
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val provider: String
)

@Entity(
    tableName = "notizen",
    foreignKeys = [ForeignKey(entity = Projekt::class, parentColumns = ["id"], childColumns =  ["projektId"]),
    ForeignKey(entity = Location::class, parentColumns = ["latitude", "longitude"], childColumns = ["latitude", "longitude"])
    ])
data class Notiz (
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    val projektId: Long,
    val latitude: Double,
    val longitude: Double,
    var thema: String,
    var notiz: String
)

@Database(
    entities = [Projekt::class, Notiz::class, Location::class],
    version = 1
)
abstract class GeoNotesDatabase : RoomDatabase() {
    companion object{
        private  var INSTANCE: GeoNotesDatabase? = null
        fun  getInstance(context: Context) : GeoNotesDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    GeoNotesDatabase::class.java,
                    "GeoNotesDatabase")
                    .build()
            }
            return INSTANCE as GeoNotesDatabase
        }
    }
    abstract fun projekteDao() : ProjekteDao
}

@Dao interface ProjekteDao {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    fun insertProjekt(projekt: Projekt): Long

    @Query("SELECT * FROM projekte")
    fun  getProjekte() : List<Projekt>
}