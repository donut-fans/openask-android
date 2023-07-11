package fans.openask.model

/**
 *
 * Created by Irving
 */
class TAGModel {
	constructor(id: Int?, name: String?, iconUrl: Int?) {
		this.id = id
		this.name = name
		this.iconUrl = iconUrl
	}
	
	var id:Int? = null
	var code:String? = null
	var name:String? = null
	var iconUrl:Int? = null
	var isChecked:Boolean? = false
	
}