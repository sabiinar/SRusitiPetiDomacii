package rs.ac.pr.ftn.srusitipetidomacii




class WordsRepository {
    private val _words = mutableListOf(
        "Demokratija", "Sloboda", "Studenti", "Univerzitet"
    )
    val words: List<String> get() = _words.toList()

    fun addWord(word: String) {
        _words.add(word)
    }

    fun removeWord(word: String) {
        _words.remove(word)
    }

    fun clearAll() {
        _words.clear()
    }

    fun sortWords(ascending: Boolean) {
        if (ascending) _words.sort() else _words.sortDescending()
    }
}
