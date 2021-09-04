package com.codecad.core

class ChainComparator<T>(val comparators: List<(T, T) -> Int>) : Comparator<T> {
    override fun compare(o1: T, o2: T): Int {
        for(comparator in comparators){
            val comparison = comparator(o1,o2)

            if(comparison != 0){
                return comparison
            }
        }

        return 0
    }

    class Builder<T>{
        val comparators: MutableList<(T, T) -> Int> = mutableListOf()

        fun withComparable(invert: Boolean = false, supplier: (T) -> Comparable<*>) : Builder<T>{
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

        fun build() : ChainComparator<T>{
            return ChainComparator(comparators)
        }
    }
}
