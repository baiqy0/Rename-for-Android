package com.renamdroid.batchrename.core

/**
 * 自然排序比较器。
 *
 * 普通字符串排序会把 "Episode 10" 排到 "Episode 2" 前面，
 * 自然排序会把连续的数字当作数值来比较，正好符合影视文件的命名习惯。
 */
object NaturalSort {

    fun compare(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        val la = a.length
        val lb = b.length
        while (ia < la && ib < lb) {
            val ca = a[ia]
            val cb = b[ib]
            val da = ca.isDigit()
            val db = cb.isDigit()
            if (da && db) {
                // 两边都是数字：取出完整数字段，按数值比较
                val startA = ia
                while (ia < la && a[ia].isDigit()) ia++
                val startB = ib
                while (ib < lb && b[ib].isDigit()) ib++
                val numA = a.substring(startA, ia).trimStart('0')
                val numB = b.substring(startB, ib).trimStart('0')
                // 先比有效位数，再字典序
                if (numA.length != numB.length) {
                    return numA.length.compareTo(numB.length)
                }
                val cmp = numA.compareTo(numB)
                if (cmp != 0) return cmp
                // 数值相等时，前导零多的排后面（保持稳定）
                val rawCmp = (ia - startA).compareTo(ib - startB)
                if (rawCmp != 0) return rawCmp
            } else if (da != db) {
                // 一个是数字一个不是：数字排在前面（"01.mkv" 排在 ".mkv" 相关前面时更直观）
                return if (da) -1 else 1
            } else {
                val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (cmp != 0) return cmp
                ia++
                ib++
            }
        }
        return (la - ia).compareTo(lb - ib)
    }
}
