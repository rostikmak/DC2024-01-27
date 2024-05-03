package com.github.kornet_by.dc.lab4.dao

import com.github.kornet_by.dc.lab4.bean.Message

interface MessageDao {
	suspend fun create(item: Message): Long

	suspend fun deleteById(id: Long): Int

	suspend fun getAll(): List<Message?>

	suspend fun getById(id: Long): Message

	suspend fun update(item: Message): Int
}