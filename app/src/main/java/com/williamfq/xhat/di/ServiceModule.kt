package com.williamfq.xhat.di

import android.content.Context
import com.williamfq.data.manager.UserManager
import com.williamfq.data.manager.UserManagerImpl
import com.williamfq.data.service.MessagingServiceImpl
import com.williamfq.domain.service.MessagingService
import com.williamfq.xhat.service.audio.AudioManager
import com.williamfq.xhat.service.audio.AudioManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindMessagingService(messagingServiceImpl: MessagingServiceImpl): MessagingService

    companion object {
        @Provides
        @Singleton
        fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
            return AudioManagerImpl(context)
        }

        @Provides
        @Singleton
        fun provideUserManager(userManagerImpl: UserManagerImpl): UserManager = userManagerImpl
    }
}