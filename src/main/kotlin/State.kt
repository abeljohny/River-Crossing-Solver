data class State(val leftBank: Set<Entity>, val boatOnLeft: Boolean) {
    val rightBank: Set<Entity> = allEntities - leftBank
}
