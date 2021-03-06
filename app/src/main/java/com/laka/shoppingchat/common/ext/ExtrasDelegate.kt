package com.laka.shoppingchat.common.ext

/**
 * Created by aa on 2019-01-24.
 * @auto sming
 */
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.laka.androidlib.widget.swipebacklayout.SwipeBackActivity
import kotlin.reflect.KProperty

class ExtrasDelegate<out T>(private val extraName: String, private val defaultValue: T) {

    private var extra: T? = null

    operator fun getValue(thisRef: SwipeBackActivity, property: KProperty<*>): T {
        extra = getExtra(extra, extraName, thisRef)
        return extra ?: defaultValue
    }

    operator fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        extra = getExtra(extra, extraName, thisRef)
        return extra ?: defaultValue
    }

}

fun <T> extraDelegate(extra: String, default: T) = ExtrasDelegate(extra, default)

fun extraDelegate(extra: String) = extraDelegate(extra, null)

@Suppress("UNCHECKED_CAST")
private fun <T> getExtra(oldExtra: T?, extraName: String, thisRef: SwipeBackActivity): T? =
        oldExtra ?: thisRef.intent?.extras?.get(extraName) as T?

@Suppress("UNCHECKED_CAST")
private fun <T> getExtra(oldExtra: T?, extraName: String, thisRef: Fragment): T? =
        oldExtra ?: thisRef.arguments?.get(extraName) as T?