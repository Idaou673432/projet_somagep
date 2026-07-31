package com.example.data

import kotlinx.coroutines.flow.Flow

class LeakRepository(private val leakReportDao: LeakReportDao) {
    val allLeaks: Flow<List<LeakReport>> = leakReportDao.getAllLeaks()

    suspend fun getLeakById(id: Int): LeakReport? {
        return leakReportDao.getLeakById(id)
    }

    suspend fun insertLeak(leak: LeakReport): Long {
        return leakReportDao.insertLeak(leak)
    }

    suspend fun updateLeak(leak: LeakReport) {
        leakReportDao.updateLeak(leak)
    }

    suspend fun deleteLeak(leak: LeakReport) {
        leakReportDao.deleteLeak(leak)
    }

    suspend fun deleteAll() {
        leakReportDao.deleteAll()
    }
}
