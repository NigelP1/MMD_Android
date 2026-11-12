package uk.makemydaylondon.mmd

/** Reads a file from src/test/resources. */
fun resource(path: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource(path)) { "Missing test resource $path" }.readText()
