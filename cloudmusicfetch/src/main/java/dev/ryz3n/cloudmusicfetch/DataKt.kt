package dev.ryz3n.cloudmusicfetch

// http://music.163.com/api/song/media?id=541499418
data class Lyric(
        val code: Int,
        val lyric: String,
        val lyricVersion: Int,
        val songStatus: Int
)


// http://music.163.com/api/song/detail/?id=541499418&ids=[541499418]
data class Album(
        val code: Int,
        val songs: List<Song>
)

data class Song(
        val album: AlbumX
)

data class AlbumX(
        val blurPicUrl: String, // 专辑封面url
        val briefDesc: String,
        val company: String, // 唱片公司
        val companyId: Int,
        val description: String,
        val id: Int,
        val mark: Int,
        val name: String, // 专辑名
        val pic: Long,
        val picId: Long,
        val picUrl: String,
        val publishTime: Long, // 发布时间（时间戳）
        val size: Int,
        val subType: String,
        val tags: String,
        val transName: Any,
        val type: String
)

data class AlbumDetail(
        val albumName: String,
        val albumCompany: String,
        val albumPublishYear: String,
        val albumBlurPicByteArr: ByteArray
)