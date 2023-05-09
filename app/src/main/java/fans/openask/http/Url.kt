package fans.openask.http

import fans.openask.BuildConfig
import rxhttp.wrapper.annotation.DefaultDomain

/**
 *
 * Created by Irving
 */
class Url {
	
	companion object {
		@DefaultDomain
		var BASE_URL = String(BuildConfig.BASE_URL)
		
		private fun String(baseUrl: String): String {
			return baseUrl
		}
	}
	
}