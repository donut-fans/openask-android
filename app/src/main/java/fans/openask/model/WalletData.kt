package fans.openask.model

/**
 *
 * Created by Irving
 */
public class WalletData {
	
	var balance:Int? = null
	var totalEarning:Int? = null
	var accountDtos: MutableList<AccountCoinModel>? = null
	
	public class AccountCoinModel{
		
		var id:String? = null
		var payMethodId:Int? = null
		var name:String? = null
		var availableAmount:String? = null
		var bigType:String? = null
		var currency:String? = null
		var freezeAmount:String? = null
		var symbol:String? = null
		var totalBalance:Double? = null
		
	}
	
}