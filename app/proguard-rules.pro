# Netty/LittleProxy reflection
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class io.netty.** { *; }
-keep class org.littleshoot.proxy.** { *; }
-dontwarn io.netty.**
-dontwarn org.littleshoot.proxy.**

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# dnsjava
-keep class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-dontwarn com.google.errorprone.annotations.**
