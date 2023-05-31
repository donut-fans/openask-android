package fans.openask.utils

/**
 *
 * Created by Irving
 */
class TimeUtils {
	
	companion object {
		
		fun timeConversion(time: Int): String? {
			var hour = 0
			var minutes = 0
			var sencond = 0
			val temp = time % 3600
			if (time > 3600) {
				hour = time / 3600
				if (temp != 0) {
					if (temp > 60) {
						minutes = temp / 60
						if (temp % 60 != 0) {
							sencond = temp % 60
						}
					} else {
						sencond = temp
					}
				}
			} else {
				minutes = time / 60
				if (time % 60 != 0) {
					sencond = time % 60
				}
			}
			return (if (hour < 10) "0$hour" else hour).toString() + ":" + (if (minutes < 10) "0$minutes" else minutes) + ":" + if (sencond < 10) "0$sencond" else sencond
		}
		
		fun timeConversion2(time: Int): String? {
			var minutes = 0
			var sencond = 0
			val temp = time % 3600
			if (time > 3600) {
				if (temp != 0) {
					if (temp > 60) {
						minutes = temp / 60
						if (temp % 60 != 0) {
							sencond = temp % 60
						}
					} else {
						sencond = temp
					}
				}
			} else {
				minutes = time / 60
				if (time % 60 != 0) {
					sencond = time % 60
				}
			}
			return (if (minutes < 10) "0$minutes" else minutes).toString() + ":" + (if (sencond < 10) "0$sencond" else sencond).toString()
		}
		
	}
}