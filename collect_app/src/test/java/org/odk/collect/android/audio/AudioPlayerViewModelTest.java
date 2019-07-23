package org.odk.collect.android.audio;

import android.media.MediaPlayer;

import androidx.lifecycle.LiveData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.odk.collect.android.support.LiveDataTester;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AudioPlayerViewModelTest {

    private final MediaPlayer mediaPlayer = mock(MediaPlayer.class);
    private final LiveDataTester liveDataTester = new LiveDataTester();

    private AudioPlayerViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new AudioPlayerViewModel(() -> mediaPlayer);
    }

    @After
    public void teardown() {
        liveDataTester.teardown();
    }

    @Test
    public void isPlaying_whenNothingPlaying_returnsFalse() {
        LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        assertThat(isPlaying.getValue(), is(false));
    }

    @Test
    public void isPlaying_whenClipIDPlaying_returnsTrue() {
        LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        assertThat(isPlaying.getValue(), is(true));
    }

    @Test
    public void isPlaying_whenDifferentClipIDPlaying_returnsFalse() {
        LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip2"));

        viewModel.play("clip1", "file://other.mp3");
        assertThat(isPlaying.getValue(), is(false));
    }

    @Test
    public void isPlaying_whenClipIDPlaying_thenStopped_returnsFalse() {
        LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.stop();

        assertThat(isPlaying.getValue(), is(false));
    }

    @Test
    public void isPlaying_whenClipIDPlaying_thenCompleted_returnsFalse() {
        final LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");

        ArgumentCaptor<MediaPlayer.OnCompletionListener> captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener.class);
        verify(mediaPlayer).setOnCompletionListener(captor.capture());
        captor.getValue().onCompletion(mediaPlayer);

        assertThat(isPlaying.getValue(), is(false));
    }

    @Test
    public void isPlaying_whenPlayingAndThenBackgrounding_returnsFalse() {
        LiveData<Boolean> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.background();

        assertThat(isPlaying.getValue(), is(false));
    }

    @Test
    public void play_resetsAndPreparesAndStartsMediaPlayer() throws Exception {
        viewModel.play("clip1", "file://audio.mp3");

        InOrder inOrder = Mockito.inOrder(mediaPlayer);

        inOrder.verify(mediaPlayer).reset();
        inOrder.verify(mediaPlayer).setDataSource("file://audio.mp3");
        inOrder.verify(mediaPlayer).prepare();
        inOrder.verify(mediaPlayer).start();
    }

    @Test
    public void play_afterBackground_createsANewMediaPlayer() {
        RecordingMockMediaPlayerFactory factory = new RecordingMockMediaPlayerFactory();
        AudioPlayerViewModel viewModel = new AudioPlayerViewModel(factory);

        viewModel.play("clip1", "file://audio.mp3");
        assertThat(factory.createdInstances.size(), equalTo(1));
        verify(factory.createdInstances.get(0)).start();

        viewModel.background();
        viewModel.play("clip1", "file://audio.mp3");
        assertThat(factory.createdInstances.size(), equalTo(2));
        verify(factory.createdInstances.get(1)).start();
    }

    @Test
    public void stop_stopsMediaPlayer() {
        viewModel.stop();
        verify(mediaPlayer).stop();
    }

    @Test
    public void background_releasesMediaPlayer() {
        viewModel.background();
        verify(mediaPlayer).release();
    }

    @Test
    public void onCleared_releasesMediaPlayer() {
        viewModel.onCleared();
        verify(mediaPlayer).release();
    }

    private static class RecordingMockMediaPlayerFactory implements MediaPlayerFactory {

        List<MediaPlayer> createdInstances = new ArrayList<>();

        @Override
        public MediaPlayer create() {
            MediaPlayer mock = mock(MediaPlayer.class);
            createdInstances.add(mock);

            return mock;
        }
    }
}