package com.codecad.core.sketch

class Key(val obj : Any){
    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }

    override fun equals(other: Any?): Boolean {
        return other is Key && obj == other.obj
    }
}

class Unifier<T : Any>(val condition : (T, T) -> Boolean){
    val nodes = hashMapOf<Key, UnionNode<T>>()

    fun add(value : T ){
        val key = Key(value)

        if(!nodes.contains(key)){
            var uniqueNode = UnionNode(value)
            for(presentNode in nodes.values){
                if( condition(presentNode.value, value) ){
                    uniqueNode = unify(presentNode, uniqueNode)
                }
            }

            nodes[key] = uniqueNode
        }
    }

    fun get( value : T ) : T{
        val node = nodes[Key(value)]
        return node?.value ?: value
    }

    fun unify(x: UnionNode<T>, y: UnionNode<T>) : UnionNode<T> {
        if (x != y) y.parent = x
        return x
    }
}

class UnionNode<T>(val value: T){
    var parent : UnionNode<T> = this

    fun find() : UnionNode<T>{
        if (parent != this) {
            parent = parent.find()
        }
        return parent
    }

    fun get() : T{
        return find().value
    }
}