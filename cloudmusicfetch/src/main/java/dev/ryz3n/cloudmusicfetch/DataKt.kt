package dev.ryz3n.cloudmusicfetch

// http://music.163.com/api/song/media?id=541499418
data class Lyric(
        val code: Int,
        val lyric: String
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
        val company: String, // 唱片公司
        val name: String, // 专辑名
        val publishTime: Long // 发布时间（时间戳）
)

data class AlbumDetail(
        val albumName: String,
        val albumCompany: String,
        val albumPublishYear: String,
        val albumBlurPicByteArr: ByteArray
)