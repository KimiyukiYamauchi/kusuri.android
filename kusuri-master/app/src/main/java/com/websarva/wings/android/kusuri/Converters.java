//package com.websarva.wings.android.kusuri;
//
//import androidx.room.TypeConverter;
//
//import java.util.Date;
//
//public class Converters {
//    //日付をタイムスタンプからYYYY/MM/DDで表示させるための処理
////RoomデータベースでDate型を使うために型変換を行うクラス(TypeConverter)
////Roomは標準でDate型をサポートしていないため、Long型（タイムスタンプ）に変換して保存し、データベースから取得する際に再びDate型に戻す
//    public class Converters {
//        @TypeConverter
//        public static Date fromTimestamp(Long value) {
//            return value == null ? null : new Date(value);
//        }
//
//        @TypeConverter
//        public static Long dateToTimestamp(Date date) {
//            return date == null ? null : date.getTime();
//        }
//    }
//
//}
