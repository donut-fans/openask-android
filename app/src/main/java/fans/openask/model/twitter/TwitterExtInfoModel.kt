package fans.openask.model.twitter

/**
 *
 * Created by Irving
 */
class TwitterExtInfoModel {
	constructor(twitterUid: String?,
	            providerId: String?,
	            photoUrl: String?,
	            displayName: String?,
	            screenName: String?,
	            bio: String?,
	            followersCount: String?) {
		this.twitterUid = twitterUid
		this.providerId = providerId
		this.photoUrl = photoUrl
		this.displayName = displayName
		this.screenName = screenName
		this.bio = bio
		this.followersCount = followersCount
	}
	
	var twitterUid:String? = null
	var providerId:String? = null
	var photoUrl:String? = null
	var displayName:String? = null
	var screenName:String? = null
	var bio:String? = null
	var followersCount:String? = null
	
}