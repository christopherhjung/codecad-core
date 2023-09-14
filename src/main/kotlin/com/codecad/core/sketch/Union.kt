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
    val nodes = hashMapOf<Key, UnionNode<T>>()

    fun find(x : UnionNode<T>) : UnionNode<T>{
        return if(x.parent !== x){
            x.parent = find(x.parent)
            x.parent
        }else{
            x
        }
    }

    fun get( value : T ) : UnionNode<T>{
        return nodes.computeIfAbsent(Key(value)){UnionNode(value)}
    }

    fun unify(x: UnionNode<T>, y: UnionNode<T>) {
        val x = find(x)
        val y = find(y)

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