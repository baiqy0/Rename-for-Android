package com.renamdroid.batchrename.core

/**
 * 从影视文件名中提取结构化信息，供模板系统使用。
 *
 * 例如：
 *   The.Last.of.Us.S02E03.2160p.WEB-DL.DDP5.1.H.265.mkv
 *   → title="The Last of Us", season="02", episode="03",
 *     resolution="2160p", source="WEB-DL", audio="DDP5.1", codec="H.265"
 */
data class MediaInfo(
    val title: String = "",
    val season: String = "",
    val episode: String = "",
    val year: String = "",
    val resolution: String = "",
    val codec: String = "",
    val source: String = "",
    val audio: String = "",
)

object MediaNameParser {

    // 季集：S02E03 / s02e03 / 2x03
    private val SEASON_EPISODE = Regex("""[Ss](\d{1,3})[Ee](\d{1,3})""")
    private val X_EPISODE = Regex("""(?<![0-9Ss])(\d{1,2})[xX](\d{1,3})(?![0-9])""")
    private val EPISODE_ONLY = Regex("""(?<![0-9Ss])[Ee][Pp]?(\d{1,3})(?![0-9])""")
    private val SEASON_ONLY = Regex("""(?<![0-9])[Ss](\d{1,2})(?![0-9Ee])""")

    private val YEAR = Regex("""(?<![0-9])(19\d{2}|20\d{2})(?![0-9])""")
    private val RESOLUTION = Regex("""(?i)(\d{3,4}[pP]|4K|8K)""")
    private val CODEC = Regex("""(?i)([Hh]\.?26[45]|HEVC|AVC|[Xx]26[45]|VC-?1)""")
    private val SOURCE = Regex(
        """(?i)\b(WEB-?DL|WEB-?Rip|WEBRip|WEB|Blu-?Ray|BDRip|BRRip|HDTV|DVDRip|DVD-?Rip|REMUX|HDRip)\b"""
    )
    private val AUDIO = Regex(
        """(?i)(DDP?\.?\d\.\d|DTS(?:-?HD)?(?:\.?MA)?|TrueHD|Atmos|FLAC|AAC\d?|EAC3|AC3)"""
    )
    private val CHINESE_EPISODE = Regex("""第\s*(\d{1,3})\s*[集话話]""")

    fun parse(rawName: String): MediaInfo {
        val name = rawName.trim()
        var season = ""
        var episode = ""

        SEASON_EPISODE.find(name)?.let { m ->
            season = m.groupValues[1]
            episode = m.groupValues[2]
        }
        if (season.isEmpty()) {
            X_EPISODE.find(name)?.let { m ->
                season = m.groupValues[1]
                episode = m.groupValues[2]
            }
        }
        if (episode.isEmpty()) {
            EPISODE_ONLY.find(name)?.let { m -> episode = m.groupValues[1] }
        }
        if (episode.isEmpty()) {
            CHINESE_EPISODE.find(name)?.let { m -> episode = m.groupValues[1] }
        }
        if (season.isEmpty()) {
            SEASON_ONLY.find(name)?.let { m -> season = m.groupValues[1] }
        }

        val year = YEAR.find(name)?.value ?: ""
        val resolution = RESOLUTION.find(name)?.value?.uppercase() ?: ""
        val codec = CODEC.find(name)?.value ?: ""
        val source = SOURCE.find(name)?.value?.uppercase()?.replace("WEB-DL", "WEB-DL") ?: ""
        val audio = AUDIO.find(name)?.value ?: ""

        // 标题：去掉所有已识别的技术标记和分隔符后剩下的内容
        var title = name
        for (token in listOfNotNull(
            SEASON_EPISODE.find(name)?.value,
            if (season.isNotEmpty() && SEASON_EPISODE.find(name) == null) X_EPISODE.find(name)?.value else null,
            if (season.isNotEmpty() && episode.isEmpty()) SEASON_ONLY.find(name)?.value else null,
            if (episode.isNotEmpty() && SEASON_EPISODE.find(name) == null && X_EPISODE.find(name) == null) EPISODE_ONLY.find(name)?.value else null,
            YEAR.find(name)?.value,
            RESOLUTION.find(name)?.value,
            CODEC.find(name)?.value,
            SOURCE.find(name)?.value,
            AUDIO.find(name)?.value,
            CHINESE_EPISODE.find(name)?.value,
        )) {
            title = title.replace(token, " ")
        }
        // 去掉字幕组标签 / 各种方括号圆括号内容
        title = title.replace(Regex("""[\[\(\{【（][^\]\)\}】）]*[\]\)\}】）]"""), " ")
        // 统一分隔符
        title = title.replace(Regex("""[._\-+]"""), " ")
        title = title.replace(Regex("""\s+"""), " ").trim()

        return MediaInfo(
            title = title,
            season = season,
            episode = episode,
            year = year,
            resolution = resolution,
            codec = codec,
            source = source,
            audio = audio,
        )
    }
}
