package com.williamfq.xhat.di

import android.content.Context
import com.williamfq.xhat.utils.VoiceRecorder
import com.williamfq.xhat.utils.VoiceRecorderImpl
import com.williamfq.domain.utils.AudioTrimmer
import com.williamfq.domain.utils.SpeechToTextService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideVoiceRecorder(
        @ApplicationContext context: Context,
        audioTrimmer: AudioTrimmer,
        speechToTextService: SpeechToTextService
    ): VoiceRecorder {
        return VoiceRecorderImpl(context, audioTrimmer, speechToTextService)
    }
}