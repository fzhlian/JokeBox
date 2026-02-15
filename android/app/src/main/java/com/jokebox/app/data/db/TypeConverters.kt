package fzhlian.JokeBox.app.data.db

import androidx.room.TypeConverter
import fzhlian.JokeBox.app.data.model.RawStatus
import fzhlian.JokeBox.app.data.model.SourceType

class RoomConverters {
    @TypeConverter
    fun sourceTypeToString(value: SourceType): String = value.name

    @TypeConverter
    fun stringToSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun rawStatusToString(value: RawStatus): String = value.name

    @TypeConverter
    fun stringToRawStatus(value: String): RawStatus = RawStatus.valueOf(value)
}

