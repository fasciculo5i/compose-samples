/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetcaster.tv.ui.episode

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetcaster.core.data.database.model.Episode
import com.example.jetcaster.core.data.database.model.EpisodeToPodcast
import com.example.jetcaster.core.data.di.Graph
import com.example.jetcaster.core.data.repository.EpisodeStore
import com.example.jetcaster.core.data.repository.PodcastsRepository
import com.example.jetcaster.tv.ui.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EpisodeScreenViewModel(
    handle: SavedStateHandle,
    podcastsRepository: PodcastsRepository = Graph.podcastRepository,
    episodeStore: EpisodeStore = Graph.episodeStore,
) : ViewModel() {

    private val episodeUri = handle.get<String>(Screen.Episode.PARAMETER_NAME)

    private val episodeToPodcastFlow = if (episodeUri != null) {
        episodeStore.episodeAndPodcastWithUri(episodeUri)
    } else {
        flowOf(null)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null
    )

    val uiStateFlow = episodeToPodcastFlow.map {
        if (it != null) {
            EpisodeScreenUiState.Ready(it)
        } else {
            EpisodeScreenUiState.Error
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EpisodeScreenUiState.Loading
    )

    fun addPlayList(episode: Episode) {
    }

    init {
        viewModelScope.launch {
            podcastsRepository.updatePodcasts(false)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val factory = object : AbstractSavedStateViewModelFactory() {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                return EpisodeScreenViewModel(
                    handle
                ) as T
            }
        }
    }
}

sealed interface EpisodeScreenUiState {
    data object Loading : EpisodeScreenUiState
    data object Error : EpisodeScreenUiState
    data class Ready(val episodeToPodcast: EpisodeToPodcast) : EpisodeScreenUiState
}
