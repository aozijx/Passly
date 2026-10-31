package android.net

import android.os.Parcel

internal object TestUri : Uri() {
    override fun isHierarchical() = true
    override fun isRelative() = false
    override fun getScheme() = "content"
    override fun getSchemeSpecificPart() = "//backup"
    override fun getEncodedSchemeSpecificPart() = "//backup"
    override fun getAuthority() = "backup"
    override fun getEncodedAuthority() = "backup"
    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null
    override fun getHost() = "backup"
    override fun getPort() = -1
    override fun getPath() = "/"
    override fun getEncodedPath() = "/"
    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null
    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getPathSegments() = emptyList<String>()
    override fun getLastPathSegment(): String? = null
    override fun toString() = "content://backup"
    override fun buildUpon(): Builder = error("Unused in access-state tests")
    override fun describeContents() = 0
    override fun writeToParcel(dest: Parcel, flags: Int) = Unit
}
