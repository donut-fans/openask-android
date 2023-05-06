package com.timely.openask.model

import android.os.Parcel
import android.os.Parcelable

/**
 *
 * Created by Irving
 */
class UserInfo() :Parcelable{
	
	var username:String? = null
	var nickname:String? = null
	var locked:Int? = null
	var headIcon:String? = null
	var unlockTime:String? = null
	var description:String? = null
	var token:String? = null
	var email:String? = null
	var id:String? = null
	var virtualBalance:String? = null
	var refreshToken:String? = null
	var role:Int? = null
	var sid:String? = null
	
	constructor(parcel: Parcel) : this() {
		username = parcel.readString()
		nickname = parcel.readString()
		locked = parcel.readValue(Int::class.java.classLoader) as? Int
		headIcon = parcel.readString()
		unlockTime = parcel.readString()
		description = parcel.readString()
		token = parcel.readString()
		email = parcel.readString()
		id = parcel.readString()
		virtualBalance = parcel.readString()
		refreshToken = parcel.readString()
		role = parcel.readValue(Int::class.java.classLoader) as? Int
		sid = parcel.readString()
	}
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(username)
		parcel.writeString(nickname)
		parcel.writeValue(locked)
		parcel.writeString(headIcon)
		parcel.writeString(unlockTime)
		parcel.writeString(description)
		parcel.writeString(token)
		parcel.writeString(email)
		parcel.writeString(id)
		parcel.writeString(virtualBalance)
		parcel.writeString(refreshToken)
		parcel.writeValue(role)
		parcel.writeString(sid)
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	companion object CREATOR : Parcelable.Creator<UserInfo> {
		override fun createFromParcel(parcel: Parcel): UserInfo {
			return UserInfo(parcel)
		}
		
		override fun newArray(size: Int): Array<UserInfo?> {
			return arrayOfNulls(size)
		}
	}
	
	
}