package compose.demo.onlyfunds

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform