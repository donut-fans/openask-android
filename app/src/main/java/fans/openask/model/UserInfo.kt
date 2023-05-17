package fans.openask.model

import android.os.Parcel
import android.os.Parcelable

/**
 *
 * Created by Irving
 */
class UserInfo() :Parcelable{

//	"username": "0x9Cb12550F94e06F2322FCd5F084cD786290f8EF9",
//	"nickname": "0x9Cb12550F94e06F2322FCd5F084cD786290f8EF9",
//	"locked": null,
//	"headIcon": null,
//	"unlockTime": null,
//	"description": null,
//	"email": null,
//	"virtualBalance": null,
//	"verifySuccess": true,
//	"bindUser": true,
//	"token": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiIwYjIwMmExMy1lZTRiLTQyZTktOWI2MS0xNjMxOTUzMmRmMGUiLCJpYXQiOjE2ODM3ODY0NDAsImV4cCI6MTY4Mzc5MzY0MCwid2FsbGV0X2FkZHJlc3MiOiIweDlDYjEyNTUwRjk0ZTA2RjIzMjJGQ2Q1RjA4NGNENzg2MjkwZjhFRjkiLCJ0cmlwYXJ0aXRlX2lkIjoiMHg5Q2IxMjU1MEY5NGUwNkYyMzIyRkNkNUYwODRjRDc4NjI5MGY4RUY5IiwicHJvZHVjdCI6Im9wZW5Bc2siLCJyZWZyZXNoIjoiNWI3YjFmMTIzMjIzOTBiYiIsInVzZXJJZCI6MTY1NjU0NjUxMjk5MDcxNTkwNX0.TmYTiUDXU-RBU-ZeilZbd-ANCjzV_mjyef3DgXdpGB92XiNvng7AAvkz6gp3UWs_",
//	"refreshToken": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiJhMDI1YjMyMC02MDBlLTQzMjQtYThiYy0zZjAwNTZmNjYxMDciLCJpYXQiOjE2ODM3ODY0NDAsImV4cCI6MTY4NDM5MTI0MCwid2FsbGV0X2FkZHJlc3MiOiIweDlDYjEyNTUwRjk0ZTA2RjIzMjJGQ2Q1RjA4NGNENzg2MjkwZjhFRjkiLCJ0cmlwYXJ0aXRlX2lkIjoiMHg5Q2IxMjU1MEY5NGUwNkYyMzIyRkNkNUYwODRjRDc4NjI5MGY4RUY5IiwicHJvZHVjdCI6Im9wZW5Bc2siLCJ1c2VySWQiOjE2NTY1NDY1MTI5OTA3MTU5MDV9.Ds1QiiBzrlrKOx64W-jZkeOEbb_8-e-e-W9XdD4TKXt2cIAcofBvYFucRqVg6VCu",
//	"userId": "1656546512990715905",
//	"isSensei": false,
//	"userNo": "1961158",
//	"sid": null
	
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
	var userId:String? = null
	var userNo:String? = null
	var isSensei:Boolean? = null
	
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
		userId = parcel.readString()
		userNo = parcel.readString()
		isSensei = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
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
		parcel.writeString(userId)
		parcel.writeString(userNo)
		parcel.writeValue(isSensei)
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