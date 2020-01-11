package com.example.tempernova.helpers

class Queue<T>(list:MutableList<T>){

    var items:MutableList<T> = list

    fun isEmpty():Boolean = items.isEmpty()

    fun size():Int = items.count()

    override fun toString() = items.toString()

    fun enqueue(element:T){
        items.add(element)
    }

    fun dequeue():Any?{
        return if (this.isEmpty()){
            null
        } else {
            items.removeAt(0)
        }
    }

    fun peek():Any?{
        return items[0]
    }

    fun clear() {
        this.items = mutableListOf()
    }

    fun add(element: T): Boolean {
        items.add(element)
        return true
    }
}
