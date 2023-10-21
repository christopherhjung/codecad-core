package com.codecad.core.volume

import java.util.*

class ChainComparator<T>(val comparators: List<Comparator<T>>, val default: Int) : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
        for(comparator in comparators){
            val comparison = comparator.compare(o1,o2)

            if(comparison != 0){
                return comparison
            }
        }

        return default
    }

    class Builder<T>{
        val comparators = ArrayList<Comparator<T>>()
        var default : Int = 0

        fun withComparator(comp : Comparator<T>) : Builder<T> {
            comparators.add(comp)
            return this
        }

        fun withComparable(invert: Boolean = false, supplier: (T) -> Comparable<*>) : Builder<T> {
            val supplier = supplier as (T) -> Comparable<Any?>
            if(invert){
                comparators.add{ a,b ->
                    supplier(b).compareTo(supplier(a))
                }
            }else{
                comparators.add{ a,b ->
                    supplier(a).compareTo(supplier(b))
                }
            }

            return this
        }

        fun withDefault( default: Int) : Builder<T> {
            this.default = default
            return this
        }

        fun build() : ChainComparator<T> {
            return ChainComparator(comparators, default)
        }
    }
}
