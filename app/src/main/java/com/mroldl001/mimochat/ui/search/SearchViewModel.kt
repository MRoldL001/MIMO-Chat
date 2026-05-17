
package com.mroldl001.mimochat.ui.search

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mroldl001.mimochat.data.repository.ChatRepository
import com.mroldl001.mimochat.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _hasSearched = mutableStateOf(false)
    val hasSearched: State<Boolean> = _hasSearched

    fun updateQuery(query: String) {
        val previousQuery = _searchQuery.value
        _searchQuery.value = query
        
        if (previousQuery != query) {
            _hasSearched.value = false
            _searchResults.value = emptyList()
        }
    }

    fun performSearch() {
        val query = _searchQuery.value

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            _hasSearched.value = false
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _hasSearched.value = true

            chatRepository.searchMessages(query).collect { results ->
                _searchResults.value = results
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}
