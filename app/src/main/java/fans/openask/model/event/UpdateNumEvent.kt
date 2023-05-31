package fans.openask.model.event

/**
 *
 * Created by Irving
 */
class UpdateNumEvent {
	companion object{
		var EVENT_TYPE_ASKS = 1
		var EVENT_TYPE_EAVESDROP = 2
		var EVENT_TYPE_AWAITING = 3
		var EVENT_TYPE_COMPLETED = 4
		var EVENT_TYPE_ANSWER = 5
	}
	
	constructor(eventType: Int?, eventValue: Int?) {
		this.eventType = eventType
		this.eventValue = eventValue
	}
	
	var eventType:Int? = null
	var eventValue:Int? = null
	
}