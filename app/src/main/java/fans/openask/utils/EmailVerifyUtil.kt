package fans.openask.utils

import androidx.core.util.PatternsCompat

/**
 *
 * Created by Irving
 */
class EmailVerifyUtil {
	
	companion object{
		fun isEmail(email: String):Boolean{
			return PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
		}
	}
	
}