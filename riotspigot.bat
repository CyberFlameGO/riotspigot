cd dytanicspigot-server\target
java -Xmx256M -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:MaxPermSize=256M -XX:-UseAdaptiveSizePolicy -Dcom.mojang.eula.agree=true -Dio.netty.recycler.maxCapacity=0 -Dio.netty.recycler.maxCapacity.default=0 -server -jar dytanicspigot.jar nogui