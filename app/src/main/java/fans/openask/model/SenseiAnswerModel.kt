package fans.openask.model

/**
 *
 * Created by Irving
 */
class SenseiAnswerModel {

//	"questionId": "1659171038232588288",
//	"questionerUid": "1658025777879904258",
//	"questioneeUid": "1659090180809207810",
//	"questionerAvatar": "https://pbs.twimg.com/profile_images/1571790900350849024/_DTeOJfx_normal.jpg",
//	"questioneeAvatar": "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
//	"questioneeName": "lx",
//	"questionerName": "刘强",
//	"questioneeUsername": "xqliu188",
//	"questionerUsername": "lixioqing2",
//	"askQuestionTime": 1684412176081,
//	"questionContent": "chrom浏览器录制回答",
//	"askQuestionStatus": 1,
//	"answerTime": 1684412215000,
//	"rewardAmount": 3.000000,
//	"rewardPaymethodId": 8,
//	"questionExpireTime": 1685016976000

	var questionId:String? = null
	var questionerUid:String? = null
	var questioneeUid:String? = null
	var questionerAvatar:String? = null
	var questioneeAvatar:String? = null
	var questioneeName:String? = null
	var questionerName:String? = null
	var questioneeUsername:String? = null
	var questionerUsername:String? = null
	var askQuestionTime:Long? = null
	var questionContent:String? = null
	var askQuestionStatus:Int? = null //0:Awating 1 completed 2 expired
	var answerTime:Long? = null
	var rewardAmount:String? = null
	var rewardPaymethodId:String? = null
	var questionExpireTime:Long? = null
	var answerState:AnswerStateModel? = null
	
}