package nu.peg.ubnt.edgemax.util

fun <T> T.orIf(predicate: (T) -> Boolean, thenValue: T): T =
        if (predicate.invoke(this)) thenValue else this