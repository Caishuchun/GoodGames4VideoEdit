package com.fortune.zg.issue

data class MediaInfoBean(
    val audiostream_avcodocname: String?,
    val audiostream_codec_fourcc: String?,
    val audiostream_codecpar_bit_rate: String?,
    val audiostream_codecpar_channels: String?,
    val audiostream_codecpar_codec_type: String?,
    val audiostream_codecpar_sample_rate: String?,
    val audiostream_duration: String?,
    val audiostream_profilestring: String?,
    val audiostream_size: String?,
    val bit_rate: String,
    val comment: String,
    val compatible_brands: String,
    val display_aspect_ratio_den: String,
    val display_aspect_ratio_num: String,
    val duration: String,
    val encoder: String,
    val filesize: String,
    val iformat_long_name: String,
    val iformat_name: String,
    val major_brand: String,
    val max_interleave_delta: String,
    val max_picture_buffer: String,
    val max_ts_probe: String,
    val minor_version: String,
    val pix_fmt_name: String,
    val protocol_blacklist: String,
    val protocol_whitelist: String,
    val url: String,
    val videostream_avcodocname: String,
    val videostream_avg_frame_rate: String,
    val videostream_codec_fourcc: String,
    val videostream_codec_time_base: String,
    val videostream_codecpar_bit_rate: String,
    val videostream_codecpar_bits_per_coded_sample: String,
    val videostream_codecpar_bits_per_raw_sample: String,
    val videostream_codecpar_codec_type: String,
    val videostream_codecpar_height: String,
    val videostream_codecpar_width: String,
    val videostream_duration: String,
    val videostream_nb_frames: String,
    val videostream_profilestring: String,
    val videostream_r_frame_rate: String,
    val videostream_sample_aspect_ratio_den: String,
    val videostream_sample_aspect_ratio_num: String,
    val videostream_size: String,
    val videostream_time_base: String,
    val videostream_time_base_den: String,
    val videostream_time_base_num: String
)