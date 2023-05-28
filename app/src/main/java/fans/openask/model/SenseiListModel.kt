package fans.openask.model

import android.os.Parcel
import android.os.Parcelable

/**
 *
 * Created by Irving
 */
class SenseiListModel() :Parcelable {
	
	var senseiUid:String? = null
	var userNo:String? = null
	var userId:String? = null
	var senseiName:String? = null
	var senseiProfileType:String? = null
	var senseiUsername:String? = null
	var senseiAvatarUrl:String? = null
	var answeredCount:String? = null
	var eavesdropCount:String? = null
	var bio:String? = null
	var followersCount:String? = null
	var followingCount:String? = null
	var joinDate:String? = null
	var minPriceAmount:String? = null
	
	constructor(parcel: Parcel) : this() {
		senseiUid = parcel.readString()
		userNo = parcel.readString()
		userId = parcel.readString()
		senseiName = parcel.readString()
		senseiProfileType = parcel.readString()
		senseiUsername = parcel.readString()
		senseiAvatarUrl = parcel.readString()
		answeredCount = parcel.readString()
		eavesdropCount = parcel.readString()
		bio = parcel.readString()
		followersCount = parcel.readString()
		followingCount = parcel.readString()
		joinDate = parcel.readString()
		minPriceAmount = parcel.readString()
	}
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(senseiUid)
		parcel.writeString(userNo)
		parcel.writeString(userId)
		parcel.writeString(senseiName)
		parcel.writeString(senseiProfileType)
		parcel.writeString(senseiUsername)
		parcel.writeString(senseiAvatarUrl)
		parcel.writeString(answeredCount)
		parcel.writeString(eavesdropCount)
		parcel.writeString(bio)
		parcel.writeString(followersCount)
		parcel.writeString(followingCount)
		parcel.writeString(joinDate)
		parcel.writeString(minPriceAmount)
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	companion object CREATOR : Parcelable.Creator<SenseiListModel> {
		override fun createFromParcel(parcel: Parcel): SenseiListModel {
			return SenseiListModel(parcel)
		}
		
		override fun newArray(size: Int): Array<SenseiListModel?> {
			return arrayOfNulls(size)
		}
	}
	
}