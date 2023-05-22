package fans.openask.model

/**
 *
 * Created by Irving
 */
class USDChargeModel {
	
	var orderId:String? = null
	
	var prePayResult:PrepayResultModel? = null
	
	class PrepayResultModel{
		var amountSubtotal:Int? = null
		var completed:Boolean? = null
		var customerId:String? = null
		var orderNo:String? = null
		var payIntentId:String? = null
		var paymentStatus:String? = null
		var sessionId:String? = null
		var payMethodId:String? = null
		var url:String? = null
		var success:Boolean? = null
	}
	
}