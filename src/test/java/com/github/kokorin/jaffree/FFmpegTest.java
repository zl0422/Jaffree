package com.github.kokorin.jaffree;

import com.github.kokorin.jaffree.cli.*;
import com.github.kokorin.jaffree.ffprobe.xml.FFprobeType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FFmpegTest {
    public static Path BIN;
    public static Path SAMPLES = Paths.get("target/samples");
    public static Path VIDEO_MP4 = SAMPLES.resolve("MPEG-4/video.mp4");
    public static Path SMALL_FLV = SAMPLES.resolve("FLV/zelda.flv");
    public static Path SMALL_MP4 = SAMPLES.resolve("MPEG-4/turn-on-off.mp4");
    public static Path ERROR_MP4 = SAMPLES.resolve("non_existent.mp4");
    public static Path TRANSPORT_VOB = SAMPLES.resolve("MPEG-VOB/transport-stream/capture.neimeng");

    @BeforeClass
    public static void setUp() throws Exception {
        String ffmpegHome = System.getProperty("FFMPEG_BIN");
        if (ffmpegHome == null) {
            ffmpegHome = System.getenv("FFMPEG_BIN");
        }
        Assert.assertNotNull("Nor command line property, neither system variable FFMPEG_BIN is set up", ffmpegHome);
        BIN = Paths.get(ffmpegHome);

        Assert.assertTrue("Sample videos weren't found: " + SAMPLES.toAbsolutePath(), Files.exists(SAMPLES));
    }

    @Test
    public void testSimpleCopy() throws Exception {
        Path tempDir = Files.createTempDirectory("jaffree");
        Path outputPath = tempDir.resolve(VIDEO_MP4.getFileName());

        FFmpegResult result = FFmpeg.atPath(BIN)
                .addInput(new Input().setUrl(VIDEO_MP4.toString()))
                .addOutput(new Output()
                        .setUrl(outputPath.toString())
                        .addCodec(null, "copy"))
                .execute();

        Assert.assertNotNull(result);
    }

    @Test
    @Ignore
    public void testOutputAdditionalOption() throws Exception {
        Path tempDir = Files.createTempDirectory("jaffree");
        Path outputPath = tempDir.resolve("test.mp3");

        FFmpegResult result = FFmpeg.atPath(BIN)
                .addInput(new Input().setUrl(SMALL_MP4.toString()))
                .addOutput(new Output()
                        .setUrl(outputPath.toString())
                        .addCodec(StreamSpecifier.withType(StreamType.AUDIO), "mp3")
                        .addOption(new Option("-vn"))
                        .addOption(new Option("-id3v2_version", "3"))
                )
                .execute();

        Assert.assertNotNull(result);
    }

    @Test
    public void testProgress() throws Exception {
        Path tempDir = Files.createTempDirectory("jaffree");
        Path outputPath = tempDir.resolve("test.mp4");

        final AtomicLong counter = new AtomicLong();

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(FFmpegProgress progress) {
                counter.incrementAndGet();
            }
        };

        FFmpegResult result = FFmpeg.atPath(BIN)
                .addInput(new Input().setUrl(SMALL_FLV.toString()))
                .addOutput(new Output().setUrl(outputPath.toString()))
                .setProgressListener(listener)
                .execute();

        Assert.assertNotNull(result);
        Assert.assertTrue(counter.get() > 0);
    }

    @Test
    public void testProgress2() throws Exception {
        Path tempDir = Files.createTempDirectory("jaffree");
        Path outputPath = tempDir.resolve("test.flv");

        final AtomicLong counter = new AtomicLong();

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(FFmpegProgress progress) {
                counter.incrementAndGet();
            }
        };

        FFmpegResult result = FFmpeg.atPath(BIN)
                .addInput(new Input().setUrl(SMALL_MP4.toString()))
                .addOutput(new Output().setUrl(outputPath.toString()))
                .setProgressListener(listener)
                .execute();

        Assert.assertNotNull(result);
        Assert.assertTrue(counter.get() > 0);
    }

    @Test
    public void testDuration() throws Exception {
        Path tempDir = Files.createTempDirectory("jaffree");
        Path outputPath = tempDir.resolve(VIDEO_MP4.getFileName());

        FFmpegResult result = FFmpeg.atPath(BIN)
                .addInput(new Input()
                        .setUrl(VIDEO_MP4.toString())
                        .setDuration(10, TimeUnit.SECONDS)
                )
                .addOutput(new Output()
                        .setUrl(outputPath.toString())
                        .addCodec(null, "copy"))
                .execute();

        Assert.assertNotNull(result);

        FFprobeType test = FFprobe.atPath(BIN)
                .setShowStreams(true)
                .setShowError(true)
                .setInputPath(outputPath)
                .execute();

        Assert.assertNull(test.getError());
        for (com.github.kokorin.jaffree.ffprobe.xml.StreamType streamType : test.getStreams().getStream()) {
            Assert.assertEquals(10.0f, streamType.getDuration(), 0.1);
        }
    }
}
