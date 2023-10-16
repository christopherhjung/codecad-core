package com.codecad.core.sketch

class Key(val obj : Any){
    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }

    override fun equals(other: Any?): Boolean {
        return other is Key && obj == other.obj
    }
}

class Unifier<T : Any>(){
    private val nodes = hashMapOf<Key, UnionNode<T>>()

    fun parent( key: T ) : T{
        val node = nodes[Key(key)]
        return if(node == null){
            key
        }else{
            node.find().value
        }
    }

    fun get( value : T ) : UnionNode<T>{
        return nodes.computeIfAbsent(Key(value)){UnionNode(value)}
    }

    fun unify(x: UnionNode<T>, y: T){
        unify(x, get(y))
    }

    fun unify(x: UnionNode<T>, y: UnionNode<T>) {
        val x = x.find()
        val y = y.find()

        if (x == y) return

        if(x.size < y.size){
            y.parent = x
            x.size += y.size
        }else{
            x.parent = y
            y.size += x.size
        }
    }
}

class UnionNode<T>(val value: T){
    var parent : UnionNode<T> = this
    var size = 1

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