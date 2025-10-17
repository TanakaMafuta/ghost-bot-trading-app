package com.ghostbot.trading.data.local.converters

import androidx.room.TypeConverter
import com.ghostbot.trading.domain.model.*

class TypeConverters {
    
    @TypeConverter
    fun fromTradeType(tradeType: TradeType): String = tradeType.name
    
    @TypeConverter
    fun toTradeType(tradeType: String): TradeType = TradeType.valueOf(tradeType)
    
    @TypeConverter
    fun fromTradeStatus(status: TradeStatus): String = status.name
    
    @TypeConverter
    fun toTradeStatus(status: String): TradeStatus = TradeStatus.valueOf(status)
    
    @TypeConverter
    fun fromSignalType(signal: SignalType): String = signal.name
    
    @TypeConverter
    fun toSignalType(signal: String): SignalType = SignalType.valueOf(signal)
    
    @TypeConverter
    fun fromAISource(source: AISource): String = source.name
    
    @TypeConverter
    fun toAISource(source: String): AISource = AISource.valueOf(source)
    
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name
    
    @TypeConverter
    fun toNotificationType(type: String): NotificationType = NotificationType.valueOf(type)
    
    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String = priority.name
    
    @TypeConverter
    fun toNotificationPriority(priority: String): NotificationPriority = NotificationPriority.valueOf(priority)
}