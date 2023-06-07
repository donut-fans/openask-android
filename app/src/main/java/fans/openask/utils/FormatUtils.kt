package fans.openask.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

fun isEmail(strEmail:String): Boolean {
    var strPattern = "^[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$"
    var p = Pattern.compile(strPattern)
    var m = p.matcher(strEmail)
    return m.matches()
}