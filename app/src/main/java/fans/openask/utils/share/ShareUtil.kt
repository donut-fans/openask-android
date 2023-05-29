package fans.openask.utils.share

import android.content.Context
import android.content.Intent

/**
 *
 * Created by Irving
 */
class ShareUtil {
	
	companion object {
		fun share(text: String, context: Context) {
			var shareIntent = Intent()
			shareIntent.action = Intent.ACTION_SEND
			shareIntent.type = "text/plain"
			//要分享的文本内容，选择某项后会直接把这段文本发送出去，相当于调用选中的应用的接口，并传参
			shareIntent.putExtra(Intent.EXTRA_TEXT, text)
			//需要使用Intent.createChooser，这里我们直接复用。第二个参数并不会显示出来
			shareIntent = Intent.createChooser(shareIntent, "Share to")
			context.startActivity(shareIntent);
		}
	}
	
}